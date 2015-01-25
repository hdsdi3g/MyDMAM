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
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.manager.AppManager;
import hd3gtv.mydmam.module.MyDMAMModule;
import hd3gtv.mydmam.module.MyDMAMModulesManager;
import hd3gtv.mydmam.useraction.dummy.UADummy;
import hd3gtv.mydmam.useraction.dummy.UADummy2;
import hd3gtv.tools.GsonIgnoreStrategy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class UAManager {
	
	private UAManager() {
	}
	
	/**
	 * UAFunctionality Name -> UAFunctionality
	 */
	private static volatile LinkedHashMap<String, UAFunctionalityContext> functionalities_class_map;
	private static volatile List<UAFunctionalityContext> functionalities_list;
	private static Gson gson;
	
	static {
		functionalities_class_map = new LinkedHashMap<String, UAFunctionalityContext>();
		functionalities_list = new ArrayList<UAFunctionalityContext>();
		
		GsonBuilder builder = new GsonBuilder();
		builder.serializeNulls();
		builder.registerTypeAdapter(UAFunctionalityDefinintion.class, new UAFunctionalityDefinintion.Serializer());
		builder.registerTypeAdapter(UACapabilityDefinition.class, new UACapabilityDefinition.Serializer());
		builder.registerTypeAdapter(UAConfigurator.class, new UAConfigurator.JsonUtils());
		builder.registerTypeAdapter(UAFinisherConfiguration.class, new UAFinisherConfiguration.Serializer());
		builder.registerTypeAdapter(UACreationRequest.class, new UACreationRequest.Serializer());
		builder.registerTypeAdapter(Class.class, new MyDMAM.GsonClassSerializer());
		
		GsonIgnoreStrategy ignore_strategy = new GsonIgnoreStrategy();
		builder.addDeserializationExclusionStrategy(ignore_strategy);
		builder.addSerializationExclusionStrategy(ignore_strategy);
		
		gson = builder.create();
		
		add(new UADummy());
		add(new UADummy2());
		
		List<MyDMAMModule> modules = MyDMAMModulesManager.getAllModules();
		for (int pos = 0; pos < modules.size(); pos++) {
			addAll(modules.get(pos).getUAfunctionality());
		}
	}
	
	public static Gson getGson() {
		return gson;
	}
	
	private static void add(UAFunctionalityContext functionality) {
		if (functionality == null) {
			return;
		}
		if (functionalities_class_map.containsKey(functionality.getClass().getName())) {
			return;
		}
		functionalities_class_map.put(functionality.getClass().getName(), functionality);
		functionalities_list.add(functionality);
	}
	
	public static List<UAFunctionalityDefinintion> getAllDeclaredFunctionalities() {
		ArrayList<UAFunctionalityDefinintion> result = new ArrayList<UAFunctionalityDefinintion>(functionalities_list.size());
		for (int pos = 0; pos < functionalities_list.size(); pos++) {
			result.add(functionalities_list.get(pos).getDefinition());
		}
		return result;
	}
	
	public static List<String> getAllDeclaredFunctionalitiesClassname() {
		ArrayList<String> result = new ArrayList<String>(functionalities_list.size());
		for (int pos = 0; pos < functionalities_list.size(); pos++) {
			result.add(functionalities_list.get(pos).getClass().getName());
		}
		return result;
	}
	
	public static int getAllDeclaredFunctionalitiesCount() {
		return functionalities_list.size();
	}
	
	private static void addAll(List<? extends UAFunctionalityContext> functionalities) {
		if (functionalities == null) {
			return;
		}
		for (int pos = 0; pos < functionalities.size(); pos++) {
			add(functionalities.get(pos));
		}
	}
	
	public static UAFunctionalityContext getByName(String classname) {
		return functionalities_class_map.get(classname);
	}
	
	public static void createWorkers(AppManager manager) {
		if (Configuration.global.isElementKeyExists("useraction", "workers_activated") == false) {
			return;
		}
		List<List<String>> conf_workers = Configuration.global.getListsInListValues("useraction", "workers_activated");
		
		List<String> list;
		UAFunctionalityContext functionality;
		List<UAFunctionalityContext> worker_functionalities_list;
		boolean founded;
		for (int pos_conf_worker = 0; pos_conf_worker < conf_workers.size(); pos_conf_worker++) {
			worker_functionalities_list = new ArrayList<UAFunctionalityContext>();
			list = conf_workers.get(pos_conf_worker);
			for (int pos_list = 0; pos_list < list.size(); pos_list++) {
				founded = false;
				for (int pos_funct = 0; pos_funct < functionalities_list.size(); pos_funct++) {
					functionality = functionalities_list.get(pos_funct);
					if (functionality.getClass().getName().toLowerCase().startsWith(list.get(pos_list).toLowerCase())) {
						if (worker_functionalities_list.contains(functionality) == false) {
							worker_functionalities_list.add(functionality);
							founded = true;
							break;
						}
					}
				}
				if (founded == false) {
					Log2Dump dump = new Log2Dump();
					dump.add("worker", pos_conf_worker + 1);
					dump.add("pos in worker", pos_list + 1);
					Log2.log.error("Can't found User action functionality declared in configuration", new ClassNotFoundException(list.get(pos_list).toLowerCase()), dump);
				}
			}
			
			for (int pos_funct = worker_functionalities_list.size() - 1; pos_funct > -1; pos_funct--) {
				functionality = worker_functionalities_list.get(pos_funct);
				if (functionality.getUserActionWorkerCapablities() == null) {
					worker_functionalities_list.remove(pos_funct);
					Log2.log.error("No declared profile for this functionality and can't add it to a worker", new NullPointerException(), functionality.getDefinition());
				}
			}
			
			if (worker_functionalities_list.isEmpty()) {
				continue;
			}
			
			Log2Dump dump = new Log2Dump();
			dump.add("Functionalities:", "\\");
			for (int pos_funct = 0; pos_funct < worker_functionalities_list.size(); pos_funct++) {
				functionality = worker_functionalities_list.get(pos_funct);
				dump.add(functionality.getClass().getSimpleName(), functionality.getLongName());
			}
			Log2.log.debug("Add Useraction worker", dump);
			manager.workerRegister(new UAWorker(worker_functionalities_list));
		}
	}
}
