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

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.manager.WorkerNG.WorkerState;
import hd3gtv.mydmam.useraction.UACapabilityDefinition;
import hd3gtv.mydmam.useraction.UAConfigurator;
import hd3gtv.mydmam.useraction.UAFunctionalityDefinintion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class AppManager {
	
	/**
	 * In sec.
	 */
	private static final int SLEEP_UPDATE_TTL = 60;
	
	/**
	 * ============================================================================
	 * Start of static realm
	 * ============================================================================
	 */
	private static final Gson gson;
	private static final Gson simple_gson;
	private static final Gson pretty_gson;
	static final long starttime;
	
	static {
		starttime = System.currentTimeMillis();
		GsonBuilder builder = new GsonBuilder();
		builder.serializeNulls();
		
		GsonIgnoreStrategy ignore_strategy = new GsonIgnoreStrategy();
		builder.addDeserializationExclusionStrategy(ignore_strategy);
		builder.addSerializationExclusionStrategy(ignore_strategy);
		
		/**
		 * Outside of this package serializers
		 */
		builder.registerTypeAdapter(UAFunctionalityDefinintion.class, new UAFunctionalityDefinintion.Serializer());
		builder.registerTypeAdapter(UACapabilityDefinition.class, new UACapabilityDefinition.Serializer());
		builder.registerTypeAdapter(UAConfigurator.class, new UAConfigurator.JsonUtils());
		builder.registerTypeAdapter(Class.class, new MyDMAM.GsonClassSerializer());
		simple_gson = builder.create();
		
		/**
		 * Inside of this package serializers
		 */
		builder.registerTypeAdapter(InstanceStatus.class, new InstanceStatus.Serializer());
		builder.registerTypeAdapter(JobNG.class, new JobNG.Serializer());
		builder.registerTypeAdapter(GsonThrowable.class, new GsonThrowable.Serializer());
		builder.registerTypeAdapter(WorkerCapablitiesStatus.class, new WorkerCapablitiesStatus.Serializer());
		builder.registerTypeAdapter(WorkerStatus.class, new WorkerStatus.Serializer());
		
		gson = builder.create();
		pretty_gson = builder.setPrettyPrinting().create();
	}
	
	static Gson getGson() {
		return gson;
	}
	
	static Gson getSimpleGson() {
		return simple_gson;
	}
	
	static Gson getPrettyGson() {
		return pretty_gson;
	}
	
	private volatile static HashMap<String, Class<?>> instance_class_name;
	private volatile static ArrayList<String> not_found_class_name;
	static {
		instance_class_name = new HashMap<String, Class<?>>();
		not_found_class_name = new ArrayList<String>();
	}
	
	static <T> T instanceClassForName(String class_name, Class<T> return_type) {
		try {
			if (not_found_class_name.contains(class_name)) {
				return null;
			}
			Class<?> item;
			if (instance_class_name.containsKey(class_name)) {
				item = instance_class_name.get(class_name);
			} else {
				item = Class.forName(class_name);
			}
			if (item.isAssignableFrom(return_type) == false) {
				Log2.log.error("Can't instanciate class", new ClassCastException(class_name));
				return null;
			}
			@SuppressWarnings("unchecked")
			T newinstance = (T) item.newInstance();
			instance_class_name.put(class_name, item);
			return newinstance;
		} catch (Exception e) {
			not_found_class_name.add(class_name);
			Log2.log.error("Can't load class", e, new Log2Dump("class_name", class_name));
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
	
	public AppManager() {
		instance_status = new InstanceStatus().populateFromThisInstance();
		service_exception = new ServiceException();
	}
	
	void workerRegister(WorkerNG worker) {
		if (worker == null) {
			throw new NullPointerException("\"worker\" can't to be null");
		}
		if (worker.isActivated() == false) {
			return;
		}
		worker.setWorker_exception(service_exception);
		enabled_workers.add(worker);
	}
	
	class ServiceException implements WorkerExceptionHandler {
		public void onError(Exception e, String error_name, WorkerNG worker) {
			// AdminMailAlert.create("Error during processing", false).addDump(job).addDump(worker).setServiceinformations(serviceinformations).send();// TODO alert
		}
		
		private void onAppManagerError(Exception e, String error_name) {
			// AdminMailAlert.create("Error during processing", false).addDump(job).addDump(worker).setServiceinformations(serviceinformations).send();// TODO alert
		}
		
		void onGenericServiceError(Exception e, String error_name, String service_name) {
			// AdminMailAlert.create("Error during processing", false).addDump(job).addDump(worker).setServiceinformations(serviceinformations).send();// TODO alert
		}
	}
	
	ServiceException getServiceException() {
		return service_exception;
	}
	
	public void startAll() {
		for (int pos = 0; pos < enabled_workers.size(); pos++) {
			enabled_workers.get(pos).getLifecyle().enable();
		}
		broker = new BrokerNG(this);
		broker.start();
		updater = new Updater();
		updater.start();
	}
	
	/**
	 * Blocking
	 */
	public void stopAll() {
		updater.stopUpdate();
		broker.askStop();
		
		for (int pos = 0; pos < enabled_workers.size(); pos++) {
			enabled_workers.get(pos).getLifecyle().askToStop();
		}
		try {
			for (int pos = 0; pos < enabled_workers.size(); pos++) {
				while (enabled_workers.get(pos).getLifecyle().getState() == WorkerState.PENDING_STOP) {
					Thread.sleep(10);
				}
			}
			while (updater.isAlive()) {
				Thread.sleep(10);
			}
			while (broker.isAlive()) {
				Thread.sleep(10);
			}
			updater = null;
		} catch (InterruptedException e) {
			service_exception.onAppManagerError(e, "Can't stop all services threads");
		}
	}
	
	Map<Class<? extends JobContext>, List<WorkerNG>> getAllCurrentWaitingWorkersByCapablitiesJobContextClasses() {
		Map<Class<? extends JobContext>, List<WorkerNG>> capablities_classes_workers = new HashMap<Class<? extends JobContext>, List<WorkerNG>>();
		WorkerNG worker;
		List<Class<? extends JobContext>> current_capablities;
		List<WorkerNG> workers_for_capablity;
		Class<? extends JobContext> current_capablity;
		
		for (int pos_wr = 0; pos_wr < enabled_workers.size(); pos_wr++) {
			worker = enabled_workers.get(pos_wr);
			if (worker.getLifecyle().getState() != WorkerState.WAITING) {
				continue;
			}
			current_capablities = worker.getWorkerCapablitiesJobContextClasses();
			if (current_capablities == null) {
				continue;
			}
			for (int pos_cc = 0; pos_cc < current_capablities.size(); pos_cc++) {
				current_capablity = current_capablities.get(pos_cc);
				if (capablities_classes_workers.containsKey(current_capablity) == false) {
					capablities_classes_workers.put(current_capablity, new ArrayList<WorkerNG>(1));
				}
				workers_for_capablity = capablities_classes_workers.get(current_capablity);
				if (workers_for_capablity.contains(worker) == false) {
					workers_for_capablity.add(worker);
				}
			}
		}
		return capablities_classes_workers;
	}
	
	public JobNG createJob(JobContext context) {
		try {
			return new JobNG(this, context);
		} catch (ClassNotFoundException e) {
			Log2.log.error("The context origin class is invalid, don't forget it will be (de)serialized.", e);
			return null;
		}
	}
	
	InstanceStatus getInstance_status() {
		return instance_status;
	}
	
	private class Updater extends Thread {
		boolean stop_update;
		
		public Updater() {
			setName("Updater for " + instance_status.getInstanceNamePid());
			setDaemon(true);
		}
		
		public void run() {
			stop_update = false;
			try {
				while (stop_update == false) {
					DatabaseLayer.updateInstanceStatus(instance_status);
					DatabaseLayer.updateWorkerStatus(enabled_workers);
					// TODO Queue/Broker (current job statuses) regular push
					// TODO InstanceAction regular pulls
					Thread.sleep(SLEEP_UPDATE_TTL * 1000);
				}
			} catch (Exception e) {
				service_exception.onAppManagerError(e, "Fatal updater error, need to restart it");
			}
		}
		
		public synchronized void stopUpdate() {
			this.stop_update = true;
		}
	}
}
