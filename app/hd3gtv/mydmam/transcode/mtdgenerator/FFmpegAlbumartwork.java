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

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.metadata.MetadataGeneratorRenderer;
import hd3gtv.mydmam.metadata.PreviewType;
import hd3gtv.mydmam.metadata.RenderedFile;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.EntryRenderer;
import hd3gtv.mydmam.transcode.TranscodeProfile;
import hd3gtv.mydmam.transcode.mtdcontainer.FFprobe;
import hd3gtv.tools.ExecprocessBadExecutionException;
import hd3gtv.tools.ExecprocessGettext;

public class FFmpegAlbumartwork implements MetadataGeneratorRenderer {
	
	private TranscodeProfile tprofile;
	
	public static class Albumartwork extends EntryRenderer {
		public String getES_Type() {
			return "ffalbumartwork";
		}
		
	}
	
	public FFmpegAlbumartwork() {
		if (TranscodeProfile.isConfigured()) {
			tprofile = TranscodeProfile.getTranscodeProfile("ffmpeg_album_artwork");
		}
	}
	
	public boolean isEnabled() {
		return (tprofile != null);
	}
	
	public boolean canProcessThis(String mimetype) {
		return FFprobeAnalyser.canProcessThisAudioOnly(mimetype);
	}
	
	public String getLongName() {
		return "FFmpeg album artwork extracting";
	}
	
	public EntryRenderer process(Container container) throws Exception {
		FFprobe ffprobe = container.getByClass(FFprobe.class);
		if (ffprobe == null) {
			return null;
		}
		
		/**
		 * Must not have real video stream.
		 */
		if (ffprobe.hasVideo()) {
			return null;
		}
		
		/**
		 * Must have fake video stream : artwork
		 */
		if (ffprobe.getStreamsByCodecType("video") == null) {
			return null;
		}
		
		RenderedFile element = new RenderedFile("album_artwork", tprofile.getExtension("jpg"));
		
		ExecprocessGettext process = tprofile.createProcessConfiguration(container.getPhysicalSource(), element.getTempFile()).prepareExecprocess();
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
		
		Albumartwork result = new Albumartwork();
		element.consolidateAndExportToEntry(result, container, this);
		return result;
	}
	
	public PreviewType getPreviewTypeForRenderer(Container container, EntryRenderer entry) {
		return null;
	}
	
	public Class<? extends EntryRenderer> getRootEntryClass() {
		return Albumartwork.class;
	}
	
}
