/*
 * This file is part of MyDMAM.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 17 sept. 2017
 * 
*/
package hd3gtv.mydmam.embddb.store;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.tools.StreamMaker;
import hd3gtv.tools.ThreadPoolExecutorFactory;

public class FileHashTable {
	private static Logger log = Logger.getLogger(FileHashTable.class);
	
	private static final byte[] FILE_INDEX_HEADER = "MYDMAMHSHIDX".getBytes(MyDMAM.UTF8);
	private static final int FILE_INDEX_VERSION = 1;
	private static final int HASH_ENTRY_SIZE = 12;
	private static final int FILE_INDEX_HEADER_LENGTH = FILE_INDEX_HEADER.length + 4 + 4 + 4;
	private static final int LINKED_LIST_ENTRY_SIZE = ItemKey.SIZE + 16;
	
	static final Set<OpenOption> OPEN_OPTIONS_FILE_EXISTS;
	static final Set<OpenOption> OPEN_OPTIONS_FILE_NOT_EXISTS;
	
	static {
		OPEN_OPTIONS_FILE_EXISTS = new HashSet<OpenOption>(3);
		Collections.addAll(OPEN_OPTIONS_FILE_EXISTS, StandardOpenOption.READ, StandardOpenOption.WRITE);
		OPEN_OPTIONS_FILE_NOT_EXISTS = new HashSet<OpenOption>(5);
		Collections.addAll(OPEN_OPTIONS_FILE_NOT_EXISTS, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW, StandardOpenOption.SPARSE);
	}
	
	private final int table_size;
	private final File index_file;
	private final AsynchronousFileChannel index_channel;
	private final long file_index_start;
	private final long start_linked_lists_zone_in_index_file;
	private AtomicLong file_index_write_pointer;
	private final ThreadPoolExecutorFactory index_executor;
	
	private final FileData data_engine;// TODO rename
	
	public FileHashTable(File index_file, File data_file, int table_size) throws IOException, InterruptedException, ExecutionException { // TODO all with CompletableFuture
		
		this.table_size = table_size;
		this.index_file = index_file;
		if (index_file == null) {
			throw new NullPointerException("\"index_file\" can't to be null");
		}
		
		data_engine = new FileData(data_file);
		
		index_executor = new ThreadPoolExecutorFactory(getClass().getSimpleName() + "_Index", Thread.MAX_PRIORITY);
		
		ByteBuffer bytebuffer_header_index = ByteBuffer.allocate(FILE_INDEX_HEADER_LENGTH);
		file_index_start = bytebuffer_header_index.capacity();
		
		start_linked_lists_zone_in_index_file = file_index_start + ((long) table_size) * 12l;
		
		if (index_file.exists()) {
			index_channel = AsynchronousFileChannel.open(index_file.toPath(), FileHashTable.OPEN_OPTIONS_FILE_EXISTS, index_executor.getThreadPoolExecutor());
			index_channel.read(bytebuffer_header_index, 0).get();
			bytebuffer_header_index.flip();
			
			TransactionJournal.readAndEquals(bytebuffer_header_index, FILE_INDEX_HEADER, bad_datas -> {
				return new IOException("Invalid file header: " + new String(bad_datas));
			});
			int version = bytebuffer_header_index.getInt();
			if (version != FILE_INDEX_VERSION) {
				throw new IOException("Invalid version: " + version + " instead of " + FILE_INDEX_VERSION);
			}
			
			int actual_table_size = bytebuffer_header_index.getInt();
			if (actual_table_size != table_size) {
				throw new IOException("Invalid table_size: file is " + actual_table_size + " instead of " + table_size);
			}
			int actual_key_size = bytebuffer_header_index.getInt();
			if (actual_key_size != ItemKey.SIZE) {
				throw new IOException("Invalid key_size: file is " + actual_key_size + " instead of " + ItemKey.SIZE);
			}
			
			file_index_write_pointer = new AtomicLong(Long.max(index_channel.size(), start_linked_lists_zone_in_index_file));
		} else {
			index_channel = AsynchronousFileChannel.open(index_file.toPath(), FileHashTable.OPEN_OPTIONS_FILE_NOT_EXISTS, index_executor.getThreadPoolExecutor());
			bytebuffer_header_index.put(FILE_INDEX_HEADER);
			bytebuffer_header_index.putInt(FILE_INDEX_VERSION);
			bytebuffer_header_index.putInt(table_size);
			bytebuffer_header_index.putInt(ItemKey.SIZE);
			bytebuffer_header_index.flip();
			index_channel.write(bytebuffer_header_index, 0).get();
			file_index_write_pointer = new AtomicLong(start_linked_lists_zone_in_index_file);
		}
	}
	
