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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.mydmam.transcode.mtdgenerator;

import hd3gtv.mydmam.metadata.JobContextRenderer;
import hd3gtv.mydmam.metadata.container.EntryRenderer;

import com.google.gson.JsonObject;

public abstract class JobContextFFmpegLowresRenderer extends JobContextRenderer {
	
	boolean faststarted;
	float source_fps;
	float source_duration;
	
	float performance_fps;
	int frame;
	int dup_frames;
	int drop_frames;
	
	public JsonObject contextToJson() {
		JsonObject json = super.contextToJson();
		json.addProperty("performance_fps", performance_fps);
		json.addProperty("frame", frame);
		json.addProperty("dup_frames", dup_frames);
		json.addProperty("drop_frames", drop_frames);
		
		json.addProperty("source_duration", source_duration);
		json.addProperty("faststarted", faststarted);
		json.addProperty("source_fps", source_fps);
		return json;
	}
	
	public void contextFromJson(JsonObject json_object) {
		super.contextFromJson(json_object);
		performance_fps = json_object.get("performance_fps").getAsFloat();
		frame = json_object.get("frame").getAsInt();
		dup_frames = json_object.get("dup_frames").getAsInt();
		drop_frames = json_object.get("drop_frames").getAsInt();
		
		source_duration = json_object.get("source_duration").getAsFloat();
		faststarted = json_object.get("faststarted").getAsBoolean();
		source_fps = json_object.get("source_fps").getAsFloat();
	}
	
	abstract String getTranscodeProfileName();
	
	abstract Class<? extends EntryRenderer> getEntryRendererClass();
	
}