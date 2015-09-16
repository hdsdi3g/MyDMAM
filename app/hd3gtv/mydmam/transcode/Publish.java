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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import hd3gtv.configuration.Configuration;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.manager.AppManager;
import hd3gtv.mydmam.manager.JobContext;
import hd3gtv.mydmam.manager.JobNG;
import hd3gtv.mydmam.manager.JobProgression;
import hd3gtv.mydmam.manager.WorkerCapablities;
import hd3gtv.mydmam.manager.WorkerNG;
import hd3gtv.mydmam.storage.AbstractFile;
import hd3gtv.mydmam.storage.Storage;
import hd3gtv.tools.ExecBinaryPath;
import hd3gtv.tools.Execprocess;
import hd3gtv.tools.ExecprocessGettext;
import hd3gtv.tools.Timecode;

public class Publish extends WorkerNG {
	
	public static final String TRANSCODE_PROFILE_NAME = "ffmpeg_vod_live";
	
	private String workername;
	private long maxpresencewaittime;
	private long waittimesizedetection;
	private File sourcelocalfiles;
	private String deststorage;
	private Execprocess process;
	private FFmpegProgress progress;
	private File templocaldir;
	private boolean stop;
	
	public static void createPublishJob(String mediaid, Timecode duration, String program_name, Class<?> referer) throws ConnectionException {
		JobNG job = AppManager.createJob(new JobContextPublishing(mediaid, duration, program_name));
		job.setCreator(referer).setName(program_name).setUrgent().setExpirationTime(2, TimeUnit.HOURS).publish();
	}
	
	public Publish() throws FileNotFoundException {
		if (Configuration.global.isElementExists("transcoding_probe") == false) {
			return;
		}
		
		maxpresencewaittime = Configuration.global.getValue("transcoding_probe", "maxpresencewaittime", 300l) * 1000l;
		waittimesizedetection = Configuration.global.getValue("transcoding_probe", "waittimesizedetection", 1l) * 1000l;
		
		try {
			sourcelocalfiles = new File(Configuration.global.getValue("transcoding_probe", "sourcelocalfiles", null));
		} catch (Exception e) {
			throw new NullPointerException("\"sourcelocalfiles\" param in transcoding in xml configuration can't to be null : " + e.getMessage());
		}
		if ((sourcelocalfiles.exists() == false) | (sourcelocalfiles.isDirectory() == false) | (sourcelocalfiles.canRead() == false)) {
			throw new FileNotFoundException(sourcelocalfiles.getPath());
		}
		
		try {
			deststorage = Configuration.global.getValue("transcoding_probe", "deststorage", null);
			Storage.getByName(deststorage).testStorageOperations();
		} catch (Exception e) {
			throw new FileNotFoundException(deststorage);
		}
		
		try {
			templocaldir = new File(Configuration.global.getValue("transcoding_probe", "templocaldir", null));
		} catch (Exception e) {
			throw new NullPointerException("\"templocaldir\" param in transcoding in xml configuration can't to be null : " + e.getMessage());
		}
		if ((templocaldir.exists() == false) | (templocaldir.isDirectory() == false) | (sourcelocalfiles.canWrite() == false)) {
			throw new FileNotFoundException(templocaldir.getPath());
		}
		
		Log2Dump dump = new Log2Dump();
		dump.add("workername", workername);
		dump.add("maxpresencewaittime", maxpresencewaittime);
		dump.add("sourcelocalfiles", sourcelocalfiles);
		dump.add("deststorage", deststorage);
		dump.add("templocaldir", templocaldir);
		Log2.log.debug("Init Publish", dump);
	}
	
