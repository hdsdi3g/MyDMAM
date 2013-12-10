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

public interface StorageListing {
	
	/**
	 * @return false for stop
	 */
	boolean onFoundFile(AbstractFile file, String storagename);
	
	boolean isSearchIsRecursive();
	
	boolean canSelectfileInSearch();
	
	boolean canSelectdirInSearch();
	
	boolean canSelectHiddenInSearch();
	
	/**
	 * @return may be null;
	 */
	IgnoreFiles getRules();
	
	void onEndSearch();
	
}
