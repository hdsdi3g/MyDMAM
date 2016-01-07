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
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
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
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.serializers.StringSerializer;

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.GitInfo;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.db.AllRowsFoundRow;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.tools.GsonIgnoreStrategy;

public final class InstanceStatus {
	
	private static final ColumnFamily<String, String> CF_INSTANCES = new ColumnFamily<String, String>("mgrInstances", StringSerializer.get(), StringSerializer.get());
	
	public enum CF_COLS {
		COL_THREADS, COL_CLASSPATH, COL_SUMMARY, COL_ITEMS, COL_PERFSTATS;
		
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
			case COL_PERFSTATS:
				return "perfstats";
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
		@SuppressWarnings("unused")
		private String java_vendor;
		private String host_name;
		private ArrayList<String> host_addresses;
		
		@SuppressWarnings("unused")
		private String os_arch;
		@SuppressWarnings("unused")
		private String os_name;
		@SuppressWarnings("unused")
		private String os_version;
		@SuppressWarnings("unused")
		private String user_country;
		@SuppressWarnings("unused")
		private String user_language;
		@SuppressWarnings("unused")
		private String user_name;
		@SuppressWarnings("unused")
		private String user_timezone;
		@SuppressWarnings("unused")
		private int cpucount;
		
		/**
		 * Only if manager is set
		 */
		private String app_name;
		
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
			java_version = System.getProperty("java.version", "0");
			java_vendor = System.getProperty("java.vendor", "(No set)");
			os_arch = System.getProperty("os.arch", "(No set)");
			os_name = System.getProperty("os.name", "(No set)");
			os_version = System.getProperty("os.version", "(No set)");
			user_country = System.getProperty("user.country", "(No set)");
			user_language = System.getProperty("user.language", "(No set)");
			user_name = System.getProperty("user.name", "(No set)");
			user_timezone = System.getProperty("user.timezone", "(No set)");
			cpucount = Runtime.getRuntime().availableProcessors();
			starttime = ManagementFactory.getRuntimeMXBean().getStartTime();
			
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
		long id;
		@SuppressWarnings("unused")
		String classname;
		@SuppressWarnings("unused")
		String state;
		@SuppressWarnings("unused")
		boolean isdaemon;
		@SuppressWarnings("unused")
		String execpoint;
		@SuppressWarnings("unused")
		long cpu_time_ms = -1;
		
