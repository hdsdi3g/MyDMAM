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
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
*/
package hd3gtv.mydmam.metadata;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import hd3gtv.configuration.Configuration;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.metadata.MetadataIndexingOperation.MetadataIndexingLimit;
import hd3gtv.mydmam.metadata.container.ContainerOperations;
import hd3gtv.mydmam.transcode.images.ImageMagickAnalyser;
import hd3gtv.mydmam.transcode.images.ImageMagickFFmpegThumbnailer;
import hd3gtv.mydmam.transcode.images.ImageMagickThumbnailer;
import hd3gtv.mydmam.transcode.images.ImageMagickThumbnailer.Cartridge;
import hd3gtv.mydmam.transcode.images.ImageMagickThumbnailer.FullDisplay;
import hd3gtv.mydmam.transcode.images.ImageMagickThumbnailer.Icon;
import hd3gtv.mydmam.transcode.mtdgenerator.FFmpegAlbumartwork;
import hd3gtv.mydmam.transcode.mtdgenerator.FFmpegInterlacingDetection;
import hd3gtv.mydmam.transcode.mtdgenerator.FFmpegLowresRenderer;
import hd3gtv.mydmam.transcode.mtdgenerator.FFmpegSnapshot;
import hd3gtv.mydmam.transcode.mtdgenerator.FFprobeAnalyser;
import hd3gtv.mydmam.transcode.mtdgenerator.JobContextFFmpegLowresRendererAudio;
import hd3gtv.mydmam.transcode.mtdgenerator.JobContextFFmpegLowresRendererHD;
import hd3gtv.mydmam.transcode.mtdgenerator.JobContextFFmpegLowresRendererLQ;
import hd3gtv.mydmam.transcode.mtdgenerator.JobContextFFmpegLowresRendererSD;

public class MetadataCenter {
	
	private static ArrayList<MetadataGeneratorAnalyser> metadataGeneratorAnalysers;
	private static ArrayList<MetadataGeneratorRenderer> metadataGeneratorRenderers;
	private static Map<String, MetadataGeneratorAnalyser> master_as_preview_mime_list_providers;
	static File rendering_temp_directory;
	static File rendering_local_directory;
	
	static ArrayList<MetadataConfigurationItem> conf_items;
	
	public static class MetadataConfigurationItem {
		String storage_label_name;
		String currentpath;
		MetadataIndexingLimit limit = MetadataIndexingLimit.NOLIMITS;
		ArrayList<Class<? extends MetadataGenerator>> blacklist;
		
