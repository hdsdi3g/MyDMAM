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
package hd3gtv.mydmam.transcode;

import hd3gtv.configuration.Configuration;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.analysis.Analyser;
import hd3gtv.mydmam.analysis.MetadataIndexerResult;
import hd3gtv.mydmam.analysis.PreviewType;
import hd3gtv.mydmam.analysis.RenderedElement;
import hd3gtv.mydmam.analysis.Renderer;
import hd3gtv.mydmam.taskqueue.Profile;
import hd3gtv.tools.ExecprocessBadExecutionException;
import hd3gtv.tools.ExecprocessGettext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;

public class FFmpegSnapshoot implements Renderer {
	
	private String ffmpeg_bin;
	
	public FFmpegSnapshoot() {
		ffmpeg_bin = Configuration.global.getValue("transcoding", "ffmpeg_bin", "ffmpeg");
	}
	
	public boolean isEnabled() {
		return (new File(ffmpeg_bin)).exists() & TranscodeProfileManager.isEnabled();
	}
	
	public boolean canProcessThis(String mimetype) {
		return FFprobeAnalyser.canProcessThisVideoOnly(mimetype);
	}
	
	public String getName() {
		return "FFmpeg Snapshoot";
	}
	
	public List<RenderedElement> process(MetadataIndexerResult analysis_result) throws Exception {
		/**
		 * There are video streams in this file ?
		 */
		boolean found = false;
		for (Map.Entry<Analyser, JSONObject> entry : analysis_result.getAnalysis_results().entrySet()) {
			if (entry.getKey() instanceof FFprobeAnalyser) {
				if (FFprobeAnalyser.hasVideo(entry.getValue())) {
					found = true;
				}
				break;
			}
		}
		if (found == false) {
			return null;
		}
		
		ArrayList<RenderedElement> result = new ArrayList<RenderedElement>();
		RenderedElement element = new RenderedElement("snap", ".png");
		
		TranscodeProfile tprofile = TranscodeProfileManager.getProfile(new Profile("ffmpeg", "ffmpeg_snapshoot_first"));
		ArrayList<String> param = tprofile.makeCommandline(analysis_result.getOrigin().getAbsolutePath(), element.getTempFile().getAbsolutePath());
		
		ExecprocessGettext process = new ExecprocessGettext(ffmpeg_bin, param);
		process.setEndlinewidthnewline(true);
		try {
			process.start();
		} catch (IOException e) {
			if (e instanceof ExecprocessBadExecutionException) {
				Log2Dump dump = new Log2Dump();
				dump.add("file", analysis_result.getOrigin());
				dump.add("mime", analysis_result.getMimetype());
				if (process.getRunprocess().getExitvalue() == 1) {
					dump.add("stderr", process.getResultstderr().toString().trim());
					Log2.log.error("Invalid data found when processing input", null, dump);
				} else {
					dump.add("stdout", process.getResultstdout().toString().trim());
					dump.add("stderr", process.getResultstderr().toString().trim());
					dump.add("exitcode", process.getRunprocess().getExitvalue());
					Log2.log.error("Problem with ffprobe", null, dump);
				}
			}
			throw e;
		}
		
		result.add(element);
		
		return result;
	}
	
	public String getElasticSearchIndexType() {
		return "ffsnapshoot";
	}
	
	public PreviewType getPreviewTypeForRenderer(JSONObject mtd_summary, List<RenderedElement> rendered_elements) {
		if (rendered_elements == null) {
			return null;
		}
		if (rendered_elements.isEmpty()) {
			return null;
		}
		return PreviewType.full_size_thumbnail;
	}
	
	public JSONObject getPreviewConfigurationForRenderer(PreviewType preview_type, JSONObject mtd_summary, List<RenderedElement> rendered_elements) {
		if (preview_type == null) {
			return null;
		}
		return null;// getSnapConfiguration(analysis_result).get(0).toDatabase();
	}
	
}