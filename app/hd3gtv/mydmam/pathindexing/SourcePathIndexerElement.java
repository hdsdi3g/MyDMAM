/*
 * This file is part of MyDMAM
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
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
*/
package hd3gtv.mydmam.pathindexing;

import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.db.Elasticsearch;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.regex.Pattern;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.search.SearchHit;
import org.json.simple.JSONObject;

@SuppressWarnings("unchecked")
public class SourcePathIndexerElement implements Serializable, Log2Dumpable/*, JsonSerializer<SourcePathIndexerElement>*/{
	
	private static final long serialVersionUID = -3421843205650861824L;
	
	public static final String ROOT_DIRECTORY_KEY = SourcePathIndexerElement.hashThis("");
	
	public String currentpath;
	
	public long size = 0;
	
	public long date = 0;
	
	public String storagename;
	
	public String id;
	
	public boolean directory;
	
	public String parentpath;
	
	public long dateindex = 0;
	
	public JSONObject toJson() {
		JSONObject jo = new JSONObject();
		if (currentpath != null) {
			jo.put("path", currentpath);
		}
		jo.put("directory", directory);
		if (size > 0) {
			jo.put("size", size);
		}
		if (date > 0) {
			jo.put("date", date);
		}
		if (dateindex > 0) {
			jo.put("dateindex", dateindex);
		}
		if (storagename != null) {
			jo.put("storagename", storagename);
		}
		if (id != null) {
			jo.put("id", id);
		}
		if (parentpath != null) {
			jo.put("parentpath", hashThis(storagename + ":" + parentpath));
		} else {
			jo.put("parentpath", hashThis(""));
		}
		String sanitise = sanitisePathToFilename();
		if (sanitise != null) {
			jo.put("idxfilename", sanitise);
		}
		
		return jo;
	}
	
	public static final String[] TOSTRING_HEADERS = { "Element key", "Storage index name", "Directory", "Full path", "Id", "Size", "Element date", "Element name", "Simple name", "Extension",
			"Indexing date", "Parent key" };
	
	public String toString(String separator) {
		StringBuffer sb = new StringBuffer();
		sb.append(prepare_key());
		sb.append(separator);
		sb.append(storagename);
		sb.append(separator);
		if (directory) {
			sb.append("directory");
		} else {
			sb.append("file");
		}
		sb.append(separator);
		sb.append(currentpath);
		sb.append(separator);
		sb.append(id);
		sb.append(separator);
		sb.append(size);
		sb.append(separator);
		sb.append(date / 1000l);
		sb.append(separator);
		String filename = currentpath.substring(currentpath.lastIndexOf("/") + 1);
		sb.append(filename);
		sb.append(separator);
		if (currentpath.lastIndexOf(".") > 0) {
			sb.append(filename.substring(0, filename.lastIndexOf(".") - 1));
			sb.append(separator);
			sb.append(filename.substring(filename.lastIndexOf(".")));
		} else {
			sb.append(separator);
		}
		sb.append(separator);
		sb.append(dateindex / 1000l);
		sb.append(separator);
		sb.append(parentpath);
		return sb.toString();
	}
	
	private static final Pattern combining_diacritical_marks = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
	
	/**
	 * remove accents, uderscores from path
	 */
	public String sanitisePathToFilename() {
		if (currentpath == null) {
			return null;
		}
		if (currentpath.equals("")) {
			return null;
		}
		if (currentpath.equals("/")) {
			return null;
		}
		String filename = currentpath;
		if (currentpath.indexOf("/") > -1) {
			int poslastslash = currentpath.lastIndexOf("/");
			int posfileext = currentpath.lastIndexOf(".");
			if (poslastslash + 1 < posfileext - 1) {
				filename = currentpath.substring(poslastslash + 1, posfileext);
			} else {
				filename = currentpath.substring(poslastslash + 1);
			}
		}
		String filename_normalized = combining_diacritical_marks.matcher(Normalizer.normalize(filename, Normalizer.Form.NFD)).replaceAll("").trim().toLowerCase();
		
		StringBuffer sb = new StringBuffer();
		char[] filename_chr = filename_normalized.toCharArray();
		char chr = 0x0;
		char previous_chr;
		for (int pos = 0; pos < filename_chr.length; pos++) {
			previous_chr = chr;
			chr = filename_chr[pos];
			if (chr > 47 & chr < 58) {
				/** 0-9 */
				sb.append(chr);
				continue;
			}
			if (chr > 64 & chr < 91) {
				/** A-Z */
				sb.append(chr);
				continue;
			}
			if (chr > 96 & chr < 123) {
				/** a-z */
				sb.append(chr);
				continue;
			}
			/**
			 * Add space if last chr is not a space
			 */
			if (previous_chr != 32) {
				/** space */
				sb.append(" ");
			}
		}
		
		return sb.toString();
	}
	
