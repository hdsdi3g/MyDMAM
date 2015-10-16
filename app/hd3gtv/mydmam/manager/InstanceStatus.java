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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.serializers.StringSerializer;

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.GitInfo;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.db.AllRowsFoundRow;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.tools.GsonIgnore;
import hd3gtv.tools.TimeUtils;
import play.Play;

public final class InstanceStatus {
	
	private static final ColumnFamily<String, String> CF_INSTANCES = new ColumnFamily<String, String>("mgrInstances", StringSerializer.get(), StringSerializer.get());
	
	private static Keyspace keyspace;
	
	static {
		try {
			keyspace = CassandraDb.getkeyspace();
			String default_keyspacename = CassandraDb.getDefaultKeyspacename();
			if (CassandraDb.isColumnFamilyExists(keyspace, CF_INSTANCES.getName()) == false) {
				CassandraDb.createColumnFamilyString(default_keyspacename, CF_INSTANCES.getName(), false);
			}
		} catch (Exception e) {
			Loggers.Manager.error("Can't init database CFs", e);
		}
	}
	
	/**
	 * In sec.
	 */
	static final int TTL = 120;
	private static final ArrayList<String> current_classpath;
	private static final String current_instance_name;
	private static final String current_instance_name_pid;
	private static final String current_pid;
	private static final String current_app_version;
	private static final String current_java_version;
	private static String current_host_name;
	
	private static Type al_string_typeOfT = new TypeToken<ArrayList<String>>() {
	}.getType();
	private static Type al_threadstacktrace_typeOfT = new TypeToken<ArrayList<ThreadStackTrace>>() {
	}.getType();
	private static Type al_cyclicjobscreator_typeOfT = new TypeToken<ArrayList<CyclicJobCreator>>() {
	}.getType();
	private static Type al_triggerjobscreator_typeOfT = new TypeToken<ArrayList<TriggerJobCreator>>() {
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
			current_classpath.add(sb_classpath.toString().toLowerCase());
		}
		
		try {
			current_host_name = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e)
		
		{
			current_host_name = "";
		}
		
		current_instance_name = Configuration.global.getValue("service", "workername", "unknown-pleaseset-" + String.valueOf(System.currentTimeMillis()));
		
		String instance_raw = ManagementFactory.getRuntimeMXBean().getName();
		current_pid = instance_raw.substring(0, instance_raw.indexOf("@"));
		current_instance_name_pid = current_instance_name + "#" + current_pid + "@" + current_host_name;
		
		GitInfo git = GitInfo.getFromRoot();
		if (git != null)
		
		{
			current_app_version = git.getBranch() + " " + git.getCommit();
		} else
		
