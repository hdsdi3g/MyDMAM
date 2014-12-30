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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam.taskqueue;

import hd3gtv.log2.Log2Dump;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.query.IndexQuery;

/**
 * Task in process
 */
@Deprecated
public class Job extends Task {
	
	Job() {
	}
	
	String processing_error;
	
	Worker worker;
	
	public int progress = 0;
	
	public int progress_size = 0;
	
	public int step = 0;
	
	public int step_count = 0;
	
	long start_date = 0;
	
	long end_date = 0;
	
	public String last_message;
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = super.getLog2Dump();
		dump.add("last_message", last_message);
		dump.add("progress", progress);
		dump.add("progress_size", progress_size);
		dump.add("step", step);
		dump.add("step_count", step_count);
		dump.addDate("start_date", start_date);
		dump.addDate("end_date", end_date);
		dump.add("processing_error", processing_error);
		if (worker != null) {
			dump.add("worker ref", worker.worker_ref);
			dump.add("worker name", worker.getLongWorkerName());
			dump.add("worker class", worker.getClass().getName());
		}
		return dump;
	}
	
	void pushToDatabase(MutationBatch mutator, int ttl) {
		super.pushToDatabase(mutator, ttl);
		
		if (worker != null) {
			mutator.withRow(Broker.CF_TASKQUEUE, key).putColumnIfNotNull("worker", worker.worker_ref, ttl);
		}
		
		mutator.withRow(Broker.CF_TASKQUEUE, key).putColumnIfNotNull("processing_error", processing_error, ttl);
		mutator.withRow(Broker.CF_TASKQUEUE, key).putColumnIfNotNull("progress", progress, ttl);
		mutator.withRow(Broker.CF_TASKQUEUE, key).putColumnIfNotNull("progress_size", progress_size, ttl);
		mutator.withRow(Broker.CF_TASKQUEUE, key).putColumnIfNotNull("step", step, ttl);
		mutator.withRow(Broker.CF_TASKQUEUE, key).putColumnIfNotNull("step_count", step_count, ttl);
		mutator.withRow(Broker.CF_TASKQUEUE, key).putColumnIfNotNull("start_date", start_date, ttl);
		mutator.withRow(Broker.CF_TASKQUEUE, key).putColumnIfNotNull("end_date", end_date, ttl);
		mutator.withRow(Broker.CF_TASKQUEUE, key).putColumnIfNotNull("last_message", last_message, ttl);
	}
	
	void pushToDatabaseEndLifejob(MutationBatch mutator, int ttl) {
		mutator.withRow(Broker.CF_TASKQUEUE, key).putColumn("delete", 1, ttl);
	}
	
	static void selectEndLifeJobs(IndexQuery<String, String> index_query) {
		index_query.addExpression().whereColumn("delete").equals().value(1);
		index_query.withColumnSlice("delete");
	}
	
	/**
	 * Get not processing_error and worker
	 */
	void pullFromDatabase(String key, ColumnList<String> columns) throws ParseException {
		super.pullFromDatabase(key, columns);
		processing_error = columns.getStringValue("processing_error", "");
		progress = columns.getIntegerValue("progress", 0);
		progress_size = columns.getIntegerValue("progress_size", 0);
		step = columns.getIntegerValue("step", 0);
		step_count = columns.getIntegerValue("step_count", 0);
		start_date = columns.getLongValue("start_date", 0l);
		end_date = columns.getLongValue("end_date", 0l);
		last_message = columns.getStringValue("last_message", "");
	}
	
	static boolean isAJob(ColumnList<String> columns) {
		try {
			return columns.getColumnByName("start_date").hasValue();
		} catch (NullPointerException e) {
			return false;
		}
	}
	
	@SuppressWarnings("unchecked")
	static JSONObject pullJSONFromDatabase(ColumnList<String> columns) throws ParseException {
		JSONObject jo = Task.pullJSONFromDatabase(columns);
		jo.put("processing_error", columns.getStringValue("processing_error", ""));
		jo.put("progress", columns.getIntegerValue("progress", 0));
		jo.put("progress_size", columns.getIntegerValue("progress_size", 0));
		jo.put("step", columns.getIntegerValue("step", 0));
		jo.put("step_count", columns.getIntegerValue("step_count", 0));
		jo.put("start_date", columns.getLongValue("start_date", 0l));
		jo.put("end_date", columns.getLongValue("end_date", 0l));
		jo.put("last_message", columns.getStringValue("last_message", ""));
		jo.put("worker", columns.getStringValue("worker", ""));
		return jo;
	}
}