	/*
	 Method: Separate chaining with linked lists
	 Index file struct:
	            <------------ n entries == table_size ------------->
	 [header...][hash entry][hash entry][hash entry][hash entry]...[linked list entry][linked list entry]...EOF
	            ^ file_index_start      < 12 bytes >                                  <key_size+16 bytes>
	
	 With hash entry struct:
	 <---int, 4 bytes----><----------------long, 8 bytes------------------->
	 [Compressed hash key][absolute position for first index in linked list]
	 
	 With linked list entry struct:
	 <key_size><----------------long, 8 bytes------------------><---------------long, 8 bytes------------------>
	 [hash key][absolute position for user's datas in data file][absolute position for linked list's next index]
	 
	 Data file struct:
	 [header...][user data block][user data block][user data block][user data block]...EOF
	            ^ file_data_start
	
	 With user data block struct:
	 <int, 4 bytes><key_size><--int, 4 bytes--->
	 [ entry len  ][hash key][user's datas size][user's datas][suffix tag]
	*/
	
	private long computeIndexFilePosition(int compressed_key) {
		if (compressed_key > table_size) {
			throw new IndexOutOfBoundsException("Can't get a compressed_key (" + compressed_key + ") > table size (" + table_size + ")");
		}
		return file_index_start + compressed_key * HASH_ENTRY_SIZE;
	}
	
	private int compressKey(byte[] key) {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(key);
		return Math.abs(result) % table_size;
	}
	
	/**
	 * @param onDone data_pointer or -1
	 */
	private void getDataPointerFromHashKey(byte[] key, Consumer<Throwable> onError, Consumer<Long> onDone) {
		// TODO get bloom filter
		int compressed_key = compressKey(key);
		
		readHashEntry(compressed_key, onError, linked_list_pointer -> {
			if (linked_list_pointer == -1l) {
				onDone.accept(-1l);
				return;
			}
			findInLinkedlistEntryData(linked_list_pointer, key, onError, data_pointer -> {
				onDone.accept(data_pointer);
			});
		});
	}
	
	private void updateIndex(byte[] key, long data_pointer, Consumer<Throwable> onError, Runnable onDone) {
		int compressed_key = compressKey(key);
		
		readHashEntry(compressed_key, onError, first_linked_list_pointer -> {
			if (first_linked_list_pointer == -1l) {
				/**
				 * Hash entry don't exists, create it and put in the hash table.
				 */
				writeAppendLinkedlistEntry(key, data_pointer, onError, new_linked_list_pointer -> {
					writeHashEntry(compressed_key, new_linked_list_pointer, onError, onDone);
					// TODO update bloom filter (recalculate ?)
				});
			} else {
				/**
				 * Search if linked list entry exists
				 */
				findLinkedlistEntry(first_linked_list_pointer, -1, key, onError, (linked_list_pointer, next_list_pointer) -> {
					/**
					 * Entry exists, replace current entry
					 */
					writeLinkedlistEntry(linked_list_pointer, key, data_pointer, next_list_pointer, onError, onDone);
				}, last_linked_list_pointer -> {
					if (last_linked_list_pointer == -1l) {
						/**
						 * Add new linked list entry, and attach it for last entry. Overwrite current Hash entry.
						 */
						writeAppendLinkedlistEntry(key, data_pointer, onError, new_linked_list_pointer -> {
							writeHashEntry(compressed_key, new_linked_list_pointer, onError, onDone);
							// TODO update bloom filter (recalculate ?)
						});
					} else {
						/**
						 * Append new entry to actual list (chain)
						 */
						writeAppendLinkedlistEntry(key, data_pointer, onError, new_linked_list_pointer -> {
							writeNextLinkedlistEntry(last_linked_list_pointer, new_linked_list_pointer, onError, onDone);
							// TODO update bloom filter (recalculate ?)
						});
					}
				}, null);
			}
		});
	}
	
