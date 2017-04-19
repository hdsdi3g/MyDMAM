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

import java.text.Normalizer;
import java.util.ArrayList;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.auth.AuthTurret;
import hd3gtv.mydmam.auth.UserNG;

public class UserSearchResult {
	
	String q;
	ArrayList<Item> results;
	
	public class Item {
		String username;
		String long_name;
		String mail_addr;
		
		private Item() {
		}
		
		private Item(UserNG user) {
			// TODO
		}
	}
	
	private String filterChars(String in) {
		return MyDMAM.PATTERN_Combining_Diacritical_Marks_Spaced.matcher(Normalizer.normalize(in, Normalizer.Form.NFD)).replaceAll("").toLowerCase();
	}
	
	public void search(String q, AuthTurret turret) throws Exception {
		if (q == null) {
			throw new NullPointerException("\"q\" can't to be null");
		}
		if (q.isEmpty()) {
			throw new IndexOutOfBoundsException("\"q\" can't to be empty");
		}
		results = new ArrayList<>(10);
		
		/**
		 * First pass search in local user table
		 */
		final String query = filterChars(q);
		
		turret.getAllUsers().forEach((key, user) -> {
			if (results.size() == 10) {
				return;
			}
			
			if (filterChars(user.getName()).contains(query)) {
				results.add(new Item(user));
			} else if (user.getFullname() != null) {
				if (filterChars(user.getFullname()).contains(query)) {
					results.add(new Item(user));
				}
			}
		});
		
		if (results.size() >= 10) {
			return;
		}
		
		// TODO 2 pass search in backends...
	}
	
}
