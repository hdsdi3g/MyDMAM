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

public class TranscoderWorker extends WorkerNG {
	
	public static void declareTranscoders(AppManager manager) throws NullPointerException, IOException {
		if (TranscodeProfile.isConfigured() == false) {
			return;
		}
		
		// TODO if configured, map profiles and transcoder count, else:
		
		List<TranscodeProfile> all_profiles = TranscodeProfile.getAllTranscodeProfiles();
		manager.workerRegister(new TranscoderWorker(all_profiles, new File(Configuration.global.getValue("????", "temp_directory", System.getProperty("java.io.tmpdir")))));
		// TODO get temp dir...
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
	}
	
	protected boolean isActivated() {
		return TranscodeProfile.isConfigured();
	}
	
	public List<WorkerCapablities> getWorkerCapablities() {
		return capabilities;
	}
	
	@Override
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
		
		File local_dest_dir = Storage.getLocalFile(SourcePathIndexerElement.prepareStorageElement(transcode_context.dest_storage_name));
		
		List<String> profiles_to_transcode = transcode_context.hookednames;
		
		TranscodeProfile transcode_profile;
		ProcessConfiguration process;
		File temp_output_file;
		for (int pos = 0; pos < profiles_to_transcode.size(); pos++) {
			if (stop_process) {
				return;
			}
			transcode_profile = TranscodeProfile.getTranscodeProfile(profiles_to_transcode.get(pos));
			
			temp_output_file = new File(temp_directory.getAbsolutePath() + File.separator + transcode_context.source_pathindex_key + "_" + (pos + 1) + transcode_profile.getExtension(""));
			
			process = transcode_profile.createProcessConfiguration(physical_source, temp_output_file);
			
			// TODO start transcode, with progression
			
			if (transcode_profile.getOutputformat().isFaststarted()) {
				File fast_started_file = new File(temp_output_file.getAbsolutePath() + "-faststart" + transcode_profile.getExtension(""));
				Publish.faststartFile(temp_output_file, fast_started_file);
				FileUtils.forceDelete(temp_output_file);
				temp_output_file = fast_started_file;
			}
			
			// TODO add prefix/suffix for output file + recreate sub dir
			if (local_dest_dir != null) {
				FileUtils.moveFile(temp_output_file, new File(local_dest_dir.getAbsolutePath() + File.separator + physical_source.getName() + transcode_profile.getExtension("")));
			} else if (stop_process == false) {
				AbstractFile distant_file = Storage.getByName(transcode_context.dest_storage_name).getRootPath().getAbstractFile("/" + physical_source.getName() + transcode_profile.getExtension(""));
				FileUtils.copyFile(temp_output_file, distant_file.getOutputStream(0xFFFF));
				distant_file.close();
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
