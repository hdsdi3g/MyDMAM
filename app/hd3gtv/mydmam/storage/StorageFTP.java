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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import hd3gtv.mydmam.Loggers;

public class StorageFTP extends StorageURILoginPassword {
	
	private boolean ftp_active;
	
	/**
	 * Root path, end with a "/"
	 */
	private final String root_path;
	
	StorageFTP(URILoginPasswordConfiguration configuration, boolean ftp_active) {
		super(configuration);
		this.ftp_active = ftp_active;
		if (configuration.port == 0) {
			configuration.port = 21;
		}
		
		if (configuration.relative_path.equals("/") == false) {
			if (configuration.relative_path.startsWith("/") == false) {
				configuration.relative_path = "/" + configuration.relative_path;
			}
			if (configuration.relative_path.endsWith("/")) {
				configuration.relative_path = configuration.relative_path.substring(0, configuration.relative_path.length() - 1);
			}
			
			root_path = configuration.relative_path;
		} else {
			root_path = "/";
		}
	}
	
	class AbstractFileFtp implements AbstractFile {
		
		private FTPClient ftpclient;
		private FTPFile file;
		
		/**
		 * Chroot path
		 */
		private String path;
		
		/**
		 * root directory
		 */
		private AbstractFileFtp() throws IOException {
			ftpclient = new FTPClient();
			reconnect();
			
			file = getRandomFtpFile(configuration.relative_path);
			
			if (file == null) {
				throw new IOException("Can't found root path");
			}
			path = "/";
		}
		
		private void reconnect() throws IOException {
			try {
				ftpclient.disconnect();
			} catch (Exception e) {
			}
			
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
			
			if (ftp_active) {
				ftpclient.enterLocalActiveMode();
			} else {
				ftpclient.enterLocalPassiveMode();
			}
		}
		
		private AbstractFileFtp(AbstractFileFtp referer, FTPFile file, String path) {
			this.ftpclient = referer.ftpclient;
			this.file = file;
			this.path = path;
			
			if (file.isDirectory() & (path.endsWith("/") == false)) {
				this.path = path + "/";
			}
		}
		
		public List<AbstractFile> listFiles() {
			try {
				ftpclient.setListHiddenFiles(true);
				FTPFile[] list = ftpclient.listFiles(root_path + path);
				if (list == null) {
					return new ArrayList<AbstractFile>();
				}
				
				ArrayList<AbstractFile> al_ablist = new ArrayList<AbstractFile>(list.length);
				for (int pos = 0; pos < list.length; pos++) {
					if (list[pos].getName().equals(".")) {
						continue;
					}
					if (list[pos].getName().equals("..")) {
						continue;
					}
					al_ablist.add(new AbstractFileFtp(this, list[pos], path + list[pos].getName()));
				}
				
				return al_ablist;
			} catch (IOException e) {
				Loggers.Storage_FTP.error("Can't list files, " + this, e);
				return null;
			}
		}
		
		public boolean canRead() {
			return file.hasPermission(FTPFile.USER_ACCESS, FTPFile.READ_PERMISSION);
		}
		
		public boolean canWrite() {
			/*if (configurator.readonly) {
				return false;
			}*/
			return file.hasPermission(FTPFile.USER_ACCESS, FTPFile.WRITE_PERMISSION);
		}
		
		public long lastModified() {
			return file.getTimestamp().getTimeInMillis();
		}
		
		public String getPath() {
			if (path.endsWith("/")) {
				return path.substring(0, path.length() - 1);
			} else {
				return path;
			}
		}
		
		public boolean isDirectory() {
			return file.isDirectory();
		}
		
		public boolean isFile() {
			return file.isFile();
		}
		
		public boolean isHidden() {
			return (file.getName().startsWith("."));
		}
		
		public String getName() {
			return file.getName();
		}
		
		public long length() {
			return file.getSize();
		}
		
		public void close() {
			try {
				if (ftpclient.isConnected()) {
					ftpclient.disconnect();
				}
			} catch (IOException e) {
				Loggers.Storage_FTP.error("Can't disconnect to FTP server, " + this, e);
			}
		}
		
