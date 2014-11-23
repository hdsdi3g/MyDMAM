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
import com.netflix.astyanax.model.ColumnList;

public class InstanceStatus implements CassandraDbImporterExporter {
	
	private static final ArrayList<String> current_classpath;
	private static final String current_instance_name;
	private static final String current_instance_name_pid;
	// private static final String current_app_name; TODO get AppName
	private static final String current_app_version;
	private static final String current_java_version;
	private static String current_host_name;
	
	private static Type al_string_typeOfT = new TypeToken<ArrayList<String>>() {
	}.getType();
	private static Type al_threadstacktrace_typeOfT = new TypeToken<ArrayList<ThreadStackTrace>>() {
	}.getType();
	private static Type al_uafunctionalitydefinintion_typeOfT = new TypeToken<ArrayList<UAFunctionalityDefinintion>>() {
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
		
		try {
			current_host_name = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			current_host_name = "somehost";
		}
		
		current_instance_name = Configuration.global.getValue("service", "workername", "unknown-pleaseset-" + String.valueOf(System.currentTimeMillis()));
		String instance_raw = ManagementFactory.getRuntimeMXBean().getName();
		current_instance_name_pid = current_instance_name + "#" + instance_raw.substring(0, instance_raw.indexOf("@")) + "@" + current_host_name;
		
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
	private @GsonIgnore ArrayList<ThreadStackTrace> threadstacktraces;
	private String java_version;
	private String host_name;
	private ArrayList<String> host_addresses;
	private @GsonIgnore ArrayList<UAFunctionalityDefinintion> useraction_functionality_list;
	
	public class ThreadStackTrace {
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
			
			for (int pos = 0; pos < stes.length; pos++) {
				if (stes[pos].isNativeMethod()) {
					continue;
				}
				StringBuffer sb = new StringBuffer();
				/**
				 * "at " Added only for Eclipse can transform the text into a link in Console view...
				 */
				sb.append("at ");
				
				sb.append(stes[pos].getClassName());
				sb.append(".");
				sb.append(stes[pos].getMethodName());
				if (stes[pos].getFileName() != null) {
					sb.append("(");
					sb.append(stes[pos].getFileName());
					int linenumber = stes[pos].getLineNumber();
					if (linenumber > 0) {
						sb.append(":");
						sb.append(linenumber);
					}
					sb.append(")");
				}
				execpoint = sb.toString();
				break;
			}
			return this;
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
		host_name = current_host_name;
		
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
			result.add("classpath", AppManager.getSimpleGson().toJsonTree(src.classpath, al_string_typeOfT));
			result.add("threadstacktraces", AppManager.getSimpleGson().toJsonTree(src.threadstacktraces, al_threadstacktrace_typeOfT));
			result.add("host_addresses", AppManager.getSimpleGson().toJsonTree(src.host_addresses, al_string_typeOfT));
			result.add("useraction_functionality_list", AppManager.getSimpleGson().toJsonTree(src.useraction_functionality_list, al_uafunctionalitydefinintion_typeOfT));
			return result;
		}
	}
	
	public String getDatabaseKey() {
		return instance_name_pid;
	}
	
	public void exportToDatabase(ColumnListMutation<String> mutator) {
		mutator.putColumn("instance_name", instance_name, DatabaseLayer.TTL);
		mutator.putColumn("instance_name_pid", instance_name_pid, DatabaseLayer.TTL);
		mutator.putColumn("app_version", app_version, DatabaseLayer.TTL);
		mutator.putColumn("uptime", uptime, DatabaseLayer.TTL);
		mutator.putColumn("java_version", java_version, DatabaseLayer.TTL);
		mutator.putColumn("host_name", host_name, DatabaseLayer.TTL);
		mutator.putColumn("classpath", AppManager.getSimpleGson().toJson(classpath, al_string_typeOfT), DatabaseLayer.TTL);
		mutator.putColumn("threadstacktraces", AppManager.getSimpleGson().toJson(threadstacktraces, al_threadstacktrace_typeOfT), DatabaseLayer.TTL);
		mutator.putColumn("host_addresses", AppManager.getSimpleGson().toJson(host_addresses, al_string_typeOfT), DatabaseLayer.TTL);
		mutator.putColumn("useraction_functionality_list", AppManager.getSimpleGson().toJson(useraction_functionality_list, al_uafunctionalitydefinintion_typeOfT), DatabaseLayer.TTL);
	}
	
	public void importFromDatabase(ColumnList<String> columnlist) {
		if (columnlist.isEmpty()) {
			return;
		}
		instance_name = columnlist.getStringValue("instance_name", "");
		instance_name_pid = columnlist.getStringValue("instance_name_pid", "");
		app_version = columnlist.getStringValue("app_version", "");
		uptime = columnlist.getLongValue("uptime", -1l);
		java_version = columnlist.getStringValue("java_version", "");
		host_name = columnlist.getStringValue("host_name", "");
		classpath = AppManager.getSimpleGson().fromJson(columnlist.getStringValue("classpath", "[]"), al_string_typeOfT);
		threadstacktraces = AppManager.getSimpleGson().fromJson(columnlist.getStringValue("threadstacktraces", "[]"), al_threadstacktrace_typeOfT);
		host_addresses = AppManager.getSimpleGson().fromJson(columnlist.getStringValue("host_addresses", "[]"), al_string_typeOfT);
		useraction_functionality_list = AppManager.getSimpleGson().fromJson(columnlist.getStringValue("useraction_functionality_list", "[]"), al_uafunctionalitydefinintion_typeOfT);
	}
	
	public String toString() {
		return AppManager.getPrettyGson().toJson(this);
	}
}
