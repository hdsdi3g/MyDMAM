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
package hd3gtv.mydmam.analysis;

import hd3gtv.configuration.Configuration;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.taskqueue.Profile;
import hd3gtv.mydmam.transcode.TranscodeProfile;
import hd3gtv.mydmam.transcode.TranscodeProfileManager;
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
	
	public List<RenderedElement> process(AnalysisResult analysis_result) throws Exception {
		/**
		 * There are video streams in this file ?
		 */
		for (Map.Entry<MetadataProvider, JSONObject> entry : analysis_result.processing_results.entrySet()) {
			if (entry.getKey() instanceof FFprobeAnalyser) {
				if (FFprobeAnalyser.hasVideo(entry.getValue()) == false) {
					return null;
				}
				break;
			}
		}
		
		ArrayList<RenderedElement> result = new ArrayList<RenderedElement>();
		RenderedElement element = new RenderedElement("snap", ".png");
		
		TranscodeProfile tprofile = TranscodeProfileManager.getProfile(new Profile("ffmpeg", "ffmpeg_snapshoot_first"));
		ArrayList<String> param = tprofile.makeCommandline(analysis_result.origin.getAbsolutePath(), element.getTempFile().getAbsolutePath());
		
		ExecprocessGettext process = new ExecprocessGettext(ffmpeg_bin, param);
		process.setEndlinewidthnewline(true);
		try {
			process.start();
		} catch (IOException e) {
			if (e instanceof ExecprocessBadExecutionException) {
				Log2Dump dump = new Log2Dump();
				dump.add("file", analysis_result.origin);
				dump.add("mime", analysis_result.mimetype);
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
	
}
