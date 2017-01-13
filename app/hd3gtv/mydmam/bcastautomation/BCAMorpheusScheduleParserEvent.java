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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import hd3gtv.configuration.ConfigurationItem;
import hd3gtv.tools.Timecode;

/**
 * Store raw Playslist/Asrun event
 */
class BCAMorpheusScheduleParserEvent {
	
	Properties fields;
	
	int uid;
	private String type;
	
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
	
	BCAMorpheusScheduleParserEvent(int uid, String type) {
		this.uid = uid;
		this.type = type;
		fields = new Properties();
	}
	
	BCAAutomationEvent getBCAEvent(BCAMorpheusScheduleParser parser) {
		return new BCAEvent(this, parser);
	}
	
	private class BCAEvent extends BCAAutomationEvent {
		String channel;
		private BCAMorpheusScheduleParserEvent ref;
		
		private BCAEvent(BCAMorpheusScheduleParserEvent ref, BCAMorpheusScheduleParser parser) {
			this.ref = ref;
			if (ref == null) {
				throw new NullPointerException("\"ref\" can't to be null");
			}
			
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
		
		public JsonObject getOtherProperties(HashMap<String, ConfigurationItem> import_other_properties_configuration) {
			if (import_other_properties_configuration == null) {
				return null;
			}
			if (otherproperties == null) {
				otherproperties = new JsonObject();
				
				import_other_properties_configuration.forEach((p_name, p_content_conf) -> {
					LinkedHashMap<String, ?> p_conf = p_content_conf.content;
					
					final BCAMorpheusScheduleParserEvent event;
					if (p_conf.containsKey("event_type")) {
						/**
						 * Get field parameter(s) value(s) from an event_type
						 */
						String event_type = (String) p_conf.get("event_type");
						
						if (event_type.equalsIgnoreCase(type)) {
							event = ref;
						} else {
							Optional<BCAMorpheusScheduleParserEvent> s_event = events.values().stream().filter(ev -> {
								return event_type.equalsIgnoreCase(ev.type);
							}).findFirst();
							if (s_event.isPresent()) {
								event = s_event.get();
							} else {
								event = null;
							}
						}
					} else {
						event = null;
					}
					
					if (p_conf.containsKey("fieldparameter") == false) {
						/**
						 * No fieldparameter -> we use Enabled fieldparameter by default
						 */
						if (event != null) {
							otherproperties.addProperty(p_name, event.fields.getProperty("Enabled", "True").equalsIgnoreCase("True"));
						}
						return;
					}
					
					Object o_fieldparameter = p_conf.get("fieldparameter");
					if (o_fieldparameter instanceof String) {
						String fieldparameter = (String) o_fieldparameter;
						
						if (event != null) {
							String p = event.fields.getProperty(fieldparameter);
							if (p != null) {
								if (p.isEmpty() == false) {
									otherproperties.addProperty(p_name, p);
								}
							}
						} else {
							final JsonArray ja = new JsonArray();
							events.forEach((i, ev) -> {
								String p = ev.fields.getProperty(fieldparameter);
								if (p != null) {
									if (p.isEmpty() == false) {
										ja.add(new JsonPrimitive(p));
									}
								}
							});
							if (ja.size() > 0) {
								otherproperties.add(p_name, ja);
							}
						}
						
					} else if (o_fieldparameter instanceof ArrayList<?>) {
						@SuppressWarnings("unchecked")
						ArrayList<String> fieldparameters = (ArrayList<String>) o_fieldparameter;
						
						JsonArray ja = new JsonArray();
						if (event != null) {
							fieldparameters.forEach(fp -> {
								String p = event.fields.getProperty(fp);
								if (p != null) {
									if (p.isEmpty() == false) {
										ja.add(new JsonPrimitive(p));
									}
								}
							});
						}
						if (ja.size() > 0) {
							otherproperties.add(p_name, ja);
						}
					}
				});
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
