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
package hd3gtv.elemtl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.JsonSyntaxException;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.bcastautomation.BCACatchedEvent;
import hd3gtv.mydmam.bcastautomation.BCAEventCatcherHandler;

public class CuePointEngine extends BCAEventCatcherHandler {
	
	private HashMap<ElemtlServer, Integer> event_id_by_server;
	
	public CuePointEngine() throws JsonSyntaxException, IOException {
		super();
		event_id_by_server = new HashMap<>();
		
		ArrayList<String> srvs = Configuration.global.getValues("elemtl", "servers", null);
		if (srvs == null) {
			Loggers.Elemtl.error("No server list");
			return;
		}
		ArrayList<String> ids = Configuration.global.getValues("elemtl", "event_id_by_server", null);
		if (ids == null) {
			Loggers.Elemtl.error("No event_id_by_server list");
			return;
		}
		if (srvs.size() != ids.size()) {
			Loggers.Elemtl.error("Not the same list size with servers and event_id_by_server list");
			return;
		}
		
		for (int pos = 0; pos < srvs.size(); pos++) {
			try {
				event_id_by_server.put(new ElemtlServer(new URL(srvs.get(pos))), Integer.parseInt(ids.get(pos)));
			} catch (MalformedURLException e) {
				Loggers.Elemtl.error("Can't load server list", e);
			}
		}
		
		if (srvs.isEmpty()) {
			return;
		}
	}
	
	public void handleEventCreation(BCACatchedEvent entry) {
		event_id_by_server.forEach((s, id) -> {
			try {
				s.createCuePoint(entry.getDate(), Math.round(entry.getDuration().getValue()), id, Integer.parseInt(entry.getExternalRef()));
			} catch (Exception e) {
				Loggers.Elemtl.error("Can't push to server " + s, e);
			}
		});
	}
	
	public void handleEventRemoving(BCACatchedEvent entry) {
		event_id_by_server.forEach((s, id) -> {
			try {
				s.removeCuePoint(id, Integer.parseInt(entry.getExternalRef()));
			} catch (Exception e) {
				Loggers.Elemtl.error("Can't push to server " + s, e);
			}
		});
	}
	
	public String getName() {
		return "Elemtl CuePoint on playlist event";
	}
	
	public String getVendor() {
		return "MyDMAM Internal";
	}
	
}
