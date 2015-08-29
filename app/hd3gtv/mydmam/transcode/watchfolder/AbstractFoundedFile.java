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
package hd3gtv.mydmam.transcode.watchfolder;

import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.storage.AbstractFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.util.List;
import java.util.Objects;

import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.model.Row;

public class AbstractFoundedFile implements AbstractFile {
	
	enum Status {
		DETECTED, IN_PROCESSING, PROCESSED, ERROR
	}
	
	String path;
	String storage_name;
	long date;
	long size;
	Status status = Status.DETECTED;
	long last_checked;
	
	AbstractFoundedFile(Row<String, String> db_row) {
		path_index_key = db_row.getKey();
		path = db_row.getColumns().getStringValue("path", "/");
		storage_name = db_row.getColumns().getStringValue("storage_name", "");
		date = db_row.getColumns().getLongValue("date", 0l);
		size = db_row.getColumns().getLongValue("size", 0l);
		status = Status.valueOf(db_row.getColumns().getStringValue("status", Status.DETECTED.name()));
		last_checked = db_row.getColumns().getLongValue("last_checked", System.currentTimeMillis());
	}
	
	void saveToCassandra(MutationBatch mutator) {
		mutator.withRow(WatchFolderDB.CF_WATCHFOLDERS, getPathIndexKey()).putColumn("path", path, WatchFolderTranscoder.TTL_CASSANDRA);
		mutator.withRow(WatchFolderDB.CF_WATCHFOLDERS, getPathIndexKey()).putColumn("storage", storage_name, WatchFolderTranscoder.TTL_CASSANDRA);
		mutator.withRow(WatchFolderDB.CF_WATCHFOLDERS, getPathIndexKey()).putColumn("date", date, WatchFolderTranscoder.TTL_CASSANDRA);
		mutator.withRow(WatchFolderDB.CF_WATCHFOLDERS, getPathIndexKey()).putColumn("size", size, WatchFolderTranscoder.TTL_CASSANDRA);
		mutator.withRow(WatchFolderDB.CF_WATCHFOLDERS, getPathIndexKey()).putColumn("status", status.name(), WatchFolderTranscoder.TTL_CASSANDRA);
		mutator.withRow(WatchFolderDB.CF_WATCHFOLDERS, getPathIndexKey()).putColumn("last_checked", last_checked, WatchFolderTranscoder.TTL_CASSANDRA);
	}
	
	AbstractFoundedFile(AbstractFile found_file, String storage_name) {
		path = found_file.getPath();
		this.storage_name = storage_name;
		date = found_file.lastModified();
		size = found_file.length();
		last_checked = System.currentTimeMillis();
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("storage_name", storage_name);
		dump.add("path", path);
		dump.addDate("date", date);
		dump.add("size", size);
		dump.add("status", status);
		dump.addDate("last_checked", last_checked);
		return dump;
	}
	
	private transient String path_index_key;
	
	private transient int hash = 0;
	
	/**
	 * Only based on storage_name & path
	 */
	public int hashCode() {
		if (hash == 0) {
			hash = Objects.hash(storage_name, path);
		}
		return hash;
	}
	
	/**
	 * Only based on storage_name & path
	 */
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if ((obj instanceof AbstractFoundedFile) == false) {
			return false;
		}
		return ((AbstractFoundedFile) obj).hashCode() == hashCode();
	}
	
	String getPathIndexKey() {
		if (path_index_key == null) {
			path_index_key = SourcePathIndexerElement.prepare_key(storage_name, path);
		}
		return path_index_key;
	}
	
	/**
	 * @return null
	 */
	public List<AbstractFile> listFiles() {
		return null;
	}
	
	/**
	 * @return false
	 */
	public boolean canRead() {
		return false;
	}
	
	/**
	 * @return false
	 */
	public boolean canWrite() {
		return false;
	}
	
	public long lastModified() {
		return date;
	}
	
	public String getPath() {
		return path;
	}
	
	/**
	 * @return false
	 */
	public boolean isDirectory() {
		return false;
	}
	
	/**
	 * @return true
	 */
	public boolean isFile() {
		return true;
	}
	
	/**
	 * @return false
	 */
	public boolean isHidden() {
		return false;
	}
	
	public String getName() {
		if (path.lastIndexOf("/") > 0) {
			path = path.substring(path.lastIndexOf("/") + 1, path.length());
		}
		return path.substring(1);
	}
	
	public long length() {
		return size;
	}
	
	public void close() {
	}
	
	/**
	 * @return null
	 */
	public BufferedInputStream getInputStream(int buffersize) {
		return null;
	}
	
	/**
	 * @return null
	 */
	public BufferedOutputStream getOutputStream(int buffersize) {
		return null;
	}
	
	/**
	 * @return null
	 */
	public AbstractFile moveTo(String newpath) {
		return null;
	}
	
	/**
	 * @return null
	 */
	public AbstractFile mkdir(String newpath) {
		return null;
	}
	
	/**
	 * @return null
	 */
	public AbstractFile getAbstractFile(String newpath) {
		return null;
	}
	
	/**
	 * @return false
	 */
	public boolean delete() {
		return false;
	}
	
}
