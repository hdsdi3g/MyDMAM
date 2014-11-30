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
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.manager.WorkerNG.WorkerState;
import hd3gtv.mydmam.useraction.UACapabilityDefinition;
import hd3gtv.mydmam.useraction.UAConfigurator;
import hd3gtv.mydmam.useraction.UAFunctionalityDefinintion;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class AppManager {
	
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
	private WorkerException worker_exception;
	private Updater updater;
	
	// TODO store a map with class name <-> class instance tested
	
	public AppManager() {
		instance_status = new InstanceStatus().populateFromThisInstance();
		worker_exception = new WorkerException();
	}
	
	void workerRegister(WorkerNG worker) {
		if (worker == null) {
			throw new NullPointerException("\"worker\" can't to be null");
		}
		if (worker.isActivated() == false) {
			return;
		}
		worker.setWorker_exception(worker_exception);
		enabled_workers.add(worker);
	}
	
	private class WorkerException implements WorkerExceptionHandler {
		public void onError(Exception e, String error_name, WorkerNG worker) {
			// AdminMailAlert.create("Error during processing", false).addDump(job).addDump(worker).setServiceinformations(serviceinformations).send();// TODO alert
		}
	}
	
	public void startAll() {
		for (int pos = 0; pos < enabled_workers.size(); pos++) {
			enabled_workers.get(pos).getLifecyle().enable();
		}
		// TODO start queue
		updater = new Updater();
		updater.start();
	}
	
	/**
	 * Blocking
	 */
	public void stopAll() {
		updater.stopUpdate();
		
		for (int pos = 0; pos < enabled_workers.size(); pos++) {
			enabled_workers.get(pos).getLifecyle().askToStop();
		}
		try {
			for (int pos = 0; pos < enabled_workers.size(); pos++) {
				while (enabled_workers.get(pos).getLifecyle().getStatus() == WorkerState.PENDING_STOP) {
					Thread.sleep(10);
				}
			}
			while (updater.isAlive()) {
				Thread.sleep(10);
			}
			updater = null;
		} catch (InterruptedException e) {
		}
		// TODO stop queue
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
		
		@Override
		public void run() {
			stop_update = false;
			while (stop_update == false) {
				// TODO InstanceAction regular pulls
				DatabaseLayer.updateInstanceStatus(instance_status);
				DatabaseLayer.updateWorkerStatus(enabled_workers);
			}
		}
		
		public synchronized void stopUpdate() {
			this.stop_update = true;
		}
	}
}
