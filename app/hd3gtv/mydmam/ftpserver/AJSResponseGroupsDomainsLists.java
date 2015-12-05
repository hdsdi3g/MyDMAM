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
package hd3gtv.mydmam.ftpserver;

import java.util.ArrayList;

import hd3gtv.configuration.Configuration;

public class AJSResponseGroupsDomainsLists {
	
	private static ArrayList<String> _groups;
	private static ArrayList<String> _domains;
	
	static {
		_groups = Configuration.global.getValues("ftpserveradmin", "groups", null);
		_domains = Configuration.global.getValues("ftpserveradmin", "domains", null);
	}
	
	public ArrayList<String> groups;
	public ArrayList<String> domains;
	
	public AJSResponseGroupsDomainsLists() {
		groups = _groups;
		domains = _domains;
	}
}
