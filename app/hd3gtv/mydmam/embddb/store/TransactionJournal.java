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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterators;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.tools.ThreadPoolExecutorFactory;

/**
 * Write only if new file, read only if file exists.
 */
class TransactionJournal {
	
	private static Logger log = Logger.getLogger(TransactionJournal.class);
	private static final byte[] JOURNAL_HEADER = "MYDMAMJOURNAL".getBytes(MyDMAM.UTF8);
	private static final int JOURNAL_VERSION = 1;
	
	private static final byte[] ENTRY_HEADER = "HEAD".getBytes(MyDMAM.UTF8);
	private static final byte[] ENTRY_SEPARATOR = "NEXT".getBytes(MyDMAM.UTF8);
	private static final String EXTENSION = ".myjournal";
	
	private final FileChannel file_channel;
	private final ThreadPoolExecutorFactory executor;
	private final File file;
	private final long creation_date;
	
	private TransactionJournal(File file) throws IOException {
		this.file = file;
		if (file == null) {
			throw new NullPointerException("\"file\" can't to be null");
		}
		
		ByteBuffer bytebuffer_header = ByteBuffer.allocate(JOURNAL_HEADER.length + 4 + 8);
		
		executor = new ThreadPoolExecutorFactory(getClass().getSimpleName(), Thread.MAX_PRIORITY);
		
		if (file.exists()) {
			file_channel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
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
			file_channel = FileChannel.open(file.toPath(), StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW, StandardOpenOption.APPEND);
			creation_date = System.currentTimeMillis();
			bytebuffer_header.put(JOURNAL_HEADER);
			bytebuffer_header.putInt(JOURNAL_VERSION);
			bytebuffer_header.putLong(creation_date);
			bytebuffer_header.flip();
			file_channel.write(bytebuffer_header);
			
			executor.setSimplePoolSize();
		}
		
	}
	
	TransactionJournal(File base_directory, UUID instance) throws IOException {
		this(new File(base_directory.getPath() + File.separator + instance.toString().toUpperCase() + "_" + Loggers.dateFilename(System.currentTimeMillis()) + EXTENSION));
	}
	
	static List<TransactionJournal> allJournalsByDate(File base_directory) {
		return Arrays.asList(base_directory.listFiles((dire, name) -> {
			return name.endsWith(EXTENSION);
		})).stream().filter(f -> {
			return f.canRead();
		}).map(f -> {
			try {
				return new TransactionJournal(f);
			} catch (IOException e) {
				throw new RuntimeException("Can't open file " + f.getAbsolutePath(), e);
			}
		}).sorted((l, r) -> {
			return (int) Math.signum(l.creation_date - r.creation_date);
		}).collect(Collectors.toList());
	}
	
	/**
	 * Put a file_channel.force in the queue.
	 */
	Future<?> sync(Consumer<IOException> onError) {
		return executor.submit(() -> {
			try {
				file_channel.force(true);
			} catch (IOException e) {
				log.error("Can't write in the file " + file.getPath());
				onError.accept(e);
			}
		});
	}
	
	/**
	 * Put a close() in the queue.
	 */
	Future<?> close(Consumer<IOException> onError) {
		return executor.submit(() -> {
			try {
				if (file_channel.isOpen()) {
					file_channel.close();
				}
			} catch (IOException e) {
				log.error("Can't close the file " + file.getPath());
				onError.accept(e);
			}
		});
	}
	
	/**
	 * Sync (close + delete)
	 */
	void delete() throws IOException {
		if (file_channel.isOpen()) {
			file_channel.close();
		}
		FileUtils.forceDelete(file);
	}
	
	private void writeNextEntrySeparator(int size) throws IOException {
		ByteBuffer write_buffer = ByteBuffer.allocate(ENTRY_SEPARATOR.length + 4);
		write_buffer.put(ENTRY_SEPARATOR);
		write_buffer.putInt(size);
		write_buffer.flip();
		file_channel.write(write_buffer);
	}
	
	private int readNextEntrySeparator() throws IOException {
		ByteBuffer read_buffer = ByteBuffer.allocate(ENTRY_SEPARATOR.length + 4);
		file_channel.read(read_buffer);
		read_buffer.flip();
		readAndEquals(read_buffer, ENTRY_SEPARATOR, buff -> {
			return new IOException("Expected entry separator tag instead of " + new String(buff));
		});
		return read_buffer.getInt();
	}
	
