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
import java.util.Arrays;
import java.util.LinkedHashMap;

import org.apache.commons.io.FileUtils;

import com.google.gson.JsonObject;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.db.ElasticsearchBulkOperation;
import hd3gtv.mydmam.manager.JobContext;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.storage.AbstractFile;
import hd3gtv.mydmam.storage.Storage;
import hd3gtv.tools.Timecode;

public class JobContextTranscoder extends JobContext implements ProgressForJobContextFFmpegBased {
	
	public String source_pathindex_key;
	public String dest_storage_name;
	
	public String dest_file_prefix;
	public String dest_file_suffix;
	public String dest_sub_directory;
	
	/**
	 * Used by ProcessingKit.
	 */
	public JsonObject options;
	
	Timecode duration;
	float performance_fps;
	int frame;
	int dup_frames;
	int drop_frames;
	
	public JsonObject contextToJson() {
		JsonObject jo = new JsonObject();
		jo.addProperty("source_pathindex_key", source_pathindex_key);
		jo.addProperty("dest_storage_name", dest_storage_name);
		
		if (dest_file_prefix != null) {
			jo.addProperty("dest_file_prefix", dest_file_prefix);
		}
		if (dest_file_suffix != null) {
			jo.addProperty("dest_file_suffix", dest_file_suffix);
		}
		if (dest_sub_directory != null) {
			jo.addProperty("dest_sub_directory", dest_sub_directory);
		}
		
		if (duration != null) {
			jo.addProperty("duration", duration.toString());
			jo.addProperty("source_fps", duration.getFps());
		}
		if (frame > -1) {
			JsonObject jo_progress = new JsonObject();
			jo_progress.addProperty("performance_fps", performance_fps);
			jo_progress.addProperty("frame", frame);
			jo_progress.addProperty("dup_frames", dup_frames);
			jo_progress.addProperty("drop_frames", drop_frames);
			jo.add("progress", jo_progress);
		}
		if (options == null) {
			options = new JsonObject();
		}
		jo.add("options", options);
		
		return jo;
	}
	
	public void contextFromJson(JsonObject json_object) {
		source_pathindex_key = json_object.get("source_pathindex_key").getAsString();
		dest_storage_name = json_object.get("dest_storage_name").getAsString();
		
		if (json_object.has("dest_file_prefix")) {
			dest_file_prefix = json_object.get("dest_file_prefix").getAsString();
		}
		if (json_object.has("dest_file_suffix")) {
			dest_file_suffix = json_object.get("dest_file_suffix").getAsString();
		}
		if (json_object.has("dest_sub_directory")) {
			dest_sub_directory = json_object.get("dest_sub_directory").getAsString();
		}
		
		if (json_object.has("duration")) {
			if (json_object.has("source_fps")) {
				duration = new Timecode(json_object.get("duration").getAsString(), json_object.get("source_fps").getAsFloat());
			} else {
				duration = new Timecode(json_object.get("duration").getAsString(), 25);
			}
		}
		if (json_object.has("progress")) {
			JsonObject jo_progress = json_object.get("progress").getAsJsonObject();
			performance_fps = jo_progress.get("performance_fps").getAsFloat();
			frame = jo_progress.get("frame").getAsInt();
			dup_frames = jo_progress.get("dup_frames").getAsInt();
			drop_frames = jo_progress.get("drop_frames").getAsInt();
		}
		
		if (json_object.has("options")) {
			options = json_object.get("options").getAsJsonObject();
		} else {
			options = new JsonObject();
		}
		
	}
	
	public void setPerformance_fps(float performance_fps) {
		this.performance_fps = performance_fps;
	}
	
	public void setFrame(int frame) {
		this.frame = frame;
	}
	
	public void setDupFrame(int dup_frames) {
		this.dup_frames = dup_frames;
	}
	
	public void setDropFrame(int drop_frames) {
		this.drop_frames = drop_frames;
	}
	
	public Timecode getSourceDuration() {
		return this.duration;
	}
	
	public void setDuration(Timecode duration) {
		this.duration = duration;
	}
	
