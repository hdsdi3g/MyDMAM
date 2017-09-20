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
 * Copyright (C) hdsdi3g for hd3g.tv 20 sept. 2017
 * 
*/
package hd3gtv.mydmam.embddb.store;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.log4j.Logger;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.tools.ThreadPoolExecutorFactory;

/**
 * Store raw datas sequentially in a bin File. Without index !
 */
class FileData {
	private static Logger log = Logger.getLogger(FileData.class);
	
	private static final byte[] FILE_DATA_HEADER = "MYDMAMHSHDTA".getBytes(MyDMAM.UTF8);
	private static final int FILE_DATA_VERSION = 1;
	private static final byte[] NEXT_TAG = "####".getBytes(MyDMAM.UTF8);
	private static final int FILE_DATA_HEADER_LENGTH = FILE_DATA_HEADER.length + 4;
	
	private final AsynchronousFileChannel data_channel;
	private AtomicLong file_data_write_pointer;
	// final File data_file;
	private final long file_data_start;
	private final ThreadPoolExecutorFactory data_executor;
	final Predicate<Long> checkDataPointerValidity;
	final Function<Long, Entry> getDataEntryFromDataPointer;
	
	FileData(File data_file) throws IOException, InterruptedException, ExecutionException {
		// this.data_file = data_file;
		if (data_file == null) {
			throw new NullPointerException("\"data_file\" can't to be null");
		}
		
		data_executor = new ThreadPoolExecutorFactory(FileHashTable.class.getName() + "/" + getClass().getSimpleName(), Thread.MAX_PRIORITY);
		
		ByteBuffer bytebuffer_header_data = ByteBuffer.allocate(FILE_DATA_HEADER_LENGTH);
		file_data_start = bytebuffer_header_data.capacity();
		
		if (data_file.exists()) {
			data_channel = AsynchronousFileChannel.open(data_file.toPath(), FileHashTable.OPEN_OPTIONS_FILE_EXISTS, data_executor.getThreadPoolExecutor());
			
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
			data_channel = AsynchronousFileChannel.open(data_file.toPath(), FileHashTable.OPEN_OPTIONS_FILE_NOT_EXISTS, data_executor.getThreadPoolExecutor());
			bytebuffer_header_data.put(FILE_DATA_HEADER);
			bytebuffer_header_data.putInt(FILE_DATA_VERSION);
			bytebuffer_header_data.flip();
			data_channel.write(bytebuffer_header_data, 0).get();
		}
		file_data_write_pointer = new AtomicLong(data_channel.size());
		
		checkDataPointerValidity = data_pointer -> {
			return (data_pointer > file_data_start) & (data_pointer < data_file.length());
		};
		
		getDataEntryFromDataPointer = data_pointer -> {
			try {
				return readData(data_pointer);
			} catch (InterruptedException | ExecutionException | IOException e) {
				throw new RuntimeException(e);
			}
		};
	}
	
	/**
	 * Prepare data entry and write it
	 * @param onDone the new data pointer.
	 */
	void writeData(byte[] key, byte[] user_data, Consumer<Throwable> onError, Consumer<Long> onDone) {
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
		
		long data_pointer = file_data_write_pointer.getAndAdd(computeExactlyDataEntrySize(user_data.length));
		
		FileHashTable.asyncWrite(data_channel, data_buffer, data_pointer, onError, s3 -> {
			onDone.accept(data_pointer);
		});
	}
	
