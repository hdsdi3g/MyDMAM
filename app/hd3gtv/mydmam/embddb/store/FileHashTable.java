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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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
	private final ThreadPoolExecutorFactory write_executor;
	
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
		
		write_executor = new ThreadPoolExecutorFactory(getClass().getSimpleName() + "_Writer", Thread.MAX_PRIORITY - 1);
		write_executor.setSimplePoolSize();
		
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
	
	static CompletableFuture<Integer> asyncRead(AsynchronousFileChannel channel, ByteBuffer buffer, long position, String read_what) {
		CompletableFuture<Integer> completable_future = new CompletableFuture<>();
		
		channel.read(buffer, position, position, new CompletionHandler<Integer, Long>() {
			
			public void completed(Integer result, Long position) {
				if (log.isTraceEnabled()) {
					log.trace("Async read done (" + read_what + "): " + result + " bytes from " + position);
				}
				completable_future.complete(result);
			}
			
			public void failed(Throwable e, Long position) {
				if (log.isDebugEnabled()) {
					log.debug("Async read fail (" + read_what + "): in " + position, e);
				}
				completable_future.completeExceptionally(e);
			}
		});
		return completable_future;
	}
	
	/**
	 * @param buffer full buffer write is tested
	 */
	static CompletableFuture<Integer> asyncWrite(AsynchronousFileChannel channel, ByteBuffer buffer, long position, String write_what) {
		CompletableFuture<Integer> completable_future = new CompletableFuture<>();
		int rem = buffer.remaining();
		
		channel.write(buffer, position, position, new CompletionHandler<Integer, Long>() {
			
			public void completed(Integer writed_size, Long position) {
				if (rem != writed_size) {
					if (log.isDebugEnabled()) {
						log.debug("Async write fail (" + write_what + "): in " + position);
					}
					completable_future.completeExceptionally(new IOException("Can't write all datas: " + writed_size + "/" + rem));
				} else {
					if (log.isTraceEnabled()) {
						log.trace("Async write done (" + write_what + "): " + writed_size + " bytes from " + position);
					}
					completable_future.complete(writed_size);
				}
			}
			
			public void failed(Throwable e, Long position) {
				if (log.isDebugEnabled()) {
					log.debug("Async write fail (" + write_what + "): in " + position, e);
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
	
	class HashEntry {// XXX private
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
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("compressed_hash_key=" + compressed_hash_key + ",");
			sb.append("linked_list_first_index=" + linked_list_first_index);
			return sb.toString();
		}
	}
	
	private class LinkedListEntry {
		byte[] current_key;
		long data_pointer;
		long next_linked_list_pointer;
		
		final long linked_list_pointer;
		
		LinkedListEntry(byte[] current_key, long data_pointer, long next_linked_list_pointer) {
			this.current_key = current_key;
			this.data_pointer = data_pointer;
			this.next_linked_list_pointer = next_linked_list_pointer;
			linked_list_pointer = -1;
		}
		
		/*
		Linked list entry struct:
		<key_size><----------------long, 8 bytes------------------><---------------long, 8 bytes------------------>
		[hash key][absolute position for user's datas in data file][absolute position for linked list's next index]
		*/
		LinkedListEntry(long linked_list_pointer, ByteBuffer linkedlist_entry_buffer) {
			this.linked_list_pointer = linked_list_pointer;
			if (linked_list_pointer < 1l) {
				throw new NullPointerException("\"linked_list_pointer\" can't to be < 1 (" + linkedlist_entry_buffer + ")");
			}
			
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
				log.trace("Prepare LinkedListEntry write: current_key = #" + MyDMAM.byteToString(current_key).substring(0, 8) + ", data_pointer = " + data_pointer + ", next_linked_list_pointer = " + next_linked_list_pointer);
			}
			write_buffer.put(current_key);
			write_buffer.putLong(data_pointer);
			write_buffer.putLong(next_linked_list_pointer);
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("current_key=#" + MyDMAM.byteToString(current_key).substring(0, 8) + ",");
			sb.append("data_pointer=" + data_pointer + ",");
			sb.append("next_linked_list_pointer=" + next_linked_list_pointer);
			return sb.toString();
		}
		
		void clear() {
			data_pointer = 0;
			next_linked_list_pointer = 0;
			for (int i = 0; i < current_key.length; i++) {
				current_key[i] = 0;
			}
		}
		
	}
	
	private CompletableFuture<Stream<LinkedListEntry>> getAllLinkedListItemsForHashEntry(HashEntry entry) {
		CompletableFuture<Stream<LinkedListEntry>> result = new CompletableFuture<>();
		AtomicReference<Long> next_pointer = new AtomicReference<>(entry.linked_list_first_index);
		
		result.complete(StreamMaker.create(() -> {
			if (next_pointer.get() <= 0) {
				return null;
			}
			ByteBuffer linkedlist_entry_buffer = ByteBuffer.allocate(LINKED_LIST_ENTRY_SIZE);
			try {
				final long linked_list_pointer = next_pointer.getAndSet(0l);
				return asyncRead(index_channel, linkedlist_entry_buffer, linked_list_pointer, "for each linkedlist entries").exceptionally(e -> {
					result.completeExceptionally(e);
					return null;
				}).thenApply(s -> {
					if (s != LINKED_LIST_ENTRY_SIZE) {
						return null;
					}
					linkedlist_entry_buffer.flip();
					LinkedListEntry r = new LinkedListEntry(linked_list_pointer, linkedlist_entry_buffer);
					linkedlist_entry_buffer.clear();
					next_pointer.set(r.next_linked_list_pointer);
					return r;
				}).get();
			} catch (InterruptedException | ExecutionException e) {
				result.completeExceptionally(e);
				return null;
			}
		}).stream());
		
		return result;
	}
	
	/**
	 * @return fully sync
	 */
	private CompletableFuture<List<LinkedListEntry>> getAllLinkedListItems() {
		ByteBuffer read_buffer = ByteBuffer.allocate(HASH_ENTRY_SIZE * table_size);
		
		CompletableFuture<List<LinkedListEntry>> result = new CompletableFuture<>();
		
		try {
			int size = asyncRead(index_channel, read_buffer, file_index_start, "all hash table").get();
			
			if (size < 1) {
				return CompletableFuture.completedFuture(Collections.emptyList());
			}
			read_buffer.flip();
			if (read_buffer.capacity() != size) {
				return CompletableFuture.completedFuture(Collections.emptyList());
			}
			
			ArrayList<LinkedListEntry> entries = new ArrayList<>();
			
			while (read_buffer.remaining() - HASH_ENTRY_SIZE >= 0) {
				HashEntry hash_entry = new HashEntry(read_buffer);
				if (hash_entry.linked_list_first_index < 1) {
					continue;
				}
				
				entries.addAll(getAllLinkedListItemsForHashEntry(hash_entry).get().collect(Collectors.toList()));
			}
			result.complete(entries);
		} catch (InterruptedException | ExecutionException e) {
			result.completeExceptionally(e);
		}
		
		return result;
	}
	
	public CompletableFuture<Stream<ItemKey>> forEachKeys() {
		return getAllLinkedListItems().thenApply(lle_stream -> {
			return lle_stream.stream().map(lle -> {
				return new ItemKey(lle.current_key);
			});
		});
	}
	
	public CompletableFuture<Stream<CompletableFuture<Entry>>> forEachKeyValue() {
		return getAllLinkedListItems().thenApply(lle_stream -> {
			return lle_stream.stream().map(lle -> {
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
	private CompletableFuture<Void> writeHashEntry(HashEntry hash_entry) {
		ByteBuffer key_table_buffer = ByteBuffer.allocate(HASH_ENTRY_SIZE);
		hash_entry.toByteBuffer(key_table_buffer);
		key_table_buffer.flip();
		
		if (log.isTraceEnabled()) {
			log.trace("Write hash_entry " + hash_entry);
		}
		
		return asyncWrite(index_channel, key_table_buffer, computeIndexFilePosition(hash_entry.compressed_hash_key), "hash entry").thenApplyAsync(s -> {
			return null;
		}, write_executor.getThreadPoolExecutor());
	}
	
	/**
	 * @param onDone the Absolute position for the first index in linked list, or null
	 */
	private CompletableFuture<HashEntry> readHashEntry(int compressed_key) {
		long index_file_pos = computeIndexFilePosition(compressed_key);
		if (index_file_pos > index_file.length()) {
			if (log.isTraceEnabled()) {
				log.trace("Can't found hash entry (EOF), compress key=" + compressed_key);
			}
			return CompletableFuture.completedFuture(null);
		}
		
		ByteBuffer key_table_buffer = ByteBuffer.allocate(HASH_ENTRY_SIZE);
		return asyncRead(index_channel, key_table_buffer, index_file_pos, "hash entry").thenApply(size -> {
			if (size != HASH_ENTRY_SIZE) {
				if (log.isTraceEnabled()) {
					log.trace("Invalid hash entry size (" + size + " bytes(s)), compress key=" + compressed_key);
				}
				return null;
			}
			key_table_buffer.flip();
			int real_compress_key = key_table_buffer.getInt();
			if (compressed_key != real_compress_key) {
				if (log.isTraceEnabled()) {
					log.trace("Invalid hash entry header (real compress key=" + real_compress_key + "), compress key=" + compressed_key);
				}
				return null;
			}
			long result = key_table_buffer.getLong();
			if (result == 0) {
				if (log.isTraceEnabled()) {
					log.trace("Empty value for hash entry, compress key=" + compressed_key);
				}
				return null;
			}
			return new HashEntry(compressed_key, result);
		});
	}
	
	private CompletableFuture<Void> writeLinkedlistEntry(long linked_list_pointer, LinkedListEntry linked_list_entry) {
		ByteBuffer linkedlist_entry_buffer = ByteBuffer.allocate(LINKED_LIST_ENTRY_SIZE);
		linked_list_entry.toByteBuffer(linkedlist_entry_buffer);
		linkedlist_entry_buffer.flip();
		
		if (log.isTraceEnabled()) {
			log.trace("Write linked_list_entry " + linked_list_entry + " in " + linked_list_pointer);
		}
		
		return asyncWrite(index_channel, linkedlist_entry_buffer, linked_list_pointer, "Linked list entry").thenAcceptAsync(s -> {
		}, write_executor.getThreadPoolExecutor());
	}
	
	/**
	 * @param onDone created item position
	 */
	private CompletableFuture<Long> writeNewLinkedlistEntry(LinkedListEntry ll_entry) {
		/*
		Linked list entry struct:
		<key_size><----------------long, 8 bytes------------------><---------------long, 8 bytes--------------------->
		[hash key][absolute position for user's datas in data file][absolute position for linked list's next index: 0]
		*/
		ByteBuffer linkedlist_entry_buffer = ByteBuffer.allocate(LINKED_LIST_ENTRY_SIZE);
		ll_entry.toByteBuffer(linkedlist_entry_buffer);
		linkedlist_entry_buffer.flip();
		
		long linked_list_pointer = file_index_write_pointer.getAndAdd(LINKED_LIST_ENTRY_SIZE);
		
		if (log.isTraceEnabled()) {
			log.trace("Add new linked_list_entry " + ll_entry + " in " + linked_list_pointer);
		}
		
		return asyncWrite(index_channel, linkedlist_entry_buffer, linked_list_pointer, "add new linked list entry").thenApplyAsync(s -> {
			return linked_list_pointer;
		}, write_executor.getThreadPoolExecutor());
	}
	
	/**
	 * @param onDone data_pointer or -1
	 */
	private CompletableFuture<Long> getDataPointerFromHashKey(byte[] key) {
		int compressed_key = compressKey(key);
		
		return readHashEntry(compressed_key).thenCompose(hash_entry -> {
			if (hash_entry == null) {
				if (log.isTraceEnabled()) {
					log.trace("Can't found hash key " + MyDMAM.byteToString(key));
				}
				return CompletableFuture.completedFuture(-1l);
			}
			
			return getAllLinkedListItemsForHashEntry(hash_entry).thenApply(linked_list_items -> {
				Optional<LinkedListEntry> o_linked_list_item = linked_list_items.filter(linked_list_item -> {
					return Arrays.equals(key, linked_list_item.current_key);
				}).findFirst();
				
				if (o_linked_list_item.isPresent() == false) {
					return -1l;
				} else {
					return o_linked_list_item.get().data_pointer;
				}
			});
		});
	}
	
	public CompletableFuture<Void> put(ItemKey item_key, byte[] user_data) {
		byte[] key = item_key.key;
		
		return data.write(key, user_data).thenComposeAsync(data_pointer -> {
			int compressed_key = compressKey(key);
			
			return readHashEntry(compressed_key).thenComposeAsync(hash_entry -> {
				if (hash_entry == null) {
					if (log.isTraceEnabled()) {
						log.trace("Hash entry (compressed_key=" + compressed_key + ") don't exists, create it and put in the hash table");
					}
					return writeNewLinkedlistEntry(new LinkedListEntry(key, data_pointer, -1)).thenComposeAsync(new_linked_list_pointer -> {
						return writeHashEntry(new HashEntry(compressed_key, new_linked_list_pointer));
					}, write_executor.getThreadPoolExecutor());
				} else {
					if (log.isTraceEnabled()) {
						log.trace("Search if linked list entry exists for key #" + MyDMAM.byteToString(key).substring(0, 12) + " in compressed_key=" + compressed_key);
					}
					return getAllLinkedListItemsForHashEntry(hash_entry).thenComposeAsync(linked_list_items -> {
						Optional<LinkedListEntry> o_linked_list_item = linked_list_items.filter(linked_list_item -> {
							return Arrays.equals(key, linked_list_item.current_key);
						}).findFirst();
						
						if (o_linked_list_item.isPresent()) {
							if (log.isTraceEnabled()) {
								log.trace("Entry exists, replace current entry for #" + MyDMAM.byteToString(key).substring(0, 12) + " in compressed_key=" + compressed_key + " with new data_pointer=" + data_pointer);
							}
							LinkedListEntry linked_list_entry = o_linked_list_item.get();
							linked_list_entry.data_pointer = data_pointer;
							return writeLinkedlistEntry(linked_list_entry.linked_list_pointer, linked_list_entry);
						} else {
							if (log.isTraceEnabled()) {
								log.trace("Append new entry to actual list (chain) for #" + MyDMAM.byteToString(key).substring(0, 12) + " in compressed_key=" + compressed_key + " with new data_pointer=" + data_pointer);
							}
							return writeNewLinkedlistEntry(new LinkedListEntry(key, data_pointer, -1)).thenComposeAsync(new_linked_list_pointer -> {
								return writeHashEntry(new HashEntry(compressed_key, new_linked_list_pointer));
							}, write_executor.getThreadPoolExecutor());
						}
					}, write_executor.getThreadPoolExecutor());
				}
			}, write_executor.getThreadPoolExecutor());
		}, write_executor.getThreadPoolExecutor());
	}
	
	/**
	 * @return entry can be null if not found.
	 */
	public CompletableFuture<Entry> getEntry(ItemKey key) {
		return getDataPointerFromHashKey(key.key).thenCompose(data_pointer -> {
			if (data_pointer == -1) {
				if (log.isTraceEnabled()) {
					log.trace("Can't found entry for " + MyDMAM.byteToString(key.key));
				}
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
		final Predicate<LinkedListEntry> isThisSearchedItem = linked_list_item -> {
			return Arrays.equals(key, linked_list_item.current_key);
		};
		
		return readHashEntry(compressed_key).thenComposeAsync(hash_entry -> {
			if (hash_entry == null) {
				/**
				 * Can't found hash record: nothing to delete.
				 */
				if (log.isTraceEnabled()) {
					log.trace("Can't found hash key (compress key=" + compressed_key + ") for " + MyDMAM.byteToString(key));
				}
				return CompletableFuture.completedFuture(null);
			} else {
				return getAllLinkedListItemsForHashEntry(hash_entry).thenComposeAsync(linked_list_items -> {
					List<LinkedListEntry> hash_entry_linked_list = StreamMaker.takeUntilTrigger(isThisSearchedItem, linked_list_items).collect(Collectors.toList());
					if (hash_entry_linked_list.isEmpty()) {
						/**
						 * Nothing to remove: empty list...
						 */
						return CompletableFuture.completedFuture(null);
					}
					LinkedListEntry last_linked_list_item_to_remove = hash_entry_linked_list.get(hash_entry_linked_list.size() - 1);
					
					if (isThisSearchedItem.test(last_linked_list_item_to_remove) == false) {
						/**
						 * Item is not present to hash_list... so, nothing to remove.
						 */
						return CompletableFuture.completedFuture(null);
					}
					
					if (hash_entry.linked_list_first_index != hash_entry_linked_list.get(0).linked_list_pointer) {
						CompletableFuture<Void> error = new CompletableFuture<Void>();
						error.completeExceptionally(new IOException("Invalid hashtable structure for " + compressed_key + " (" + hash_entry.linked_list_first_index + ", " + hash_entry_linked_list.get(0).linked_list_pointer));
						return error;
					}
					
					// last_linked_list_item_to_remove.data_pointer //TODO mark as "delete" data
					long next_valid_linked_list_pointer = last_linked_list_item_to_remove.linked_list_pointer;
					
					/**
					 * Clear the actual
					 */
					last_linked_list_item_to_remove.clear();
					return writeLinkedlistEntry(last_linked_list_item_to_remove.linked_list_pointer, last_linked_list_item_to_remove).thenComposeAsync(b -> {
						
						if (hash_entry_linked_list.size() == 1) {
							/**
							 * {55:A}[A>B][B>-1], remove [A]: {55:B}-----[B>-1]
							 * change hash_entry first target == me.next.target
							 */
							hash_entry.linked_list_first_index = next_valid_linked_list_pointer;
							return writeHashEntry(hash_entry);
						} else {
							/**
							 * [A>B][B>C][C>-1], remove [B]: [A>C]-----[C>-1]
							 * change the me.previous.next_target == me.next.target
							 */
							LinkedListEntry last_valid_linked_list_item = hash_entry_linked_list.get(hash_entry_linked_list.size() - 2);
							last_valid_linked_list_item.next_linked_list_pointer = next_valid_linked_list_pointer;
							return writeLinkedlistEntry(last_valid_linked_list_item.linked_list_pointer, last_valid_linked_list_item);
						}
					}, write_executor.getThreadPoolExecutor());
				}, write_executor.getThreadPoolExecutor());
			}
		}, write_executor.getThreadPoolExecutor());
	}
	
	public CompletableFuture<Integer> size() {
		return getAllLinkedListItems().thenApply(entries -> {
			return (int) entries.size();
		});
	}
	
	public CompletableFuture<Boolean> has(ItemKey key) {
		return getDataPointerFromHashKey(key.key).thenApply(data_pointer -> {
			return data_pointer != -1;
		});
	}
	
	public void clear() throws IOException {
		write_executor.getThreadPoolExecutor().getQueue().clear();
		data.clear();
		
		log.info("Clear " + index_file);
		file_index_write_pointer.set(start_linked_lists_zone_in_index_file);
		index_executor.getThreadPoolExecutor().getQueue().clear();
		write_executor.getThreadPoolExecutor().getQueue().clear();
		index_channel.truncate(FILE_INDEX_HEADER_LENGTH);
		index_channel.force(true);
	}
	
}
