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

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.mail.AdminMailAlert;
import hd3gtv.mydmam.manager.WorkerNG.WorkerState;
import hd3gtv.tools.StoppableThread;

public final class AppManager implements InstanceActionReceiver, InstanceStatusItem {
	
	/**
	 * In sec.
	 */
	private static final int SLEEP_BASE_TIME_UPDATE = 10;
	private static final int SLEEP_COUNT_UPDATE = 6;
	
	/**
	 * ============================================================================
	 * Start of static realm
	 * ============================================================================
	 */
	static final long starttime;
	
	static {
		starttime = ManagementFactory.getRuntimeMXBean().getStartTime();
	}
	
	private volatile static HashMap<String, Class<?>> instance_class_name;
	private volatile static ArrayList<String> not_found_class_name;
	
	static {
		instance_class_name = new HashMap<String, Class<?>>();
		not_found_class_name = new ArrayList<String>();
	}
	
	public static boolean isClassForNameExists(String class_name) {// TODO move to Factory
		try {
			if (not_found_class_name.contains(class_name)) {
				return false;
			}
			if (instance_class_name.containsKey(class_name) == false) {
				instance_class_name.put(class_name, MyDMAM.factory.getClassByName(class_name));
			}
			return true;
		} catch (Exception e) {
			not_found_class_name.add(class_name);
		}
		return false;
	}
	
	public static <T> T instanceClassForName(String class_name, Class<T> return_type) {// TODO move to Factory
		try {
			if (isClassForNameExists(class_name) == false) {
				throw new ClassNotFoundException(class_name);
			}
			Class<?> item = instance_class_name.get(class_name);
			if (return_type.isAssignableFrom(item) == false) {
				throw new ClassCastException(item.getName() + " by " + return_type.getName());
			}
			@SuppressWarnings("unchecked")
			T newinstance = (T) item.newInstance();
			instance_class_name.put(class_name, item);
			return newinstance;
		} catch (Exception e) {
			if (not_found_class_name.contains(class_name) == false) {
				not_found_class_name.add(class_name);
			}
			if (instance_class_name.containsKey(class_name)) {
				instance_class_name.remove(class_name);
			}
			Loggers.Manager.error("Can't load class: " + class_name, e);
		}
		return null;
	}
	
	/**
	 * ============================================================================
	 * End of static realm
	 * ============================================================================
	 */
	
	/**
	 * All configured workers.
	 */
	private volatile List<WorkerNG> enabled_workers;
	
	private InstanceStatus instance_status;
	private ServiceException service_exception;
	private Updater updater;
	private BrokerNG broker;
	private String app_name;
	private ArrayList<InstanceActionReceiver> all_instance_action_receviers;
	
	public AppManager() {
		this.app_name = "Manager";
		all_instance_action_receviers = new ArrayList<InstanceActionReceiver>();
		all_instance_action_receviers.add(this);
		service_exception = new ServiceException(this);
		enabled_workers = new ArrayList<WorkerNG>();
		broker = new BrokerNG(this);
		instance_status = new InstanceStatus(this);
		updater = new Updater(this);
	}
	
	/**
	 * @return may be null
	 */
	String getAppName() {
		return app_name;
	}
	
	void setAppName(String app_name) {
		this.app_name = app_name;
		if (app_name == null) {
			throw new NullPointerException("\"app_name\" can't to be null");
		}
	}
	
	public void register(WorkerNG worker) {
		if (worker == null) {
			throw new NullPointerException("\"worker\" can't to be null");
		}
		if (worker.isActivated() == false) {
			return;
		} else {
			if (broker.isAlive() == false) {
				broker.start();
			}
		}
		worker.setManager(this);
		enabled_workers.add(worker);
		all_instance_action_receviers.add(worker);
		
		Loggers.Manager.debug("Register worker: " + worker.toString());
	}
	
	public void register(CyclicJobCreator cyclic_creator) {
		if (broker.isAlive() == false) {
			broker.start();
		}
		
		broker.cyclicJobsRegister(cyclic_creator);
		all_instance_action_receviers.add(cyclic_creator);
	}
	
	public void register(TriggerJobCreator trigger_creator) {
		if (broker.isAlive() == false) {
			broker.start();
		}
		
		broker.triggerJobsRegister(trigger_creator);
		all_instance_action_receviers.add(trigger_creator);
	}
	
	public void registerInstanceActionReceiver(InstanceActionReceiver recevier) {
		if (recevier == null) {
			throw new NullPointerException("\"recevier\" can't to be null");
		}
		if (recevier.getClassToCallback() == null) {
			throw new NullPointerException("recevier callback class can't to be null");
		}
		if (recevier.getReferenceKey() == null) {
			throw new NullPointerException("recevier ReferenceKey can't to be null");
		}
		all_instance_action_receviers.add(recevier);
	}
	