	private void unNullableVars() {
		if (dest_file_prefix == null) {
			dest_file_prefix = "";
		}
		if (dest_file_suffix == null) {
			dest_file_suffix = "";
		}
		if (dest_sub_directory == null) {
			dest_sub_directory = "";
		}
	}
	
	public File getLocalDestDirectory() throws IOException {
		unNullableVars();
		SourcePathIndexerElement dest_storage = SourcePathIndexerElement.prepareStorageElement(dest_storage_name);
		
		File local_dest_dir = Storage.getLocalFile(dest_storage);
		if (local_dest_dir == null) {
			return null;
		}
		File local_full_dest_dir = local_dest_dir.getAbsoluteFile();
		if (dest_sub_directory.equals("") == false) {
			File dir_to_create = new File(local_full_dest_dir.getAbsolutePath() + dest_sub_directory);
			Loggers.Transcode.debug("Force mkdir " + dir_to_create);
			
			FileUtils.forceMkdir(dir_to_create);
			local_full_dest_dir = dir_to_create;
		}
		return local_full_dest_dir;
	}
	
	/**
	 * @param dest_extension "." are not mandatory
	 */
	public void moveProcessedFileToDestDirectory(File source, String dest_file_base_name, String dest_extension) throws IOException {
		File local_full_dest_dir = getLocalDestDirectory();
		LinkedHashMap<String, Object> log = new LinkedHashMap<>();
		log.put("source", source);
		
		if (local_full_dest_dir != null) {
			/**
			 * Move to local (mounted) storage.
			 */
			StringBuilder full_file = new StringBuilder();
			full_file.append(local_full_dest_dir.getPath());
			full_file.append(File.separator);
			full_file.append(dest_file_prefix);
			full_file.append(dest_file_base_name);
			full_file.append(dest_file_suffix);
			if (dest_extension.startsWith(".") == false) {
				full_file.append(".");
			}
			full_file.append(dest_extension);
			
			File dest_file = new File(full_file.toString());
			log.put("dest_file", dest_file);
			Loggers.Transcode.debug("Move transcoded file to destination " + log);
			
			FileUtils.moveFile(source, dest_file);
		} else {
			/**
			 * Push to distant storage.
			 */
			AbstractFile root_path = Storage.getByName(dest_storage_name).getRootPath();
			
			StringBuilder full_dest_dir = new StringBuilder();
			if (dest_sub_directory.equals("") == false) {
				String[] dirs_to_create = dest_sub_directory.split("/");
				for (int pos_dtc = 0; pos_dtc < dirs_to_create.length; pos_dtc++) {
					full_dest_dir.append("/");
					full_dest_dir.append(dirs_to_create[pos_dtc]);
					root_path.mkdir(full_dest_dir.toString());
				}
			}
			
			full_dest_dir.append("/");
			full_dest_dir.append(dest_file_prefix);
			full_dest_dir.append(dest_file_base_name);
			full_dest_dir.append(dest_file_suffix);
			if (dest_extension.startsWith(".") == false) {
				full_dest_dir.append(".");
			}
			full_dest_dir.append(dest_extension);
			
			AbstractFile distant_file = root_path.getAbstractFile(full_dest_dir.toString());
			
			log.put("storage_dest", dest_storage_name);
			log.put("full_dest_dir", full_dest_dir.toString());
			Loggers.Transcode.debug("Move transcoded file to destination " + log);
			
			FileUtils.copyFile(source, distant_file.getOutputStream(0xFFFF));
			root_path.close();
			
			Loggers.Transcode.debug("Delete source" + source);
			FileUtils.forceDelete(source);
		}
		
	}
	
	public void refreshDestDirectoryPathIndex() throws Exception {
		unNullableVars();
		SourcePathIndexerElement dest_storage = SourcePathIndexerElement.prepareStorageElement(dest_storage_name);
		Loggers.Transcode.debug("Refresh dest storage index: " + dest_storage);
		
		Explorer explorer = new Explorer();
		ElasticsearchBulkOperation bulk = Elasticsearch.prepareBulk();
		explorer.refreshCurrentStoragePath(bulk, Arrays.asList(dest_storage), true);
		bulk.terminateBulk();
	}
	
}
