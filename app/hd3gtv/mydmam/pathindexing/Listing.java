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
package hd3gtv.mydmam.pathindexing;

import hd3gtv.log2.Log2;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.storage.AbstractFile;
import hd3gtv.storage.IgnoreFiles;
import hd3gtv.storage.StorageListing;

class Listing implements StorageListing {
	
	IndexingEvent elementpush;
	long count = 0;
	ImporterStorage referer;
	
	public Listing(ImporterStorage referer) {
		this.referer = referer;
		referer.stop = false;
	}
	
	public void onNotFoundFile(String path, String storagename) {
		try {
			elementpush.onRemoveFile(storagename, path);
		} catch (Exception e) {
			Log2.log.error("Can't process not found element", e);
		}
	}
	
	public int maxPathWidthCrawl() {
		if (referer.limit_to_current_directory) {
			return 1;
		}
		return 100;
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
		element.storagename = referer.pathindex_storage_name;
		
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
	
	public boolean onStartSearch(AbstractFile file) {
		if (referer.stop == true) {
			return false;
		}
		try {
			if (referer.currentworkingdir != null) {
				return onFoundFile(file, referer.pathindex_storage_name);
			} else {
				/**
				 * Search from root storage
				 */
				elementpush.onFoundElement(SourcePathIndexerElement.prepareStorageElement(referer.pathindex_storage_name));
			}
			return true;
		} catch (Exception e) {
			Log2.log.error("Can't process found root element", e);
			return false;
		}
	}
	
	public void onEndSearch() {
	}
	
	public String getCurrentWorkingDir() {
		return referer.currentworkingdir;
	}
	
}