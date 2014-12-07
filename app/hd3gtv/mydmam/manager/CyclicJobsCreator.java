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
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

public final class CyclicJobsCreator {
	
	// public void createTasks() throws ConnectionException;
	
	// @return in msec
	// public long getInitialCyclicPeriodTasks();
	
	// public String getShortCyclicName();
	
	// public String getLongCyclicName();
	
	// public boolean isCyclicConfigurationAllowToEnabled();
	
	// public boolean isPeriodDurationForCreateTasksCanChange();
	
	private AppManager manager;
	private List<CyclicDeclaration> contexts;
	private Class<?> creator;
	
	private class CyclicDeclaration {
		/**
		 * In msec
		 */
		long period_time;
		List<JobContext> contexts;
		String name;
		
		CyclicDeclaration(String name, long period, TimeUnit unit, JobContext... contexts) {
			this.name = name;
			period_time = unit.toMillis(period);
			this.contexts = new ArrayList<JobContext>(contexts.length);
			for (int pos = 0; pos < contexts.length; pos++) {
				this.contexts.add(contexts[pos]);
			}
		}
	}
	
	public CyclicJobsCreator(AppManager manager) throws NullPointerException {
		this.manager = manager;
		if (manager == null) {
			throw new NullPointerException("\"manager\" can't to be null");
		}
		contexts = new ArrayList<CyclicDeclaration>();
	}
	
	public CyclicJobsCreator setCreator(Class<?> creator) {
		this.creator = creator;
		return this;
	}
	
	/**
	 * @param contexts will be dependant (the second need the first, the third need the second, ... the first is the most prioritary)
	 * @throws ClassNotFoundException a context can't to be serialized
	 */
	public CyclicJobsCreator addCyclic(String name, long period, TimeUnit unit, JobContext... contexts) throws ClassNotFoundException {
		if (name == null) {
			throw new NullPointerException("\"name\" can't to be null");
		}
		if (contexts == null) {
			throw new NullPointerException("\"contexts\" can't to be null");
		}
		if (unit == null) {
			unit = TimeUnit.SECONDS;
		}
		if (contexts.length == 0) {
			throw new NullPointerException("\"contexts\" can't to be empty");
		}
		/**
		 * Test each context serialisation
		 */
		for (int pos = 0; pos < contexts.length; pos++) {
			new JobNG(manager, contexts[pos]);
		}
		this.contexts.add(new CyclicDeclaration(name, period, unit, contexts));
		return this;
	}
	
	// TODO do cyclic
	
	void createJobs() throws ConnectionException {
		CyclicDeclaration declaration;
		for (int pos_c = 0; pos_c < contexts.size(); pos_c++) {
			declaration = contexts.get(pos_c);
			long period_time = declaration.period_time;
			List<JobContext> declaration_contexts = declaration.contexts;
			JobNG require = null;
			
			for (int pos_dc = 0; pos_dc < declaration_contexts.size(); pos_dc++) {
				JobContext declatation_context = declaration_contexts.get(pos_dc);
				JobNG job = manager.createJob(declatation_context);
				job.setDeleteAfterCompleted();
				job.setCreator(creator);
				job.setExpirationTime(period_time, TimeUnit.MILLISECONDS);
				job.setMaxExecutionTime(period_time, TimeUnit.MILLISECONDS);
				if (declaration_contexts.size() > 0) {
					job.setName(declaration.name);
				} else {
					job.setName(declaration.name + " (" + (pos_dc + 1) + "/" + declaration_contexts.size() + ")");
				}
				job.setRequireCompletedJob(require);
				require = job;
				job.publish();
			}
		}
		
	}
}
