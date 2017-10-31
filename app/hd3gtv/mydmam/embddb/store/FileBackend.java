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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.embddb.store.FileData.Entry;
import hd3gtv.tools.CopyMove;
import hd3gtv.tools.FreeDiskSpaceWarningException;
import hd3gtv.tools.ThreadPoolExecutorFactory;

public class FileBackend {
	
	private static Logger log = Logger.getLogger(FileBackend.class);
	
	private final File base_directory;
	private final ArrayList<StoreBackend> backends;
	private final Consumer<ItemKey> previous_pushed_items_in_journals;
	private final ThreadPoolExecutorFactory history_journal_write_pool;
	
	public FileBackend(File base_directory, Consumer<ItemKey> previous_pushed_items_in_journals, ThreadPoolExecutorFactory history_journal_write_pool) throws IOException {
		this.base_directory = base_directory;
		if (base_directory == null) {
			throw new NullPointerException("\"base_directory\" can't to be null");
		}
		this.previous_pushed_items_in_journals = previous_pushed_items_in_journals;
		if (previous_pushed_items_in_journals == null) {
			throw new NullPointerException("\"previous_pushed_items_in_journals\" can't to be null");
		}
		this.history_journal_write_pool = history_journal_write_pool;
		if (history_journal_write_pool == null) {
			throw new NullPointerException("\"history_journal_write_pool\" can't to be null");
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
	
	public File getBaseDirectory() {
		return base_directory;
	}
	
	/**
	 * Usable for tests
	 */
	FileBackend(File base_directory) throws IOException {
		this(base_directory, key -> {
		}, new ThreadPoolExecutorFactory("History journal", Thread.NORM_PRIORITY).setSimplePoolSize());
	}
	
	/**
	 * Thread safe
	 * @param table_size an estimation, will be corrected to 1+2^n
	 * @return a new backend, or the same as previousely created
	 */
	StoreBackend get(String database_name, String class_name, int table_size, long grace_period_for_expired_items) throws IOException {
		synchronized (backends) {
			return backends.stream().filter(b -> {
				return b.database_name.equals(database_name);
			}).filter(b -> {
				return b.class_name.equals(class_name);
			}).findFirst().orElseGet(() -> {
				try {
					StoreBackend result = new StoreBackend(database_name, class_name, FileHashTable.computeHashTableBestSize(table_size), grace_period_for_expired_items);
					backends.add(result);
					return result;
				} catch (IOException e) {
					throw new RuntimeException("Can't create backend", e);
				}
			});
		}
	}
	
	/**
	 * Add 1MB to expected_data_size_to_write
	 */
	void checkFreeSpace(long expected_data_size_to_write) throws IOException {
		FreeDiskSpaceWarningException.check(base_directory, expected_data_size_to_write + (1_024l * 1_024l));
	}
	
	class StoreBackend {
		private String database_name;
		private String class_name;
		
		private final TransactionJournal transaction_journal;
		private final HistoryJournal history_journal;
		
		private FileHashTableData data_hash_table;
		private FileIndexDates expiration_dates;
		private FileIndexPaths index_paths;
		
		private final File index_file;
		private final File data_file;
		private final File expiration_dates_file;
		private final File index_paths_file;
		private final File index_paths_llists_file;
		private final File transaction_journal_file;
		
		private final File old_index_file;
		private final File old_data_file;
		private final File old_expiration_dates_file;
		private final File old_index_paths_file;
		private final File old_index_paths_llists_file;
		
		private StoreBackend(String database_name, String class_name, int default_table_size, long grace_period_for_expired_items) throws IOException {
			this.database_name = database_name;
			if (database_name == null) {
				throw new NullPointerException("\"database_name\" can't to be null");
			}
			this.class_name = class_name;
			if (class_name == null) {
				throw new NullPointerException("\"class_name\" can't to be null");
			}
			if (default_table_size < 1) {
				throw new NullPointerException("\"default_table_size\" can't to be < 1 (" + default_table_size + ")");
			}
			if (grace_period_for_expired_items < 1) {
				throw new NullPointerException("\"grace_period_for_expired_items\" can't to be < 1 (" + grace_period_for_expired_items + ")");
			}
			
			index_file = makeFile("index.myhshtable");
			data_file = makeFile("data.mydatalist");
			expiration_dates_file = makeFile("expiration_dates.myhshtable");
			index_paths_file = makeFile("index_paths.myhshtable");
			index_paths_llists_file = makeFile("index_paths_llists.myllist");
			transaction_journal_file = makeFile("transaction_journal.myjournal");
			
			history_journal = new HistoryJournal(history_journal_write_pool, makeDir(), grace_period_for_expired_items, default_table_size);
			transaction_journal = new TransactionJournal(transaction_journal_file);
			data_hash_table = new FileHashTableData(index_file, data_file, default_table_size);
			expiration_dates = new FileIndexDates(expiration_dates_file, default_table_size);
			index_paths = new FileIndexPaths(index_paths_file, index_paths_llists_file, default_table_size);
			doDurableWritesAndRotateJournal().forEach(previous_pushed_items_in_journals);
			
			old_index_file = new File(index_file.getPath() + ".cleanup");
			old_data_file = new File(data_file.getPath() + ".cleanup");
			old_expiration_dates_file = new File(expiration_dates_file.getPath() + ".cleanup");
			old_index_paths_file = new File(index_paths_file.getPath() + ".cleanup");
			old_index_paths_llists_file = new File(index_paths_llists_file.getPath() + ".cleanup");
		}
		
		public String toString() {
			return database_name + " for " + class_name;
		}
		
		void close() {
			backends.remove(this);
			try {
				history_journal.close();
			} catch (IOException e) {
				log.error("Can't close history_journal for " + toString());
			}
			try {
				transaction_journal.close();
			} catch (IOException e) {
				log.error("Can't close journal for " + toString());
			}
			try {
				data_hash_table.close();
			} catch (IOException e) {
				log.error("Can't close data_hash_table for " + toString());
			}
			try {
				expiration_dates.close();
			} catch (IOException e) {
				log.error("Can't close expiration_dates for " + toString());
			}
			try {
				index_paths.close();
			} catch (IOException e) {
				log.error("Can't close index_paths for " + toString());
			}
		}
		
		File makeFile(String name) {
			return new File(base_directory.getPath() + File.separator + database_name + File.separator + class_name + File.separator + name);
		}
		
		File makeDir() throws IOException {
			File new_dir = new File(base_directory.getPath() + File.separator + database_name + File.separator + class_name);
			FileUtils.forceMkdir(new_dir);
			return new_dir;
		}
		
		/**
		 * Thread safe
		 */
		void writeInJournal(Item item, long expiration_date) throws IOException {
			checkFreeSpace(item.getByteBufferWriteSize());
			transaction_journal.write(item.getKey(), item, expiration_date, item.getPath());
			history_journal.writeSync(item);
		}
		
		/**
		 * NOT Thread safe
		 * @return updated keys
		 */
		List<ItemKey> doDurableWritesAndRotateJournal() throws IOException {
			
			CompletableFuture<Void> cf_history_journal_defragment = CompletableFuture.runAsync(() -> {
				try {
					history_journal.defragment();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}, history_journal_write_pool);
			
			/**
			 * To protect future writes... it will throw a NPE
			 */
			HashMap<ItemKey, Long> all_last_record_dates = new HashMap<>();
			
			/**
			 * 1st pass: get and compare dates
			 */
			transaction_journal.readAll(true).forEach(entry -> {
				if (all_last_record_dates.containsKey(entry.key)) {
					long actual_date = all_last_record_dates.get(entry.key);
					if (entry.expiration_date > actual_date) {
						all_last_record_dates.put(entry.key, entry.expiration_date);
					}
				} else {
					all_last_record_dates.put(entry.key, entry.expiration_date);
				}
			});
			
			HashMap<ItemKey, HashSet<ItemKey>> all_actual_paths_keys = index_paths.getAll(entry_key -> {
				return true;
			});
			
			/**
			 * 2nd pass: do writes
			 */
			List<ItemKey> all_updated_keys = transaction_journal.readAll(false).map(entry -> {
				if (all_last_record_dates.containsKey(entry.key) == false) {
					throw new NullPointerException("Can't found key " + entry.key + ", invalid journal read/update during reading");
				}
				
				if (entry.expiration_date != all_last_record_dates.get(entry.key)) {
					/**
					 * Get only the last record, and write only this last.
					 */
					return null;
				}
				try {
					checkFreeSpace(entry.data_export_source.getByteBufferWriteSize());
					
					data_hash_table.put(entry.key, entry.data_export_source);
					expiration_dates.put(entry.key, entry.expiration_date);
					FileIndexPaths.update(entry.key, entry.path, all_actual_paths_keys);
					return entry.key;
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}).distinct().collect(Collectors.toList());
			
			index_paths.setAll(all_actual_paths_keys);
			transaction_journal.clear();
			
			try {
				cf_history_journal_defragment.get();
			} catch (InterruptedException | ExecutionException e) {
				throw new IOException("Can't defragment history journal", e);
			}
			
			return all_updated_keys;
		}
		
		long getDataFileSize() {
			return index_file.length() + data_file.length();
		}
		
		long getIndexPathFileSize() {
			return index_paths_llists_file.length();
		}
		
		/**
		 * NOT Thread safe
		 * Don't touch to actual journals (transaction and history). Also remove outdated records.
		 */
		void cleanUpFiles() throws IOException {
			data_hash_table.close();
			expiration_dates.close();
			index_paths.close();
			
			rotateFiles(index_file, old_index_file);
			rotateFiles(data_file, old_data_file);
			rotateFiles(expiration_dates_file, old_expiration_dates_file);
			rotateFiles(index_paths_file, old_index_paths_file);
			rotateFiles(index_paths_llists_file, old_index_paths_llists_file);
			
			FileHashTableData old_data_hash_table = new FileHashTableData(old_index_file, old_data_file, 0);
			FileIndexDates old_expiration_dates = new FileIndexDates(old_expiration_dates_file, 0);
			FileIndexPaths old_index_paths = new FileIndexPaths(old_index_paths_file, old_index_paths_llists_file, 0);
			
			int size = FileHashTable.computeHashTableBestSize(old_data_hash_table.size());
			
			data_hash_table = new FileHashTableData(index_file, data_file, size);
			expiration_dates = new FileIndexDates(expiration_dates_file, size);
			index_paths = new FileIndexPaths(index_paths_file, index_paths_llists_file, size);
			
			old_data_hash_table.streamKeyValue().forEach(item -> {
				try {
					long expiration_date = old_expiration_dates.get(item.key);
					if (expiration_date == 0) {
						if (old_expiration_dates.has(item.key) == false) {
							throw new NullPointerException("Can't found a valid expiration_date for " + item.key);
						}
					}
					if (expiration_date < System.currentTimeMillis()) {
						return;
					}
					checkFreeSpace(item.data.remaining());
					
					data_hash_table.put(item.key, item.data);
					expiration_dates.put(item.key, expiration_date);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			
			HashMap<ItemKey, HashSet<ItemKey>> all_path = old_index_paths.getAll(entry_key -> {
				try {
					return data_hash_table.has(entry_key);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			
			index_paths.setAll(all_path);
			
			old_data_hash_table.close();
			old_expiration_dates.close();
			old_index_paths.close();
			
			truncateFile(old_index_file);
			truncateFile(old_data_file);
			truncateFile(old_expiration_dates_file);
			truncateFile(old_index_paths_file);
			truncateFile(old_index_paths_llists_file);
		}
		
		/**
		 * Thread safe
		 * @return raw content
		 */
		ByteBuffer read(ItemKey key) throws IOException {
			if (expiration_dates.get(key) < System.currentTimeMillis()) {
				return null;
			}
			Entry result = data_hash_table.getEntry(key);
			if (result == null) {
				return null;
			}
			return result.data;
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
		Stream<ByteBuffer> getAllDatas() throws IOException {
			return data_hash_table.streamKeyValue().filter(entry -> {
				try {
					return expiration_dates.get(entry.key) > System.currentTimeMillis();
				} catch (IOException e) {
					throw new RuntimeException("Can't read in expiration_dates file", e);
				}
			}).map(entry -> {
				return entry.data;
			});
		}
		
		/**
		 * Thread safe
		 * @return raw content
		 */
		Stream<ByteBuffer> getDatasByPath(String path) throws IOException {
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
		
		/**
		 * NOT Thread safe
		 */
		void clear() throws IOException {
			data_hash_table.clear();
			expiration_dates.clear();
			index_paths.clear();
			transaction_journal.clear();
			history_journal.clear();
		}
		
		public HistoryJournal getHistoryJournal() {
			return history_journal;
		}
	}
	
	/**
	 * Exchange actual and new_old content (triple rename), and truncate *actual* (old new).
	 */
	static void rotateFiles(File actual, File new_old) throws IOException {
		if (new_old.exists() == false) {
			FileUtils.moveFile(actual, new_old);
			return;
		}
		
		truncateFile(new_old);
		File rotate = new File(actual.getParent() + File.separator + "rotate");
		if (rotate.exists()) {
			throw new IOException("Rotate file can't exists !");
		}
		FileUtils.moveFile(actual, rotate);
		FileUtils.moveFile(new_old, actual);
		FileUtils.moveFile(rotate, new_old);
	}
	
	static void truncateFile(File file) throws IOException {
		FileChannel channel = FileChannel.open(file.toPath(), MyDMAM.OPEN_OPTIONS_FILE_EXISTS);
		channel.truncate(0);
		channel.close();
	}
	
}
