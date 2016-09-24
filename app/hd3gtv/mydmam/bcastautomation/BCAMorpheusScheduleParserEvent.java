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
package hd3gtv.mydmam.bcastautomation;

import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.JsonObject;

import hd3gtv.tools.Timecode;

/**
 * Store raw Playslist/Asrun event
 */
class BCAMorpheusScheduleParserEvent {
	
	BCAMorpheus bca;
	
	Properties fields;
	
	int uid;
	String fullyqualifiedtype;
	
	int previousuid;
	int owneruid;
	boolean isfixed;
	String eventkind;
	boolean isupstreamevent;
	boolean ismediaball;
	boolean isbackupmixerevent;
	boolean isguardevent;
	boolean isupstreamguardevent;
	long notionalstarttime;
	String notionalduration;
	
	HashMap<Integer, BCAMorpheusScheduleParserEvent> events;
	
	BCAMorpheusScheduleParserEvent(int uid, String fullyqualifiedtype, BCAMorpheus bca) {
		this.uid = uid;
		this.fullyqualifiedtype = fullyqualifiedtype;
		fields = new Properties();
		this.bca = bca;
	}
	
	BCAAutomationEvent getBCAEvent(BCAMorpheusScheduleParser parser) {
		return new BCAEvent(parser);
	}
	
	private class BCAEvent extends BCAAutomationEvent {
		String channel;
		
		private BCAEvent(BCAMorpheusScheduleParser parser) {
			channel = parser.getChannel();
			if (events == null) {
				events = new HashMap<>(1);
			}
		}
		
		public long getStartDate() {
			return notionalstarttime;
		}
		
		public String getName() {
			return fields.getProperty("EventName", "");
		}
		
		String automationid;
		
		public String getAutomationId() {
			if (automationid == null) {
				StringBuilder result = new StringBuilder();
				events.values().forEach(sub_event -> {
					if (sub_event.fields.getProperty("MaterialId") != null && result.length() == 0) {
						result.append(sub_event.fields.getProperty("MaterialId"));
					}
				});
				if (result.length() == 0) {
					automationid = "~" + getFileId();
				}
				automationid = result.toString();
			}
			return automationid;
		}
		
		String fileid;
		
		public String getFileId() {
			if (fileid == null) {
				StringBuilder result = new StringBuilder();
				events.values().forEach(sub_event -> {
					if (sub_event.fields.getProperty("FileId") != null && result.length() == 0) {
						result.append(sub_event.fields.getProperty("FileId"));
					}
				});
				fileid = result.toString();
			}
			
			return fileid;
		}
		
		Boolean is_recording;
		
		public boolean isRecording() {
			if (is_recording == null) {
				AtomicBoolean result = new AtomicBoolean(false);
				events.values().forEach(sub_event -> {
					if (sub_event.eventkind.equalsIgnoreCase("RecordEvent")) {
						result.set(true);
					}
				});
				is_recording = result.get();
			}
			return is_recording;
		}
		
		public String getVideoSource() {
			return fields.getProperty("VideoSource", "");
		}
		
		public Timecode getDuration() {
			return new Timecode(notionalduration, 25);
		}
		
		public boolean isAutomationPaused() {
			if (fields.containsKey("HoldFlag")) {
				return fields.getProperty("HoldFlag").equals("True");
			}
			return false;
		}
		
		Timecode som;
		
		public Timecode getSOM() {
			if (som == null) {
				StringBuilder result = new StringBuilder();
				events.values().forEach(sub_event -> {
					if (sub_event.fields.getProperty("InPoint") != null && result.length() == 0) {
						result.append(sub_event.fields.getProperty("InPoint"));
					}
				});
				if (result.length() == 0) {
					som = new Timecode(0, 25);
				} else {
					som = new Timecode(result.toString(), 25);
				}
			}
			return som;
		}
		
		public String getComment() {
			return fields.getProperty("Notes", "");
		}
		
		public String getChannel() {
			return channel;
		}
		
		JsonObject otherproperties;
		
		public JsonObject getOtherProperties() {
			if (otherproperties == null) {
				otherproperties = new JsonObject();
				otherproperties.addProperty("mppid", fields.getProperty("MultipartProgrammeId", ""));
				
				StringBuilder result = new StringBuilder();
				events.values().forEach(sub_event -> {
					if (sub_event.fields.getProperty("PlayPage") != null && result.length() == 0) {
						result.append(sub_event.fields.getProperty("PlayPage"));
					}
				});
				otherproperties.addProperty("playpage", result.toString());
			}
			
			return otherproperties;
		}
		
		public String getMaterialType() {
			return fields.getProperty("EventMaterialType", "");
		}
		
		public String getAutomationType() {
			return "Morpheus";
		}
	}
	
}
