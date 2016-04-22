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

import java.awt.Point;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.manager.JobNG;
import hd3gtv.mydmam.manager.JobProgression;
import hd3gtv.mydmam.metadata.ContainerEntryResult;
import hd3gtv.mydmam.metadata.FutureCreateJobs;
import hd3gtv.mydmam.metadata.JobContextMetadataRenderer;
import hd3gtv.mydmam.metadata.MetadataGeneratorRendererViaWorker;
import hd3gtv.mydmam.metadata.PreviewType;
import hd3gtv.mydmam.metadata.RenderedFile;
import hd3gtv.mydmam.metadata.WorkerRenderer;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.ContainerEntry;
import hd3gtv.mydmam.metadata.container.EntryRenderer;
import hd3gtv.mydmam.transcode.Publish;
import hd3gtv.mydmam.transcode.TranscodeProfile;
import hd3gtv.mydmam.transcode.TranscodeProfile.ProcessConfiguration;
import hd3gtv.mydmam.transcode.TranscodeProgress;
import hd3gtv.mydmam.transcode.TranscodeProgressFFmpeg;
import hd3gtv.mydmam.transcode.mtdcontainer.FFmpegInterlacingStats;
import hd3gtv.mydmam.transcode.mtdcontainer.FFprobe;
import hd3gtv.mydmam.transcode.mtdcontainer.Stream;
import hd3gtv.tools.Execprocess;
import hd3gtv.tools.StoppableProcessing;
import hd3gtv.tools.Timecode;
import hd3gtv.tools.VideoConst.Interlacing;

public class FFmpegLowresRenderer implements MetadataGeneratorRendererViaWorker {
	
	private TranscodeProfile transcode_profile;
	private PreviewType preview_type;
	private boolean audio_only;
	private Class<? extends EntryRenderer> root_entry_class;
	private Class<? extends JobContextFFmpegLowresRenderer> context_class;
	
