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
package hd3gtv.mydmam.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * TODO Replace IsAlive, and Broker operations
 */
public class AppManager {
	
	private static final Gson gson;
	private static final Gson simple_gson;
	
	static {
		GsonBuilder builder = new GsonBuilder();
		builder.serializeNulls();
		// builder.registerTypeAdapter(InstanceStatus.class, new InstanceStatus().new Serializer());
		gson = builder.create();
		simple_gson = new Gson();
	}
	
	static Gson getGson() {
		return gson;
	}
	
	static Gson getSimpleGson() {
		return simple_gson;
	}
	
}
