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
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
*/
package hd3gtv.mydmam.user;

import com.google.gson.Gson;
import com.netflix.astyanax.model.ColumnFamily;

public class AuthTurret {
	
	// TODO get an User from login + Password (+ domain)
	// TODO create new User
	
	// TODO import conf
	// TODO domain isolation or not
	
	private Gson gson_simple;
	private Gson gson;
	private ColumnFamily<String, String> user_cf;
	
	public Gson getGson() {
		return gson;
	}
	
	public Gson getGsonSimple() {
		return gson_simple;
	}
	
	public ColumnFamily<String, String> getUserCF() {
		return user_cf;
	}
}
