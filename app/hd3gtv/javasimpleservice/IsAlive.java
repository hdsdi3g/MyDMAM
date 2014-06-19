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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.javasimpleservice;

import hd3gtv.log2.Log2;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.mail.AdminMailAlert;
import hd3gtv.tools.TimeUtils;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.query.AllRowsQuery;
import com.netflix.astyanax.serializers.StringSerializer;

public class IsAlive extends Thread {
	
	public static final ColumnFamily<String, String> CF_WORKERS = new ColumnFamily<String, String>("workers", StringSerializer.get(), StringSerializer.get());
	
	private ServiceManager manager;
	private boolean stopthread;
	private int period;
	
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
				
				mutator.execute();
				
				sleep(period * 1000);
			}
		} catch (Exception e) {
			Log2.log.error("Is alive fatal error", e);
			AdminMailAlert.create("IsAlive error", true).setThrowable(e).send();
		}
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
			ja_result.add(jo);
		}
		
		return ja_result;
	}
}
