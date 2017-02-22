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
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
*/
package hd3gtv.mydmam.bcastautomation;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import com.google.common.primitives.Longs;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.serializers.StringSerializer;

import hd3gtv.configuration.ConfigurationItem;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.bcastautomation.TimedEventStore.TimedEvent;
import hd3gtv.mydmam.db.CassandraDb;

final class BCAAutomationEventProcessor implements BCAAutomationEventHandler {
	
	private MessageDigest md;
	private TimedEvent t_event;
	private HashSet<String> actual_event_list;
	private HashSet<String> actual_event_pause_automation_list;
	private TimedEventStore database;
	private long max_retention_duration;
	private HashMap<String, ConfigurationItem> import_other_properties_configuration;
	private String cf_name;
	private boolean is_first_list_event;
	
	BCAAutomationEventProcessor(String cf_name, long max_retention_duration, HashMap<String, ConfigurationItem> import_other_properties_configuration) throws ConnectionException {
		this.max_retention_duration = max_retention_duration;
		this.import_other_properties_configuration = import_other_properties_configuration;
		this.cf_name = cf_name;
		
		database = new TimedEventStore(CassandraDb.getkeyspace(), cf_name, max_retention_duration);
		actual_event_list = new HashSet<>();
		actual_event_pause_automation_list = new HashSet<>();
		is_first_list_event = true;
		
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
		}
	}
	
	void clearEventList() throws Exception {
		actual_event_list.clear();
		actual_event_pause_automation_list.clear();
		is_first_list_event = true;
	}
	
	void preparePurgeEventList() throws Exception {
		database.getAllKeys(future -> {
			actual_event_list.add(future);
		}, aired_key -> {
		}, nonaired_key -> {
			actual_event_list.add(nonaired_key);
		}, pauseautomation_key -> {
			actual_event_pause_automation_list.add(pauseautomation_key);
		});
		Loggers.BroadcastAutomation.debug("Event list as currently " + actual_event_list.size() + " future and non-aired events");
	}
	
	public void onAutomationEvent(BCAAutomationEvent event) {
		if (event.getStartDate() < (System.currentTimeMillis() - max_retention_duration)) {
			/**
			 * Too old event (normally an as-run)
			 */
			if (Loggers.BroadcastAutomation.isTraceEnabled()) {
				Loggers.BroadcastAutomation.trace("Process event: event \"" + event.getName() + "\" at the " + new Date(event.getStartDate()) + " is too old. It will not be added to database. "
						+ event.serialize(import_other_properties_configuration).toString());
			}
			return;
		}
		/**
		 * Compute event unique key
		 */
		md.reset();
		if (event.isRecording()) {
			md.update(Longs.toByteArray(1l));
		} else {
			md.update(Longs.toByteArray(0l));
		}
		if (event.isAutomationPaused()) {
			md.update(Longs.toByteArray(1l));
		} else {
			md.update(Longs.toByteArray(0l));
		}
		md.update(event.getAutomationId().getBytes());
		md.update(event.getChannel().getBytes());
		md.update(event.getSOM().toString().getBytes());
		md.update(event.getDuration().toString().getBytes());
		md.update(Longs.toByteArray(event.getStartDate()));
		md.update(event.getVideoSource().getBytes());
		md.update(event.getMaterialType().getBytes());
		md.update(event.getFileId().getBytes());
		md.update(event.getName().getBytes());
		md.update(event.getComment().getBytes());
		if (import_other_properties_configuration != null) {
			md.update(event.getOtherProperties(import_other_properties_configuration).toString().getBytes());
		}
		String event_key = MyDMAM.byteToString(md.digest());
		
		if (event.isPlaylist() | event.isOnair()) {
			if (actual_event_list.contains(event_key) || (actual_event_pause_automation_list.contains(event_key) && is_first_list_event)) {
				if (Loggers.BroadcastAutomation.isTraceEnabled()) {
					Loggers.BroadcastAutomation.trace("Process event: event [" + event_key + "] \"" + event.getName() + "\" at the " + new Date(event.getStartDate())
							+ " is already added in database. " + event.serialize(import_other_properties_configuration).toString());
				}
				actual_event_list.remove(event_key);
				actual_event_pause_automation_list.remove(event_key);
				return;
			}
		} else if (event.isAsrun() && event.isAutomationPaused() && is_first_list_event) {
			if (actual_event_pause_automation_list.contains(event_key)) {
				if (Loggers.BroadcastAutomation.isTraceEnabled()) {
					Loggers.BroadcastAutomation.trace("Process event: event [" + event_key + "] \"" + event.getName() + "\" at the " + new Date(event.getStartDate())
							+ " is already added in database (it's a \"too long time\" pause automation event). " + event.serialize(import_other_properties_configuration).toString());
				}
				actual_event_pause_automation_list.remove(event_key);
				return;
			}
		}
		
		/**
		 * Create event db pusher
		 */
		if (t_event == null) {
			try {
				t_event = database.createEvent(event_key, event.getStartDate(), event.getLongDuration());
			} catch (ConnectionException e) {
				Loggers.BroadcastAutomation.warn("Can't push to database", e);
				return;
			}
		} else {
			t_event = t_event.createAnother(event_key, event.getStartDate(), event.getLongDuration());
		}
		
		/**
		 * Push to database
		 */
		t_event.getMutator().putColumn(BCAWatcher.DB_COL_CONTENT_NAME, event.serialize(import_other_properties_configuration).toString());
		
		if (is_first_list_event) {
			if (event.isAutomationPaused() && (event.isAsrun() || event.isOnair())) {
				/**
				 * The playlist has an event that blocks time: it will be marked.
				 * In this case, the event can be detected as As-run, because if the actual duration time is more than the planned duration time.
				 */
				Loggers.BroadcastAutomation.debug("Detect automation paused on \"" + event.getName() + "\"");
				t_event.setPauseAutomationEvent();
				actual_event_pause_automation_list.remove(event_key);
			}
			
			is_first_list_event = false;
		}
		
		if (Loggers.BroadcastAutomation.isTraceEnabled()) {
			Loggers.BroadcastAutomation.trace("Process event: event [" + event_key + "] \"" + event.getName() + "\" at the " + new Date(event.getStartDate()) + " will be added in database. "
					+ event.serialize(import_other_properties_configuration).toString());
		}
	}
	
	void endsOperation() throws ConnectionException {
		if (t_event != null) {
			t_event.close();
		}
		
		if (actual_event_list.isEmpty() == false) {
			Loggers.BroadcastAutomation.debug("Start purge obsolete " + actual_event_list.size() + " event(s)");
			
			MutationBatch mutator = CassandraDb.prepareMutationBatch(CassandraDb.getDefaultKeyspacename());
			ColumnFamily<String, String> cf = new ColumnFamily<String, String>(cf_name, StringSerializer.get(), StringSerializer.get());
			
			actual_event_list.forEach((event_key) -> {
				Loggers.BroadcastAutomation.trace("Process event: clean obsolete event [" + event_key + "] from database");
				mutator.withRow(cf, event_key).delete();
			});
			
			/**
			 * Destroy all pause auto events if the playlist resumed the time
			 */
			actual_event_pause_automation_list.forEach((event_key) -> {
				Loggers.BroadcastAutomation.trace("Process event: clean obsolete pause-automation event [" + event_key + "] from database");
				mutator.withRow(cf, event_key).delete();
			});
			
			mutator.execute();
		}
	}
}