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

import java.util.concurrent.TimeUnit;

import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

public final class CyclicJobCreator extends JobCreator {
	
	private long period;
	private long next_date_to_create_jobs;
	
	public CyclicJobCreator(AppManager manager, long period, TimeUnit unit, boolean not_at_boot) throws NullPointerException {
		super(manager);
		if (unit == null) {
			unit = TimeUnit.SECONDS;
		}
		this.period = unit.toMillis(period);
		
		if (not_at_boot) {
			next_date_to_create_jobs = System.currentTimeMillis() + period;
		} else {
			next_date_to_create_jobs = 0;
		}
	}
	
	synchronized void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if (isEnabled() == true) {
			period = 0;
		}
	}
	
	/**
	 * @param period push the next date to create jobs.
	 */
	synchronized void setPeriod(long period) {
		this.period = period;
		next_date_to_create_jobs = System.currentTimeMillis() + period;
	}
	
	boolean needToCreateJobs() {
		if (isEnabled() == false) {
			return false;
		}
		return System.currentTimeMillis() > next_date_to_create_jobs;
	}
	
	void createJobs(MutationBatch mutator) throws ConnectionException {
		next_date_to_create_jobs = System.currentTimeMillis() + period;
		super.createJobs(mutator);
	}
	
	protected void createJobsInternal(MutationBatch mutator, JobNG job, JobNG require) throws ConnectionException {
		job.setExpirationTime(period, TimeUnit.MILLISECONDS);
		job.setMaxExecutionTime(period, TimeUnit.MILLISECONDS);
	}
	
	static JobCreatorSerializer<CyclicJobCreator> serializer = new JobCreatorSerializer<CyclicJobCreator>(CyclicJobCreator.class);
}
