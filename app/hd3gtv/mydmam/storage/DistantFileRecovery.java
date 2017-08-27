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
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.metadata.container.ContainerOrigin;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.tools.CopyMove;
import hd3gtv.tools.FreeDiskSpaceWarningException;

public class DistantFileRecovery {
	
	private static File temp_directory;
	private static DistantFileRecovery instance;
	
	/**
	 * In sec
	 */
	private static long ttl;
	
	static {
		temp_directory = new File(Configuration.global.getValue("storage", "temp_directory", System.getProperty("java.io.tmpdir")));
		ttl = Configuration.global.getValue("storage", "temp_directory_ttl", 60 * 60);
		
		try {
			CopyMove.checkExistsCanRead(temp_directory);
			CopyMove.checkIsDirectory(temp_directory);
			CopyMove.checkIsWritable(temp_directory);
			if (temp_directory.getAbsolutePath().equals(new File(System.getProperty("java.io.tmpdir")).getAbsolutePath()) == false) {
				Loggers.Storage_DFR.debug("Clean " + temp_directory + " before operations");
				
				FileUtils.cleanDirectory(temp_directory);
			} else {
				Loggers.Storage_DFR.warn("Don't set default temp dir to storage.temp_directory (" + temp_directory + ")");
			}
		} catch (Exception e) {
			Loggers.Storage_DFR.warn("Invalid storage temp directory configuration: " + temp_directory, e);
			temp_directory = new File(System.getProperty("java.io.tmpdir"));
		}
		
		instance = new DistantFileRecovery();
	}
	
	private ConcurrentHashMap<String, Item> items;
	private Cleaner cleaner;
	
	private DistantFileRecovery() {
		items = new ConcurrentHashMap<String, DistantFileRecovery.Item>(1);
	}
	
	/**
	 * @param use_current_thread_status_to_check_the_needs_for_this_file: true if bypass TTL (delete is based on isAlive) OR false for delete after TTL countdown.
	 *        Beware: if true and if this current Thread is a endless loop, this file will be deleted only during JVM reboot !
	 * @return never null. Download if needed. Has a TTL ! Name based on MTD key.
	 */
	public static File getFile(SourcePathIndexerElement element, boolean use_current_thread_status_to_check_the_needs_for_this_file) throws Exception {
		FreeDiskSpaceWarningException.check(temp_directory, element.size);
		
		String base_name_unique_element_key = ContainerOrigin.fromSource(element, null).getUniqueElementKey();
		
		Item item = instance.items.putIfAbsent(base_name_unique_element_key, instance.new Item(base_name_unique_element_key));
		
		if (item == null) {
			Loggers.Storage_DFR.debug("Item [" + base_name_unique_element_key + "] is a new Item");
			item = instance.items.get(base_name_unique_element_key);
		}
		
		if (instance.cleaner == null) {
			instance.cleaner = instance.new Cleaner();
		} else if (instance.cleaner.isAlive() == false) {
			instance.cleaner = instance.new Cleaner();
		}
		
		if (item.is_downloaded()) {
			Loggers.Storage_DFR.debug("Item [" + base_name_unique_element_key + "] was previously downloaded");
			return item.update_activity(use_current_thread_status_to_check_the_needs_for_this_file);
		} else {
			if (item.is_downloading()) {
				Loggers.Storage_DFR.debug("Item [" + base_name_unique_element_key + "] is currently downloaded by another thread...");
				while (item.is_downloaded() == false) {
					Thread.sleep(100);
				}
				Loggers.Storage_DFR.debug("Item [" + base_name_unique_element_key + "] is just downloaded by another thread...");
				return item.update_activity(use_current_thread_status_to_check_the_needs_for_this_file);
			} else {
				Loggers.Storage_DFR.debug("Item [" + base_name_unique_element_key + "] is new, start download operation now");
				return item.download(element, use_current_thread_status_to_check_the_needs_for_this_file);
			}
		}
	}
	
	public static void manuallyReleaseFile(SourcePathIndexerElement element) {
		String base_name_unique_element_key = ContainerOrigin.fromSource(element, null).getUniqueElementKey();
		
		Item item = instance.items.get(base_name_unique_element_key);
		if (item != null) {
			instance.items.remove(base_name_unique_element_key);
			if (item.local_file != null) {
				if (item.local_file.exists()) {
					Loggers.Storage_DFR.debug("Manually release file, item [" + base_name_unique_element_key + "] will be deleted");
					item.deleteFile();
				}
			}
		}
	}
	
	public class Item {
		private String base_name_unique_element_key;
		private long created_date;
		private long last_activity;
		private File local_file;
		private String original_path;
		private boolean cant_download;
		private ArrayList<Thread> users;
		
		private Item(String base_name_unique_element_key) {
			created_date = System.currentTimeMillis();
			last_activity = created_date;
			cant_download = false;
			this.base_name_unique_element_key = base_name_unique_element_key;
			
			users = new ArrayList<Thread>(1);
		}
		
