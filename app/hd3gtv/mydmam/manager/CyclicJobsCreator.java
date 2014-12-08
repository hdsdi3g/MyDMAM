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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

public final class CyclicJobsCreator {
	
	transient private AppManager manager;
	@GsonIgnore
	private List<CyclicJobDeclaration> contexts;
	private Class<?> creator;
	private long period;
	private long next_date_to_create_jobs;
	private boolean enabled;
	@SuppressWarnings("unused")
	private String long_name;
	@SuppressWarnings("unused")
	private String vendor_name;
	
	public CyclicJobsCreator(AppManager manager, long period, TimeUnit unit, boolean not_at_boot) throws NullPointerException {
		this.manager = manager;
		if (manager == null) {
			throw new NullPointerException("\"manager\" can't to be null");
		}
		contexts = new ArrayList<CyclicJobDeclaration>();
		if (unit == null) {
			unit = TimeUnit.SECONDS;
		}
		this.period = unit.toMillis(period);
		
		if (not_at_boot) {
			next_date_to_create_jobs = System.currentTimeMillis() + period;
		} else {
			next_date_to_create_jobs = 0;
		}
		enabled = true;
	}
	
	public CyclicJobsCreator setOptions(Class<?> creator, String long_name, String vendor_name) {
		this.creator = creator;
		this.long_name = long_name;
		this.vendor_name = vendor_name;
		return this;
	}
	
	/**
	 * @param contexts will be dependant (the second need the first, the third need the second, ... the first is the most prioritary)
	 * @throws ClassNotFoundException a context can't to be serialized
	 */
	public CyclicJobsCreator addCyclic(String jobname, JobContext... contexts) throws ClassNotFoundException {
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
		this.contexts.add(new CyclicJobDeclaration(manager, creator, jobname, period, contexts));
		return this;
	}
	
	/**
	 * @param period push the next date to create jobs.
	 */
	synchronized void setPeriod(long period) {
		this.period = period;
		next_date_to_create_jobs = System.currentTimeMillis() + period;
	}
	
	/**
	 * @param enabled true, start now.
	 */
	synchronized void setEnabled(boolean enabled) {
		if (enabled == true) {
			period = 0;
		}
		this.enabled = enabled;
	}
	
	boolean needToCreateJobs() {
		if (enabled == false) {
			return false;
		}
		return System.currentTimeMillis() > next_date_to_create_jobs;
	}
	
	void createJobs(MutationBatch mutator) throws ConnectionException {
		next_date_to_create_jobs = System.currentTimeMillis() + period;
		for (int pos = 0; pos < contexts.size(); pos++) {
			contexts.get(pos).createJobs(mutator);
		}
	}
	
	static class Serializer implements JsonSerializer<CyclicJobsCreator>, JsonDeserializer<CyclicJobsCreator> {
		
		private static Type al_JobDeclaration_typeOfT = new TypeToken<ArrayList<CyclicJobDeclaration>>() {
		}.getType();
		
		public CyclicJobsCreator deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonObject jo = json.getAsJsonObject();
			CyclicJobsCreator result = AppManager.getSimpleGson().fromJson(json, CyclicJobsCreator.class);
			result.contexts = AppManager.getSimpleGson().fromJson(jo.get("contexts"), al_JobDeclaration_typeOfT);
			return result;
		}
		
		public JsonElement serialize(CyclicJobsCreator src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject result = AppManager.getSimpleGson().toJsonTree(src).getAsJsonObject();
			result.add("contexts", AppManager.getSimpleGson().toJsonTree(src.contexts, al_JobDeclaration_typeOfT));
			return result;
		}
	}
	
	public String toString() {
		return AppManager.getPrettyGson().toJson(this);
	}
	
}