		@SuppressWarnings("unchecked")
		private MetadataConfigurationItem(LinkedHashMap<String, ?> conf) {
			if (conf.containsKey("to") == false) {
				throw new NullPointerException("No \"to\" storage:/path indication");
			}
			String apply_to = (String) conf.get("to");
			
			if (apply_to.indexOf(":") == -1) {
				throw new NullPointerException("Invalid \"to\" value. Format is: \"storage:/path\", or \"storage:/\"");
			}
			storage_label_name = apply_to.substring(0, apply_to.indexOf(":"));
			currentpath = apply_to.substring(apply_to.indexOf(":") + 1);
			if (storage_label_name.trim().isEmpty()) {
				throw new NullPointerException("Invalid storage name value for \"to\"");
			}
			if (currentpath.trim().isEmpty()) {
				throw new NullPointerException("Invalid current path value for \"to\"");
			}
			
			if (conf.containsKey("limit")) {
				try {
					limit = MetadataIndexingLimit.valueOf(((String) conf.get("limit")).toUpperCase());
				} catch (IllegalArgumentException e) {
					Log2.log.error("Bad enum value for limit", e, new Log2Dump("to", apply_to));
				}
			}
			
			blacklist = new ArrayList<Class<? extends MetadataGenerator>>();
			if (conf.containsKey("blacklist")) {
				ArrayList<String> str_blacklist = (ArrayList<String>) conf.get("blacklist");
				for (int pos_bl = 0; pos_bl < str_blacklist.size(); pos_bl++) {
					try {
						Class<?> c = Class.forName(str_blacklist.get(pos_bl));
						if (MetadataGenerator.class.isAssignableFrom(c)) {
							blacklist.add((Class<? extends MetadataGenerator>) c);
						} else {
							throw new ClassNotFoundException(c.getName() + " is not a MetadataGenerator");
						}
					} catch (ClassNotFoundException e) {
						Log2.log.error("Invalid/not found class for blacklist", e, new Log2Dump("to", apply_to));
					}
				}
			}
		}
		
		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append(storage_label_name);
			sb.append(":");
			sb.append(currentpath);
			sb.append(", only for ");
			sb.append(limit);
			if (blacklist.isEmpty()) {
				sb.append(", no blacklists ");
			} else {
				sb.append(", blacklisted to ");
				for (int pos = 0; pos < blacklist.size(); pos++) {
					sb.append(blacklist.get(pos));
					sb.append(" ");
				}
			}
			sb.append(";");
			return sb.toString();
		}
	}
	
	static {
		metadataGeneratorAnalysers = new ArrayList<MetadataGeneratorAnalyser>();
		metadataGeneratorRenderers = new ArrayList<MetadataGeneratorRenderer>();
		
		master_as_preview_mime_list_providers = null;
		conf_items = new ArrayList<MetadataCenter.MetadataConfigurationItem>();
		
		if (Configuration.global.isElementExists("metadata_analysing")) {
			List<LinkedHashMap<String, ?>> list_conf = Configuration.global.getListMapValues("metadata_analysing", "items");
			for (int pos_item = 0; pos_item < list_conf.size(); pos_item++) {
				conf_items.add(new MetadataConfigurationItem(list_conf.get(pos_item)));
			}
			
			if (Configuration.global.getValueBoolean("metadata_analysing", "master_as_preview")) {
				master_as_preview_mime_list_providers = new HashMap<String, MetadataGeneratorAnalyser>();
			}
			rendering_temp_directory = new File(Configuration.global.getValue("metadata_analysing", "temp_directory", System.getProperty("java.io.tmpdir", "/tmp")));
			rendering_local_directory = new File(Configuration.global.getValue("metadata_analysing", "local_directory", System.getProperty("user.home", "/tmp")));
		}
		
		try {
			addProvider(new ImageMagickAnalyser());
			
			addProvider(new FFprobeAnalyser());
			addProvider(new FFmpegInterlacingDetection());
			addProvider(new FFmpegSnapshot());
			addProvider(new FFmpegAlbumartwork());
			
			addProvider(new ImageMagickThumbnailer(FullDisplay.class, PreviewType.full_size_thumbnail, FullDisplay.profile_name));
			addProvider(new ImageMagickThumbnailer(Cartridge.class, PreviewType.cartridge_thumbnail, Cartridge.profile_name));
			addProvider(new ImageMagickThumbnailer(Icon.class, PreviewType.icon_thumbnail, Icon.profile_name));
			addProvider(new ImageMagickFFmpegThumbnailer(FullDisplay.class, PreviewType.full_size_thumbnail, FullDisplay.profile_name));
			addProvider(new ImageMagickFFmpegThumbnailer(Cartridge.class, PreviewType.cartridge_thumbnail, Cartridge.profile_name));
			addProvider(new ImageMagickFFmpegThumbnailer(Icon.class, PreviewType.icon_thumbnail, Icon.profile_name));
			
			addProvider(new FFmpegLowresRenderer(JobContextFFmpegLowresRendererLQ.class, PreviewType.video_lq_pvw, false));
			addProvider(new FFmpegLowresRenderer(JobContextFFmpegLowresRendererSD.class, PreviewType.video_sd_pvw, false));
			addProvider(new FFmpegLowresRenderer(JobContextFFmpegLowresRendererHD.class, PreviewType.video_hd_pvw, false));
			addProvider(new FFmpegLowresRenderer(JobContextFFmpegLowresRendererAudio.class, PreviewType.audio_pvw, true));
		} catch (Exception e) {
			Log2.log.error("Can't instanciate Providers", e);
		}
	}
	
	private static void addProvider(MetadataGenerator provider) {
		if (provider == null) {
			return;
		}
		if (provider.isEnabled() == false) {
			return;
		}
		
		Log2.log.info("Load provider " + provider.getLongName());
		try {
			ContainerOperations.declareEntryType(provider.getRootEntryClass());
		} catch (Exception e) {
			Log2.log.error("Can't declare (de)serializer from Entry provider " + provider.getLongName(), e);
			return;
		}
		
		MetadataGeneratorAnalyser metadataGeneratorAnalyser;
		if (provider instanceof MetadataGeneratorAnalyser) {
			metadataGeneratorAnalyser = (MetadataGeneratorAnalyser) provider;
			metadataGeneratorAnalysers.add(metadataGeneratorAnalyser);
			if (master_as_preview_mime_list_providers != null) {
				List<String> list = metadataGeneratorAnalyser.getMimeFileListCanUsedInMasterAsPreview();
				if (list != null) {
					for (int pos = 0; pos < list.size(); pos++) {
						master_as_preview_mime_list_providers.put(list.get(pos).toLowerCase(), metadataGeneratorAnalyser);
					}
				}
			}
		} else if (provider instanceof MetadataGeneratorRenderer) {
			metadataGeneratorRenderers.add((MetadataGeneratorRenderer) provider);
		} else {
			Log2.log.error("Can't add unrecognized provider", null);
		}
	}
	
	private MetadataCenter() {
	}
	
	public static ArrayList<MetadataGeneratorRenderer> getRenderers() {
		return metadataGeneratorRenderers;
	}
	
	public static ArrayList<MetadataGeneratorAnalyser> getAnalysers() {
		return metadataGeneratorAnalysers;
	}
	
	public static Map<String, MetadataGeneratorAnalyser> getMasterAsPreviewMimeListProviders() {
		return master_as_preview_mime_list_providers;
	}
	
}