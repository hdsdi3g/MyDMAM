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
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
*/
package hd3gtv.mydmam.bcastautomation;

import java.util.HashMap;

import com.google.gson.JsonObject;

import hd3gtv.configuration.ConfigurationItem;
import hd3gtv.tools.Timecode;

public abstract class BCAAutomationEvent {
	
	public abstract long getStartDate();
	
	public abstract String getName();
	
	public abstract String getAutomationId();
	
	public abstract String getFileId();
	
	public abstract boolean isRecording();
	
	public abstract String getVideoSource();
	
	public abstract Timecode getDuration();
	
	public abstract boolean isAutomationPaused();
	
	public abstract Timecode getSOM();
	
	public abstract String getComment();
	
	public abstract String getChannel();
	
	public abstract JsonObject getOtherProperties(HashMap<String, ConfigurationItem> import_other_properties_configuration);
	
	public abstract String getMaterialType();
	
	public abstract String getAutomationType();
	
	/**
	 * @param import_other_properties_configuration can to be null
	 */
	public final JsonObject serialize(HashMap<String, ConfigurationItem> import_other_properties_configuration) {
		JsonObject jo = new JsonObject();
		long start_date = getStartDate();
		Timecode duration = getDuration();
		
		jo.addProperty("startdate", start_date);
		jo.addProperty("name", getName());
		jo.addProperty("automation_id", getAutomationId());
		jo.addProperty("file_id", getFileId());
		jo.addProperty("recording", isRecording());
		jo.addProperty("video_source", getVideoSource());
		jo.addProperty("duration", duration.toString());
		jo.addProperty("enddate", start_date + (long) (duration.getValue() * 1000f));
		jo.addProperty("automation_paused", isAutomationPaused());
		jo.addProperty("som", getSOM().toString());
		jo.addProperty("comment", getComment());
		jo.addProperty("channel", getChannel());
		if (import_other_properties_configuration != null) {
			jo.add("other", getOtherProperties(import_other_properties_configuration));
		}
		jo.addProperty("material_type", getMaterialType());
		jo.addProperty("automation_type", getAutomationType());
		return jo;
	}
	
}
