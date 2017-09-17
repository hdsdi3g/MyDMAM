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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.tools.ThreadPoolExecutorFactory;

public class FileHashTable {
	private static Logger log = Logger.getLogger(FileHashTable.class);
	
	private final int table_size;
	private final int key_size;
	private final File index_file;
	private final File data_file;
	private final AsynchronousFileChannel index_channel;
	private final AsynchronousFileChannel data_channel;
	
	private static final byte[] FILE_INDEX_HEADER = "MYDMAMHSHIDX".getBytes(MyDMAM.UTF8);
	private static final int FILE_INDEX_VERSION = 1;
	private static final byte[] FILE_DATA_HEADER = "MYDMAMHSHDTA".getBytes(MyDMAM.UTF8);
	private static final int FILE_DATA_VERSION = 1;
	private static final byte[] NEXT_TAG = "####".getBytes(MyDMAM.UTF8);
	
	private final long file_index_start;
	private final long file_data_start;
	
	private static final int HASH_ENTRY_SIZE = 12;
	private final int LINKED_LIST_ENTRY_SIZE;
	
	private AtomicLong file_index_write_pointer;
	private AtomicLong file_data_write_pointer;
	
	public FileHashTable(File index_file, File data_file, int table_size, int key_size) throws IOException, InterruptedException, ExecutionException {
		this.table_size = table_size;
		this.key_size = key_size;
		this.index_file = index_file;
		if (index_file == null) {
			throw new NullPointerException("\"index_file\" can't to be null");
		}
		this.data_file = data_file;
		if (data_file == null) {
			throw new NullPointerException("\"data_file\" can't to be null");
		}
		
		ThreadPoolExecutorFactory index_executor = new ThreadPoolExecutorFactory(getClass().getSimpleName() + "_Index", Thread.MAX_PRIORITY);
		ThreadPoolExecutorFactory data_executor = new ThreadPoolExecutorFactory(getClass().getSimpleName() + "_Datas", Thread.MAX_PRIORITY);
		
		Set<OpenOption> open_options_file_exists = new HashSet<OpenOption>(3);
		Collections.addAll(open_options_file_exists, StandardOpenOption.APPEND, StandardOpenOption.READ, StandardOpenOption.WRITE);
		Set<OpenOption> open_options_file_not_exists = new HashSet<OpenOption>(5);
		Collections.addAll(open_options_file_not_exists, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW, StandardOpenOption.SPARSE);
		
		ByteBuffer bytebuffer_header_index = ByteBuffer.allocate(FILE_INDEX_HEADER.length + 4 + 4 + 4);
		file_index_start = bytebuffer_header_index.capacity();
		
		long start_linked_lists_zone_in_index_file = file_index_start + ((long) table_size) * 12l;
		
		if (index_file.exists()) {
			index_channel = AsynchronousFileChannel.open(index_file.toPath(), open_options_file_exists, index_executor.getThreadPoolExecutor());
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
			if (actual_key_size != key_size) {
				throw new IOException("Invalid key_size: file is " + actual_key_size + " instead of " + key_size);
			}
			
			file_index_write_pointer = new AtomicLong(Long.max(index_channel.size(), start_linked_lists_zone_in_index_file));
		} else {
			index_channel = AsynchronousFileChannel.open(index_file.toPath(), open_options_file_not_exists, index_executor.getThreadPoolExecutor());
			bytebuffer_header_index.put(FILE_INDEX_HEADER);
			bytebuffer_header_index.putInt(FILE_INDEX_VERSION);
			bytebuffer_header_index.putInt(table_size);
			bytebuffer_header_index.putInt(key_size);
			bytebuffer_header_index.flip();
			index_channel.write(bytebuffer_header_index, 0).get();
			file_index_write_pointer = new AtomicLong(start_linked_lists_zone_in_index_file);
		}
		
		ByteBuffer bytebuffer_header_data = ByteBuffer.allocate(FILE_DATA_HEADER.length + 4);
		file_data_start = bytebuffer_header_data.capacity();
		
		if (data_file.exists()) {
			data_channel = AsynchronousFileChannel.open(data_file.toPath(), open_options_file_exists, data_executor.getThreadPoolExecutor());
			
			data_channel.read(bytebuffer_header_data, 0).get();
			bytebuffer_header_data.flip();
			
			TransactionJournal.readAndEquals(bytebuffer_header_data, FILE_DATA_HEADER, bad_datas -> {
				return new IOException("Invalid file header: " + new String(bad_datas));
			});
			int version = bytebuffer_header_data.getInt();
			if (version != FILE_DATA_VERSION) {
				throw new IOException("Invalid version: " + version + " instead of " + FILE_DATA_VERSION);
			}
		} else {
			data_channel = AsynchronousFileChannel.open(data_file.toPath(), open_options_file_not_exists, data_executor.getThreadPoolExecutor());
			bytebuffer_header_data.put(FILE_DATA_HEADER);
			bytebuffer_header_data.putInt(FILE_DATA_VERSION);
			bytebuffer_header_data.flip();
			data_channel.write(bytebuffer_header_data, 0).get();
		}
		file_data_write_pointer = new AtomicLong(data_channel.size());
		
		LINKED_LIST_ENTRY_SIZE = key_size + 16;
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
		return file_index_start + compressed_key * 12;
	}
	
