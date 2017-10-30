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
 * Copyright (C) hdsdi3g for hd3g.tv 26 oct. 2017
 * 
*/
package hd3gtv.mydmam.embddb.store;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.concurrent.LinkedTransferQueue;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonIgnore;
import hd3gtv.tools.FreeDiskSpaceWarningException;
import hd3gtv.tools.StreamMaker;
import hd3gtv.tools.ThreadPoolExecutorFactory;

@GsonIgnore
public class HistoryJournal implements Closeable {
	private static Logger log = Logger.getLogger(HistoryJournal.class);
	
	private static final byte[] JOURNAL_HEADER = "MYDMAMHISTORYJOURNAL".getBytes(MyDMAM.UTF8);
	private static final int JOURNAL_VERSION = 1;
	private static final int HEADER_LENGTH = JOURNAL_HEADER.length + 4 + 8;
	
	private static final byte ENTRY_SEPARATOR = 0x0;
	private static final String FILE_NAME = "store.myhistory";
	
	private FileChannel file_channel;
	private final File file;
	private long creation_date;
	private final long grace_period_for_expired_items;
	private final long max_losted_data_space_size;
	
	private volatile long oldest_valid_recorded_value_position;
	private volatile boolean pending_close;
	
	private final LinkedTransferQueue<HistoryEntry> pending_writes;
	private final ThreadPoolExecutorFactory write_pool;
	
	/**
	 * @param write_pool should set to setSimplePoolSize
	 */
	public HistoryJournal(ThreadPoolExecutorFactory write_pool, File base_directory, long grace_period_for_expired_items, int max_losted_item_count) throws IOException {
		this.write_pool = write_pool;
		if (write_pool == null) {
			throw new NullPointerException("\"write_pool\" can't to be null");
		}
		if (base_directory == null) {
			throw new NullPointerException("\"base_directory\" can't to be null");
		}
		this.grace_period_for_expired_items = grace_period_for_expired_items;
		if (grace_period_for_expired_items <= 0) {
			throw new NullPointerException("\"grace_period_for_expired_items\" can't to be <= 0");
		}
		if (max_losted_item_count <= 0) {
			throw new NullPointerException("\"max_losted_item_count\" can't to be <= 0");
		}
		this.max_losted_data_space_size = (long) max_losted_item_count * (long) ENTRY_SIZE;
		pending_writes = new LinkedTransferQueue<>();
		
		this.file = new File(base_directory.getAbsolutePath() + File.separator + FILE_NAME);
		open();
		oldest_valid_recorded_value_position = HEADER_LENGTH;
	}
	
	private void open() throws IOException {
		ByteBuffer bytebuffer_header = ByteBuffer.allocate(HEADER_LENGTH);
		
		if (file.exists()) {
			file_channel = FileChannel.open(file.toPath(), MyDMAM.OPEN_OPTIONS_FILE_EXISTS);
			file_channel.read(bytebuffer_header);
			bytebuffer_header.flip();
			
			TransactionJournal.readAndEquals(bytebuffer_header, JOURNAL_HEADER, bad_datas -> {
				return new IOException("Invalid file header: " + new String(bad_datas));
			});
			int journal_version = bytebuffer_header.getInt();
			if (journal_version != JOURNAL_VERSION) {
				throw new IOException("Invalid history journal version: " + journal_version + " instead of " + JOURNAL_VERSION);
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
		pending_close = false;
	}
	
	public void close() throws IOException {
		if (file_channel.isOpen()) {
			pending_close = true;
			while (pending_writes.isEmpty() == false) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					log.error("Can't sleep", e);
				}
			}
			channelSync();
			file_channel.close();
		}
	}
	
	void purge() throws IOException {
		if (file_channel.isOpen()) {
			pending_close = true;
			pending_writes.clear();
			file_channel.force(true);
			file_channel.close();
		}
		file.delete();
		file.deleteOnExit();
	}
	
