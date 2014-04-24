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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam.taskqueue;

import hd3gtv.javasimpleservice.ServiceInformations;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Event;
import hd3gtv.mydmam.mail.MessageAlert;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

class WorkerEngine extends Thread {
	Worker worker;
	Job job;
	ServiceInformations serviceinformations;
	
	WorkerEngine(Worker referer, Job job, ServiceInformations serviceinformations) {
		this.job = job;
		this.worker = referer;
		this.serviceinformations = serviceinformations;
		setDaemon(true);
		setName(referer.getShortWorkerName() + "-for-" + job.key);
	}
	
	public void run() {
		worker.status = WorkerStatus.PROCESSING;
		job.worker = worker;
		if (worker instanceof WorkerCyclicEngine) {
			job.delete_after_done = true;
		}
		job.start_date = System.currentTimeMillis();
		job.status = TaskJobStatus.PROCESSING;
		job.processing_error = null;
		try {
			if (job.cyclic_source == false) {
				Log2.log.info("Start process", job);
			}
			worker.process(job);
			
			if (worker.status == WorkerStatus.STOPPED) {
				job.status = TaskJobStatus.STOPPED;
			} else {
				job.status = TaskJobStatus.DONE;
			}
		} catch (Exception e) {
			job.processing_error = exceptionToString(e);
			job.status = TaskJobStatus.ERROR;
			Log2.log.error("Error during processing", null, job);
			MessageAlert.create("Error during processing", false).addDump(job).addDump(worker).setServiceinformations(serviceinformations).send();
		}
		job.end_date = System.currentTimeMillis();
		if (worker.status != WorkerStatus.STOPPED) {
			worker.status = WorkerStatus.WAITING;
		}
		if (job.status == TaskJobStatus.DONE) {
			try {
				worker.broker.doneJob(job);
			} catch (ConnectionException e) {
				Log2.log.error("Lost Cassandra connection", e);
			}
		}
	}
	
	void askStopProcess() {
		worker.status = WorkerStatus.PENDING_CANCEL_TASK;
		job.status = TaskJobStatus.STOPPED;
		try {
			Log2.log.debug("Force stop process");
			worker.forceStopProcess();
			Log2.log.info("Process is stopped");
		} catch (Exception e) {
			job.processing_error = exceptionToString(e);
			Log2.log.error("Error during stop processing", e);
			MessageAlert.create("Error during stop processing", false).addDump(job).addDump(worker).setThrowable(e).setServiceinformations(serviceinformations).send();
		}
		worker.status = WorkerStatus.STOPPED;
	}
	
	private static String exceptionToString(Exception e) {
		StringBuffer sb = new StringBuffer();
		sb.append(e.getClass().getName());
		sb.append(": ");
		sb.append(e.getMessage());
		sb.append("\n");
		Log2Event.throwableToString(e, sb, "\n");
		return sb.toString();
	}
	
}
