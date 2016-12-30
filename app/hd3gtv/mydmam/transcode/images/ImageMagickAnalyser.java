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
package hd3gtv.mydmam.transcode.images;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.metadata.ContainerEntryResult;
import hd3gtv.mydmam.metadata.MetadataExtractor;
import hd3gtv.mydmam.metadata.PreviewType;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.ContainerEntry;
import hd3gtv.mydmam.metadata.container.ContainerOperations;
import hd3gtv.mydmam.metadata.container.EntryRenderer;
import hd3gtv.tools.ExecBinaryPath;
import hd3gtv.tools.ExecprocessBadExecutionException;
import hd3gtv.tools.ExecprocessGettext;
import hd3gtv.tools.ExecprocessTooLongTimeExecutionException;
import hd3gtv.tools.StoppableProcessing;

public class ImageMagickAnalyser implements MetadataExtractor {
	
	static final ArrayList<String> mimetype_list;
	static final ArrayList<String> convert_limits_params;
	
	static int max_time_sec;
	
	static {
		mimetype_list = new ArrayList<String>();
		mimetype_list.add("image/jpeg");
		mimetype_list.add("image/png");
		mimetype_list.add("image/bmp");
		mimetype_list.add("image/gif");
		mimetype_list.add("image/vnd.adobe.photoshop");
		mimetype_list.add("image/tiff");
		mimetype_list.add("image/svg+xml");
		mimetype_list.add("application/postscript");
		mimetype_list.add("image/jp2");
		mimetype_list.add("application/dicom");
		mimetype_list.add("image/x-icon");
		mimetype_list.add("image/pict");
		mimetype_list.add("image/vndwapwbmp");
		mimetype_list.add("image/x-pcx");
		mimetype_list.add("image/x-portable-bitmap");
		mimetype_list.add("image/x-xbm");
		mimetype_list.add("image/xpm");
		mimetype_list.add("image/cineon");
		mimetype_list.add("image/dpx");
		mimetype_list.add("image/tga");
		mimetype_list.add("image/exr");
		mimetype_list.add("image/vnd.radiance");
		mimetype_list.add("image/webp");
		mimetype_list.add("image/sgi");
		mimetype_list.add("image/x-palm-pixmap");
		mimetype_list.add("image/x-g3-fax");
		mimetype_list.add("image/jpcd");
		mimetype_list.add("image/x-sct");
		mimetype_list.add("image/jbig");
		mimetype_list.add("image/x-miff");
		mimetype_list.add("image/x-sun");
		
		max_time_sec = 300;
		
		convert_limits_params = new ArrayList<String>(4);
		if (Configuration.global.isElementExists("imagemagick_limits")) {
			String memory = Configuration.global.getValue("imagemagick_limits", "memory", null);
			if (memory != null) {
				convert_limits_params.add("-limit");
				convert_limits_params.add("memory");
				convert_limits_params.add(memory);
			}
			
			String disk = Configuration.global.getValue("imagemagick_limits", "disk", null);
			if (disk != null) {
				convert_limits_params.add("-limit");
				convert_limits_params.add("disk");
				convert_limits_params.add(disk);
			}
			
			String file = Configuration.global.getValue("imagemagick_limits", "file", null);
			if (file != null) {
				convert_limits_params.add("-limit");
				convert_limits_params.add("file");
				convert_limits_params.add(file);
			}
			
			String time = Configuration.global.getValue("imagemagick_limits", "time", null);
			if (time != null) {
				convert_limits_params.add("-limit");
				convert_limits_params.add("time");
				convert_limits_params.add(time);
				max_time_sec = Integer.parseInt(time);
			}
		}
	}
	
	public ContainerEntryResult processFast(Container container) throws Exception {
		StoppableProcessing stoppable = new StoppableProcessing() {
			long end_time = System.currentTimeMillis() + ((long) max_time_sec * 1000l);
			
			public boolean isWantToStopCurrentProcessing() {
				return System.currentTimeMillis() > end_time;
			}
		};
		return processFull(container, stoppable);
	}
	
	public ContainerEntryResult processFull(Container container, StoppableProcessing stoppable) throws Exception {
		
		ArrayList<String> param = new ArrayList<String>();
		ExecprocessGettext process = null;
		try {
			param.addAll(convert_limits_params);
			param.add(container.getPhysicalSource().getPath() + "[0]");
			param.add("json:-");
			
			process = new ExecprocessGettext(ExecBinaryPath.get("convert"), param);
			process.setEndlinewidthnewline(true);
			process.setMaxexectime(max_time_sec);
			process.start();
			
			JsonObject result = filter(process.getResultstdout().toString()).getAsJsonObject().get("image").getAsJsonObject();
			
			if (result.has("profiles")) {
				JsonObject jo_profiles = result.get("profiles").getAsJsonObject();
				if (jo_profiles.has("iptc")) {
					try {
						/**
						 * Import and inject IPTC
						 */
						param.clear();
						param.addAll(convert_limits_params);
						param.add(container.getPhysicalSource().getPath() + "[0]");
						param.add("iptctext:-");
						process = new ExecprocessGettext(ExecBinaryPath.get("convert"), param);
						process.setEndlinewidthnewline(true);
						process.setExitcodemusttobe0(false);
						process.setMaxexectime(max_time_sec);
						process.start();
						
						if (process.getRunprocess().getExitvalue() == 0) {
							if (result.has("properties")) {
								ImageAttributes.injectIPTCInProperties(process.getResultstdout().toString(), result.get("properties").getAsJsonObject());
							} else {
								JsonObject jo_properties = new JsonObject();
								result.add("properties", jo_properties);
								ImageAttributes.injectIPTCInProperties(process.getResultstdout().toString(), jo_properties);
							}
						}
					} catch (ExecprocessTooLongTimeExecutionException e) {
						Loggers.Transcode.error("Can't extract IPTC with convert from \"" + container.getPhysicalSource().getPath() + "\", " + e.getMessage());
					}
				}
				result.remove("profiles");
			}
			
			result.remove("artifacts");
			result.remove("name");
			
			ImageAttributes ia = ContainerOperations.getGson().fromJson(result, ImageAttributes.class);
			container.getSummary().putSummaryContent(ia, ia.createSummary());
			
			return new ContainerEntryResult(ia);
		} catch (IOException e) {
			if (e instanceof ExecprocessBadExecutionException) {
				Loggers.Transcode.error("Problem with convert, " + process + ", " + container);
			}
			throw e;
		}
	}
	