	void channelSync() throws IOException {
		file_channel.force(true);
	}
	
	private static final int ENTRY_SIZE = 1 + 8 + 8 + ItemKey.SIZE + 4 + Item.CRC32_SIZE;
	
	/**
	 * Thread safe and blocking.
	 */
	void write(Item item) throws IOException {
		if (pending_close) {
			throw new RuntimeException("Current channel is pending close");
		}
		if (file_channel.isOpen() == false) {
			throw new RuntimeException("Current channel is closed");
		}
		if (item.getDeleteDate() + grace_period_for_expired_items < System.currentTimeMillis()) {
			return;
		}
		HistoryEntry result = new HistoryEntry(item);
		pending_writes.add(result);
		
		synchronized (file) {
			ArrayList<HistoryEntry> to_push_list = new ArrayList<>();
			to_push_list.ensureCapacity(pending_writes.size());
			pending_writes.drainTo(to_push_list);
			
			if (to_push_list.isEmpty()) {
				return;
			}
			
			int expected_write_size = ENTRY_SIZE * to_push_list.size();
			ByteBuffer write_buffer = ByteBuffer.allocate(expected_write_size);
			to_push_list.forEach(h_e -> {
				h_e.write(write_buffer);
			});
			write_buffer.flip();
			
			int writed_size = file_channel.write(write_buffer);
			if (writed_size != expected_write_size) {
				throw new IOException("Can't write in history journal (" + writed_size + "/" + expected_write_size + ")");
			}
		}
	}
	
	@GsonIgnore
	public class HistoryEntry {
		public final long update_date;
		public final long delete_date;
		public final ItemKey key;
		public final int data_size;
		public final byte[] data_digest;
		
		/**
		 * Can be null
		 */
		final Item source_item;
		
		/**
		 * ENTRY_SEPARATOR, update_date, delete_date, key, data_size, data_digest
		 */
		private HistoryEntry(ByteBuffer read_buffer) throws IOException {
			if (read_buffer.get() != ENTRY_SEPARATOR) {
				throw new IOException("Invalid entry separator");
			}
			update_date = read_buffer.getLong();
			delete_date = read_buffer.getLong();
			key = new ItemKey(read_buffer);
			data_size = read_buffer.getInt();
			data_digest = new byte[Item.CRC32_SIZE];
			read_buffer.get(data_digest);
			source_item = null;
		}
		
		private HistoryEntry(Item item) throws IOException {
			this.source_item = item;
			update_date = item.getUpdated();
			delete_date = item.getDeleteDate();
			key = item.getKey();
			data_size = item.getPayload().length;
			data_digest = item.getDigest();
		}
		
		private void write(ByteBuffer write_buffer) {
			write_buffer.put(ENTRY_SEPARATOR);
			write_buffer.putLong(update_date);
			write_buffer.putLong(delete_date);
			write_buffer.put(key.key);
			write_buffer.putInt(data_size);
			write_buffer.put(data_digest);
		}
		
	}
	
	/**
	 * Thread safe. Filter out expired and updated before-delete-grace-period entries.
	 * Should not be used for a get *all* entries, only recents entries like max(start_date, now - grace period).
	 * Can get multiple versions for same Itemkey, and not necessarily date sorted.
	 */
	public Stream<HistoryEntry> getAllSince(long start_date) throws IOException {
		long _size = 0;
		synchronized (file) {
			file_channel.force(true);
			_size = file_channel.size();
		}
		final long pos = oldest_valid_recorded_value_position;
		final long size = _size - pos;
		
		if (size == 0) {
			return Stream.empty();
		}
		final MappedByteBuffer map = file_channel.map(MapMode.READ_ONLY, pos, size);
		
		StreamMaker<HistoryEntry> s_m = StreamMaker.create(() -> {
			try {
				while (map.hasRemaining()) {
					return new HistoryEntry(map);
				}
				return null;
			} catch (Exception e) {
				throw new RuntimeException("Can't read " + file, e);
			}
		});
		
		return s_m.stream().filter(h_e -> {
			return h_e.update_date >= start_date && h_e.delete_date + grace_period_for_expired_items > System.currentTimeMillis();
		});
	}
	
