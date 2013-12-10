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
import java.io.IOException;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

class AbstractFileBridgeSmb implements AbstractFile {
	
	private StorageConfigurator configurator;
	private NtlmPasswordAuthentication auth;
	private SmbFile file;
	private String root_path;
	
	public AbstractFileBridgeSmb(StorageConfigurator configurator) throws IOException {
		this.configurator = configurator;
		auth = new NtlmPasswordAuthentication("", configurator.username, configurator.password);
		StringBuffer sb = new StringBuffer();
		sb.append("smb://");
		sb.append(configurator.host);
		sb.append(configurator.path);
		file = new SmbFile(sb.toString(), auth);
		root_path = file.getPath();
	}
	
	private AbstractFileBridgeSmb(AbstractFileBridgeSmb referer, SmbFile file) {
		this.configurator = referer.configurator;
		this.auth = referer.auth;
		this.file = file;
		root_path = referer.root_path;
	}
	
	public Log2Dump getLog2Dump() {
		return new Log2Dump("smbfile", file.getPath());
	}
	
	public AbstractFile[] listFiles() {
		try {
			SmbFile[] list = file.listFiles();
			if (list == null) {
				return new AbstractFileBridgeSmb[1];
			}
			AbstractFileBridgeSmb[] abstractlist = new AbstractFileBridgeSmb[list.length];
			for (int pos = 0; pos < list.length; pos++) {
				abstractlist[pos] = new AbstractFileBridgeSmb(this, list[pos]);
			}
			return abstractlist;
		} catch (SmbException e) {
			Log2.log.error("Can't list files", e, this);
			return null;
		}
	}
	
	public boolean canRead() {
		try {
			return file.canRead();
		} catch (SmbException e) {
			Log2.log.error("Can't access to file", e, this);
			return false;
		}
	}
	
	public boolean canWrite() {
		if (configurator.readonly) {
			return false;
		}
		try {
			return file.canWrite();
		} catch (SmbException e) {
			Log2.log.error("Can't access to file", e, this);
			return false;
		}
	}
	
	public long lastModified() {
		return file.getLastModified();
	}
	
	public boolean isDirectory() {
		try {
			return file.isDirectory();
		} catch (SmbException e) {
			Log2.log.error("Can't access to file", e, this);
			return false;
		}
	}
	
	public boolean isFile() {
		try {
			return file.isFile();
		} catch (SmbException e) {
			Log2.log.error("Can't access to file", e, this);
			return false;
		}
	}
	
	public boolean isHidden() {
		try {
			return file.isHidden();
		} catch (SmbException e) {
			Log2.log.error("Can't access to file", e, this);
			return false;
		}
	}
	
	public String getPath() {
		String path = file.getPath();
		if (path.endsWith("/")) {
			return path.substring(root_path.length() - 1, path.length() - 1);
		} else {
			return path.substring(root_path.length() - 1);
		}
	}
	
	public String getName() {
		String name = file.getName();
		if (name.endsWith("/")) {
			return name.substring(0, name.length() - 1);
		} else {
			return name;
		}
	}
	
	public long length() {
		try {
			return file.length();
		} catch (SmbException e) {
			Log2.log.error("Can't access to file", e, this);
			return -1;
		}
	}
	
	public void close() {
	}
	
	public BufferedInputStream getInputStream(int buffersize) {
		try {
			return new BufferedInputStream(file.getInputStream(), buffersize);
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
			return new BufferedOutputStream(file.getOutputStream(), buffersize);
		} catch (IOException e) {
			Log2.log.error("Can't access to file", e, this);
		}
		return null;
	}
	
	private SmbFile getNewFile(String newpath) {
		if (newpath.startsWith("/") == false) {
			newpath = "/" + newpath;
		}
		
		StringBuffer sb = new StringBuffer();
		sb.append("smb://");
		sb.append(configurator.host);
		sb.append(configurator.path.substring(0, configurator.path.length() - 1));
		sb.append(newpath);
		
		try {
			SmbFile newfile = new SmbFile(sb.toString(), auth);
			if (newfile.getCanonicalPath().startsWith(root_path) == false) {
				/** Security problem */
				return null;
			} else {
				return newfile;
			}
		} catch (IOException e) {
			/** Maybe a security problem */
			Log2.log.error("Can't access to file", e, this);
			return null;
		}
	}
	
	public AbstractFile moveTo(String newpath) {
		if (configurator.readonly) {
			return null;
		}
		SmbFile newfile = getNewFile(newpath);
		if (newfile == null) {
			return null;
		}
		
		try {
			file.renameTo(newfile);
			return new AbstractFileBridgeSmb(this, newfile);
		} catch (SmbException e) {
			Log2.log.error("Can't access to file", e, this);
			return null;
		}
	}
	
	public AbstractFile mkdir(String newpath) {
		if (configurator.readonly) {
			return null;
		}
		if (newpath.endsWith("/") == false) {
			newpath = newpath + "/";
		}
		
		SmbFile newfile = getNewFile(newpath);
		if (newfile == null) {
			return null;
		}
		try {
			newfile.mkdir();
			return new AbstractFileBridgeSmb(this, newfile);
		} catch (SmbException e) {
			Log2.log.error("Can't access to file", e, this);
			return null;
		}
	}
	
	public AbstractFile getAbstractFile(String newpath) {
		SmbFile newfile = getNewFile(newpath);
		if (newfile == null) {
			return null;
		}
		return new AbstractFileBridgeSmb(this, newfile);
	}
	
	private static boolean recursiveDelete(SmbFile currentfile) {
		try {
			if (currentfile.isDirectory()) {
				SmbFile[] files = currentfile.listFiles();
				for (int pos = 0; pos < files.length; pos++) {
					if (recursiveDelete(files[pos]) == false) {
						return false;
					}
				}
			}
			currentfile.delete();
			return true;
		} catch (SmbException e) {
			Log2.log.error("Can't delete file", e);
			return false;
		}
	}
	
	public boolean delete() {
		if (configurator.readonly) {
			return false;
		}
		return recursiveDelete(file);
	}
}