	/*public static void unRegisterRecevier(InstanceActionReceiver recevier) {
		if (recevier == null) {
			throw new NullPointerException("\"recevier\" can't to be null");
		}
		all_receviers.remove(recevier);
	}*/
	
	class ServiceException {
		private AppManager manager;
		
		private ServiceException(AppManager manager) {
			this.manager = manager;
		}
		
		public void onError(Exception e, String error_name, WorkerNG worker, JobNG job) {
			LinkedHashMap<String, Object> log = new LinkedHashMap<String, Object>();
			log.put(">", error_name);
			log.put("worker", worker.getReferenceKey());
			log.put("job", job.toStringLight());
			Loggers.Manager.warn("Worker error: " + log, e);
			
			AdminMailAlert alert = AdminMailAlert.create(error_name, false).setManager(manager).setThrowable(e);
			alert.send();
		}
		
		private void onAppManagerError(Exception e, String error_name) {
			Loggers.Manager.warn("App manager error (" + error_name + "), a mail will be send", e);
			
			AdminMailAlert alert = AdminMailAlert.create(error_name, true).setManager(manager).setThrowable(e);
			alert.send();
		}
		
		void onQueueServiceError(Exception e, String error_name, String service_name) {
			Loggers.Manager.warn("Queue service error (" + error_name + ") for service \"" + service_name + "\"; a mail will be send", e);
			
			AdminMailAlert alert = AdminMailAlert.create(error_name, false).setManager(manager).setThrowable(e);
			alert.addToMessagecontent("Service name: " + service_name);
			alert.send();
		}
		
		void onCassandraError(Exception e) {
			Loggers.Manager.warn("Cassandra error", e);
			
			AdminMailAlert alert = AdminMailAlert.create("Cassandra error", false).setManager(manager).setThrowable(e);
			alert.send();
		}
		
		void onQueueJobProblem(String error_name, List<JobNG> jobs) {
			if (Loggers.Manager.isDebugEnabled()) {
				Loggers.Manager.debug("Queue job problem (" + error_name + ") a mail will be send");
				for (int pos = 0; pos < jobs.size(); pos++) {
					Loggers.Manager.trace("Job in queue:\t" + jobs.get(pos).toString());
				}
			}
			
			AdminMailAlert alert = AdminMailAlert.create(error_name, false).setManager(manager);
			alert.send();
		}
		
		void onMaxExecJobTime(JobNG job) {
			Loggers.Manager.warn("Max exec time for job: " + job);
			
			AdminMailAlert alert = AdminMailAlert.create("A job has an execution time too long", false).setManager(manager);
			alert.send();
		}
	}
	
	ServiceException getServiceException() {
		return service_exception;
	}
	
	public void startAll() {
		Loggers.Manager.debug("Start " + enabled_workers.size() + " worker(s)");
		
		if (enabled_workers.isEmpty() == false && broker.isAlive() == false) {
			broker.start();
		}
		
		for (int pos = 0; pos < enabled_workers.size(); pos++) {
			enabled_workers.get(pos).getLifecyle().enable();
		}
		
		if (updater == null) {
			updater = new Updater(this);
		}
		Loggers.Manager.debug("Start updater");
		updater.start();
	}
	
	/**
	 * Blocking
	 */
	public void stopAll() {
		if (updater != null) {
			Loggers.Manager.debug("Stop updater");
			updater.stopUpdate();
		}
		if (broker.isAlive()) {
			Loggers.Manager.debug("Stop broker");
			broker.askStop();
		}
		
		for (int pos = 0; pos < enabled_workers.size(); pos++) {
			Loggers.Manager.debug("Stop worker " + enabled_workers.get(pos));
			enabled_workers.get(pos).getLifecyle().askToStopAndRefuseNewJobs();
		}
		try {
			for (int pos = 0; pos < enabled_workers.size(); pos++) {
				Loggers.Manager.trace("Wait worker to stop... " + enabled_workers.get(pos));
				while (enabled_workers.get(pos).getLifecyle().getState() == WorkerState.PENDING_STOP) {
					Thread.sleep(1);
				}
			}
			
			if (updater != null) {
				Loggers.Manager.debug("Wait updater to stop...");
				while (updater.isAlive()) {
					Thread.sleep(1);
				}
			}
			updater = null;
			
			if (broker.isAlive()) {
				Loggers.Manager.debug("Wait broker to stop...");
				while (broker.isAlive()) {
					Thread.sleep(1);
				}
			}
		} catch (InterruptedException e) {
			service_exception.onAppManagerError(e, "Can't stop all services threads");
		}
	}
	
