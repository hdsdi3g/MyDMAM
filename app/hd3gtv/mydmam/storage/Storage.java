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
 * Copyright (C) hdsdi3g for hd3g.tv 2015
 * 
*/
package hd3gtv.mydmam.storage;

import hd3gtv.configuration.Configuration;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.useraction.fileoperation.CopyMove;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class Storage {
	
	private static final List<Storage> declared_storages;
	private static final Map<String, Storage> declared_storages_by_name;
	
	static {
		declared_storages = new ArrayList<Storage>();
		List<LinkedHashMap<String, ?>> raw_defs = Configuration.global.getListMapValues("storage", "definitions");
		
		LinkedHashMap<String, ?> storage_def;
		Storage storage;
		for (int pos_l = 0; pos_l < raw_defs.size(); pos_l++) {
			storage_def = raw_defs.get(pos_l);
			try {
				storage = StorageType.getByDefinitionConfiguration(storage_def);
				if (storage == null) {
					throw new NullPointerException("Can't load storage");
				}
				storage.name = (String) storage_def.get("name");
				
				storage.regular_indexing = false;
				if (storage_def.containsKey("regular_indexing")) {
					storage.regular_indexing = (Boolean) storage_def.get("regular_indexing");
				}
				
				storage.period = 3600;
				if (storage_def.containsKey("period")) {
					storage.period = (Integer) storage_def.get("period");
				}
				
				if (storage_def.containsKey("mounted")) {
					storage.mounted = new File((String) storage_def.get("mounted"));
					CopyMove.checkExistsCanRead(storage.mounted);
					CopyMove.checkIsDirectory(storage.mounted);
				}
				declared_storages.add(storage);
			} catch (Exception e) {
				Log2.log.error("Can't setup storage, check configuration", e);
			}
		}
		
		declared_storages_by_name = new HashMap<String, Storage>();
		for (int pos = 0; pos < declared_storages.size(); pos++) {
			declared_storages_by_name.put(declared_storages.get(pos).name, declared_storages.get(pos));
		}
	}
	
	public static Storage getByName(String storage_name) {
		if (declared_storages_by_name.containsKey(storage_name) == false) {
			throw new NullPointerException("Can't found \"" + storage_name + "\" storage in configuration.");
		}
		return declared_storages_by_name.get(storage_name);
	}
	
	public static File getLocalBridgedElement(SourcePathIndexerElement element) {
		// TODO and rename
		if (element == null) {
			return null;
		}
		
		/*File base_path = bridge.get(element.storagename);
		if (base_path == null) {
			return null;
		}
		return new File(base_path.getPath() + element.currentpath);*/
		return null;
	}
	
	public static ArrayList<String> getBridgedStoragesName() {
		// TODO and rename
		return null;
	}
	
	public static void testAllStoragesConnection() {
		for (int pos = 0; pos < declared_storages.size(); pos++) {
			Log2Dump dump = new Log2Dump();
			try {
				dump = new Log2Dump();
				dump.add("name", declared_storages.get(pos).name);
				declared_storages.get(pos).testStorageConnection();
			} catch (Exception e) {
				Log2.log.error("Fail to test storage", e, dump);
			}
		}
	}
	
	/**
	 * Start dynamic zone...
	 */
	
	private String name;
	private boolean regular_indexing;
	private int period;
	private File mounted;
	
	/**
	 * Caution : AbstractFile is not thread safe !
	 * Don't forget to close it !
	 */
	public abstract AbstractFile getRootPath() throws NullPointerException, IOException;
	
	public void testStorageConnection() throws Exception {
		AbstractFile file;
		file = getRootPath();
		if (file.canRead() == false) {
			file.close();
			throw new IOException("Can't read");
		}
		file.close();
	}
	
	public void testStorageOperations() throws Exception {
		AbstractFile root_path;
		AbstractFile test_dir;
		AbstractFile test_file;
		AbstractFile test_file_rename;
		String test_dir_name;
		String test_file_name;
		InputStream inputstream;
		OutputStream outputstream;
		int readsize;
		byte[] datasource = ("This is a simple file test by MyDMAM at " + (new Date())).getBytes();
		byte[] buffer = new byte[1024];
		
		root_path = null;
		test_dir = null;
		test_file = null;
		test_file_rename = null;
		test_dir_name = null;
		test_file_name = null;
		inputstream = null;
		outputstream = null;
		readsize = 0;
		
		root_path = getRootPath();
		if (root_path == null) {
			throw new IOException("Can't open storage");
		}
		if (root_path.canRead() == false) {
			throw new IOException("Can't read to storage");
		}
		if (root_path.canWrite() == false) {
			throw new IOException("Can't write to storage");
		}
		test_dir_name = "/autotestwrite-" + String.valueOf(System.currentTimeMillis());
		test_dir = root_path.mkdir(test_dir_name);
		if (test_dir == null) {
			throw new IOException("Can't create new dir");
		}
		test_file_name = test_dir_name + "/" + "testfile.bin";
		test_file = root_path.getAbstractFile(test_file_name);
		if (test_file == null) {
			throw new IOException("Can't init an empty file");
		}
		
		outputstream = test_file.getOutputStream(1024);
		if (outputstream == null) {
			throw new IOException("Can't prepare file to write");
		}
		outputstream.write(datasource);
		outputstream.close();
		
		test_file_rename = test_file.moveTo(test_dir_name + "/" + "testfile_rename.txt");
		if (test_file_rename == null) {
			throw new IOException("Can't rename uploaded file");
		}
		
		if (test_file_rename.getName().equals("testfile_rename.txt") == false) {
			throw new IOException("Rename function don't work");
		}
		
		inputstream = test_file_rename.getInputStream(1024);
		if (inputstream == null) {
			throw new IOException("Can't prepare uploaded file to read");
		}
		readsize = inputstream.read(buffer);
		inputstream.close();
		
		if (readsize != datasource.length) {
			throw new IOException("The uploaded file hasn't the good size");
		}
		for (int pos_byte = 0; pos_byte < datasource.length; pos_byte++) {
			if (buffer[pos_byte] != datasource[pos_byte]) {
				throw new IOException("The uploaded file is corrupted");
			}
		}
		if (test_dir.delete() == false) {
			throw new IOException("Can't remove test directory");
		}
		
		root_path.close();
	}
	
	/**
	 * Do a recusive dir listing and callback for each file
	 * A close() will be call.
	 */
	public void dirList(StorageListing listing) {
		if (listing == null) {
			throw new NullPointerException("\"listing\" can't to be null");
		}
		IgnoreFiles rules = listing.getRules();
		
		AbstractFile root_path = null;
		try {
			root_path = getRootPath();
			if (root_path.isDirectory() == false) {
				throw new IOException("Invalid Storage: " + name);
			}
			
			String working_dir = listing.getCurrentWorkingDir();
			if (working_dir != null) {
				if (working_dir.startsWith("/")) {
					root_path = root_path.getAbstractFile(working_dir);
				}
			}
			
			if (listing.onStartSearch(root_path) == false) {
				root_path.close();
				return;
			}
			
			int width_crawl_allowed = listing.maxPathWidthCrawl();
			if (width_crawl_allowed == 0) {
				width_crawl_allowed = 1;
			}
			
			if (root_path.isFile() | root_path.isDirectory()) {
				if (root_path.isFile()) {
					ArrayList<AbstractFile> files = new ArrayList<AbstractFile>(1);
					files.add(root_path);
					recursiveDirectorySearch(files, listing, rules, width_crawl_allowed);
				} else {
					recursiveDirectorySearch(root_path.listFiles(), listing, rules, width_crawl_allowed);
				}
			} else {
				listing.onNotFoundFile(root_path.getPath(), name);
			}
			root_path.close();
		} catch (NullPointerException e) {
			throw e;
		} catch (IOException e) {
			Log2Dump dump = new Log2Dump();
			dump.add("storagename", name);
			Log2.log.error("Error while open an access to storage", e, dump);
		}
		listing.onEndSearch();
	}
	
	private boolean recursiveDirectorySearch(List<AbstractFile> files, StorageListing listing, IgnoreFiles rules, int width_crawl_allowed) {
		if (width_crawl_allowed == 0) {
			return false;
		}
		if (files == null) {
			return true;
		}
		if (listing == null) {
			return true;
		}
		if (files.size() == 0) {
			return true;
		}
		ArrayList<AbstractFile> dirs = new ArrayList<AbstractFile>();
		
		/**
		 * First, the files, and the dirs
		 */
		for (int pos = 0; pos < files.size(); pos++) {
			if (files.get(pos).canRead() == false) {
				continue;
			}
			if (files.get(pos).isHidden() & (listing.canSelectHiddenInSearch() == false)) {
				continue;
			}
			if (rules != null) {
				if (files.get(pos).isFile() & rules.isFileNameIsAllowed(files.get(pos).getName()) == false) {
					continue;
				}
				if (files.get(pos).isDirectory() & rules.isDirNameIsAllowed(files.get(pos).getName()) == false) {
					continue;
				}
			}
			if (files.get(pos).isDirectory()) {
				if (listing.canSelectdirInSearch()) {
					if (listing.onFoundFile(files.get(pos), name) == false) {
						return false;
					}
				}
				if (listing.isSearchIsRecursive()) {
					dirs.add(files.get(pos));
				}
			} else if (files.get(pos).isFile() & listing.canSelectfileInSearch()) {
				if (listing.onFoundFile(files.get(pos), name) == false) {
					return false;
				}
			}
		}
		
		/**
		 * After crawl to sub dirs.
		 */
		for (int pos_dirs = 0; pos_dirs < dirs.size(); pos_dirs++) {
			if (recursiveDirectorySearch(dirs.get(pos_dirs).listFiles(), listing, rules, width_crawl_allowed - 1) == false) {
				return false;
			}
		}
		dirs.clear();
		return true;
	}
	
}
