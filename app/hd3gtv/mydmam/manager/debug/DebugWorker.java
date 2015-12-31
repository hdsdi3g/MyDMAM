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
 * Copyright (C) hdsdi3g for hd3g.tv 2015
 * 
*/
package hd3gtv.mydmam.manager.debug;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.manager.AppManager;
import hd3gtv.mydmam.manager.JobContext;
import hd3gtv.mydmam.manager.JobNG;
import hd3gtv.mydmam.manager.JobProgression;
import hd3gtv.mydmam.manager.WorkerCapablities;
import hd3gtv.mydmam.manager.WorkerNG;
import hd3gtv.mydmam.storage.Storage;

public class DebugWorker extends WorkerNG {
	
	// private static int sleep_time = 10;
	private static int nb_tasks_by_core = 1;
	private static File datalog;
	private static String instance_name;
	private static final Random random;
	
	private static String test_storage_name;
	private static ArrayList<JobNG> required_tests = new ArrayList<JobNG>();
	
	static {
		random = new Random();
		test_storage_name = Storage.getAllStoragesNames().get(0);
	}
	
	public static void declareWorkers(AppManager manager) throws Exception {
		if (Configuration.global.isElementKeyExists("service", "debug_multiple_workers") == false) {
			return;
		}
		if (Configuration.global.getValueBoolean("service", "debug_multiple_workers") == false) {
			return;
		}
		
		int cores = Runtime.getRuntime().availableProcessors();
		for (int pos = 0; pos < cores; pos++) {
			manager.register(new DebugWorker());
		}
		
		datalog = new File("debug_worker.log");
		instance_name = manager.getInstanceStatus().summary.getInstanceNamePid();
		
		FileUtils.writeStringToFile(datalog, Loggers.dateLog(System.currentTimeMillis()) + "\tinit\t" + instance_name + "\t\t\n", true);
		
		for (int pos = 0; pos < cores; pos++) {
			for (int pos_c = 0; pos_c < nb_tasks_by_core; pos_c++) {
				required_tests.add(AppManager.createJob(new JobContextDebug()).setCreator(DebugWorker.class).setName("Debug").publish());
				// FileUtils.writeStringToFile(datalog, Loggers.dateLog(System.currentTimeMillis()) + "\tcreate\t" + instance_name + "\t" + job_key + "\t\n", true);
			}
		}
	}
	
	public WorkerCategory getWorkerCategory() {
		return WorkerCategory.INTERNAL;
	}
	
	public String getWorkerLongName() {
		return "Debug Worker";
	}
	
	public String getWorkerVendorName() {
		return "Internal MyDMAM";
	}
	
	public List<WorkerCapablities> getWorkerCapablities() {
		return WorkerCapablities.createList(JobContextDebug.class, Arrays.asList(test_storage_name), Arrays.asList("foo", "bar"));
	}
	
	protected void workerProcessJob(JobProgression progression, JobContext context) throws Exception {
		// FileUtils.writeStringToFile(datalog, Loggers.dateLog(System.currentTimeMillis()) + "\texec\t" + instance_name + "\t" + progression.getJobKey() + "\t" + Thread.currentThread().getName() +
		// "\n", true);
		
		progression.incrStepCount();
		for (int pos = 0; pos < 100; pos++) {
			progression.updateProgress(pos, 100);
			Thread.sleep(random.nextInt(1000) + 1);
		}
		progression.incrStep();
		progression.update("Sleep is done...");
		
		JobNG job = AppManager.createJob(new JobContextDebug(test_storage_name, "bar")).setCreator(DebugWorker.class).setDeleteAfterCompleted().setName("Debug after load");
		if (random.nextBoolean()) {
			job.setUrgent();
			Thread.sleep(random.nextInt(2000) + 1);
		}
		job.setExpirationTime(1, TimeUnit.DAYS).setMaxExecutionTime(10, TimeUnit.MINUTES);
		progression.update("...create the next job");
		// job.setRequiredCompletedJob(required_tests);
		job.publish();
		
		if (random.nextBoolean()) {
			throw new Exception("Test exception !");
		}
	}
	
	protected void forceStopProcess() throws Exception {
	}
	
	protected boolean isActivated() {
		if (Configuration.global.isElementKeyExists("service", "debug_multiple_workers") == false) {
			return false;
		}
		if (Configuration.global.getValueBoolean("service", "debug_multiple_workers") == false) {
			return false;
		}
		return true;
	}
	
}
