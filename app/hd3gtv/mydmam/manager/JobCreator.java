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

import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

public abstract class JobCreator<T extends JobCreatorDeclaration> {
	
	transient protected AppManager manager;
	private Class<?> creator;
	private boolean enabled;
	@SuppressWarnings("unused")
	private String long_name;
	@SuppressWarnings("unused")
	private String vendor_name;
	@GsonIgnore
	protected List<T> declarations;
	
	public JobCreator(AppManager manager) {
		this.manager = manager;
		if (manager == null) {
			throw new NullPointerException("\"manager\" can't to be null");
		}
		enabled = true;
		declarations = new ArrayList<T>();
	}
	
	public final JobCreator<T> setOptions(Class<?> creator, String long_name, String vendor_name) {
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
	
	protected abstract T createDeclaration(AppManager manager, Class<?> creator, String name, JobContext... contexts);
	
	// return new JobCreatorDeclaration(manager, creator, name, contexts);
	
	/**
	 * @param contexts will be dependant (the second need the first, the third need the second, ... the first is the most prioritary)
	 * @throws ClassNotFoundException a context can't to be serialized
	 */
	public final JobCreator<T> add(String jobname, JobContext... contexts) throws ClassNotFoundException {
		if (jobname == null) {
			throw new NullPointerException("\"jobname\" can't to be null");
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
			new JobNG(manager, contexts[pos]);
		}
		this.declarations.add(createDeclaration(manager, creator, jobname, contexts));
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
	
}
