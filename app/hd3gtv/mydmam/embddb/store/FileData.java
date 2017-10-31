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
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Function;

import org.apache.log4j.Logger;

import hd3gtv.mydmam.MyDMAM;

/**
 * Store raw datas sequentially in a bin File. Without index !
 * Thread safe.
 * Don't support properly parallel writing but support parallel calls.
 */
class FileData {
	private static Logger log = Logger.getLogger(FileData.class);
	
	private static final byte[] FILE_DATA_HEADER = "MYDMAMHSHDTA".getBytes(MyDMAM.UTF8);
	private static final int FILE_DATA_HEADER_LENGTH = FILE_DATA_HEADER.length + 4;
	private static final int FILE_DATA_VERSION = 1;
	
	private static final byte[] ENTRY_HEADER = "DATA".getBytes(MyDMAM.UTF8);
	private static final byte ENTRY_FOOTER = 0x0;
	private static final byte MARK_VALID_ENTRY = 0x0;
	private static final byte MARK_DELETED_ENTRY = 0x1;
	
	private volatile long file_data_write_pointer;
	private final File data_file;
	private final FileChannel channel;
	private final ArrayList<DeletedEntry> deleted_entries;
	
	FileData(File data_file) throws IOException {
		this.data_file = data_file;
		if (data_file == null) {
			throw new NullPointerException("\"data_file\" can't to be null");
		}
		
		ByteBuffer bytebuffer_header_data = ByteBuffer.allocate(FILE_DATA_HEADER_LENGTH);
		
		deleted_entries = new ArrayList<>();
		
		if (data_file.exists()) {
			channel = FileChannel.open(data_file.toPath(), MyDMAM.OPEN_OPTIONS_FILE_EXISTS);
			int size = channel.read(bytebuffer_header_data, 0);
			if (size != FILE_DATA_HEADER_LENGTH) {
				throw new IOException("Invalid header for " + data_file);
			}
			bytebuffer_header_data.flip();
			
			TransactionJournal.readAndEquals(bytebuffer_header_data, FILE_DATA_HEADER, bad_datas -> {
				return new IOException("Invalid file header: " + new String(bad_datas) + " for " + data_file);
			});
			int version = bytebuffer_header_data.getInt();
			if (version != FILE_DATA_VERSION) {
				throw new IOException("Invalid version: " + version + " instead of " + FILE_DATA_VERSION + " for " + data_file);
			}
		} else {
			channel = FileChannel.open(data_file.toPath(), MyDMAM.OPEN_OPTIONS_FILE_NOT_EXISTS);
			bytebuffer_header_data.put(FILE_DATA_HEADER);
			bytebuffer_header_data.putInt(FILE_DATA_VERSION);
			bytebuffer_header_data.flip();
			channel.write(bytebuffer_header_data, 0);
		}
		file_data_write_pointer = channel.size();
	}
	
	public void close() throws IOException {
		channel.close();
	}
	
	/**
	 * @return new data_pointer
	 */
	long write(ItemKey key, ByteBuffer data_source) throws IOException {
		int size = data_source.remaining();
		int pos = data_source.position();
		final ByteBuffer read_buffer = data_source.asReadOnlyBuffer();
		read_buffer.position(pos);
		read_buffer.limit(pos + size);
		data_source.position(pos + size);
		
		return write(key, new ByteBufferExporter() {
			
			public void toByteBuffer(ByteBuffer write_buffer) throws IOException {
				write_buffer.put(read_buffer);
			}
			
			public int getByteBufferWriteSize() {
				return size;
			}
		});
	}
	
