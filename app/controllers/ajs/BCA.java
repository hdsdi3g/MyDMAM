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
import hd3gtv.mydmam.bcastautomation.AJSRequestEventsKeys;
import hd3gtv.mydmam.bcastautomation.BCAWatcher;
import hd3gtv.mydmam.bcastautomation.AJSEventsList;
import hd3gtv.mydmam.bcastautomation.TimedEventStore;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.web.AJSController;

/**
 * Broadcast automation
 */
public class BCA extends AJSController {
	
	private static TimedEventStore database = null;
	
	@Check("BCA")
	public static AJSEventsList allEvents() throws Exception {
		if (database == null) {
			database = new TimedEventStore(CassandraDb.getkeyspace(), BCAWatcher.CF_NAME);
		}
		
		AJSEventsList r = new AJSEventsList();
		r.items = new LinkedHashMap<>();
		
		database.getFilteredAll().forEach(event -> {
			r.items.put(event.getKey(), event.getCols().getColumnByName(BCAWatcher.DB_COL_CONTENT_NAME).getStringValue());
		});
		
		return r;
	}
	
	@Check("BCA")
	public static AJSEventsList allKeys() throws Exception {
		if (database == null) {
			database = new TimedEventStore(CassandraDb.getkeyspace(), BCAWatcher.CF_NAME);
		}
		
		AJSEventsList r = new AJSEventsList();
		r.items = new LinkedHashMap<>();
		
		database.getAllKeys(event -> {
			r.items.put(event, "a");
		});
		
		return r;
	}
	
	@Check("BCA")
	public static AJSEventsList getEventsByKeys(AJSRequestEventsKeys keys) throws Exception {
		if (database == null) {
			database = new TimedEventStore(CassandraDb.getkeyspace(), BCAWatcher.CF_NAME);
		}
		
		AJSEventsList r = new AJSEventsList();
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
