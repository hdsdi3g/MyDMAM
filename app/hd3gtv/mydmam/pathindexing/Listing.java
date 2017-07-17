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

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.storage.AbstractFile;
import hd3gtv.mydmam.storage.IgnoreFiles;
import hd3gtv.mydmam.storage.StorageCrawler;

class Listing implements StorageCrawler {
	
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
			Loggers.Pathindex.error("Can't process not found element", e);
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
			element.id = Importer.getIdExtractorFileName().getId(file.getName());
		}
		element.storagename = referer.getName();
		
		try {
			if (elementpush.onFoundElement(element)) {
				count++;
			}
		} catch (Exception e) {
			Loggers.Pathindex.error("Can't process found element", e);
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
		return IgnoreFiles.directory_config_list;
	}
	
	public boolean onStartSearch(String storage_name, AbstractFile file) {
		if (referer.stop == true) {
			return false;
		}
		try {
			if (referer.currentworkingdir != null) {
				return onFoundFile(file, referer.getName());
			} else {
				/**
				 * Search from root storage
				 */
				elementpush.onFoundElement(SourcePathIndexerElement.prepareStorageElement(referer.getName()));
			}
			return true;
		} catch (Exception e) {
			Loggers.Pathindex.error("Can't process found root element", e);
			return false;
		}
	}
	
	public void onEndSearch() {
	}
	
	public String getCurrentWorkingDir() {
		return referer.currentworkingdir;
	}
	
}