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
 * Copyright (C) hdsdi3g for hd3g.tv 13 sept. 2017
 * 
*/
package hd3gtv.mydmam.embddb.store;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;

import hd3gtv.mydmam.embddb.store.FileData.Entry;
import hd3gtv.tools.CopyMove;

public class FileBackend {
	
	private final File base_directory;
	private final ArrayList<StoreBackend> backends;
	private final UUID instance;
	
	public FileBackend(File base_directory, UUID instance) throws IOException {
		this.base_directory = base_directory;
		if (base_directory == null) {
			throw new NullPointerException("\"base_directory\" can't to be null");
		}
		this.instance = instance;
		if (instance == null) {
			throw new NullPointerException("\"instance\" can't to be null");
		}
		
		if (base_directory.exists()) {
			CopyMove.checkIsDirectory(base_directory);
			CopyMove.checkExistsCanRead(base_directory);
			CopyMove.checkIsWritable(base_directory);
		} else {
			FileUtils.forceMkdir(base_directory);
		}
		backends = new ArrayList<>();
	}
	
	/**
	 * @param table_size an estimation, will be corrected to 1+2^n
	 */
	StoreBackend create(String database_name, String class_name, int table_size) throws IOException {
		synchronized (backends) {
			StoreBackend result = new StoreBackend(database_name, class_name, FileHashTable.computeHashTableBestSize(table_size));
			backends.add(result);
			return result;
		}
	}
	
	class StoreBackend {
		private String database_name;
		private String class_name;
		private final int default_table_size;
		
		private File journal_directory;
		private TransactionJournal journal;
		private FileHashTableData data_hash_table;
		private FileIndexDates expiration_dates;
		private FileIndexPaths index_paths;
		
		private final File index_file;
		private final File data_file;
		private final File expiration_dates_file;
		private final File index_paths_file;
		private final File index_paths_llists_file;
		
		private StoreBackend(String database_name, String class_name, int default_table_size) throws IOException {
			this.database_name = database_name;
			if (database_name == null) {
				throw new NullPointerException("\"database_name\" can't to be null");
			}
			this.class_name = class_name;
			if (class_name == null) {
				throw new NullPointerException("\"class_name\" can't to be null");
			}
			this.default_table_size = default_table_size;
			if (default_table_size < 1) {
				throw new NullPointerException("\"default_table_size\" can't to be < 1 (" + default_table_size + ")");
			}
			
			journal_directory = makeFile("journal");
			FileUtils.forceMkdir(journal_directory);
			journal = new TransactionJournal(journal_directory, instance);
			
			index_file = makeFile("index.myhshtable");
			data_file = makeFile("data.mydatalist");
			expiration_dates_file = makeFile("expiration_dates.myhshtable");
			index_paths_file = makeFile("index_paths.myhshtable");
			index_paths_llists_file = makeFile("index_paths_llists.myllist");
			
			data_hash_table = new FileHashTableData(index_file, data_file, default_table_size);
			expiration_dates = new FileIndexDates(expiration_dates_file, default_table_size);
			index_paths = new FileIndexPaths(index_paths_file, index_paths_llists_file, default_table_size);
		}
		
		private File makeFile(String name) {
			return new File(base_directory.getPath() + File.separator + database_name + File.separator + class_name + File.separator + name);
		}
		
		/**
		 * Thread safe
		 */
		void writeInJournal(Item item, long expiration_date) throws IOException {
			journal.write(item.getKey(), item.toRawContent(), expiration_date, item.getPath());
		}
		
