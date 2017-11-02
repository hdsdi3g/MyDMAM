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
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonIgnore;
import hd3gtv.tools.FreeDiskSpaceWarningException;
import hd3gtv.tools.StreamMaker;

@GsonIgnore
/**
 * Limited to MAX_INT / 2 of lower if not has enough memory.
 */
public class HistoryJournal implements Closeable {
	private static Logger log = Logger.getLogger(HistoryJournal.class);
	
	private static final byte[] JOURNAL_HEADER = "MYDMAMHISTORYJOURNAL".getBytes(MyDMAM.UTF8);
	private static final int JOURNAL_VERSION = 1;
	private static final int HEADER_LENGTH = JOURNAL_HEADER.length + 4 + 8;
	private static final byte ENTRY_SEPARATOR = (byte) 0xFF;
	
	private FileChannel file_channel;
	private final File file;
	private long creation_date;
	private final long grace_period_for_expired_items;
	private final long max_losted_data_space_size;
	
	private volatile long oldest_valid_recorded_value_position;
	private volatile boolean pending_close;
	
	/**
	 * @param write_pool should set to setSimplePoolSize
	 */
	public HistoryJournal(File file, long grace_period_for_expired_items, int max_losted_item_count) throws IOException {
		this.file = file;
		if (file == null) {
			throw new NullPointerException("\"file\" can't to be null");
		}
		this.grace_period_for_expired_items = grace_period_for_expired_items;
		if (grace_period_for_expired_items <= 0) {
			throw new NullPointerException("\"grace_period_for_expired_items\" can't to be <= 0");
		}
		if (max_losted_item_count <= 0) {
			throw new NullPointerException("\"max_losted_item_count\" can't to be <= 0");
		}
		this.max_losted_data_space_size = (long) max_losted_item_count * (long) ENTRY_SIZE;
		open();
		oldest_valid_recorded_value_position = HEADER_LENGTH;
	}
	