	/**
	 * @param onHashEntry return an Stream of firsts linked_list_pointers
	 * @return
	 */
	CompletableFuture<Stream<HashEntry>> getAllHashEntries() { // TODO set to private
		ByteBuffer read_buffer = ByteBuffer.allocate(HASH_ENTRY_SIZE * table_size);
		CompletableFuture<Integer> on_done = asyncRead(index_channel, read_buffer, file_index_start);
		
		return on_done.thenApply(size -> {
			if (size == 0) {
				return Stream.empty();
			}
			read_buffer.flip();
			if (read_buffer.capacity() != size) {
				return Stream.empty();
			}
			return StreamMaker.create(() -> {
				if (read_buffer.remaining() - HASH_ENTRY_SIZE >= 0) {
					/*
					Hash entry struct:
					<---int, 4 bytes----><----------------long, 8 bytes------------------->
					[Compressed hash key][absolute position for first index in linked list]
					*/
					int compressed_hash_key = read_buffer.getInt();
					long linked_list_first_index = read_buffer.getLong();
					
					return new HashEntry(compressed_hash_key, linked_list_first_index);
				} else {
					return null;
				}
			}, e -> {
				on_done.completeExceptionally(e);
			}).stream();
		});
	}
	
	class HashEntry { // TODO set to private
		int compressed_hash_key;
		long linked_list_first_index;
		
		HashEntry(int compressed_hash_key, long linked_list_first_index) {
			this.compressed_hash_key = compressed_hash_key;
			this.linked_list_first_index = linked_list_first_index;
		}
	}
	
	class LinkedListEntry { // TODO set to private
		byte[] current_key;
		long data_pointer;
		
		LinkedListEntry(byte[] current_key, long data_pointer) {
			this.current_key = current_key;
			this.data_pointer = data_pointer;
		}
		
	}
	
	/*
	Linked list entry struct:
	<key_size><----------------long, 8 bytes------------------><---------------long, 8 bytes------------------>
	[hash key][absolute position for user's datas in data file][absolute position for linked list's next index]
	*/
	CompletableFuture<Stream<LinkedListEntry>> getAllLinkedListItems(CompletableFuture<Stream<HashEntry>> hash_entries) { // TODO set to private
		ByteBuffer linkedlist_entry_buffer = ByteBuffer.allocate(LINKED_LIST_ENTRY_SIZE);
		
		AtomicReference<Long> next_pointer = new AtomicReference<>(-1l);
		
		return hash_entries.thenApply(entries -> {
			return entries.map(entry -> {
				return entry.linked_list_first_index;
			}).flatMap(linked_list_pointer -> {
				next_pointer.set(linked_list_pointer);
				
				return StreamMaker.create(() -> {
					if (next_pointer.get() <= 0) {
						return null;
					}
					try {
						return asyncRead(index_channel, linkedlist_entry_buffer, next_pointer.get()).thenApply(s -> {
							if (s != LINKED_LIST_ENTRY_SIZE) {
								return null;
							}
							linkedlist_entry_buffer.flip();
							byte[] current_key = new byte[ItemKey.SIZE];
							linkedlist_entry_buffer.get(current_key);
							long data_pointer = linkedlist_entry_buffer.getLong();
							long next_linked_list_pointer = linkedlist_entry_buffer.getLong();
							linkedlist_entry_buffer.clear();
							
							next_pointer.set(next_linked_list_pointer);
							return new LinkedListEntry(current_key, data_pointer);
						}).get();
					} catch (InterruptedException | ExecutionException e) {
						hash_entries.completeExceptionally(e);
						return null;
					}
				}).stream();
			});
		});
	}
	
