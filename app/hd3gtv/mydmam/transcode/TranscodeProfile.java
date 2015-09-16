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

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.ConfigurationItem;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;
import hd3gtv.tools.ExecBinaryPath;
import hd3gtv.tools.Execprocess;
import hd3gtv.tools.ExecprocessEvent;
import hd3gtv.tools.ExecprocessGettext;

@SuppressWarnings("unchecked")
public class TranscodeProfile implements Log2Dumpable {
	
	private static final String TAG_PROGRESSFILE = "<%$PROGRESSFILE%>";
	private static final String TAG_INPUTFILE = "<%$INPUTFILE%>";
	private static final String TAG_OUTPUTFILE = "<%$OUTPUTFILE%>";
	private static final String TAG_STARTVAR = "<%$VAR=";
	private static final String TAG_ENDVAR = "%>";
	private static final String TAG_STARTPARAM = "<%$";
	private static final String TAG_ENDPARAM = "%>";
	
	private ArrayList<String> params;
	private String extension;
	private OutputFormat outputformat;
	private File executable;
	
	private String name;
	
	private static LinkedHashMap<String, TranscodeProfile> profiles;
	
	static {
		try {
			profiles = new LinkedHashMap<String, TranscodeProfile>();
			if (isConfigured()) {
				HashMap<String, ConfigurationItem> tp_list = Configuration.global.getElement("transcodingprofiles");
				
				if (tp_list.isEmpty()) {
					Log2.log.error("Can't found \"profile\" element in transcoding block in XML configuration", null);
				}
				
				String profile_name;
				String profile_extension;
				String param;
				for (Map.Entry<String, ConfigurationItem> entry : tp_list.entrySet()) {
					profile_name = entry.getKey();
					
					TranscodeProfile profile = new TranscodeProfile(profile_name);
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
						profile.params.add(param);
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
					
					if (Configuration.isElementKeyExists(tp_list, entry.getKey(), "executable") == false) {
						throw new NullPointerException("Missing executable name for transcoding/" + entry.getKey());
					}
					profile.executable = ExecBinaryPath.get(Configuration.getValue(tp_list, entry.getKey(), "executable", null));
					
					if (profile.name == null) {
						throw new NullPointerException("\"profile_name\" can't to be null : check configuration.");
					}
					
					boolean founded = false;
					for (int pos = 0; pos < profile.params.size(); pos++) {
						if (profile.params.get(pos).contains(TAG_OUTPUTFILE)) {
							founded = true;
							break;
						}
					}
					if (founded == false) {
						throw new NullPointerException("No " + TAG_OUTPUTFILE + " in command check configuration.");
					}
					
					profiles.put(profile_name, profile);
				}
				
				Log2Dump dump = new Log2Dump();
				for (Map.Entry<String, TranscodeProfile> entry : profiles.entrySet()) {
					dump.addAll(entry.getValue());
				}
				
				Log2.log.debug("Set transcoding configuration", dump);
			}
		} catch (Exception e) {
			Log2.log.error("Can't load transcoding configuration", e);
		}
	}
	
	private TranscodeProfile(String name) {
		this.name = name;
		if (name == null) {
			throw new NullPointerException("\"name\" can't to be null");
		}
		params = new ArrayList<String>();
	}
	
	public static boolean isConfigured() {
		return Configuration.global.isElementExists("transcodingprofiles");
	}
	
	/**
	 * @param context with transcodecategory and transcodename keys
	 * @return null, or valid Tprofile
	 */
	public static TranscodeProfile getTranscodeProfile(String name) {
		if (isConfigured() == false) {
			Log2.log.error("TranscodeProfile is not configured", new NullPointerException());
			return null;
		}
		if (name == null) {
			throw new NullPointerException("\"name\" can't to be null");
		}
		if (profiles.containsKey(name)) {
			return profiles.get(name);
		}
		return null;
	}
	
