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

import java.io.IOException;
import java.util.ArrayList;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.metadata.MetadataGeneratorRenderer;
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

public class FFmpegSnapshot implements MetadataGeneratorRenderer {
	
	private TranscodeProfile tprofile;
	
	public static class Snapshot extends EntryRenderer {
		public String getES_Type() {
			return "ffsnapshot";
		}
		
	}
	
	public FFmpegSnapshot() {
		if (TranscodeProfile.isConfigured()) {
			tprofile = TranscodeProfile.getTranscodeProfile("ffmpeg_snapshot_first");
		}
	}
	
	public boolean isEnabled() {
		return (tprofile != null);
	}
	
	public boolean canProcessThis(String mimetype) {
		return FFprobeAnalyser.canProcessThisVideoOnly(mimetype);
	}
	
	public String getLongName() {
		return "FFmpeg Snapshot";
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
		
		RenderedFile element = new RenderedFile("snap", tprofile.getExtension("png"));
		
		ProcessConfiguration process_conf = tprofile.createProcessConfiguration(container.getPhysicalSource(), element.getTempFile());
		
		ArrayList<String> filters = new ArrayList<String>();
		if (ffprobe.getDuration().getValue() > 10) {
			/**
			 * Duration is > 10 seconds: ask to ffmpeg to choose the best capture frame from the 100 first.
			 */
			filters.add("thumbnail");
		}
		
		if (interlacing != Interlacing.Progressive) {
			filters.add("yadif");
		}
		
		if (ffprobe.hasVerticalBlankIntervalInImage()) {
			/**
			 * Cut the 32 lines from the top.
			 */
			filters.add("crop=w=in_w:h=in_h-32:x=0:y=32");
		}
		filters.add("scale=iw*sar:ih");
		
		StringBuilder sb_filters = new StringBuilder();
		if (filters.isEmpty() == false) {
			for (int pos_flt = 0; pos_flt < filters.size(); pos_flt++) {
				sb_filters.append(filters.get(pos_flt));
				if (pos_flt + 1 < filters.size()) {
					sb_filters.append(",");
				}
			}
		} else {
			sb_filters.append("null");
		}
		
		process_conf.getParamTags().put("FILTERS", sb_filters.toString());
		
		ExecprocessGettext process = process_conf.prepareExecprocess();
		process.setEndlinewidthnewline(true);
		try {
			process.start();
		} catch (IOException e) {
			if (e instanceof ExecprocessBadExecutionException) {
				if (process.getRunprocess().getExitvalue() == 1) {
					Loggers.Transcode_Metadata.error("Invalid data found when processing input, " + process + ", " + container);
				} else {
					Loggers.Transcode_Metadata.error("Problem with ffmpeg, " + process + ", " + container);
				}
			}
			throw e;
		}
		
		Snapshot result = new Snapshot();
		element.consolidateAndExportToEntry(result, container, this);
		return result;
	}
	
	public PreviewType getPreviewTypeForRenderer(Container container, EntryRenderer entry) {
		return null;
	}
	
	public Class<? extends EntryRenderer> getRootEntryClass() {
		return Snapshot.class;
	}
	
}
