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
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
*/
package hd3gtv.mydmam.assetsxcross;

import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;

@FunctionalInterface
public interface PathindexScanToESMatchingItem<T> {
	
	public void match(SourcePathIndexerElement element, T db_entry);
	
	/**
	 * Do nothing.
	 */
	public default void notMatch(SourcePathIndexerElement element, NotMatchReason reason) {
	}
	
	public enum NotMatchReason {
		NO_ID_FOR_ELEMENT, MISSING_IN_DATABASE
	}
	
}