	/**
	 * @param onDone key->data or null -> new byte[0] if not found datas
	 */
	void readData(long data_pointer, Consumer<Throwable> onError, BiConsumer<byte[], byte[]> onDone) {
		/*
		<int, 4 bytes><key_size><--int, 4 bytes--->
		[ entry len  ][hash key][user's datas size][user's datas][suffix tag]
		 * */
		ByteBuffer header_buffer = ByteBuffer.allocate(4);
		
		FileHashTable.asyncRead(data_channel, header_buffer, data_pointer, onError, s -> {
			if (s != 4) {
				onDone.accept(null, new byte[0]);
				return;
			}
			header_buffer.flip();
			int data_entry_size = header_buffer.getInt();
			if (data_entry_size < 0) {
				onDone.accept(null, new byte[0]);
				return;
			}
			ByteBuffer data_buffer = ByteBuffer.allocate(data_entry_size);
			FileHashTable.asyncRead(data_channel, data_buffer, data_pointer + 4l, onError, s2 -> {
				if (s2 != data_entry_size) {
					onDone.accept(null, new byte[0]);
					return;
				}
				data_buffer.flip();
				byte[] key = new byte[ItemKey.SIZE];
				data_buffer.get(key);
				
				byte[] data = null;
				int user_data_length = data_buffer.getInt();
				
				if (user_data_length < 0) {
					onDone.accept(null, new byte[0]);
					return;
				} else if (user_data_length == 0) {
					data = new byte[0];
				} else {
					data = new byte[user_data_length];
					data_buffer.get(data);
				}
				
				try {
					TransactionJournal.readAndEquals(data_buffer, NEXT_TAG, err -> {
						return new IOException("Bad tag separator: " + new String(err, MyDMAM.UTF8));
					});
					onDone.accept(key, data);
				} catch (IOException e) {
					onError.accept(e);
				}
			});
		});
	}
	
	/**
	 * Sync.
	 * @return can be null
	 */
	Entry readData(long data_pointer) throws InterruptedException, ExecutionException, IOException {
		/*
		<int, 4 bytes><key_size><--int, 4 bytes--->
		[ entry len  ][hash key][user's datas size][user's datas][suffix tag]
		 * */
		ByteBuffer header_buffer = ByteBuffer.allocate(4);
		int size = data_channel.read(header_buffer, data_pointer).get();
		if (size != 4) {
			throw new IOException("Invalid header size: " + size);
		}
		header_buffer.flip();
		int data_entry_size = header_buffer.getInt();
		if (data_entry_size < 0) {
			throw new IOException("Invalid data_entry_size: " + data_entry_size);
		}
		
		ByteBuffer data_buffer = ByteBuffer.allocate(data_entry_size);
		
		size = data_channel.read(data_buffer, data_pointer + 4l).get();
		if (size != data_entry_size) {
			throw new IOException("Can't read datas: " + size + "/" + data_entry_size);
		}
		data_buffer.flip();
		byte[] key = new byte[ItemKey.SIZE];
		data_buffer.get(key);
		
		byte[] data = null;
		int user_data_length = data_buffer.getInt();
		if (user_data_length < 0) {
			throw new IOException("Invalid user data len: " + user_data_length);
		} else if (user_data_length == 0) {
			data = new byte[0];
		} else {
			data = new byte[user_data_length];
			data_buffer.get(data);
		}
		
		TransactionJournal.readAndEquals(data_buffer, NEXT_TAG, err -> {
			return new IOException("Bad tag separator: " + new String(err, MyDMAM.UTF8));
		});
		return new Entry(key, data);
	}
	
	public class Entry {
		public final ItemKey key;
		public final byte[] value;
		
		private Entry(byte[] key, byte[] value) {
			this.key = new ItemKey(key);
			this.value = value;
		}
	}
	
	private int computeExactlyDataEntrySize(int user_data_len) {
		/*
		<int, 4 bytes><key_size><--int, 4 bytes--->
		[ entry len  ][hash key][user's datas size][user's datas][suffix tag]
		* */
		return 4 + ItemKey.SIZE + 4 + user_data_len + NEXT_TAG.length;
	}
	
	void clear() throws IOException {
		file_data_write_pointer.set(FILE_DATA_HEADER_LENGTH);
		data_executor.getThreadPoolExecutor().getQueue().clear();
		data_channel.truncate(FILE_DATA_HEADER_LENGTH);
		data_channel.force(true);
	}
	
	// TODO mark obsolete segments data file...
	
}
