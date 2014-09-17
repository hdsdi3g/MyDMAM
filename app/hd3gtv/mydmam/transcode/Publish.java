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

import hd3gtv.configuration.Configuration;
import hd3gtv.javasimpleservice.ServiceManager;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.taskqueue.Broker;
import hd3gtv.mydmam.taskqueue.Job;
import hd3gtv.mydmam.taskqueue.Profile;
import hd3gtv.mydmam.taskqueue.Worker;
import hd3gtv.storage.AbstractFile;
import hd3gtv.storage.StorageManager;
import hd3gtv.tools.Execprocess;
import hd3gtv.tools.ExecprocessGettext;
import hd3gtv.tools.Timecode;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

@SuppressWarnings("unchecked")
public class Publish extends Worker {
	
	public static final Profile PROFILE_FFMPEG_VOD_LIVE = new Profile("ffmpeg", "ffmpeg_vod_live");
	
	private String workername;
	private long maxpresencewaittime;
	private File sourcelocalfiles;
	private String deststorage;
	private Execprocess process;
	private FFmpegProgress progress;
	private File templocaldir;
	private boolean stop;
	
	public static void createPublishTask(String mediaid, Timecode duration, String program_name, boolean islive, Class<?> referer) throws ConnectionException {
		JSONObject jo = new JSONObject();
		jo.put("id", mediaid);
		jo.put("duration", duration.toString());
		
		if (islive) {
			Broker.publishTask(program_name, PROFILE_FFMPEG_VOD_LIVE, jo, referer, true, System.currentTimeMillis() + (long) (2 * 3600 * 1000), null, false);
		} else {
			Log2.log.error("No publish tasks for no live case", null);
		}
	}
	
	public Publish() throws FileNotFoundException {
		if (Configuration.global.isElementExists("transcoding_probe") == false) {
			return;
		}
		
		workername = ServiceManager.getInstancename(false);
		
		maxpresencewaittime = Configuration.global.getValue("transcoding_probe", "maxpresencewaittime", 300l) * 1000l;
		
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
			StorageManager.getGlobalStorage().testIOForStorages(deststorage);
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
		Log2.log.info("Init Publish", dump);
	}
	
	public void process(Job job) throws Exception {
		stop = false;
		
		String mediaid;
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
		
		job.last_message = "Attente de la présence du media...";
		job.step_count = 6;
		
		/**
		 * Get ID
		 */
		mediaid = (String) job.getContext().get("id");
		
		/**
		 * Wait & with cancel if too long time.
		 */
		source_file = new File(sourcelocalfiles.getPath() + File.separator + mediaid.toUpperCase() + ".mov");
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
			job.last_message = "Fichier introuvable dans le stockage.";
			throw new FileNotFoundException(source_file.getPath());
		}
		
		if (stop) {
			return;
		}
		job.step = 1;
		job.last_message = "Media en cours de copie, attente de sa disponiblité pour son traitement...";
		
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
				Thread.sleep(1000);
			} else {
				Thread.sleep(1000);
				if (source_file.length() == old_size) {
					break;
				} else {
					old_size = source_file.length();
					Thread.sleep(1000);
				}
			}
		}
		
		if (stop) {
			return;
		}
		job.step = 2;
		job.last_message = "Conversion ffmpeg";
		
		String temp_key = job.getKey().substring(8, 16);
		dest_file_ffmpeg = new File(templocaldir.getPath() + File.separator + mediaid.toUpperCase() + "-" + temp_key + ".mp4");
		
		profile = TranscodeProfile.getTranscodeProfile(job.getProfile());
		
		File progress_file = new File(dest_file_ffmpeg.getPath() + "-progress.txt");
		progress_file.delete();
		
		progress = new FFmpegProgress(progress_file, job, new Timecode((String) job.getContext().get("duration"), 25));
		progress.start();
		
		FFmpegEvents events = new FFmpegEvents(temp_key);
		this.process = profile.prepareExecprocess(Configuration.global.getValue("transcoding", "ffmpeg_bin", "ffmpeg"), events, source_file, dest_file_ffmpeg, progress_file);
		
		Log2Dump dump = new Log2Dump();
		dump.add("job", job.getKey());
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
		
		job.step = 3;
		job.last_message = "Finalisation du traitement";
		
		/**
		 * qt-faststart convert
		 */
		dest_file_qtfs = new File(templocaldir.getPath() + File.separator + mediaid.toUpperCase() + "-" + temp_key + "-faststart" + ".f4v");
		faststartFile(dest_file_ffmpeg, dest_file_qtfs);
		
		dest_file_ffmpeg.delete();
		
		if (stop) {
			return;
		}
		job.step = 4;
		job.last_message = "Publication du média";
		
		/**
		 * End storage move
		 */
		bis = new BufferedInputStream(new FileInputStream(dest_file_qtfs), 0xFFFF);
		dest_dir = StorageManager.getGlobalStorage().getRootPath(deststorage);
		dest_file = dest_dir.getAbstractFile("/" + mediaid.toUpperCase() + ".f4v");
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
		
		job.step = 5;
		job.last_message = "Publication terminée";
		job.progress = 1;
		job.progress_size = 1;
	}
	
	public String getShortWorkerName() {
		return "publish";
	}
	
	public String getLongWorkerName() {
		return "Media transcoding and publishing";
	}
	
	public List<Profile> getManagedProfiles() {
		ArrayList<Profile> profiles = new ArrayList<Profile>();
		profiles.add(PROFILE_FFMPEG_VOD_LIVE);
		return profiles;
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
	
	public boolean isConfigurationAllowToEnabled() {
		return Configuration.global.isElementExists("transcoding_probe");
	}
	
	public static void faststartFile(File source_file, File dest_file) throws Exception {
		ArrayList<String> param = new ArrayList<String>();
		param.add(source_file.getPath());
		param.add(dest_file.getPath());
		
		Log2Dump dump = new Log2Dump();
		dump.add("source_file", source_file);
		dump.add("dest_file", dest_file);
		Log2.log.debug("Fast start", dump);
		
		ExecprocessGettext process = new ExecprocessGettext(Configuration.global.getValue("transcoding", "qtfaststart_bin", "qt-faststart"), param);
		process.setEndlinewidthnewline(true);
		process.start();
		
		dump.add("result", process.getResultstdout());
		Log2.log.info("Fast start done", dump);
		source_file.delete();
	}
}
