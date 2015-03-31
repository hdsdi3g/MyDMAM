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
package hd3gtv.mydmam.storage;

import hd3gtv.log2.Log2Dumpable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.util.List;

/**
 * Caution : this API is not thread safe !
 */
public abstract class AbstractFile implements Log2Dumpable {
	
	private Storage referer;
	
	protected AbstractFile(Storage referer) {
		this.referer = referer;
		if (referer == null) {
			throw new NullPointerException("\"referer\" can't to be null");
		}
	}
	
	protected AbstractFile(AbstractFile referer) {
		this.referer = referer.referer;
	}
	
	public abstract List<AbstractFile> listFiles();
	
	public abstract boolean canRead();
	
	public abstract boolean canWrite();
	
	public abstract long lastModified();
	
	public abstract String getPath();
	
	public abstract boolean isDirectory();
	
	public abstract boolean isFile();
	
	public abstract boolean isHidden();
	
	public abstract String getName();
	
	public abstract long length();
	
	public abstract boolean exists();
	
	/**
	 * Close connection to server but don't close specific stream.
	 */
	public abstract void close();
	
	/**
	 * Don't forget to close the stream
	 */
	public abstract BufferedInputStream getInputStream(int buffersize);
	
	/**
	 * Overwrite the actual stream
	 * Don't forget to close the stream
	 */
	public abstract BufferedOutputStream getOutputStream(int buffersize);
	
	/**
	 * @param newpath Always a full path, never relative
	 */
	public abstract AbstractFile renameTo(String newpath);
	
	/**
	 * @param newpath Always a full path, never relative
	 */
	public abstract AbstractFile mkdir(String newpath);
	
	/**
	 * @param newpath Always a full path, never relative
	 */
	public abstract AbstractFile getAbstractFile(String newpath);
	
	/**
	 * Recusive
	 */
	public abstract boolean delete();
	
	public Storage getStorage() {
		return referer;
	}
	
	public String getParentPath() {
		String path = getPath();
		int last_slash = path.lastIndexOf("/");
		if (last_slash > 0) {
			return path.substring(0, last_slash - 1);
		} else {
			return "/";
		}
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(referer.getProtocol());
		sb.append("://");
		sb.append(referer.getName());
		sb.append("/");
		sb.append(getPath());
		return sb.toString();
	}
	
	/**
	 * @param dest Always a full path, never relative
	 */
	public void copyTo(Storage dest_storage, String dest_path, CopyProgression copy_progression) {
		if (dest_storage.getClass() == referer.getClass()) {
			// TODO classic copy
			if (dest_storage instanceof StorageLocalFile) {
				
			} else {
				
			}
		} else {
			// TODO cross storage type copy
		}
	}
	
}