	/**
	 * @param nextItem key -> Data pointer
	 */
	private void getAllHashsLinkedListItems(BiConsumer<byte[], Long> nextKeyDataPointer, Consumer<Throwable> onError) {
		// TODO
		/*
		fht.getAllHashEntries().get().forEach(l -> {
			System.out.println(l);
		});
		 * */
		/*getAllHashEntries(linked_list_pointer_stream -> {
			linked_list_pointer_stream.forEach(linked_list_pointer -> {
				getAllNextLinkedListItemsFrom(linked_list_pointer, nextKeyDataPointer, onError);
			});
		}, onError);*/
	}
	
	/*
	Linked list entry struct:
	<key_size><----------------long, 8 bytes------------------><---------------long, 8 bytes------------------>
	[hash key][absolute position for user's datas in data file][absolute position for linked list's next index]
	*/
	/*public Stream<ItemKey> forEachKeys() throws RuntimeException, IOException, InterruptedException, ExecutionException {
		return forEachHashEntry().flatMap(first_linked_list_pointer -> {
			return forEachLinkedListItemChainDataKeys(first_linked_list_pointer, linkedlist_entry_buffer -> {
				byte[] result = new byte[ItemKey.SIZE];
				linkedlist_entry_buffer.get(result, 0, ItemKey.SIZE);
				return result;
			});
		}).map(k -> {
			return new ItemKey(k);
		});
	}*/
	
	/*
	Linked list entry struct:
	<key_size><----------------long, 8 bytes------------------><---------------long, 8 bytes------------------>
	[hash key][absolute position for user's datas in data file][absolute position for linked list's next index]
	*/
	/*Stream<Entry> forEachKeyValues() throws RuntimeException, IOException, InterruptedException, ExecutionException {
		return forEachHashEntry().flatMap(first_linked_list_pointer -> {
			return forEachLinkedListItemChainDataKeys(first_linked_list_pointer, linkedlist_entry_buffer -> {
				return linkedlist_entry_buffer.getLong(ItemKey.SIZE);
			});
		}).filter(data_engine.checkDataPointerValidity).map(data_engine.getDataEntryFromDataPointer).filter(entry -> {
			return entry != null;
		});
	}*/
	
	/**
	 * Just write a new entry
	 * Prepare entry and write it
	 */
	private void writeHashEntry(int compressed_key, long first_linked_list_pointer, Consumer<Throwable> onError, Runnable onDone) {
		/*
		Hash entry struct:
		<---int, 4 bytes----><----------------long, 8 bytes------------------->
		[Compressed hash key][absolute position for first index in linked list]
		*/
		ByteBuffer key_table_buffer = ByteBuffer.allocate(HASH_ENTRY_SIZE);
		key_table_buffer.putInt(compressed_key);
		key_table_buffer.putLong(first_linked_list_pointer);
		key_table_buffer.flip();
		
		asyncWrite(index_channel, key_table_buffer, computeIndexFilePosition(compressed_key), onError, size -> {
			onDone.run();
		});
	}
	
	private void writeClearHashEntry(int compressed_key, Consumer<Throwable> onError, Runnable onDone) {
		/*
		Hash entry struct:
		<---int, 4 bytes----><----------------long, 8 bytes------------------->
		[Compressed hash key][absolute position for first index in linked list]
		*/
		ByteBuffer key_table_buffer = ByteBuffer.allocate(HASH_ENTRY_SIZE);
		key_table_buffer.putInt(0);
		key_table_buffer.putLong(0);
		key_table_buffer.flip();
		asyncWrite(index_channel, key_table_buffer, computeIndexFilePosition(compressed_key), onError, size -> {
			onDone.run();
		});
	}
	
