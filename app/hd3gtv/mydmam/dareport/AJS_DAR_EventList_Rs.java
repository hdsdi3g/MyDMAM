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
package hd3gtv.mydmam.dareport;

import java.util.ArrayList;
import java.util.HashMap;

import hd3gtv.mydmam.auth.AuthTurret;

public class AJS_DAR_EventList_Rs {
	
	ArrayList<DAREvent> events;
	HashMap<String, ArrayList<String>> report_authors_by_event_name;
	HashMap<String, String> usernames;
	
	public void populate(AuthTurret turret) throws Exception {
		events = DAREvent.list();
		report_authors_by_event_name = DARReport.listAuthorsByEvents();
		
		usernames = new HashMap<>();
		
		report_authors_by_event_name.values().stream().flatMap(list -> {
			return list.stream();
		}).map(user_key -> {
			return turret.getByUserKey(user_key);
		}).filter(user -> {
			return user != null;
		}).forEach(user -> {
			if (usernames.containsKey(user.getKey()) == false) {
				usernames.put(user.getKey(), user.getFullname());
			}
		});
	}
	
}
