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
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.embddb.store.FileData.Entry;
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
	
	private final FileData data;
	
	public FileHashTable(File index_file, File data_file, int table_size) throws IOException, InterruptedException, ExecutionException {
		
		this.table_size = table_size;
		this.index_file = index_file;
		if (index_file == null) {
			throw new NullPointerException("\"index_file\" can't to be null");
		}
		
		data = new FileData(data_file);
		
		index_executor = new ThreadPoolExecutorFactory(getClass().getSimpleName() + "_Index", Thread.MAX_PRIORITY);
		index_executor.setSimplePoolSize();// XXX
		
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
	
	static CompletableFuture<Integer> asyncRead(AsynchronousFileChannel channel, ByteBuffer buffer, long position) {
		CompletableFuture<Integer> completable_future = new CompletableFuture<>();
		
		channel.read(buffer, position, position, new CompletionHandler<Integer, Long>() {
			
			public void completed(Integer result, Long position) {
				if (log.isTraceEnabled()) {
					log.trace("Async read done: " + result + " bytes from " + position);
				}
				completable_future.complete(result);
			}
			
			public void failed(Throwable e, Long position) {
				if (log.isDebugEnabled()) {
					log.debug("Async read fail: in " + position, e);
				}
				completable_future.completeExceptionally(e);
			}
		});
		return completable_future;
	}
	
	/**
	 * @param buffer full buffer write is tested
	 */
	static CompletableFuture<Integer> asyncWrite(AsynchronousFileChannel channel, ByteBuffer buffer, long position) {
		CompletableFuture<Integer> completable_future = new CompletableFuture<>();
		int rem = buffer.remaining();
		
		channel.write(buffer, position, position, new CompletionHandler<Integer, Long>() {
			
			public void completed(Integer writed_size, Long position) {
				if (rem != writed_size) {
					if (log.isDebugEnabled()) {
						log.debug("Async write fail: in " + position);
					}
					completable_future.completeExceptionally(new IOException("Can't write all datas: " + writed_size + "/" + rem));
				} else {
					if (log.isTraceEnabled()) {
						log.trace("Async write done: " + writed_size + " bytes from " + position);
					}
					completable_future.complete(writed_size);
				}
			}
			
			public void failed(Throwable e, Long position) {
				if (log.isDebugEnabled()) {
					log.debug("Async write fail: in " + position, e);
				}
				completable_future.completeExceptionally(e);
			}
		});
		return completable_future;
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
	 * @param onHashEntry return an Stream of firsts linked_list_pointers
	 * @return
	 */
	private CompletableFuture<Stream<HashEntry>> getAllHashEntries() {
		ByteBuffer read_buffer = ByteBuffer.allocate(HASH_ENTRY_SIZE * table_size);
		CompletableFuture<Integer> on_done = asyncRead(index_channel, read_buffer, file_index_start);
		
		return on_done.thenApply(size -> {
			if (size < 1) {
				return Stream.empty();
			}
			read_buffer.flip();
			if (read_buffer.capacity() != size) {
				return Stream.empty();
			}
			return StreamMaker.create(() -> {
				if (read_buffer.remaining() - HASH_ENTRY_SIZE >= 0) {
					return new HashEntry(read_buffer);
				} else {
					return null;
				}
			}, e -> {
				on_done.completeExceptionally(e);
			}).stream();
		});
	}
	
	private class HashEntry {
		int compressed_hash_key;
		long linked_list_first_index;
		
		HashEntry(int compressed_hash_key, long linked_list_first_index) {
			this.compressed_hash_key = compressed_hash_key;
			this.linked_list_first_index = linked_list_first_index;
		}
		
		/*
		Hash entry struct:
		<---int, 4 bytes----><----------------long, 8 bytes------------------->
		[Compressed hash key][absolute position for first index in linked list]
		*/
		HashEntry(ByteBuffer read_buffer) {
			compressed_hash_key = read_buffer.getInt();
			linked_list_first_index = read_buffer.getLong();
			if (log.isTraceEnabled()) {
				log.trace("Read new HashEntry: compressed_hash_key = " + compressed_hash_key + ", linked_list_first_index = " + linked_list_first_index);
			}
		}
		
		void toByteBuffer(ByteBuffer write_buffer) {
			if (log.isTraceEnabled()) {
				log.trace("Prepare HashEntry write: compressed_hash_key = " + compressed_hash_key + ", linked_list_first_index = " + linked_list_first_index);
			}
			write_buffer.putInt(compressed_hash_key);
			write_buffer.putLong(linked_list_first_index);
		}
	}
	
	private class LinkedListEntry {
		byte[] current_key;
		long data_pointer;
		long next_linked_list_pointer;
		
		LinkedListEntry(byte[] current_key, long data_pointer, long next_linked_list_pointer) {
			this.current_key = current_key;
			this.data_pointer = data_pointer;
			this.next_linked_list_pointer = next_linked_list_pointer;
		}
		
		/*
		Linked list entry struct:
		<key_size><----------------long, 8 bytes------------------><---------------long, 8 bytes------------------>
		[hash key][absolute position for user's datas in data file][absolute position for linked list's next index]
		*/
		LinkedListEntry(ByteBuffer linkedlist_entry_buffer) {
			current_key = new byte[ItemKey.SIZE];
			linkedlist_entry_buffer.get(current_key);
			data_pointer = linkedlist_entry_buffer.getLong();
			next_linked_list_pointer = linkedlist_entry_buffer.getLong();
			
			if (log.isTraceEnabled()) {
				log.trace("Read new LinkedListEntry: current_key = " + MyDMAM.byteToString(current_key) + ", data_pointer = " + data_pointer + ", next_linked_list_pointer = " + next_linked_list_pointer);
			}
		}
		
		void toByteBuffer(ByteBuffer write_buffer) {
			if (log.isTraceEnabled()) {
				log.trace("Prepare LinkedListEntry write: current_key = " + MyDMAM.byteToString(current_key) + ", data_pointer = " + data_pointer + ", next_linked_list_pointer = " + next_linked_list_pointer);
			}
			write_buffer.put(current_key);
			write_buffer.putLong(data_pointer);
			write_buffer.putLong(next_linked_list_pointer);
		}
	}
	
	private CompletableFuture<Stream<LinkedListEntry>> getAllLinkedListItems(CompletableFuture<Stream<HashEntry>> hash_entries) {
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
							LinkedListEntry result = new LinkedListEntry(linkedlist_entry_buffer);
							linkedlist_entry_buffer.clear();
							next_pointer.set(result.next_linked_list_pointer);
							return result;
						}).get();
					} catch (InterruptedException | ExecutionException e) {
						hash_entries.completeExceptionally(e);
						return null;
					}
				}).stream();
			});
		});
	}
	
	public CompletableFuture<Stream<ItemKey>> forEachKeys() {
		return getAllLinkedListItems(getAllHashEntries()).thenApply(lle_stream -> {
			return lle_stream.map(lle -> {
				return new ItemKey(lle.current_key);
			});
		});
	}
	
	public CompletableFuture<Stream<CompletableFuture<Entry>>> forEachKeyValue() {
		return getAllLinkedListItems(getAllHashEntries()).thenApply(lle_stream -> {
			return lle_stream.map(lle -> {
				if (lle.data_pointer <= 0) {
					return null;
				}
				return data.read(lle.data_pointer);
			});
		});
	}
	
	public Stream<Entry> getAllKeyValues() {
		try {
			return forEachKeyValue().get().map(cf_entry -> {
				try {
					return cf_entry.get();
				} catch (Exception e) {
					throw new RuntimeException(e.getCause());
				}
			});
		} catch (Exception e) {
			throw new RuntimeException(e.getCause());
		}
	}
	
	/**
	 * Just write an entry: Prepare entry and write it
	 */
	private CompletableFuture<Void> writeHashEntry(HashEntry has_entry) {
		ByteBuffer key_table_buffer = ByteBuffer.allocate(HASH_ENTRY_SIZE);
		has_entry.toByteBuffer(key_table_buffer);
		key_table_buffer.flip();
		
		return asyncWrite(index_channel, key_table_buffer, computeIndexFilePosition(has_entry.compressed_hash_key)).thenApply(s -> {
			return null;
		});
	}
	
	/**
	 * @param onDone the Absolute position for the first index in linked list, or null
	 */
	private CompletableFuture<HashEntry> readHashEntry(int compressed_key) {
		long index_file_pos = computeIndexFilePosition(compressed_key);
		if (index_file_pos > index_file.length()) {
			return CompletableFuture.completedFuture(null);
		}
		
		ByteBuffer key_table_buffer = ByteBuffer.allocate(HASH_ENTRY_SIZE);
		return asyncRead(index_channel, key_table_buffer, index_file_pos).thenApply(size -> {
			if (size != HASH_ENTRY_SIZE) {
				return null;
			}
			key_table_buffer.flip();
			if (compressed_key != key_table_buffer.getInt()) {
				return null;
			}
			long result = key_table_buffer.getLong();
			if (result == 0) {
				return null;
			}
			return new HashEntry(compressed_key, result);
		});
	}
	
	private CompletableFuture<Void> writeLinkedlistEntry(long linked_list_pointer, LinkedListEntry linked_list_entry) {
		ByteBuffer linkedlist_entry_buffer = ByteBuffer.allocate(LINKED_LIST_ENTRY_SIZE);
		linked_list_entry.toByteBuffer(linkedlist_entry_buffer);
		linkedlist_entry_buffer.flip();
		
		return asyncWrite(index_channel, linkedlist_entry_buffer, linked_list_pointer).thenAccept(s -> {
		});
	}
	
	/**
	 * @return the data_pointer or -1
	 */
	private CompletableFuture<Long> findInLinkedlistEntryData(long linked_list_pointer, byte[] targeted_key) {
		/*
		Linked list entry struct:
		<key_size><----------------long, 8 bytes------------------><---------------long, 8 bytes------------------>
		[hash key][absolute position for user's datas in data file][absolute position for linked list's next index]
		          <----------------- GET THIS -------------------->
		*/
		ByteBuffer linkedlist_entry_buffer = ByteBuffer.allocate(LINKED_LIST_ENTRY_SIZE);
		
		return asyncRead(index_channel, linkedlist_entry_buffer, linked_list_pointer).thenCompose(s -> {
			if (s != LINKED_LIST_ENTRY_SIZE) {
				return CompletableFuture.completedFuture(-1l);
			}
			linkedlist_entry_buffer.flip();
			byte[] current_key = new byte[targeted_key.length];
			linkedlist_entry_buffer.get(current_key);
			long data_pointer = linkedlist_entry_buffer.getLong();
			long next_linked_list_pointer = linkedlist_entry_buffer.getLong();
			
			if (Arrays.equals(targeted_key, current_key)) {
				if (data_pointer < 1l) {
					return CompletableFuture.completedFuture(-1l);
				} else {
					return CompletableFuture.completedFuture(data_pointer);
				}
			} else if (next_linked_list_pointer > -1) {
				/**
				 * The princess is in another castle
				 */
				return findInLinkedlistEntryData(next_linked_list_pointer, targeted_key);
			} else {
				return CompletableFuture.completedFuture(-1l);
			}
		});
	}
	
	private CompletableFuture<Void> writeNextLinkedlistEntry(long linked_list_pointer, long new_next_linked_list_pointer) {
		/*
		Linked list entry struct:
		<key_size><----------------long, 8 bytes------------------><---------------long, 8 bytes------------------>
		[hash key][absolute position for user's datas in data file][absolute position for linked list's next index]
		                                                           <--------------- UPDATE THIS ------------------>
		*/
		ByteBuffer linkedlist_entry_buffer = ByteBuffer.allocate(8);
		linkedlist_entry_buffer.putLong(new_next_linked_list_pointer);
		linkedlist_entry_buffer.flip();
		
		return asyncWrite(index_channel, linkedlist_entry_buffer, linked_list_pointer + ItemKey.SIZE + 8).thenAccept(s -> {
		});
	}
	
	private class LinkedListFoundEntry {
		boolean not_found;
		long linked_list_pointer_origin;
		long next_linked_list_pointer;
		long linked_list_pointer;
	}
	
	private CompletableFuture<LinkedListFoundEntry> findLinkedlistEntry(long linked_list_pointer, long linked_list_pointer_origin, byte[] targeted_key) {
		/*
		Linked list entry struct:
		<key_size><----------------long, 8 bytes------------------><---------------long, 8 bytes------------------>
		[hash key][absolute position for user's datas in data file][absolute position for linked list's next index]
		^ GET THIS
		*/
		ByteBuffer linkedlist_entry_buffer = ByteBuffer.allocate(LINKED_LIST_ENTRY_SIZE);
		
		return asyncRead(index_channel, linkedlist_entry_buffer, linked_list_pointer).thenCompose(s -> {
			LinkedListFoundEntry result = new LinkedListFoundEntry();
			
			if (s != LINKED_LIST_ENTRY_SIZE) {
				result.not_found = true;
			} else {
				linkedlist_entry_buffer.flip();
				byte[] current_key = new byte[targeted_key.length];
				linkedlist_entry_buffer.get(current_key);
				/*long data_pointer =*/ linkedlist_entry_buffer.getLong();
				long next_linked_list_pointer = linkedlist_entry_buffer.getLong();
				
				if (Arrays.equals(targeted_key, current_key)) {
					result.linked_list_pointer = linked_list_pointer;
					result.next_linked_list_pointer = next_linked_list_pointer;
					result.linked_list_pointer_origin = linked_list_pointer_origin;
				} else if (next_linked_list_pointer > -1) {
					/**
					 * The princess (entry) is in another castle
					 */
					return findLinkedlistEntry(next_linked_list_pointer, linked_list_pointer, targeted_key);
				} else {
					result.linked_list_pointer = linked_list_pointer;
				}
			}
			return CompletableFuture.completedFuture(result);
		});
	}
	
	/**
	 * Simple get, don't do recursive things.
	 * @retrun the next linked list entry pointer (for this current) or null
	 */
	private CompletableFuture<LinkedListEntry> isLinkedlistEntryTargetKey(long linked_list_pointer, byte[] targeted_key) {
		/*
		Linked list entry struct:
		<key_size><----------------long, 8 bytes------------------><---------------long, 8 bytes------------------>
		[hash key][absolute position for user's datas in data file][absolute position for linked list's next index]
		^ GET THIS
		*/
		ByteBuffer linkedlist_entry_buffer = ByteBuffer.allocate(LINKED_LIST_ENTRY_SIZE);
		
		return asyncRead(index_channel, linkedlist_entry_buffer, linked_list_pointer).thenApply(s -> {
			if (s != LINKED_LIST_ENTRY_SIZE) {
				return null;
			}
			linkedlist_entry_buffer.flip();
			LinkedListEntry result = new LinkedListEntry(linkedlist_entry_buffer);
			if (Arrays.equals(targeted_key, result.current_key)) {
				return result;
			} else {
				return null;
			}
		});
	}
	
	/**
	 * @param onDone created item position
	 */
	private CompletableFuture<Long> writeAppendLinkedlistEntry(LinkedListEntry ll_entry) {
		/*
		Linked list entry struct:
		<key_size><----------------long, 8 bytes------------------><---------------long, 8 bytes--------------------->
		[hash key][absolute position for user's datas in data file][absolute position for linked list's next index: 0]
		*/
		ByteBuffer linkedlist_entry_buffer = ByteBuffer.allocate(LINKED_LIST_ENTRY_SIZE);
		ll_entry.toByteBuffer(linkedlist_entry_buffer);
		linkedlist_entry_buffer.flip();
		
		long linked_list_pointer = file_index_write_pointer.getAndAdd(LINKED_LIST_ENTRY_SIZE);
		
		return asyncWrite(index_channel, linkedlist_entry_buffer, linked_list_pointer).thenApply(s -> {
			return linked_list_pointer;
		});
	}
	
	/**
	 * @param onDone data_pointer or -1
	 */
	private CompletableFuture<Long> getDataPointerFromHashKey(byte[] key) {
		int compressed_key = compressKey(key);
		
		return readHashEntry(compressed_key).thenCompose(hash_entry -> {
			if (hash_entry == null) {
				return CompletableFuture.completedFuture(-1l);
			}
			return findInLinkedlistEntryData(hash_entry.linked_list_first_index, key);
		});
	}
	
	private CompletableFuture<Void> updateIndex(byte[] key, long data_pointer) {
		int compressed_key = compressKey(key);
		
		return readHashEntry(compressed_key).thenCompose(hash_entry -> {
			if (hash_entry == null) {
				/**
				 * Hash entry don't exists, create it and put in the hash table.
				 */
				return writeAppendLinkedlistEntry(new LinkedListEntry(key, data_pointer, -1)).thenCompose(new_linked_list_pointer -> {
					return writeHashEntry(new HashEntry(compressed_key, new_linked_list_pointer));
				});
			} else {
				/**
				 * Search if linked list entry exists
				 */
				return findLinkedlistEntry(hash_entry.linked_list_first_index, -1, key).thenCompose(ll_founded_entry -> {
					if (ll_founded_entry.not_found) {
						return CompletableFuture.completedFuture(null);
					} else if (ll_founded_entry.linked_list_pointer > -1 && ll_founded_entry.next_linked_list_pointer > -1) {
						/**
						 * Entry exists, replace current entry
						 **/
						return writeLinkedlistEntry(ll_founded_entry.linked_list_pointer, new LinkedListEntry(key, data_pointer, ll_founded_entry.next_linked_list_pointer));
					} else if (ll_founded_entry.linked_list_pointer_origin == -1l) {
						/**
						 * Add new linked list entry, and attach it for last entry. Overwrite current Hash entry.
						 */
						return writeAppendLinkedlistEntry(new LinkedListEntry(key, data_pointer, -1)).thenCompose(new_linked_list_pointer -> {
							return writeHashEntry(new HashEntry(compressed_key, new_linked_list_pointer));
						});
					} else {
						/**
						 * Append new entry to actual list (chain)
						 */
						return writeAppendLinkedlistEntry(new LinkedListEntry(key, data_pointer, -1)).thenCompose(new_linked_list_pointer -> {
							return writeNextLinkedlistEntry(ll_founded_entry.linked_list_pointer_origin, new_linked_list_pointer);
						});
					}
				});
			}
		});
	}
	
	public CompletableFuture<Void> put(ItemKey key, byte[] user_data) {// TODO interblocking !! use simple Queue for writes !
		return data.write(key.key, user_data).thenCompose(data_pointer -> {
			return updateIndex(key.key, data_pointer);
		});
	}
	
	/**
	 * @return entry can be null if not found.
	 */
	public CompletableFuture<Entry> getEntry(ItemKey key) {
		return getDataPointerFromHashKey(key.key).thenCompose(data_pointer -> {
			if (data_pointer == -1) {
				return CompletableFuture.completedFuture(null);
			} else {
				return data.read(data_pointer);
			}
		});
	}
	
	/**
	 * Internal datas will not removed. Only references.
	 */
	public CompletableFuture<Void> remove(ItemKey item_key) {
		byte[] key = item_key.key;
		int compressed_key = compressKey(key);
		
		return readHashEntry(compressed_key).thenCompose(hash_entry -> {
			if (hash_entry == null) {
				/**
				 * Can't found hash record: nothing to delete.
				 */
				return CompletableFuture.completedFuture(null);
			} else {
				return isLinkedlistEntryTargetKey(hash_entry.linked_list_first_index, key).thenCompose(ll_entry -> {
					if (ll_entry == null) {
						/**
						 * Empty list... Clear hashentry
						 */
						return writeHashEntry(new HashEntry(compressed_key, 0));
					} else if (ll_entry.next_linked_list_pointer == -1l) {
						/**
						 * First linkedlist record is not this key. Let's finds the next chain entries.
						 */
						return findLinkedlistEntry(ll_entry.next_linked_list_pointer, -1, key).thenCompose(ll_next_entry -> {
							if (ll_next_entry.linked_list_pointer_origin > -1) {
								/**
								 * Not in the list? Nothing to delete.
								 */
								return CompletableFuture.completedFuture(null);
							} else if (ll_next_entry.linked_list_pointer_origin > -1 && ll_next_entry.next_linked_list_pointer > -1) {
								/**
								 * Raccord in linkedlist chain the previous item and the next, and orphan this record.
								 */
								return writeNextLinkedlistEntry(ll_next_entry.linked_list_pointer_origin, ll_next_entry.next_linked_list_pointer);
							} else {
								/**
								 * This first linkedlist record is the key. Change it for the next.
								 */
								return writeHashEntry(new HashEntry(compressed_key, ll_next_entry.next_linked_list_pointer));
							}
						});
					} else {
						return CompletableFuture.completedFuture(null); // This is strange...
					}
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
	
	public CompletableFuture<Boolean> has(ItemKey key) {
		return getDataPointerFromHashKey(key.key).thenApply(data_pointer -> {
			return data_pointer != -1;
		});
	}
	
	public void clear() throws IOException, InterruptedException, ExecutionException {
		data.clear();
		
		file_index_write_pointer.set(start_linked_lists_zone_in_index_file);
		index_executor.getThreadPoolExecutor().getQueue().clear();
		index_channel.truncate(FILE_INDEX_HEADER_LENGTH);
		index_channel.force(true);
	}
	
}