	/**
	 * @param onDone the Absolute position for the first index in linked list, or -1
	 */
	private void readHashEntry(int compressed_key, Consumer<Throwable> onError, Consumer<Long> onDone) {
		/*
		Hash entry struct:
		<---int, 4 bytes----><----------------long, 8 bytes------------------->
		[Compressed hash key][absolute position for first index in linked list]
		                     <----------------- GET THIS --------------------->
		*/
		long index_file_pos = computeIndexFilePosition(compressed_key);
		if (index_file_pos > index_file.length()) {
			onDone.accept(-1l);
			return;
		}
		
		ByteBuffer key_table_buffer = ByteBuffer.allocate(HASH_ENTRY_SIZE);
		asyncRead(index_channel, key_table_buffer, index_file_pos, onError, size -> {
			if (size != HASH_ENTRY_SIZE) {
				onDone.accept(-1l);
				return;
			}
			key_table_buffer.flip();
			if (compressed_key != key_table_buffer.getInt()) {
				onDone.accept(-1l);
				return;
			}
			long result = key_table_buffer.getLong();
			if (result == 0) {
				onDone.accept(-1l);
				return;
			}
			onDone.accept(result);
		});
	}
	
	private void writeLinkedlistEntry(long linked_list_pointer, byte[] key, long data_pointer, long next_linked_list_pointer, Consumer<Throwable> onError, Runnable onDone) {
		/*
		Linked list entry struct:
		<key_size><----------------long, 8 bytes------------------><---------------long, 8 bytes------------------>
		[hash key][absolute position for user's datas in data file][absolute position for linked list's next index]
		*/
		ByteBuffer linkedlist_entry_buffer = ByteBuffer.allocate(LINKED_LIST_ENTRY_SIZE);
		linkedlist_entry_buffer.put(key);
		linkedlist_entry_buffer.putLong(data_pointer);
		linkedlist_entry_buffer.putLong(next_linked_list_pointer);
		linkedlist_entry_buffer.flip();
		
		asyncWrite(index_channel, linkedlist_entry_buffer, linked_list_pointer, onError, s -> {
			onDone.run();
		});
	}
	
	/**
	 * @param onDone the data_pointer or -1
	 */
	private void findInLinkedlistEntryData(long linked_list_pointer, byte[] targeted_key, Consumer<Throwable> onError, Consumer<Long> onDone) {
		/*
		Linked list entry struct:
		<key_size><----------------long, 8 bytes------------------><---------------long, 8 bytes------------------>
		[hash key][absolute position for user's datas in data file][absolute position for linked list's next index]
		          <----------------- GET THIS -------------------->
		*/
		ByteBuffer linkedlist_entry_buffer = ByteBuffer.allocate(LINKED_LIST_ENTRY_SIZE);
		
		asyncRead(index_channel, linkedlist_entry_buffer, linked_list_pointer, onError, s -> {
			if (s != LINKED_LIST_ENTRY_SIZE) {
				onDone.accept(-1l);
				return;
			}
			linkedlist_entry_buffer.flip();
			byte[] current_key = new byte[targeted_key.length];
			linkedlist_entry_buffer.get(current_key);
			long data_pointer = linkedlist_entry_buffer.getLong();
			long next_linked_list_pointer = linkedlist_entry_buffer.getLong();
			
			if (Arrays.equals(targeted_key, current_key)) {
				if (data_pointer < 1l) {
					onDone.accept(-1l);
				} else {
					onDone.accept(data_pointer);
				}
			} else if (next_linked_list_pointer > -1) {
				/**
				 * The princess is in another castle
				 */
				findInLinkedlistEntryData(next_linked_list_pointer, targeted_key, onError, onDone);
			} else {
				onDone.accept(-1l);
			}
		});
	}
	
