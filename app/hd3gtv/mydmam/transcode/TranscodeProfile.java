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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.ConfigurationItem;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.manager.InstanceStatusItem;
import hd3gtv.mydmam.transcode.watchfolder.WatchFolderDB;
import hd3gtv.tools.ExecBinaryPath;
import hd3gtv.tools.Execprocess;
import hd3gtv.tools.ExecprocessGettext;

@SuppressWarnings("unchecked")
public class TranscodeProfile implements InstanceStatusItem {
	
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
	private String executable_name;
	
	public enum CurrentDirectoryMode {
		/**
		 * Don't CWD process
		 */
		none,
		/** CWD to the input file directory */
		input,
		/** CWD to the output file directory */
		output,
	}
	
	private CurrentDirectoryMode current_directory_mode = CurrentDirectoryMode.none;
	
	private String name;
	
	private static LinkedHashMap<String, TranscodeProfile> profiles;
	private static LinkedHashMap<String, Class<? extends ExecprocessTranscodeEvent>> executables_events;
	private static LinkedHashMap<String, Class<? extends TranscodeProgress>> executables_transcode_progress;
	
	static {
		executables_events = new LinkedHashMap<String, Class<? extends ExecprocessTranscodeEvent>>(1);
		executables_events.put("ffmpeg", FFmpegEvents.class);
		
		executables_transcode_progress = new LinkedHashMap<String, Class<? extends TranscodeProgress>>(1);
		executables_transcode_progress.put("ffmpeg", TranscodeProgressFFmpeg.class);
		
		try {
			profiles = new LinkedHashMap<String, TranscodeProfile>();
			
			if (isConfigured()) {
				HashMap<String, ConfigurationItem> tp_list = Configuration.global.getElement("transcodingprofiles");
				
				if (tp_list.isEmpty()) {
					Loggers.Transcode.error("\"transcodingprofiles\" configuration can't be empty");
				} else {
					
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
						
						if (Configuration.isElementKeyExists(tp_list, entry.getKey(), "current_directory_mode")) {
							profile.current_directory_mode = CurrentDirectoryMode.valueOf(Configuration.getValue(tp_list, entry.getKey(), "current_directory_mode", "none"));
						}
						
						profile.executable_name = Configuration.getValue(tp_list, entry.getKey(), "executable", null);
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
						
						Loggers.Transcode.debug("Declared transcoding profile:\t" + profile.toString());
					}
				}
			}
		} catch (Exception e) {
			Loggers.Transcode.error("Can't load transcoding configuration", e);
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
			Loggers.Transcode.error("TranscodeProfile is not configured");
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
		
		sb.append(executable.getPath());
		sb.append(" ");
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
		
		if (current_directory_mode != CurrentDirectoryMode.none) {
			sb.append(", current_directory_mode: ");
			sb.append(current_directory_mode);
		}
		
		sb.append("\t[");
		sb.append(extension);
		sb.append("]");
		
		if (outputformat != null) {
			sb.append("\t");
			sb.append(outputformat);
		}
		
		return sb.toString().trim();
	}
	
	public OutputFormat getOutputformat() {
		return outputformat;
	}
	
	public class OutputFormat {
		private int width = -1;
		private int height = -1;
		private boolean faststarted = false;
		
		private OutputFormat(LinkedHashMap<String, ?> configuration_item) {
			if (configuration_item == null) {
				return;
			}
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
		
		public String toString() {
			return toJson().toString();
		}
		
		JsonObject toJson() {
			JsonObject jo = new JsonObject();
			if (width > -1) {
				jo.addProperty("width", width);
			}
			if (height > -1) {
				jo.addProperty("height", height);
			}
			jo.addProperty("faststarted", faststarted);
			return jo;
		}
	}
	
	public class ProcessConfiguration {
		private File input_file;
		private File output_file;
		private ArrayList<String> initial_params;
		
		private HashMap<String, String> param_tags;
		private ExecprocessTranscodeEvent event;
		private TranscodeProgress progress;
		
		private ProcessConfiguration(File input_file, File output_file) {
			this.input_file = input_file;
			this.output_file = output_file;
			param_tags = new HashMap<String, String>();
			initial_params = new ArrayList<String>();
		}
		
		public boolean wantAProgressFile() {
			for (int pos = 0; pos < params.size(); pos++) {
				if (params.get(pos).equals(TAG_PROGRESSFILE)) {
					return true;
				}
			}
			return false;
		}
		
		public HashMap<String, String> getParamTags() {
			return param_tags;
		}
		
		public ArrayList<String> getInitialParams() {
			return initial_params;
		}
		
		/**
		 * Event will be ready after prepareExecprocess() call.
		 */
		public ExecprocessTranscodeEvent getEvent() {
			return event;
		}
		
