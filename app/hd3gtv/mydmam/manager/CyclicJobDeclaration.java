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

class CyclicJobDeclaration {
	
	/**
	 * In msec
	 */
	private long period;
	@GsonIgnore
	private ArrayList<JobContext> contexts;
	private String name;
	transient private AppManager manager;
	private Class<?> creator;
	
	CyclicJobDeclaration(AppManager manager, Class<?> creator, String name, long period, JobContext... contexts) {
		this.manager = manager;
		this.creator = creator;
		this.name = name;
		this.period = period;
		this.contexts = new ArrayList<JobContext>(contexts.length);
		for (int pos = 0; pos < contexts.length; pos++) {
			this.contexts.add(contexts[pos]);
		}
	}
	
	void createJobs(MutationBatch mutator) throws ConnectionException {
		JobNG require = null;
		for (int pos_dc = 0; pos_dc < contexts.size(); pos_dc++) {
			JobContext declatation_context = contexts.get(pos_dc);
			JobNG job = manager.createJob(declatation_context);
			job.setDeleteAfterCompleted();
			job.setCreator(creator);
			job.setExpirationTime(period, TimeUnit.MILLISECONDS);
			job.setMaxExecutionTime(period, TimeUnit.MILLISECONDS);
			if (contexts.size() > 0) {
				job.setName(name);
			} else {
				job.setName(name + " (" + (pos_dc + 1) + "/" + contexts.size() + ")");
			}
			job.setRequireCompletedJob(require);
			require = job;
			job.publish(mutator);
		}
	}
	
	void setManager(AppManager manager) {
		this.manager = manager;
	}
	
	static class Serializer implements JsonSerializer<CyclicJobDeclaration>, JsonDeserializer<CyclicJobDeclaration> {
		private static Type al_JobContext_typeOfT = new TypeToken<ArrayList<JobContext>>() {
		}.getType();
		
		public CyclicJobDeclaration deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonObject jo = json.getAsJsonObject();
			CyclicJobDeclaration result = AppManager.getSimpleGson().fromJson(json, CyclicJobDeclaration.class);
			result.contexts = AppManager.getSimpleGson().fromJson(jo.get("contexts"), al_JobContext_typeOfT);
			return result;
		}
		
		public JsonElement serialize(CyclicJobDeclaration src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject result = AppManager.getSimpleGson().toJsonTree(src).getAsJsonObject();
			result.add("contexts", AppManager.getSimpleGson().toJsonTree(src.contexts, al_JobContext_typeOfT));
			return result;
		}
		
	}
	
}
