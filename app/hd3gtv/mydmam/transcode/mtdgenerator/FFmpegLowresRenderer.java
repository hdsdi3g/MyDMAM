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
import hd3gtv.mydmam.manager.JobProgression;
import hd3gtv.mydmam.metadata.FutureCreateTasks;
import hd3gtv.mydmam.metadata.GeneratorRendererViaWorker;
import hd3gtv.mydmam.metadata.JobContextRenderer;
import hd3gtv.mydmam.metadata.PreviewType;
import hd3gtv.mydmam.metadata.RenderedFile;
import hd3gtv.mydmam.metadata.WorkerRenderer;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.EntryRenderer;
import hd3gtv.mydmam.transcode.FFmpegEvents;
import hd3gtv.mydmam.transcode.FFmpegProgress;
import hd3gtv.mydmam.transcode.FFmpegProgressCallback;
import hd3gtv.mydmam.transcode.Publish;
import hd3gtv.mydmam.transcode.TranscodeProfile;
import hd3gtv.mydmam.transcode.mtdcontainer.FFprobe;
import hd3gtv.tools.Execprocess;
import hd3gtv.tools.Timecode;

import java.awt.Point;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

public class FFmpegLowresRenderer implements GeneratorRendererViaWorker {
	
	private String ffmpeg_bin;
	
	private TranscodeProfile transcode_profile;
	private PreviewType preview_type;
	private boolean audio_only;
	private Class<? extends EntryRenderer> root_entry_class;
	private Class<? extends JobContextFFmpegLowresRenderer> context_class;
	
	public FFmpegLowresRenderer(Class<? extends JobContextFFmpegLowresRenderer> context_class, PreviewType preview_type, boolean audio_only) throws InstantiationException, IllegalAccessException {
		ffmpeg_bin = Configuration.global.getValue("transcoding", "ffmpeg_bin", "ffmpeg");
		this.context_class = context_class;
		if (context_class == null) {
			throw new NullPointerException("\"context_class\" can't to be null");
		}
		
		JobContextFFmpegLowresRenderer item = context_class.newInstance();
		root_entry_class = item.getEntryRendererClass();
		
		if (TranscodeProfile.isConfigured()) {
			this.transcode_profile = TranscodeProfile.getTranscodeProfile("ffmpeg", item.getTranscodeProfileName());
			
			this.preview_type = preview_type;
			if (preview_type == null) {
				throw new NullPointerException("\"preview_type\" can't to be null");
			}
			this.audio_only = audio_only;
		}
	}
	
	public String getLongName() {
		if (transcode_profile != null) {
			return "FFmpeg renderer (" + transcode_profile.getName() + ")";
		} else {
			return "FFmpeg renderer (null)";
		}
	}
	
	public boolean isEnabled() {
		return (new File(ffmpeg_bin)).exists() & (transcode_profile != null);
	}
	
	public boolean canProcessThis(String mimetype) {
		if (audio_only) {
			return FFprobeAnalyser.canProcessThisAudioOnly(mimetype);
		} else {
			return FFprobeAnalyser.canProcessThisVideoOnly(mimetype);
		}
	}
	
	public EntryRenderer process(Container container) throws Exception {
		return null;
	}
	
	public PreviewType getPreviewTypeForRenderer(Container container, EntryRenderer entry) {
		return preview_type;
	}
	
	private Execprocess process;
	private FFmpegProgress progress;
	private boolean stop;
	
