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
import java.util.List;

import org.apache.commons.io.FileUtils;

import hd3gtv.configuration.Configuration;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
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
import hd3gtv.mydmam.useraction.fileoperation.CopyMove;
import hd3gtv.tools.Execprocess;

public class TranscoderWorker extends WorkerNG {
	
	public static void declareTranscoders(AppManager manager) throws NullPointerException, IOException {
		if (TranscodeProfile.isConfigured() == false) {
			return;
		}
		
		// TODO n Transcoding workers
		// TODO if configured, map profiles and transcoder count, else:
		
		List<TranscodeProfile> all_profiles = TranscodeProfile.getAllTranscodeProfiles();
		manager.workerRegister(new TranscoderWorker(all_profiles, new File(Configuration.global.getValue("transcoding", "temp_directory", System.getProperty("java.io.tmpdir")))));
	}
	
	private List<WorkerCapablities> capabilities;
	private Explorer explorer;
	private File temp_directory;
	
	public TranscoderWorker(final List<TranscodeProfile> profiles, File temp_directory) throws NullPointerException, IOException {
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
		
		capabilities = new ArrayList<WorkerCapablities>(profiles.size());
		capabilities.add(new WorkerCapablities() {
			
			@Override
			public List<String> getStoragesAvaliable() {
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
		if (process != null) {
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
			transcode_profile = TranscodeProfile.getTranscodeProfile(profiles_to_transcode.get(pos));
			
			temp_output_file = new File(temp_directory.getAbsolutePath() + File.separator + transcode_context.source_pathindex_key + "_" + (pos + 1) + transcode_profile.getExtension(""));
			
			process_configuration = transcode_profile.createProcessConfiguration(physical_source, temp_output_file);
			
			if (process_configuration.wantAProgressFile()) {
				progress_file = new File(temp_directory.getAbsolutePath() + File.separator + transcode_context.source_pathindex_key + "_" + (pos + 1) + "progress.txt");
				
				tprogress = process_configuration.getProgress();
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
			process = process_configuration.setProgressFile(progress_file).prepareExecprocess(progression.getJobKey());
			
			progression.update("Transcode source file with " + transcode_profile.getName() + " (" + transcode_profile.getExecutable().getName() + ")");
			
			Log2Dump dump = new Log2Dump();
			dump.add("physical_source", physical_source);
			dump.add("profile", transcode_profile.getName());
			dump.add("temp_output_file", temp_output_file);
			Log2.log.info("Transcode file", dump);
			
			process.run();
			
			if (tprogress != null) {
				tprogress.stopWatching();
			}
			if (progress_file != null) {
				if (progress_file.exists()) {
					FileUtils.forceDelete(progress_file);
				}
			}
			
			if (process.getExitvalue() != 0) {
				if (process_configuration.getEvent() != null) {
					throw new IOException("Bad transcoder execution: " + process_configuration.getEvent().getLast_message());
				}
				throw new IOException("Bad transcoder execution");
			}
			
			process = null;
			
			if (stop_process) {
				return;
			}
			
			if (transcode_profile.getOutputformat().isFaststarted()) {
				progression.update("Faststart transcoded file");
				File fast_started_file = new File(temp_output_file.getAbsolutePath() + "-faststart" + transcode_profile.getExtension(""));
				
				dump = new Log2Dump();
				dump.add("temp_output_file", temp_output_file);
				dump.add("fast_started_file", fast_started_file);
				Log2.log.info("Faststart file", dump);
				
				Publish.faststartFile(temp_output_file, fast_started_file);
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
				dump = new Log2Dump();
				dump.add("temp_output_file", temp_output_file);
				dump.add("dest_file", dest_file);
				Log2.log.debug("Move transcoded file to destination", dump);
				
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
				
				dump = new Log2Dump();
				dump.add("temp_output_file", temp_output_file);
				dump.add("storage_dest", transcode_context.dest_storage_name);
				dump.add("full_dest_dir", full_dest_dir.toString());
				Log2.log.debug("Move transcoded file to destination", dump);
				
				FileUtils.copyFile(temp_output_file, distant_file.getOutputStream(0xFFFF));
				
				root_path.close();
				FileUtils.forceDelete(temp_output_file);
			}
		}
		
		if (stop_process) {
			return;
		}
		
		if (download_temp) {
			try {
				FileUtils.forceDelete(physical_source);
			} catch (Exception e) {
				throw new IOException("Can't delete temp file", e);
			}
		}
		
	}
}
