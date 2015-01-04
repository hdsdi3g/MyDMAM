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
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.mail.AdminMailAlert;
import hd3gtv.mydmam.manager.WorkerNG.WorkerState;
import hd3gtv.mydmam.useraction.UACapabilityDefinition;
import hd3gtv.mydmam.useraction.UAConfigurator;
import hd3gtv.mydmam.useraction.UAFunctionalityDefinintion;
import hd3gtv.mydmam.useraction.UAWorker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.netflix.astyanax.MutationBatch;

public final class AppManager implements InstanceActionReceiver {
	
	// TODO #78.1 Refactoring User Action API and display all availability for user and admin
	// TODO #78.2, check if UA capacity need no read only for storage
	
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
		builder.registerTypeAdapter(InstanceAction.class, new InstanceAction.Serializer());
		builder.registerTypeAdapter(JobNG.class, new JobNG.Serializer());
		builder.registerTypeAdapter(JobAction.class, new JobAction.Serializer());
		builder.registerTypeAdapter(GsonThrowable.class, new GsonThrowable.Serializer());
		builder.registerTypeAdapter(WorkerCapablitiesExporter.class, new WorkerCapablitiesExporter.Serializer());
		builder.registerTypeAdapter(WorkerExporter.class, new WorkerExporter.Serializer());
		
		builder.registerTypeAdapter(JobContext.class, new JobContext.Serializer());
		builder.registerTypeAdapter(new TypeToken<ArrayList<JobContext>>() {
		}.getType(), new JobContext.SerializerList());
		
		builder.registerTypeAdapter(JobCreatorDeclarationSerializer.class, new JobCreatorDeclarationSerializer());
		builder.registerTypeAdapter(TriggerJobCreator.class, TriggerJobCreator.serializer);
		builder.registerTypeAdapter(CyclicJobCreator.class, CyclicJobCreator.serializer);
		
