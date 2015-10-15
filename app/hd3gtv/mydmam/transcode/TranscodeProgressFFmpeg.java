/*
 * This file is part of MyDMAM
 * 
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam.transcode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.manager.JobContext;
import hd3gtv.mydmam.manager.JobProgression;
import hd3gtv.tools.Timecode;

public class TranscodeProgressFFmpeg extends TranscodeProgress {
	
	protected void processAnalystProgressFile(File progressfile, JobProgression progression, JobContext context) throws Exception {
		stopthread = false;
		String line;
		int raw_framepos;
		BufferedReader reader;
		
		float source_duration = 1;
		float fps = 1;
		
		ProgressForJobContextFFmpegBased progress_context = null;
		if (context instanceof ProgressForJobContextFFmpegBased) {
			progress_context = (ProgressForJobContextFFmpegBased) context;
			Timecode tc_source = progress_context.getSourceDuration();
			if (tc_source == null) {
				throw new NullPointerException("No source duration extracted from profile");
			}
			source_duration = tc_source.getValue();
			fps = tc_source.getFps();
		}
		
		String last_frame = "0";
		String last_fps = "0";
		String last_dup_frames = "0";
		String last_drop_frames = "0";
		int last_percent = 0;
		int lastpublished_percent = 0;
		
		float position;
		float performance_fps;
		int dup_frames;
		int drop_frames;
		
		/**
		 * frame=149
		 * fps=13.6
		 * stream_0_1_q=-2.0
		 * total_size=292144
		 * out_time_ms=6016000
		 * out_time=00:00:06.016000
		 * dup_frames=0
		 * drop_frames=0
		 * progress=end
		 * progress=continue
		 */
		reader = null;
		while (stopthread == false) {
			try {
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(progressfile)), 0xFFFF);
			} catch (FileNotFoundException e) {
				Thread.sleep(100);
				continue;
			}
			
			while (((line = reader.readLine()) != null)) {
				if (line.startsWith("fps=")) {
					last_fps = line.substring(4);
				} else if (line.startsWith("frame=")) {
					last_frame = line.substring(6);
				} else if (line.startsWith("dup_frames=")) {
					last_dup_frames = line.substring(11);
				} else if (line.startsWith("drop_frames=")) {
					last_drop_frames = line.substring(12);
				} else if (line.equals("progress=end")) {
					reader.close();
					Loggers.Transcode.debug("Watch ffmpeg progress is ended");
					return;
				}
			}
			reader.close();
			
			if (last_fps.equals("0")) {
				Thread.sleep(500);
				continue;
			}
			
			raw_framepos = Integer.parseInt(last_frame);
			last_percent = Math.round((((float) raw_framepos / fps) / source_duration) * 100f);
			if (lastpublished_percent == last_percent) {
				Thread.sleep(500);
				continue;
			} else {
				lastpublished_percent = last_percent;
			}
			
			position = (float) raw_framepos / fps;
			performance_fps = Float.parseFloat(last_fps);
			dup_frames = Integer.parseInt(last_dup_frames);
			drop_frames = Integer.parseInt(last_drop_frames);
			
			if (progress_context != null) {
				progress_context.setPerformance_fps(performance_fps);
				progress_context.setFrame(raw_framepos);
				progress_context.setDupFrame(dup_frames);
				progress_context.setDropFrame(drop_frames);
			}
			
			progression.updateProgress(Math.round(position), (int) Math.round(Math.ceil(source_duration)));
			
			Thread.sleep(2000);
		}
	}
}