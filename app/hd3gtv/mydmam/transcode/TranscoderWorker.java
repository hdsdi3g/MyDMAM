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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.manager.AppManager;
import hd3gtv.mydmam.manager.JobContext;
import hd3gtv.mydmam.manager.JobProgression;
import hd3gtv.mydmam.manager.WorkerCapablities;
import hd3gtv.mydmam.manager.WorkerNG;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.ContainerOperations;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.storage.DistantFileRecovery;
import hd3gtv.mydmam.storage.Storage;
import hd3gtv.mydmam.transcode.TranscodeProfile.ProcessConfiguration;
import hd3gtv.mydmam.transcode.images.ImageMagickThumbnailer;
import hd3gtv.mydmam.transcode.watchfolder.WatchFolderDB;
import hd3gtv.tools.CopyMove;
import hd3gtv.tools.ExecBinaryPath;
import hd3gtv.tools.Execprocess;
import hd3gtv.tools.ExecprocessGettext;
import hd3gtv.tools.StoppableProcessing;

public class TranscoderWorker extends WorkerNG implements StoppableProcessing {
	
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
		
		ProcessingKitEngine process_kit_engine = new ProcessingKitEngine(manager);
		
		List<LinkedHashMap<String, ?>> transc_conf = Configuration.global.getListMapValues("transcodingworkers", "instances");
		
		for (int pos_instance = 0; pos_instance < transc_conf.size(); pos_instance++) {
			int count = 1;
			if (transc_conf.get(pos_instance).containsKey("count")) {
				count = (Integer) transc_conf.get(pos_instance).get("count");
			}
			
			Object raw_profile = transc_conf.get(pos_instance).get("profiles");
			
			TranscodeProfile profile;
			ProcessingKit process_kit;
			if (raw_profile instanceof String) {
				profile = TranscodeProfile.getTranscodeProfile((String) raw_profile);
				process_kit = null;
				if (profile == null) {
					process_kit = process_kit_engine.get((String) raw_profile);
					if (process_kit == null) {
						throw new IOException("Can't found profile \"" + (String) raw_profile + "\"");
					}
				}
				for (int pos_count = 0; pos_count < count; pos_count++) {
					Loggers.Transcode.trace("Create transcoder worker for " + raw_profile);
					
					if (profile != null) {
						manager.register(new TranscoderWorker(Arrays.asList(profile), temp_dir, new ArrayList<>(1)));
					} else if (process_kit != null) {
						manager.register(new TranscoderWorker(new ArrayList<>(1), temp_dir, Arrays.asList(process_kit)));
					}
				}
			} else if (raw_profile instanceof ArrayList<?>) {
				ArrayList<String> profiles_name = (ArrayList<String>) raw_profile;
				ArrayList<TranscodeProfile> profiles = new ArrayList<TranscodeProfile>(profiles_name.size());
				ArrayList<ProcessingKit> process_kits = new ArrayList<ProcessingKit>(profiles_name.size());
				
				for (int pos_profile_name = 0; pos_profile_name < profiles_name.size(); pos_profile_name++) {
					profile = TranscodeProfile.getTranscodeProfile(profiles_name.get(pos_profile_name));
					process_kit = null;
					if (profile == null) {
						process_kit = process_kit_engine.get(profiles_name.get(pos_profile_name));
						if (process_kit == null) {
							throw new IOException("Can't found profile \"" + profiles_name.get(pos_profile_name) + "\"");
						}
						process_kits.add(process_kit);
					} else {
						profiles.add(profile);
					}
				}
				for (int pos_count = 0; pos_count < count; pos_count++) {
					Loggers.Transcode.trace("Create transcoder worker for " + profiles + " and " + process_kits);
					manager.register(new TranscoderWorker(profiles, temp_dir, process_kits));
				}
			}
		}
		
