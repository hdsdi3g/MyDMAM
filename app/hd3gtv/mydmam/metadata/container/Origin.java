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

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Origin {
	
	long date;
	String key;
	String storage;
	long size;
	private transient File physical_source;
	private transient SourcePathIndexerElement pathindex_element;
	
	Origin() {
	}
	
	public static Origin fromSource(SourcePathIndexerElement element, File physical_source) {
		Origin origin = new Origin();
		origin.date = element.date;
		origin.key = element.prepare_key();
		origin.size = element.size;
		origin.storage = element.storagename;
		origin.pathindex_element = element;
		origin.physical_source = physical_source;
		return origin;
	}
	
	/**
	 * Retrive from ES, and caching.
	 */
	public File getPhysicalSource() throws IOException {
		if (physical_source == null) {
			physical_source = Explorer.getLocalBridgedElement(getPathindexElement());
			if (physical_source == null) {
				throw new IOException("Can't bridge with " + storage + " storage");
			}
		}
		return physical_source;
	}
	
	/**
	 * Retrive from ES, and caching.
	 */
	public SourcePathIndexerElement getPathindexElement() throws FileNotFoundException {
		if (pathindex_element == null) {
			Explorer explorer = new Explorer();
			pathindex_element = explorer.getelementByIdkey(key);
			if (pathindex_element == null) {
				throw new FileNotFoundException("Can't found pathindex element with key: " + key);
			}
		}
		return pathindex_element;
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
	
	public String getUniqueElementKey() throws FileNotFoundException {
		return getUniqueElementKey(getPathindexElement());
	}
	
	/**
	 * If the file size/date change, this id will change
	 */
	public static String getUniqueElementKey(SourcePathIndexerElement element) {
		StringBuffer sb = new StringBuffer();
		sb.append(element.storagename);
		sb.append(element.currentpath);
		sb.append(element.size);
		sb.append(element.date);
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
			md.update(sb.toString().getBytes());
			return "mtd-" + MyDMAM.byteToString(md.digest());
		} catch (NoSuchAlgorithmException e) {
			throw new NullPointerException(e.getMessage());
		}
	}
	
}
