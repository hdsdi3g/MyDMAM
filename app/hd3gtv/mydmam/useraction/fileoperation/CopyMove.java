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

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.manager.JobProgression;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

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
	private boolean delete_after_copy;
	private ArrayList<File> list_to_copy;
	private JobProgression progression;
	
	private long total_size = 0;
	private long progress_copy = 0;
	private int actual_progress_value = 0;
	private int last_progress_value = 0;
	private int progress_size = 0;
	
	public CopyMove(File source, File destination) throws NullPointerException, IOException {
		this.source = source;
		this.destination = destination;
		
		checkExistsCanRead(source);
		
		checkExistsCanRead(destination);
		checkIsDirectory(destination);
		checkIsWritable(destination);
	}
	
	public void setDelete_after_copy(boolean delete_after_copy) throws IOException {
		this.delete_after_copy = delete_after_copy;
		if (delete_after_copy) {
			checkIsWritable(source);
		}
	}
	
	public void setProgression(JobProgression progression) {
		this.progression = progression;
	}
	
	public void operate() throws IOException {
		if (delete_after_copy) {
			/**
			 * Move operation
			 * Can we simply move ?
			 */
			File moveto = new File(destination.getAbsoluteFile() + File.separator + source.getName());
			if (source.renameTo(moveto)) {
				return;
			} else {
				Log2Dump dump = new Log2Dump();
				dump.add("source", source);
				dump.add("moveto", moveto);
				Log2.log.debug("Can't simply move, do a copy + move operation", dump);
			}
		}
		
		/**
		 * Dirlist source
		 */
		if (source.isDirectory()) {
			dirListing();
		} else {
			list_to_copy.add(source);
		}
		
		File item_to_copy;
		/**
		 * Compute total_size to display progress.
		 */
		total_size = 0;
		for (int pos_ltc = 0; pos_ltc < list_to_copy.size(); pos_ltc++) {
			item_to_copy = list_to_copy.get(pos_ltc);
			total_size += item_to_copy.length();
		}
		
		/**
		 * check UsableSpace
		 */
		if (destination.getUsableSpace() > 0) {
			if (total_size > destination.getUsableSpace()) {
				throw new IOException("Not enough space in destination directory");
			}
		}
		
		/**
		 * Copy source
		 */
		File destination_element_to_copy;
		progress_size = ((int) total_size / (1024 * 1024));
		actual_progress_value = 0;
		last_progress_value = 0;
		progress_copy = 0;
		
		int chars_to_ignore = source.getParentFile().getAbsolutePath().length() + 1;
		String base_destination_path = destination.getAbsolutePath() + File.separator;
		
		for (int pos_ltc = 0; pos_ltc < list_to_copy.size(); pos_ltc++) {
			item_to_copy = list_to_copy.get(pos_ltc);
			destination_element_to_copy = new File(base_destination_path + item_to_copy.getAbsolutePath().substring(chars_to_ignore));
			
			if (item_to_copy.isDirectory()) {
				if (destination_element_to_copy.exists()) {
					if (destination_element_to_copy.isDirectory() == false) {
						throw new IOException("Destination directory exists, and it not a directory: \"" + destination_element_to_copy.getPath() + "\"");
					}
				} else {
					if (destination_element_to_copy.mkdirs() == false) {
						throw new IOException("Can't create destination directory: \"" + destination_element_to_copy.getPath() + "\"");
					}
				}
			} else {
				copyFile(item_to_copy, destination_element_to_copy);
			}
			
			if (progression != null) {
				progress_copy += destination_element_to_copy.length();
				actual_progress_value = (int) (progress_copy / (1024 * 1024));
				if (actual_progress_value > last_progress_value) {
					last_progress_value = actual_progress_value;
					progression.updateProgress(actual_progress_value, progress_size);
				}
			}
		}
		
		if (delete_after_copy) {
			/**
			 * To complete a move operation
			 */
			for (int pos_ltc = list_to_copy.size() - 1; pos_ltc > -1; pos_ltc--) {
				item_to_copy = list_to_copy.get(pos_ltc);
				if (item_to_copy.delete() == false) {
					throw new IOException("Can't delete moved file: \"" + item_to_copy.getPath() + "\"");
				}
			}
		}
	}
	
	/**
	 * 50 Mbytes in bytes
	 */
	private static final long FIFTY_MB = 52428800;
	
	private void copyFile(File source_file, File destination_file) throws IOException {
		if (destination_file.exists()) {
			Log2.log.info("destination_file exists, it will be overwrite", new Log2Dump("file", destination_file));
		}
		
		/**
		 * Imported from org.apache.commons.io.FileUtils
		 * Licensed to the Apache Software Foundation,
		 * http://www.apache.org/licenses/LICENSE-2.0
		 */
		FileInputStream fis = null;
		FileOutputStream fos = null;
		FileChannel input = null;
		FileChannel output = null;
		try {
			fis = new FileInputStream(source_file);
			fos = new FileOutputStream(destination_file);
			input = fis.getChannel();
			output = fos.getChannel();
			long size = input.size();
			long pos = 0;
			long count = 0;
			while (pos < size) {
				count = (size - pos) > FIFTY_MB ? FIFTY_MB : (size - pos);
				pos += output.transferFrom(input, pos, count);
				
				if (progression != null) {
					actual_progress_value = (int) ((pos + progress_copy) / (1024 * 1024));
					if (actual_progress_value > last_progress_value) {
						last_progress_value = actual_progress_value;
						progression.updateProgress(actual_progress_value, progress_size);
					}
				}
			}
		} finally {
			IOUtils.closeQuietly(output);
			IOUtils.closeQuietly(fos);
			IOUtils.closeQuietly(input);
			IOUtils.closeQuietly(fis);
		}
		
		if (source_file.length() != destination_file.length()) {
			throw new IOException("Failed to copy full contents from '" + source_file + "' to '" + destination_file + "'");
		}
		if (destination_file.setExecutable(source_file.canExecute()) == false) {
			Log2.log.error("Can't set Executable status to dest file", new IOException(destination_file.getPath()));
		}
		if (destination_file.setLastModified(source_file.lastModified()) == false) {
			Log2.log.error("Can't set LastModified status to dest file", new IOException(destination_file.getPath()));
		}
	}
	
	private void dirListing() throws IOException {
		ArrayList<File> bucket = new ArrayList<File>();
		ArrayList<File> next_bucket = new ArrayList<File>();
		bucket.add(source);
		
		File bucket_item;
		File[] list_content;
		
		while (true) {
			for (int pos_b = 0; pos_b < bucket.size(); pos_b++) {
				bucket_item = bucket.get(pos_b).getCanonicalFile();
				
				if (FileUtils.isSymlink(bucket_item)) {
					continue;
				}
				
				if (list_to_copy.contains(bucket_item)) {
					continue;
				}
				if (bucket_item.isDirectory()) {
					list_content = bucket_item.listFiles();
					for (int pos_lc = 0; pos_lc < list_content.length; pos_lc++) {
						next_bucket.add(list_content[pos_lc]);
					}
				}
				list_to_copy.add(bucket_item);
			}
			if (next_bucket.isEmpty()) {
				return;
			}
			bucket.clear();
			bucket.addAll(next_bucket);
			next_bucket.clear();
		}
	}
	
	// TODO internal tests for debugging
	
	public static void main(String[] args) throws Exception {
		// TODO create temp file
		// TODO rename temp file, restore source
		// TODO move temp file + check, restore source
		// TODO copy temp file + check
		// TODO delete temp file, and dest
		
		// TODO create temp dir tree with files, one must to be > 50 MB
		// TODO rename temp dir, restore source
		// TODO move temp dir + check, restore source
		// TODO copy temp dir + check
		// TODO delete temp dir, and dest
		
		/**
		 * TODO test api for:
		 * - create temp files, and one > 50 MB
		 * - create tree dir
		 * - force rename or just copy/delete
		 * - check all files
		 * - recursive delete
		 */
		
	}
	
}