	private void open() throws IOException {
		ByteBuffer bytebuffer_header = ByteBuffer.allocate(HEADER_LENGTH);
		
		if (file.exists()) {
			file_channel = FileChannel.open(file.toPath(), MyDMAM.OPEN_OPTIONS_FILE_EXISTS);
			if (file.length() == 0) {
				creation_date = System.currentTimeMillis();
				bytebuffer_header.put(JOURNAL_HEADER);
				bytebuffer_header.putInt(JOURNAL_VERSION);
				bytebuffer_header.putLong(creation_date);
				bytebuffer_header.flip();
				file_channel.write(bytebuffer_header);
			} else {
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
			}
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
	
	/**
	 * Thread safe
	 * @return
	 * @throws IOException
	 */
	long getFileSize() throws IOException {
		synchronized (file) {
			return file_channel.size();
		}
	}
	
	public void close() throws IOException {
		if (file_channel.isOpen()) {
			pending_close = true;
			/*while (pending_writes.isEmpty() == false) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					log.error("Can't sleep", e);
				}
			}*/
			channelSync();
			file_channel.close();
		}
	}
	
	void channelSync() throws IOException {
		file_channel.force(true);
	}
	
	private static final int ENTRY_SIZE = 1 + 8 + 8 + ItemKey.SIZE + 4 + Item.CRC32_SIZE;
	
	/*
	 * Thread safe and non blocking.
	 * Waits the ends of stream for start write.
	 */
	/*CompletableFuture<List<Item>> write(Stream<Item> items) {
		if (pending_close | file_channel.isOpen() == false) {
			throw new RuntimeException("Current channel is pending close or closed");
		}
		final Iterator<Item> i_item = items.filter(item -> {
			return item.getDeleteDate() + grace_period_for_expired_items >= System.currentTimeMillis();
		}).iterator();
		
		return CompletableFuture.supplyAsync(() -> {
			ArrayList<Item> result = new ArrayList<>();
			ArrayList<HistoryEntry> to_push_list = new ArrayList<>();
			
			while (true) {
				to_push_list.clear();
				while (i_item.hasNext() && to_push_list.size() < 10_000) {
					to_push_list.add(new HistoryEntry(i_item.next()));
				}
				if (to_push_list.isEmpty()) {
					break;
				}
				
				try {
					if (pending_close | file_channel.isOpen() == false) {
						throw new RuntimeException("Current channel is pending close or closed");
					}
					
					long expected_write_size = (long) ENTRY_SIZE * (long) to_push_list.size();
					MappedByteBuffer write_buffer;
					synchronized (file) {
						long channel_pos = file_channel.position();
						write_buffer = file_channel.map(MapMode.READ_WRITE, channel_pos, expected_write_size);
						
						to_push_list.forEach(h_e -> {
							h_e.write(write_buffer);
							result.add(h_e.item);
						});
						write_buffer.force();
						file_channel.position(channel_pos + expected_write_size);
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			return result;
		}, write_pool);
	}*/
	
	/**
	 * Thread safe and BLOCKING.
	 */
	Item writeSync(Item item) throws IOException {
		if (pending_close | file_channel.isOpen() == false) {
			throw new RuntimeException("Current channel is pending close or closed");
		}
		
		ByteBuffer write_buffer = ByteBuffer.allocate(ENTRY_SIZE);
		new HistoryEntry(item).write(write_buffer);
		write_buffer.flip();
		
		synchronized (file) {
			if (pending_close | file_channel.isOpen() == false) {
				throw new RuntimeException("Current channel is pending close or closed");
			}
			/**
			 * Always appends
			 */
			int writed_size = file_channel.write(write_buffer, file_channel.size());
			if (writed_size != ENTRY_SIZE) {
				throw new IOException("Can't write in history journal (" + writed_size + "/" + ENTRY_SIZE + ")");
			}
		}
		return item;
	}
	
	@GsonIgnore
	public class HistoryEntry {
		public final long update_date;
		public final long delete_date;
		public final ItemKey key;
		public final int data_size;
		public final byte[] data_digest;
		
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
		}
		
		private HistoryEntry(Item item) {
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
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((key == null) ? 0 : key.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof HistoryEntry)) {
				return false;
			}
			HistoryEntry other = (HistoryEntry) obj;
			if (!getOuterType().equals(other.getOuterType())) {
				return false;
			}
			if (key == null) {
				if (other.key != null) {
					return false;
				}
			} else if (!key.equals(other.key)) {
				return false;
			}
			return true;
		}
		
		private HistoryJournal getOuterType() {
			return HistoryJournal.this;
		}
		
	}
	
	/**
	 * Thread safe. Filter out expired and updated before-delete-grace-period entries.
	 * Should not be used for a get *all* entries, only recents entries like max(start_date, now - grace period).
	 * Can get multiple versions for same Itemkey, and not necessarily date sorted.
	 * BLOCKING
	 */
	public Stream<HistoryEntry> getAllSince(long start_date) throws IOException {
		synchronized (file) {
			long datas_to_read = file_channel.size() - oldest_valid_recorded_value_position;
			if (datas_to_read <= 0) {
				return Stream.empty();
			}
			if (log.isTraceEnabled()) {
				log.trace("Prepare to read " + datas_to_read + " bytes of datas from " + oldest_valid_recorded_value_position);
			}
			ByteBuffer read_buffer = ByteBuffer.allocate((int) datas_to_read);
			file_channel.read(read_buffer, oldest_valid_recorded_value_position);
			read_buffer.flip();
			
			StreamMaker<HistoryEntry> s_m = StreamMaker.create(() -> {
				try {
					while (read_buffer.remaining() >= ENTRY_SIZE) {
						return new HistoryEntry(read_buffer);
					}
					return null;
				} catch (Exception e) {
					throw new RuntimeException("Can't read " + file, e);
				}
			});
			
			return s_m.stream().filter(h_e -> {
				return h_e.update_date >= start_date;
			}).filter(h_e -> {
				return h_e.delete_date + grace_period_for_expired_items >= System.currentTimeMillis();
			});
		}
	}
	
