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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.ConfigurationItem;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonKit;

public abstract class BCAEventCatcherHandler {
	
	private HashMap<String, ConfigurationItem> import_other_properties_configuration;
	private long max_date_to_check;
	private String property_to_catch;
	
	private File json_db_file;
	private ArrayList<BCAEventCatched> entries;
	private ArrayList<BCAEventCatched> to_add = new ArrayList<>();
	private ArrayList<BCAEventCatched> to_remove = new ArrayList<>();
	
	private boolean modified;
	
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
		
		to_add = new ArrayList<>();
		to_remove = new ArrayList<>();
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
		Optional<BCAEventCatched> opt_entry = entries.stream().filter(event_catch -> {
			return event_catch.compare(event);
		}).findFirst();
		
		if (opt_entry.isPresent()) {
			opt_entry.get().setChecked(true);
		} else {
			BCAEventCatched entry = BCAEventCatched.create(event, createExternalRef());
			to_add.add(entry);
			entries.add(entry);
		}
	}
	
	void onBeforeCatchEvents() {
		to_add.clear();
		to_remove.clear();
		
		modified = false;
		
		entries.removeIf(en -> {
			if (en.isOld()) {
				modified = true;
				return true;
			}
			return false;
		});
		
		entries.forEach(en -> {
			en.setChecked(false);
		});
	}
	
	void onAfterCatchEvents() {
		entries.removeIf(entry -> {
			if (entry.isChecked() == false) {
				to_remove.add(entry);
				return true;
			}
			return false;
		});
		
		modified = modified | to_remove.isEmpty() == false | to_add.isEmpty() == false;
		
		to_remove.forEach(entry -> {
			Loggers.BroadcastAutomation.info("Catch event moved/removed in playlist: \"" + entry.toString() + "\"");
			// handleEventRemoving(entry); XXX
		});
		to_add.forEach(entry -> {
			Loggers.BroadcastAutomation.info("Catch new event: \"" + entry.toString() + "\"");
			// handleEventCreation(entry); XXX
		});
		
		if (modified) {
			try {
				FileUtils.writeStringToFile(json_db_file, MyDMAM.gson_kit.getGsonPretty().toJson(entries, GsonKit.type_ArrayList_BCACatchEntry), MyDMAM.UTF8);
			} catch (IOException e) {
				Loggers.BroadcastAutomation.error("Can't write to json events: \"" + entries.toString() + "\"");
			}
			modified = false;
		}
	}
	
	private String createExternalRef() {
		AtomicInteger i = new AtomicInteger(0);// TODO Not start from 0
		
		while (entries.stream().anyMatch(entry -> {
			return Integer.parseInt(entry.getExternalRef()) == i.get();
		})) {
			i.incrementAndGet();
		}
		return String.valueOf(i.get());
	}
	
	protected abstract void handleEventCreation(BCAEventCatched entry);
	
	protected abstract void handleEventRemoving(BCAEventCatched entry);
	
	public abstract String getName();
	
	public abstract String getVendor();
	
}