	/**
	 * @return new data_pointer
	 */
	long write(ItemKey key, ByteBufferExporter data_source) throws IOException {
		/*
		<header size ><key_size><boolean, 1 byte><--int, 4 bytes--->              <-- byte 0 -->
		[entry header][hash key][ deleted mark  ][user's datas size][user's datas][entry footer]
		 * */
		int user_data_size = data_source.getByteBufferWriteSize();
		
		int data_entry_size = computeExactlyDataEntrySize(user_data_size);
		ByteBuffer data_buffer = ByteBuffer.allocate(data_entry_size);
		
		data_buffer.put(ENTRY_HEADER);
		data_buffer.put(key.key);
		data_buffer.put(MARK_VALID_ENTRY);
		data_buffer.putInt(user_data_size);
		data_source.toByteBuffer(data_buffer);
		data_buffer.put(ENTRY_FOOTER);
		data_buffer.flip();
		
		synchronized (deleted_entries) {
			Optional<DeletedEntry> best_deleted_entry = deleted_entries.stream().filter(d_entry -> {
				return d_entry.data_size >= user_data_size;
			}).min((l, r) -> {
				return Long.compare(l.data_size, r.data_size);
			});
			
			long data_pointer = file_data_write_pointer;
			if (best_deleted_entry.isPresent()) {
				data_pointer = best_deleted_entry.get().data_pointer;
			}
			
			if (log.isTraceEnabled()) {
				log.trace("Prepare to write datas: key = " + key + ", " + data_entry_size + " bytes from " + data_pointer);
			}
			
			int size = channel.write(data_buffer, data_pointer);
			
			if (best_deleted_entry.isPresent() == false) {
				/**
				 * Not recycling...
				 */
				file_data_write_pointer += size;
			}
			
			if (size != data_entry_size) {
				throw new IOException("Can't write data entry: " + size + " on " + data_entry_size + " bytes for " + key);
			}
			
			if (best_deleted_entry.isPresent()) {
				/**
				 * Don't reuse...
				 */
				deleted_entries.remove(best_deleted_entry.get());
			}
			
			return data_pointer;
		}
	}
	
	private class DeletedEntry {
		long data_pointer;
		int data_size;
		
		DeletedEntry(long data_pointer, int data_size) {
			this.data_pointer = data_pointer;
			this.data_size = data_size;
		}
		
	}
	
	void markDelete(long data_pointer, ItemKey expected_key) throws IOException {
		/*
		<header size ><key_size><boolean, 1 byte><--int, 4 bytes--->              <-- byte 0 -->
		[entry header][hash key][ deleted mark  ][user's datas size][user's datas][entry footer]
		 */
		
		/**
		 * Check if data_pointer point to a real data entry, and match with the good key.
		 */
		ByteBuffer buffer = ByteBuffer.allocate(ENTRY_HEADER.length + ItemKey.SIZE);
		
		int size = channel.read(buffer, data_pointer);
		if (size != ENTRY_HEADER.length + ItemKey.SIZE) {
			throw new IOException("Invalid read size: read = " + size + " instead of " + ENTRY_HEADER.length + ItemKey.SIZE);
		}
		buffer.flip();
		TransactionJournal.readAndEquals(buffer, ENTRY_HEADER, err -> {
			return new IOException("Bad entry header: " + new String(err, MyDMAM.UTF8));
		});
		TransactionJournal.readAndEquals(buffer, expected_key.key, err -> {
			return new IOException("Bad expected key in header: " + MyDMAM.byteToString(err) + " instead of " + expected_key);
		});
		
		/**
		 * Set delete mark to 1
		 */
		long zone_pointer = data_pointer + ENTRY_HEADER.length + ItemKey.SIZE;
		
		buffer = ByteBuffer.wrap(new byte[] { MARK_DELETED_ENTRY });
		size = channel.write(buffer, zone_pointer);
		if (size != 1) {
			throw new IOException("Can't write delete mark: " + size);
		}
		
		/**
		 * Get data size for this entry
		 */
		buffer = ByteBuffer.allocate(4);
		zone_pointer = data_pointer + ENTRY_HEADER.length + ItemKey.SIZE + 1;
		size = channel.read(buffer, zone_pointer);
		if (size != 4) {
			throw new IOException("Can't read data size (" + size + ") for pointer " + data_pointer);
		}
		buffer.flip();
		int data_size = buffer.getInt();
		
		synchronized (deleted_entries) {
			deleted_entries.add(new DeletedEntry(data_pointer, data_size));
		}
	}
	
	/*public static <T extends Exception> void readAndEquals(ByteBuffer buffer, long expected, Function<Long, T> onDifference) throws T {
		long real_value = buffer.getLong();
		if (expected != real_value) {
			throw onDifference.apply(real_value);
		}
	}*/
	
