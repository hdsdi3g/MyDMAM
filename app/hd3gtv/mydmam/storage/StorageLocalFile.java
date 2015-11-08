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
package hd3gtv.mydmam.storage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import hd3gtv.mydmam.Loggers;

public class StorageLocalFile extends Storage {
	
	private File root;
	
	StorageLocalFile(File root) {
		this.root = root;
	}
	
	File getRoot() {
		return root;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("root: ");
		sb.append(root);
		sb.append("\tStorage\t");
		sb.append(super.toString());
		return sb.toString();
	}
	
	class Localfile implements AbstractFile {
		
		private File file;
		
		private Localfile(File file) {
			this.file = file;
		}
		
		public List<AbstractFile> listFiles() {
			File[] list = file.listFiles();
			if (list == null) {
				return new ArrayList<AbstractFile>(1);
			}
			ArrayList<AbstractFile> abstractlist = new ArrayList<AbstractFile>();
			for (int pos = 0; pos < list.length; pos++) {
				abstractlist.add(new Localfile(list[pos]));
			}
			return abstractlist;
		}
		
		public boolean canRead() {
			return file.canRead();
		}
		
		public boolean canWrite() {
			/*if (configurator.readonly) {
				return false;
			}*/
			return file.canWrite();
		}
		
		public long lastModified() {
			return file.lastModified();
		}
		
		public String getPath() {
			String subpath = "/" + file.getPath().substring(root.getPath().length());
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
				Loggers.Storage_Local.error("Can't access to file, " + this, e);
			}
			return null;
		}
		
		public BufferedOutputStream getOutputStream(int buffersize) {
			/*if (configurator.readonly) {
				return null;
			}*/
			try {
				return new BufferedOutputStream(new FileOutputStream(file), buffersize);
			} catch (IOException e) {
				Loggers.Storage_Local.error("Can't access to file, " + this, e);
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
			
			File newfile = new File(root + newpath);
			try {
				if (newfile.getCanonicalPath().startsWith(root.getPath()) == false) {
					/** Security problem */
					return null;
				}
			} catch (IOException e) {
				/** Maybe a security problem */
				Loggers.Storage_Local.error("Can't access to file, " + this, e);
				return null;
			}
			return newfile;
		}
		
		public AbstractFile moveTo(String newpath) {
			/*if (configurator.readonly) {
				return null;
			}*/
			File newfile = getNewFile(newpath);
			if (newfile == null) {
				return null;
			}
			
			if (file.renameTo(newfile)) {
				return new Localfile(newfile);
			} else {
				return null;
			}
		}
		
		public AbstractFile mkdir(String newpath) {
			/*if (configurator.readonly) {
				return null;
			}*/
			File newfile = getNewFile(newpath);
			if (newfile == null) {
				return null;
			}
			if (newfile.mkdir()) {
				return new Localfile(newfile);
			} else {
				return null;
			}
		}
		
		public AbstractFile getAbstractFile(String newpath) {
			File newfile = getNewFile(newpath);
			if (newfile == null) {
				return null;
			}
			
			return new Localfile(newfile);
		}
		
		public boolean delete() {
			/*if (configurator.readonly) {
				return false;
			}*/
			return FileUtils.deleteQuietly(file);
		}
		
	}
	
	public AbstractFile getRootPath() throws NullPointerException, IOException {
		return new Localfile(root);
	}
	
}
