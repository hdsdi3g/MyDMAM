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

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public class StorageSMB extends StorageURILoginPassword {
	
	private NtlmPasswordAuthentication auth;
	private String root_path;
	
	StorageSMB(URILoginPasswordConfiguration configuration) {
		super(configuration);
		auth = new NtlmPasswordAuthentication("", configuration.login, configuration.password);
		
		StringBuffer sb = new StringBuffer();
		sb.append("smb://");
		sb.append(configuration.host);
		sb.append(configuration.relative_path);
		sb.append("/");
		root_path = sb.toString();
		/*
		configuration.relative_path = file.getPath();*/
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = super.getLog2Dump();
		dump.add("configuration", configuration.toString());
		return dump;
	}
	
	class AbstractFileSmb extends AbstractFile {
		
		private SmbFile file;
		
		private AbstractFileSmb(Storage referer) throws IOException {
			super(referer);
			this.file = new SmbFile(root_path, auth);
		}
		
		private AbstractFileSmb(SmbFile file, AbstractFile referer) {
			super(referer);
			this.file = file;
		}
		
		public Log2Dump getLog2Dump() {
			return new Log2Dump("smbfile", file.getPath());
		}
		
		public List<AbstractFile> listFiles() {
			try {
				SmbFile[] list = file.listFiles();
				if (list == null) {
					return new ArrayList<AbstractFile>(1);
				}
				List<AbstractFile> abstractlist = new ArrayList<AbstractFile>();
				for (int pos = 0; pos < list.length; pos++) {
					abstractlist.add(new AbstractFileSmb(list[pos], this));
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
			/*if (configurator.readonly) {
				return false;
			}*/
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
			/*if (configurator.readonly) {
				return null;
			}*/
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
			sb.append(configuration.host);
			sb.append(root_path.substring(0, root_path.length() - 1));
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
		
		public AbstractFile renameTo(String newpath) {
			/*if (configurator.readonly) {
				return null;
			}*/
			SmbFile newfile = getNewFile(newpath);
			if (newfile == null) {
				return null;
			}
			
			try {
				file.renameTo(newfile);
				return new AbstractFileSmb(newfile, this);
			} catch (SmbException e) {
				Log2.log.error("Can't access to file", e, this);
				return null;
			}
		}
		
		public AbstractFile mkdir(String newpath) {
			/*if (configurator.readonly) {
				return null;
			}*/
			if (newpath.endsWith("/") == false) {
				newpath = newpath + "/";
			}
			
			SmbFile newfile = getNewFile(newpath);
			if (newfile == null) {
				return null;
			}
			try {
				newfile.mkdir();
				return new AbstractFileSmb(newfile, this);
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
			return new AbstractFileSmb(newfile, this);
		}
		
		private boolean recursiveDelete(SmbFile currentfile) {
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
			/*if (configurator.readonly) {
				return false;
			}*/
			return recursiveDelete(file);
		}
		
		public boolean exists() {
			try {
				return file.exists();
			} catch (SmbException e) {
				Log2.log.error("Can't check if this file exists", e);
				return false;
			}
		}
	}
	
	public AbstractFile getRootPath() throws NullPointerException, IOException {
		return new AbstractFileSmb(this);
	}
	
	public long getUsableSpace() {
		try {
			return new SmbFile(root_path, auth).getDiskFreeSpace();
		} catch (Exception e) {
			Log2.log.error("Can't extract usable space", e);
			return Long.MAX_VALUE;
		}
	}
	
	public String getProtocol() {
		return "smb";
	}
	
}