		/**
		 * Fake FTPFile, only for respond to root path queries.
		 */
		private class RootFTPFile extends FTPFile {
			
			private static final long serialVersionUID = 4499653893317959711L;
			
			public String getName() {
				return "";
			}
			
			public long getSize() {
				return 0;
			}
			
			public Calendar getTimestamp() {
				Calendar result = Calendar.getInstance();
				result.setTimeInMillis(0);
				return result;
			}
			
			public boolean hasPermission(int access, int permission) {
				return true;
			}
			
			public boolean isDirectory() {
				return true;
			}
			
			public boolean isFile() {
				return false;
			}
			
		}
		
		/**
		 * Fake FTPFile, only for respond to new file queries.
		 */
		private class EmptyFTPFile extends FTPFile {
			
			private static final long serialVersionUID = 3506850846197477990L;
			
			String name;
			
			public EmptyFTPFile(String name) {
				this.name = name;
			}
			
			public String getName() {
				return name;
			}
			
			public long getSize() {
				return -1;
			}
			
			public Calendar getTimestamp() {
				Calendar result = Calendar.getInstance();
				result.setTimeInMillis(0);
				return result;
			}
			
			public boolean hasPermission(int access, int permission) {
				return false;
			}
			
			public boolean isDirectory() {
				return false;
			}
			
			public boolean isFile() {
				return false;
			}
			
		}
		
		private class FTPClosableInputStream extends BufferedInputStream {
			public FTPClosableInputStream(InputStream in, int buffersize) {
				super(in, buffersize);
			}
			
			public void close() throws IOException {
				super.close();
				if ((ftpclient.isConnected() == false) | (ftpclient.completePendingCommand() == false)) {
					ftpclient.disconnect();
					reconnect();
				}
			}
		}
		
		public BufferedInputStream getInputStream(int buffersize) {
			try {
				ftpclient.setFileType(FTP.BINARY_FILE_TYPE);
				String fullpath = root_path + path;
				if (root_path.equals("/")) {
					fullpath = path;
				}
				
				InputStream is = ftpclient.retrieveFileStream(fullpath);
				if (is == null) {
					throw new NullPointerException("Retrieve File Stream is null");
				}
				return new FTPClosableInputStream(is, buffersize);
			} catch (IOException e) {
				Loggers.Storage_FTP.error("Can't access to file, " + this, e);
			}
			return null;
		}
		
		private class FTPClosableOutputStream extends BufferedOutputStream {
			
			public FTPClosableOutputStream(OutputStream out, int buffer) {
				super(out, buffer);
			}
			
			public void close() throws IOException {
				super.close();
				if ((ftpclient.isConnected() == false) | (ftpclient.completePendingCommand() == false)) {
					ftpclient.disconnect();
					reconnect();
				}
			}
			
		}
		
		public BufferedOutputStream getOutputStream(int buffersize) {
			/*if (configurator.readonly) {
				return null;
			}*/
			try {
				ftpclient.setFileType(FTP.BINARY_FILE_TYPE);
				String fullpath = root_path + path;
				if (root_path.equals("/")) {
					fullpath = path;
				}
				
				OutputStream os = ftpclient.storeFileStream(fullpath);
				if (os == null) {
					throw new NullPointerException("Store File Stream is null");
				}
				return new FTPClosableOutputStream(os, buffersize);
			} catch (IOException e) {
				Loggers.Storage_FTP.error("Can't access to file, " + this, e);
			}
			return null;
		}
		
		public AbstractFile moveTo(String newpath) {
			/*if (configurator.readonly) {
				return null;
			}*/
			if (newpath.startsWith("/") == false) {
				newpath = "/" + newpath;
			}
			
			try {
				String newpath_ = (root_path + newpath).replaceAll("//", "/");
				
				if (FTPReply.isPositiveIntermediate(ftpclient.rnfr(root_path + path))) {
					if (FTPReply.isPositiveCompletion(ftpclient.rnto(newpath_))) {
						return new AbstractFileFtp(this, getRandomFtpFile(newpath_), newpath);
					} else {
						throw new IOException("Can't rename, set to new name");
					}
				} else {
					throw new IOException("Can't rename, prepare name");
				}
			} catch (IOException e) {
				/** Maybe a security problem */
				Loggers.Storage_FTP.error("Can't access to file, " + this, e);
				return null;
			}
		}
		
