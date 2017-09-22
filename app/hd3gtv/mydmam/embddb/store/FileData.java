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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

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
	}
	
	/**
	 * @return data_pointer
	 */
	CompletableFuture<Long> write(byte[] key, byte[] user_data) {
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
		
		return FileHashTable.asyncWrite(data_channel, data_buffer, data_pointer).thenApply(size -> {
			return data_pointer;
		});
	}
	
	CompletableFuture<Entry> read(long data_pointer) {
		CompletableFuture<Entry> result = new CompletableFuture<>();
		
		/*
		<int, 4 bytes><key_size><--int, 4 bytes--->
		[ entry len  ][hash key][user's datas size][user's datas][suffix tag]
		 * */
		ByteBuffer header_buffer = ByteBuffer.allocate(4);
		
		FileHashTable.asyncRead(data_channel, header_buffer, data_pointer).exceptionally(e -> {
			result.completeExceptionally(e);
			return null;
		}).thenAccept(s -> {
			if (s != 4) {
				result.completeExceptionally(new IOException("Invalid read size: read = " + s));
				return;
			}
			header_buffer.flip();
			int data_entry_size = header_buffer.getInt();
			if (data_entry_size < 0) {
				result.completeExceptionally(new IOException("Invalid data size: data_entry_size = " + data_entry_size));
				return;
			}
			
			ByteBuffer data_buffer = ByteBuffer.allocate(data_entry_size);
			FileHashTable.asyncRead(data_channel, data_buffer, data_pointer + 4l).exceptionally(e -> {
				result.completeExceptionally(e);
				return null;
			}).thenAccept(s2 -> {
				if (s2 != data_entry_size) {
					result.completeExceptionally(new IOException("Invalid read size: read = " + s2));
					return;
				}
				data_buffer.flip();
				byte[] key = new byte[ItemKey.SIZE];
				data_buffer.get(key);
				
				byte[] data = null;
				int user_data_length = data_buffer.getInt();
				if (user_data_length < 0) {
					result.completeExceptionally(new IOException("Invalid read size: user_data_length = " + user_data_length));
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
					result.complete(new Entry(key, data));
				} catch (IOException e) {
					result.completeExceptionally(e);
				}
			});
		});
		
		return result;
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
	
	// TODO mark obsolete segments data file... + add more information in header and footer, like integrity control sum.
	// TODO deleted (free) zones index
	// TODO if overwrite a same/smaller length zone >> don't append, just overwrite
	// TODO if overwrite a same length zone, check integrity control sum before overwrite
	
}
