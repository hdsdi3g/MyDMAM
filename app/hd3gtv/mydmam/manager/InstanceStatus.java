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
import java.util.HashMap;
import java.util.List;
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
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.query.AllRowsQuery;
import com.netflix.astyanax.serializers.StringSerializer;

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.GitInfo;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;
import hd3gtv.log2.Log2Filter;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.db.AllRowsFoundRow;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.useraction.UAFunctionalityContext;
import hd3gtv.mydmam.useraction.UAFunctionalityDefinintion;
import hd3gtv.mydmam.useraction.UAManager;
import hd3gtv.mydmam.useraction.UAWorker;
import hd3gtv.tools.GsonIgnore;
import hd3gtv.tools.TimeUtils;
import play.Play;

public final class InstanceStatus implements Log2Dumpable {
	
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
	private static Type al_uafunctionalitydefinintion_typeOfT = new TypeToken<ArrayList<UAFunctionalityDefinintion>>() {
	}.getType();
	private static Type al_cyclicjobscreator_typeOfT = new TypeToken<ArrayList<CyclicJobCreator>>() {
	}.getType();
	private static Type al_triggerjobscreator_typeOfT = new TypeToken<ArrayList<TriggerJobCreator>>() {
	}.getType();
	private static Type al_log2filter_typeOfT = new TypeToken<ArrayList<Log2Filter>>() {
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
		} catch (UnknownHostException e) {
			current_host_name = "";
		}
		
		current_instance_name = Configuration.global.getValue("service", "workername", "unknown-pleaseset-" + String.valueOf(System.currentTimeMillis()));
		String instance_raw = ManagementFactory.getRuntimeMXBean().getName();
		current_pid = instance_raw.substring(0, instance_raw.indexOf("@"));
		current_instance_name_pid = current_instance_name + "#" + current_pid + "@" + current_host_name;
		
		GitInfo git = GitInfo.getFromRoot();
		if (git != null) {
			current_app_version = git.getBranch() + " " + git.getCommit();
		} else {
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
	private long next_updater_refresh_date;
	private @GsonIgnore ArrayList<ThreadStackTrace> threadstacktraces;
	private String java_version;
	private String host_name;
	private ArrayList<String> host_addresses;
	private boolean brokeralive;
	private boolean is_off_hours;
	private @GsonIgnore ArrayList<UAFunctionalityDefinintion> useraction_functionality_list;
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
		useraction_functionality_list = new ArrayList<UAFunctionalityDefinintion>();
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
		
		useraction_functionality_list.clear();
		List<UAFunctionalityContext> full_functionality_list = new ArrayList<UAFunctionalityContext>();
		List<UAWorker> workers = manager.getAllActiveUAWorkers();
		for (int pos = 0; pos < workers.size(); pos++) {
			full_functionality_list.addAll(workers.get(pos).getFunctionalities_list());
		}
		for (int pos = 0; pos < full_functionality_list.size(); pos++) {
			useraction_functionality_list.add(full_functionality_list.get(pos).getDefinition());
		}
		
		if (push_to_db) {
			try {
				long start_time = System.currentTimeMillis();
				MutationBatch mutator = CassandraDb.prepareMutationBatch();
				mutator.withRow(CF_INSTANCES, instance_name_pid).putColumn(COL_NAME_UA_LIST, AppManager.getGson().toJson(useraction_functionality_list, al_uafunctionalitydefinintion_typeOfT), TTL);
				mutator.withRow(CF_INSTANCES, instance_name_pid).putColumn("source", AppManager.getGson().toJson(this), TTL);
				mutator.execute();
				Loggers.Manager.debug("Update instance status took " + (System.currentTimeMillis() - start_time));
			} catch (ConnectionException e) {
				manager.getServiceException().onCassandraError(e);
			}
		}
		
		return this;
	}
	
	private static final String COL_NAME_UA_LIST = "useraction_functionality_list";
	
	static class Serializer implements JsonSerializer<InstanceStatus> {
		public JsonElement serialize(InstanceStatus src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject result = (JsonObject) AppManager.getSimpleGson().toJsonTree(src);
			result.add("classpath", AppManager.getGson().toJsonTree(src.classpath, al_string_typeOfT));
			result.add("threadstacktraces", AppManager.getGson().toJsonTree(src.threadstacktraces, al_threadstacktrace_typeOfT));
			result.add("host_addresses", AppManager.getGson().toJsonTree(src.host_addresses, al_string_typeOfT));
			result.add("declared_cyclics", AppManager.getGson().toJsonTree(src.declared_cyclics, al_cyclicjobscreator_typeOfT));
			result.add("declared_triggers", AppManager.getGson().toJsonTree(src.declared_triggers, al_triggerjobscreator_typeOfT));
			result.add("log2filters", AppManager.getGson().toJsonTree(Log2.log.getFilters(), al_log2filter_typeOfT));
			result.add("loggersfilters", Loggers.getAllLevels());
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
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("app_name", app_name);
		dump.add("instance_name", instance_name);
		dump.add("instance_name_pid", instance_name_pid);
		dump.add("app_version", app_version);
		dump.add("java_version", java_version);
		dump.add("brokeralive", brokeralive);
		if (next_updater_refresh_date > 0) {
			dump.addDate("next_updater_refresh_date", next_updater_refresh_date);
		}
		dump.add("is_off_hours", is_off_hours);
		dump.add("uptime (sec)", uptime / 1000);
		dump.add("host_name", host_name);
		dump.add("host_addresses", host_addresses);
		dump.add("declared_cyclics", declared_cyclics);
		dump.add("declared_triggers", declared_triggers);
		return dump;
	}
	
