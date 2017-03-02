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
import java.util.List;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.metadata.ContainerEntryResult;
import hd3gtv.mydmam.metadata.MetadataExtractor;
import hd3gtv.mydmam.metadata.PreviewType;
import hd3gtv.mydmam.metadata.RenderedFile;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.EntryRenderer;
import hd3gtv.mydmam.transcode.TranscodeProfile;
import hd3gtv.mydmam.transcode.TranscodeProfile.ProcessConfiguration;
import hd3gtv.mydmam.transcode.mtdcontainer.FFprobe;
import hd3gtv.mydmam.transcode.mtdcontainer.Stream;
import hd3gtv.tools.ExecprocessBadExecutionException;
import hd3gtv.tools.ExecprocessGettext;
import hd3gtv.tools.StoppableProcessing;

public class FFmpegAlbumartwork implements MetadataExtractor {
	
	private TranscodeProfile tprofile;
	
	public static final String ES_TYPE = "ffalbumartwork";
	
	public FFmpegAlbumartwork() {
		if (TranscodeProfile.isConfigured()) {
			tprofile = TranscodeProfile.getTranscodeProfile("ffmpeg_album_artwork");
		}
	}
	
	public boolean isEnabled() {
		return (tprofile != null);
	}
	
	public boolean canProcessThisMimeType(String mimetype) {
		return FFprobeAnalyser.canProcessThisAudioOnly(mimetype) | mimetype.equalsIgnoreCase("video/quicktime") | mimetype.equalsIgnoreCase("video/mp4");
	}
	
	public String getLongName() {
		return "FFmpeg album artwork extracting";
	}
	
	public ContainerEntryResult processFast(Container container) throws Exception {
		return null;
	}
	
	public ContainerEntryResult processFull(Container container, StoppableProcessing stoppable) throws Exception {
		FFprobe ffprobe = container.getByClass(FFprobe.class);
		if (ffprobe == null) {
			return null;
		}
		
		Stream artwork_stream = ffprobe.getAttachedPicStream();
		if (artwork_stream == null) {
			return null;
		}
		
		RenderedFile element = new RenderedFile("album_artwork", tprofile.getExtension("jpg"));
		
		ProcessConfiguration process_conf = tprofile.createProcessConfiguration(container.getPhysicalSource(), element.getTempFile());
		process_conf.getParamTags().put("PICSTREAMID", "0:" + artwork_stream.getIndex());
		
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
		
		EntryRenderer result = new EntryRenderer(FFmpegAlbumartwork.ES_TYPE);
		element.consolidateAndExportToEntry(result, container, this);
		return new ContainerEntryResult(result);
	}
	
	public PreviewType getPreviewTypeForRenderer(Container container, EntryRenderer entry) {
		return null;
	}
	
	public List<String> getMimeFileListCanUsedInMasterAsPreview() {
		return null;
	}
	
	public boolean isCanUsedInMasterAsPreview(Container container) {
		return false;
	}
	
}
