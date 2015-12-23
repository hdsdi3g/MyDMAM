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
package hd3gtv.mydmam.web;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;

import org.apache.commons.io.FileUtils;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;

public class JSDatabaseEntry {
	
	JSDatabaseEntry() {
	}
	
	private String relative_file_name;
	private String relative_root_name;
	private long size;
	private long date;
	private String hash;
	
	JSDatabaseEntry(File module_path, File real_file, String relative_root_name) {
		this.relative_root_name = relative_root_name;
		relative_file_name = real_file.getAbsolutePath().substring(module_path.getAbsolutePath().length() + relative_root_name.length());
		
		size = real_file.length();
		date = real_file.lastModified();
		try {
			hash = makeMD5(FileUtils.readFileToString(real_file));
		} catch (IOException e) {
			Loggers.Play.error("Can't open JS file: " + real_file, e);
		}
	}
	
	void checkRealFile(File module_path) throws FileNotFoundException, IOException {
		File real_file = new File(module_path.getPath() + File.separator + relative_root_name + relative_file_name);
		if (real_file.exists() == false) {
			throw new FileNotFoundException(real_file.getPath());
		}
		if (real_file.length() != size) {
			throw new IOException("Size as changed for " + real_file.getPath() + " (" + size + " -> " + real_file.length() + ")");
		}
		if (real_file.lastModified() == date) {
			/**
			 * Don't compute MD5, this file seems the same.
			 */
			return;
		}
		String new_md5 = makeMD5(FileUtils.readFileToString(real_file));
		if (hash.equalsIgnoreCase(new_md5) == false) {
			throw new IOException("File as changed for " + real_file.getPath() + " (" + hash.substring(0, 5) + "... -> " + new_md5.substring(0, 5) + "...)");
		}
		return;
	}
	
	static String makeKeyName(File module_path, File source) {
		return "jsfile:" + makeMD5(source.getAbsolutePath().substring(module_path.getAbsolutePath().length() + 1));
	}
	
	private static String makeMD5(String source) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(source.getBytes());
			return MyDMAM.byteToString(md.digest());
		} catch (Exception e) {
			Loggers.Play.error("Can't compute MD5 for JS Database");
			return "";
		}
	}
	
	public String toString() {
		return relative_root_name + relative_file_name;
	}
}
