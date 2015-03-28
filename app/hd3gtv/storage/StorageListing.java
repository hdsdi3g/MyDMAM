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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.storage;

@Deprecated
public interface StorageListing {
	
	/**
	 * @return false for stop
	 */
	boolean onFoundFile(AbstractFile file, String storagename);
	
	/**
	 * @return false for stop
	 */
	void onNotFoundFile(String path, String storagename);
	
	boolean isSearchIsRecursive();
	
	boolean canSelectfileInSearch();
	
	boolean canSelectdirInSearch();
	
	boolean canSelectHiddenInSearch();
	
	int maxPathWidthCrawl();
	
	/**
	 * @return may be null;
	 */
	IgnoreFiles getRules();
	
	/**
	 * @return false for stop
	 */
	boolean onStartSearch(AbstractFile search_root_path);
	
	void onEndSearch();
	
	/**
	 * @return may be null, don't chroot, keep the same root, but start crawl from this.
	 */
	String getCurrentWorkingDir();
}
