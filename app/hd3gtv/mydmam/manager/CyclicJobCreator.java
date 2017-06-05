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
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonDeSerializer;
import hd3gtv.mydmam.gson.GsonKit;

public final class CyclicJobCreator extends JobCreator {
	
	private long period;
	private transient long origin_period;
	private long next_date_to_create_jobs;
	private boolean only_off_hours;
	
	public CyclicJobCreator(AppManager manager, long period, TimeUnit unit, boolean not_at_boot) throws NullPointerException {
		super(manager);
		if (unit == null) {
			unit = TimeUnit.SECONDS;
		}
		this.period = unit.toMillis(period);
		origin_period = this.period;
		
		if (not_at_boot) {
			next_date_to_create_jobs = System.currentTimeMillis() + this.period;
		} else {
			next_date_to_create_jobs = 0;
		}
	}
	
	void setLastDateCreatedJobLikeThis(long last_date) {
		next_date_to_create_jobs = last_date + period;
		Loggers.Manager.info("Cyclic \"" + this.getJobName() + "\" has manually set a next_date_to_create_jobs at " + Loggers.dateLog(next_date_to_create_jobs));
	}
	
	synchronized void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if (isEnabled() == false) {
			period = 0;
		} else {
			period = origin_period;
		}
	}
	
	/**
	 * @param period push the next date to create jobs.
	 */
	synchronized void setPeriod(long period) {
		this.period = period;
		origin_period = period;
		next_date_to_create_jobs = System.currentTimeMillis() + period;
	}
	
	public CyclicJobCreator setOnlyOffHours(boolean only_off_hours) {
		this.only_off_hours = only_off_hours;
		return this;
	}
	
	boolean needToCreateJobs() {
		if (isEnabled() == false) {
			return false;
		}
		if (only_off_hours) {
			if (AppManager.isActuallyOffHours() == false) {
				return false;
			}
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
	
	public Class<? extends InstanceActionReceiver> getClassToCallback() {
		return CyclicJobCreator.class;
	}
	
	public void doAnAction(JsonObject order) throws Exception {
		super.doAnAction(order);
		
		if (order.has("setperiod")) {
			Loggers.Manager.debug("Change cyclic period: " + order.get("setperiod").getAsLong() + " ms");
			setPeriod(order.get("setperiod").getAsLong());
		}
		if (order.has("setnextdate")) {
			next_date_to_create_jobs = order.get("setnextdate").getAsLong();
			Loggers.Manager.debug("Change cyclic next date: " + Loggers.dateLog(next_date_to_create_jobs));
		}
		
		Loggers.Manager.info("Change cyclic: " + toString());
	}
	
	public Class<?> getInstanceStatusItemReferenceClass() {
		return CyclicJobCreator.class;
	}
	
	public static class Serializer implements GsonDeSerializer<CyclicJobCreator> {
		
		public JsonElement serialize(CyclicJobCreator src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject result = MyDMAM.gson_kit.getGsonSimple().toJsonTree(src).getAsJsonObject();
			result.add("declarations", MyDMAM.gson_kit.getGson().toJsonTree(src.declarations, GsonKit.type_ArrayList_JobCreatorJobDeclaration));
			return result;
		}
		
		public CyclicJobCreator deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonObject jo = json.getAsJsonObject();
			CyclicJobCreator result = MyDMAM.gson_kit.getGsonSimple().fromJson(json, CyclicJobCreator.class);
			result.declarations = MyDMAM.gson_kit.getGson().fromJson(jo.get("declarations"), GsonKit.type_ArrayList_JobCreatorJobDeclaration);
			return result;
		}
		
	}
}