		public AbstractFile mkdir(String newpath) {
			/*if (configurator.readonly) {
				return null;
			}*/
			if (newpath.startsWith("/") == false) {
				newpath = "/" + newpath;
			}
			
			String fullpath = root_path + newpath;
			fullpath = fullpath.replaceAll("//", "/");
			
			try {
				
				if (ftpclient.makeDirectory(fullpath)) {
					return new AbstractFileFtp(this, getRandomFtpFile(fullpath), newpath);
				} else {
					return null;
				}
			} catch (IOException e) {
				/** Maybe a security problem */
				Loggers.Storage_FTP.error("Can't access to file, " + this, e);
				return null;
			}
		}
		
		private FTPFile getRandomFtpFile(String newpath) {
			if (newpath.equals("/")) {
				return new RootFTPFile();
			} else {
				if (newpath.endsWith("/")) {
					newpath = newpath.substring(0, newpath.length() - 1);
				}
				if (newpath.startsWith("/") == false) {
					newpath = "/" + newpath;
				}
				/**
				 * The function "ftpclient.mlistFile(pathname)" don't exist on all servers...
				 * It do a list of the content of the parent path whose we want to set for path, and select our directory/file element
				 * Stupid, but functionnal.
				 */
				FTPFile[] list;
				try {
					ftpclient.setListHiddenFiles(true);
					list = ftpclient.listFiles(newpath.substring(0, newpath.lastIndexOf("/")));
				} catch (IOException e) {
					return null;
				}
				for (int pos = 0; pos < list.length; pos++) {
					if (list[pos].getName().equals(newpath.substring(newpath.lastIndexOf("/") + 1, newpath.length()))) {
						return list[pos];
					}
				}
				/**
				 * Ask file don't exist...
				 */
				return new EmptyFTPFile(newpath.substring(newpath.lastIndexOf("/") + 1, newpath.length()));
			}
		}
		
		public AbstractFile getAbstractFile(String newpath) {
			if (newpath.startsWith("/") == false) {
				newpath = "/" + newpath;
			}
			
			return new AbstractFileFtp(this, getRandomFtpFile(root_path + newpath), newpath);
		}
		
		private boolean recursiveDirectoryDelete(String fullpath) {
			try {
				ftpclient.setListHiddenFiles(true);
				
				FTPFile[] list = ftpclient.listFiles(fullpath);
				String subfilename;
				for (int pos = 0; pos < list.length; pos++) {
					subfilename = (fullpath + "/" + list[pos].getName()).replaceAll("//", "/");
					
					if (list[pos].isDirectory()) {
						if (recursiveDirectoryDelete(subfilename) == false) {
							return false;
						}
					} else {
						if (ftpclient.deleteFile(subfilename) == false) {
							return false;
						}
					}
				}
				return ftpclient.removeDirectory(fullpath);
			} catch (IOException e) {
				Loggers.Storage_FTP.error("Can't delete file", e);
				return false;
			}
		}
		
		public boolean delete() {
			/*if (configurator.readonly) {
				return false;
			}*/
			
			String fullpath = (root_path + path).replaceAll("//", "/");
			
			try {
				reconnect();
			} catch (IOException e1) {
				Loggers.Storage_FTP.error("Can't delete file (reconnect)", e1);
				return false;
			}
			
			if (isDirectory()) {
				return recursiveDirectoryDelete(fullpath);
			} else {
				try {
					return ftpclient.deleteFile(fullpath);
				} catch (IOException e) {
					Loggers.Storage_FTP.error("Can't delete file", e);
					return false;
				}
			}
		}
		
	}
	
	public AbstractFile getRootPath() throws NullPointerException, IOException {
		return new AbstractFileFtp();
	}
	
}