		{
			current_app_version = "unknow";
		}
		current_java_version = System.getProperty("java.version");
		
	}
	
	private transient AppManager manager;
	
	private ArrayList<String> classpath;
	private String instance_name;
	private String instance_name_pid;
	private String app_name;
	private String app_version;
	private long uptime;
	@SuppressWarnings("unused")
	private long next_updater_refresh_date;
	private @GsonIgnore ArrayList<ThreadStackTrace> threadstacktraces;
	@SuppressWarnings("unused")
	private String java_version;
	private String host_name;
	private ArrayList<String> host_addresses;
	@SuppressWarnings("unused")
	private boolean brokeralive;
	@SuppressWarnings("unused")
	private boolean is_off_hours;
	private @GsonIgnore ArrayList<CyclicJobCreator> declared_cyclics;
	private @GsonIgnore ArrayList<TriggerJobCreator> declared_triggers;
	
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
			
			StringBuffer sb = new StringBuffer();
			for (int pos = 0; pos < stes.length; pos++) {
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
					} else {
						sb.append(":1");
					}
					sb.append(")");
				}
				sb.append("\n");
			}
			execpoint = sb.toString();
			return this;
		}
		
	}
	
	InstanceStatus populateFromThisInstance(AppManager manager) {
		this.manager = manager;
		classpath = current_classpath;
		instance_name = current_instance_name;
		instance_name_pid = current_instance_name_pid;
		app_version = current_app_version;
		java_version = current_java_version;
		host_name = current_host_name;
		threadstacktraces = new ArrayList<InstanceStatus.ThreadStackTrace>();
		brokeralive = manager.getBroker().isAlive();
		is_off_hours = AppManager.isActuallyOffHours();
		next_updater_refresh_date = manager.getNextUpdaterRefreshDate();
		
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
		
		Loggers.Manager.debug("Populate instance status from AppManager");
		refresh(false);
		
		return this;
	}
	
	/**
	 * @return this
	 */
	InstanceStatus refresh(boolean push_to_db) {
		app_name = manager.getAppName();
		uptime = System.currentTimeMillis() - AppManager.starttime;
		threadstacktraces.clear();
		brokeralive = manager.getBroker().isAlive();
		is_off_hours = AppManager.isActuallyOffHours();
		declared_cyclics = manager.getBroker().getDeclared_cyclics();
		declared_triggers = manager.getBroker().getDeclared_triggers();
		next_updater_refresh_date = manager.getNextUpdaterRefreshDate();
		for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
			threadstacktraces.add(new ThreadStackTrace().importThread(entry.getKey(), entry.getValue()));
		}
		
		if (push_to_db) {
			try {
				long start_time = System.currentTimeMillis();
				MutationBatch mutator = CassandraDb.prepareMutationBatch();
				mutator.withRow(CF_INSTANCES, instance_name_pid).putColumn("source", AppManager.getGson().toJson(this), TTL);
				mutator.execute();
				Loggers.Manager.debug("Update instance status took " + (System.currentTimeMillis() - start_time));
			} catch (ConnectionException e) {
				manager.getServiceException().onCassandraError(e);
			}
		}
		
		return this;
	}
	
	static class Serializer implements JsonSerializer<InstanceStatus> {
		public JsonElement serialize(InstanceStatus src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject result = (JsonObject) AppManager.getSimpleGson().toJsonTree(src);
			result.add("classpath", AppManager.getGson().toJsonTree(src.classpath, al_string_typeOfT));
			result.add("threadstacktraces", AppManager.getGson().toJsonTree(src.threadstacktraces, al_threadstacktrace_typeOfT));
			result.add("host_addresses", AppManager.getGson().toJsonTree(src.host_addresses, al_string_typeOfT));
			result.add("declared_cyclics", AppManager.getGson().toJsonTree(src.declared_cyclics, al_cyclicjobscreator_typeOfT));
			result.add("declared_triggers", AppManager.getGson().toJsonTree(src.declared_triggers, al_triggerjobscreator_typeOfT));
			result.add("log2filters", new JsonArray());
			result.addProperty("uptime_from", TimeUtils.secondsToYWDHMS(src.uptime / 1000));
			return result;
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
	
	public static String getThisCurrentPID() {
		return current_pid;
	}
	
	public static String getThisInstanceNamePid() {
		return current_instance_name_pid;
	}
	
	public static void truncate() throws ConnectionException {
		CassandraDb.truncateColumnFamilyString(keyspace, CF_INSTANCES.getName());
	}
	
	/**
	 * For get some manager tools, without start a manager.
	 */
	public static class Gatherer {
		private static final AppManager manager;
		
		static {
			String name = "Gatherer";
			if (Play.initialized) {
				name = "This Play instance";
			}
			manager = new AppManager(name);
		}
		
		public static InstanceStatus getDefaultManagerInstanceStatus() {
			return manager.getInstance_status();
		}
		
		public static String getAllInstancesJsonString() {
			final JsonArray result = new JsonArray();
			final JsonParser parser = new JsonParser();
			
			try {
				CassandraDb.allRowsReader(CF_INSTANCES, new AllRowsFoundRow() {
					public void onFoundRow(Row<String, String> row) throws Exception {
						result.add(parser.parse(row.getColumns().getStringValue("source", "{}")));
					}
				}, "source");
			} catch (Exception e) {
				manager.getServiceException().onCassandraError(e);
			}
			
			result.add(AppManager.getGson().toJsonTree(manager.getInstance_status().refresh(false)));
			return result.toString();
		}
		
	}
	
}
