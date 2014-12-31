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
package hd3gtv.mydmam.transcode;

import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;
import hd3gtv.mydmam.manager.JobContext;
import hd3gtv.tools.Timecode;

import com.google.gson.JsonObject;

public class JobContextPublishing extends JobContext implements Log2Dumpable {
	
	String mediaid;
	Timecode duration;
	String program_name;
	
	float performance_fps;
	int frame;
	int dup_frames;
	int drop_frames;
	
	JobContextPublishing(String mediaid, Timecode duration, String program_name) {
		this.mediaid = mediaid;
		this.duration = duration;
		this.program_name = program_name;
		performance_fps = 0;
		frame = -1;
		dup_frames = 0;
		drop_frames = 0;
	}
	
	/**
	 * Only for (de)serialization.
	 */
	JobContextPublishing() {
	}
	
	public JsonObject contextToJson() {
		JsonObject json = new JsonObject();
		json.addProperty("mediaid", mediaid);
		json.addProperty("duration", duration.toString());
		json.addProperty("program_name", program_name);
		
		if (frame > -1) {
			JsonObject jo_progress = new JsonObject();
			jo_progress.addProperty("performance_fps", performance_fps);
			jo_progress.addProperty("frame", frame);
			jo_progress.addProperty("dup_frames", dup_frames);
			jo_progress.addProperty("drop_frames", drop_frames);
			json.add("progress", jo_progress);
		}
		
		return json;
	}
	
	public void contextFromJson(JsonObject json_object) {
		mediaid = json_object.get("mediaid").getAsString();
		duration = new Timecode(json_object.get("duration").getAsString(), 25);
		program_name = json_object.get("program_name").getAsString();
		
		if (json_object.has("progress")) {
			JsonObject jo_progress = json_object.get("progress").getAsJsonObject();
			performance_fps = jo_progress.get("performance_fps").getAsFloat();
			frame = jo_progress.get("frame").getAsInt();
			dup_frames = jo_progress.get("dup_frames").getAsInt();
			drop_frames = jo_progress.get("drop_frames").getAsInt();
		}
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("mediaid", mediaid);
		dump.add("duration", duration);
		dump.add("program_name", program_name);
		dump.add("performance_fps", performance_fps);
		dump.add("frame", frame);
		dump.add("dup_frames", dup_frames);
		dump.add("drop_frames", drop_frames);
		return dump;
	}
	
}
