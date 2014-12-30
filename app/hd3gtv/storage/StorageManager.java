/*
 * This file is part of hd3g.tv' Java Storage Abstraction
 * 
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
*/
package hd3gtv.storage;

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.ConfigurationItem;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class StorageManager {
	
	private ArrayList<StorageConfigurator> list_storages;
	private HashMap<String, StorageConfigurator> map_storages;
	
	private static StorageManager global;
	
	public static void refreshGlobalStorage() {
		global = new StorageManager();
	}
	
	public static StorageManager getGlobalStorage() {
		try {
			if (global == null) {
				refreshGlobalStorage();
			}
		} catch (Exception e) {
			Log2.log.error("Can't load storage configuration", e);
		}
		return global;
	}
	
	public StorageManager() {
		
		if (Configuration.global.isElementExists("storage") == false) {
			throw new NullPointerException("No Storage configuration");
		}
		
		list_storages = new ArrayList<StorageConfigurator>();
		map_storages = new HashMap<String, StorageConfigurator>();
		
		HashMap<String, ConfigurationItem> conf_storages = Configuration.global.getElement("storage");
		
		if (conf_storages.isEmpty()) {
			Log2.log.info("No storages found in XML configuration");
			return;
		}
		
		for (Map.Entry<String, ConfigurationItem> entry : conf_storages.entrySet()) {
			importElement(entry.getKey(), conf_storages);
		}
		
		if (list_storages.isEmpty()) {
			Log2.log.info("No storages found in XML configuration");
			return;
		}
		
		Log2Dump dump = new Log2Dump();
		for (int pos = 0; pos < list_storages.size(); pos++) {
			dump.add(list_storages.get(pos).name, list_storages.get(pos));
		}
		Log2.log.info("Storages found and loaded from configuration", dump);
		
	}
	
	private void importElement(String storagename, HashMap<String, ConfigurationItem> conf_storages) {
		StorageConfigurator configurator = new StorageConfigurator();
		
		configurator.storagetype = StorageType.fromString(Configuration.getValue(conf_storages, storagename, "type", null));
		configurator.readonly = Configuration.getValueBoolean(conf_storages, storagename, "readonly");
		
		configurator.name = storagename.toLowerCase();
		configurator.host = Configuration.getValue(conf_storages, storagename, "host", null);
		
		configurator.username = Configuration.getValue(conf_storages, storagename, "username", null);
		configurator.password = Configuration.getValue(conf_storages, storagename, "password", null);
		
		configurator.path = Configuration.getValue(conf_storages, storagename, "path", "");
		if (configurator.path != null & configurator.path.equals("") == false) {
			if (configurator.path.startsWith("/") == false) {
				configurator.path = "/" + configurator.path;
			}
			if (configurator.path.endsWith("/") == false) {
				configurator.path = configurator.path + "/";
			}
		} else {
			if (configurator.storagetype == StorageType.file) {
				configurator.path = File.separator;
			} else {
				configurator.path = "/";
			}
		}
		
		if ((configurator.storagetype == StorageType.ftp) | (configurator.storagetype == StorageType.ftpnexio)) {
			configurator.port = Configuration.getValue(conf_storages, storagename, "port", 21);
			configurator.passive = Configuration.getValueBoolean(conf_storages, storagename, "passive");
		}
		list_storages.add(configurator);
		map_storages.put(configurator.name, configurator);
	}
	
	public void testAllStorages() {
		AbstractFile file;
		Log2Dump dump;
		for (int pos = 0; pos < list_storages.size(); pos++) {
			dump = new Log2Dump();
			dump.add(list_storages.get(pos).name, list_storages.get(pos));
			try {
				file = getRootPath(list_storages.get(pos).name);
				if (file.canRead() == false) {
					file.close();
					throw new IOException("Can't read");
				}
				
				file.close();
				Log2.log.info("Storage test ok", dump);
			} catch (Exception e) {
				Log2.log.error("Error during storage test", e, dump);
			}
		}
	}
	
	public void testIOForStorages(String... storagename) throws IOException, NullPointerException {
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
		
		for (int pos_sn = 0; pos_sn < storagename.length; pos_sn++) {
			root_path = null;
			test_dir = null;
			test_file = null;
			test_file_rename = null;
			test_dir_name = null;
			test_file_name = null;
			inputstream = null;
			outputstream = null;
			readsize = 0;
			
			try {
				root_path = getRootPath(storagename[pos_sn]);
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
				
			} catch (NullPointerException e) {
				throw e;
			} catch (IOException e) {
				Log2Dump dump = new Log2Dump();
				dump.add("storagename", storagename[pos_sn]);
				dump.add("root_path", root_path);
				dump.add("test_dir", test_dir);
				dump.add("test_file", test_file);
				dump.add("test_file_rename", test_file_rename);
				dump.add("test_dir_name", test_dir_name);
				dump.add("test_file_name", test_file_name);
				dump.add("readsize", readsize);
				
				Log2.log.error("Error with storage", e, dump);
				throw e;
			}
		}
	}
	
	/**
	 * Caution : AbstractFile is not thread safe !
	 */
	public AbstractFile getRootPath(String storagename) throws NullPointerException, IOException {
		StorageConfigurator configurator = map_storages.get(storagename);
		if (configurator == null) {
			throw new NullPointerException("Storage \"" + storagename + "\" don't exists in configuration");
		}
		if (configurator.storagetype == StorageType.file) {
			return new AbstractFileBridgeLocalfile(configurator);
		} else if (configurator.storagetype == StorageType.smb) {
			return new AbstractFileBridgeSmb(configurator);
		} else if (configurator.storagetype == StorageType.ftp) {
			return new AbstractFileBridgeFtp(configurator);
		} else if (configurator.storagetype == StorageType.ftpnexio) {
			return new AbstractFileBridgeFtpNexio(configurator);
		}
		return null;
	}
	
	/**
	 * Do a recusive dir listing and callback for each file
	 * A close() will be call.
	 */
	public void dirList(StorageListing listing, String storagename) {
		if (listing == null) {
			throw new NullPointerException("\"listing\" can't to be null");
		}
		if (storagename == null) {
			throw new NullPointerException("\"storagename\" can't to be null");
		}
		IgnoreFiles rules = listing.getRules();
		
		AbstractFile root_path = null;
		try {
			root_path = getRootPath(storagename);
			if (root_path.isDirectory() == false) {
				throw new IOException("Invalid Storage: " + storagename);
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
					AbstractFile[] files = new AbstractFile[1];
					files[0] = root_path;
					recursiveDirectorySearch(files, listing, storagename, rules, width_crawl_allowed);
				} else {
					recursiveDirectorySearch(root_path.listFiles(), listing, storagename, rules, width_crawl_allowed);
				}
			} else {
				listing.onNotFoundFile(root_path.getPath(), storagename);
			}
			root_path.close();
		} catch (NullPointerException e) {
			throw e;
		} catch (IOException e) {
			Log2Dump dump = new Log2Dump();
			dump.add("storagename", storagename);
			Log2.log.error("Error while open an access to storage", e, dump);
		}
		listing.onEndSearch();
	}
	
	private static boolean recursiveDirectorySearch(AbstractFile[] files, StorageListing listing, String storagename, IgnoreFiles rules, int width_crawl_allowed) {
		if (width_crawl_allowed == 0) {
			return false;
		}
		if (files == null) {
			return true;
		}
		if (listing == null) {
			return true;
		}
		if (storagename == null) {
			return true;
		}
		if (files.length == 0) {
			return true;
		}
		ArrayList<AbstractFile> dirs = new ArrayList<AbstractFile>();
		
		/**
		 * First, the files, and the dirs
		 */
		for (int pos = 0; pos < files.length; pos++) {
			if (files[pos].canRead() == false) {
				continue;
			}
			if (files[pos].isHidden() & (listing.canSelectHiddenInSearch() == false)) {
				continue;
			}
			if (rules != null) {
				if (files[pos].isFile() & rules.isFileNameIsAllowed(files[pos].getName()) == false) {
					continue;
				}
				if (files[pos].isDirectory() & rules.isDirNameIsAllowed(files[pos].getName()) == false) {
					continue;
				}
			}
			if (files[pos].isDirectory()) {
				if (listing.canSelectdirInSearch()) {
					if (listing.onFoundFile(files[pos], storagename) == false) {
						return false;
					}
				}
				if (listing.isSearchIsRecursive()) {
					dirs.add(files[pos]);
				}
			} else if (files[pos].isFile() & listing.canSelectfileInSearch()) {
				if (listing.onFoundFile(files[pos], storagename) == false) {
					return false;
				}
			}
		}
		
		/**
		 * After crawl to sub dirs.
		 */
		for (int pos_dirs = 0; pos_dirs < dirs.size(); pos_dirs++) {
			if (recursiveDirectorySearch(dirs.get(pos_dirs).listFiles(), listing, storagename, rules, width_crawl_allowed - 1) == false) {
				return false;
			}
		}
		dirs.clear();
		return true;
	}
}
