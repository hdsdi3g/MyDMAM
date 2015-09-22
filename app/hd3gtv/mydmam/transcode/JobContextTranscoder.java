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

import com.google.gson.JsonObject;

import hd3gtv.mydmam.manager.JobContext;
import hd3gtv.tools.Timecode;

public class JobContextTranscoder extends JobContext implements ProgressForJobContextFFmpegBased {
	
	public String source_pathindex_key;
	public String dest_storage_name;
	
	public String dest_file_prefix;
	public String dest_file_suffix;
	public String dest_sub_directory;
	
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
	
}
