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
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
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
import hd3gtv.tools.GsonIgnoreStrategy;
import play.Play;

public final class InstanceStatus {
	
	private static final ColumnFamily<String, String> CF_INSTANCES = new ColumnFamily<String, String>("mgrInstances", StringSerializer.get(), StringSerializer.get());
	
	public enum CF_COLS {
		COL_THREADS, COL_CLASSPATH, COL_SUMMARY, COL_ITEMS;
		
		public String toString() {
			switch (this) {
			case COL_THREADS:
				return "threadstacktraces";
			case COL_CLASSPATH:
				return "classpath";
			case COL_SUMMARY:
				return "summary";
			case COL_ITEMS:
				return "items";
			default:
				return "none";
			}
		}
	}
	
	private static Keyspace keyspace;
	
	private static final Gson gson;
	
	/*private static Type type_instance_status_item = new TypeToken<ArrayList<InstanceStatusItem>>() {
	}.getType();*/
	
	static {
		GsonBuilder builder = new GsonBuilder();
		builder.serializeNulls();
		GsonIgnoreStrategy ignore_strategy = new GsonIgnoreStrategy();
		builder.addDeserializationExclusionStrategy(ignore_strategy);
		builder.addSerializationExclusionStrategy(ignore_strategy);
		gson = builder.create();
		
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
	
	private static InstanceStatus is_static;
	
	/**
	 * @return an InstanceStatus gathered
	 */
	public static InstanceStatus getStatic() {
		if (is_static == null) {
			String name = "Gatherer";
			if (Play.initialized) {
				name = "This Play instance";
			}
			is_static = new AppManager(name).getInstanceStatus();
		}
		return is_static;
	}
	
	private transient AppManager manager;
	
	public final Summary summary;
	
	private JsonArray classpath;
	
	private ArrayList<InstanceStatusItem> items;
	
	public InstanceStatus(AppManager manager) {
		this.manager = manager;
		if (manager == null) {
			throw new NullPointerException("\"manager\" can't to be null");
		}
		summary = new Summary();
		items = new ArrayList<InstanceStatusItem>();
		items.add(manager);
		
		String java_classpath = System.getProperty("java.class.path");
		String[] classpath_lines = java_classpath.split(System.getProperty("path.separator"));
		classpath = new JsonArray();
		for (int pos = 0; pos < classpath_lines.length; pos++) {
			File file = new File(classpath_lines[pos]);
			StringBuffer sb_classpath = new StringBuffer();
			sb_classpath.append(file.getParentFile().getParentFile().getName());
			sb_classpath.append("/");
			sb_classpath.append(file.getParentFile().getName());
			sb_classpath.append("/");
			sb_classpath.append(file.getName());
			classpath.add(new JsonPrimitive(sb_classpath.toString().toLowerCase()));
		}
	}
	
	public class Summary {
		private String instance_name;
		private String pid;
		private String instance_name_pid;
		private String app_version;
		@SuppressWarnings("unused")
		private String java_version;
		private String host_name;
		private ArrayList<String> host_addresses;
		
		/**
		 * Only if manager is set
		 */
		private String app_name;
		
		/**
		 * Only if manager is set
		 */
		@SuppressWarnings("unused")
		private long starttime;
		
		public Summary() {
			try {
				host_name = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				host_name = "";
				Loggers.Manager.warn("Can't extract host name", e);
			}
			
			instance_name = Configuration.global.getValue("service", "workername", "unknown-pleaseset-" + String.valueOf(System.currentTimeMillis()));
			String instance_raw = ManagementFactory.getRuntimeMXBean().getName();
			pid = instance_raw.substring(0, instance_raw.indexOf("@"));
			instance_name_pid = instance_name + "#" + pid + "@" + host_name;
			
			GitInfo git = GitInfo.getFromRoot();
			if (git != null) {
				app_version = git.getBranch() + " " + git.getCommit();
			} else {
				app_version = "unknow";
			}
			java_version = System.getProperty("java.version");
			
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
			
			if (manager != null) {
				app_name = manager.getAppName();
				starttime = AppManager.starttime;
			}
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
		
		public String getPID() {
			return pid;
		}
	}
	
	private static class ThreadStackTrace {
		@SuppressWarnings("unused")
		String name;
		@SuppressWarnings("unused")
		long id;
		@SuppressWarnings("unused")
		String classname;
		@SuppressWarnings("unused")
		String state;
		@SuppressWarnings("unused")
		boolean isdaemon;
		@SuppressWarnings("unused")
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
	
	public void registerInstanceStatusItem(InstanceStatusItem item) {
		if (item == null) {
			throw new NullPointerException("\"item\" can't to be null");
		}
		if (item.getReferenceKey() == null) {
			throw new NullPointerException("\"instance status item name\" can't to be null");
		}
		items.add(item);
	}
	
	public static JsonArray getThreadstacktraces() {
		JsonArray threadstacktraces = new JsonArray();
		for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
			threadstacktraces.add(gson.toJsonTree(new ThreadStackTrace().importThread(entry.getKey(), entry.getValue())));
		}
		return threadstacktraces;
	}
	
	public JsonArray getClasspath() {
		return classpath;
	}
	
	public JsonArray getItems() {
		JsonArray ja_items = new JsonArray();
		JsonObject jo;
		InstanceStatusItem item;
		for (int pos = 0; pos < items.size(); pos++) {
			jo = new JsonObject();
			item = items.get(pos);
			try {
				jo.addProperty("key", item.getReferenceKey());
				jo.addProperty("class", item.getInstanceStatusItemReferenceClass().getSimpleName());
				jo.add("content", item.getInstanceStatusItem());
			} catch (Exception e) {
				jo.addProperty("error", e.getMessage());
				Loggers.Manager.warn("Can't get InstanceStatusItem for " + item.getReferenceKey(), e);
			}
			ja_items.add(jo);
		}
		return ja_items;
	}
	
	void refresh() {
		try {
			long start_time = System.currentTimeMillis();
			MutationBatch mutator = CassandraDb.prepareMutationBatch();
			String key = summary.instance_name_pid;
			mutator.withRow(CF_INSTANCES, key).putColumn(CF_COLS.COL_SUMMARY.toString(), gson.toJson(summary), TTL);
			mutator.withRow(CF_INSTANCES, key).putColumn(CF_COLS.COL_THREADS.toString(), getThreadstacktraces().toString(), TTL);
			mutator.withRow(CF_INSTANCES, key).putColumn(CF_COLS.COL_CLASSPATH.toString(), classpath.toString(), TTL);
			mutator.withRow(CF_INSTANCES, key).putColumn(CF_COLS.COL_ITEMS.toString(), getItems().toString(), TTL);
			mutator.execute();
			
			if (Loggers.Manager.isTraceEnabled()) {
				Loggers.Manager.trace("Update instance status took " + (System.currentTimeMillis() - start_time));
			}
		} catch (ConnectionException e) {
			manager.getServiceException().onCassandraError(e);
		}
	}
	
	public String toString() {
		return gson.toJson(this.summary);
	}
	
	public static void truncate() throws ConnectionException {
		CassandraDb.truncateColumnFamilyString(keyspace, CF_INSTANCES.getName());
	}
	
	/**
	 * @return raw Cassandra items.
	 */
	public JsonObject getAll(final CF_COLS col_name) {
		if (col_name == null) {
			throw new NullPointerException("\"col_name\" can't to be null");
		}
		
		final JsonObject result = new JsonObject();
		final JsonParser parser = new JsonParser();
		
		try {
			CassandraDb.allRowsReader(CF_INSTANCES, new AllRowsFoundRow() {
				public void onFoundRow(Row<String, String> row) throws Exception {
					String value = row.getColumns().getStringValue(col_name.toString(), "null");
					if (value.equals("null")) {
						return;
					}
					result.add(row.getKey(), parser.parse(value));
				}
			}, col_name.toString());
		} catch (Exception e) {
			manager.getServiceException().onCassandraError(e);
		}
		return result;
	}
	
}
