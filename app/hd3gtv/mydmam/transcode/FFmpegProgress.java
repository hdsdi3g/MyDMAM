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

import hd3gtv.log2.Log2;
import hd3gtv.mydmam.taskqueue.Job;
import hd3gtv.tools.Timecode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;

class FFmpegProgress extends Thread {
	private File progressfile;
	
	/** in seconds */
	private float source_duration;
	private Job job;
	private boolean stopthread;
	private float fps;
	
	public FFmpegProgress(File progressfile, Job job, Timecode source_duration) {
		this.progressfile = progressfile;
		this.setDaemon(true);
		this.setName("FFmpegProgress for " + job.getKey());
		this.source_duration = source_duration.getValue();
		this.fps = source_duration.getFps();
		this.job = job;
	}
	
	synchronized void stopWatching() {
		stopthread = true;
	}
	
	public void run() {
		try {
			stopthread = false;
			String line;
			int raw_framepos;
			BufferedReader reader;
			
			String last_frame = "0";
			String last_fps = "0";
			String last_dup_frames = "0";
			String last_drop_frames = "0";
			int last_percent = 0;
			int lastpublished_percent = 0;
			
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
					sleep(100);
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
						Log2.log.debug("Watch ffmpeg progress is ended");
						return;
					}
				}
				reader.close();
				
				if (last_fps.equals("0")) {
					sleep(500);
					continue;
				}
				
				raw_framepos = Integer.parseInt(last_frame);
				last_percent = Math.round((((float) raw_framepos / fps) / source_duration) * 100f);
				if (lastpublished_percent == last_percent) {
					sleep(500);
					continue;
				} else {
					lastpublished_percent = last_percent;
				}
				
				job.progress = Math.round(last_percent);
				job.progress_size = 100;
				job.getContext().put("fps", Float.parseFloat(last_fps));
				job.getContext().put("frame", raw_framepos);
				job.getContext().put("dup_frames", Integer.parseInt(last_dup_frames));
				job.getContext().put("drop_frames", Integer.parseInt(last_drop_frames));
				sleep(2000);
			}
		} catch (Exception e) {
			Log2.log.error("Progress error", e);
		}
	}
}