	public static void truncate() throws ConnectionException {
		CassandraDb.truncateColumnFamilyString(keyspace, CF_INSTANCES.getName());
	}
	
	public static String getCurrentAvailabilitiesAsJsonString(ArrayList<String> privileges_for_user) throws ConnectionException {
		if (privileges_for_user == null) {
			return "{}";
		}
		if (privileges_for_user.isEmpty()) {
			return "{}";
		}
		
		Type useraction_functionality_list_typeOfT = new TypeToken<List<UAFunctionalityDefinintion>>() {
		}.getType();
		
		AllRowsQuery<String, String> all_rows = CassandraDb.getkeyspace().prepareQuery(CF_INSTANCES).getAllRows().withColumnSlice(InstanceStatus.COL_NAME_UA_LIST);
		OperationResult<Rows<String, String>> rows = all_rows.execute();
		
		Map<String, List<UAFunctionalityDefinintion>> all = new HashMap<String, List<UAFunctionalityDefinintion>>();
		
		List<UAFunctionalityDefinintion> list;
		for (Row<String, String> row : rows.getResult()) {
			Column<String> col = row.getColumns().getColumnByName(COL_NAME_UA_LIST);
			if (col == null) {
				continue;
			}
			list = UAManager.getGson().fromJson(col.getStringValue(), useraction_functionality_list_typeOfT);
			
			for (int pos = list.size() - 1; pos > -1; pos--) {
				if (privileges_for_user.contains(list.get(pos).classname) == false) {
					list.remove(pos);
				}
			}
			all.put(row.getKey(), list);
		}
		
		List<UAFunctionalityDefinintion> merged_definitions = new ArrayList<UAFunctionalityDefinintion>();
		List<UAFunctionalityDefinintion> current_definitions;
		for (Map.Entry<String, List<UAFunctionalityDefinintion>> entry : all.entrySet()) {
			current_definitions = entry.getValue();
			for (int pos_current = 0; pos_current < current_definitions.size(); pos_current++) {
				UAFunctionalityDefinintion.mergueInList(merged_definitions, current_definitions.get(pos_current));
			}
		}
		
		JsonObject result = new JsonObject();
		JsonObject result_implementation;
		JsonObject result_capability;
		JsonObject result_configurator;
		UAFunctionalityDefinintion current;
		
		for (int pos = 0; pos < merged_definitions.size(); pos++) {
			current = merged_definitions.get(pos);
			
			result_implementation = new JsonObject();
			result_implementation.addProperty("messagebasename", current.messagebasename);
			result_implementation.addProperty("section", current.section.name());
			result_implementation.addProperty("powerful_and_dangerous", current.powerful_and_dangerous);
			
			result_capability = (JsonObject) UAManager.getGson().toJsonTree(current.capability);
			result_implementation.add("capability", result_capability);
			
			result_configurator = (JsonObject) UAManager.getGson().toJsonTree(current.configurator);
			result_configurator.remove("type");
			result_configurator.remove("origin");
			result_implementation.add("configurator", result_configurator);
			
			result.add(current.classname, result_implementation);
		}
		
		return UAManager.getGson().toJson(result);
	}
	
	public static String getAllAvailabilitiesAsJsonString() throws ConnectionException {
		AllRowsQuery<String, String> all_rows = CassandraDb.getkeyspace().prepareQuery(CF_INSTANCES).getAllRows().withColumnSlice(InstanceStatus.COL_NAME_UA_LIST);
		OperationResult<Rows<String, String>> rows = all_rows.execute();
		
		JsonObject result = new JsonObject();
		JsonParser parser = new JsonParser();
		
		JsonArray ja_functionality_list;
		Column<String> col;
		for (Row<String, String> row : rows.getResult()) {
			col = row.getColumns().getColumnByName(COL_NAME_UA_LIST);
			if (col == null) {
				continue;
			}
			ja_functionality_list = parser.parse(col.getStringValue()).getAsJsonArray();
			for (int pos_ja_fl = 0; pos_ja_fl < ja_functionality_list.size(); pos_ja_fl++) {
				ja_functionality_list.get(pos_ja_fl).getAsJsonObject().remove("configurator");
			}
			result.add(row.getKey(), ja_functionality_list);
		}
		
		return result.toString();
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
