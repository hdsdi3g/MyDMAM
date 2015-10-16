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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.util.List;

/**
 * Caution : this API is not thread safe !
 */
public interface AbstractFile {
	
	public List<AbstractFile> listFiles();
	
	public boolean canRead();
	
	public boolean canWrite();
	
	public long lastModified();
	
	public String getPath();
	
	public boolean isDirectory();
	
	public boolean isFile();
	
	public boolean isHidden();
	
	public String getName();
	
	public long length();
	
	/**
	 * Close connection to server but don't close specific stream.
	 */
	public void close();
	
	/**
	 * Don't forget to close the stream
	 */
	public BufferedInputStream getInputStream(int buffersize);
	
	/**
	 * Overwrite the actual stream
	 * Don't forget to close the stream
	 */
	public BufferedOutputStream getOutputStream(int buffersize);
	
	/**
	 * @param newpath Always a full path, never relative
	 */
	public AbstractFile moveTo(String newpath);
	
	/**
	 * @param newpath Always a full path, never relative
	 */
	public AbstractFile mkdir(String newpath);
	
	/**
	 * @param newpath Always a full path, never relative
	 */
	public AbstractFile getAbstractFile(String newpath);
	
	/**
	 * Recusive
	 */
	public boolean delete();
	
}
