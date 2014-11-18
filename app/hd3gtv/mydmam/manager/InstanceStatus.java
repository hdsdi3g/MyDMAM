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

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.GitInfo;
import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;

public class InstanceStatus implements Log2Dumpable {
	
	private static final ArrayList<String> current_classpath;
	private static final String current_instance_name;
	private static final String current_instance_name_pid;
	// private static final String current_app_name; TODO get AppName
	private static final String current_app_version;
	
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
	}
	
	private ArrayList<String> classpath;
	private String instance_name;
	private String instance_name_pid;
	private String app_version;
	
	InstanceStatus create() {
		InstanceStatus status = new InstanceStatus();
		status.classpath = current_classpath;
		status.instance_name = current_instance_name;
		status.instance_name_pid = current_instance_name_pid;
		status.app_version = current_app_version;
		
		// TODO status
		/*
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		mutator.withRow(CF_WORKERS, workername).putColumn("app-name", , period * 2);
		mutator.withRow(CF_WORKERS, workername).putColumn("app-version", manager.getApplicationVersion(), period * 2);
		mutator.withRow(CF_WORKERS, workername).putColumn("java-uptime", manager.getJavaUptime(), period * 2);
		mutator.withRow(CF_WORKERS, workername).putColumn("java-classpath", classpath, period * 2);
		
		JSONArray js_stacktraces = new JSONArray();
		Map<Thread, StackTraceElement[]> stacktraces = Thread.getAllStackTraces();
		Thread key;
		int linenumber;
		
		for (Map.Entry<Thread, StackTraceElement[]> entry : stacktraces.entrySet()) {
			JSONObject jo_stacktrace = new JSONObject();
			key = entry.getKey();
			if (key.getName().equals("Signal Dispatcher")) {
				continue;
			}
			if (key.getName().equals("Reference Handler")) {
				continue;
			}
			if (key.getName().equals("Finalizer")) {
				continue;
			}
			if (key.getName().equals("DestroyJavaVM")) {
				continue;
			}
			if (key.getName().equals("Attach Listener")) {
				continue;
			}
			if (key.getName().equals("Poller SunPKCS11-Darwin")) {
				continue;
			}
			if (key.getClass().getName().equals("hd3gtv.mydmam.probe.IsAlive")) {
				continue;
			}
			
			jo_stacktrace.put("name", key.getName());
			jo_stacktrace.put("id", key.getId());
			jo_stacktrace.put("classname", key.getClass().getName());
			jo_stacktrace.put("state", key.getState().toString());
			jo_stacktrace.put("isdaemon", key.isDaemon());
			
			if (entry.getValue().length > 0) {
				StringBuffer sb = new StringBuffer();
				sb.append(entry.getValue()[0].getClassName());
				sb.append(".");
				sb.append(entry.getValue()[0].getMethodName());
				if (entry.getValue()[0].getFileName() != null) {
					sb.append("(");
					sb.append(entry.getValue()[0].getFileName());
					linenumber = entry.getValue()[0].getLineNumber();
					if (linenumber > 0) {
						sb.append(":");
						sb.append(linenumber);
					}
					sb.append(")");
				}
				jo_stacktrace.put("execpoint", sb.toString());
			}
			
			js_stacktraces.add(jo_stacktrace);
		}
		mutator.withRow(CF_WORKERS, workername).putColumn("stacktraces", js_stacktraces.toJSONString(), period * 2);
		
		mutator.withRow(CF_WORKERS, workername).putColumn("java-version", System.getProperty("java.version"), period * 2);
		
		JSONObject jo_address = new JSONObject();
		try {
			jo_address.put("hostname", InetAddress.getLocalHost().getHostName());
		} catch (UnknownHostException e) {
			Log2.log.error("Can't resolve localhost name", e);
			jo_address.put("hostname", "localhost");
		}
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		
		JSONArray js_addresss = new JSONArray();
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
				js_addresss.add(currentInterface.getName() + " " + currentAddress.getHostAddress());
			}
		}
		jo_address.put("address", js_addresss);
		
		mutator.withRow(CF_WORKERS, workername).putColumn("java-address", jo_address.toJSONString(), period * 2);
		
		ArrayList<UAFunctionalityDefinintion> useraction_functionality_list = new ArrayList<UAFunctionalityDefinintion>();
		WorkerGroup worker_group = manager.getWorkergroup();
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
		}
		
		String json_useraction_functionality_list = UAManager.getGson().toJson(useraction_functionality_list, useraction_functionality_list_typeOfT);
		
		mutator.withRow(CF_WORKERS, workername).putColumn("useraction_functionality_list", json_useraction_functionality_list, period * 2);
		
		mutator.execute();
		
		sleep(period * 1000);
		*/
		
		return status;
	}
	
	/*class Serializer implements JsonSerializer<InstanceStatus>, JsonDeserializer<InstanceStatus> {
		@Override
		public InstanceStatus deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public JsonElement serialize(InstanceStatus src, Type typeOfSrc, JsonSerializationContext context) {
			// TODO Auto-generated method stub
			return null;
		}
	}*/
	
	@Override
	public Log2Dump getLog2Dump() {
		// TODO Auto-generated method stub
		return null;
	}
}
