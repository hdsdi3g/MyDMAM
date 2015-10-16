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
 * Copyright (C) hdsdi3g for hd3g.tv 2015
 * 
*/
package hd3gtv.mydmam.transcode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.manager.AppManager;
import hd3gtv.mydmam.manager.JobContext;
import hd3gtv.mydmam.manager.JobProgression;
import hd3gtv.mydmam.manager.WorkerCapablities;
import hd3gtv.mydmam.manager.WorkerNG;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.storage.AbstractFile;
import hd3gtv.mydmam.storage.Storage;
import hd3gtv.mydmam.transcode.TranscodeProfile.ProcessConfiguration;
import hd3gtv.tools.CopyMove;
import hd3gtv.tools.Execprocess;

public class TranscoderWorker extends WorkerNG {
	
	@SuppressWarnings("unchecked")
	public static void declareTranscoders(AppManager manager) throws NullPointerException, IOException {
		if (TranscodeProfile.isConfigured() == false) {
			return;
		}
		if (Configuration.global.isElementExists("transcodingworkers") == false) {
			return;
		}
		
		File temp_dir = new File(Configuration.global.getValue("transcodingworkers", "temp_directory", System.getProperty("java.io.tmpdir")));
		Loggers.Transcode.debug("Init Transcoder workers with this tmp dir: " + temp_dir);
		
		FileUtils.forceMkdir(temp_dir);
		
		List<LinkedHashMap<String, ?>> transc_conf = Configuration.global.getListMapValues("transcodingworkers", "instances");
		
		for (int pos_instance = 0; pos_instance < transc_conf.size(); pos_instance++) {
			int count = 1;
			if (transc_conf.get(pos_instance).containsKey("count")) {
				count = (Integer) transc_conf.get(pos_instance).get("count");
			}
			
			Object raw_profile = transc_conf.get(pos_instance).get("profiles");
			
			TranscodeProfile profile;
			TranscoderWorker transcoderworker;
			if (raw_profile instanceof String) {
				profile = TranscodeProfile.getTranscodeProfile((String) raw_profile);
				if (profile == null) {
					throw new IOException("Can't found profile \"" + (String) raw_profile + "\"");
				}
				for (int pos_count = 0; pos_count < count; pos_count++) {
					Loggers.Transcode.trace("Create transcoder worker for " + profile);
					transcoderworker = new TranscoderWorker(Arrays.asList(profile), temp_dir);
					manager.workerRegister(transcoderworker);
				}
			} else if (raw_profile instanceof ArrayList<?>) {
				ArrayList<String> profiles_name = (ArrayList<String>) raw_profile;
				ArrayList<TranscodeProfile> profiles = new ArrayList<TranscodeProfile>(profiles_name.size());
				
				for (int pos_profile_name = 0; pos_profile_name < profiles_name.size(); pos_profile_name++) {
					profile = TranscodeProfile.getTranscodeProfile(profiles_name.get(pos_profile_name));
					if (profile == null) {
						throw new IOException("Can't found profile \"" + (String) raw_profile + "\"");
					}
					profiles.add(profile);
				}
				for (int pos_count = 0; pos_count < count; pos_count++) {
					Loggers.Transcode.trace("Create transcoder worker for " + profiles);
					transcoderworker = new TranscoderWorker(profiles, temp_dir);
					manager.workerRegister(transcoderworker);
				}
			}
		}
	}
	
	private List<WorkerCapablities> capabilities;
	private Explorer explorer;
	private File temp_directory;
	
	private TranscoderWorker(final List<TranscodeProfile> profiles, File temp_directory) throws NullPointerException, IOException {
		explorer = new Explorer();
		if (profiles == null) {
			throw new NullPointerException("\"profiles\" can't to be null");
		}
		if (profiles.isEmpty()) {
			throw new NullPointerException("\"profiles\" can't to be empty");
		}
		
		this.temp_directory = temp_directory;
		if (temp_directory == null) {
			throw new NullPointerException("\"temp_directory\" can't to be null");
		}
		CopyMove.checkExistsCanRead(temp_directory);
		CopyMove.checkIsDirectory(temp_directory);
		CopyMove.checkIsWritable(temp_directory);
		
		Loggers.Transcode.trace("New transcoder temp_directory " + temp_directory);
		
		capabilities = new ArrayList<WorkerCapablities>(profiles.size());
		capabilities.add(new WorkerCapablities() {
			
			public List<String> getStoragesAvaliable() {
				Loggers.Transcode.trace("New transcoder storages " + Storage.getAllStoragesNames());
				return Storage.getAllStoragesNames();
			}
			
			public Class<? extends JobContext> getJobContextClass() {
				return JobContextTranscoder.class;
			}
			
			public List<String> getHookedNames() {
				ArrayList<String> names = new ArrayList<String>();
				for (int pos = 0; pos < profiles.size(); pos++) {
					names.add(profiles.get(pos).getName());
				}
				Loggers.Transcode.trace("New transcoder hooked names / profiles " + names);
				return names;
			}
		});
	}
	
