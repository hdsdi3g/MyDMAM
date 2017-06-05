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
package hd3gtv.mydmam.auth.asyncjs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.auth.AuthTurret;
import hd3gtv.mydmam.auth.UserNG;
import hd3gtv.mydmam.web.AJSController;

public class UserSearchResult {
	
	String q;
	ArrayList<UserInfo> results;
	
	public void search(String raw, AuthTurret turret) throws Exception {
		if (raw == null) {
			throw new NullPointerException("\"q\" can't to be null");
		}
		if (raw.isEmpty()) {
			throw new IndexOutOfBoundsException("\"q\" can't to be empty");
		}
		
		final String query = AuthTurret.filterChars(raw);
		q = query;
		results = new ArrayList<>(10);
		
		if (query.length() < 3) {
			return;
		}
		
		List<UserNG> founded = turret.searchUser(AJSController.getUserProfile().getDomain(), raw);
		
		if (founded == null) {
			return;
		}
		
		results.addAll(founded.stream().map(userng -> {
			return new UserInfo(userng);
		}).collect(Collectors.toList()));
	}
	
	public void resolve(JsonArray user_key_list, String domain_to_check, AuthTurret turret) throws Exception {
		if (user_key_list == null) {
			throw new NullPointerException("\"user_key_list\" can't to be null");
		}
		if (user_key_list.size() == 0) {
			throw new IndexOutOfBoundsException("\"user_key_list\" can't to be empty");
		}
		q = null;
		results = new ArrayList<>(user_key_list.size());
		
		HashMap<String, UserNG> all_users = turret.getAllUsers();
		for (int pos = 0; pos < user_key_list.size(); pos++) {
			String key = user_key_list.get(pos).getAsString();
			if (all_users.containsKey(key) == false) {
				continue;
			}
			UserNG user = all_users.get(key);
			if (all_users.get(key).getDomain().equalsIgnoreCase(domain_to_check) == false) {
				Loggers.Auth.warn("Security! User " + AJSController.getUserProfile().getFullname() + " want to resolve an user key not from it's own domain (" + domain_to_check + " / " + user.getDomain() + ")");
				return;
			}
			results.add(new UserInfo(user));
		}
	}
	
}
