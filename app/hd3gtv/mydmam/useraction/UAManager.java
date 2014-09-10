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

import hd3gtv.configuration.Configuration;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.module.MyDMAMModule;
import hd3gtv.mydmam.module.MyDMAMModulesManager;
import hd3gtv.mydmam.taskqueue.WorkerGroup;
import hd3gtv.mydmam.useraction.dummy.UADummy;

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
		add(new UADummy());
		
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
	
	@SuppressWarnings("unchecked")
	public static void createWorkers(WorkerGroup wgroup) {
		if (Configuration.global.isElementKeyExists("useraction", "workers_activated") == false) {
			return;
		}
		List<LinkedHashMap<String, ?>> conf_workers = Configuration.global.getListMapValues("useraction", "workers_activated");
		
		LinkedHashMap<String, ?> conf_worker;
		List<String> list;
		UAFunctionality functionality;
		List<UAFunctionality> worker_functionalities_list;
		for (int pos_conf_worker = 0; pos_conf_worker < conf_workers.size(); pos_conf_worker++) {
			worker_functionalities_list = new ArrayList<UAFunctionality>();
			conf_worker = conf_workers.get(pos_conf_worker);
			if (conf_worker.containsKey("class")) {
				list = (List<String>) conf_worker.get("class");
				for (int pos_list = 0; pos_list < list.size(); pos_list++) {
					for (int pos_funct = 0; pos_funct < functionalities_list.size(); pos_funct++) {
						functionality = functionalities_list.get(pos_funct);
						if (functionality.getClass().getName().startsWith(list.get(pos_list))) {
							if (worker_functionalities_list.contains(functionality) == false) {
								worker_functionalities_list.add(functionality);
							}
						}
					}
				}
			}
			if (conf_worker.containsKey("name")) {
				list = (List<String>) conf_worker.get("name");
				for (int pos_list = 0; pos_list < list.size(); pos_list++) {
					for (int pos_funct = 0; pos_funct < functionalities_list.size(); pos_funct++) {
						functionality = functionalities_list.get(pos_funct);
						if (functionality.getName().equals(list.get(pos_list))) {
							if (worker_functionalities_list.contains(functionality) == false) {
								worker_functionalities_list.add(functionality);
							}
						}
					}
				}
			}
			
			if (worker_functionalities_list.isEmpty() == false) {
				Log2Dump dump = new Log2Dump();
				dump.add("Functionalities:", "");
				for (int pos_funct = 0; pos_funct < worker_functionalities_list.size(); pos_funct++) {
					functionality = functionalities_list.get(pos_funct);
					dump.add(functionality.getName(), functionality.getLongName());
				}
				Log2.log.info("Add Useraction worker", dump);
				wgroup.addWorker(new UAWorker(worker_functionalities_list));
			}
		}
	}
	
}
