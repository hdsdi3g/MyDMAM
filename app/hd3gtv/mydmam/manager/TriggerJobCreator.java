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

import hd3gtv.log2.Log2;
import hd3gtv.mydmam.db.CassandraDb;

import java.util.HashMap;
import java.util.List;

import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.serializers.StringSerializer;

public class TriggerJobCreator extends JobCreator<TriggerJobCreatorDeclaration> {
	
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
			Log2.log.error("Can't init database CFs", e);
		}
	}
	
	static void doneJob(JobNG job, MutationBatch mutator) {
		if (job.getContext() == null) {
			return;
		}
		String key = JobContext.Utility.prepareContextKeyForTrigger(job.getContext());
		mutator.withRow(CF_DONE_JOBS, key).putColumn("source", AppManager.getGson().toJson(job), JobNG.TTL);
		mutator.withRow(CF_DONE_JOBS, key).putColumn("end_date", job.getEndDate(), JobNG.TTL);
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
	
	/**
	 * End of static realm
	 */
	
	// private JobContext context_hook;
	private String context_hook_trigger_key;
	
	public TriggerJobCreator(AppManager manager, JobContext context_hook) {
		super(manager);
		// this.context_hook = context_hook;
		if (context_hook == null) {
			throw new NullPointerException("\"context_hook\" can't to be null");
		}
		context_hook_trigger_key = JobContext.Utility.prepareContextKeyForTrigger(context_hook);
	}
	
	protected TriggerJobCreatorDeclaration createDeclaration(AppManager manager, Class<?> creator, String name, JobContext... contexts) {
		return new TriggerJobCreatorDeclaration(manager, creator, name, contexts);
	}
	
	static JobCreatorSerializer<TriggerJobCreator, TriggerJobCreatorDeclaration> serializer = new JobCreatorSerializer<TriggerJobCreator, TriggerJobCreatorDeclaration>();
	
}