		gson = builder.create();
		pretty_gson = builder.setPrettyPrinting().create();
	}
	
	public static Gson getGson() {
		return gson;
	}
	
	public static Gson getSimpleGson() {
		return simple_gson;
	}
	
	public static Gson getPrettyGson() {
		return pretty_gson;
	}
	
	private volatile static HashMap<String, Class<?>> instance_class_name;
	private volatile static ArrayList<String> not_found_class_name;
	static {
		instance_class_name = new HashMap<String, Class<?>>();
		not_found_class_name = new ArrayList<String>();
	}
	
	public static boolean isClassForNameExists(String class_name) {
		try {
			if (not_found_class_name.contains(class_name)) {
				return false;
			}
			if (instance_class_name.containsKey(class_name) == false) {
				instance_class_name.put(class_name, Class.forName(class_name));
			}
			return true;
		} catch (Exception e) {
			not_found_class_name.add(class_name);
		}
		return false;
	}
	
	public static <T> T instanceClassForName(String class_name, Class<T> return_type) {
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
	private DatabaseLayer database_layer;
	private String app_name;
	
	public AppManager(String app_name) {
		this.app_name = app_name;
		service_exception = new ServiceException(this);
		database_layer = new DatabaseLayer(this);
		enabled_workers = new ArrayList<WorkerNG>();
		broker = new BrokerNG(this);
		instance_status = new InstanceStatus().populateFromThisInstance(this);
		updater = new Updater();
	}
	
	String getAppName() {
		return app_name;
	}
	
	BrokerNG getBroker() {
		return broker;
	}
	
	DatabaseLayer getDatabaseLayer() {
		return database_layer;
	}
	
	public void workerRegister(WorkerNG worker) {
		if (worker == null) {
			throw new NullPointerException("\"worker\" can't to be null");
		}
		if (worker.isActivated() == false) {
			return;
		}
		worker.setManager(this);
		enabled_workers.add(worker);
	}
	
	public void cyclicJobsRegister(CyclicJobCreator cyclic_creator) {
		if (cyclic_creator == null) {
			throw new NullPointerException("\"cyclic_creator\" can't to be null");
		}
		broker.getDeclared_cyclics().add(cyclic_creator);
	}
	
	public void triggerJobsRegister(TriggerJobCreator trigger_creator) {
		if (trigger_creator == null) {
			throw new NullPointerException("\"trigger_creator\" can't to be null");
		}
		broker.getDeclared_triggers().add(trigger_creator);
	}
	
	class ServiceException {
		private AppManager manager;
		
		private ServiceException(AppManager manager) {
			this.manager = manager;
		}
		
		public void onError(Exception e, String error_name, WorkerNG worker) {
			AdminMailAlert alert = AdminMailAlert.create(error_name, false).setManager(manager).setThrowable(e);
			alert.addDump(worker.getExporter()).send();
		}
		
		private void onAppManagerError(Exception e, String error_name) {
			AdminMailAlert alert = AdminMailAlert.create(error_name, true).setManager(manager).setThrowable(e);
			alert.addDump(instance_status).send();
		}
		
		void onQueueServiceError(Exception e, String error_name, String service_name) {
			AdminMailAlert alert = AdminMailAlert.create(error_name, false).setManager(manager).setThrowable(e);
			alert.addToMessagecontent("Service name: " + service_name);
			alert.addDump(instance_status).send();
		}
		
		void onCassandraError(Exception e) {
			AdminMailAlert alert = AdminMailAlert.create("Cassandra error", false).setManager(manager).setThrowable(e);
			alert.addDump(instance_status).send();
		}
		
		void onQueueJobProblem(String error_name, List<JobNG> jobs) {
			AdminMailAlert alert = AdminMailAlert.create(error_name, false).setManager(manager);
			for (int pos = 0; pos < jobs.size(); pos++) {
				alert.addDump(jobs.get(pos));
			}
			alert.addDump(instance_status).send();
		}
		
		void onMaxExecJobTime(JobNG job) {
			AdminMailAlert alert = AdminMailAlert.create("A job has an execution time too long", false).setManager(manager);
			alert.addDump(job);
			alert.addDump(instance_status).send();
		}
	}
	
	ServiceException getServiceException() {
		return service_exception;
	}
	
	public void startAll() {
		for (int pos = 0; pos < enabled_workers.size(); pos++) {
			enabled_workers.get(pos).getLifecyle().enable();
		}
		if (broker == null) {
			broker = new BrokerNG(this);
		}
		broker.start();
		if (updater == null) {
			updater = new Updater();
		}
		updater.start();
	}
	
	/**
	 * Don't start broker and workers.
	 */
	public void startJustService() {
		if (broker == null) {
			broker = new BrokerNG(this);
		}
		if (updater == null) {
			updater = new Updater();
		}
		updater.start();
	}
	
	/**
	 * Blocking
	 */
	public void stopAll() {
		if (updater != null) {
			updater.stopUpdate();
		}
		if (broker != null) {
			broker.askStop();
		}
		
		for (int pos = 0; pos < enabled_workers.size(); pos++) {
			enabled_workers.get(pos).getLifecyle().askToStopAndRefuseNewJobs();
		}
		try {
			for (int pos = 0; pos < enabled_workers.size(); pos++) {
				while (enabled_workers.get(pos).getLifecyle().getState() == WorkerState.PENDING_STOP) {
					Thread.sleep(10);
				}
			}
			
			if (updater != null) {
				while (updater.isAlive()) {
					Thread.sleep(10);
				}
			}
			updater = null;
			
			if (broker != null) {
				while (broker.isAlive()) {
					Thread.sleep(10);
				}
			}
			broker = null;
		} catch (InterruptedException e) {
			service_exception.onAppManagerError(e, "Can't stop all services threads");
		}
	}
	
	boolean isWorkingToShowUIStatus() {
		for (int pos = 0; pos < enabled_workers.size(); pos++) {
			if (enabled_workers.get(pos).getLifecyle().getState() == WorkerState.PROCESSING) {
				return true;
			}
			if (enabled_workers.get(pos).getLifecyle().getState() == WorkerState.PENDING_STOP) {
				return true;
			}
		}
		
		return false;
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
	
	List<UAWorker> getAllActiveUAWorkers() {
		ArrayList<UAWorker> uaworkers = new ArrayList<UAWorker>();
		WorkerNG worker;
		
		for (int pos_wr = 0; pos_wr < enabled_workers.size(); pos_wr++) {
			worker = enabled_workers.get(pos_wr);
			if (worker.getLifecyle().isThisState(WorkerState.WAITING, WorkerState.PROCESSING) == false) {
				continue;
			}
			// TODO #78.2, migrate UAWorker to WorkerNG
			/*if ((worker instanceof WorkerNG) == false) {
				continue;
			}
			uaworkers.add((WorkerNG) worker);*/
		}
		return uaworkers;
	}
	
	public static JobNG createJob(JobContext context) {
		try {
			return new JobNG(context);
		} catch (ClassNotFoundException e) {
			Log2.log.error("The context origin class is invalid, don't forget it will be (de)serialized.", e);
			return null;
		}
	}
	
	public InstanceStatus getInstance_status() {
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
				List<InstanceAction> pending_actions = new ArrayList<InstanceAction>();
				
				while (stop_update == false) {
					// TODO #78.4, add next refresh date
					// TODO #78.4, keep duration rotative "while" Threads (min/moy/max values). Warn if too long ?
					database_layer.updateInstanceStatus(instance_status.refresh());
					database_layer.updateWorkerStatus(enabled_workers);
					
					for (int pos = 0; pos < SLEEP_COUNT_UPDATE; pos++) {
						if (stop_update) {
							return;
						}
						
						InstanceAction.getAllPendingInstancesAction(pending_actions);
						if (pending_actions.isEmpty() == false) {
							boolean pending_refresh = processInstanceAction(pending_actions);
							if (pending_refresh & (stop_update == false)) {
								Thread.sleep(1000);
								database_layer.updateInstanceStatus(instance_status.refresh());
								database_layer.updateWorkerStatus(enabled_workers);
							}
						}
						
						Thread.sleep(SLEEP_BASE_TIME_UPDATE * 1000);
					}
				}
			} catch (Exception e) {
				service_exception.onAppManagerError(e, "Fatal updater error, need to restart it");
			}
		}
		
		public synchronized void stopUpdate() {
			this.stop_update = true;
		}
		
		boolean processInstanceAction(List<InstanceAction> pending_actions) throws Exception {
			boolean is_action = false;
			InstanceAction current_instance_action;
			String target_class_name;
			String ref_key;
			JsonObject order;
			ArrayList<CyclicJobCreator> declared_cyclics = broker.getDeclared_cyclics();
			ArrayList<TriggerJobCreator> declared_triggers = broker.getDeclared_triggers();
			
			MutationBatch mutator = CassandraDb.prepareMutationBatch();
			
			for (int pos_pa = 0; pos_pa < pending_actions.size(); pos_pa++) {
				current_instance_action = pending_actions.get(pos_pa);
				target_class_name = current_instance_action.getTargetClassname();
				ref_key = current_instance_action.getTarget_reference_key();
				order = current_instance_action.getOrder();
				
				if (target_class_name.equals(AppManager.class.getSimpleName())) {
					if (ref_key.equals(instance_status.getInstanceNamePid())) {
						Log2.log.info("Do an instance action on manager", current_instance_action);
						doAnAction(current_instance_action.getOrder());
						current_instance_action.delete(mutator);
						is_action = true;
					}
					
				} else if (target_class_name.equals(WorkerNG.class.getSimpleName())) {
					for (int pos_wr = 0; pos_wr < enabled_workers.size(); pos_wr++) {
						if (ref_key.equals(enabled_workers.get(pos_wr).getReferenceKey())) {
							Log2.log.info("Do an instance action on worker", current_instance_action);
							enabled_workers.get(pos_wr).doAnAction(order);
							current_instance_action.delete(mutator);
							is_action = true;
							break;
						}
					}
				} else if (target_class_name.equals(CyclicJobCreator.class.getSimpleName())) {
					for (int pos_dc = 0; pos_dc < declared_cyclics.size(); pos_dc++) {
						if (ref_key.equals(declared_cyclics.get(pos_dc).getReference_key())) {
							Log2.log.info("Do an instance action on cyclic", current_instance_action);
							declared_cyclics.get(pos_dc).doAnAction(order);
							current_instance_action.delete(mutator);
							is_action = true;
							break;
						}
					}
				} else if (target_class_name.equals(TriggerJobCreator.class.getSimpleName())) {
					for (int pos_dt = 0; pos_dt < declared_triggers.size(); pos_dt++) {
						if (ref_key.equals(declared_triggers.get(pos_dt).getReference_key())) {
							Log2.log.info("Do an instance action on trigger", current_instance_action);
							declared_triggers.get(pos_dt).doAnAction(order);
							current_instance_action.delete(mutator);
							is_action = true;
							break;
						}
					}
				} else {
					Log2.log.error("An instance action is not plugged to a known class", new ClassNotFoundException(target_class_name), current_instance_action);
				}
			}
			
			if (mutator.isEmpty() == false) {
				mutator.execute();
			}
			return is_action;
		}
	}
	
	public void doAnAction(JsonObject order) {
		if (order.has("broker")) {
			if (order.get("broker").getAsString().equals("start")) {
				broker.start();
			} else if (order.get("broker").getAsString().equals("stop")) {
				broker.askStop();
			}
		}
	}
}
