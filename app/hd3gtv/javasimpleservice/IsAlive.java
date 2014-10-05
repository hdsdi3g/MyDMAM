/*
 * This file is part of MyDMAM
 * 
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
*/
package hd3gtv.javasimpleservice;

import hd3gtv.log2.Log2;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.mail.AdminMailAlert;
import hd3gtv.mydmam.taskqueue.Worker;
import hd3gtv.mydmam.taskqueue.WorkerGroup;
import hd3gtv.mydmam.useraction.UAFunctionality;
import hd3gtv.mydmam.useraction.UAFunctionalityDefinintion;
import hd3gtv.mydmam.useraction.UAManager;
import hd3gtv.mydmam.useraction.UAWorker;
import hd3gtv.tools.TimeUtils;

import java.lang.reflect.Type;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonObject;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.query.AllRowsQuery;
import com.netflix.astyanax.serializers.StringSerializer;

@SuppressWarnings("unchecked")
public class IsAlive extends Thread {
	
	private static final ColumnFamily<String, String> CF_WORKERS = new ColumnFamily<String, String>("workers", StringSerializer.get(), StringSerializer.get());
	
	private ServiceManager manager;
	private boolean stopthread;
	private int period;
	private static Type useraction_functionality_list_typeOfT = new TypeToken<List<UAFunctionalityDefinintion>>() {
	}.getType();
	
	static {
		try {
			Keyspace keyspace = CassandraDb.getkeyspace();
			if (CassandraDb.isColumnFamilyExists(keyspace, CF_WORKERS.getName()) == false) {
				CassandraDb.createColumnFamilyString(CassandraDb.getDefaultKeyspacename(), CF_WORKERS.getName(), false);
			}
		} catch (ConnectionException e) {
			Log2.log.info("Can't init database access");
		}
	}
	
	public IsAlive(ServiceManager manager) throws Exception {
		this.manager = manager;
		this.setDaemon(true);
		this.setName("IsAlive");
		period = 60;
	}
	
	public synchronized void stopWatch() {
		this.stopthread = true;
	}
	
	public void run() {
		try {
			stopthread = false;
			
			for (int pos = 0; pos < 50; pos++) {
				/**
				 * Sleep 5 sec for wait the full app start.
				 */
				if (stopthread) {
					return;
				}
				sleep(100);
			}
			
			Log2.log.info("Start regular service instance status uploads");
			
			while (stopthread == false) {
				String workername = ServiceManager.getInstancename(true);
				
				MutationBatch mutator = CassandraDb.prepareMutationBatch();
				mutator.withRow(CF_WORKERS, workername).putColumn("app-name", manager.getApplicationName(), period * 2);
				mutator.withRow(CF_WORKERS, workername).putColumn("app-version", manager.getApplicationVersion(), period * 2);
				mutator.withRow(CF_WORKERS, workername).putColumn("java-uptime", manager.getJavaUptime(), period * 2);
				
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
			}
		} catch (Exception e) {
			Log2.log.error("Is alive fatal error", e);
			AdminMailAlert.create("IsAlive error", true).setThrowable(e).send();
		}
	}
	
	public static String getCurrentAvailabilitiesAsJsonString(ArrayList<String> privileges_for_user) throws ConnectionException {
		AllRowsQuery<String, String> all_rows = CassandraDb.getkeyspace().prepareQuery(CF_WORKERS).getAllRows().withColumnSlice("useraction_functionality_list");
		OperationResult<Rows<String, String>> rows = all_rows.execute();
		
		Map<String, List<UAFunctionalityDefinintion>> all = new HashMap<String, List<UAFunctionalityDefinintion>>();
		
		List<UAFunctionalityDefinintion> list;
		for (Row<String, String> row : rows.getResult()) {
			Column<String> col = row.getColumns().getColumnByName("useraction_functionality_list");
			if (col == null) {
				continue;
			}
			list = UAManager.getGson().fromJson(col.getStringValue(), useraction_functionality_list_typeOfT);
			
			if (privileges_for_user != null) {
				if (privileges_for_user.isEmpty() == false) {
					for (int pos = list.size() - 1; pos > -1; pos--) {
						if (privileges_for_user.contains(list.get(pos).classname) == false) {
							list.remove(pos);
						}
					}
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
			
			result_capability = (JsonObject) UAManager.getGson().toJsonTree(current.capability);
			result_capability.remove("musthavelocalstorageindexbridge");
			result_implementation.add("capability", result_capability);
			
			result_configurator = (JsonObject) UAManager.getGson().toJsonTree(current.configurator);
			result_configurator.remove("type");
			result_configurator.remove("origin");
			result_implementation.add("configurator", result_configurator);
			
			result.add(current.classname, result_implementation);
		}
		
		return UAManager.getGson().toJson(result);
	}
	
	public static JSONArray getLastStatusWorkers() throws Exception {
		JSONArray ja_result = new JSONArray();
		AllRowsQuery<String, String> all_rows = CassandraDb.getkeyspace().prepareQuery(CF_WORKERS).getAllRows();
		all_rows.setRepeatLastToken(false).setRowLimit(100);
		OperationResult<Rows<String, String>> rows = all_rows.execute();
		
		ColumnList<String> cols;
		for (Row<String, String> row : rows.getResult()) {
			cols = row.getColumns();
			JSONObject jo = new JSONObject();
			jo.put("workername", row.getKey());
			jo.put("appname", cols.getStringValue("app-name", ""));
			jo.put("appversion", cols.getStringValue("app-version", "?"));
			jo.put("javauptime", TimeUtils.secondsToYWDHMS(cols.getLongValue("java-uptime", 0l) / 1000));
			jo.put("stacktraces", (JSONArray) (new JSONParser()).parse(cols.getStringValue("stacktraces", "[]")));
			jo.put("javaversion", cols.getStringValue("java-version", ""));
			jo.put("javaaddress", (JSONObject) (new JSONParser()).parse(cols.getStringValue("java-address", "{}")));
			jo.put("useraction_functionality_list", (JSONArray) (new JSONParser()).parse(cols.getStringValue("useraction_functionality_list", "[]")));
			ja_result.add(jo);
		}
		
		return ja_result;
	}
}
