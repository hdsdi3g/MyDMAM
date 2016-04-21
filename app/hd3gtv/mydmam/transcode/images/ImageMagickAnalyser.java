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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
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
import hd3gtv.tools.StoppableProcessing;

public class ImageMagickAnalyser implements MetadataExtractor {
	
	static final ArrayList<String> mimetype_list;
	static final ArrayList<String> convert_limits_params;
	
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
			}
		}
	}
	
	public ContainerEntryResult processFast(Container container) throws Exception {
		return processFull(container, null);
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
			process.start();
			
			JsonParser p = new JsonParser();
			JsonObject result = p.parse(process.getResultstdout().toString()).getAsJsonObject();
			result = result.get("image").getAsJsonObject();
			
			if (result.has("profiles")) {
				JsonObject jo_profiles = result.get("profiles").getAsJsonObject();
				if (jo_profiles.has("iptc")) {
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
}
