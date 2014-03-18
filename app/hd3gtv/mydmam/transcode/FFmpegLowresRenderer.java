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
import hd3gtv.mydmam.analysis.MetadataIndexerResult;
import hd3gtv.mydmam.analysis.MetadataRendererWorker;
import hd3gtv.mydmam.analysis.PreviewType;
import hd3gtv.mydmam.analysis.RenderedElement;
import hd3gtv.mydmam.analysis.RendererViaWorker;
import hd3gtv.mydmam.taskqueue.Job;
import hd3gtv.mydmam.taskqueue.Profile;
import hd3gtv.tools.Execprocess;
import hd3gtv.tools.Timecode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;

public class FFmpegLowresRenderer implements RendererViaWorker {
	
	private String ffmpeg_bin;
	
	public FFmpegLowresRenderer() {
		ffmpeg_bin = Configuration.global.getValue("transcoding", "ffmpeg_bin", "ffmpeg");
	}
	
	public String getName() {
		return "FFmpeg lowres renderer";
	}
	
	public String getProfileName() {
		return getElasticSearchIndexType();
	}
	
	public String getElasticSearchIndexType() {
		return "ffmpeglowres";
	}
	
	public boolean isEnabled() {
		return (new File(ffmpeg_bin)).exists() & TranscodeProfileManager.isEnabled();
	}
	
	public boolean canProcessThis(String mimetype) {
		return FFprobeAnalyser.canProcessThisVideoOnly(mimetype);
	}
	
	public List<RenderedElement> process(MetadataIndexerResult analysis_result) throws Exception {
		JSONObject renderer_context = new JSONObject();
		renderer_context.put("fps", 25.0f);// TODO get FPS for analysis
		
		MetadataRendererWorker.createTask(analysis_result.getReference().prepare_key(), "FFmpeg lowres for metadata", renderer_context, this);
		return null;
	}
	
	@Override
	public PreviewType getPreviewTypeForRenderer(JSONObject mtd_summary, List<RenderedElement> rendered_elements) {
		// TODO Auto-generated method stub
		// mtd_summary.containsKey(key)
		
		return null;
	}
	
	@Override
	public JSONObject getPreviewConfigurationForRenderer(PreviewType preview_type, JSONObject mtd_summary, List<RenderedElement> rendered_elements) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private Execprocess process;
	private FFmpegProgress progress;
	private boolean stop;
	
	public List<RenderedElement> standaloneProcess(File origin, Job job, JSONObject renderer_context) throws Exception {
		stop = false;
		job.step_count = 3;
		
		if (origin.exists() == false) {
			job.last_message = "Can't found master original file.";
			throw new FileNotFoundException(origin.getPath());
		}
		
		if (stop) {
			return null;
		}
		job.step = 1;
		
		job.last_message = "Start ffmpeg convert operation";
		
		TranscodeProfile profile = TranscodeProfileManager.getProfile(new Profile("ffmpeg", "ffmpeg_lowres"));
		
		RenderedElement progress_file = new RenderedElement("video_progress", "txt");
		RenderedElement temp_element = new RenderedElement("video_lowq_noFS", profile.getExtention("mp4"));
		
		Float source_fps = 25f;
		if (renderer_context.containsKey("fps")) {
			source_fps = (Float) renderer_context.get("fps");
		}
		
		progress = new FFmpegProgress(progress_file.getTempFile(), job, new Timecode((String) job.getContext().get("duration"), source_fps));
		progress.start();
		
		FFmpegEvents events = new FFmpegEvents(job.getKey() + ": " + origin.getName());
		process = new Execprocess(ffmpeg_bin, profile.makeCommandline(origin.getAbsolutePath(), temp_element.getTempFile().getAbsolutePath(), progress_file.getTempFile().getPath()), events);
		
		Log2Dump dump = new Log2Dump();
		dump.add("job", job.getKey());
		dump.add("origin", origin);
		dump.add("temp_file", temp_element.getTempFile());
		dump.add("profile", profile);
		dump.add("commandline", process.getCommandline());
		Log2.log.info("Start ffmpeg", dump);
		
		process.run();
		
		progress.stopWatching();
		progress_file.deleteTempFile();
		
		if (stop) {
			return null;
		}
		
		if (process.getExitvalue() != 0) {
			throw new IOException("Bad ffmpeg execution: " + events.getLast_message());
		}
		
		job.step = 2;
		job.last_message = "Finalizing";
		
		RenderedElement final_element = new RenderedElement("video_lowq", profile.getExtention("mp4"));
		/**
		 * qt-faststart convert
		 */
		Publish.faststartFile(temp_element.getTempFile(), final_element.getTempFile());
		
		temp_element.deleteTempFile();
		
		if (stop) {
			return null;
		}
		job.step = 3;
		
		job.last_message = "Converting is ended";
		job.progress = 1;
		job.progress_size = 1;
		
		ArrayList<RenderedElement> result = new ArrayList<RenderedElement>();
		result.add(final_element);
		return result;
	}
	
	public synchronized void stopStandaloneProcess() throws Exception {
		stop = true;
		if (progress != null) {
			progress.stopWatching();
		}
		if (process != null) {
			process.kill();
		}
	}
	
}