	protected void workerProcessJob(final JobProgression progression, JobContext context) throws Exception {
		stop = false;
		
		final JobContextPublishing context_publish = (JobContextPublishing) context;
		
		long start_wait_time;
		File source_file;
		long old_size;
		File dest_file_ffmpeg;
		TranscodeProfile profile;
		File dest_file_qtfs;
		BufferedInputStream bis;
		AbstractFile dest_dir;
		AbstractFile dest_file;
		BufferedOutputStream bos;
		byte[] buffer;
		int len;
		
		progression.update("Attente de la présence du media...");
		progression.updateStep(0, 5);
		
		/**
		 * Wait & with cancel if too long time.
		 */
		source_file = new File(sourcelocalfiles.getPath() + File.separator + context_publish.mediaid.toUpperCase() + ".mov");
		start_wait_time = System.currentTimeMillis();
		while ((start_wait_time + maxpresencewaittime) > System.currentTimeMillis()) {
			if (stop) {
				return;
			}
			if (source_file.exists()) {
				if (source_file.length() > 0) {
					break;
				}
			}
			Thread.sleep(1000);
		}
		
		if (source_file.exists() == false) {
			progression.update("Fichier introuvable dans le stockage.");
			throw new FileNotFoundException(source_file.getPath());
		}
		
		if (stop) {
			return;
		}
		progression.updateStep(1, 5);
		progression.update("Media en cours de copie, attente de sa disponiblité pour son traitement...");
		
		/**
		 * Wait copy
		 */
		old_size = source_file.length();
		while (true) {
			if (stop) {
				return;
			}
			if (source_file.length() != old_size) {
				old_size = source_file.length();
				Thread.sleep(waittimesizedetection);
			} else {
				Thread.sleep(waittimesizedetection);
				if (source_file.length() == old_size) {
					break;
				} else {
					old_size = source_file.length();
					Thread.sleep(waittimesizedetection);
				}
			}
		}
		
		if (stop) {
			return;
		}
		progression.updateStep(2, 5);
		progression.update("Conversion ffmpeg");
		
		dest_file_ffmpeg = new File(templocaldir.getPath() + File.separator + context_publish.mediaid.toUpperCase() + "-" + progression.getJobKey().replaceAll(":", "-") + ".mp4");
		
		profile = TranscodeProfile.getTranscodeProfile(TRANSCODE_PROFILE_NAME);
		
		File progress_file = new File(dest_file_ffmpeg.getPath() + "-progress.txt");
		progress_file.delete();
		
		FFmpegProgressCallback progress_callback = new FFmpegProgressCallback() {
			public void updateProgression(float position, float duration, float performance_fps, int frame, int dup_frames, int drop_frames) {
				context_publish.performance_fps = performance_fps;
				context_publish.frame = frame;
				context_publish.dup_frames = dup_frames;
				context_publish.drop_frames = drop_frames;
				progression.updateProgress(Math.round(position), (int) Math.round(Math.ceil(duration)));
			}
			
			public Timecode getSourceDuration() {
				return context_publish.duration;
			}
			
			public String getJobKey() {
				return progression.getJobKey();
			}
		};
		
		progress = new FFmpegProgress(progress_file, progress_callback);
		progress.start();
		
		FFmpegEvents events = new FFmpegEvents("tmp-" + progression.getJobKey());
		
		Log2Dump dump = new Log2Dump();
		dump.addAll(context_publish);
		dump.add("source_file", source_file);
		dump.add("dest_file", dest_file_ffmpeg);
		dump.add("progress_file", progress_file);
		dump.add("profile", profile);
		Log2.log.debug("Prepare execprocess", dump);
		
		this.process = profile.createProcessConfiguration(source_file, dest_file_ffmpeg).setProgressFile(progress_file).prepareExecprocess(events);
		
		dump = new Log2Dump();
		dump.addAll(context_publish);
		dump.add("source_file", source_file);
		dump.add("dest_file", dest_file_ffmpeg);
		dump.add("profile", profile);
		dump.add("commandline", process.getCommandline());
		Log2.log.info("Start ffmpeg", dump);
		
		process.run();
		
		progress.stopWatching();
		progress_file.delete();
		
		if (stop) {
			return;
		}
		
		if (process.getExitvalue() != 0) {
			throw new IOException("Bad ffmpeg execution: " + events.getLast_message());
		}
		
		progression.updateStep(3, 5);
		progression.update("Finalisation du traitement");
		
		/**
		 * qt-faststart convert
		 */
		dest_file_qtfs = new File(templocaldir.getPath() + File.separator + context_publish.mediaid.toUpperCase() + "-" + progression.getJobKey() + "-faststart" + ".f4v");
		faststartFile(dest_file_ffmpeg, dest_file_qtfs);
		
		dest_file_ffmpeg.delete();
		
		if (stop) {
			return;
		}
		progression.updateStep(4, 5);
		progression.update("Publication du média");
		progression.updateProgress(1, 1);
		
		/**
		 * End storage move
		 */
		bis = new BufferedInputStream(new FileInputStream(dest_file_qtfs), 0xFFFF);
		dest_dir = Storage.getByName(deststorage).getRootPath();
		dest_file = dest_dir.getAbstractFile("/" + context_publish.mediaid.toUpperCase() + ".f4v");
		bos = dest_file.getOutputStream(0xFFFF);
		buffer = new byte[0xFFFF];
		while ((len = bis.read(buffer)) > 0) {
			if (stop) {
				bis.close();
				return;
			}
			bos.write(buffer, 0, len);
		}
		bis.close();
		bos.close();
		dest_file.close();
		dest_dir.close();
		
		dest_file_qtfs.delete();
		
		progression.updateStep(5, 5);
		progression.update("Publication terminée");
	}
	
	public synchronized void forceStopProcess() throws Exception {
		stop = true;
		if (progress != null) {
			progress.stopWatching();
		}
		if (process != null) {
			process.kill();
		}
	}
	
	public static void faststartFile(File source_file, File dest_file) throws Exception {
		ArrayList<String> param = new ArrayList<String>();
		param.add(source_file.getPath());
		param.add(dest_file.getPath());
		
		Log2Dump dump = new Log2Dump();
		dump.add("source_file", source_file);
		dump.add("dest_file", dest_file);
		Log2.log.debug("Fast start", dump);
		
		ExecprocessGettext process = new ExecprocessGettext(ExecBinaryPath.get("qt-faststart"), param);
		process.setEndlinewidthnewline(true);
		process.start();
		
		dump.add("result", process.getResultstdout());
		Log2.log.info("Fast start done", dump);
		source_file.delete();
	}
	
	public WorkerCategory getWorkerCategory() {
		return WorkerCategory.EXTERNAL_MODULE;
	}
	
	public String getWorkerLongName() {
		return "Media transcoding and publishing";
	}
	
	public String getWorkerVendorName() {
		return "MyDMAM Addons";
	}
	
	public List<WorkerCapablities> getWorkerCapablities() {
		return WorkerCapablities.createList(JobContextPublishing.class);
	}
	
	protected boolean isActivated() {
		return Configuration.global.isElementExists("transcoding_probe");
	}
}