	public EntryRenderer standaloneProcess(File origin, final JobProgression job_progress, Container container, JobContextRenderer renderer_context) throws Exception {
		stop = false;
		job_progress.updateStep(1, 3);
		
		final JobContextFFmpegLowresRenderer ffmpeg_renderer_context = (JobContextFFmpegLowresRenderer) renderer_context;
		
		if (origin.exists() == false) {
			job_progress.update("Can't found master original file.");
			throw new FileNotFoundException(origin.getPath());
		}
		
		if (stop) {
			return null;
		}
		
		job_progress.update("Start ffmpeg convert operation");
		
		RenderedFile progress_file = new RenderedFile("video_progress", "txt");
		RenderedFile temp_element = new RenderedFile(transcode_profile.getName(), transcode_profile.getExtension("mp4"));
		
		FFmpegProgressCallback callback = new FFmpegProgressCallback() {
			
			public void updateProgression(float position, float duration, float performance_fps, int frame, int dup_frames, int drop_frames) {
				ffmpeg_renderer_context.performance_fps = performance_fps;
				ffmpeg_renderer_context.frame = frame;
				ffmpeg_renderer_context.dup_frames = dup_frames;
				ffmpeg_renderer_context.drop_frames = drop_frames;
				job_progress.updateProgress(Math.round(position), (int) Math.round(Math.ceil(duration)));
			}
			
			public Timecode getSourceDuration() {
				return new Timecode(ffmpeg_renderer_context.source_duration, ffmpeg_renderer_context.source_fps);
			}
			
			public String getJobKey() {
				return job_progress.getJobKey();
			}
			
		};
		
		progress = new FFmpegProgress(progress_file.getTempFile(), callback);
		progress.start();
		
		FFmpegEvents events = new FFmpegEvents(job_progress.getJobKey() + ": " + origin.getName());
		process = transcode_profile.prepareExecprocess(ffmpeg_bin, events, origin, temp_element.getTempFile(), progress_file.getTempFile());
		
		Log2Dump dump = new Log2Dump();
		dump.add("job", job_progress.getJobKey());
		dump.add("origin", origin);
		dump.add("temp_file", temp_element.getTempFile());
		dump.add("transcode_profile", transcode_profile);
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
		
		job_progress.updateStep(2, 3);
		job_progress.update("Finalizing");
		
		RenderedFile final_element = null;
		
		/**
		 * qt-faststart convert
		 */
		
		if (ffmpeg_renderer_context.faststarted) {
			final_element = new RenderedFile(transcode_profile.getName(), transcode_profile.getExtension("mp4"));
			Publish.faststartFile(temp_element.getTempFile(), final_element.getTempFile());
			temp_element.deleteTempFile();
		} else {
			final_element = temp_element;
		}
		
		if (stop) {
			return null;
		}
		
		job_progress.updateStep(3, 3);
		job_progress.update("Converting is ended");
		
		return final_element.consolidateAndExportToEntry(root_entry_class.newInstance(), container, this);
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
	
	public void prepareTasks(final Container container, List<FutureCreateTasks> current_create_task_list) throws Exception {
		final GeneratorRendererViaWorker source = this;
		
		FutureCreateTasks result = new FutureCreateTasks() {
			public void createTask() throws ConnectionException {
				FFprobe ffprobe = container.getByClass(FFprobe.class);
				if (ffprobe == null) {
					return;
				}
				if (audio_only == ffprobe.hasVideo()) {
					/**
					 * Audio profile with audio source OR video+audio profile with video+audio source
					 */
					return;
				}
				Timecode timecode = ffprobe.getDuration();
				if (timecode == null) {
					return;
				}
				if (transcode_profile == null) {
					return;
				}
				
				if (container.getSummary().master_as_preview) {
					/**
					 * Must I render a preview file ?
					 */
					if (ffprobe.hasVideo() == false) {
						/**
						 * Source is audio only, Master as preview is ok, no rendering.
						 */
						return;
					} else {
						/**
						 * video is ok ?
						 */
						Point resolution = ffprobe.getVideoResolution();
						if (resolution == null) {
							return;
						}
						if (transcode_profile.getOutputformat() != null) {
							/**
							 * Test if source file has an upper resolution relative at the profile
							 */
							Point profile_resolution = transcode_profile.getOutputformat().getResolution();
							if ((profile_resolution.x > resolution.x) | (profile_resolution.y > resolution.y)) {
								return;
							} else if ((profile_resolution.x == resolution.x) & (profile_resolution.y == resolution.y)) {
								return;
							}
						}
					}
				}
				
				JobContextFFmpegLowresRenderer renderer_context = null;
				try {
					renderer_context = context_class.newInstance();
				} catch (Exception e) {
					Log2.log.error("Impossible error", e);
					return;
				}
				renderer_context.source_fps = timecode.getFps();
				renderer_context.source_duration = timecode.getValue();
				if (transcode_profile.getOutputformat() != null) {
					renderer_context.faststarted = transcode_profile.getOutputformat().isFaststarted();
				} else {
					renderer_context.faststarted = false;
				}
				
				try {
					WorkerRenderer.createTask(container.getOrigin().getPathindexElement(), "FFmpeg lowres for metadatas", renderer_context, source);
				} catch (FileNotFoundException e) {
					Log2.log.error("Can't found valid element", e, container);
				}
			}
		};
		
		current_create_task_list.add(result);
	}
	
	public Class<? extends EntryRenderer> getRootEntryClass() {
		return root_entry_class;
	}
	
	public Class<? extends JobContextRenderer> getContextClass() {
		return context_class;
	}
	
}