	/*
	 * Thread safe
	 * Ignore actual deleted and expired values.
	 * @param include_oldest_entries based on last defragment measure, or just file size.
	 */
	/*public int getEntryCount(boolean include_oldest_entries) throws IOException {
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
		
		if (width % (long) ENTRY_SIZE != 0) {
			throw new IOException("Invalid file size (file is currently writed ?)");
		}
		
		return (int) (width / (long) ENTRY_SIZE);
	}*/
	
	/**
	 * Can take time...
	 * Thread safe
	 */
	void defragment() throws IOException {
		synchronized (file) {
			long actual_garbadge_size = oldest_valid_recorded_value_position - (long) HEADER_LENGTH;
			if (actual_garbadge_size < max_losted_data_space_size) {
				/**
				 * SetOldestValidRecordedValuePosition: search the last expired entry.
				 */
				
				final int offset_read = (int) oldest_valid_recorded_value_position;
				long datas_to_read = file_channel.size() - oldest_valid_recorded_value_position;
				if (log.isTraceEnabled()) {
					log.trace("Prepare to read " + datas_to_read + " bytes of datas from " + oldest_valid_recorded_value_position);
				}
				ByteBuffer read_buffer = ByteBuffer.allocate((int) datas_to_read);
				file_channel.read(read_buffer, offset_read);
				read_buffer.flip();
				
				Predicate<HistoryEntry> isEntryExpired = entry -> {
					if (entry.delete_date + grace_period_for_expired_items >= System.currentTimeMillis()) {
						/**
						 * Not expired item
						 */
						return false;
					} else if (entry.update_date + grace_period_for_expired_items >= System.currentTimeMillis()) {
						/**
						 * Not too "old" item
						 */
						return false;
					} else {
						return true;
					}
				};
				
				while (read_buffer.remaining() >= ENTRY_SIZE) {
					oldest_valid_recorded_value_position = offset_read + read_buffer.position();
					if (isEntryExpired.test(new HistoryEntry(read_buffer)) == false) {
						break;
					}
				}
				
				actual_garbadge_size = oldest_valid_recorded_value_position - (long) HEADER_LENGTH;
				if (actual_garbadge_size < max_losted_data_space_size) {
					while (read_buffer.remaining() >= ENTRY_SIZE & read_buffer.remaining() > (int) (max_losted_data_space_size - actual_garbadge_size) & actual_garbadge_size < max_losted_data_space_size) {
						if (isEntryExpired.test(new HistoryEntry(read_buffer))) {
							actual_garbadge_size += (long) ENTRY_SIZE;
						}
					}
					read_buffer.clear();
					
					if (actual_garbadge_size < max_losted_data_space_size) {
						/**
						 * Ok, not too many garbadge.
						 */
						return;
					} else {
						/**
						 * After read next entries, there are still too many losted datas in whole file: do a real defrag.
						 */
					}
				} else {
					/**
					 * After read first entries, there are too many losted datas here: do a real defrag.
					 */
				}
			} else {
				/**
				 * Actually, too many losted datas in first entries: do a real defrag.
				 */
			}
			
			FreeDiskSpaceWarningException.check(file.getParentFile(), file.length() * 2l);
			
			file_channel.force(true);
			file_channel.close();
			
			File new_old = new File(file.getAbsolutePath() + ".cleanup");
			FileBackend.rotateFiles(file, new_old);
			open();
			HistoryJournal old_history = new HistoryJournal(new_old, grace_period_for_expired_items, 1);
			
			List<HistoryEntry> to_push = old_history.getAllSince(0).collect(Collectors.toList());
			
			if (to_push.isEmpty() == false) {
				ByteBuffer write_buffer = ByteBuffer.allocate(to_push.size() * ENTRY_SIZE);
				to_push.forEach(entry -> {
					entry.write(write_buffer);
				});
				write_buffer.flip();
				file_channel.write(write_buffer);
			}
			
			FileBackend.truncateFile(new_old);
			oldest_valid_recorded_value_position = HEADER_LENGTH;
		}
	}
	
	void clear() throws IOException {
		synchronized (file) {
			file_channel.truncate(HEADER_LENGTH);
			file_channel.force(true);
		}
	}
	
}