	public WorkerCategory getWorkerCategory() {
		return WorkerCategory.INTERNAL;
	}
	
	public String getWorkerLongName() {
		return "Media transcoding processor";
	}
	
	public String getWorkerVendorName() {
		return "MyDMAM Internal";
	}
	
	private boolean stop_process;
	
	protected synchronized void forceStopProcess() throws Exception {
		stop_process = true;
		Loggers.Transcode.debug("Wan't to stop process " + process.getCommandline());
		
		if (process != null) {
			Loggers.Transcode.warn("Wan't to kill process " + process.getCommandline());
			process.kill();
		}
	}
	
	protected boolean isActivated() {
		return TranscodeProfile.isConfigured();
	}
	
	public List<WorkerCapablities> getWorkerCapablities() {
		return capabilities;
	}
	
	private Execprocess process;
	
	protected void workerProcessJob(JobProgression progression, JobContext context) throws Exception {
		JobContextTranscoder transcode_context = (JobContextTranscoder) context;
		
		Loggers.Transcode.debug("Recover source file from local or distant storage " + transcode_context.contextToJson().toString());
		
		/**
		 * Recover source file from local or distant storage.
		 */
		SourcePathIndexerElement pi_item = explorer.getelementByIdkey(transcode_context.source_pathindex_key);
		if (pi_item == null) {
			throw new NullPointerException("Can't found source file in index");
		}
		
		if (stop_process) {
			return;
		}
		
		Loggers.Transcode.debug("Get physical_source from storage " + transcode_context.contextToJson().toString());
		File physical_source = Storage.getLocalFile(pi_item);
		boolean download_temp = false;
		if (physical_source == null) {
			download_temp = true;
			try {
				physical_source = Storage.getDistantFile(pi_item, temp_directory);
			} catch (IOException e) {
				throw new IOException("Can't download found file to temp directory", e);
			}
		}
		
		if (stop_process) {
			return;
		}
		
		// Container container = ContainerOperations.getByPathIndexId(transcode_context.source_pathindex_key);
		
		final File local_dest_dir = Storage.getLocalFile(SourcePathIndexerElement.prepareStorageElement(transcode_context.dest_storage_name));
		
		Loggers.Transcode.debug("physical_source is " + physical_source.getPath());
		Loggers.Transcode.debug("local_dest_dir is " + local_dest_dir.getPath());
		
		List<String> profiles_to_transcode = transcode_context.hookednames;
		
		TranscodeProfile transcode_profile;
		ProcessConfiguration process_configuration;
		TranscodeProgress tprogress;
		File temp_output_file;
		File progress_file = null;
		
		for (int pos = 0; pos < profiles_to_transcode.size(); pos++) {
			if (stop_process) {
				process = null;
				return;
			}
			if (profiles_to_transcode.size() > 1) {
				Loggers.Transcode.debug("Transcode step: " + (pos + 1) + "/" + profiles_to_transcode.size());
			}
			
			transcode_profile = TranscodeProfile.getTranscodeProfile(profiles_to_transcode.get(pos));
			Loggers.Transcode.debug("Get transcode_profile: " + transcode_profile.getName());
			
			temp_output_file = new File(temp_directory.getAbsolutePath() + File.separator + transcode_context.source_pathindex_key + "_" + (pos + 1) + transcode_profile.getExtension(""));
			Loggers.Transcode.debug("Get temp_output_file: " + temp_output_file.getPath());
			
			process_configuration = transcode_profile.createProcessConfiguration(physical_source, temp_output_file);
			if (Loggers.Transcode.isDebugEnabled()) {
				Loggers.Transcode.debug("process_configuration: " + process_configuration);
			}
			
			if (process_configuration.wantAProgressFile()) {
				progress_file = new File(temp_directory.getAbsolutePath() + File.separator + transcode_context.source_pathindex_key + "_" + (pos + 1) + "progress.txt");
				
				tprogress = process_configuration.getProgress();
				Loggers.Transcode.debug("Process configuration want a progress: " + tprogress.getClass().getName());
				tprogress.init(progress_file, progression, context);
				tprogress.startWatching();
			} else {
				progress_file = null;
				tprogress = null;
			}
			
			progression.updateStep(pos + 1, profiles_to_transcode.size());
			
			/**
			 * @see FFmpegLowresRenderer, if it's a video file
			 *      process_conf.getParamTags().put("FILTERS", sb_filters.toString());
			 */
			Loggers.Transcode.debug("Prepare prepareExecprocess for process_configuration");
			process = process_configuration.setProgressFile(progress_file).prepareExecprocess(progression.getJobKey());
			
			progression.update("Transcode source file with " + transcode_profile.getName() + " (" + transcode_profile.getExecutable().getName() + ")");
			
			LinkedHashMap<String, Object> log = new LinkedHashMap<String, Object>();
			log.put("physical_source", physical_source);
			log.put("profile", transcode_profile.getName());
			log.put("temp_output_file", temp_output_file);
			Loggers.Transcode.info("Transcode file " + log.toString());
			
			process.run();
			
			Loggers.Transcode.debug("Transcoding is ended");
			
			if (tprogress != null) {
				tprogress.stopWatching();
			}
			if (progress_file != null) {
				if (progress_file.exists()) {
					Loggers.Transcode.debug("Progress file exists, remove it: " + progress_file.getPath());
					FileUtils.forceDelete(progress_file);
				}
			}
			
			if (process.getExitvalue() != 0) {
				if (process_configuration.getEvent() != null) {
					throw new IOException("Bad transcoder execution: " + process_configuration.getEvent().getLast_message());
				}
				throw new IOException("Bad transcoder execution (exit value is: " + process.getExitvalue() + ")");
			}
			
			process = null;
			
			if (stop_process) {
				return;
			}
			
			if (transcode_profile.getOutputformat().isFaststarted()) {
				progression.update("Faststart transcoded file");
				File fast_started_file = new File(temp_output_file.getAbsolutePath() + "-faststart" + transcode_profile.getExtension(""));
				
				log = new LinkedHashMap<String, Object>();
				log.put("temp_output_file", temp_output_file);
				log.put("fast_started_file", fast_started_file);
				Loggers.Transcode.info("Faststart file " + log);
				
				Publish.faststartFile(temp_output_file, fast_started_file);
				Loggers.Transcode.debug("Delete temp_output_file " + temp_output_file);
				FileUtils.forceDelete(temp_output_file);
				temp_output_file = fast_started_file;
			}
			
			if (stop_process) {
				return;
			}
			
			if (transcode_context.dest_file_prefix == null) {
				transcode_context.dest_file_prefix = "";
			}
			if (transcode_context.dest_file_suffix == null) {
				transcode_context.dest_file_suffix = "";
			}
			
			progression.update("Move transcoded file to destination");
			
			if (local_dest_dir != null) {
				File local_full_dest_dir = local_dest_dir.getAbsoluteFile();
				if (transcode_context.dest_sub_directory != null) {
					File dir_to_create = new File(local_full_dest_dir.getAbsolutePath() + transcode_context.dest_sub_directory);
					Loggers.Transcode.debug("Force mkdir " + dir_to_create);
					
					FileUtils.forceMkdir(dir_to_create);
					local_full_dest_dir = dir_to_create;
				}
				
				StringBuilder full_file = new StringBuilder();
				full_file.append(local_full_dest_dir.getPath());
				full_file.append(File.separator);
				full_file.append(transcode_context.dest_file_prefix);
				full_file.append(physical_source.getName());
				full_file.append(transcode_context.dest_file_suffix);
				full_file.append(transcode_profile.getExtension(""));
				
				File dest_file = new File(full_file.toString());
				log = new LinkedHashMap<String, Object>();
				log.put("temp_output_file", temp_output_file);
				log.put("dest_file", dest_file);
				Loggers.Transcode.debug("Move transcoded file to destination " + log);
				
				FileUtils.moveFile(temp_output_file, dest_file);
			} else {
				AbstractFile root_path = Storage.getByName(transcode_context.dest_storage_name).getRootPath();
				
				StringBuilder full_dest_dir = new StringBuilder();
				if (transcode_context.dest_sub_directory != null) {
					String[] dirs_to_create = transcode_context.dest_sub_directory.split("/");
					for (int pos_dtc = 0; pos_dtc < dirs_to_create.length; pos_dtc++) {
						full_dest_dir.append("/");
						full_dest_dir.append(dirs_to_create[pos_dtc]);
						root_path.mkdir(full_dest_dir.toString());
					}
				}
				
				full_dest_dir.append("/");
				full_dest_dir.append(transcode_context.dest_file_prefix);
				full_dest_dir.append(physical_source.getName());
				full_dest_dir.append(transcode_context.dest_file_suffix);
				full_dest_dir.append(transcode_profile.getExtension(""));
				
				AbstractFile distant_file = root_path.getAbstractFile(full_dest_dir.toString());
				
				log = new LinkedHashMap<String, Object>();
				log.put("temp_output_file", temp_output_file);
				log.put("storage_dest", transcode_context.dest_storage_name);
				log.put("full_dest_dir", full_dest_dir.toString());
				Loggers.Transcode.debug("Move transcoded file to destination " + log);
				
				FileUtils.copyFile(temp_output_file, distant_file.getOutputStream(0xFFFF));
				
				root_path.close();
				
				Loggers.Transcode.debug("Delete temp_output_file" + temp_output_file);
				FileUtils.forceDelete(temp_output_file);
			}
		}
		
		if (stop_process) {
			return;
		}
		
		if (download_temp) {
			try {
				Loggers.Transcode.debug("Delete physical_source" + physical_source);
				FileUtils.forceDelete(physical_source);
			} catch (Exception e) {
				throw new IOException("Can't delete temp file", e);
			}
		}
		
	}
}
