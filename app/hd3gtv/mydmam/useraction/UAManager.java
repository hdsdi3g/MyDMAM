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
package hd3gtv.mydmam.useraction;

import hd3gtv.mydmam.module.MyDMAMModule;
import hd3gtv.mydmam.module.MyDMAMModulesManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class UAManager {
	
	private UAManager() {
	}
	
	/**
	 * UAFunctionality Name -> UAFunctionality
	 */
	private static volatile LinkedHashMap<String, UAFunctionality> functionalities_class_map;
	private static volatile List<UAFunctionality> functionalities_list;
	
	static {
		functionalities_class_map = new LinkedHashMap<String, UAFunctionality>();
		functionalities_list = new ArrayList<UAFunctionality>();
		
		// TODO add() internal implementations
		
		List<MyDMAMModule> modules = MyDMAMModulesManager.getAllModules();
		for (int pos = 0; pos < modules.size(); pos++) {
			addAll(modules.get(pos).getUAfunctionality());
		}
	}
	
	private static void add(UAFunctionality functionality) {
		if (functionality == null) {
			return;
		}
		if (functionalities_class_map.containsKey(functionality.getName())) {
			return;
		}
		functionalities_class_map.put(functionality.getName(), functionality);
		functionalities_list.add(functionality);
	}
	
	private static void addAll(List<? extends UAFunctionality> functionalities) {
		if (functionalities == null) {
			return;
		}
		for (int pos = 0; pos < functionalities.size(); pos++) {
			add(functionalities.get(pos));
			add(functionalities.get(pos));
		}
	}
	
	public static UAFunctionality getByName(String name) {
		return functionalities_class_map.get(name);
	}
	
	// TODO create workers with Configuration (set a Map for workers <-> functionalities)
}
