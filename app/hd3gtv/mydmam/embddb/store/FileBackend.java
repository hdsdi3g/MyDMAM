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
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;

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
	
	StoreBackend create(String database_name, String class_name, int table_size) throws IOException {
		synchronized (backends) {
			StoreBackend result = new StoreBackend(database_name, class_name, table_size);
			backends.add(result);
			return result;
		}
	}
	
	class StoreBackend {
		private String database_name;
		private String class_name;
		
		private TransactionJournal journal;
		private FileHashTable hash_table;
		
		private StoreBackend(String database_name, String class_name, int table_size) throws IOException {
			this.database_name = database_name;
			if (database_name == null) {
				throw new NullPointerException("\"database_name\" can't to be null");
			}
			this.class_name = class_name;
			if (class_name == null) {
				throw new NullPointerException("\"class_name\" can't to be null");
			}
			
			File journal_directory = new File(base_directory.getPath() + File.separator + database_name + File.separator + class_name + File.separator + "journal");
			FileUtils.forceMkdir(journal_directory);
			journal = new TransactionJournal(journal_directory, instance);
			
			File index_file = new File(base_directory.getPath() + File.separator + database_name + File.separator + class_name + File.separator + "index.myhshtable");
			File data_file = new File(base_directory.getPath() + File.separator + database_name + File.separator + class_name + File.separator + "data.myhshtable");
			hash_table = new FileHashTable(index_file, data_file, table_size);
		}
		
		void writeInCommitlog(ItemKey key, byte[] content) throws IOException {
			// TODO
		}
		
		void rotateAndReadCommitlog(BiConsumer<ItemKey, byte[]> all_reader) throws IOException {
			// TODO
			
		}
		
		void writeInDatabase(ItemKey key, byte[] content, String _id, String path, long delete_date) throws IOException {
			// TODO
		}
		
		/**
		 * Remove all for delete_date < Now - grace_period
		 */
		void removeOutdatedRecordsInDatabase(long grace_period) throws IOException {
			// TODO
		}
		
		/**
		 * @return raw content
		 */
		byte[] read(ItemKey key) throws IOException {
			return null;// TODO
		}
		
		boolean contain(ItemKey key) throws IOException {
			return false;// TODO
		}
		
		/**
		 * @return raw content
		 */
		Stream<byte[]> getAllDatas() throws IOException {
			return null; // TODO
		}
		
		/**
		 * @return raw content
		 */
		Stream<byte[]> getDatasByPath(String path) throws IOException {
			return null;// TODO
		}
	}
	
	// TODO on close hook : flush all
	// TODO unit tests
}