	public static <T extends Exception> void readByteAndEquals(ByteBuffer buffer, byte expected, Function<Byte, T> onDifference) throws T {
		byte real_value = buffer.get();
		if (expected != real_value) {
			throw onDifference.apply(real_value);
		}
	}
	
	Entry read(long data_pointer, ItemKey expected_key) throws IOException {
		/*
		<header size ><key_size><boolean, 1 byte><--int, 4 bytes--->              <-- byte 0 -->
		[entry header][hash key][ deleted mark  ][user's datas size][user's datas][entry footer]
		 * */
		int full_header_len = ENTRY_HEADER.length + ItemKey.SIZE + 1 + 4;
		ByteBuffer header_buffer = ByteBuffer.allocate(full_header_len);
		if (log.isTraceEnabled()) {
			log.trace("Prepare to read data header from " + data_pointer);
		}
		
		int s = channel.read(header_buffer, data_pointer);
		if (s != full_header_len) {
			throw new IOException("Invalid read size: read = " + s + " instead of " + full_header_len);
		}
		header_buffer.flip();
		
		TransactionJournal.readAndEquals(header_buffer, ENTRY_HEADER, err -> {
			return new IOException("Bad entry header: " + new String(err, MyDMAM.UTF8));
		});
		TransactionJournal.readAndEquals(header_buffer, expected_key.key, err -> {
			return new IOException("Bad expected key in header: " + MyDMAM.byteToString(err) + " instead of " + expected_key);
		});
		readByteAndEquals(header_buffer, MARK_VALID_ENTRY, err -> {
			return new IOException("Data entry marked as deleted/invalid: " + err + " for " + expected_key);
		});
		int user_data_length = header_buffer.getInt();
		if (user_data_length < 0) {
			throw new IOException("Invalid data size: data_entry_size: " + user_data_length);
		}
		
		ByteBuffer data_buffer = ByteBuffer.allocate(user_data_length + 1);// + ENTRY FOOTER
		int s2 = channel.read(data_buffer, data_pointer + (long) full_header_len);
		if (s2 != user_data_length + 1) {
			throw new IOException("Invalid read size, read only the first " + s2 + " byte(s)");
		}
		data_buffer.flip();
		
		ByteBuffer out_bytebuffer = null;
		
		int actual_pos = data_buffer.position();
		out_bytebuffer = data_buffer.asReadOnlyBuffer();
		out_bytebuffer.position(actual_pos);
		out_bytebuffer.limit(actual_pos + user_data_length);
		
		Entry result = new Entry(expected_key, out_bytebuffer);
		data_buffer.position(actual_pos + user_data_length);
		
		readByteAndEquals(data_buffer, ENTRY_FOOTER, err -> {
			return new IOException("Invalid data entry footer: " + err);
		});
		
		return result;
	}
	
	private int computeExactlyDataEntrySize(int user_data_len) {
		/*
		<header size ><key_size><boolean, 1 byte><--int, 4 bytes--->              <-- byte 0 -->
		[entry header][hash key][ deleted mark  ][user's datas size][user's datas][entry footer]
		 * */
		return ENTRY_HEADER.length + ItemKey.SIZE + 1 + 4 + user_data_len + 1;
	}
	
	public final class Entry {
		public final ItemKey key;
		public final ByteBuffer data;
		
		private Entry(ItemKey key, ByteBuffer data) {
			this.key = key;
			this.data = data;
		}
		
		public String toString() {
			return key.toString();
		}
		
		private volatile byte[] _data;
		
		public byte[] toBytes() {
			if (_data == null) {
				synchronized (data) {
					int size = data.remaining();
					int pos = data.position();
					_data = new byte[size];
					data.get(_data);
					data.position(pos);
				}
			}
			
			return _data;
		}
		
	}
	
	void clear() throws IOException {
		log.info("Clear " + data_file);
		synchronized (deleted_entries) {
			deleted_entries.clear();
			file_data_write_pointer = FILE_DATA_HEADER_LENGTH;
			try {
				channel.truncate(FILE_DATA_HEADER_LENGTH);
				channel.force(true);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
}
