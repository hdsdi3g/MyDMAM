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

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

class AbstractFileBridgeLocalfile implements AbstractFile {
	
	private File file;
	private StorageConfigurator configurator;
	private String root_path;
	
	AbstractFileBridgeLocalfile(StorageConfigurator configurator) throws IOException {
		this.configurator = configurator;
		File file_tmp = new File(configurator.path);
		file = new File(file_tmp.getCanonicalPath());
		root_path = file.getPath();
	}
	
	private AbstractFileBridgeLocalfile(AbstractFileBridgeLocalfile referer, File file) {
		this.configurator = referer.configurator;
		this.file = file;
		this.root_path = referer.root_path;
	}
	
	public AbstractFile[] listFiles() {
		File[] list = file.listFiles();
		if (list == null) {
			return new AbstractFileBridgeLocalfile[1];
		}
		AbstractFileBridgeLocalfile[] abstractlist = new AbstractFileBridgeLocalfile[list.length];
		for (int pos = 0; pos < list.length; pos++) {
			abstractlist[pos] = new AbstractFileBridgeLocalfile(this, list[pos]);
		}
		return abstractlist;
	}
	
	public Log2Dump getLog2Dump() {
		return new Log2Dump("file", file);
	}
	
	public boolean canRead() {
		return file.canRead();
	}
	
	public boolean canWrite() {
		if (configurator.readonly) {
			return false;
		}
		return file.canWrite();
	}
	
	public long lastModified() {
		return file.lastModified();
	}
	
	public String getPath() {
		String subpath = file.getPath().substring(root_path.length());
		if (File.separator.equals("\\")) {
			return subpath.replaceAll("\\", "/");
		} else {
			return subpath;
		}
	}
	
	public boolean isDirectory() {
		return file.isDirectory();
	}
	
	public boolean isFile() {
		return file.isFile();
	}
	
	public boolean isHidden() {
		return file.isHidden();
	}
	
	public String getName() {
		return file.getName();
	}
	
	public long length() {
		return file.length();
	}
	
	public void close() {
	}
	
	public BufferedInputStream getInputStream(int buffersize) {
		try {
			return new BufferedInputStream(new FileInputStream(file), buffersize);
		} catch (IOException e) {
			Log2.log.error("Can't access to file", e, this);
		}
		return null;
	}
	
	public BufferedOutputStream getOutputStream(int buffersize) {
		if (configurator.readonly) {
			return null;
		}
		try {
			return new BufferedOutputStream(new FileOutputStream(file), buffersize);
		} catch (IOException e) {
			Log2.log.error("Can't access to file", e, this);
		}
		return null;
	}
	
	private File getNewFile(String newpath) {
		if (newpath.startsWith("/") == false) {
			newpath = "/" + newpath;
		}
		
		if (File.separator.equals("\\")) {
			newpath = newpath.replaceAll("/", "\\");
		}
		
		File newfile = new File(root_path + newpath);
		try {
			if (newfile.getCanonicalPath().startsWith(root_path) == false) {
				/** Security problem */
				return null;
			}
		} catch (IOException e) {
			/** Maybe a security problem */
			Log2.log.error("Can't access to file", e, this);
			return null;
		}
		return newfile;
	}
	
	public AbstractFile moveTo(String newpath) {
		if (configurator.readonly) {
			return null;
		}
		File newfile = getNewFile(newpath);
		if (newfile == null) {
			return null;
		}
		
		if (file.renameTo(newfile)) {
			return new AbstractFileBridgeLocalfile(this, newfile);
		} else {
			return null;
		}
	}
	
	public AbstractFile mkdir(String newpath) {
		if (configurator.readonly) {
			return null;
		}
		File newfile = getNewFile(newpath);
		if (newfile == null) {
			return null;
		}
		if (newfile.mkdir()) {
			return new AbstractFileBridgeLocalfile(this, newfile);
		} else {
			return null;
		}
	}
	
	public AbstractFile getAbstractFile(String newpath) {
		File newfile = getNewFile(newpath);
		if (newfile == null) {
			return null;
		}
		
		return new AbstractFileBridgeLocalfile(this, newfile);
	}
	
	private static boolean recursiveDelete(File currentfile) {
		if (currentfile.isDirectory()) {
			File[] files = currentfile.listFiles();
			for (int pos = 0; pos < files.length; pos++) {
				if (recursiveDelete(files[pos]) == false) {
					return false;
				}
			}
			return currentfile.delete();
		} else {
			return currentfile.delete();
		}
	}
	
	public boolean delete() {
		if (configurator.readonly) {
			return false;
		}
		return recursiveDelete(file);
	}
	
}
