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
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;

import hd3gtv.mydmam.MyDMAM;

/**
 * Store raw datas sequentially in a bin File. Without index !
 * No internal sync, no internal lock. NOT THREAD SAFE. Don't support properly parallel writing.
 */
class FileData {
	private static Logger log = Logger.getLogger(FileData.class);
	
	private static final byte[] FILE_DATA_HEADER = "MYDMAMHSHDTA".getBytes(MyDMAM.UTF8);
	private static final int FILE_DATA_VERSION = 1;
	private static final byte[] NEXT_TAG = "####".getBytes(MyDMAM.UTF8);
	private static final int FILE_DATA_HEADER_LENGTH = FILE_DATA_HEADER.length + 4;
	
	private volatile long file_data_write_pointer;
	private final File data_file;
	private final FileChannel channel;
	// private final long file_data_start;
	
	FileData(File data_file) throws IOException, InterruptedException, ExecutionException {
		this.data_file = data_file;
		if (data_file == null) {
			throw new NullPointerException("\"data_file\" can't to be null");
		}
		
		ByteBuffer bytebuffer_header_data = ByteBuffer.allocate(FILE_DATA_HEADER_LENGTH);
		// file_data_start = bytebuffer_header_data.capacity();
		
		if (data_file.exists()) {
			channel = FileChannel.open(data_file.toPath(), FileHashTable.OPEN_OPTIONS_FILE_EXISTS);
			int size = channel.read(bytebuffer_header_data, 0);
			if (size != FILE_DATA_HEADER_LENGTH) {
				throw new IOException("Invalid header");
			}
			bytebuffer_header_data.flip();
			
			TransactionJournal.readAndEquals(bytebuffer_header_data, FILE_DATA_HEADER, bad_datas -> {
				return new IOException("Invalid file header: " + new String(bad_datas));
			});
			int version = bytebuffer_header_data.getInt();
			if (version != FILE_DATA_VERSION) {
				throw new IOException("Invalid version: " + version + " instead of " + FILE_DATA_VERSION);
			}
		} else {
			channel = FileChannel.open(data_file.toPath(), FileHashTable.OPEN_OPTIONS_FILE_NOT_EXISTS);
			bytebuffer_header_data.put(FILE_DATA_HEADER);
			bytebuffer_header_data.putInt(FILE_DATA_VERSION);
			bytebuffer_header_data.flip();
			channel.write(bytebuffer_header_data, 0);
		}
		file_data_write_pointer = channel.size();
	}
	
	/**
	 * @return new data_pointer
	 */
	long write(byte[] key, byte[] user_data) throws IOException {
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
		
		long data_pointer = file_data_write_pointer;
		file_data_write_pointer += computeExactlyDataEntrySize(user_data.length);
		
		if (log.isTraceEnabled()) {
			log.trace("Prepare to write datas: key = " + MyDMAM.byteToString(key) + ", " + data_entry_size + " bytes from " + data_pointer);
		}
		
		channel.write(data_buffer, data_pointer);
		return data_pointer;
	}
	
	Entry read(long data_pointer) throws IOException {
		/*
		<int, 4 bytes><key_size><--int, 4 bytes--->
		[ entry len  ][hash key][user's datas size][user's datas][suffix tag]
		 * */
		ByteBuffer header_buffer = ByteBuffer.allocate(4);
		
		if (log.isTraceEnabled()) {
			log.trace("Prepare to read data header from " + data_pointer);
		}
		
		int s = channel.read(header_buffer, data_pointer);
		if (s != 4) {
			throw new IOException("Invalid read size: read = " + s);
		}
		header_buffer.flip();
		int data_entry_size = header_buffer.getInt();
		if (data_entry_size < 0) {
			throw new IOException("Invalid data size: data_entry_size = " + data_entry_size);
		}
		
		ByteBuffer data_buffer = ByteBuffer.allocate(data_entry_size);
		
		if (log.isTraceEnabled()) {
			log.trace("Prepare to read full data content " + data_entry_size + " bytes from " + (data_pointer + 4l));
		}
		
		int s2 = channel.read(data_buffer, data_pointer + 4l);
		if (s2 != data_entry_size) {
			throw new IOException("Invalid read size: read = " + s2);
		}
		data_buffer.flip();
		byte[] key = new byte[ItemKey.SIZE];
		data_buffer.get(key);
		
		byte[] data = null;
		int user_data_length = data_buffer.getInt();
		if (user_data_length < 0) {
			throw new IOException("Invalid read size: user_data_length = " + user_data_length);
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
			return new Entry(key, data);
		} catch (IOException e) {
			throw e;
		}
	}
	
	public class Entry {
		public final ItemKey key;
		public final byte[] value;
		
		private Entry(byte[] key, byte[] value) {
			this.key = new ItemKey(key);
			this.value = value;
		}
		
		public String toString() {
			return key + " > " + MyDMAM.byteToString(value);
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
		log.info("Clear " + data_file);
		file_data_write_pointer = FILE_DATA_HEADER_LENGTH;
		try {
			channel.truncate(FILE_DATA_HEADER_LENGTH);
			channel.force(true);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	// TODO mark obsolete segments data file... + add more information in header and footer, like integrity control sum.
	// TODO deleted (free) zones index
	// TODO if overwrite a same/smaller length zone >> don't append, just overwrite
	// TODO if overwrite a same length zone, check integrity control sum before overwrite
	
}
