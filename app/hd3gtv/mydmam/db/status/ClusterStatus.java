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
import java.util.List;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.mail.AdminMailAlert;
import hd3gtv.mydmam.manager.AppManager;

public class ClusterStatus {
	
	public enum ClusterType {
		ELASTICSEARCH;
		
		static ClusterType fromClass(Class<?> referer) {
			if (referer == ElasticsearchStatus.class) {
				return ClusterType.ELASTICSEARCH;
			}
			return null;
		}
	}
	
	private enum Gravity {
		WARN, ERROR, RECOVERED;
		
		String getText() {
			if (this == WARN) {
				return "Warning";
			}
			if (this == ERROR) {
				return "Error";
			}
			if (this == RECOVERED) {
				return "Problem recovered";
			}
			return null;
		}
	}
	
	ElasticsearchStatus es_status;
	private List<ClusterStatusEvents> callbacks_events;
	
	private final List<MessageEvent> all_messages_events;
	
	private class MessageEvent {
		Gravity gravity;
		ClusterType provider;
		String message;
		
		MessageEvent(Gravity gravity, ClusterType provider, String message) {
			this.gravity = gravity;
			this.provider = provider;
			this.message = message;
		}
	}
	
	public ClusterStatus() {
		callbacks_events = new ArrayList<ClusterStatusEvents>();
		es_status = new ElasticsearchStatus(this);
		all_messages_events = new ArrayList<ClusterStatus.MessageEvent>();
	}
	
	protected void refresh(AppManager manager) {
		all_messages_events.clear();
		es_status.refreshStatus(false);
		
		if (all_messages_events.isEmpty()) {
			return;
		}
		
		StringBuffer sb;
		ArrayList<String> messages_list = new ArrayList<String>();
		for (Gravity gravity : Gravity.values()) {
			sb = new StringBuffer();
			for (MessageEvent message_event : all_messages_events) {
				if (message_event.gravity != gravity) {
					continue;
				}
				sb.append(" * ");
				sb.append(gravity.getText());
				sb.append(" from ");
				sb.append(message_event.provider);
				sb.append(": ");
				sb.append(message_event.message.trim());
				sb.append(MyDMAM.LINESEPARATOR);
			}
			if (sb.length() == 0) {
				continue;
			}
			for (int pos = 0; pos < callbacks_events.size(); pos++) {
				if (gravity == Gravity.ERROR) {
					callbacks_events.get(pos).clusterHasAGraveState(sb.toString().trim());
				} else if (gravity == Gravity.WARN) {
					callbacks_events.get(pos).clusterHasAWarningState(sb.toString().trim());
				} else if (gravity == Gravity.RECOVERED) {
					callbacks_events.get(pos).clusterIsFunctional(sb.toString().trim());
				}
			}
			messages_list.add(sb.toString().trim());
		}
		
		if (messages_list.isEmpty() == false) {
			Loggers.ClusterStatus.info("Status change: " + messages_list);
			
			AdminMailAlert.create("Watching cluster status, state is changing", false).addToMessagecontent(messages_list).setManager(manager).send();
		}
	}
	
	public ClusterStatus prepareReports() {
		es_status.refreshStatus(true);
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
	
	void onWarning(Class<?> provider, String message) {
		all_messages_events.add(new MessageEvent(Gravity.WARN, ClusterType.fromClass(provider), message));
	}
	
	void onRecovered(Class<?> provider, String message) {
		all_messages_events.add(new MessageEvent(Gravity.RECOVERED, ClusterType.fromClass(provider), message));
	}
	
	void onGrave(Class<?> provider, String message) {
		all_messages_events.add(new MessageEvent(Gravity.ERROR, ClusterType.fromClass(provider), message));
	}
	
	public ElasticsearchStatus getESLastStatus() {
		return es_status;
	}
	
}