	private void writeNextLinkedlistEntry(long linked_list_pointer, long new_next_linked_list_pointer, Consumer<Throwable> onError, Runnable onDone) {
		/*
		Linked list entry struct:
		<key_size><----------------long, 8 bytes------------------><---------------long, 8 bytes------------------>
		[hash key][absolute position for user's datas in data file][absolute position for linked list's next index]
		                                                           <--------------- UPDATE THIS ------------------>
		*/
		ByteBuffer linkedlist_entry_buffer = ByteBuffer.allocate(8);
		linkedlist_entry_buffer.putLong(new_next_linked_list_pointer);
		linkedlist_entry_buffer.flip();
		
		asyncWrite(index_channel, linkedlist_entry_buffer, linked_list_pointer + ItemKey.SIZE + 8, onError, s -> {
			onDone.run();
		});
	}
	
	/**
	 * @param onFoundEntry the current linked list entry pointer and the next linked list entry pointer (for this current). Can be null.
	 * @param onNotFoundEntry the last linked list entry or -1 (empty list)
	 * @param onFoundEntryOriginNext the origin (previous item in list) and the next linked list entry pointer (for this current). Can be null.
	 */
	private void findLinkedlistEntry(long linked_list_pointer, long linked_list_pointer_origin, byte[] targeted_key, Consumer<Throwable> onError, BiConsumer<Long, Long> onFoundEntry, Consumer<Long> onNotFoundEntry, BiConsumer<Long, Long> onFoundEntryOriginNext) {
		/*
		Linked list entry struct:
		<key_size><----------------long, 8 bytes------------------><---------------long, 8 bytes------------------>
		[hash key][absolute position for user's datas in data file][absolute position for linked list's next index]
		^ GET THIS
		*/
		ByteBuffer linkedlist_entry_buffer = ByteBuffer.allocate(LINKED_LIST_ENTRY_SIZE);
		
		asyncRead(index_channel, linkedlist_entry_buffer, linked_list_pointer, onError, s -> {
			if (s != LINKED_LIST_ENTRY_SIZE) {
				onNotFoundEntry.accept(-1l);
				return;
			}
			linkedlist_entry_buffer.flip();
			byte[] current_key = new byte[targeted_key.length];
			linkedlist_entry_buffer.get(current_key);
			/*long data_pointer =*/ linkedlist_entry_buffer.getLong();
			long next_linked_list_pointer = linkedlist_entry_buffer.getLong();
			
			if (Arrays.equals(targeted_key, current_key)) {
				if (onFoundEntry != null) {
					onFoundEntry.accept(linked_list_pointer, next_linked_list_pointer);
				}
				if (onFoundEntryOriginNext != null) {
					onFoundEntryOriginNext.accept(linked_list_pointer_origin, next_linked_list_pointer);
				}
			} else if (next_linked_list_pointer > -1) {
				/**
				 * The princess (entry) is in another castle
				 */
				findLinkedlistEntry(next_linked_list_pointer, linked_list_pointer, targeted_key, onError, onFoundEntry, onNotFoundEntry, onFoundEntryOriginNext);
			} else {
				onNotFoundEntry.accept(linked_list_pointer);
			}
		});
	}
	
