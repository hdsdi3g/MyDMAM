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
import hd3gtv.tools.Execprocess;
import hd3gtv.tools.Timecode;

import java.awt.Point;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.json.simple.JSONObject;

public class FFmpegLowresRenderer implements RendererViaWorker {
	
	public static final TranscodeProfile transcode_profile_ffmpeg_lowres_lq = new TranscodeProfile("ffmpeg", "ffmpeg_lowres_lq");
	public static final TranscodeProfile transcode_profile_ffmpeg_lowres_sd = new TranscodeProfile("ffmpeg", "ffmpeg_lowres_sd");
	public static final TranscodeProfile transcode_profile_ffmpeg_lowres_hd = new TranscodeProfile("ffmpeg", "ffmpeg_lowres_hd");
	public static final TranscodeProfile transcode_profile_ffmpeg_lowres_audio = new TranscodeProfile("ffmpeg", "ffmpeg_lowres_audio");
	
	private String ffmpeg_bin;
	
	private TranscodeProfile transcode_profile;
	private PreviewType preview_type;
	private boolean audio_only;
	
	public FFmpegLowresRenderer(TranscodeProfile transcode_profile, PreviewType preview_type, boolean audio_only) {
		ffmpeg_bin = Configuration.global.getValue("transcoding", "ffmpeg_bin", "ffmpeg");
		this.transcode_profile = transcode_profile;
		if (transcode_profile == null) {
			throw new NullPointerException("\"transcode_profile\" can't to be null");
		}
		this.preview_type = preview_type;
		if (preview_type == null) {
			throw new NullPointerException("\"preview_type\" can't to be null");
		}
		this.audio_only = audio_only;
	}
	
	public String getLongName() {
		return "FFmpeg renderer (" + transcode_profile.getName() + ")";
	}
	
	public String getElasticSearchIndexType() {
		return "pvw_" + transcode_profile.getName();
	}
	
	public boolean isEnabled() {
		return (new File(ffmpeg_bin)).exists() & TranscodeProfileManager.isEnabled();
	}
	
	public boolean canProcessThis(String mimetype) {
		if (audio_only) {
			return FFprobeAnalyser.canProcessThisAudioOnly(mimetype);
		} else {
			return FFprobeAnalyser.canProcessThisVideoOnly(mimetype);
		}
	}
	
	public List<RenderedElement> process(MetadataIndexerResult analysis_result) throws Exception {
		JSONObject processresult = FFprobeAnalyser.getAnalysedProcessresult(analysis_result);
		if (processresult == null) {
			return null;
		}
		if (audio_only == FFprobeAnalyser.hasVideo(processresult)) {
			/**
			 * Audio profile with audio source OR video+audio profile with video+audio source
			 */
			return null;
		}
		Timecode timecode = FFprobeAnalyser.getDuration(processresult);
		if (timecode == null) {
			return null;
		}
		TranscodeProfile t_profile = TranscodeProfileManager.getProfile(transcode_profile);
		if (t_profile == null) {
			return null;
		}
		
		if (analysis_result.isMaster_as_preview()) {
			/**
			 * Must I render a preview file ?
			 */
			if (FFprobeAnalyser.hasVideo(processresult) == false) {
				/**
				 * Source is audio only, Master as preview is ok, no rendering.
				 */
				return null;
			} else {
				/**
				 * video is ok ?
				 */
				Point resolution = FFprobeAnalyser.getVideoResolution(processresult);
				if (resolution == null) {
					return null;
				}
				if (t_profile.getOutputformat() != null) {
					/**
					 * Test if source file has an upper resolution relative at the profile
					 */
					Point profile_resolution = t_profile.getOutputformat().getResolution();
					if ((profile_resolution.x > resolution.x) | (profile_resolution.y > resolution.y)) {
						return null;
					} else if ((profile_resolution.x == resolution.x) & (profile_resolution.y == resolution.y)) {
						return null;
					}
				}
			}
		}
		
		JSONObject renderer_context = new JSONObject();
		renderer_context.put("fps", timecode.getFps());
		renderer_context.put("duration", timecode.getValue());
		if (t_profile.getOutputformat() != null) {
			renderer_context.put("faststarted", t_profile.getOutputformat().isFaststarted());
		} else {
			renderer_context.put("faststarted", false);
		}
		
		MetadataRendererWorker.createTask(analysis_result.getReference().prepare_key(), "FFmpeg lowres for metadatas", renderer_context, this);
		return null;
	}
	
	public PreviewType getPreviewTypeForRenderer(LinkedHashMap<String, JSONObject> all_metadatas_for_element, List<RenderedElement> rendered_elements) {
		return preview_type;
	}
	
	public JSONObject getPreviewConfigurationForRenderer(PreviewType preview_type, LinkedHashMap<String, JSONObject> all_metadatas_for_element, List<RenderedElement> rendered_elements) {
		/*JSONObject processresult = all_metadatas_for_element.get(new FFprobeAnalyser().getElasticSearchIndexType());
		processresult.get("ffmpeglowres");*/
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
		job.step = 0;
		
		job.last_message = "Start ffmpeg convert operation";
		
		TranscodeProfile profile = TranscodeProfileManager.getProfile(transcode_profile);
		
		RenderedElement progress_file = new RenderedElement("video_progress", "txt");
		RenderedElement temp_element = new RenderedElement(transcode_profile.getName(), profile.getExtension("mp4"));
		
		Float source_fps = 25f;
		if (renderer_context.containsKey("fps")) {
			source_fps = ((Double) renderer_context.get("fps")).floatValue();
		}
		Float duration = 1f;
		if (renderer_context.containsKey("duration")) {
			duration = ((Double) renderer_context.get("duration")).floatValue();
		}
		
		progress = new FFmpegProgress(progress_file.getTempFile(), job, new Timecode(duration, source_fps));
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
		
		job.step = 1;
		job.last_message = "Finalizing";
		
		RenderedElement final_element = null;
		
		/**
		 * qt-faststart convert
		 */
		Boolean faststarted = false;
		if (renderer_context.containsKey("faststarted")) {
			faststarted = ((Boolean) renderer_context.get("faststarted"));
		}
		if (faststarted) {
			final_element = new RenderedElement(transcode_profile.getName(), profile.getExtension("mp4"));
			Publish.faststartFile(temp_element.getTempFile(), final_element.getTempFile());
			temp_element.deleteTempFile();
		} else {
			final_element = temp_element;
		}
		
		if (stop) {
			return null;
		}
		job.step = 2;
		
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
