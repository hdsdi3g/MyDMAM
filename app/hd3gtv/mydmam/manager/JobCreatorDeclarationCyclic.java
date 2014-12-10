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

class JobCreatorDeclarationCyclic extends JobCreatorDeclaration {
	
	/**
	 * In msec
	 */
	private long period;
	
	JobCreatorDeclarationCyclic(AppManager manager, Class<?> creator, String name, long period, JobContext... contexts) {
		super(manager, creator, name, contexts);
		this.period = period;
	}
	
	protected void createJobsInternal(MutationBatch mutator, JobNG job, JobNG require) throws ConnectionException {
		job.setExpirationTime(period, TimeUnit.MILLISECONDS);
		job.setMaxExecutionTime(period, TimeUnit.MILLISECONDS);
	}
	
	static JobCreatorDeclarationSerializer<JobCreatorDeclarationCyclic> serializer = new JobCreatorDeclarationSerializer<JobCreatorDeclarationCyclic>();
	
}