	/**
	 * Simple get, don't do recursive things.
	 * @param onDone the next linked list entry pointer (for this current) or -1 (empty list/not found)
	 */
	private void isLinkedlistEntryTargetKey(long linked_list_pointer, byte[] targeted_key, Consumer<Throwable> onError, Consumer<Long> onFounded, Runnable onNotFounded) {
		/*
		Linked list entry struct:
		<key_size><----------------long, 8 bytes------------------><---------------long, 8 bytes------------------>
		[hash key][absolute position for user's datas in data file][absolute position for linked list's next index]
		^ GET THIS
		*/
		ByteBuffer linkedlist_entry_buffer = ByteBuffer.allocate(LINKED_LIST_ENTRY_SIZE);
		
		asyncRead(index_channel, linkedlist_entry_buffer, linked_list_pointer, onError, s -> {
			if (s != LINKED_LIST_ENTRY_SIZE) {
				onNotFounded.run();
				return;
			}
			linkedlist_entry_buffer.flip();
			byte[] current_key = new byte[targeted_key.length];
			linkedlist_entry_buffer.get(current_key);
			/*long data_pointer =*/ linkedlist_entry_buffer.getLong();
			long next_linked_list_pointer = linkedlist_entry_buffer.getLong();
			
			if (Arrays.equals(targeted_key, current_key)) {
				onFounded.accept(next_linked_list_pointer);
			} else {
				onNotFounded.run();
			}
		});
	}
	
	/**
	 * @param onDone created item position
	 */
	private void writeAppendLinkedlistEntry(byte[] key, long data_pointer, Consumer<Throwable> onError, Consumer<Long> onDone) {
		/*
		Linked list entry struct:
		<key_size><----------------long, 8 bytes------------------><---------------long, 8 bytes------------------>
		[hash key][absolute position for user's datas in data file][absolute position for linked list's next index]
		*/
		ByteBuffer linkedlist_entry_buffer = ByteBuffer.allocate(LINKED_LIST_ENTRY_SIZE);
		linkedlist_entry_buffer.put(key);
		linkedlist_entry_buffer.putLong(data_pointer);
		linkedlist_entry_buffer.putLong(-1l); // next_linked_list_pointer
		linkedlist_entry_buffer.flip();
		
		long linked_list_pointer = file_index_write_pointer.getAndAdd(LINKED_LIST_ENTRY_SIZE);
		
		asyncWrite(index_channel, linkedlist_entry_buffer, linked_list_pointer, onError, s -> {
			onDone.accept(linked_list_pointer);
		});
	}
	
	// TODO add bloom filter for all keys
	
	static CompletableFuture<Integer> asyncRead(AsynchronousFileChannel channel, ByteBuffer buffer, long position) {
		CompletableFuture<Integer> completable_future = new CompletableFuture<>();
		
		channel.read(buffer, position, position, new CompletionHandler<Integer, Long>() {
			
			public void completed(Integer result, Long position) {
				completable_future.complete(result);
			}
			
			public void failed(Throwable e, Long position) {
				completable_future.completeExceptionally(e);
			}
		});
		return completable_future;
	}
	
	static CompletableFuture<Integer> asyncWrite(AsynchronousFileChannel channel, ByteBuffer buffer, long position) {
		CompletableFuture<Integer> completable_future = new CompletableFuture<>();
		
		channel.write(buffer, position, position, new CompletionHandler<Integer, Long>() {
			
			public void completed(Integer result, Long position) {
				completable_future.complete(result);
			}
			
			public void failed(Throwable e, Long position) {
				completable_future.completeExceptionally(e);
			}
		});
		return completable_future;
	}
	
	@Deprecated
	static void asyncRead(AsynchronousFileChannel channel, ByteBuffer buffer, long position, Consumer<Throwable> onError, Consumer<Integer> completed) {
		channel.read(buffer, position, position, new CompletionHandler<Integer, Long>() {
			
			public void completed(Integer result, Long position) {
				completed.accept(result);
			}
			
			public void failed(Throwable e, Long position) {
				onError.accept(e);
			}
		});
	}
	
	@Deprecated
	static void asyncWrite(AsynchronousFileChannel channel, ByteBuffer buffer, long position, Consumer<Throwable> onError, Consumer<Integer> completed) {
		channel.write(buffer, position, position, new CompletionHandler<Integer, Long>() {
			
			public void completed(Integer result, Long position) {
				completed.accept(result);
			}
			
			public void failed(Throwable e, Long position) {
				onError.accept(e);
			}
		});
	}
	