	/**
	 * Preparation (byte wrapping) is blocking, but write is put in the queue (non-blocking).
	 * @return Future Null ilf error.
	 */
	Future<ItemKey> write(String database_name, String data_class_name, ItemKey key, byte[] content, BiConsumer<ItemKey, IOException> onError) {
		JournalEntry entry = new JournalEntry(database_name, data_class_name, key, content);
		ByteBuffer write_buffer = ByteBuffer.allocate(entry.estimateSize());
		entry.saveRawEntry(write_buffer);
		write_buffer.flip();
		
		return executor.submit(() -> {
			try {
				writeNextEntrySeparator(write_buffer.remaining());
				file_channel.write(write_buffer);
				return entry.key;
			} catch (IOException e) {
				onError.accept(entry.key, e);
			}
			return null;
		});
	};
	
	/**
	 * Fully sync
	 */
	Stream<JournalEntry> readAll() {
		long actual_pos = 0;
		long size = file.length();
		
		try {
			actual_pos = file_channel.position();
		} catch (IOException e1) {
			throw new RuntimeException("Can't get actual position for " + file, e1);
		}
		
		Iterator<JournalEntry> iterator = new Iterator<TransactionJournal.JournalEntry>() {
			
			ByteBuffer read_buffer = ByteBuffer.allocate(0xFFFF);
			
			public JournalEntry next() {
				try {
					read_buffer.clear();
					
					int next_size = readNextEntrySeparator();
					if (read_buffer.capacity() < next_size) {
						read_buffer = ByteBuffer.allocate(next_size);
					}
					read_buffer.limit(next_size);
					
					file_channel.read(read_buffer);
					read_buffer.flip();
					
					return new JournalEntry(read_buffer);
				} catch (Exception e) {
					throw new RuntimeException("Can't read " + file, e);
				}
			}
			
			public boolean hasNext() {
				try {
					return file_channel.position() < size;
				} catch (Exception e) {
					throw new RuntimeException("Can't read " + file, e);
				}
			}
		};
		
		final long _actual_pos = actual_pos;
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false).onClose(() -> {
			try {
				file_channel.position(_actual_pos);
			} catch (IOException e) {
				throw new RuntimeException("Can't reset file_channel position", e);
			}
		});
	}
	
	class JournalEntry {
		final String database_name;
		final String data_class_name;
		final ItemKey key;
		final byte[] content;
		final long date;
		
		private JournalEntry(String database_name, String data_class_name, ItemKey key, byte[] content) {
			this.database_name = database_name;
			if (database_name == null) {
				throw new NullPointerException("\"database_name\" can't to be null");
			}
			this.data_class_name = data_class_name;
			if (data_class_name == null) {
				throw new NullPointerException("\"data_class_name\" can't to be null");
			}
			this.key = key;
			if (key == null) {
				throw new NullPointerException("\"key\" can't to be null");
			}
			this.content = content;
			if (content == null) {
				throw new NullPointerException("\"content\" can't to be null");
			}
			date = System.currentTimeMillis();
		}
		
		/**
		 * @param write_buffer not cleared before, not flipped after (only put)
		 */
		private void saveRawEntry(ByteBuffer write_buffer) {
			write_buffer.put(ENTRY_HEADER);
			write_buffer.putLong(date);
			writeNextBlock(write_buffer, database_name.getBytes(MyDMAM.UTF8));
			writeNextBlock(write_buffer, data_class_name.getBytes(MyDMAM.UTF8));
			writeNextBlock(write_buffer, key.key);
			writeNextBlock(write_buffer, content);
		}
		
		private int estimateSize() {
			return ENTRY_HEADER.length + 8 + (4 + database_name.length() * 2) + (4 + data_class_name.length() * 2) + (4 + key.key.length) + (4 + content.length);
		}
		
		/**
		 * READ
		 * @param write_buffer not flipped before, not cleared after (only get)
		 */
		private JournalEntry(ByteBuffer read_buffer) throws IOException {
			readAndEquals(read_buffer, ENTRY_HEADER, header -> {
				return new IOException("Invalid header for entry: " + String.valueOf(header));
			});
			
			date = read_buffer.getLong();
			database_name = new String(readNextBlock(read_buffer), MyDMAM.UTF8);
			data_class_name = new String(readNextBlock(read_buffer), MyDMAM.UTF8);
			key = new ItemKey(readNextBlock(read_buffer));
			content = readNextBlock(read_buffer);
		}
		
	}
	
	public static <T extends Exception> void readAndEquals(ByteBuffer buffer, byte[] compare_to, Function<byte[], T> onDifference) throws T {
		byte[] real_value = new byte[compare_to.length];
		buffer.get(real_value);
		if (Arrays.equals(compare_to, real_value) == false) {
			throw onDifference.apply(real_value);
		}
	}
	
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
	
	long getFileSize() { // TODO external rotate (just recreate : file name is date based)
		return file.length();
	}
	
	// TODO read http://distributeddatastore.blogspot.fr/2013/08/cassandra-sstable-storage-format.html
}
