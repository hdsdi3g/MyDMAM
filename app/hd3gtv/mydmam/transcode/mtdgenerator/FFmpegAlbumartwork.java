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
import hd3gtv.mydmam.metadata.WorkerRenderer;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.Entry;
import hd3gtv.mydmam.metadata.container.EntryRenderer;
import hd3gtv.mydmam.metadata.container.SelfSerializing;
import hd3gtv.mydmam.taskqueue.Profile;
import hd3gtv.mydmam.transcode.TranscodeProfile;
import hd3gtv.tools.ExecprocessBadExecutionException;
import hd3gtv.tools.ExecprocessGettext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class FFmpegAlbumartwork implements GeneratorRenderer {
	
	private String ffmpeg_bin;
	private TranscodeProfile tprofile;
	
	public static class Albumartwork extends EntryRenderer {
		public String getES_Type() {
			return "ffalbumartwork";
		}
		
		protected Entry create() {
			return new Albumartwork();
		}
		
		protected List<Class<? extends SelfSerializing>> getSerializationDependencies() {
			return null;
		}
	}
	
	public FFmpegAlbumartwork() {
		ffmpeg_bin = Configuration.global.getValue("transcoding", "ffmpeg_bin", "ffmpeg");
		if (TranscodeProfile.isConfigured()) {
			tprofile = TranscodeProfile.getTranscodeProfile(new Profile("ffmpeg", "ffmpeg_album_artwork"));
		}
	}
	
	public boolean isEnabled() {
		return (new File(ffmpeg_bin)).exists() & (tprofile != null);
	}
	
	public boolean canProcessThis(String mimetype) {
		return FFprobeAnalyser.canProcessThisAudioOnly(mimetype);
	}
	
	public String getLongName() {
		return "FFmpeg album artwork extracting";
	}
	
	public EntryRenderer process(Container container) throws Exception {
		JSONObject analysed_result = null; // TODO container.getByClass()
		if (analysed_result == null) {
			return null;
		}
		
		/**
		 * Must not have real video stream.
		 */
		if (FFprobeAnalyser.hasVideo(analysed_result)) {
			return null;
		}
		
		/**
		 * Must have fake video stream : artwork
		 */
		boolean containt_artwork = false;
		if (analysed_result.containsKey("streams") == false) {
			return null;
		}
		JSONArray streams = (JSONArray) analysed_result.get("streams");
		for (int pos = 0; pos < streams.size(); pos++) {
			JSONObject stream = (JSONObject) streams.get(pos);
			String codec_type = (String) stream.get("codec_type");
			if (codec_type.equalsIgnoreCase("video")) {
				containt_artwork = true;
				break;
			}
		}
		if (containt_artwork == false) {
			return null;
		}
		
		ArrayList<RenderedFile> result = new ArrayList<RenderedFile>();
		RenderedFile element = new RenderedFile("album_artwork", tprofile.getExtension("jpg"));
		
		ExecprocessGettext process = tprofile.prepareExecprocessGettext(ffmpeg_bin, container.getOrigin().getPhysicalSource(), element.getTempFile());
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
		
		return element.consolidateAndExportToEntry(new Albumartwork(), container, this);
	}
	
	public PreviewType getPreviewTypeForRenderer(Container container, List<RenderedFile> rendered_elements) {
		return PreviewType.full_size_thumbnail;
	}
	
	public Profile getManagedProfile() {
		return new Profile(WorkerRenderer.PROFILE_CATEGORY, "ffalbumartwork");
	}
	
	public Class<? extends EntryRenderer> getRootEntryClass() {
		return Albumartwork.class;
	}
	
}
