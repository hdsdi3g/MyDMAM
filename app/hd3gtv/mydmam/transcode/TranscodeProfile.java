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

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.ConfigurationItem;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;
import hd3gtv.tools.Execprocess;
import hd3gtv.tools.ExecprocessEvent;
import hd3gtv.tools.ExecprocessGettext;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class TranscodeProfile implements Log2Dumpable {
	
	static final String TAG_PROGRESSFILE = "<%$PROGRESSFILE%>";
	static final String TAG_INPUTFILE = "<%$INPUTFILE%>";
	static final String TAG_OUTPUTFILE = "<%$OUTPUTFILE%>";
	static final String TAG_STARTVAR = "<%$VAR=";
	static final String TAG_ENDVAR = "%>";
	
	private ArrayList<String> param;
	private String extension;
	private OutputFormat outputformat;
	
	private String name;
	private String category;
	
	private static ArrayList<TranscodeProfile> profiles;
	
	private TranscodeProfile(String category, String name) {
		this.category = category;
		if (category == null) {
			throw new NullPointerException("\"category\" can't to be null");
		}
		this.name = name;
		if (name == null) {
			throw new NullPointerException("\"name\" can't to be null");
		}
		this.category = this.category.toLowerCase();
		this.name = this.name.toLowerCase();
		param = new ArrayList<String>();
	}
	
	public static boolean isConfigured() {
		return Configuration.global.isElementExists("transcodingprofiles");
	}
	
	static {
		try {
			profiles = new ArrayList<TranscodeProfile>(1);
			if (isConfigured()) {
				HashMap<String, ConfigurationItem> tp_list = Configuration.global.getElement("transcodingprofiles");
				
				if (tp_list.isEmpty()) {
					Log2.log.error("Can't found \"profile\" element in transcoding block in XML configuration", null);
				}
				
				profiles = new ArrayList<TranscodeProfile>(tp_list.size());
				
				String profile_type;
				String profile_name;
				String profile_extension;
				String param;
				for (Map.Entry<String, ConfigurationItem> entry : tp_list.entrySet()) {
					profile_type = Configuration.getValue(tp_list, entry.getKey(), "type", null);
					if (profile_type == null) {
						throw new NullPointerException("Attribute \"type\" in \"profile\" element for transcoding can't to be null");
					} else if (profile_type.equals("")) {
						throw new NullPointerException("Attribute \"type\" in \"profile\" element for transcoding can't to be empty");
					}
					profile_name = entry.getKey();
					
					TranscodeProfile profile = new TranscodeProfile(profile_type, profile_name);
					String[] params = Configuration.getValue(tp_list, entry.getKey(), "command", null).trim().split(" ");
					for (int pos_par = 0; pos_par < params.length; pos_par++) {
						param = params[pos_par].trim();
						if (param.length() == 0) {
							continue;
						}
						if (param.startsWith(TranscodeProfile.TAG_STARTVAR) & param.endsWith(TranscodeProfile.TAG_ENDVAR)) {
							param = param.substring(TranscodeProfile.TAG_STARTVAR.length(), param.length() - TranscodeProfile.TAG_ENDVAR.length());
							param = Configuration.getValue(tp_list, entry.getKey(), param, null);
							if (param == null) {
								throw new NullPointerException("Can't found " + params[pos_par] + " param variable");
							}
							param = param.trim();
						}
						profile.param.add(param);
					}
					
					profile_extension = Configuration.getValue(tp_list, entry.getKey(), "extension", null);
					if (profile_extension != null) {
						if (profile_extension.equals("") == false) {
							profile.extension = profile_extension;
						}
					}
					
					if (Configuration.isElementKeyExists(tp_list, entry.getKey(), "output")) {
						try {
							Object o_output = tp_list.get(entry.getKey()).content.get("output");
							profile.outputformat = profile.new OutputFormat((LinkedHashMap<String, ?>) o_output);
						} catch (Exception e) {
							throw new IOException("Can't load transcoding/" + entry.getKey() + "/output node");
						}
					}
					
					profile.testValidityProfile();
					profiles.add(profile);
				}
				
				Log2Dump dump = new Log2Dump();
				for (int pos = 0; pos < profiles.size(); pos++) {
					dump.add("transcoding profile", pos);
					dump.addAll(profiles.get(pos));
				}
				
				Log2.log.debug("Set transcoding configuration", dump);
			}
		} catch (Exception e) {
			Log2.log.error("Can't load transcoding configuration", e);
		}
	}
	
	/**
	 * @return start with "."
	 */
	public String getExtension(String default_value) {
		if (extension == null) {
			if (default_value.startsWith(".")) {
				return default_value;
			} else {
				return "." + default_value;
			}
		} else {
			if (extension.startsWith(".")) {
				return extension;
			} else {
				return "." + extension;
			}
		}
	}
	
	public final String getCategory() {
		return category;
	}
	
	public final String getName() {
		return name;
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
	
	private ArrayList<String> makeCommandline(String source_file_path, String dest_file_path, String progress_file_path) {
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
	
	public Execprocess prepareExecprocess(String executable, ExecprocessEvent events, File source_file, File... dest_files) throws IOException, NullPointerException {
		if (executable == null) {
			throw new NullPointerException("\"executable\" can't to be null");
		}
		if (source_file == null) {
			throw new NullPointerException("\"source_file\" can't to be null");
		}
		if (dest_files == null) {
			throw new NullPointerException("\"dest_files\" can't to be null");
		}
		if (dest_files.length == 0) {
			throw new NullPointerException("\"dest_files\" can't to be empty");
		}
		if (dest_files.length > 1) {
			return new Execprocess(executable, makeCommandline(source_file.getCanonicalPath(), dest_files[0].getCanonicalPath(), dest_files[1].getCanonicalPath()), events);
		} else {
			return new Execprocess(executable, makeCommandline(source_file.getCanonicalPath(), dest_files[0].getCanonicalPath(), null), events);
		}
	}
	
	public ExecprocessGettext prepareExecprocessGettext(String executable, File source_file, File... dest_files) throws IOException, NullPointerException {
		if (executable == null) {
			throw new NullPointerException("\"executable\" can't to be null");
		}
		if (source_file == null) {
			throw new NullPointerException("\"source_file\" can't to be null");
		}
		if (dest_files == null) {
			throw new NullPointerException("\"dest_files\" can't to be null");
		}
		if (dest_files.length == 0) {
			throw new NullPointerException("\"dest_files\" can't to be empty");
		}
		if (dest_files.length > 1) {
			return new ExecprocessGettext(executable, makeCommandline(source_file.getCanonicalPath(), dest_files[0].getCanonicalPath(), dest_files[1].getCanonicalPath()));
		} else {
			return new ExecprocessGettext(executable, makeCommandline(source_file.getCanonicalPath(), dest_files[0].getCanonicalPath(), null));
		}
	}
	
	public OutputFormat getOutputformat() {
		return outputformat;
	}
	
	public class OutputFormat implements Log2Dumpable {
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
		
		public Log2Dump getLog2Dump() {
			Log2Dump dump = new Log2Dump();
			if (width > 0 | height > 0) {
				dump.add("resolution", width + "x" + height);
			}
			dump.add("faststarted", faststarted);
			return dump;
		}
	}
	
	/**
	 * @param context with transcodecategory and transcodename keys
	 * @return null, or valid Tprofile
	 */
	/*public static TranscodeProfile getTranscodeProfile(JsonObject context) {
		if (context.has("transcodecategory") == false) {
			return null;
		}
		if (context.has("transcodename") == false) {
			return null;
		}
		return getTranscodeProfile(context.get("transcodecategory").getAsString(), context.get("transcodename").getAsString());
	}
	
	public final JsonObject toJson() {
		JsonObject jo = new JsonObject();
		jo.addProperty("transcodecategory", category);
		jo.addProperty("transcodename", name);
		return jo;
	}
	
	public static JsonObject transcodeProfiletoJsonContext(String category, String name) {
		JsonObject jo = new JsonObject();
		jo.addProperty("transcodecategory", category);
		jo.addProperty("transcodename", name);
		return jo;
	}*/
	
	/**
	 * @param context with transcodecategory and transcodename keys
	 * @return null, or valid Tprofile
	 */
	public static TranscodeProfile getTranscodeProfile(String category, String name) {
		if (isConfigured() == false) {
			return null;
		}
		if (category == null) {
			throw new NullPointerException("\"category\" can't to be null");
		}
		if (name == null) {
			throw new NullPointerException("\"name\" can't to be null");
		}
		
		for (int pos_pr = 0; pos_pr < profiles.size(); pos_pr++) {
			if (profiles.get(pos_pr).category.equalsIgnoreCase(category)) {
				if (profiles.get(pos_pr).name.equalsIgnoreCase(name)) {
					return profiles.get(pos_pr);
				}
			}
		}
		return null;
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump("profile", category + ":" + name);
		StringBuffer sb = new StringBuffer();
		ArrayList<String> cmd_line = makeCommandline("<input>", "<output>", null);
		for (int pos = 0; pos < cmd_line.size(); pos++) {
			sb.append(cmd_line.get(pos));
			sb.append(" ");
		}
		
		dump.add("commandline", sb.toString().trim());
		dump.add("extension", extension);
		if (outputformat != null) {
			dump.addAll(outputformat.getLog2Dump());
		}
		return dump;
	}
}
