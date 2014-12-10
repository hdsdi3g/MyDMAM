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

import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

class JobCreatorDeclaration {
	
	@GsonIgnore
	protected ArrayList<JobContext> contexts;
	private String name;
	transient private AppManager manager;
	private Class<?> creator;
	
	JobCreatorDeclaration(AppManager manager, Class<?> creator, String name, JobContext... contexts) {
		this.creator = creator;
		this.name = name;
		this.contexts = new ArrayList<JobContext>(contexts.length);
		for (int pos = 0; pos < contexts.length; pos++) {
			this.contexts.add(contexts[pos]);
		}
	}
	
	final void setManager(AppManager manager) {
		this.manager = manager;
	}
	
	protected void createJobsInternal(MutationBatch mutator, JobNG job, JobNG require) throws ConnectionException {
	}
	
	final void createJobs(MutationBatch mutator) throws ConnectionException {
		JobNG require = null;
		for (int pos_dc = 0; pos_dc < contexts.size(); pos_dc++) {
			JobContext declatation_context = contexts.get(pos_dc);
			JobNG job = manager.createJob(declatation_context);
			job.setDeleteAfterCompleted();
			job.setCreator(creator);
			if (contexts.size() > 0) {
				job.setName(name);
			} else {
				job.setName(name + " (" + (pos_dc + 1) + "/" + contexts.size() + ")");
			}
			job.setRequireCompletedJob(require);
			createJobsInternal(mutator, job, require);
			job.publish(mutator);
			require = job;
		}
	}
	
}
