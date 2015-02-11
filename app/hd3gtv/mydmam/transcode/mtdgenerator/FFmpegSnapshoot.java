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
import hd3gtv.mydmam.transcode.TranscodeProfile;
import hd3gtv.mydmam.transcode.TranscodeProfile.ProcessConfiguration;
import hd3gtv.mydmam.transcode.mtdcontainer.FFmpegInterlacingStats;
import hd3gtv.mydmam.transcode.mtdcontainer.FFprobe;
import hd3gtv.tools.ExecprocessBadExecutionException;
import hd3gtv.tools.ExecprocessGettext;
import hd3gtv.tools.VideoConst.Interlacing;

import java.io.File;
import java.io.IOException;

public class FFmpegSnapshoot implements GeneratorRenderer {
	
	private String ffmpeg_bin;
	private TranscodeProfile tprofile;
	
	public static class Snapshoot extends EntryRenderer {
		public String getES_Type() {
			return "ffsnapshoot";
		}
		
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
		
		Interlacing interlacing = Interlacing.Progressive;
		FFmpegInterlacingStats interlace_stats = container.getByClass(FFmpegInterlacingStats.class);
		if (interlace_stats != null) {
			interlacing = interlace_stats.getInterlacing();
		}
		if (interlacing == Interlacing.Unknow) {
			interlacing = Interlacing.Progressive;
		}
		
		RenderedFile element = new RenderedFile("snap", tprofile.getExtension("jpg"));
		
		ProcessConfiguration process_conf = tprofile.createProcessConfiguration(ffmpeg_bin, container.getPhysicalSource(), element.getTempFile());
		ExecprocessGettext process = process_conf.prepareExecprocess();
		
		if (ffprobe.hasVerticalBlankIntervalInImage()) {
			
		}
		if (interlacing != Interlacing.Progressive) {
			
		}
		
		// TODO add some filters: -vf yadif,crop=w=in_w:h=in_h-32:x=0:y=32,scale=w=in_w*sar:h=in_h <%$FILTERS%>
		
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
		return result;
	}
	
	public PreviewType getPreviewTypeForRenderer(Container container, EntryRenderer entry) {
		return null;
	}
	
	public Class<? extends EntryRenderer> getRootEntryClass() {
		return Snapshoot.class;
	}
	
}
