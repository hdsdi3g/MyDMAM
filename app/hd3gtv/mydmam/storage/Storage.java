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

import org.apache.commons.io.FileUtils;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.storage.StorageLocalFile.Localfile;
import hd3gtv.tools.CopyMove;

public abstract class Storage {
	
	private static final List<Storage> declared_storages;
	private static final Map<String, Storage> declared_storages_by_name;
	private static final ArrayList<String> declared_storages_local_access;
	private static final List<Storage> regular_indexing_storages;
	
	static {
		declared_storages = new ArrayList<Storage>();
		declared_storages_by_name = new HashMap<String, Storage>();
		declared_storages_local_access = new ArrayList<String>();
		regular_indexing_storages = new ArrayList<Storage>();
		
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
					storage.mounted = new File((String) storage_def.get("mounted")).getCanonicalFile();
					CopyMove.checkExistsCanRead(storage.mounted);
					CopyMove.checkIsDirectory(storage.mounted);
				}
				
				registerStorage(storage);
			} catch (Exception e) {
				Loggers.Storage.error("Can't setup storage, check configuration", e);
			}
		}
		
	}
	
	public static void registerStorage(Storage new_storage) {
		declared_storages.add(new_storage);
		declared_storages_by_name.put(new_storage.name, new_storage);
		
		if (new_storage instanceof StorageLocalFile) {
			declared_storages_local_access.add(new_storage.name);
		} else if (new_storage.mounted != null) {
			declared_storages_local_access.add(new_storage.name);
		}
		
		if (new_storage.regular_indexing && (new_storage.period > 0)) {
			regular_indexing_storages.add(new_storage);
		}
	}
	
	public static Storage getByName(String storage_name) {
		if (declared_storages_by_name.containsKey(storage_name) == false) {
			throw new NullPointerException("Can't found \"" + storage_name + "\" storage in configuration.");
		}
		return declared_storages_by_name.get(storage_name);
	}
	
	/**
	 * @return null if storage can't provide a File
	 */
	public static File getLocalFile(SourcePathIndexerElement element) {
		if (element == null) {
			return null;
		}
		if (declared_storages_local_access.contains(element.storagename) == false) {
			return null;
		}
		
		File base_path = null;
		Storage storage = declared_storages_by_name.get(element.storagename);
		if (storage instanceof StorageLocalFile) {
			base_path = ((StorageLocalFile) storage).getRoot();
		} else {
			base_path = storage.mounted;
		}
		
		if (base_path == null) {
			return null;
		}
		return new File(base_path.getPath() + element.currentpath.replaceAll("/", File.separator));
	}
	
	/**
	 * Storage File based, or mounted storages on localhost.
	 */
	public static ArrayList<String> getLocalAccessStoragesName() {
		return declared_storages_local_access;
	}
	
	public static ArrayList<String> getAllStoragesNames() {
		return new ArrayList<String>(declared_storages_by_name.keySet());
	}
	
	public static void testAllStoragesConnection() {
		for (int pos = 0; pos < declared_storages.size(); pos++) {
			try {
				declared_storages.get(pos).testStorageConnection();
			} catch (Exception e) {
				Loggers.Storage.error("Fail to test storage: " + declared_storages.get(pos).name, e);
			}
		}
	}
	
	public static boolean hasRegularIndexing() {
		return regular_indexing_storages.isEmpty() == false;
	}
	
	public static List<Storage> getRegularIndexingStorages() {
		return regular_indexing_storages;
	}
	
	/**
	 * Start dynamic zone...
	 */
	private String name;
	private boolean regular_indexing;
	private int period;
	private File mounted;
	
	protected Storage overloadInternalParams(String name, boolean regular_indexing, int period, File mounted) throws IOException {
		this.name = name;
		this.regular_indexing = regular_indexing;
		this.period = period;
		this.mounted = mounted;
		if (mounted != null) {
			CopyMove.checkExistsCanRead(mounted);
			CopyMove.checkIsDirectory(mounted);
		}
		return this;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("name: ");
		sb.append(name);
		sb.append(", type: ");
		sb.append(getClass().getSimpleName());
		if (regular_indexing) {
			sb.append(", regular_indexing: ");
			sb.append(regular_indexing);
		}
		if (period > 0) {
			sb.append(", period: ");
			sb.append(period);
		}
		if (mounted != null) {
			sb.append(", mounted: ");
			sb.append(mounted);
		}
		return sb.toString();
	}
	
	public String getName() {
		return name;
	}
	
	/**
	 * In sec
	 */
	public int getPeriod() {
		return period;
	}
	
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
	public void dirList(StorageCrawler listing) {
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
			
			if (listing.onStartSearch(name, root_path) == false) {
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
			Loggers.Storage.error("Error while open an access to storagename: " + name, e);
		}
		listing.onEndSearch();
	}
	
	private boolean recursiveDirectorySearch(List<AbstractFile> files, StorageCrawler listing, IgnoreFiles rules, int width_crawl_allowed) throws IOException {
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
		
		File local_file;
		
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
			
			if (files.get(pos) instanceof Localfile) {
				local_file = ((Localfile) files.get(pos)).getRealFile();
				if (FileUtils.isSymlink(local_file)) {
					/**
					 * This file is a local file, and it's a symlink...
					 */
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
