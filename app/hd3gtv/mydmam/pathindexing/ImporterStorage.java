/*
 * This file is part of MyDMAM
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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam.pathindexing;

import hd3gtv.log2.Log2;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.storage.AbstractFile;
import hd3gtv.storage.IgnoreFiles;
import hd3gtv.storage.StorageListing;
import hd3gtv.storage.StorageManager;

import org.elasticsearch.client.Client;

public class ImporterStorage extends Importer {
	
	private String storagename;
	private String poolname;
	private long ttl;
	private boolean stop;
	
	public ImporterStorage(Client client, String storagename, String poolname, long ttl) {
		super(client);
		this.storagename = storagename;
		if (storagename == null) {
			throw new NullPointerException("\"storagename\" can't to be null");
		}
		this.poolname = poolname;
		if (poolname == null) {
			throw new NullPointerException("\"poolname\" can't to be null");
		}
		this.ttl = ttl;
	}
	
	public synchronized void stopScan() {
		stop = true;
	}
	
	protected String getName() {
		return poolname;
	}
	
	private class Listing implements StorageListing {
		
		IndexingEvent elementpush;
		long count = 0;
		ImporterStorage referer;
		
		public Listing(ImporterStorage referer) {
			this.referer = referer;
			referer.stop = false;
		}
		
		public boolean onFoundFile(AbstractFile file, String storagename) {
			if (referer.stop == true) {
				return false;
			}
			SourcePathIndexerElement element = new SourcePathIndexerElement();
			element.date = file.lastModified();
			element.dateindex = System.currentTimeMillis();
			element.currentpath = file.getPath();
			element.directory = file.isDirectory();
			if (file.getPath().equals("/") == false) {
				element.parentpath = element.currentpath.substring(0, element.currentpath.length() - (file.getName().length() + 1));
				if (element.parentpath.equals("")) {
					element.parentpath = "/";
				}
			}
			if (element.directory == false) {
				element.size = file.length();
				element.id = MyDMAM.getIdFromFilename(file.getName());
			}
			element.storagename = poolname;
			
			try {
				if (elementpush.onFoundElement(element)) {
					count++;
				}
			} catch (Exception e) {
				Log2.log.error("Can't process found element", e);
			}
			return true;
		}
		
		public boolean isSearchIsRecursive() {
			return true;
		}
		
		public boolean canSelectfileInSearch() {
			return true;
		}
		
		public boolean canSelectdirInSearch() {
			return true;
		}
		
		public boolean canSelectHiddenInSearch() {
			return false;
		}
		
		public IgnoreFiles getRules() {
			return new IgnoreFiles() {
				
				public boolean isFileNameIsAllowed(String filename) {
					if (filename.endsWith(".lnk")) {
						return false;
					}
					if (filename.endsWith("desktop.ini")) {
						return false;
					}
					if (filename.endsWith(".DS_Store")) {
						return false;
					}
					if (filename.endsWith(".localized")) {
						return false;
					}
					if (filename.endsWith(".Icon")) {
						return false;
					}
					return true;
				}
				
				public boolean isDirNameIsAllowed(String dirname) {
					return true;
				}
			};
		}
		
		public void onEndSearch() {
		}
	}
	
	protected long doIndex(IndexingEvent elementpush) throws Exception {
		Listing listing = new Listing(this);
		listing.elementpush = elementpush;
		StorageManager.getGlobalStorage().dirList(listing, storagename);
		listing.elementpush.onFoundElement(SourcePathIndexerElement.prepareStorageElement(poolname));
		return listing.count;
	}
	
	protected long getTTL() {
		return ttl;
	}
}
