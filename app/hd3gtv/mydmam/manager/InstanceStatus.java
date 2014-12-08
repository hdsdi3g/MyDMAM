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
import hd3gtv.mydmam.useraction.UAFunctionality;
import hd3gtv.mydmam.useraction.UAFunctionalityDefinintion;
import hd3gtv.mydmam.useraction.UAWorker;

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
import java.util.List;
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

public final class InstanceStatus implements Log2Dumpable {
	
	/**
	 * In sec.
	 */
	static final int TTL = 120;
	private static final ArrayList<String> current_classpath;
	private static final String current_instance_name;
	private static final String current_instance_name_pid;
	private static final String current_app_version;
	private static final String current_java_version;
	private static String current_host_name;
	
	private static Type al_string_typeOfT = new TypeToken<ArrayList<String>>() {
	}.getType();
	private static Type al_threadstacktrace_typeOfT = new TypeToken<ArrayList<ThreadStackTrace>>() {
	}.getType();
	private static Type al_uafunctionalitydefinintion_typeOfT = new TypeToken<ArrayList<UAFunctionalityDefinintion>>() {
	}.getType();
	private static Type al_cyclicjobscreator_typeOfT = new TypeToken<ArrayList<CyclicJobsCreator>>() {
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
	private String app_name;
	private String app_version;
	private long uptime;
	private @GsonIgnore ArrayList<ThreadStackTrace> threadstacktraces;
	private String java_version;
	private String host_name;
	private ArrayList<String> host_addresses;
	private @GsonIgnore ArrayList<UAFunctionalityDefinintion> useraction_functionality_list;
	private @GsonIgnore ArrayList<CyclicJobsCreator> declared_cyclics;
	
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
	
	InstanceStatus populateFromThisInstance(AppManager manager) {
		classpath = current_classpath;
		instance_name = current_instance_name;
		instance_name_pid = current_instance_name_pid;
		app_version = current_app_version;
		java_version = current_java_version;
		uptime = System.currentTimeMillis() - AppManager.starttime;
		threadstacktraces = new ArrayList<InstanceStatus.ThreadStackTrace>();
		host_name = current_host_name;
		this.app_name = manager.getAppName();
		declared_cyclics = manager.getBroker().getDeclared_cyclics();
		
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
		List<UAFunctionality> full_functionality_list = new ArrayList<UAFunctionality>();
		List<UAWorker> workers = manager.getAllActiveUAWorkers();
		for (int pos = 0; pos < workers.size(); pos++) {
			full_functionality_list.addAll(workers.get(pos).getFunctionalities_list());
		}
		for (int pos = 0; pos < full_functionality_list.size(); pos++) {
			useraction_functionality_list.add(full_functionality_list.get(pos).getDefinition());
		}
		return this;
	}
	
	static class Serializer implements JsonSerializer<InstanceStatus>, JsonDeserializer<InstanceStatus>, CassandraDbImporterExporter<InstanceStatus> {
		
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
			result.declared_cyclics = AppManager.getSimpleGson().fromJson(src.get("declared_cyclics"), al_cyclicjobscreator_typeOfT);
			return result;
		}
		
		public JsonElement serialize(InstanceStatus src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject result = (JsonObject) AppManager.getSimpleGson().toJsonTree(src);
			result.add("classpath", AppManager.getSimpleGson().toJsonTree(src.classpath, al_string_typeOfT));
			result.add("threadstacktraces", AppManager.getSimpleGson().toJsonTree(src.threadstacktraces, al_threadstacktrace_typeOfT));
			result.add("host_addresses", AppManager.getSimpleGson().toJsonTree(src.host_addresses, al_string_typeOfT));
			result.add("useraction_functionality_list", AppManager.getSimpleGson().toJsonTree(src.useraction_functionality_list, al_uafunctionalitydefinintion_typeOfT));
			result.add("declared_cyclics", AppManager.getSimpleGson().toJsonTree(src.declared_cyclics, al_cyclicjobscreator_typeOfT));
			return result;
		}
		
		public void exportToDatabase(InstanceStatus src, ColumnListMutation<String> mutator) {
			mutator.putColumn("instance_name", src.instance_name, TTL);
			mutator.putColumn("instance_name_pid", src.instance_name_pid, TTL);
			mutator.putColumn("app_version", src.app_version, TTL);
			mutator.putColumn("uptime", src.uptime, TTL);
			mutator.putColumn("java_version", src.java_version, TTL);
			mutator.putColumn("host_name", src.host_name, TTL);
			mutator.putColumn("classpath", AppManager.getSimpleGson().toJson(src.classpath, al_string_typeOfT), TTL);
			mutator.putColumn("threadstacktraces", AppManager.getSimpleGson().toJson(src.threadstacktraces, al_threadstacktrace_typeOfT), TTL);
			mutator.putColumn("host_addresses", AppManager.getSimpleGson().toJson(src.host_addresses, al_string_typeOfT), TTL);
			mutator.putColumn("useraction_functionality_list", AppManager.getSimpleGson().toJson(src.useraction_functionality_list, al_uafunctionalitydefinintion_typeOfT), TTL);
			mutator.putColumn("declared_cyclics", AppManager.getSimpleGson().toJson(src.declared_cyclics, al_cyclicjobscreator_typeOfT), TTL);
		}
		
		public String getDatabaseKey(InstanceStatus src) {
			return src.instance_name_pid;
		}
		
		public InstanceStatus importFromDatabase(ColumnList<String> columnlist) {
			InstanceStatus src = new InstanceStatus();
			src.instance_name = columnlist.getStringValue("instance_name", "");
			src.instance_name_pid = columnlist.getStringValue("instance_name_pid", "");
			src.app_version = columnlist.getStringValue("app_version", "");
			src.uptime = columnlist.getLongValue("uptime", -1l);
			src.java_version = columnlist.getStringValue("java_version", "");
			src.host_name = columnlist.getStringValue("host_name", "");
			src.classpath = AppManager.getSimpleGson().fromJson(columnlist.getStringValue("classpath", "[]"), al_string_typeOfT);
			src.threadstacktraces = AppManager.getSimpleGson().fromJson(columnlist.getStringValue("threadstacktraces", "[]"), al_threadstacktrace_typeOfT);
			src.host_addresses = AppManager.getSimpleGson().fromJson(columnlist.getStringValue("host_addresses", "[]"), al_string_typeOfT);
			src.useraction_functionality_list = AppManager.getSimpleGson().fromJson(columnlist.getStringValue("useraction_functionality_list", "[]"), al_uafunctionalitydefinintion_typeOfT);
			src.declared_cyclics = AppManager.getSimpleGson().fromJson(columnlist.getStringValue("declared_cyclics", "[]"), al_cyclicjobscreator_typeOfT);
			return src;
		}
	}
	
	public String toString() {
		return AppManager.getPrettyGson().toJson(this);
	}
	
	public String getHostName() {
		return host_name;
	}
	
	public String getInstanceNamePid() {
		return instance_name_pid;
	}
	
	public String getInstanceName() {
		return instance_name;
	}
	
	public String getAppName() {
		return app_name;
	}
	
	public String getAppVersion() {
		return app_version;
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("app_name", app_name);
		dump.add("instance_name", instance_name);
		dump.add("instance_name_pid", instance_name_pid);
		dump.add("app_version", app_version);
		dump.add("java_version", java_version);
		dump.add("classpath", classpath);
		dump.add("uptime (sec)", uptime / 1000);
		dump.add("host_name", host_name);
		dump.add("host_addresses", host_addresses);
		dump.add("declared_cyclics", declared_cyclics);
		return dump;
	}
}
