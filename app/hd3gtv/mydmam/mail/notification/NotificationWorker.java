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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.mydmam.mail.notification;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.taskqueue.Broker;
import hd3gtv.mydmam.taskqueue.CyclicCreateTasks;
import hd3gtv.mydmam.taskqueue.Job;
import hd3gtv.mydmam.taskqueue.Profile;
import hd3gtv.mydmam.taskqueue.Worker;
import hd3gtv.mydmam.taskqueue.WorkerGroup;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.indices.IndexMissingException;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

public class NotificationWorker extends Worker {
	
	static final Profile notification_clean_profile = new Profile("notification", "clean");
	static final Profile notification_alert_profile = new Profile("notification", "alert");
	private static long grace_period_duration = 1000 * 3600 * 24;
	
	private Cleaner cleaner;
	private Alerter alerter;
	
	public NotificationWorker(WorkerGroup workergroup) throws Exception {
		cleaner = new Cleaner();
		cleaner.period = 1000 * 3600; // 1 hour
		alerter = new Alerter();
		alerter.period = 1000 * 60 * 10; // 10 min
		
		if (workergroup != null) {
			workergroup.addWorker(this);
			workergroup.addCyclicWorker(cleaner);
			workergroup.addCyclicWorker(alerter);
		}
	}
	
	private class Cleaner implements CyclicCreateTasks {
		
		long period;
		
		public void createTasks() throws ConnectionException {
			Broker.publishTask("Close old notifications", notification_clean_profile, null, this, false, 0, null, false);
		}
		
		public long getInitialCyclicPeriodTasks() {
			return period;
		}
		
		public String getShortCyclicName() {
			return "notifications-cleaner";
		}
		
		public String getLongCyclicName() {
			return "Close old notifications";
		}
		
		public boolean isCyclicConfigurationAllowToEnabled() {
			return isConfigurationAllowToEnabled();
		}
		
		public boolean isPeriodDurationForCreateTasksCanChange() {
			return true;
		}
	}
	
	private class Alerter implements CyclicCreateTasks {
		
		long period;
		
		public void createTasks() throws ConnectionException {
			Broker.publishTask("Notifications alerter", notification_alert_profile, null, this, false, 0, null, false);
		}
		
		public long getInitialCyclicPeriodTasks() {
			return period;
		}
		
		public String getShortCyclicName() {
			return "notifications-alerter";
		}
		
		public String getLongCyclicName() {
			return "Notifications alerter";
		}
		
		public boolean isCyclicConfigurationAllowToEnabled() {
			return isConfigurationAllowToEnabled();
		}
		
		public boolean isPeriodDurationForCreateTasksCanChange() {
			return true;
		}
	}
	
	public void process(Job job) throws Exception {
		try {
			Elasticsearch.enableTTL(Notification.ES_INDEX, Notification.ES_DEFAULT_TYPE);
			
			int count = 0;
			if (job.getProfile().equals(notification_alert_profile)) {
				count = Notification.updateTasksJobsEvolutionsForNotifications();
				
				if (count == 0) {
					job.last_message = "No notifications to send";
					return;
				}
				job.last_message = count + " notifications(s) sended";
				
			} else if (job.getProfile().equals(notification_clean_profile)) {
				count = Notification.updateOldsAndNonClosedNotifications(grace_period_duration);
				job.last_message = count + " element(s) closed";
			}
		} catch (IndexMissingException e) {
			/**
			 * Empty Db, ignore this.
			 */
			job.last_message = "Database (ES) is not definited";
		}
	}
	
	public String getShortWorkerName() {
		return "notification";
	}
	
	public String getLongWorkerName() {
		return "Scan and send notifications to users";
	}
	
	public List<Profile> getManagedProfiles() {
		ArrayList<Profile> profiles = new ArrayList<Profile>();
		profiles.add(notification_alert_profile);
		profiles.add(notification_clean_profile);
		return profiles;
	}
	
	public synchronized void forceStopProcess() throws Exception {
	}
	
	public boolean isConfigurationAllowToEnabled() {
		return Configuration.global.getValueBoolean("service", "notifications_scan");
	}
}
