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
import hd3gtv.log2.Log2Dumpable;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.tools.GsonIgnore;

import java.util.ArrayList;
import java.util.UUID;

import com.google.gson.JsonObject;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

abstract class JobCreator implements Log2Dumpable, InstanceActionReceiver {
	
	transient protected AppManager manager;
	private Class<?> creator;
	private boolean enabled;
	private String long_name;
	private String vendor_name;
	@GsonIgnore
	ArrayList<Declaration> declarations;
	private String reference_key;
	
	class Declaration implements Log2Dumpable {
		ArrayList<JobContext> contexts;
		String job_name;
		
		Declaration() {
		}
		
		Declaration(String job_name, JobContext... contexts) {
			this.job_name = job_name;
			this.contexts = new ArrayList<JobContext>(contexts.length);
			for (int pos = 0; pos < contexts.length; pos++) {
				this.contexts.add(contexts[pos]);
			}
		}
		
		void createJobs(MutationBatch mutator) throws ConnectionException {
			JobNG require = null;
			for (int pos_dc = 0; pos_dc < contexts.size(); pos_dc++) {
				JobContext declatation_context = contexts.get(pos_dc);
				JobNG job = AppManager.createJob(declatation_context);
				job.setDeleteAfterCompleted();
				job.setCreator(creator);
				if (contexts.size() > 0) {
					job.setName(job_name);
				} else {
					job.setName(job_name + " (" + (pos_dc + 1) + "/" + contexts.size() + ")");
				}
				job.setRequireCompletedJob(require);
				createJobsInternal(mutator, job, require);
				job.publish(mutator);
				require = job;
			}
		}
		
		public Log2Dump getLog2Dump() {
			Log2Dump dump = new Log2Dump();
			dump.add("job_name", job_name);
			for (int pos = 0; pos < contexts.size(); pos++) {
				dump.add("context", AppManager.getGson().toJson(contexts.get(pos), JobContext.class));
			}
			return dump;
		}
	}
	
	public JobCreator(AppManager manager) {
		this.manager = manager;
		if (manager == null) {
			throw new NullPointerException("\"manager\" can't to be null");
		}
		enabled = true;
		declarations = new ArrayList<Declaration>();
		reference_key = getClass().getName().toLowerCase() + ":" + UUID.randomUUID().toString();
	}
	
	public final JobCreator setOptions(Class<?> creator, String long_name, String vendor_name) {
		this.creator = creator;
		this.long_name = long_name;
		this.vendor_name = vendor_name;
		return this;
	}
	
	/**
	 * @param enabled true, start now.
	 */
	synchronized void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	boolean isEnabled() {
		return enabled;
	}
	
	protected void createJobsInternal(MutationBatch mutator, JobNG job, JobNG require) throws ConnectionException {
	}
	
	/**
	 * @param contexts will be dependant (the second need the first, the third need the second, ... the first is the most prioritary)
	 * @throws ClassNotFoundException a context can't to be serialized
	 */
	public final JobCreator add(String job_name, JobContext... contexts) throws ClassNotFoundException {
		if (job_name == null) {
			throw new NullPointerException("\"job_name\" can't to be null");
		}
		if (contexts == null) {
			throw new NullPointerException("\"contexts\" can't to be null");
		}
		if (contexts.length == 0) {
			throw new NullPointerException("\"contexts\" can't to be empty");
		}
		/**
		 * Test each context serialisation
		 */
		for (int pos = 0; pos < contexts.length; pos++) {
			new JobNG(contexts[pos]);
		}
		declarations.add(new Declaration(job_name, contexts));
		return this;
	}
	
	public String toString() {
		return AppManager.getPrettyGson().toJson(this);
	}
	
	void createJobs(MutationBatch mutator) throws ConnectionException {
		for (int pos = 0; pos < declarations.size(); pos++) {
			declarations.get(pos).createJobs(mutator);
		}
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("reference_key", reference_key);
		dump.add("creator", creator);
		dump.add("enabled", enabled);
		dump.add("long_name", long_name);
		dump.add("vendor_name", vendor_name);
		dump.add("declarations", declarations);
		return dump;
	}
	
	final String getReference_key() {
		return reference_key;
	}
	
	public void doAnAction(JsonObject order) {
		if (order.has("activity")) {
			if (order.get("activity").getAsString().equals("enable")) {
				setEnabled(true);
				Log2.log.info("Enable job creator", this);
			} else if (order.get("activity").getAsString().equals("disable")) {
				setEnabled(false);
				Log2.log.info("Disable job creator", this);
			} else if (order.get("activity").getAsString().equals("createjobs")) {
				try {
					MutationBatch mutator = CassandraDb.prepareMutationBatch();
					createJobs(mutator);
					if (mutator.isEmpty() == false) {
						mutator.execute();
						Log2.log.info("Create jobs", this);
					}
				} catch (ConnectionException e) {
					manager.getServiceException().onCassandraError(e);
				}
			}
		}
	}
	
}
