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

import java.util.ArrayList;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.gson.GsonIgnore;

public abstract class JobCreator implements InstanceActionReceiver, InstanceStatusItem {
	
	transient protected AppManager manager;
	private Class<?> creator;
	private boolean enabled;
	private String long_name;
	@SuppressWarnings("unused")
	private String vendor_name;
	
	/**
	 * Only one
	 */
	@GsonIgnore
	ArrayList<Declaration> declarations;
	
	@GsonIgnore
	private Declaration current_declaration;
	
	private String reference_key;
	
	public class Declaration {
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
				job.setRequiredCompletedJob(require);
				createJobsInternal(mutator, job, require);
				job.publish(mutator);
				require = job;
			}
		}
		
	}
	
	public JobCreator(AppManager manager) {
		this.manager = manager;
		if (manager == null) {
			throw new NullPointerException("\"manager\" can't to be null");
		}
		enabled = true;
		declarations = new ArrayList<Declaration>(1);
		reference_key = getClass().getName().toLowerCase() + ":" + UUID.randomUUID().toString();
		
		manager.getInstanceStatus().registerInstanceStatusItem(this);
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
	
	public String getLongName() {
		return long_name;
	}
	
	String getJobName() {
		return current_declaration.job_name;
	}
	
	/**
	 * @param contexts will be dependant (the second need the first, the third need the second, ... the first is the most prioritary)
	 * @throws ClassNotFoundException a context can't to be serialized
	 */
	public final JobCreator createThis(String job_name, Class<?> creator, String long_name, String vendor_name, JobContext... contexts) throws ClassNotFoundException {
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
		
		this.creator = creator;
		this.long_name = long_name;
		this.vendor_name = vendor_name;
		
		if (declarations.isEmpty() == false) {
			declarations.clear();
		}
		current_declaration = new Declaration(job_name, contexts);
		declarations.add(current_declaration);
		return this;
	}
	
	public String toString() {
		return MyDMAM.gson_kit.getGson().toJson(this); // TODO pretty json
	}
	
	void createJobs(MutationBatch mutator) throws ConnectionException {
		if (current_declaration != null) {
			current_declaration.createJobs(mutator);
		}
	}
	
	public final String getReferenceKey() {
		return reference_key;
	}
	
	/**
	 * Call createThis() before this !
	 * @return the same key like CF_DONE_JOBS.key
	 */
	String getFirstContextKey() throws NullPointerException {
		if (current_declaration == null) {
			throw new NullPointerException("Job creator has not be configured correctly");
		}
		if (current_declaration.contexts.isEmpty()) {
			throw new NullPointerException("Job creator has not be configured correctly");
		}
		return JobContext.Utility.prepareContextKeyForTrigger(current_declaration.contexts.get(0));
	}
	
	public void doAnAction(JsonObject order) throws Exception {
		if (order.has("activity")) {
			if (order.get("activity").getAsString().equals("enable")) {
				setEnabled(true);
				Loggers.Manager.info("Enable job creator:\t" + toString());
			} else if (order.get("activity").getAsString().equals("disable")) {
				setEnabled(false);
				Loggers.Manager.info("Disable job creator:\t" + toString());
			} else if (order.get("activity").getAsString().equals("createjobs")) {
				try {
					MutationBatch mutator = CassandraDb.prepareMutationBatch();
					createJobs(mutator);
					if (mutator.isEmpty() == false) {
						mutator.execute();
						Loggers.Manager.info("Create jobs:\t" + toString());
					}
				} catch (ConnectionException e) {
					manager.getServiceException().onCassandraError(e);
				}
			}
		}
	}
	
	public JsonElement getInstanceStatusItem() {
		return MyDMAM.gson_kit.getGson().toJsonTree(this);
	}
}