	boolean isWorkingToShowUIStatus() {
		for (int pos = 0; pos < enabled_workers.size(); pos++) {
			if (enabled_workers.get(pos).getLifecyle().getState() == WorkerState.PROCESSING) {
				Loggers.Manager.trace("isWorkingToShowUIStatus ? true");
				return true;
			}
			if (enabled_workers.get(pos).getLifecyle().getState() == WorkerState.PENDING_STOP) {
				Loggers.Manager.trace("isWorkingToShowUIStatus ? true");
				return true;
			}
		}
		
		Loggers.Manager.trace("isWorkingToShowUIStatus ? false");
		return false;
	}
	
	List<WorkerNG> getEnabledWorkers() {
		return Collections.unmodifiableList(enabled_workers);
	}
	
	public static JobNG createJob(JobContext context) {
		try {
			return new JobNG(context);
		} catch (ClassNotFoundException e) {
			Loggers.Manager.error("The context origin class (" + context.getClass() + ") is invalid, don't forget it will be (de)serialized.", e);
			return null;
		}
	}
	
	public InstanceStatus getInstanceStatus() {
		return instance_status;
	}
	
	private class Updater extends StoppableThread {
		AppManager referer;
		long next_refresh_date;
		
		public Updater(AppManager referer) {
			super("Updater for " + instance_status.summary.getInstanceNamePid());
			this.referer = referer;
			setLogger(Loggers.Manager);
		}
		
		public void run() {
			try {
				while (isWantToRun()) {
					next_refresh_date = System.currentTimeMillis() + (SLEEP_COUNT_UPDATE * SLEEP_BASE_TIME_UPDATE * 1000);
					
					instance_status.refresh();
					WorkerExporter.updateWorkerStatus(enabled_workers, referer);
					
					for (int pos = 0; pos < SLEEP_COUNT_UPDATE; pos++) {
						if (isWantToStop()) {
							return;
						}
						next_refresh_date = System.currentTimeMillis() + ((SLEEP_COUNT_UPDATE - pos) * SLEEP_BASE_TIME_UPDATE * 1000);
						
						boolean pending_actions = InstanceAction.performInstanceActions(all_instance_action_receviers);
						
						if (pending_actions & isWantToRun()) {
							next_refresh_date = System.currentTimeMillis() + ((SLEEP_COUNT_UPDATE - pos) * SLEEP_BASE_TIME_UPDATE * 1000) + 1000;
							stoppableSleep(1000);
							instance_status.refresh();
							WorkerExporter.updateWorkerStatus(enabled_workers, referer);
						}
						
						stoppableSleep(SLEEP_BASE_TIME_UPDATE * 1000);
					}
				}
			} catch (Exception e) {
				service_exception.onAppManagerError(e, "Fatal updater error, need to restart it");
			}
		}
		
		public synchronized void stopUpdate() {
			wantToStop();
			next_refresh_date = 0;
		}
		
	}
	
	static boolean isActuallyOffHours() {
		if (Configuration.global.isElementKeyExists("service", "fullhours") == false) {
			return false;
		}
		
		Calendar cal = Calendar.getInstance();
		int this_date = cal.get(Calendar.HOUR_OF_DAY);
		ArrayList<String> fullhours = Configuration.global.getValues("service", "fullhours", "0-24");
		String[] fullhour;
		for (int pos_fh = 0; pos_fh < fullhours.size(); pos_fh++) {
			fullhour = fullhours.get(pos_fh).trim().split("-");
			int start = 0;
			int end = 24;
			for (int pos_hr = 0; pos_hr < fullhour.length; pos_hr++) {
				if (pos_hr == 0) {
					start = Integer.parseInt(fullhour[pos_hr].trim());
				} else {
					end = Integer.parseInt(fullhour[pos_hr].trim());
				}
			}
			if ((this_date < start | this_date >= end) == false) {
				return false;
			}
		}
		return true;
	}
	
	public Class<? extends InstanceActionReceiver> getClassToCallback() {
		return AppManager.class;
	}
	
	public void doAnAction(JsonObject order) throws Exception {
		if (order.has("broker")) {
			if (order.get("broker").getAsString().equals("start")) {
				broker.start();
			} else if (order.get("broker").getAsString().equals("stop")) {
				broker.askStop();
			}
		}
	}
	
	public String getReferenceKey() {
		return instance_status.summary.getInstanceNamePid();
	}
	
	public synchronized JsonElement getInstanceStatusItem() {
		JsonObject jo = new JsonObject();
		jo.addProperty("brokeralive", broker.isAlive());
		jo.addProperty("is_off_hours", AppManager.isActuallyOffHours());
		
		if (updater == null) {
			jo.addProperty("next_updater_refresh_date", 0);
		} else {
			jo.addProperty("next_updater_refresh_date", updater.next_refresh_date);
		}
		return jo;
	}
	
	public Class<?> getInstanceStatusItemReferenceClass() {
		return AppManager.class;
	}
}
