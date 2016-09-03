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
package hd3gtv.mydmam.bcastautomation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.manager.AppManager;
import hd3gtv.mydmam.manager.InstanceActionReceiver;
import hd3gtv.mydmam.manager.InstanceStatusItem;

public class BCAWatcher implements InstanceStatusItem, InstanceActionReceiver {
	
	public BCAWatcher(AppManager manager, Configuration configuration) {
		// TODO Auto-generated constructor stub
		if (Configuration.global.isElementExists("xxx") == false) {
			return;
		}
		
	}
	
	public void stop() {
		// TODO
	}
	
	public Class<? extends InstanceActionReceiver> getClassToCallback() {
		return BCAWatcher.class;
	}
	
	@Override
	public void doAnAction(JsonObject order) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public JsonElement getInstanceStatusItem() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String getReferenceKey() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Class<?> getInstanceStatusItemReferenceClass() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
