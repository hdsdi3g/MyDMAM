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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.mydmam.transcode.mtdgenerator;

import hd3gtv.configuration.Configuration;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.metadata.GeneratorRenderer;
import hd3gtv.mydmam.metadata.PreviewType;
import hd3gtv.mydmam.metadata.RenderedFile;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.EntryRenderer;
import hd3gtv.mydmam.metadata.container.SelfSerializing;
import hd3gtv.mydmam.transcode.TranscodeProfile;
import hd3gtv.mydmam.transcode.mtdcontainer.FFprobe;
import hd3gtv.tools.ExecprocessBadExecutionException;
import hd3gtv.tools.ExecprocessGettext;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FFmpegSnapshoot implements GeneratorRenderer {
	
	private String ffmpeg_bin;
	private TranscodeProfile tprofile;
	
	public static class Snapshoot extends EntryRenderer {
		public String getES_Type() {
			return "ffsnapshoot";
		}
		
		protected EntryRenderer create() {
			return new Snapshoot();
		}
		
		protected List<Class<? extends SelfSerializing>> getSerializationDependencies() {
			return null;
		}
		// TODO add the mgr to 3 types of thumbnail
	}
	
	public FFmpegSnapshoot() {
		ffmpeg_bin = Configuration.global.getValue("transcoding", "ffmpeg_bin", "ffmpeg");
		if (TranscodeProfile.isConfigured()) {
			tprofile = TranscodeProfile.getTranscodeProfile("ffmpeg_snapshoot_first");
		}
	}
	
	public boolean isEnabled() {
		return (new File(ffmpeg_bin)).exists() & (tprofile != null);
	}
	
	public boolean canProcessThis(String mimetype) {
		return FFprobeAnalyser.canProcessThisVideoOnly(mimetype);
	}
	
	public String getLongName() {
		return "FFmpeg Snapshoot";
	}
	
	public EntryRenderer process(Container container) throws Exception {
		FFprobe ffprobe = container.getByClass(FFprobe.class);
		if (ffprobe == null) {
			return null;
		}
		if (ffprobe.hasVideo() == false) {
			return null;
		}
		
		RenderedFile element = new RenderedFile("snap", tprofile.getExtension("jpg"));
		
		ExecprocessGettext process = tprofile.createProcessConfiguration(ffmpeg_bin, container.getPhysicalSource(), element.getTempFile()).prepareExecprocess();
		process.setEndlinewidthnewline(true);
		try {
			process.start();
		} catch (IOException e) {
			if (e instanceof ExecprocessBadExecutionException) {
				Log2Dump dump = new Log2Dump();
				dump.addAll(container);
				if (process.getRunprocess().getExitvalue() == 1) {
					dump.add("stderr", process.getResultstderr().toString().trim());
					Log2.log.error("Invalid data found when processing input", null, dump);
				} else {
					dump.add("stdout", process.getResultstdout().toString().trim());
					dump.add("stderr", process.getResultstderr().toString().trim());
					dump.add("exitcode", process.getRunprocess().getExitvalue());
					Log2.log.error("Problem with ffmpeg", null, dump);
				}
			}
			throw e;
		}
		
		Snapshoot result = new Snapshoot();
		element.consolidateAndExportToEntry(result, container, this);
		// TODO add 3 types of thumbnail, with MetadataCenter.standaloneAnalysis(physical_source), and ImageMagickThumbnailer
		return result;
	}
	
	public PreviewType getPreviewTypeForRenderer(Container container, EntryRenderer entry) {
		// TODO extends to 3 types of thumbnail
		return PreviewType.full_size_thumbnail;
	}
	
	public Class<? extends EntryRenderer> getRootEntryClass() {
		return Snapshoot.class;
	}
	
}