	public void put(ItemKey key, byte[] user_data, Consumer<Throwable> onError, Consumer<ItemKey> onDone) {
		data_engine.writeData(key.key, user_data, onError, data_pointer -> {
			updateIndex(key.key, data_pointer, onError, () -> {
				onDone.accept(key);
			});
		});
	}
	
	public void get(ItemKey key, Consumer<Throwable> onError, BiConsumer<ItemKey, byte[]> onDone, Consumer<ItemKey> notFound) {
		getDataPointerFromHashKey(key.key, onError, data_pointer -> {
			if (data_pointer == -1) {
				notFound.accept(key);
			} else {
				data_engine.readData(data_pointer, onError, (k, d) -> {
					if (k == null) {
						notFound.accept(key);
					} else {
						onDone.accept(key, d);
					}
				});
			}
		});
	}
	
	/**
	 * Internal datas will not removed. Only references.
	 */
	public void remove(ItemKey item_key, Consumer<Throwable> onError, Consumer<ItemKey> onDone) {
		byte[] key = item_key.key;
		int compressed_key = compressKey(key);
		
		readHashEntry(compressed_key, onError, linked_list_pointer -> {
			if (linked_list_pointer == -1l) {
				/**
				 * Can't found hash record: nothing to delete.
				 */
				onDone.accept(item_key);
			} else {
				isLinkedlistEntryTargetKey(linked_list_pointer, key, onError, next_linked_list_pointer -> {
					if (next_linked_list_pointer == -1l) {
						/**
						 * First linkedlist record is not this key. Let's finds the next chain entries.
						 */
						findLinkedlistEntry(next_linked_list_pointer, -1, key, onError, null, last_linked_list_pointer -> {
							/**
							 * Not in the list? Nothing to delete.
							 */
							onDone.accept(item_key);
						}, (previous_linked_list_pointer, next_founded_list_pointer) -> {
							/**
							 * Raccord in linkedlist chain the previous item and the next, and orphan this record.
							 */
							writeNextLinkedlistEntry(previous_linked_list_pointer, next_founded_list_pointer, onError, () -> {
								onDone.accept(item_key);
							});
							// TODO update bloom filter (recalculate ?)
						});
					} else {
						/**
						 * This first linkedlist record is the key. Change it for the next.
						 */
						writeHashEntry(compressed_key, next_linked_list_pointer, onError, () -> {
							onDone.accept(item_key);
						});
						// TODO update bloom filter (recalculate ?)
					}
				}, () -> {
					/**
					 * Empty list... Clear hashentry
					 */
					writeClearHashEntry(compressed_key, onError, () -> {
						onDone.accept(item_key);
					});
					// TODO update bloom filter (recalculate ?)
				});
			}
		});
	}
	
	public CompletableFuture<Integer> size() throws IOException {
		return getAllLinkedListItems(getAllHashEntries()).thenApply(entries -> {
			return (int) entries.count();
		});
	}
	
	public CompletableFuture<Boolean> isEmpty() {
		return getAllHashEntries().thenApply(entries -> {
			return entries.anyMatch(p -> true);
		}).thenApply(r -> !r);
	}
	
	public void has(ItemKey key, Consumer<Throwable> onError, Consumer<Boolean> onDone) {
		// TODO via bloom filter...
		getDataPointerFromHashKey(key.key, onError, data_pointer -> {
			onDone.accept(data_pointer != -1);
		});
	}
	
	public void clear() throws IOException, InterruptedException, ExecutionException {
		data_engine.clear();
		
		file_index_write_pointer.set(start_linked_lists_zone_in_index_file);
		index_executor.getThreadPoolExecutor().getQueue().clear();
		index_channel.truncate(FILE_INDEX_HEADER_LENGTH);
		index_channel.force(true);
	}
	
}
