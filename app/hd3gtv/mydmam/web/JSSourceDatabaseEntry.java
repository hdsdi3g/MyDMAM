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
import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.util.Comparator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;

public class JSSourceDatabaseEntry {
	
	JSSourceDatabaseEntry() {
	}
	
	/**
	 * Like "mydmam/async/pathindex/tools.jsx"
	 */
	private String relative_file_name;
	
	/**
	 * Like "/app/react"
	 */
	private String relative_root_name;
	private long size;
	private long date;
	private String hash;
	
	final static class Serializer implements JsonSerializer<JSSourceDatabaseEntry>, JsonDeserializer<JSSourceDatabaseEntry> {
		
		public JsonElement serialize(JSSourceDatabaseEntry src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject result = new JsonObject();
			if (File.separator.equals("\\")) {
				result.addProperty("relative_file_name", src.relative_file_name.replace("\\", "/"));
				result.addProperty("relative_root_name", src.relative_root_name.replace("\\", "/"));
			} else {
				result.addProperty("relative_file_name", src.relative_file_name);
				result.addProperty("relative_root_name", src.relative_root_name);
			}
			result.addProperty("size", src.size);
			result.addProperty("date", src.date);
			result.addProperty("hash", src.hash);
			return result;
		}
		
		public JSSourceDatabaseEntry deserialize(JsonElement jejson, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JSSourceDatabaseEntry result = new JSSourceDatabaseEntry();
			JsonObject src = jejson.getAsJsonObject();
			
			if (File.separator.equals("\\")) {
				result.relative_file_name = src.get("relative_file_name").getAsString().replace("/", "\\");
				result.relative_root_name = src.get("relative_root_name").getAsString().replace("/", "\\");
			} else {
				result.relative_file_name = src.get("relative_file_name").getAsString();
				result.relative_root_name = src.get("relative_root_name").getAsString();
			}
			result.size = src.get("size").getAsLong();
			result.date = src.get("date").getAsLong();
			result.hash = src.get("hash").getAsString();
			
			return result;
		}
		
	}
	
	/**
	 * @param module_path like "/opt/mydmam-my-super-module"
	 * @param real_file like "/opt/mydmam-my-super-module/app/react/a_js_scope/myfile.js"
	 * @param relative_root_name like "/app/react"
	 */
	JSSourceDatabaseEntry(File module_path, File real_file, String relative_root_name) {
		this.relative_root_name = relative_root_name;
		if (relative_root_name.endsWith(File.separator)) {
			relative_root_name = relative_root_name.substring(0, relative_root_name.length() - 1);
		}
		relative_file_name = real_file.getAbsolutePath().substring(module_path.getAbsolutePath().length() + relative_root_name.length() + 1);
		
		size = real_file.length();
		date = real_file.lastModified();
		try {
			hash = makeMD5(real_file);
		} catch (IOException e) {
			Loggers.Play_JSSource.error("Can't open JS file: " + real_file, e);
		}
	}
	
	void checkRealFile(File module_path) throws FileNotFoundException, IOException {
		File real_file = new File(module_path.getPath() + File.separator + relative_root_name + File.separator + relative_file_name);
		
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
			Loggers.Play_JSSource.trace("Check file " + real_file + ": same date, this file seems the same");
			return;
		}
		
		String new_md5 = makeMD5(real_file);
		if (hash.equalsIgnoreCase(new_md5) == false) {
			throw new IOException("File as changed for " + real_file.getPath() + " (" + hash.substring(0, 5) + "... -> " + new_md5.substring(0, 5) + "...)");
		}
		Loggers.Play_JSSource.trace("Check file " + real_file + ": same hash");
	}
	
	void refresh(File module_path) {
		File real_file = new File(module_path.getPath() + File.separator + relative_root_name + File.separator + relative_file_name);
		Loggers.Play_JSSource.trace("Refresh file " + real_file + " (module_path: " + module_path + ")");
		size = real_file.length();
		date = real_file.lastModified();
		try {
			hash = makeMD5(real_file);
		} catch (IOException e) {
			Loggers.Play_JSSource.error("Can't compute MD5", e);
		}
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
			Loggers.Play_JSSource.error("Can't compute MD5", e);
			return "";
		}
	}
	
	/**
	 * @param source Read line by line the file, and avoid to end-line chars problems and empty lines.
	 */
	private static String makeMD5(File source) throws IOException {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			
			FileUtils.readLines(source).forEach(line -> {
				String trimed_line = line.trim();
				if (trimed_line.isEmpty() == false) {
					md.update(trimed_line.getBytes());
				}
			});
			
			String result = MyDMAM.byteToString(md.digest());
			Loggers.Play_JSSource.trace("Check MD5 for file " + source + ": " + result);
			return result;
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			Loggers.Play_JSSource.error("Can't compute MD5", e);
			return "";
		}
	}
	
	public String toString() {
		return getRelativePath();
	}
	
	String getRelativePath() {
		return relative_root_name + File.separator + relative_file_name;
	}
	
	/**
	 * @return null if no scope
	 */
	String computeJSScope() {
		if (relative_file_name.indexOf(File.separator) > -1) {
			String scope = relative_file_name.substring(0, FilenameUtils.indexOfLastSeparator(relative_file_name));
			StringBuffer sb = new StringBuffer();
			for (int pos = 0; pos < scope.length(); pos++) {
				String thischar = scope.substring(pos, pos + 1);
				if (thischar.equals(File.separator)) {
					sb.append(".");
				} else {
					sb.append(thischar);
				}
			}
			return sb.toString();
		}
		return null;
	}
	
	File getRealFile(File module_path) {
		return new File(module_path.getPath() + File.separator + relative_root_name + File.separator + relative_file_name);
	}
	
	String getHash() {
		return hash;
	}
	
	private File computeOutputFilepath(File module_path, File output_directory) {
		File source_file = getRealFile(module_path);
		String source_base_name = FilenameUtils.getBaseName(source_file.getPath());
		String source_scope = computeJSScope();
		
		StringBuilder sb = new StringBuilder();
		sb.append(output_directory.getPath());
		sb.append(File.separator);
		if (source_scope != null) {
			sb.append(source_scope);
			sb.append(".");
		} else {
			sb.append("_");
		}
		sb.append(source_base_name);
		sb.append(".js");
		return new File(sb.toString());
	}
	
	private transient File transformed_version;
	
	File computeTransformedFilepath(File module_path, File transformed_directory) {
		if (transformed_version == null) {
			transformed_version = computeOutputFilepath(module_path, transformed_directory);
		}
		return transformed_version;
	}
	
	private transient File reduced_version;
	
	File computeReducedFilepath(File module_path, File reduced_directory) {
		if (reduced_version == null) {
			reduced_version = computeOutputFilepath(module_path, reduced_directory);
		}
		return reduced_version;
	}
	
	static final SortComparator COMPARATOR = new SortComparator();
	
	static class SortComparator implements Comparator<JSSourceDatabaseEntry> {
		private SortComparator() {
		}
		
		public int compare(JSSourceDatabaseEntry o1, JSSourceDatabaseEntry o2) {
			return o1.relative_file_name.compareTo(o2.relative_file_name);
		}
		
	}
	
}