		private synchronized File download(SourcePathIndexerElement element, boolean linked_to_a_thread) throws Exception {
			try {
				Loggers.Storage_DFR.trace("Item [" + base_name_unique_element_key + "] start download");
				/**
				 * Unlock is_downloading()
				 */
				original_path = element.currentpath;
				
				String ext = FilenameUtils.getExtension(element.currentpath);
				if (ext.equals("") == false) {
					ext = "." + ext;
				}
				File _local_file = new File(temp_directory.getAbsolutePath() + File.separator + base_name_unique_element_key + ext);
				
				if (_local_file.exists() == false) {
					Loggers.Storage_DFR.trace("Item [" + base_name_unique_element_key + "] copy distant to local");
					AbstractFile distant_file = Storage.getByName(element.storagename).getRootPath().getAbstractFile(element.currentpath);
					FileUtils.copyInputStreamToFile(distant_file.getInputStream(0xFFFF), _local_file);
					distant_file.close();
				} else {
					Loggers.Storage_DFR.warn("Recover a distant file, but it exists in destination: " + _local_file);
				}
				
				update_activity(linked_to_a_thread);
				
				Loggers.Storage_DFR.trace("Item [" + base_name_unique_element_key + "] end download operation, " + _local_file.getPath() + ", " + _local_file.exists());
				/**
				 * Unlock is_downloaded()
				 */
				local_file = new File(_local_file.getCanonicalPath());
				
				return local_file;
			} catch (Exception e) {
				/**
				 * This will eject all is_downloading/is_downloaded
				 */
				cant_download = true;
				throw e;
			}
		}
		
		private synchronized boolean is_downloaded() throws FileNotFoundException {
			if (cant_download) {
				throw new FileNotFoundException("Can't download item");
			}
			return local_file != null;
		}
		
		private synchronized boolean is_downloading() throws FileNotFoundException {
			if (cant_download) {
				throw new FileNotFoundException("Can't download item");
			}
			return original_path != null;
		}
		
		private synchronized File update_activity(boolean linked_to_a_thread) {
			Loggers.Storage_DFR.trace("Item [" + base_name_unique_element_key + "] update activity");
			last_activity = System.currentTimeMillis();
			
			if (linked_to_a_thread) {
				Loggers.Storage_DFR.debug("Item [" + base_name_unique_element_key + "] is linked by this current Thread");
				if (users.contains(Thread.currentThread()) == false) {
					users.add(Thread.currentThread());
				}
			}
			
			return this.local_file;
		}
		
		private synchronized boolean isExpired() {
			if (cant_download) {
				if ((last_activity + (ttl * 1000l) < System.currentTimeMillis())) {
					Loggers.Storage_DFR.debug("Item [" + base_name_unique_element_key + "] is expired because: cant_download + last_activity<");
					if (local_file.exists()) {
						deleteFile();
					}
					return true;
				}
			} else if (local_file != null) {
				if (local_file.exists() == false) {
					Loggers.Storage_DFR.debug("Item [" + base_name_unique_element_key + "] is expired because: !local_file: " + local_file.getPath() + ", " + local_file.exists());
					return true;
				} else if ((last_activity + (ttl * 1000l) < System.currentTimeMillis())) {
					
					for (int pos = users.size() - 1; pos > -1; pos--) {
						if (users.get(pos).isAlive()) {
							/**
							 * Some threads who need this file are actually alive !
							 */
							Loggers.Storage_DFR.debug("Item [" + base_name_unique_element_key + "] is not expired because: " + users.get(pos).getName() + " is alive");
							return false;
						} else {
							users.remove(pos);
						}
					}
					
					Loggers.Storage_DFR.debug("Item [" + base_name_unique_element_key + "] is expired because: last_activity<");
					deleteFile();
					return true;
				}
			}
			if (original_path == null & (created_date + (60l * 1000l) < System.currentTimeMillis())) {
				Loggers.Storage_DFR.debug("Item was created [" + base_name_unique_element_key + "], but never downloaded");
				return true;
			}
			return false;
		}
		
		private void deleteFile() {
			try {
				Loggers.Storage_DFR.debug("Item [" + base_name_unique_element_key + "] will be deleted");
				FileUtils.forceDelete(local_file);
			} catch (Exception e) {
				Loggers.Storage_DFR.error("Can't delete distant recovery file", e);
			}
		}
	}
	
	private class Cleaner extends Thread {
		
		public Cleaner() {
			setName("DistantFileRecoveryCleaner");
			setDaemon(true);
			start();
		}
		
		public void run() {
			try {
				if (items.isEmpty()) {
					/**
					 * Too fast loading ?
					 */
					sleep(1000);
				}
				Loggers.Storage_DFR.debug("Start Cleaner");
				
				List<String> remove_this = new ArrayList<String>();
				
				while (items.isEmpty() == false) {
					for (Item item : items.values()) {
						if (item.isExpired()) {
							remove_this.add(item.base_name_unique_element_key);
						}
					}
					
					if (remove_this.isEmpty() == false) {
						for (int pos = 0; pos < remove_this.size(); pos++) {
							items.remove(remove_this.get(pos));
						}
						remove_this.clear();
					}
					
					sleep(10000);
				}
				
				Loggers.Storage_DFR.debug("DistantFileRecovery Cleaner has finish its job");
			} catch (Exception e) {
				Loggers.Storage_DFR.error("Error with Distant file recovery Cleaner", e);
			}
		}
	}
	
}
