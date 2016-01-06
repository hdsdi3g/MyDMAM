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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.manager.AppManager;
import hd3gtv.mydmam.manager.JobContext;
import hd3gtv.mydmam.manager.JobNG;
import hd3gtv.mydmam.manager.JobProgression;
import hd3gtv.mydmam.manager.WorkerCapablities;
import hd3gtv.mydmam.manager.WorkerNG;
import hd3gtv.mydmam.storage.AbstractFile;
import hd3gtv.mydmam.storage.Storage;
import hd3gtv.mydmam.transcode.TranscodeProfile.ProcessConfiguration;
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
	private File templocaldir;
	private boolean stop;
	private TranscodeProgress transcode_progress;
	
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
		
		LinkedHashMap<String, Object> log = new LinkedHashMap<String, Object>();
		log.put("workername", workername);
		log.put("maxpresencewaittime", maxpresencewaittime);
		log.put("sourcelocalfiles", sourcelocalfiles);
		log.put("deststorage", deststorage);
		log.put("templocaldir", templocaldir);
		Loggers.Transcode.info("Init Publish " + log.toString());
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
		Loggers.Transcode.debug("Publish wait media " + context_publish.contextToJson().toString());
		
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
			Thread.sleep(10);
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
		Loggers.Transcode.debug("Publish media is copying " + context_publish.contextToJson().toString());
		
		/**
		 * Wait copy
		 */
		old_size = source_file.length();
		while (true) {
			if (stop) {
				return;
			}
			Loggers.Transcode.trace("Publish media size is growing " + context_publish.contextToJson().toString() + " / " + source_file.length());
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
		
		LinkedHashMap<String, Object> log = new LinkedHashMap<String, Object>();
		log.put("source_file", source_file);
		log.put("dest_file", dest_file_ffmpeg);
		log.put("progress_file", progress_file);
		log.put("profile", profile);
		Loggers.Transcode.debug("Publish prepare transcoding " + context_publish.contextToJson().toString() + " " + log.toString());
		
		ProcessConfiguration process_conf = profile.createProcessConfiguration(source_file, dest_file_ffmpeg);
		
		transcode_progress = process_conf.getProgress();
		if (transcode_progress == null) {
			transcode_progress = new TranscodeProgressFFmpeg();
		}
		transcode_progress.init(progress_file, progression, context_publish).startWatching();
		
		this.process = process_conf.prepareExecprocess("tmp-" + progression.getJobKey());
		Loggers.Transcode.debug("process_conf: " + process_conf);
		
		Loggers.Transcode.info("Publish transcode " + context_publish.contextToJson().toString() + " " + process.getCommandline());
		
		process.run();
		
		transcode_progress.stopWatching();
		progress_file.delete();
		
		Loggers.Transcode.debug("Publish end transcoding " + context_publish.contextToJson().toString());
		
		if (stop) {
			return;
		}
		
		if (process.getExitvalue() != 0) {
			if (process_conf.getEvent() != null) {
				throw new IOException("Bad ffmpeg execution: " + process_conf.getEvent().getLast_message());
			}
			throw new IOException("Bad ffmpeg execution");
		}
		
		progression.updateStep(3, 5);
		progression.update("Finalisation du traitement");
		
		Loggers.Transcode.debug("Publish fast start file " + context_publish.contextToJson().toString());
		
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
		
		Loggers.Transcode.debug("Publish upload media to end dest " + context_publish.contextToJson().toString());
		
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
		Loggers.Transcode.debug("Publish done " + context_publish.contextToJson().toString());
	}
	
	public synchronized void forceStopProcess() throws Exception {
		stop = true;
		if (transcode_progress != null) {
			transcode_progress.stopWatching();
		}
		if (process != null) {
			process.kill();
		}
	}
	
	/**
	 * @param dest_file will be deleted at the end.
	 * @throws Exception
	 */
	public static void faststartFile(File source_file, File dest_file) throws Exception {
		ArrayList<String> param = new ArrayList<String>();
		param.add(source_file.getPath());
		param.add(dest_file.getPath());
		
		LinkedHashMap<String, Object> log = new LinkedHashMap<String, Object>();
		log.put("source_file", source_file);
		log.put("dest_file", dest_file);
		Loggers.Transcode.debug("Faststart file: " + log);
		
		ExecprocessGettext process = new ExecprocessGettext(ExecBinaryPath.get("qtfaststart"), param);
		process.setEndlinewidthnewline(true);
		process.start();
		
		FileUtils.forceDelete(source_file);
		
		Loggers.Transcode.debug("Fast start file done: " + process.getResultstdout());
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
