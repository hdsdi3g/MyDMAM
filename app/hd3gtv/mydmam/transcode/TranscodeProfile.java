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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam.transcode;

import hd3gtv.mydmam.taskqueue.Profile;

import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class TranscodeProfile extends Profile {
	
	static final String TAG_PROGRESSFILE = "<%$PROGRESSFILE%>";
	static final String TAG_INPUTFILE = "<%$INPUTFILE%>";
	static final String TAG_OUTPUTFILE = "<%$OUTPUTFILE%>";
	static final String TAG_STARTVAR = "<%$VAR=";
	static final String TAG_ENDVAR = "%>";
	
	private ArrayList<String> param;
	private String extention;
	
	private OutputFormat outputformat;
	
	TranscodeProfile(String category, String name) {
		super(category, name);
		param = new ArrayList<String>();
	}
	
	ArrayList<String> getParam() {
		return param;
	}
	
	void setExtention(String extention) {
		this.extention = extention;
	}
	
	/**
	 * @return start with "."
	 */
	public String getExtention(String default_value) {
		if (extention == null) {
			if (default_value.startsWith(".")) {
				return default_value;
			} else {
				return "." + default_value;
			}
		} else {
			if (extention.startsWith(".")) {
				return extention;
			} else {
				return "." + extention;
			}
		}
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(getCategory());
		sb.append("[\"");
		sb.append(getName());
		sb.append("\"]\t");
		
		for (int pos = 0; pos < param.size(); pos++) {
			if (param.get(pos).split(" ").length > 1) {
				sb.append("\"");
				sb.append(param.get(pos));
				sb.append("\"");
			} else {
				sb.append(param.get(pos));
			}
			sb.append(" ");
		}
		
		return sb.toString().trim();
	}
	
	void testValidityProfile() throws NullPointerException {
		if (getCategory() == null) {
			throw new NullPointerException("\"profile_type\" can't to be null : check configuration.");
		}
		if (getName() == null) {
			throw new NullPointerException("\"profile_name\" can't to be null : check configuration.");
		}
		
		for (int pos = 0; pos < param.size(); pos++) {
			if (param.get(pos).equals(TAG_OUTPUTFILE)) {
				return;
			}
		}
		throw new NullPointerException("No <outputfile/> in command check configuration.");
	}
	
	public ArrayList<String> makeCommandline(String source_file_path, String dest_file_path) {
		return makeCommandline(source_file_path, dest_file_path, null);
	}
	
	public ArrayList<String> makeCommandline(String source_file_path, String dest_file_path, String progress_file_path) {
		ArrayList<String> cmdline = new ArrayList<String>();
		
		for (int pos = 0; pos < param.size(); pos++) {
			if (param.get(pos).equals(TAG_INPUTFILE)) {
				if (source_file_path == null) {
					cmdline.add("-");
				} else {
					cmdline.add(source_file_path);
				}
			} else if (param.get(pos).equals(TAG_OUTPUTFILE)) {
				cmdline.add(dest_file_path);
			} else if (param.get(pos).equals(TAG_PROGRESSFILE)) {
				if (progress_file_path != null) {
					cmdline.add(progress_file_path);
				} else {
					cmdline.add(System.getProperty("java.io.tmpdir") + File.separator + "progress.txt");
				}
			} else {
				cmdline.add(param.get(pos));
			}
		}
		
		return cmdline;
	}
	
	void setOutputformat(LinkedHashMap<String, ?> configuration_item) {
		outputformat = new OutputFormat(configuration_item);
	}
	
	public OutputFormat getOutputformat() {
		return outputformat;
	}
	
	public class OutputFormat {
		private int width = -1;
		private int height = -1;
		private boolean faststarted = false;
		
		private OutputFormat(LinkedHashMap<String, ?> configuration_item) {
			try {
				width = (Integer) configuration_item.get("width");
			} catch (Exception e) {
			}
			try {
				height = (Integer) configuration_item.get("height");
			} catch (Exception e) {
			}
			try {
				faststarted = (Boolean) configuration_item.get("faststart");
			} catch (Exception e) {
			}
		}
		
		public Point getResolution() {
			return new Point(width, height);
		}
		
		public boolean isFaststarted() {
			return faststarted;
		}
	}
	
}
