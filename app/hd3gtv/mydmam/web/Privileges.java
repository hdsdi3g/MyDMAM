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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/

package hd3gtv.mydmam.web;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;

public class Privileges {
	
	private static List<String> privileges;
	
	static {
		// TODO get Real privileges from controllers
		
		privileges = new ArrayList<String>();
		privileges.add("eatBanana");
		privileges.add("eatStrawberry");
		privileges.add("moveSpinach");
		privileges.add("cookSpinach");
		privileges.add("getSpinach");
	}
	
	public static List<String> getPrivileges() {
		return privileges;
	}
	
	public static JSONArray getJSONPrivileges() {
		JSONArray ja = new JSONArray();
		ja.addAll(privileges);
		return ja;
	}
	
}
