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
package controllers.ajs;

import java.util.LinkedHashMap;

import controllers.Check;
import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.bcastautomation.ArrayListString;
import hd3gtv.mydmam.bcastautomation.BCAWatcher;
import hd3gtv.mydmam.bcastautomation.LinkedHashMapStringString;
import hd3gtv.mydmam.bcastautomation.TimedEventStore;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.web.AJSController;

/**
 * Broadcast automation
 */
public class BCA extends AJSController {
	
	private static TimedEventStore database = null;
	
	@Check("BCA")
	public static LinkedHashMapStringString allEvents() throws Exception {
		if (database == null) {
			database = new TimedEventStore(CassandraDb.getkeyspace(), BCAWatcher.CF_NAME);
		}
		
		LinkedHashMapStringString r = new LinkedHashMapStringString();
		r.items = new LinkedHashMap<>();
		
		database.getAll().forEach(event -> {
			r.items.put(event.getKey(), event.getCols().getColumnByName(BCAWatcher.DB_COL_CONTENT_NAME).getStringValue());
		});
		
		return r;
	}
	
	@Check("BCA")
	public static LinkedHashMapStringString futureKeys() throws Exception {
		if (database == null) {
			database = new TimedEventStore(CassandraDb.getkeyspace(), BCAWatcher.CF_NAME);
		}
		
		LinkedHashMapStringString r = new LinkedHashMapStringString();
		r.items = new LinkedHashMap<>();
		
		database.getAllKeys(future -> {
			r.items.put(future, "");
		}, aired_key -> {
		}, nonaired_key -> {
		});
		
		return r;
	}
	
	@Check("BCA")
	public static LinkedHashMapStringString pastKeys() throws Exception {
		if (database == null) {
			database = new TimedEventStore(CassandraDb.getkeyspace(), BCAWatcher.CF_NAME);
		}
		
		LinkedHashMapStringString r = new LinkedHashMapStringString();
		r.items = new LinkedHashMap<>();
		
		database.getAllKeys(future -> {
		}, aired_key -> {
			r.items.put(aired_key, "aired");
		}, nonaired_key -> {
			r.items.put(nonaired_key, "nonaired");
		});
		
		return r;
	}
	
	@Check("BCA")
	public static LinkedHashMapStringString getEventsByKeys(ArrayListString keys) throws Exception {
		if (database == null) {
			database = new TimedEventStore(CassandraDb.getkeyspace(), BCAWatcher.CF_NAME);
		}
		
		LinkedHashMapStringString r = new LinkedHashMapStringString();
		r.items = new LinkedHashMap<>();
		
		database.getByKeys(keys.items).forEach(event -> {
			r.items.put(event.getKey(), event.getCols().getColumnByName(BCAWatcher.DB_COL_CONTENT_NAME).getStringValue());
		});
		
		return r;
	}
	
	public static boolean isEnabled() {
		return Configuration.global.isElementExists("broadcast_automation");
	}
	
}