	private int compressKey(byte[] key) {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(key);
		return Math.abs(result) % table_size;
	}
	
	public void put(byte[] key, byte[] user_data, Consumer<Throwable> onError, Consumer<byte[]> onDone) {
		if (key.length != key_size) {
			throw new IndexOutOfBoundsException("Key length (" + key.length + ") can't be different of this class key_size (" + key_size + ")");
		}
		int compressed_key = compressKey(key);
		long index_file_pos = computeIndexFilePosition(compressed_key);
		
		if (index_file_pos < index_file.length()) {
			/**
			 * Try to read before write
			 */
			// TODO READ, + UPDATE
			/*asyncRead(index_channel, key_table_buffer, index_file_pos, onError, size -> {
				if (size == 0) {
				}
			});*/
			onError.accept(new IOException("Not implemented: " + index_file_pos + ", " + index_file.length()));
		} else {
			/**
			 * New data, just write
			 */
			writeHashEntry(compressed_key, onError, linked_list_pointer -> {
				writeLinkedlistEntry(linked_list_pointer, key, user_data.length, -1, onError, data_pointer -> {
					writeData(data_pointer, key, user_data, onError, () -> {
						onDone.accept(key);
					});
				});
			});
		}
	}
	
	// TODO add bloom filter for all keys
	
	/**
	 * Just write a new entry
	 * Prepare entry and write it
	 */
	private void writeHashEntry(int compressed_key, Consumer<Throwable> onError, Consumer<Long> onDone) {
		/*
		Hash entry struct:
		<---int, 4 bytes----><----------------long, 8 bytes------------------->
		[Compressed hash key][absolute position for first index in linked list]
		*/
		ByteBuffer key_table_buffer = ByteBuffer.allocate(HASH_ENTRY_SIZE);
		long linked_list_pointer = file_index_write_pointer.getAndAdd(LINKED_LIST_ENTRY_SIZE);
		key_table_buffer.putInt(compressed_key);
		key_table_buffer.putLong(linked_list_pointer);
		key_table_buffer.flip();
		
		asyncWrite(index_channel, key_table_buffer, computeIndexFilePosition(compressed_key), onError, size -> {
			onDone.accept(linked_list_pointer);
		});
	}
	
	private void setNewLinkedlistTargetInHashEntry(int compressed_key, long new_next_linked_list_pointer, Consumer<Throwable> onError, Runnable onDone) {
		/*
		Hash entry struct:
		<---int, 4 bytes----><----------------long, 8 bytes------------------->
		[Compressed hash key][absolute position for first index in linked list]
		                     <---------------- UPDATE THIS ------------------->
		*/
		
		ByteBuffer key_table_buffer = ByteBuffer.allocate(8);
		key_table_buffer.putLong(new_next_linked_list_pointer);
		key_table_buffer.flip();
		
		asyncWrite(index_channel, key_table_buffer, computeIndexFilePosition(compressed_key) + 4, onError, s -> {
			onDone.run();
		});
	}
	
