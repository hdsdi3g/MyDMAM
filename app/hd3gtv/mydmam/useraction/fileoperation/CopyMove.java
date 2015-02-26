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
 * Copyright (C) hdsdi3g for hd3g.tv 26 févr. 2015
 * 
*/
package hd3gtv.mydmam.useraction.fileoperation;

import hd3gtv.mydmam.manager.JobProgression;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class CopyMove {
	
	public static void checkExistsCanRead(File element) throws IOException, NullPointerException {
		if (element == null) {
			throw new NullPointerException("element is null");
		}
		if (element.exists() == false) {
			throw new FileNotFoundException("\"" + element.getPath() + "\" in filesytem");
		}
		if (element.canRead() == false) {
			throw new IOException("Can't read element \"" + element.getPath() + "\"");
		}
		
	}
	
	public static void checkIsDirectory(File element) throws FileNotFoundException {
		if (element.isDirectory() == false) {
			throw new FileNotFoundException("\"" + element.getPath() + "\" is not a directory");
		}
	}
	
	public static void checkIsWritable(File element) throws IOException {
		if (element.canWrite() == false) {
			throw new IOException("\"" + element.getPath() + "\" is not writable");
		}
	}
	
	private File source;
	private File destination;
	private boolean delete_during_copy;
	private boolean delete_after_copy;
	private ArrayList<File> list_to_copy;
	private JobProgression progression;
	private byte[] buffer;
	
	public CopyMove(File source, File destination) throws NullPointerException, IOException {
		this.source = source;
		this.destination = destination;
		
		checkExistsCanRead(source);
		
		checkExistsCanRead(destination);
		checkIsDirectory(destination);
		checkIsWritable(destination);
		
		list_to_copy = new ArrayList<File>();
		buffer = new byte[512 * 1024];
	}
	
	public void setDelete_after_copy(boolean delete_after_copy) throws IOException {
		this.delete_after_copy = delete_after_copy;
		if (delete_after_copy) {
			checkIsWritable(source);
		}
	}
	
	public void setDelete_during_copy(boolean delete_during_copy) throws IOException {
		this.delete_during_copy = delete_during_copy;
		if (delete_during_copy) {
			checkIsWritable(source);
		}
	}
	
	public void setProgression(JobProgression progression) {
		this.progression = progression;
	}
	
	public void operate() throws IOException {
		/**
		 * Dirlist source
		 */
		list_to_copy.add(source);
		if (source.isDirectory()) {
			// TODO recursive scan
		}
		
		/**
		 * Copy source
		 */
		File item_to_copy;
		for (int pos_ltc = list_to_copy.size() - 1; pos_ltc > -1; pos_ltc--) {
			item_to_copy = list_to_copy.get(pos_ltc);
			// item_to_copy
		}
		
		if (delete_after_copy) {
			/**
			 * Delete source
			 */
			// TODO
		}
	}
	
	private void copyFile(File source_file, File destination_file) throws Exception {
		InputStream in = null;
		OutputStream out = null;
		try {
			in = new FileInputStream(source_file);
			out = new FileOutputStream(destination_file);
			
			// Transfer bytes from in to out
			int len;
			long pos = 0;
			int progress_size = (int) (source_file.length() / (1024 * 1024));
			int last_progress = 0;
			int progress;
			
			while ((len = in.read(buffer)) > 0) {
				out.write(buffer, 0, len);
				
				pos += len;
				progress = (int) (pos / (1024 * 1024));
				if (progress > last_progress) {
					progress = last_progress;
					// progression.updateProgress(progress, progress_size);
				}
			}
		} finally {
			if (in != null) {
				in.close();
			}
			if (out != null) {
				out.close();
			}
		}
		
		destination_file.setExecutable(source_file.canExecute());
		destination_file.setLastModified(source_file.lastModified());
		
		if (delete_during_copy) {
			if (source_file.delete() == false) {
				// TODO err
			}
		}
	}
	
}
