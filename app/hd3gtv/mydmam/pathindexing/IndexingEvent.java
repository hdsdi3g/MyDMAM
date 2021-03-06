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
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
*/
package hd3gtv.mydmam.pathindexing;

@FunctionalInterface
public interface IndexingEvent {
	
	/**
	 * @return true for really push to server (in case of importing), or false to stop search (in case of exporting)
	 */
	public boolean onFoundElement(SourcePathIndexerElement element) throws Exception;
	
	/**
	 * Used only for importing new file/refresh index, not for search.
	 */
	public default void onRemoveFile(String storagename, String path) throws Exception {
	}
	
}
