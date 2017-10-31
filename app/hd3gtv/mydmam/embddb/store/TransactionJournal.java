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
 * Copyright (C) hdsdi3g for hd3g.tv 16 sept. 2017
 * 
*/
package hd3gtv.mydmam.embddb.store;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.tools.StreamMaker;

class TransactionJournal implements Closeable {
	
	// private static Logger log = Logger.getLogger(TransactionJournal.class);
	private static final byte[] JOURNAL_HEADER = "MYDMAMJOURNAL".getBytes(MyDMAM.UTF8);
	private static final int JOURNAL_VERSION = 1;
	private static final int HEADER_LENGTH = JOURNAL_HEADER.length + 4 + 8;
	
	private static final byte[] ENTRY_HEADER = "HEAD".getBytes(MyDMAM.UTF8);
	private static final byte[] ENTRY_SEPARATOR = "NEXT".getBytes(MyDMAM.UTF8);
	
	private final FileChannel file_channel;
	private final File file;
	private final long creation_date;
	
	TransactionJournal(File file) throws IOException {
		this.file = file;
		if (file == null) {
			throw new NullPointerException("\"file\" can't to be null");
		}
		
		ByteBuffer bytebuffer_header = ByteBuffer.allocate(HEADER_LENGTH);
		
		if (file.exists()) {
			file_channel = FileChannel.open(file.toPath(), MyDMAM.OPEN_OPTIONS_FILE_EXISTS);
			file_channel.read(bytebuffer_header);
			bytebuffer_header.flip();
			
			readAndEquals(bytebuffer_header, JOURNAL_HEADER, bad_datas -> {
				return new IOException("Invalid file header: " + new String(bad_datas));
			});
			int journal_version = bytebuffer_header.getInt();
			if (journal_version != JOURNAL_VERSION) {
				throw new IOException("Invalid journal version: " + journal_version + " instead of " + JOURNAL_VERSION);
			}
			creation_date = bytebuffer_header.getLong();
		} else {
			file_channel = FileChannel.open(file.toPath(), MyDMAM.OPEN_OPTIONS_FILE_NOT_EXISTS);
			creation_date = System.currentTimeMillis();
			bytebuffer_header.put(JOURNAL_HEADER);
			bytebuffer_header.putInt(JOURNAL_VERSION);
			bytebuffer_header.putLong(creation_date);
			bytebuffer_header.flip();
			file_channel.write(bytebuffer_header);
		}
	}
	
	void channelSync() throws IOException {
		file_channel.force(true);
	}
	
	public void close() throws IOException {
		if (file_channel.isOpen()) {
			channelSync();
			file_channel.close();
		}
	}
	
	/**
	 * Thread safe
	 */
	void write(ItemKey key, ByteBufferExporter data_source, long expiration_date, String path) throws IOException {
		new JournalEntry(key, data_source, expiration_date, path);
	};
	
	Stream<JournalEntry> readAll(boolean partial_read) throws IOException {
		long actual_pos = file_channel.position();
		long size = file_channel.size();
		file_channel.position(HEADER_LENGTH);
		
		ByteBuffer head_read_buffer = ByteBuffer.allocate(ENTRY_SEPARATOR.length + 4);
		
		StreamMaker<JournalEntry> s_m = StreamMaker.create(() -> {
			try {
				while (file_channel.position() < size) {
					head_read_buffer.clear();
					file_channel.read(head_read_buffer);
					head_read_buffer.flip();
					readAndEquals(head_read_buffer, ENTRY_SEPARATOR, buff -> {
						return new IOException("Expected entry separator tag instead of " + new String(buff));
					});
					int next_size = head_read_buffer.getInt();
					if (next_size < 1) {
						throw new IOException("Invalid next size: " + next_size);
					}
					
					ByteBuffer read_buffer = ByteBuffer.allocate(next_size);
					file_channel.read(read_buffer);
					read_buffer.flip();
					return new JournalEntry(read_buffer, partial_read);
				}
				return null;
			} catch (Exception e) {
				throw new RuntimeException("Can't read " + file, e);
			}
		});
		
		long now = System.currentTimeMillis();
		return s_m.stream().filter(entry -> {
			return entry.expiration_date > now;
		}).onClose(() -> {
			try {
				file_channel.position(actual_pos);
			} catch (IOException e) {
				throw new RuntimeException("Can't reset file_channel position", e);
			}
		});
	}
	