	public static List<TranscodeProfile> getAllTranscodeProfiles() {
		ArrayList<TranscodeProfile> result = new ArrayList<TranscodeProfile>(profiles.size());
		for (Map.Entry<String, TranscodeProfile> entry : profiles.entrySet()) {
			result.add(entry.getValue());
		}
		return result;
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
	
	public final String getName() {
		return name;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(getName());
		sb.append("\t");
		
		for (int pos = 0; pos < params.size(); pos++) {
			if (params.get(pos).split(" ").length > 1) {
				sb.append("\"");
				sb.append(params.get(pos));
				sb.append("\"");
			} else {
				sb.append(params.get(pos));
			}
			sb.append(" ");
		}
		
		return sb.toString().trim();
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump("profile", name);
		StringBuffer sb = new StringBuffer();
		for (int pos = 0; pos < params.size(); pos++) {
			sb.append(params.get(pos));
			sb.append(" ");
		}
		
		dump.add("commandline", sb.toString().trim());
		dump.add("extension", extension);
		if (outputformat != null) {
			dump.addAll(outputformat.getLog2Dump());
		}
		return dump;
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
			if (faststarted) {
				dump.add("faststarted", faststarted);
			}
			return dump;
		}
	}
	
	public class ProcessConfiguration implements Log2Dumpable {
		private File input_file;
		private File output_file;
		private ArrayList<String> initial_params;
		
		private File progress_file;
		private HashMap<String, String> param_tags;
		
		private ProcessConfiguration(File input_file, File output_file) {
			this.input_file = input_file;
			this.output_file = output_file;
			param_tags = new HashMap<String, String>();
			initial_params = new ArrayList<String>();
		}
		
		public ProcessConfiguration setProgressFile(File progress_file) {
			this.progress_file = progress_file;
			return this;
		}
		
		public HashMap<String, String> getParamTags() {
			return param_tags;
		}
		
		public ArrayList<String> getInitialParams() {
			return initial_params;
		}
		
		public Execprocess prepareExecprocess(ExecprocessEvent events) throws IOException {
			return new Execprocess(executable, makeCommandline(), events);
		}
		
		public ExecprocessGettext prepareExecprocess() throws IOException {
			return new ExecprocessGettext(executable, makeCommandline());
		}
		
		private ArrayList<String> makeCommandline() throws IOException {
			ArrayList<String> cmdline = new ArrayList<String>();
			
			if (initial_params != null) {
				cmdline.addAll(initial_params);
			}
			
			String param;
			
			for (int pos = 0; pos < params.size(); pos++) {
				param = params.get(pos);
				if (param.contains(TAG_INPUTFILE)) {
					if (input_file.getCanonicalPath() == null) {
						cmdline.add(param.replace(TAG_INPUTFILE, "-"));
					} else {
						cmdline.add(param.replace(TAG_INPUTFILE, input_file.getCanonicalPath()));
					}
				} else if (param.equals(TAG_OUTPUTFILE)) {
					cmdline.add(output_file.getCanonicalPath());
				} else if (param.equals(TAG_PROGRESSFILE)) {
					if (progress_file != null) {
						cmdline.add(progress_file.getCanonicalPath());
					} else {
						cmdline.add(System.getProperty("java.io.tmpdir") + File.separator + "progress.txt");
					}
				} else if (param.startsWith(TAG_STARTPARAM) & param.endsWith(TAG_ENDPARAM)) {
					param = param.substring(TAG_STARTPARAM.length(), param.length() - TAG_ENDPARAM.length());
					if (param_tags.containsKey(param)) {
						cmdline.add(param_tags.get(param));
					}
				} else {
					cmdline.add(param);
				}
			}
			
			return cmdline;
		}
		
		public Log2Dump getLog2Dump() {
			Log2Dump dump = new Log2Dump("profile", name);
			try {
				StringBuffer sb = new StringBuffer();
				ArrayList<String> cmd = makeCommandline();
				for (int pos = 0; pos < cmd.size(); pos++) {
					sb.append(cmd.get(pos));
					sb.append(" ");
				}
				dump.add("commandline", sb.toString().trim());
			} catch (IOException e) {
				dump.add("commandline", e);
			}
			dump.add("extension", extension);
			if (outputformat != null) {
				dump.addAll(outputformat.getLog2Dump());
			}
			return dump;
		}
	}
	
	public ProcessConfiguration createProcessConfiguration(File input_file, File output_file) {
		if (input_file == null) {
			throw new NullPointerException("\"input_file\" can't to be null");
		}
		if (output_file == null) {
			throw new NullPointerException("\"output_file\" can't to be null");
		}
		return new ProcessConfiguration(input_file, output_file);
	}
	
	public File getExecutable() {
		return executable;
	}
	
}
