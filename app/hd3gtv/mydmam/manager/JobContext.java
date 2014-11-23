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

import java.util.List;

import com.google.gson.JsonObject;

/**
 * Full configuration for a Job
 */
public interface JobContext {
	
	public JsonObject contextToJson();
	
	public void contextFromJson(JsonObject json_object);
	
	/**
	 * @return can be null or empty
	 */
	public List<String> getNeededIndexedStoragesName();
	
	/**
	 * @return true if worker need to mount getNeededIndexedStoragesName list localy (bridged mount on worker host), for a direct access via File.
	 */
	public boolean isNeededIndexedStoragesBridged();
	
	public abstract String getName();// TODO need to keep this ? Replace by Class ?
	
	public abstract String getCategory();// TODO need to keep this ? Replace by Class ?
	
}
