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

import groovy.json.JsonException;
import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.GitInfo;
import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;
import hd3gtv.mydmam.useraction.UAFunctionalityDefinintion;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Type;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.netflix.astyanax.ColumnListMutation;

public class InstanceStatus implements Log2Dumpable, DatabaseImporterExporter {
	
	private static final ArrayList<String> current_classpath;
	private static final String current_instance_name;
	private static final String current_instance_name_pid;
	// private static final String current_app_name; TODO get AppName
	private static final String current_app_version;
	private static final String current_java_version;
	
	private Type al_string_typeOfT = new TypeToken<ArrayList<String>>() {
	}.getType();
	private Type al_threadstacktrace_typeOfT = new TypeToken<ArrayList<ThreadStackTrace>>() {
	}.getType();
	private Type al_uafunctionalitydefinintion_typeOfT = new TypeToken<ArrayList<UAFunctionalityDefinintion>>() {
	}.getType();
	
	static {
		String java_classpath = System.getProperty("java.class.path");
		String[] classpath_lines = java_classpath.split(System.getProperty("path.separator"));
		current_classpath = new ArrayList<String>(classpath_lines.length);
		for (int pos = 0; pos < classpath_lines.length; pos++) {
			File file = new File(classpath_lines[pos]);
			StringBuffer sb_classpath = new StringBuffer();
			sb_classpath.append(file.getParentFile().getParentFile().getName());
			sb_classpath.append("/");
			sb_classpath.append(file.getParentFile().getName());
			sb_classpath.append("/");
			sb_classpath.append(file.getName());
			current_classpath.add(sb_classpath.toString());
		}
		
		current_instance_name = Configuration.global.getValue("service", "workername", "unknown-pleaseset-" + String.valueOf(System.currentTimeMillis()));
		String instance_raw = ManagementFactory.getRuntimeMXBean().getName();
		current_instance_name_pid = current_instance_name + "#" + instance_raw.substring(0, instance_raw.indexOf("@"));
		
		GitInfo git = GitInfo.getFromRoot();
		if (git != null) {
			current_app_version = git.getBranch() + " " + git.getCommit();
		} else {
			current_app_version = "unknow";
		}
		current_java_version = System.getProperty("java.version");
	}
	
	private ArrayList<String> classpath;
	private String instance_name;
	private String instance_name_pid;
	private String app_version;
	private long uptime;
	private ArrayList<ThreadStackTrace> threadstacktraces;
	private String java_version;
	private String host_name;
	private ArrayList<String> host_addresses;
	private ArrayList<UAFunctionalityDefinintion> useraction_functionality_list;
	
	public class ThreadStackTrace implements Log2Dumpable {
		String name;
		long id;
		String classname;
		String state;
		boolean isdaemon;
		String execpoint;
		
		private ThreadStackTrace importThread(Thread t, StackTraceElement[] stes) {
			name = t.getName();
			id = t.getId();
			classname = t.getClass().getName();
			state = t.getState().toString();
			isdaemon = t.isDaemon();
			
			if (stes.length > 0) {
				StringBuffer sb = new StringBuffer();
				sb.append(stes[0].getClassName());
				sb.append(".");
				sb.append(stes[0].getMethodName());
				if (stes[0].getFileName() != null) {
					sb.append("(");
					sb.append(stes[0].getFileName());
					int linenumber = stes[0].getLineNumber();
					if (linenumber > 0) {
						sb.append(":");
						sb.append(linenumber);
					}
					sb.append(")");
				}
				execpoint = sb.toString();
			}
			return this;
		}
		
		public Log2Dump getLog2Dump() {
			Log2Dump dump = new Log2Dump();
			dump.add("name", name);
			dump.add("id", id);
			dump.add("classname", classname);
			dump.add("state", state);
			dump.add("isdaemon", isdaemon);
			dump.add("execpoint", execpoint);
			return dump;
		}
	}
	
	InstanceStatus populateFromThisInstance() {
		classpath = current_classpath;
		instance_name = current_instance_name;
		instance_name_pid = current_instance_name_pid;
		app_version = current_app_version;
		java_version = current_java_version;
		uptime = System.currentTimeMillis() - AppManager.starttime;
		threadstacktraces = new ArrayList<InstanceStatus.ThreadStackTrace>();
		
		Thread key;
		for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
			key = entry.getKey();
			if (key.getName().equals("Signal Dispatcher")) {
				continue;
			} else if (key.getName().equals("Reference Handler")) {
				continue;
			} else if (key.getName().equals("Finalizer")) {
				continue;
			} else if (key.getName().equals("DestroyJavaVM")) {
				continue;
			} else if (key.getName().equals("Attach Listener")) {
				continue;
			} else if (key.getName().equals("Poller SunPKCS11-Darwin")) {
				continue;
			}
			threadstacktraces.add(new ThreadStackTrace().importThread(key, entry.getValue()));
		}
		