	public FFmpegLowresRenderer(Class<? extends JobContextFFmpegLowresRenderer> context_class, PreviewType preview_type, boolean audio_only) throws InstantiationException, IllegalAccessException {
		this.context_class = context_class;
		if (context_class == null) {
			throw new NullPointerException("\"context_class\" can't to be null");
		}
		
		JobContextFFmpegLowresRenderer item = context_class.newInstance();
		root_entry_class = item.getEntryRendererClass();
		
		if (TranscodeProfile.isConfigured()) {
			this.transcode_profile = TranscodeProfile.getTranscodeProfile(item.getTranscodeProfileName());
			
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
		return (transcode_profile != null);
	}
	
	public boolean canProcessThisMimeType(String mimetype) {
		if (audio_only) {
			return FFprobeAnalyser.canProcessThisAudioOnly(mimetype);
		} else {
			return FFprobeAnalyser.canProcessThisVideoOnly(mimetype);
		}
	}
	
	public ContainerEntryResult processFast(Container container) throws Exception {
		return null;
	}
	
	public ContainerEntryResult processFull(Container container, StoppableProcessing stoppable) throws Exception {
		return null;
	}
	
	public PreviewType getPreviewTypeForRenderer(Container container, EntryRenderer entry) {
		return preview_type;
	}
	
	private Execprocess process;
	private TranscodeProgress progress;
	private boolean stop;
	
	public ContainerEntryResult standaloneProcess(File origin, final JobProgression job_progress, Container container, JobContextMetadataRenderer renderer_context) throws Exception {
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
		
		ProcessConfiguration process_conf = transcode_profile.createProcessConfiguration(origin, temp_element.getTempFile());
		
		progress = process_conf.getProgress();
		if (progress == null) {
			progress = new TranscodeProgressFFmpeg();
		}
		progress.init(progress_file.getTempFile(), job_progress, ffmpeg_renderer_context).startWatching();
		
		/**
		 * Video filters
		 */
		ArrayList<String> filters = new ArrayList<String>();
		FFmpegInterlacingStats interlace_stats = container.getByClass(FFmpegInterlacingStats.class);
		if (interlace_stats != null) {
			if (interlace_stats.getInterlacing() != Interlacing.Progressive) {
				filters.add("yadif");
			}
		}
		FFprobe ffprobe = container.getByClass(FFprobe.class);
		if (ffprobe != null) {
			if (ffprobe.hasVerticalBlankIntervalInImage()) {
				/**
				 * Cut the 32 lines from the top.
				 */
				filters.add("crop=w=in_w:h=in_h-32:x=0:y=32");
			}
		}
		
		/**
		 * Resize the image with the correct aspect ratio, and add, if needed, black borders.
		 */
		filters.add("scale=iw*sar:ih,pad=max(iw\\,ih*(16/9)):ow/(16/9):(ow-iw)/2:(oh-ih)/2");
		
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
		
		/**
		 * Audio filter (channel map)
		 */
		if (ffprobe.hasAudio()) {
			List<Stream> audio_streams = ffprobe.getStreamsByCodecType("audio");
			
			if (audio_streams.size() == 1) {
				/**
				 * Source has only one stream
				 * -filter_complex "nullsink"
				 */
				process_conf.getParamTags().put("AUDIOMAPFILTER", "anullsink");
			} else if (audio_streams.get(0).getAudioChannelCount() == 1 && audio_streams.get(1).getAudioChannelCount() == 1) {
				/**
				 * Channel 1 and 2 are mono => regroup to stereo.
				 * -filter_complex "[0:1][0:2]amerge=inputs=2"
				 */
				process_conf.getParamTags().put("AUDIOMAPFILTER", audio_streams.get(0).getMapReference(0) + audio_streams.get(1).getMapReference(0) + "amerge=inputs=2");
			} else {
				/**
				 * Source is too complex to import it correctly. We will only take the first stream.
				 * -filter_complex "nullsink"
				 */
				process_conf.getParamTags().put("AUDIOMAPFILTER", "anullsink");
			}
		} else {
			/**
			 * No audio. No process.
			 */
			process_conf.getParamTags().put("AUDIOMAPFILTER", "nullsink");
		}
		
		process = process_conf.prepareExecprocess(job_progress.getJobKey() + ": " + origin.getName());
		
		Loggers.Transcode_Metadata.info("Start ffmpeg, " + "job: " + job_progress.getJobKey() + ", origin: " + origin + ", temp_file: " + temp_element.getTempFile() + ", transcode_profile: "
				+ transcode_profile + ", commandline: \"" + process.getCommandline() + "\"");
				
		process.run();
		
		progress.stopWatching();
		progress_file.deleteTempFile();
		
		if (stop) {
			return null;
		}
		
		if (process.getExitvalue() != 0) {
			if (process_conf.getEvent() != null) {
				throw new IOException("Bad ffmpeg execution: " + process_conf.getEvent().getLast_message());
			}
			throw new IOException("Bad ffmpeg execution");
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
		
		return new ContainerEntryResult(final_element.consolidateAndExportToEntry(root_entry_class.newInstance(), container, this));
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
	
	public void prepareJobs(final Container container, List<FutureCreateJobs> current_create_jobs_list) throws Exception {
		final MetadataGeneratorRendererViaWorker source = this;
		
		FutureCreateJobs result = new FutureCreateJobs() {
			public JobNG createJob() throws ConnectionException {
				FFprobe ffprobe = container.getByClass(FFprobe.class);
				if (ffprobe == null) {
					return null;
				}
				if (audio_only == ffprobe.hasVideo()) {
					/**
					 * Audio profile with audio source OR video+audio profile with video+audio source
					 */
					return null;
				}
				Timecode timecode = ffprobe.getDuration();
				if (timecode == null) {
					return null;
				}
				if (transcode_profile == null) {
					return null;
				}
				
				if (container.getSummary().master_as_preview) {
					/**
					 * Must I render a preview file ?
					 */
					if (ffprobe.hasVideo() == false) {
						/**
						 * Source is audio only, Master as preview is ok, no rendering.
						 */
						return null;
					} else {
						/**
						 * video is ok ?
						 */
						Point resolution = ffprobe.getVideoResolution();
						if (resolution == null) {
							return null;
						}
						if (transcode_profile.getOutputformat() != null) {
							/**
							 * Test if source file has an upper resolution relative at the profile
							 */
							Point profile_resolution = transcode_profile.getOutputformat().getResolution();
							if ((profile_resolution.x > resolution.x) | (profile_resolution.y > resolution.y)) {
								return null;
							} else if ((profile_resolution.x == resolution.x) & (profile_resolution.y == resolution.y)) {
								return null;
							}
						}
					}
				}
				
				JobContextFFmpegLowresRenderer renderer_context = null;
				try {
					renderer_context = context_class.newInstance();
				} catch (Exception e) {
					Loggers.Transcode_Metadata.error("Impossible error", e);
					return null;
				}
				renderer_context.source_fps = timecode.getFps();
				renderer_context.source_duration = timecode.getValue();
				if (transcode_profile.getOutputformat() != null) {
					renderer_context.faststarted = transcode_profile.getOutputformat().isFaststarted();
				} else {
					renderer_context.faststarted = false;
				}
				
				try {
					return WorkerRenderer.createJob(container.getOrigin().getPathindexElement(), "FFmpeg lowres for metadatas", renderer_context, source);
				} catch (FileNotFoundException e) {
					Loggers.Transcode_Metadata.error("Can't found valid element: " + container, e);
				}
				return null;
			}
		};
		
		current_create_jobs_list.add(result);
	}
	
	public List<Class<? extends ContainerEntry>> getAllRootEntryClasses() {
		return Arrays.asList(root_entry_class);
	}
	
	public Class<? extends JobContextMetadataRenderer> getContextClass() {
		return context_class;
	}
	
	public List<String> getMimeFileListCanUsedInMasterAsPreview() {
		return null;
	}
	
	public boolean isCanUsedInMasterAsPreview(Container container) {
		return false;
	}
}
