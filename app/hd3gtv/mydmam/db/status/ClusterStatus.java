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
package hd3gtv.mydmam.db.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClusterStatus {
	
	final static String NEW_LINE = System.getProperty("line.separator");
	
	public enum ClusterType {
		CASSANDRA, ELASTICSEARCH
	}
	
	private enum Gravity {
		WARN, ERROR, RECOVERED
	}
	
	ElasticsearchStatus es_status;
	private List<ClusterStatusEvents> callbacks_events;
	private final LinkedHashMap<Gravity, LinkedHashMap<Class<?>, List<String>>> all_messages;
	
	public ClusterStatus() {
		callbacks_events = new ArrayList<ClusterStatusEvents>();
		es_status = new ElasticsearchStatus(this);
		all_messages = new LinkedHashMap<Gravity, LinkedHashMap<Class<?>, List<String>>>();
	}
	
	public ClusterStatus refresh(boolean prepare_reports) {
		all_messages.clear();
		es_status.refreshStatus(prepare_reports);
		callbacksAll();
		return this;
	}
	
	public ClusterStatus addCallbackEvent(ClusterStatusEvents event) {
		if (event == null) {
			return this;
		}
		if (callbacks_events.contains(event)) {
			return this;
		}
		callbacks_events.add(event);
		return this;
	}
	
	private void callbacksAll() {
		if (all_messages.isEmpty()) {
			return;
		}
		
		for (int pos = 0; pos < callbacks_events.size(); pos++) {
			// TODO
			// callbacks_events.get(pos).clusterHasAWarningState(e);
		}
	}
	
	private void onSomething(Gravity gravity, Class<?> provider, String message) {
		LinkedHashMap<Class<?>, List<String>> messages_by_gravity;
		
		if (all_messages.containsKey(gravity) == false) {
			messages_by_gravity = new LinkedHashMap<Class<?>, List<String>>();
			all_messages.put(gravity, messages_by_gravity);
		} else {
			messages_by_gravity = all_messages.get(gravity);
		}
		
		List<String> messages;
		if (messages_by_gravity.containsKey(provider) == false) {
			messages = new ArrayList<String>();
			messages_by_gravity.put(provider, messages);
		} else {
			messages = messages_by_gravity.get(provider);
		}
		
		messages.add(message);
	}
	
	void onWarning(Class<?> provider, String message) {
		onSomething(Gravity.WARN, provider, message);
	}
	
	void onRecovered(Class<?> provider, String message) {
		onSomething(Gravity.RECOVERED, provider, message);
	}
	
	void onGrave(Class<?> provider, String message) {
		onSomething(Gravity.ERROR, provider, message);
	}
	
	public Map<ClusterType, Map<String, StatusReport>> getAllReports() {
		Map<ClusterType, Map<String, StatusReport>> response = new HashMap<ClusterType, Map<String, StatusReport>>(2);
		response.put(ClusterType.ELASTICSEARCH, es_status.last_status_reports);
		return response;
	}
	
	public String getAllReportsToCSVString() {
		StringBuffer sb = new StringBuffer();
		
		for (Map.Entry<ClusterType, Map<String, StatusReport>> entry : getAllReports().entrySet()) {
			// entry.getKey() entry.getValue()
			sb.append("############# ");
			sb.append(entry.getKey().name());
			sb.append(" #############");
			sb.append(NEW_LINE);
			for (Map.Entry<String, StatusReport> report : entry.getValue().entrySet()) {
				sb.append("=== ");
				sb.append(report.getKey());
				sb.append(" ===");
				sb.append(NEW_LINE);
				sb.append(report.getValue().toCSVString());
			}
		}
		return sb.toString();
	}
	
}