	/**
	 * Prepare first item in linkedlist and write it
	 */
	private void writeLinkedlistEntry(long linked_list_pointer, byte[] key, int user_data_len, long next_linked_list_pointer, Consumer<Throwable> onError, Consumer<Long> onDone) {
		/*
		Linked list entry struct:
		<key_size><----------------long, 8 bytes------------------><---------------long, 8 bytes------------------>
		[hash key][absolute position for user's datas in data file][absolute position for linked list's next index]
		*/
		ByteBuffer linkedlist_entry_buffer = ByteBuffer.allocate(LINKED_LIST_ENTRY_SIZE);
		linkedlist_entry_buffer.put(key);
		long data_pointer = file_data_write_pointer.getAndAdd(computeExactlyDataEntrySize(user_data_len));
		linkedlist_entry_buffer.putLong(data_pointer);
		linkedlist_entry_buffer.putLong(next_linked_list_pointer);
		linkedlist_entry_buffer.flip();
		
		asyncWrite(index_channel, linkedlist_entry_buffer, linked_list_pointer, onError, s -> {
			onDone.accept(data_pointer);
		});
	}
	
	private void setNextLinkedlistEntry(long linked_list_pointer, long new_next_linked_list_pointer, Consumer<Throwable> onError, Runnable onDone) {
		/*
		Linked list entry struct:
		<key_size><----------------long, 8 bytes------------------><---------------long, 8 bytes------------------>
		[hash key][absolute position for user's datas in data file][absolute position for linked list's next index]
		                                                           <--------------- UPDATE THIS ------------------>
		*/
		ByteBuffer linkedlist_entry_buffer = ByteBuffer.allocate(8);
		linkedlist_entry_buffer.putLong(new_next_linked_list_pointer);
		linkedlist_entry_buffer.flip();
		
		asyncWrite(index_channel, linkedlist_entry_buffer, linked_list_pointer + key_size + 8, onError, s -> {
			onDone.run();
		});
	}
	
	/**
	 * Prepare data entry and write it
	 */
	private void writeData(long data_pointer, byte[] key, byte[] user_data, Consumer<Throwable> onError, Runnable onDone) {
		/*
		<int, 4 bytes><key_size><--int, 4 bytes--->
		[ entry len  ][hash key][user's datas size][user's datas][suffix tag]
		 * */
		int data_entry_size = computeExactlyDataEntrySize(user_data.length);
		ByteBuffer data_buffer = ByteBuffer.allocate(data_entry_size);
		data_buffer.putInt(data_entry_size - 4);
		data_buffer.put(key);
		data_buffer.putInt(user_data.length);
		data_buffer.put(user_data);
		data_buffer.put(NEXT_TAG);
		data_buffer.flip();
		asyncWrite(data_channel, data_buffer, data_pointer, onError, s3 -> {
			onDone.run();
		});
	}
	
	private int computeExactlyDataEntrySize(int user_data_len) {
		/*
		<int, 4 bytes><key_size><--int, 4 bytes--->
		[ entry len  ][hash key][user's datas size][user's datas][suffix tag]
		* */
		return 4 + key_size + 4 + user_data_len + NEXT_TAG.length;
	}
	
	private static void asyncRead(AsynchronousFileChannel channel, ByteBuffer buffer, long position, Consumer<Throwable> onError, Consumer<Integer> completed) {
		channel.read(buffer, position, null, new CompletionHandler<Integer, Void>() {
			
			public void completed(Integer result, Void attachment) {
				completed.accept(result);
			}
			
			public void failed(Throwable e, Void attachment) {
				onError.accept(e);
			}
		});
	}
	
	private static void asyncWrite(AsynchronousFileChannel channel, ByteBuffer buffer, long position, Consumer<Throwable> onError, Consumer<Integer> completed) {
		channel.write(buffer, position, null, new CompletionHandler<Integer, Void>() {
			
			public void completed(Integer result, Void attachment) {
				completed.accept(result);
			}
			
			public void failed(Throwable e, Void attachment) {
				onError.accept(e);
			}
		});
	}
	
	/*class DeCote {
		public int size() {
			return 0;
		}
		
		public boolean isEmpty() {
			return false;
		}
		
		public boolean removeAll(Collection<?> c) {
			return false;
		}
		
		public void clear() {
		}
		
		public boolean containsKey(Object key) {
			return false;
		}
		
		public byte[] get(Object key) {
			return null;
		}
		
		public byte[] put(ItemKey key, byte[] value) {
			return null;
		}
		
		public void putAll(Map<? extends ItemKey, ? extends byte[]> m) {
		}
		
		public Set<ItemKey> keySet() {
			return null;
		}
		
		public Collection<byte[]> values() {
			return null;
		}
		
		public Set<java.util.Map.Entry<ItemKey, byte[]>> entrySet() {
			return null;
		}
		
		public byte[] remove(Object key) {
			return null;
		}
	}*/
	
	// TODO behavior with corruped files (bad writes) ?
}
