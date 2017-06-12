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
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
*/
package hd3gtv.mydmam.bcastautomation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.ConfigurationItem;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonKit;
import hd3gtv.tools.TwoListComparing;

public abstract class BCAEventCatcherHandler {
	
	private HashMap<String, ConfigurationItem> import_other_properties_configuration;
	private long max_date_to_check;
	private String property_to_catch;
	
	private File json_db_file;
	private ArrayList<BCAEventCatched> entries;
	private ArrayList<BCAAutomationEvent> actually_founded_in_playlist;
	
	public BCAEventCatcherHandler() throws JsonSyntaxException, IOException {
		import_other_properties_configuration = Configuration.getElement(Configuration.global.getElement("broadcast_automation"), "import_other_properties");
		property_to_catch = Configuration.global.getValue("broadcast_automation", "catch_property", "");
		if (property_to_catch.equals("")) {
			throw new NullPointerException("broadcast_automation.catch_property is not set");
		}
		max_date_to_check = System.currentTimeMillis() + Configuration.global.getValue("broadcast_automation", "catch_max_min_to_check", 24 * 60) * 60 * 1000;
		
		json_db_file = new File(Configuration.global.getValue("broadcast_automation", "catch_json_db_file", "catch_bca_event.json"));
		
		if (json_db_file.exists()) {
			entries = MyDMAM.gson_kit.getGson().fromJson(FileUtils.readFileToString(json_db_file, MyDMAM.UTF8), GsonKit.type_ArrayList_BCACatchEntry);
		} else {
			entries = new ArrayList<>();
		}
		
		actually_founded_in_playlist = new ArrayList<>();
	}
	
	boolean isEventCanBeCatched(BCAAutomationEvent event) {
		if (event.getStartDate() > max_date_to_check) {
			return false;
		}
		if (event.isAutomationPaused()) {
			return false;
		}
		if (event.getStartDate() < System.currentTimeMillis()) {
			return false;
		}
		
		JsonObject other_properties = event.getOtherProperties(import_other_properties_configuration);
		if (other_properties == null) {
			return false;
		}
		if (other_properties.has(property_to_catch) == false) {
			return false;
		}
		return true;
	}
	
	/**
	 * @param event was tested true by isEventCanBeCatched()
	 */
	void onCatchEvent(BCAAutomationEvent event) {
		actually_founded_in_playlist.add(event);
	}
	
	void onBeforeCatchEvents() {
		actually_founded_in_playlist.clear();
	}
	
	private static Function<BCAEventCatched, String> extract_key_from_catched_event = event -> {
		return event.getOriginalEventKey();
	};
	
	private static Function<BCAAutomationEvent, String> extract_key_from_automation_event = event -> {
		return event.getPreviouslyComputedKey();
	};
	
	void onAfterCatchEvents() {
		boolean modified = false;
		
		/**
		 * Remove old catched events.
		 */
		modified = entries.removeIf(en -> {
			Loggers.BroadcastAutomationEventCatch.debug("Remove old aired event: " + en.toString());
			return en.isOldAired();
		});
		
		/**
		 * Compare actual list and founded events.
		 */
		TwoListComparing<BCAAutomationEvent, BCAEventCatched, String> comparing = new TwoListComparing<>(actually_founded_in_playlist, entries, extract_key_from_automation_event, extract_key_from_catched_event);
		TwoListComparing<BCAAutomationEvent, BCAEventCatched, String>.ComparingResult c_result = comparing.process();
		
		if (Loggers.BroadcastAutomationEventCatch.isDebugEnabled() && c_result.hasPositiveResults()) {
			Loggers.BroadcastAutomationEventCatch.debug("Catch some events (L=new, R=actual): " + c_result.toString());
		}
		
		/**
		 * Remove previously catched events removed from last playlist.
		 */
		c_result.getMissingInLAddedInR().forEach(event -> {
			Loggers.BroadcastAutomationEventCatch.info("Catch event moved/removed in playlist: \"" + event.toString() + "\"");
			handleEventRemoving(event);
			entries.remove(event);
		});
		
		/**
		 * Add new events to add to catched event.
		 */
		entries.addAll(c_result.getMissingInRAddedInL().stream().map(event -> {
			return BCAEventCatched.create(event, createExternalRef());
		}).peek(event -> {
			Loggers.BroadcastAutomationEventCatch.info("Catch new event: \"" + event.toString() + "\"");
			handleEventCreation(event);
		}).collect(Collectors.toList()));
		
		if (modified | c_result.hasPositiveResults()) {
			try {
				FileUtils.writeStringToFile(json_db_file, MyDMAM.gson_kit.getGsonPretty().toJson(entries, GsonKit.type_ArrayList_BCACatchEntry), MyDMAM.UTF8);
			} catch (IOException e) {
				Loggers.BroadcastAutomationEventCatch.error("Can't write to json events: \"" + entries.toString() + "\"");
			}
		}
	}
	
	private String createExternalRef() {
		return String.valueOf(entries.stream().mapToInt(event -> {
			return Integer.parseInt(event.getExternalRef());
		}).max().orElse(-1) + 1);
	}
	
	protected abstract void handleEventCreation(BCAEventCatched entry);
	
	protected abstract void handleEventRemoving(BCAEventCatched entry);
	
	public abstract String getName();
	
	public abstract String getVendor();
	
}