	public boolean canProcessThisMimeType(String mimetype) {
		return mimetype_list.contains(mimetype);
	}
	
	public boolean isEnabled() {
		try {
			ExecBinaryPath.get("convert");
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	public String getLongName() {
		return "ImageMagick Analyser";
	}
	
	public List<String> getMimeFileListCanUsedInMasterAsPreview() {
		return null;
	}
	
	public boolean isCanUsedInMasterAsPreview(Container container) {
		return false;
	}
	
	public List<Class<? extends ContainerEntry>> getAllRootEntryClasses() {
		return Arrays.asList(ImageAttributes.class);
	}
	
	public PreviewType getPreviewTypeForRenderer(Container container, EntryRenderer entry) {
		return null;
	}
	
	/**
	 * ======= Json parser with json error protection =========
	 */
	
	private static Method getLineNumber_Method;
	private static Method getColumnNumber_Method;
	
	static {
		try {
			getLineNumber_Method = JsonReader.class.getDeclaredMethod("getLineNumber");
			getLineNumber_Method.setAccessible(true);
			
			getColumnNumber_Method = JsonReader.class.getDeclaredMethod("getColumnNumber");
			getColumnNumber_Method.setAccessible(true);
		} catch (NoSuchMethodException e) {
			Loggers.Transcode.error("Can't acces and change accessiblities to JsonReader methods", e);
		}
	}
	
	private static int getLineNumber(JsonReader in) {
		try {
			return (int) getLineNumber_Method.invoke(in);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new IndexOutOfBoundsException("Can't invoke getLineNumber in JsonReader, " + e.getMessage());
		}
	}
	
	private static int getColumnNumber(JsonReader in) {
		try {
			return (int) getColumnNumber_Method.invoke(in);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new IndexOutOfBoundsException("Can't invoke getColumnNumber in JsonReader, " + e.getMessage());
		}
	}
	
	private static StringReader concatLines(ArrayList<String> json_lines) {
		StringBuilder sb = new StringBuilder();
		json_lines.forEach(l -> {
			sb.append(l);
			sb.append(MyDMAM.LINESEPARATOR);
		});
		return new StringReader(sb.toString());
	}
	
	private static JsonElement filter(String convert_json_result) throws Exception {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		ArrayList<String> json_lines = (ArrayList) IOUtils.readLines(new StringReader(convert_json_result));
		
		JsonReader jr = null;
		
		for (int i = 0; i < 100; i++) {
			/**
			 * Normally a while, but... I don't want to fall in an endless loop.
			 */
			try {
				jr = new JsonReader(concatLines(json_lines));
				jr.setLenient(true);
				return Streams.parse(jr);
			} catch (Exception e) {
				if (e.getCause().getMessage().toLowerCase().startsWith("expected ':'")) {
					foundAndDeleteLine(json_lines, getLineNumber(jr) - 2);
				} else {
					removeLine(json_lines, getLineNumber(jr) - 1, getColumnNumber(jr));
					
					if (Loggers.Transcode.isDebugEnabled()) {
						Loggers.Transcode.debug("Catch problem during json extraction: " + e.getMessage(), e);
					}
				}
			}
		}
		
		throw new Exception("Can't extract shity json " + convert_json_result);
	}
	
	private static void foundAndDeleteLine(ArrayList<String> json_lines, int from) {
		int to_delete_end = from;
		
		int spaces_in = json_lines.get(from).indexOf("\"");
		
		for (int pos = from + 1; pos < json_lines.size(); pos++) {
			if (spaces_in == json_lines.get(pos).indexOf("\"")) {
				to_delete_end = pos;
				break;
			}
		}
		
		if (Loggers.Transcode.isDebugEnabled()) {
			Loggers.Transcode.debug("Correct shity json: remove lines form " + (from + 1) + " to " + (to_delete_end + 1));
		}
		
		ArrayList<String> removed_lines = new ArrayList<>();
		for (int pos = 0; pos < to_delete_end - from; pos++) {
			removed_lines.add((from + pos + 1) + "\t" + json_lines.get(from));
			json_lines.remove(from);
		}
		
		if (Loggers.Transcode.isDebugEnabled()) {
			Loggers.Transcode.debug("Correct shity json: remove this lines " + removed_lines);
		}
	}
	
	private static void removeLine(ArrayList<String> json_lines, int line_pos, int colpos) {
		if (Loggers.Transcode.isDebugEnabled()) {
			Loggers.Transcode.debug("Correct shity json: remove: " + (line_pos + 1) + "/" + " c" + colpos + "\t" + json_lines.get(line_pos));
		}
		json_lines.remove(line_pos);
	}
	
}
