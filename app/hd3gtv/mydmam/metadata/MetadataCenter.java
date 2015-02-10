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

import hd3gtv.configuration.Configuration;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.manager.InstanceStatus;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.EntryAnalyser;
import hd3gtv.mydmam.metadata.container.EntryRenderer;
import hd3gtv.mydmam.metadata.container.EntrySummary;
import hd3gtv.mydmam.metadata.container.Operations;
import hd3gtv.mydmam.metadata.container.Origin;
import hd3gtv.mydmam.module.MyDMAMModulesManager;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.transcode.images.ImageMagickAnalyser;
import hd3gtv.mydmam.transcode.images.ImageMagickFFmpegThumbnailer;
import hd3gtv.mydmam.transcode.images.ImageMagickThumbnailer;
import hd3gtv.mydmam.transcode.images.ImageMagickThumbnailer.Cartridge;
import hd3gtv.mydmam.transcode.images.ImageMagickThumbnailer.FullDisplay;
import hd3gtv.mydmam.transcode.images.ImageMagickThumbnailer.Icon;
import hd3gtv.mydmam.transcode.mtdgenerator.FFmpegAlbumartwork;
import hd3gtv.mydmam.transcode.mtdgenerator.FFmpegInterlacingDetection;
import hd3gtv.mydmam.transcode.mtdgenerator.FFmpegLowresRenderer;
import hd3gtv.mydmam.transcode.mtdgenerator.FFmpegSnapshoot;
import hd3gtv.mydmam.transcode.mtdgenerator.FFprobeAnalyser;
import hd3gtv.mydmam.transcode.mtdgenerator.JobContextFFmpegLowresRendererAudio;
import hd3gtv.mydmam.transcode.mtdgenerator.JobContextFFmpegLowresRendererHD;
import hd3gtv.mydmam.transcode.mtdgenerator.JobContextFFmpegLowresRendererLQ;
import hd3gtv.mydmam.transcode.mtdgenerator.JobContextFFmpegLowresRendererSD;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetadataCenter {
	
	private static List<GeneratorAnalyser> generatorAnalysers;
	private static List<GeneratorRenderer> generatorRenderers;
	private static Map<String, GeneratorAnalyser> master_as_preview_mime_list_providers;
	
	static {
		generatorAnalysers = new ArrayList<GeneratorAnalyser>();
		generatorRenderers = new ArrayList<GeneratorRenderer>();
		
		master_as_preview_mime_list_providers = null;
		if (Configuration.global.isElementExists("master_as_preview")) {
			if (Configuration.global.getValueBoolean("master_as_preview", "enable")) {
				master_as_preview_mime_list_providers = new HashMap<String, GeneratorAnalyser>();
			}
		}
		
		try {
			addProvider(new ImageMagickAnalyser());
			addProvider(new FFprobeAnalyser());
			addProvider(new FFmpegInterlacingDetection());
			addProvider(new FFmpegSnapshoot());
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
		
		List<Generator> all_external_providers = MyDMAMModulesManager.getAllExternalMetadataGenerator();
		for (int pos = 0; pos < all_external_providers.size(); pos++) {
			addProvider(all_external_providers.get(pos));
		}
	}
	
	public static void doNothing() {
	}
	
	private static void addProvider(Generator provider) {
		if (provider == null) {
			return;
		}
		if (provider.isEnabled() == false) {
			Log2.log.info("Provider " + provider.getLongName() + " is disabled");
			return;
		}
		try {
			Operations.declareEntryType(provider.getRootEntryClass());
		} catch (Exception e) {
			Log2.log.error("Can't declare (de)serializer from Entry provider " + provider.getLongName(), e);
			return;
		}
		
		GeneratorAnalyser generatorAnalyser;
		if (provider instanceof GeneratorAnalyser) {
			generatorAnalyser = (GeneratorAnalyser) provider;
			generatorAnalysers.add(generatorAnalyser);
			if (master_as_preview_mime_list_providers != null) {
				List<String> list = generatorAnalyser.getMimeFileListCanUsedInMasterAsPreview();
				if (list != null) {
					for (int pos = 0; pos < list.size(); pos++) {
						master_as_preview_mime_list_providers.put(list.get(pos).toLowerCase(), generatorAnalyser);
					}
				}
			}
		} else if (provider instanceof GeneratorRenderer) {
			generatorRenderers.add((GeneratorRenderer) provider);
		} else {
			Log2.log.error("Can't add unrecognized provider", null);
		}
	}
	
	private MetadataCenter() {
	}
	
	public static List<GeneratorRenderer> getRenderers() {
		return generatorRenderers;
	}
	
	/**
	 * Database independant
	 * @param current_create_job_list set null for not process rendering
	 */
	public static Container standaloneIndexing(File physical_source, SourcePathIndexerElement reference, List<FutureCreateJobs> current_create_job_list) throws Exception {
		Origin origin = Origin.fromSource(reference, physical_source);
		Container container = new Container(origin.getUniqueElementKey(), origin);
		EntrySummary entry_summary = new EntrySummary();
		container.addEntry(entry_summary);
		
		if (physical_source.length() == 0) {
			entry_summary.setMimetype("application/null");
		} else {
			entry_summary.setMimetype(MimeExtract.getMime(physical_source));
		}
		
		for (int pos = 0; pos < generatorAnalysers.size(); pos++) {
			GeneratorAnalyser generatorAnalyser = generatorAnalysers.get(pos);
			if (generatorAnalyser.canProcessThis(entry_summary.getMimetype())) {
				try {
					EntryAnalyser entry_analyser = generatorAnalyser.process(container);
					if (entry_analyser == null) {
						continue;
					}
					container.addEntry(entry_analyser);
				} catch (Exception e) {
					Log2Dump dump = new Log2Dump();
					dump.add("analyser class", generatorAnalyser);
					dump.add("analyser name", generatorAnalyser.getLongName());
					dump.add("physical_source", physical_source);
					Log2.log.error("Can't analyst/render file", e, dump);
				}
			}
		}
		
		if (master_as_preview_mime_list_providers != null) {
			String mime = container.getSummary().getMimetype().toLowerCase();
			if (master_as_preview_mime_list_providers.containsKey(mime)) {
				entry_summary.master_as_preview = master_as_preview_mime_list_providers.get(mime).isCanUsedInMasterAsPreview(container);
			}
		}
		
		if (current_create_job_list == null) {
			return container;
		}
		
		for (int pos = 0; pos < generatorRenderers.size(); pos++) {
			GeneratorRenderer generatorRenderer = generatorRenderers.get(pos);
			if (generatorRenderer.canProcessThis(entry_summary.getMimetype())) {
				try {
					EntryRenderer entry_renderer = generatorRenderer.process(container);
					if (generatorRenderer instanceof GeneratorRendererViaWorker) {
						GeneratorRendererViaWorker renderer_via_worker = (GeneratorRendererViaWorker) generatorRenderer;
						renderer_via_worker.prepareJobs(container, current_create_job_list);
					}
					if (entry_renderer == null) {
						continue;
					}
					
					container.getSummary().addPreviewsFromEntryRenderer(entry_renderer, container, generatorRenderer);
					container.addEntry(entry_renderer);
					RenderedFile.cleanCurrentTempDirectory();
				} catch (Exception e) {
					Log2Dump dump = new Log2Dump();
					dump.add("provider class", generatorRenderer);
					dump.add("provider name", generatorRenderer.getLongName());
					dump.add("physical_source", physical_source);
					Log2.log.error("Can't analyst/render file", e, dump);
				}
			}
		}
		
		return container;
	}
	
	/**
	 * Database independant, and don't create any rendering files.
	 */
	public static Container standaloneAnalysis(File physical_source) throws Exception {
		if (physical_source.isFile() == false) {
			throw new IOException(physical_source.getPath() + " is not a file");
		}
		
		SourcePathIndexerElement reference = new SourcePathIndexerElement();
		reference.size = physical_source.length();
		reference.currentpath = physical_source.getCanonicalPath();
		reference.date = physical_source.lastModified();
		reference.directory = false;
		reference.storagename = "standalone-" + InstanceStatus.getThisInstanceNamePid();
		reference.parentpath = physical_source.getParentFile().getAbsolutePath();
		
		return standaloneIndexing(physical_source, reference, null);
	}
	
}