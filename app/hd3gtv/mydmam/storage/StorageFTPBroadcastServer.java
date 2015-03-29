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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

public class StorageFTPBroadcastServer extends StorageURILoginPassword {
	
	String path;
	private static final boolean passive = true;
	
	StorageFTPBroadcastServer(URILoginPasswordConfiguration configuration) {
		super(configuration);
		path = configuration.relative_path.replaceAll("/", "").toUpperCase();
	}
	
	class AbstractFileFtpBCST implements AbstractFile {
		
		private AbstractFileFtpBCST() throws IOException {
		}
		
		public long lastModified() {
			return 0;
		}
		
		public long length() {
			return 0;
		}
		
		public boolean canRead() {
			return true;
		}
		
		public boolean canWrite() {
			/*if (configurator.readonly) {
				return false;
			} else {
				return true;
			}*/
			return true;
		}
		
		public String getPath() {
			try {
				return "/" + path.substring(configuration.relative_path.length());
			} catch (StringIndexOutOfBoundsException e) {
				return "/";
			}
		}
		
		public boolean isDirectory() {
			return true;
		}
		
		public boolean isFile() {
			return false;
		}
		
		public boolean isHidden() {
			return false;
		}
		
		public String getName() {
			return configuration.relative_path.replaceAll("/", "");
		}
		
		public void close() {
		}
		
		public AbstractFile moveTo(String newpath) {
			return null;
		}
		
		public AbstractFile mkdir(String newpath) {
			return null;
		}
		
		public BufferedInputStream getInputStream(int buffersize) {
			return null;
		}
		
		public BufferedOutputStream getOutputStream(int buffersize) {
			return null;
		}
		
		public boolean delete() {
			return false;
		}
		
		public Log2Dump getLog2Dump() {
			return new Log2Dump("ftp-nexio", getPath());
		}
		
		private FTPClient connectMe() throws IOException {
			FTPClient ftpclient = new FTPClient();
			ftpclient.connect(configuration.host, configuration.port);
			
			if (ftpclient.login(configuration.login, configuration.password) == false) {
				ftpclient.logout();
				throw new IOException("Can't login to server");
			}
			int reply = ftpclient.getReplyCode();
			if (FTPReply.isPositiveCompletion(reply) == false) {
				ftpclient.disconnect();
				throw new IOException("Can't login to server");
			}
			
			ftpclient.setFileType(FTP.BINARY_FILE_TYPE);
			
			if (passive) {
				ftpclient.enterLocalPassiveMode();
			} else {
				ftpclient.enterLocalActiveMode();
			}
			ftpclient.changeWorkingDirectory("/" + path);
			if (ftpclient.printWorkingDirectory().equals("/" + path) == false) {
				throw new IOException("Can't change working dir : " + "/" + path);
			}
			
			return ftpclient;
		}
		
		private class SimpleMediaFile implements AbstractFile {
			
			String name;
			AbstractFileFtpBCST referer;
			long size;
			FTPClient ftpclient;
			
			public SimpleMediaFile(String name, AbstractFileFtpBCST referer, long size) {
				this.name = name;
				this.referer = referer;
				this.size = size;
				ftpclient = null;
			}
			
			public Log2Dump getLog2Dump() {
				Log2Dump dump = new Log2Dump();
				dump.add("ftp-broadcast-file", name + "\t" + configuration.toString());
				return dump;
			}
			
			public List<AbstractFile> listFiles() {
				return null;
			}
			
			public boolean canRead() {
				return true;
			}
			
			public boolean canWrite() {
				return referer.canWrite();
			}
			
			public long lastModified() {
				return 0;
			}
			
			public String getPath() {
				return "/" + /*path + "/"+*/name;
			}
			
			public boolean isDirectory() {
				return false;
			}
			
			public boolean isFile() {
				return true;
			}
			
			public boolean isHidden() {
				return false;
			}
			
			public String getName() {
				return name;
			}
			
			public long length() {
				return size;
			}
			
			public void close() {
				if (ftpclient != null) {
					try {
						ftpclient.disconnect();
					} catch (IOException e) {
					}
				}
			}
			
			public BufferedInputStream getInputStream(int buffersize) {
				try {
					ftpclient = connectMe();
					ftpclient.setFileType(FTP.BINARY_FILE_TYPE);
					InputStream is = ftpclient.retrieveFileStream(name);
					if (is == null) {
						throw new NullPointerException("Retrieve File Stream is null");
					}
					return new BufferedInputStream(is, buffersize);
				} catch (IOException e) {
					Log2.log.error("Can't download file", e, this);
				}
				System.err.println("Connected 5");
				return null;
			}
			
			public BufferedOutputStream getOutputStream(int buffersize) {
				/*if (configurator.readonly) {
					return null;
				}*/
				try {
					ftpclient = connectMe();
					ftpclient.setFileType(FTP.BINARY_FILE_TYPE);
					OutputStream is = ftpclient.storeFileStream(name);
					if (is == null) {
						throw new NullPointerException("Retrieve File Stream is null");
					}
					return new BufferedOutputStream(is, buffersize);
				} catch (IOException e) {
					Log2.log.error("Can't upload file", e, this);
				}
				return null;
			}
			
			public AbstractFile moveTo(String newpath) {
				return null;
			}
			
			public AbstractFile mkdir(String newpath) {
				return null;
			}
			
			public AbstractFile getAbstractFile(String newpath) {
				return referer.getAbstractFile(newpath);
			}
			
			public boolean delete() {
				/*if (configurator.readonly) {
					return false;
				}*/
				try {
					FTPClient ftpclient = connectMe();
					boolean result = ftpclient.deleteFile(name);
					ftpclient.disconnect();
					return result;
				} catch (IOException e) {
					Log2.log.error("Can't delete file", e, this);
				}
				return false;
			}
			
		}
		
		public List<AbstractFile> listFiles() {
			try {
				FTPClient ftpclient = connectMe();
				FTPFile[] files = ftpclient.listFiles();
				
				if (files == null) {
					return new ArrayList<AbstractFile>(1);
				}
				
				ArrayList<AbstractFile> absfiles = new ArrayList<AbstractFile>();
				
				for (int pos = 0; pos < files.length; pos++) {
					if (files[pos].isDirectory() == false) {
						absfiles.add(new SimpleMediaFile(files[pos].getName(), this, files[pos].getSize()));
					}
				}
				ftpclient.disconnect();
				return absfiles;
			} catch (IOException e) {
				Log2.log.error("Can't dirlist", e, this);
			}
			return null;
		}
		
		public AbstractFile getAbstractFile(String newpath) {
			if (newpath.startsWith("/")) {
				/**
				 * No cd allowed
				 */
				return null;
			}
			return new SimpleMediaFile(newpath, this, 0);
		}
	}
	
	public AbstractFile getRootPath() throws NullPointerException, IOException {
		return new AbstractFileFtpBCST();
	}
	
}