		public Execprocess prepareExecprocess(String job_ref) throws IOException {
			Execprocess process;
			if (executables_events.containsKey(executable_name)) {
				try {
					event = executables_events.get(executable_name).newInstance();
					event.setJobRef(job_ref);
					process = new Execprocess(executable, makeCommandline(), event);
				} catch (Exception e) {
					Loggers.Transcode.error("Can't load ExecprocessEvent new instance with executable_name: " + executable_name, e);
					process = new Execprocess(executable, makeCommandline(), null);
				}
			} else {
				event = new GenericEvents(executable_name);
				event.setJobRef(job_ref);
				process = new Execprocess(executable, makeCommandline(), event);
			}
			
			if (current_directory_mode == CurrentDirectoryMode.input) {
				process.setWorkingDirectory(input_file.getParentFile());
			} else if (current_directory_mode == CurrentDirectoryMode.output) {
				process.setWorkingDirectory(output_file.getParentFile());
			}
			return process;
		}
		
		public ExecprocessGettext prepareExecprocess() throws IOException {
			ExecprocessGettext process = new ExecprocessGettext(executable, makeCommandline());
			
			if (current_directory_mode == CurrentDirectoryMode.input) {
				process.setWorkingDirectory(input_file.getParentFile());
			} else if (current_directory_mode == CurrentDirectoryMode.output) {
				process.setWorkingDirectory(output_file.getParentFile());
			}
			return process;
		}
		
		public TranscodeProgress getProgress() {
			if (executables_transcode_progress.containsKey(executable_name)) {
				try {
					progress = executables_transcode_progress.get(executable_name).newInstance();
					return progress;
				} catch (Exception e) {
					Loggers.Transcode.error("Can't load TranscodeProgress new instance with executable_name: " + executable_name, e);
					return null;
				}
			}
			throw new NullPointerException("Can't found a TranscodeProgress for executable_name: " + executable_name);
		}
		
		private ArrayList<String> makeCommandline() throws IOException {
			ArrayList<String> cmdline = new ArrayList<String>();
			
			if (initial_params != null) {
				cmdline.addAll(initial_params);
			}
			
			String input_file_path = input_file.getCanonicalPath();
			String output_file_path = output_file.getCanonicalPath();
			
			if (current_directory_mode == CurrentDirectoryMode.input) {
				input_file_path = input_file.getName();
			} else if (current_directory_mode == CurrentDirectoryMode.output) {
				output_file_path = output_file.getName();
			}
			
			String param;
			
			for (int pos = 0; pos < params.size(); pos++) {
				param = params.get(pos);
				if (param.contains(TAG_INPUTFILE)) {
					if (input_file.getCanonicalPath() == null) {
						cmdline.add(param.replace(TAG_INPUTFILE, "-"));
					} else {
						cmdline.add(param.replace(TAG_INPUTFILE, input_file_path));
					}
				} else if (param.equals(TAG_OUTPUTFILE)) {
					cmdline.add(output_file_path);
				} else if (param.equals(TAG_PROGRESSFILE)) {
					if (progress != null) {
						cmdline.add(progress.getProgressfile().getCanonicalPath());
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
		
		public String toString() {
			LinkedHashMap<String, Object> log = new LinkedHashMap<String, Object>();
			try {
				log.put("commandline", makeCommandline());
			} catch (IOException e) {
				log.put("cant make commandline", e.getMessage());
			}
			log.put("input_file", input_file);
			log.put("output_file", output_file);
			log.put("progress", progress);
			log.put("param_tags", param_tags);
			if (progress != null) {
				log.put("progress", progress.getClass());
			}
			if (event != null) {
				log.put("event", event.getClass());
			}
			
			if (current_directory_mode != CurrentDirectoryMode.none) {
				log.put("current_directory_mode", current_directory_mode);
				if (current_directory_mode == CurrentDirectoryMode.input) {
					log.put("cwd", input_file.getParentFile());
				} else if (current_directory_mode == CurrentDirectoryMode.output) {
					log.put("cwd", output_file.getParentFile());
				}
			}
			return log.toString();
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
	
	public static class Serializer implements JsonSerializer<TranscodeProfile> {
		
		public JsonElement serialize(TranscodeProfile src, Type typeOfSrc, JsonSerializationContext context) {
			if (src == null) {
				return null;
			}
			JsonObject element = new JsonObject();
			element.addProperty("executable_name", src.executable_name);
			element.addProperty("executable", src.executable.getAbsolutePath());
			element.addProperty("params", StringUtils.join(src.params, " "));
			element.addProperty("extension", src.extension);
			if (src.outputformat != null) {
				element.add("outputformat", src.outputformat.toJson());
			} else {
				element.add("outputformat", JsonNull.INSTANCE);
			}
			element.addProperty("current_directory_mode", src.current_directory_mode.name());
			return element;
		}
	}
	
	public JsonElement getInstanceStatusItem() {
		return WatchFolderDB.gson.toJsonTree(this);
	}
	
	public String getReferenceKey() {
		return name;
	}
	
	public Class<?> getInstanceStatusItemReferenceClass() {
		return TranscodeProfile.class;
	}
	
}
