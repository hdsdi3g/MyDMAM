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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.mydmam.metadata.container;

import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;

public class Origin {
	
	long date;
	String key;
	String storage;
	long size;
	
	Origin() {
	}
	
	public static Origin fromSource(SourcePathIndexerElement element) {
		Origin origin = new Origin();
		origin.date = element.date;
		origin.key = element.prepare_key();
		origin.size = element.size;
		origin.storage = element.storagename;
		return origin;
	}
	
	public long getDate() {
		return date;
	}
	
	public String getKey() {
		return key;
	}
	
	public long getSize() {
		return size;
	}
	
	public String getStorage() {
		return storage;
	}
	
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if ((obj instanceof Origin) == false) {
			return false;
		}
		Origin element = (Origin) obj;
		
		if (element.date != date) {
			return false;
		}
		if (element.key != key) {
			return false;
		}
		if (element.storage != storage) {
			return false;
		}
		if (element.size != size) {
			return false;
		}
		return true;
	}
	
}