		/**
		 * NOT Thread safe
		 */
		void doDurableWriteAndRotateJournal() throws IOException {
			journal.channelClose();
			/**
			 * To protect future writes... it will throw a NPE
			 */
			journal = null;
			
			HashMap<ItemKey, Long> all_last_record_dates = new HashMap<>();
			
			/**
			 * 1st pass: get and compare dates
			 */
			TransactionJournal.allJournalsByDate(journal_directory).forEach(current_journal -> {
				try {
					current_journal.readAll(true).forEach(entry -> {
						if (all_last_record_dates.containsKey(entry.key)) {
							long actual_date = all_last_record_dates.get(entry.key);
							if (entry.date > actual_date) {
								all_last_record_dates.put(entry.key, entry.date);
							}
						} else {
							all_last_record_dates.put(entry.key, entry.date);
						}
					});
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			
			/**
			 * 2nd pass: do writes
			 */
			TransactionJournal.allJournalsByDate(journal_directory).forEach(current_journal -> {
				try {
					current_journal.readAll(false).forEach(entry -> {
						if (all_last_record_dates.containsKey(entry.key) == false) {
							throw new NullPointerException("Can't found key " + entry.key + ", invalid journal read/update during reading");
						}
						if (entry.date != all_last_record_dates.get(entry.key)) {
							/**
							 * Get only the last record, and write only this last.
							 */
							return;
						}
						try {
							data_hash_table.put(entry.key, entry.content);
							index_paths.add(entry.key, entry.path);
							expiration_dates.put(entry.key, entry.expiration_date);
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					});
					
					current_journal.delete();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			
			journal = new TransactionJournal(journal_directory, instance);
		}
		
		/**
		 * NOT Thread safe
		 * Don't touch to actual journal. Also remove outdated records.
		 */
		void cleanUpFiles(long grace_period_for_expired_items) throws IOException {
			data_hash_table.close();
			expiration_dates.close();
			index_paths.close();
			
			data_hash_table = null;
			expiration_dates = null;
			index_paths = null;
			
			FileUtils.forceDelete(expiration_dates_file);
			FileUtils.forceDelete(index_paths_file);
			FileUtils.forceDelete(index_paths_llists_file);
			
			File old_index_file = new File(index_file.getPath() + ".cleanup");
			File old_data_file = new File(data_file.getPath() + ".cleanup");
			FileHashTableData old_data_hash_table = new FileHashTableData(old_index_file, old_data_file, 0);
			
			int size = FileHashTable.computeHashTableBestSize(old_data_hash_table.size());
			
			data_hash_table = new FileHashTableData(index_file, data_file, size);
			expiration_dates = new FileIndexDates(expiration_dates_file, size);
			index_paths = new FileIndexPaths(index_paths_file, index_paths_llists_file, size);
			
			class EntryItem {
				Entry entry;
				Item item;
				
				EntryItem(Entry entry) {
					this.entry = entry;
					item = Item.fromRawContent(entry.value);
				}
			}
			
			old_data_hash_table.forEachKeyValue().map(entry -> {
				return new EntryItem(entry);
			}).filter(ei -> {
				return ei.item.getDeleteDate() + grace_period_for_expired_items > System.currentTimeMillis();
			}).forEach(ei -> {
				try {
					data_hash_table.put(ei.entry.key, ei.entry.value);
					expiration_dates.put(ei.entry.key, ei.item.getDeleteDate() + grace_period_for_expired_items);
					index_paths.add(ei.entry.key, ei.item.getPath());
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			
			old_data_hash_table.close();
			FileUtils.forceDelete(old_index_file);
			FileUtils.forceDelete(old_data_file);
		}
		
		/**
		 * Thread safe
		 * @return raw content
		 */
		byte[] read(ItemKey key) throws IOException {
			if (expiration_dates.get(key) < System.currentTimeMillis()) {
				return null;
			}
			Entry result = data_hash_table.getEntry(key);
			if (result == null) {
				return null;
			}
			return result.value;
		}
		
		/**
		 * Thread safe
		 */
		boolean contain(ItemKey key) throws IOException {
			if (expiration_dates.get(key) < System.currentTimeMillis()) {
				return false;
			}
			return data_hash_table.has(key);
		}
		
		/**
		 * Thread safe
		 * @return raw content, without expired items
		 */
		Stream<byte[]> getAllDatas() throws IOException {
			return data_hash_table.forEachKeyValue().filter(entry -> {
				try {
					return expiration_dates.get(entry.key) > System.currentTimeMillis();
				} catch (IOException e) {
					throw new RuntimeException("Can't read in expiration_dates file", e);
				}
			}).map(entry -> {
				return entry.value;
			});
		}
		
		/**
		 * Thread safe
		 * @return raw content
		 */
		Stream<byte[]> getDatasByPath(String path) throws IOException {
			return index_paths.getAllKeysInPath(path).map(item_key -> {
				try {
					return read(item_key);
				} catch (IOException e) {
					throw new RuntimeException("Can't read data", e);
				}
			}).filter(value -> {
				return value != null;
			});
		}
	}
	
	// TODO on close hook : flush all
	// TODO unit tests for this
}