		private ThreadStackTrace importThread(Thread t, StackTraceElement[] stes) {
			name = t.getName();
			id = t.getId();
			classname = t.getClass().getName();
			state = t.getState().toString();
			isdaemon = t.isDaemon();
			if (ManagementFactory.getThreadMXBean().isThreadCpuTimeSupported()) {
				if (ManagementFactory.getThreadMXBean().isThreadCpuTimeEnabled()) {
					cpu_time_ms = ManagementFactory.getThreadMXBean().getThreadCpuTime(id) / (1000 * 1000);
				}
			}
			
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
	
	public JsonObject getPerfStats() {
		JsonObject result = new JsonObject();
		result.addProperty("now", System.currentTimeMillis());
		result.addProperty("pid", summary.pid);
		result.addProperty("instance_name", summary.instance_name);
		result.addProperty("host_name", summary.host_name);
		result.addProperty("maxMemory", Runtime.getRuntime().maxMemory());
		result.addProperty("totalMemory", Runtime.getRuntime().totalMemory());
		result.addProperty("freeMemory", Runtime.getRuntime().freeMemory());
		
		ClassLoadingMXBean clmxb = ManagementFactory.getClassLoadingMXBean();
		result.addProperty("getUnloadedClassCount", clmxb.getUnloadedClassCount());
		result.addProperty("getTotalLoadedClassCount", clmxb.getTotalLoadedClassCount());
		result.addProperty("getLoadedClassCount", clmxb.getLoadedClassCount());
		
		MemoryMXBean mmvb = ManagementFactory.getMemoryMXBean();
		result.addProperty("getObjectPendingFinalizationCount", mmvb.getObjectPendingFinalizationCount());
		result.addProperty("nonHeapUsed", mmvb.getNonHeapMemoryUsage().getUsed());
		result.addProperty("heapUsed", mmvb.getHeapMemoryUsage().getUsed());
		
		JsonArray ja_gc = new JsonArray();
		List<GarbageCollectorMXBean> m_gc = ManagementFactory.getGarbageCollectorMXBeans();
		for (int pos = 0; pos < m_gc.size(); pos++) {
			JsonObject jo_gc = new JsonObject();
			jo_gc.addProperty("name", m_gc.get(pos).getName());
			jo_gc.addProperty("time", m_gc.get(pos).getCollectionTime());
			jo_gc.addProperty("count", m_gc.get(pos).getCollectionCount());
			ja_gc.add(jo_gc);
		}
		result.add("gc", ja_gc);
		
		// List<MemoryPoolMXBean> m_pools = ManagementFactory.getMemoryPoolMXBeans();
		// m_pools.get(0).
		// List<MemoryManagerMXBean> m_managers = ManagementFactory.getMemoryManagerMXBeans();
		// m_managers.get(0).getName()
		// m_managers.get(0).getMemoryPoolNames()
		
		OperatingSystemMXBean os_mxb = ManagementFactory.getOperatingSystemMXBean();
		result.addProperty("getSystemLoadAverage", os_mxb.getSystemLoadAverage());
		
		try {
			Class.forName("com.sun.management.OperatingSystemMXBean");
			JsonObject jo_os = new JsonObject();
			com.sun.management.OperatingSystemMXBean os_sun = (com.sun.management.OperatingSystemMXBean) os_mxb;
			jo_os.addProperty("getCommittedVirtualMemorySize", os_sun.getCommittedVirtualMemorySize());
			jo_os.addProperty("getFreePhysicalMemorySize", os_sun.getFreePhysicalMemorySize());
			jo_os.addProperty("getFreeSwapSpaceSize", os_sun.getFreeSwapSpaceSize());
			jo_os.addProperty("getProcessCpuLoad", os_sun.getProcessCpuLoad());
			jo_os.addProperty("getProcessCpuTime", os_sun.getProcessCpuTime());
			jo_os.addProperty("getSystemCpuLoad", os_sun.getSystemCpuLoad());
			jo_os.addProperty("getTotalPhysicalMemorySize", os_sun.getTotalPhysicalMemorySize());
			jo_os.addProperty("getTotalSwapSpaceSize", os_sun.getTotalSwapSpaceSize());
			result.add("os", jo_os);
		} catch (ClassNotFoundException e) {
		}
		
		return result;
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
			mutator.withRow(CF_INSTANCES, key).putColumn(CF_COLS.COL_PERFSTATS.toString(), getPerfStats().toString(), TTL);
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
	
	private static AppManager static_manager;
	
	/**
	 * @return an InstanceStatus gathered
	 */
	public static InstanceStatus getStatic() {
		if (static_manager == null) {
			static_manager = new AppManager("Gatherer");
		}
		return static_manager.getInstanceStatus();
	}
	
	/**
	 * @return raw Cassandra items.
	 */
	public JsonObject getByKeys(ArrayList<String> refs) {
		if (refs == null) {
			throw new NullPointerException("\"refs\" can't to be null");
		}
		
		final JsonObject result = new JsonObject();
		final JsonParser parser = new JsonParser();
		
		List<String> col_names = Arrays.asList(CF_COLS.COL_SUMMARY.toString(), CF_COLS.COL_ITEMS.toString());
		
		try {
			Rows<String, String> rows = keyspace.prepareQuery(CF_INSTANCES).getKeySlice(refs).withColumnSlice(col_names).execute().getResult();
			
			for (Row<String, String> row : rows) {
				JsonObject item = new JsonObject();
				ColumnList<String> cols = row.getColumns();
				for (Column<String> col : cols) {
					String value = col.getStringValue();
					if (value != null) {
						item.add(col.getName(), parser.parse(value));
					}
				}
				result.add(row.getKey(), item);
			}
		} catch (Exception e) {
			manager.getServiceException().onCassandraError(e);
		}
		
		if (refs.contains(getStatic().summary.getInstanceNamePid())) {
			InstanceStatus static_status = static_manager.getInstanceStatus();
			
			JsonObject item = new JsonObject();
			item.add(CF_COLS.COL_SUMMARY.toString(), AppManager.getSimpleGson().toJsonTree(static_status.summary));
			item.add(CF_COLS.COL_ITEMS.toString(), static_status.getItems());
			item.add(CF_COLS.COL_CLASSPATH.toString(), static_status.getClasspath());
			item.add(CF_COLS.COL_PERFSTATS.toString(), static_status.getPerfStats());
			item.add(CF_COLS.COL_THREADS.toString(), InstanceStatus.getThreadstacktraces());
			result.add(static_status.summary.getInstanceNamePid(), item);
		}
		
		return result;
	}
	
}