	class JournalEntry {
		/**
		 * Direct write
		 * Thread safe
		 */
		private JournalEntry(ItemKey key, ByteBufferExporter data_source, long expiration_date, String path) throws IOException {
			synchronized (file_channel) {
				if (key == null) {
					throw new NullPointerException("\"key\" can't to be null");
				}
				if (data_source == null) {
					throw new NullPointerException("\"data_source\" can't to be null");
				}
				if (expiration_date == 0) {
					throw new NullPointerException("\"expiration_date\" can't to be equals to 0");
				}
				if (path == null) {
					path = "";
				}
				byte[] b_path = path.getBytes(MyDMAM.UTF8);
				
				int block_size = data_source.getByteBufferWriteSize();
				int data_size = ENTRY_HEADER.length + 8 + 8 + (4 + key.key.length) + (4 + block_size) + (4 + b_path.length);
				int total_size = ENTRY_SEPARATOR.length + 4 + data_size;
				
				ByteBuffer write_buffer = ByteBuffer.allocate(total_size);
				write_buffer.put(ENTRY_SEPARATOR);
				write_buffer.putInt(data_size);
				write_buffer.put(ENTRY_HEADER);
				write_buffer.putLong(System.currentTimeMillis());
				write_buffer.putLong(expiration_date);
				writeNextBlock(write_buffer, key.key);
				write_buffer.putInt(block_size);
				data_source.toByteBuffer(write_buffer);
				writeNextBlock(write_buffer, b_path);
				write_buffer.flip();
				
				int writed_size = file_channel.write(write_buffer);
				if (writed_size != total_size) {
					throw new IOException("Can't write journal (" + writed_size + "/" + total_size + ")");
				}
			}
		}
		
		ItemKey key;
		long expiration_date;
		String path;
		ByteBufferExporter data_export_source;
		long date;
		
		/**
		 * READ
		 * @param write_buffer not flipped before, but cleared after
		 * @param partial_read don't read content and path (set null)
		 */
		private JournalEntry(ByteBuffer read_buffer, boolean partial_read) throws IOException {
			readAndEquals(read_buffer, ENTRY_HEADER, header -> {
				return new IOException("Invalid header for entry: " + String.valueOf(header));
			});
			
			date = read_buffer.getLong();
			expiration_date = read_buffer.getLong();
			key = new ItemKey(readNextBlock(read_buffer));
			if (partial_read == false) {
				int size = read_buffer.getInt();
				
				int actual_pos = read_buffer.position();
				final ByteBuffer output_bytebuffer = read_buffer.asReadOnlyBuffer();
				output_bytebuffer.limit(actual_pos + size);
				output_bytebuffer.position(actual_pos);
				
				data_export_source = new ByteBufferExporter() {
					
					public void toByteBuffer(ByteBuffer write_buffer) throws IOException {
						write_buffer.put(output_bytebuffer);
					}
					
					public int getByteBufferWriteSize() {
						return size;
					}
				};
				
				read_buffer.position(actual_pos + size);
				path = new String(readNextBlock(read_buffer), MyDMAM.UTF8);
			} else {
				data_export_source = null;
				path = null;
			}
		}
		
	}
	
	public static <T extends Exception> void readAndEquals(ByteBuffer buffer, byte[] compare_to, Function<byte[], T> onDifference) throws T {
		byte[] real_value = new byte[compare_to.length];
		buffer.get(real_value);
		if (Arrays.equals(compare_to, real_value) == false) {
			throw onDifference.apply(real_value);
		}
	}
	
	/**
	 * Out size = 4 + value.length
	 */
	public static void writeNextBlock(ByteBuffer buffer, byte[] value) {
		buffer.putInt(value.length);
		if (value.length > 0) {
			buffer.put(value);
		}
	}
	
	public static byte[] readNextBlock(ByteBuffer buffer) {
		int size = buffer.getInt();
		byte[] b = new byte[size];
		if (size > 0) {
			buffer.get(b);
		}
		return b;
	}
	
	long getFileSize() {
		return file.length();
	}
	
	public void clear() throws IOException {
		synchronized (file_channel) {
			file_channel.truncate(HEADER_LENGTH);
			file_channel.force(true);
		}
	}
	
}
