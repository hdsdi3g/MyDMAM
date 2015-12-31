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
import java.util.HashMap;
import java.util.List;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.serializers.StringSerializer;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.tools.GsonIgnore;

public final class TriggerJobCreator extends JobCreator {
	
	private static Keyspace keyspace;
	private static final ColumnFamily<String, String> CF_DONE_JOBS = new ColumnFamily<String, String>("mgrDoneJobs", StringSerializer.get(), StringSerializer.get());
	
	static {
		try {
			keyspace = CassandraDb.getkeyspace();
			String default_keyspacename = CassandraDb.getDefaultKeyspacename();
			if (CassandraDb.isColumnFamilyExists(keyspace, CF_DONE_JOBS.getName()) == false) {
				CassandraDb.createColumnFamilyString(default_keyspacename, CF_DONE_JOBS.getName(), false);
			}
		} catch (Exception e) {
			Loggers.Manager.error("Can't init database CFs", e);
		}
	}
	
	static void doneJob(JobNG job, MutationBatch mutator) {
		if (job.getContext() == null) {
			return;
		}
		String key = JobContext.Utility.prepareContextKeyForTrigger(job.getContext());
		mutator.withRow(CF_DONE_JOBS, key).putColumn("source", AppManager.getGson().toJson(job), JobNG.TTL_WAITING);
		mutator.withRow(CF_DONE_JOBS, key).putColumn("end_date", job.getEndDate(), JobNG.TTL_WAITING);
	}
	
	static void prepareTriggerHooksCreateJobs(List<TriggerJobCreator> triggers, long precedent_date, MutationBatch mutator) throws ConnectionException {
		HashMap<String, TriggerJobCreator> map_triggers = new HashMap<String, TriggerJobCreator>();
		for (int pos = 0; pos < triggers.size(); pos++) {
			map_triggers.put(triggers.get(pos).context_hook_trigger_key, triggers.get(pos));
		}
		
		Rows<String, String> rows = keyspace.prepareQuery(CF_DONE_JOBS).getKeySlice(map_triggers.keySet()).withColumnSlice("end_date").execute().getResult();
		for (int pos = 0; pos < rows.size(); pos++) {
			long last_date = rows.getRowByIndex(pos).getColumns().getLongValue("end_date", 0l);
			if (last_date > precedent_date) {
				map_triggers.get(rows.getRowByIndex(pos).getKey()).createJobs(mutator);
			}
		}
	}
	
	// TODO get all done jobs to AsyncJS
	
	/**
	 * End of static realm
	 */
	
	// private JobContext context_hook;
	private String context_hook_trigger_key;
	private @GsonIgnore JobContext context_hook;
	
	public TriggerJobCreator(AppManager manager, JobContext context_hook) {
		super(manager);
		// this.context_hook = context_hook;
		if (context_hook == null) {
			throw new NullPointerException("\"context_hook\" can't to be null");
		}
		context_hook_trigger_key = JobContext.Utility.prepareContextKeyForTrigger(context_hook);
		this.context_hook = context_hook;
	}
	
	static class Serializer extends JobCreatorSerializer<TriggerJobCreator> {
		Serializer() {
			super(TriggerJobCreator.class);
		}
		
		public JsonElement serialize(TriggerJobCreator src, Type typeOfSrc, JsonSerializationContext context) {
			JsonElement result = super.serialize(src, typeOfSrc, context);
			result.getAsJsonObject().add("context_hook", AppManager.getGson().toJsonTree(src.context_hook, JobContext.class));
			return result;
		}
		
		public TriggerJobCreator deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			TriggerJobCreator result = super.deserialize(json, typeOfT, context);
			result.context_hook = AppManager.getGson().fromJson(json.getAsJsonObject().get("context_hook"), JobContext.class);
			return result;
		}
	}
	
	static Serializer serializer = new Serializer();
	
	public Class<? extends InstanceActionReceiver> getClassToCallback() {
		return TriggerJobCreator.class;
	}
	
	public Class<?> getInstanceStatusItemReferenceClass() {
		return TriggerJobCreator.class;
	}
	
}
