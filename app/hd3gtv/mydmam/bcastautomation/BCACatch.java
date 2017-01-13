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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonObject;

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.ConfigurationItem;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.manager.AppManager;
import hd3gtv.tools.CopyMove;

public class BCACatch {
	
	private BCAEngine engine;
	private HashMap<String, ConfigurationItem> import_other_properties_configuration;
	private File playlist_to_parse;
	private String property_to_catch;
	private File json_db_file;
	private long max_date_to_check;
	private boolean modified;
	
	private ArrayList<BCACatchEntry> entries;
	
	private static Type type_AL_BCACatchEntry = new TypeToken<ArrayList<BCACatchEntry>>() {
	}.getType();
	
	public BCACatch() throws NullPointerException, IOException, ReflectiveOperationException {
		engine = BCAWatcher.getEngine();
		
		import_other_properties_configuration = Configuration.getElement(Configuration.global.getElement("broadcast_automation"), "import_other_properties");
		
		json_db_file = new File(Configuration.global.getValue("broadcast_automation", "catch_json_db_file", "catch_bca_event.json"));
		
		if (json_db_file.exists()) {
			entries = AppManager.getGson().fromJson(FileUtils.readFileToString(json_db_file, MyDMAM.UTF8), type_AL_BCACatchEntry);
		} else {
			entries = new ArrayList<>();
		}
		
		playlist_to_parse = new File(Configuration.global.getValue("broadcast_automation", "catch_playlist_to_parse", ""));
		CopyMove.checkExistsCanRead(playlist_to_parse);
		CopyMove.checkIsFile(playlist_to_parse);
		
		property_to_catch = Configuration.global.getValue("broadcast_automation", "catch_property", "");
		if (property_to_catch.equals("")) {
			throw new NullPointerException("broadcast_automation.catch_property is not set");
		}
		
		max_date_to_check = System.currentTimeMillis() + Configuration.global.getValue("broadcast_automation", "catch_max_min_to_check", 24 * 60) * 60 * 1000;
	}
	
	public void save() throws IOException {
		if (modified == false) {
			return;
		}
		FileUtils.writeStringToFile(json_db_file, AppManager.getPrettyGson().toJson(entries, type_AL_BCACatchEntry), MyDMAM.UTF8);
		modified = false;
	}
	
	public BCACatch parsePlaylist(BCACatchHandler handler) throws IOException {
		ArrayList<BCACatchEntry> to_add = new ArrayList<>();
		ArrayList<BCACatchEntry> to_remove = new ArrayList<>();
		
		entries.removeIf(en -> {
			modified = true;
			return en.isOld();
		});
		
		engine.processScheduleFile(playlist_to_parse, event -> {
			if (event.getStartDate() > max_date_to_check) {
				return;
			}
			if (event.isAutomationPaused()) {
				return;
			}
			if (event.getStartDate() < System.currentTimeMillis()) {
				return;
			}
			
			JsonObject other_properties = event.getOtherProperties(import_other_properties_configuration);
			if (other_properties == null) {
				return;
			}
			if (other_properties.has(property_to_catch) == false) {
				return;
			}
			
			Optional<BCACatchEntry> opt_entry = entries.stream().filter(event_catch -> {
				return event_catch.compare(event);
			}).findFirst();
			
			if (opt_entry.isPresent()) {
				opt_entry.get().setChecked();
			} else {
				BCACatchEntry entry = BCACatchEntry.create(event, createExternalRef());
				to_add.add(entry);
				entries.add(entry);
			}
		});
		
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
			handler.handleEventRemoving(entry);
		});
		to_add.forEach(entry -> {
			Loggers.BroadcastAutomation.info("Catch new event: \"" + entry.toString() + "\"");
			handler.handleEventCreation(entry);
		});
		
		return this;
	}
	
	private String createExternalRef() {
		AtomicInteger i = new AtomicInteger(0);
		
		while (entries.stream().anyMatch(entry -> {
			return Integer.parseInt(entry.getExternalRef()) == i.get();
		})) {
			i.incrementAndGet();
		}
		return String.valueOf(i.get());
	}
	
}