		List<TranscodeProfile> transcode_profiles = TranscodeProfile.getAllTranscodeProfiles();
		for (int pos = 0; pos < transcode_profiles.size(); pos++) {
			manager.getInstanceStatus().registerInstanceStatusItem(transcode_profiles.get(pos));
		}
		
	}
	
	private List<WorkerCapablities> capabilities;
	private Explorer explorer;
	private File temp_directory;
	private transient HashMap<String, ProcessingKit> process_kits;
	
	private TranscoderWorker(final List<TranscodeProfile> profiles, File temp_directory, final List<ProcessingKit> process_kits) throws NullPointerException, IOException {
		explorer = new Explorer();
		if (profiles == null) {
			throw new NullPointerException("\"profiles\" can't to be null");
		}
		if (process_kits == null) {
			throw new NullPointerException("\"process_kits\" can't to be null");
		}
		if (profiles.isEmpty() && process_kits.isEmpty()) {
			throw new NullPointerException("\"profiles\" and \"process_kits\" can't to be empty");
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
				for (int pos = 0; pos < process_kits.size(); pos++) {
					names.add(process_kits.get(pos).getClass().getName());
				}
				Loggers.Transcode.trace("New transcoder hooked names / profiles / process_kits " + names);
				return names;
			}
		});
		
		this.process_kits = new HashMap<>(1);
		process_kits.forEach(v -> {
			this.process_kits.put(v.getClass().getName(), v);
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
			Loggers.Transcode.warn("Wan't to kill process " + process.getCommandline());
			process.kill();
		}
	}
	
	public boolean isWantToStopCurrentProcessing() {
		return stop_process;
	}
	
	protected boolean isActivated() {
		return TranscodeProfile.isConfigured();
	}
	
	public List<WorkerCapablities> getWorkerCapablities() {
		return capabilities;
	}
	
	private Execprocess process;
	
	protected void workerProcessJob(JobProgression progression, JobContext context) throws Exception {
		final JobContextTranscoder transcode_context = (JobContextTranscoder) context;
		
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
		if (physical_source == null) {
			physical_source = DistantFileRecovery.getFile(pi_item, true);
		}
		
		if (stop_process) {
			return;
		}
		
		Loggers.Transcode.debug("physical_source is " + physical_source.getPath());
		
		List<String> profiles_to_transcode = transcode_context.hookednames;
		
		TranscodeProfile transcode_profile;
		ProcessConfiguration process_configuration;
		TranscodeProgress tprogress;
		File temp_output_file;
		File progress_file = null;
		ProcessingKit process_kit;
		ProcessingKitInstance process_kit_instance;
		Container container = null;
		
		for (int pos = 0; pos < profiles_to_transcode.size(); pos++) {
			if (stop_process) {
				process = null;
				return;
			}
			if (profiles_to_transcode.size() > 1) {
				Loggers.Transcode.debug("Transcode step: " + (pos + 1) + "/" + profiles_to_transcode.size());
			}
			
			if (process_kits.containsKey(profiles_to_transcode.get(pos))) {
				/**
				 * ProcessingKit transcoding.
				 */
				process_kit = process_kits.get(profiles_to_transcode.get(pos));
				temp_output_file = new File(
						temp_directory.getAbsolutePath() + File.separator + transcode_context.source_pathindex_key + "_" + (pos + 1) + "_" + process_kit.getClass().getSimpleName());
				FileUtils.forceMkdir(temp_output_file);
				
				process_kit_instance = process_kit.createInstance(temp_output_file);
				process_kit_instance.setDestDirectory(transcode_context.getLocalDestDirectory());
				process_kit_instance.setJobProgression(progression);
				process_kit_instance.setTranscodeContext(transcode_context);
				process_kit_instance.setStoppable(this);
				
				if (container == null) {
					container = ContainerOperations.getByPathIndexId(transcode_context.source_pathindex_key);
				}
				
				Exception catched_error = null;
				List<File> output_files = null;
				try {
					output_files = process_kit_instance.process(physical_source, container);
				} catch (Exception e) {
					catched_error = e;
				}
				process_kit_instance.cleanTempFiles();
				if (catched_error != null) {
					if (output_files != null) {
						output_files.forEach(v -> {
							v.delete();
						});
					}
					throw catched_error;
				}
				
				FileUtils.forceDelete(temp_output_file);
				
				if (output_files != null) {
					output_files.forEach(v -> {
						try {
							transcode_context.moveProcessedFileToDestDirectory(v, v.getName(), FilenameUtils.getExtension(v.getName()));
						} catch (Exception e) {
							Loggers.Transcode.error("Can't move processed file " + v.getPath(), e);
						}
					});
				}
				
			} else {
				/**
				 * Classical transcoding: 1 file in dest, with 1 exec to start.
				 */
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
				 *      process_configuration.getParamTags().put("FILTERS", sb_filters.toString());
				 */
				process_configuration.getParamTags().put("FILTERS", "null");
				process_configuration.getParamTags().put("AUDIOMAPFILTER", "anull");
				process_configuration.getParamTags().put("ICCPROFILE", ImageMagickThumbnailer.getICCProfile().getAbsolutePath());
				
				Loggers.Transcode.debug("Prepare prepareExecprocess for process_configuration");
				process = process_configuration.prepareExecprocess(progression.getJobKey());
				
				progression.update("Transcode source file with " + transcode_profile.getName() + " (" + transcode_profile.getExecutable().getName() + ")");
				
				LinkedHashMap<String, Object> log = new LinkedHashMap<String, Object>();
				log.put("physical_source", physical_source);
				log.put("profile", transcode_profile.getName());
				log.put("temp_output_file", temp_output_file);
				// log.put("commandline", process.getCommandline());
				// log.put("cwd", process.getWorkingDirectory());
				Loggers.Transcode.debug("Transcode file " + log.toString());
				
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
						throw new IOException("Bad transcoder execution: \"" + process_configuration.getEvent().getLast_message() + "\"\t from \"" + process.getCommandline() + "\"");
					}
					throw new IOException("Bad transcoder execution (exit value is: " + process.getExitvalue() + ")");
				}
				
				process = null;
				
				if (stop_process) {
					return;
				}
				
				if (transcode_profile.getOutputformat() != null) {
					if (transcode_profile.getOutputformat().isFaststarted()) {
						progression.update("Faststart transcoded file");
						File fast_started_file = new File(temp_output_file.getAbsolutePath() + "-faststart" + transcode_profile.getExtension(""));
						
						log = new LinkedHashMap<String, Object>();
						log.put("temp_output_file", temp_output_file);
						log.put("fast_started_file", fast_started_file);
						Loggers.Transcode.info("Faststart file " + log);
						
						faststartFile(temp_output_file, fast_started_file);
						temp_output_file = fast_started_file;
					}
				}
				
				if (stop_process) {
					return;
				}
				
				progression.update("Move transcoded file to destination");
				transcode_context.moveProcessedFileToDestDirectory(temp_output_file, FilenameUtils.getBaseName(pi_item.currentpath), transcode_profile.getExtension(""));
			}
			
			if (stop_process) {
				return;
			}
			
			transcode_context.refreshDestDirectoryPathIndex();
			
			if (stop_process) {
				return;
			}
		}
	}
	
	public static class Serializer implements JsonSerializer<TranscoderWorker> {
		
		public JsonElement serialize(TranscoderWorker transcoder_worker, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject jo = new JsonObject();
			if (transcoder_worker == null) {
				return jo;
			}
			JsonObject jo_transcoder = new JsonObject();
			
			jo_transcoder.addProperty("temp_directory", transcoder_worker.temp_directory.getAbsolutePath());
			jo_transcoder.addProperty("temp_directory_freespace", transcoder_worker.temp_directory.getFreeSpace());
			
			return jo_transcoder;
		}
		
	}
	
	public JsonElement exportSpecificInstanceStatusItems() {
		return WatchFolderDB.gson.toJsonTree(this);
	}
	
	/**
	 * @param source_file will be deleted at the end.
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
	
}