	/**
	 * Thread safe
	 * Ignore actual deleted and expired values.
	 * @param include_oldest_entries based on last defragment measure, or just file size.
	 */
	public int getEntryCount(boolean include_oldest_entries) throws IOException {
		long pos = HEADER_LENGTH;
		if (include_oldest_entries == false) {
			pos = oldest_valid_recorded_value_position;
		}
		
		long size = pos;
		synchronized (file) {
			file_channel.force(true);
			size = file_channel.size();// Get real file size
		}
		
		long width = size - pos;
		
		return (int) (width / (long) ENTRY_SIZE);
	}
	
	/**
	 * Can take time...
	 * Thread safe
	 */
	public void defragment() throws IOException {
		synchronized (file) {
			if (oldest_valid_recorded_value_position < max_losted_data_space_size) {
				/**
				 * SetOldestValidRecordedValuePosition: search the last expired entry.
				 */
				file_channel.force(true);
				final long pos = oldest_valid_recorded_value_position;
				final long size = file_channel.size() - pos;
				
				if (size == 0) {
					return;
				}
				final MappedByteBuffer map = file_channel.map(MapMode.READ_ONLY, pos, size);
				
				while (map.hasRemaining()) {
					HistoryEntry h_e = new HistoryEntry(map);
					if (h_e.delete_date + grace_period_for_expired_items < System.currentTimeMillis()) {
						/**
						 * Last expired item
						 */
						oldest_valid_recorded_value_position = map.position();
					} else if (h_e.update_date + grace_period_for_expired_items < System.currentTimeMillis()) {
						/**
						 * First too "old" item
						 */
						oldest_valid_recorded_value_position = map.position();
					} else {
						break;
					}
				}
				map.clear();
				
				if (oldest_valid_recorded_value_position < max_losted_data_space_size) {
					return;
				}
			}
			
			FreeDiskSpaceWarningException.check(file.getParentFile(), file.length() * 2l);
			
			file_channel.force(true);
			file_channel.close();
			File new_old = new File(file.getAbsolutePath() + ".old");
			if (new_old.exists()) {
				FileUtils.forceDelete(new_old);
			}
			FileUtils.moveFile(file, new_old);
			open();
			FileChannel older_file_channel = FileChannel.open(new_old.toPath(), MyDMAM.OPEN_OPTIONS_FILE_EXISTS);
			MappedByteBuffer map = older_file_channel.map(MapMode.READ_ONLY, HEADER_LENGTH, older_file_channel.size());
			
			int actual_position;
			long delete_date;
			ByteBuffer transfert_buffer = map.asReadOnlyBuffer();
			int size;
			while (map.remaining() >= ENTRY_SIZE) {
				actual_position = map.position();
				map.position(actual_position + 1 + 8);// ENTRY_HEADER + update_date
				delete_date = map.getLong();
				
				if (delete_date + grace_period_for_expired_items < System.currentTimeMillis()) {
					/**
					 * Has expired
					 */
					continue;
				}
				transfert_buffer.limit(actual_position + ENTRY_SIZE);
				transfert_buffer.position(actual_position);
				size = file_channel.write(transfert_buffer);
				if (size != ENTRY_SIZE) {
					throw new IOException("Invalid writing: " + size + "/" + ENTRY_SIZE);
				}
				if (map.remaining() - ENTRY_SIZE >= 0) {
					map.position(actual_position + ENTRY_SIZE);
					continue;
				}
			}
			map.clear();
			older_file_channel.close();
			file_channel.force(true);
			FileUtils.forceDelete(new_old);
			oldest_valid_recorded_value_position = HEADER_LENGTH;
		}
	}
	
}
