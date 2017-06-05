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
package hd3gtv.mydmam.transcode;

import java.util.HashMap;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.manager.AppManager;

public final class ProcessingKitEngine {
	
	private HashMap<String, ProcessingKit> list_cache;
	private AppManager manager;
	
	public ProcessingKitEngine(AppManager manager) {
		list_cache = new HashMap<>(1);
		this.manager = manager;
	}
	
	public ProcessingKitEngine() {
		list_cache = new HashMap<>(1);
	}
	
	/**
	 * @return can be null
	 */
	public ProcessingKit get(String class_name) {
		if (list_cache.containsKey(class_name)) {
			if (list_cache.get(class_name).isFunctionnal() == false) {
				Loggers.Transcode.debug("Processingkit " + class_name + " is disabled.");
				return null;
			}
			return list_cache.get(class_name);
		}
		
		try {
			ProcessingKit pkit = MyDMAM.factory.create(class_name, ProcessingKit.class);
			list_cache.put(class_name, pkit);
			if (list_cache.get(class_name).isFunctionnal() == false) {
				Loggers.Transcode.warn("Processingkit " + class_name + " is disabled.");
				return null;
			}
			Loggers.Transcode.info("Load Processingkit " + pkit);
			
			if (manager != null) {
				manager.registerInstanceStatusAction(pkit);
			}
			
			return pkit;
		} catch (Exception e) {
			if (class_name.startsWith("hd3gtv.mydmam.transcode.kit") == false && class_name.indexOf(".") == -1) {
				return get("hd3gtv.mydmam.transcode.kit." + class_name);
			} else {
				Loggers.Transcode.error("Can't found or instance ProcessingKit: " + class_name, e);
			}
		}
		return null;
	}
	
}
