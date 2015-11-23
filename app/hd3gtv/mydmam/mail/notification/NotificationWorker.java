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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.indices.IndexMissingException;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.manager.AppManager;
import hd3gtv.mydmam.manager.CyclicJobCreator;
import hd3gtv.mydmam.manager.JobContext;
import hd3gtv.mydmam.manager.JobProgression;
import hd3gtv.mydmam.manager.WorkerCapablities;
import hd3gtv.mydmam.manager.WorkerNG;

public class NotificationWorker extends WorkerNG {
	
	static {
		try {
			Elasticsearch.enableTTL(Notification.ES_INDEX, Notification.ES_DEFAULT_TYPE);
		} catch (IOException e) {
			Loggers.Mail.error("Can't to set TTL for ES", e);
		}
	}
	
	private static long grace_period_duration = 1000 * 3600 * 24;
	
	public NotificationWorker(AppManager manager) throws Exception {
		if (isActivated()) {
			CyclicJobCreator cyclic_cleaner = new CyclicJobCreator(manager, 1, TimeUnit.HOURS, true);
			cyclic_cleaner.setOptions(getClass(), "Close old notifications", "Internal MyDMAM");
			cyclic_cleaner.add("Close old notifications", new JobContextNotificationClean());
			manager.register(cyclic_cleaner);
			
			CyclicJobCreator cyclic_alerter = new CyclicJobCreator(manager, 10, TimeUnit.MINUTES, false);
			cyclic_alerter.setOptions(getClass(), "Notifications alerter", "Internal MyDMAM");
			cyclic_alerter.add("Notifications alerter", new JobContextNotificationAlert());
			manager.register(cyclic_alerter);
		}
		
		manager.register(this);
	}
	
	public WorkerCategory getWorkerCategory() {
		return WorkerCategory.INTERNAL;
	}
	
	public String getWorkerLongName() {
		return "Scan and send notifications to users";
	}
	
	public String getWorkerVendorName() {
		return "MyDMAM Internal";
	}
	
	public List<WorkerCapablities> getWorkerCapablities() {
		List<WorkerCapablities> r = WorkerCapablities.createList(JobContextNotificationAlert.class);
		r.addAll(WorkerCapablities.createList(JobContextNotificationClean.class));
		return r;
	}
	
	protected void workerProcessJob(JobProgression progression, JobContext context) throws Exception {
		try {
			int count = 0;
			if (context.getClass().equals(JobContextNotificationAlert.class)) {
				count = Notification.updateJobsEvolutionsForNotifications();
				
				if (count == 0) {
					progression.update("No notifications to send");
					return;
				}
				progression.update(count + " notifications(s) sended");
				
			} else if (context.getClass().equals(JobContextNotificationClean.class)) {
				count = Notification.updateOldsAndNonClosedNotifications(grace_period_duration);
				progression.update(count + " element(s) closed");
			}
		} catch (IndexMissingException e) {
			/**
			 * Empty Db, ignore this.
			 */
			progression.update("Database (ES) is not definited");
		}
	}
	
	protected void forceStopProcess() throws Exception {
	}
	
	protected boolean isActivated() {
		return Configuration.global.getValueBoolean("service", "notifications_scan");
	}
	
}