	public static SourcePathIndexerElement fromESResponse(GetResponse response) {
		return fromESResponse(Elasticsearch.getJSONFromSimpleResponse(response));
	}
	
	public static SourcePathIndexerElement fromESResponse(SearchHit hit) {
		return fromESResponse(Elasticsearch.getJSONFromSimpleResponse(hit));
	}
	
	private static SourcePathIndexerElement fromESResponse(JSONObject jo) {
		if (jo == null) {
			return null;
		}
		
		SourcePathIndexerElement result = new SourcePathIndexerElement();
		
		if (jo.containsKey("path")) {
			result.currentpath = (String) jo.get("path");
		}
		
		if (jo.containsKey("parentpath")) {
			result.parentpath = (String) jo.get("parentpath");
		}
		if (jo.containsKey("directory")) {
			result.directory = (Boolean) jo.get("directory");
		}
		if (jo.containsKey("storagename")) {
			result.storagename = (String) jo.get("storagename");
		}
		if (jo.containsKey("id")) {
			result.id = (String) jo.get("id");
		}
		if (jo.containsKey("date")) {
			result.date = (Long) jo.get("date");
		}
		if (jo.containsKey("size")) {
			result.size = (Long) jo.get("size");
		}
		if (jo.containsKey("dateindex")) {
			result.dateindex = (Long) jo.get("dateindex");
		}
		return result;
	}
	
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if ((o instanceof SourcePathIndexerElement) == false) {
			return false;
		}
		SourcePathIndexerElement compare = (SourcePathIndexerElement) o;
		
		if (storagename.equals(compare.storagename) == false) {
			return false;
		}
		if (currentpath.equals(compare.currentpath) == false) {
			return false;
		}
		if (directory != compare.directory) {
			return false;
		}
		if (size != compare.size) {
			return false;
		}
		if (date != compare.date) {
			return false;
		}
		return true;
	}
	
	public String prepare_key() {
		return prepare_key(storagename, currentpath);
	}
	
	public static String prepare_key(String storagename, String currentpath) {
		StringBuffer sb = new StringBuffer();
		sb.append(storagename);
		sb.append(":");
		sb.append(currentpath);
		return hashThis(sb.toString());
	}
	
	public static String hashThis(String value) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
			md.update(value.getBytes());
			return MyDMAM.byteToString(md.digest());
		} catch (NoSuchAlgorithmException e) {
			throw new NullPointerException(e.getMessage());
		}
	}
	
	public static SourcePathIndexerElement prepareStorageElement(String storagename) {
		if (storagename == null) {
			throw new NullPointerException("\"storagename\" can't to be null");
		}
		SourcePathIndexerElement element = new SourcePathIndexerElement();
		element.currentpath = "/";
		element.size = 0;
		element.date = 0;
		element.storagename = storagename;
		element.id = null;
		element.directory = true;
		element.parentpath = null;
		element.dateindex = 0;
		return element;
	}
	
	@Override
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("storagename", storagename);
		dump.add("currentpath", currentpath);
		dump.add("id", id);
		dump.add("size", size);
		dump.addDate("date", date);
		dump.add("directory", directory);
		dump.add("parentpath", parentpath);
		dump.addDate("dateindex", dateindex);
		dump.add("sanitised path", this.sanitisePathToFilename());
		dump.add("key", this.prepare_key());
		return dump;
	}
	
}