		try {
			host_name = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			host_name = "localhost";
		}
		
		host_addresses = new ArrayList<String>();
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface currentInterface = interfaces.nextElement();
				Enumeration<InetAddress> addresses = currentInterface.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress currentAddress = addresses.nextElement();
					if (currentInterface.getName().equals("lo0") | currentInterface.getName().equals("lo")) {
						continue;
					}
					if (currentAddress instanceof Inet6Address) {
						continue;
					}
					host_addresses.add(currentInterface.getName() + " " + currentAddress.getHostAddress());
				}
			}
		} catch (SocketException e) {
		}
		
		useraction_functionality_list = new ArrayList<UAFunctionalityDefinintion>();
		/*WorkerGroup worker_group = manager.getWorkergroup(); //TODO plug a workergroup @see UAManager.createWorkers()
		if (worker_group != null) {
			List<Worker> workers = worker_group.getWorkerlist();
			UAWorker current_worker;
			List<UAFunctionality> full_functionality_list = new ArrayList<UAFunctionality>();
			for (int pos = 0; pos < workers.size(); pos++) {
				if (workers.get(pos) instanceof UAWorker) {
					current_worker = (UAWorker) workers.get(pos);
					if (current_worker.isEnabled() == false) {
						continue;
					}
					full_functionality_list.addAll(current_worker.getFunctionalities_list());
				}
			}
			for (int pos = 0; pos < full_functionality_list.size(); pos++) {
				useraction_functionality_list.add(full_functionality_list.get(pos).getDefinition());
			}
		}*/
		return this;
	}
	
	class Serializer implements JsonSerializer<InstanceStatus>, JsonDeserializer<InstanceStatus> {
		public InstanceStatus deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			if ((json instanceof JsonObject) == false) {
				throw new JsonException("json is not a JsonObject");
			}
			JsonObject src = (JsonObject) json;
			InstanceStatus result = AppManager.getSimpleGson().fromJson(src, InstanceStatus.class);
			result.classpath = AppManager.getSimpleGson().fromJson(src.get("classpath"), al_string_typeOfT);
			result.threadstacktraces = AppManager.getSimpleGson().fromJson(src.get("threadstacktraces"), al_threadstacktrace_typeOfT);
			result.host_addresses = AppManager.getSimpleGson().fromJson(src.get("host_addresses"), al_string_typeOfT);
			result.useraction_functionality_list = AppManager.getSimpleGson().fromJson(src.get("useraction_functionality_list"), al_uafunctionalitydefinintion_typeOfT);
			return result;
		}
		
		public JsonElement serialize(InstanceStatus src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject result = (JsonObject) AppManager.getSimpleGson().toJsonTree(src);
			result.add("classpath", AppManager.getSimpleGson().toJsonTree(classpath, al_string_typeOfT));
			result.add("threadstacktraces", AppManager.getSimpleGson().toJsonTree(threadstacktraces, al_threadstacktrace_typeOfT));
			result.add("host_addresses", AppManager.getSimpleGson().toJsonTree(host_addresses, al_string_typeOfT));
			result.add("useraction_functionality_list", AppManager.getSimpleGson().toJsonTree(useraction_functionality_list, al_uafunctionalitydefinintion_typeOfT));
			return result;
		}
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("classpath", classpath);
		dump.add("instance_name", instance_name);
		dump.add("instance_name_pid", instance_name_pid);
		dump.add("app_version", app_version);
		dump.addDate("uptime", uptime);
		dump.add("java_version", java_version);
		dump.add("host_name", host_name);
		dump.add("host_addresses", host_addresses);
		dump.add("threadstacktraces", threadstacktraces);
		dump.add("useraction_functionality_list", useraction_functionality_list);
		return null;
	}
	
	public String getDatabaseKey() {
		return instance_name_pid;
	}
	
	@Override
	public void exportToDatabase(ColumnListMutation<String> mutator) {
		// TODO Auto-generated method stub
		// mutator.putColumn(arg0, arg1, DatabaseLayer.TTL);
	}
	
	@Override
	public void importFromDatabase() {
		// TODO Auto-generated method stub
		
	}
}
