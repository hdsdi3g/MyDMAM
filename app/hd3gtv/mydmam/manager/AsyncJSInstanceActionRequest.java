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
package hd3gtv.mydmam.manager;

import com.google.gson.JsonObject;

import hd3gtv.mydmam.Loggers;

public class AsyncJSInstanceActionRequest {
	
	public String target_class_name;
	public String target_reference_key;
	public JsonObject json_order;
	
	public void doAction(String caller) {
		try {
			InstanceAction.addNew(target_class_name, target_reference_key, json_order, caller);
		} catch (Exception e) {
			Loggers.Play.error("Can't process action, target_class_name: " + target_class_name + ", target_reference_key:" + target_reference_key + ", json_order:" + json_order.toString(), e);
		}
	}
	
}
