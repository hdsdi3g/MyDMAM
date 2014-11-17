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
import hd3gtv.log2.Log2Dumpable;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.query.IndexQuery;

/**
 * Contient toutes les infos pour le traitement à effectuer.
 */
@SuppressWarnings("unchecked")
public class Task implements Log2Dumpable {
	
	Task() {
	}
	
	protected String key;
	
	public final String getKey() {
		prepareKey();
		return key;
	}
	
	final private void prepareKey() {
		if (key == null) {
			key = Broker.createKey("task", name);
		}
	}
	
	String name;
	
	Profile profile;
	
	long max_date_to_wait_processing;
	
	long create_date;
	
	String task_key_require_done;
	
	/**
	 * TODO remplace with a Gson system
	 */
	@Deprecated
	JSONObject context;
	
	String creator_classname;
	
	int priority;
	
	TaskJobStatus status;
	
	boolean delete_after_done;
	
	String creator_hostname;
	
	String creator_instancename;
	
	transient long updatedate;
	
	boolean cyclic_source;
	
	boolean trigger_source;
	
	public JSONObject getContext() {
		return context;
	}
	
	public Profile getProfile() {
		return profile;
	}
	
	final Job toJob() {
		Job job = new Job();
		job.key = key;
		job.name = name;
		job.profile = profile;
		job.max_date_to_wait_processing = max_date_to_wait_processing;
		job.create_date = create_date;
		job.task_key_require_done = task_key_require_done;
		job.context = context;
		job.priority = priority;
		job.status = status;
		job.delete_after_done = delete_after_done;
		job.creator_classname = creator_classname;
		job.creator_hostname = creator_hostname;
		job.creator_instancename = creator_instancename;
		job.cyclic_source = cyclic_source;
		job.trigger_source = trigger_source;
		return job;
	}
	
	void pushToDatabase(MutationBatch mutator, int ttl) {
		prepareKey();
		mutator.withRow(Broker.CF_TASKQUEUE, key).putColumnIfNotNull("name", name, ttl);
		if (max_date_to_wait_processing == 0l) {
			max_date_to_wait_processing = Long.MAX_VALUE;
		}
		mutator.withRow(Broker.CF_TASKQUEUE, key).putColumnIfNotNull("max_date_to_wait_processing", max_date_to_wait_processing, ttl);
		mutator.withRow(Broker.CF_TASKQUEUE, key).putColumnIfNotNull("create_date", create_date, ttl);
		mutator.withRow(Broker.CF_TASKQUEUE, key).putColumnIfNotNull("task_key_require_done", task_key_require_done, ttl);
		mutator.withRow(Broker.CF_TASKQUEUE, key).putColumnIfNotNull("creator_hostname", creator_hostname, ttl);
		mutator.withRow(Broker.CF_TASKQUEUE, key).putColumnIfNotNull("creator_instancename", creator_instancename, ttl);
		mutator.withRow(Broker.CF_TASKQUEUE, key).putColumnIfNotNull("creator_classname", creator_classname, ttl);
		mutator.withRow(Broker.CF_TASKQUEUE, key).putColumnIfNotNull("priority", priority, ttl);
		mutator.withRow(Broker.CF_TASKQUEUE, key).putColumn("delete_after_done", delete_after_done, ttl);
		mutator.withRow(Broker.CF_TASKQUEUE, key).putColumn("cyclic_source", cyclic_source, ttl);
		mutator.withRow(Broker.CF_TASKQUEUE, key).putColumn("trigger_source", trigger_source, ttl);
		mutator.withRow(Broker.CF_TASKQUEUE, key).putColumn("updatedate", System.currentTimeMillis(), ttl);
		
		if (profile != null) {
			profile.pushToDatabase(mutator, key, ttl);
		}
		if (context != null) {
			mutator.withRow(Broker.CF_TASKQUEUE, key).putColumnIfNotNull("context", context.toJSONString(), ttl);
		}
		if (status != null) {
			status.pushToDatabase(mutator, key, ttl);
		}
		mutator.withRow(Broker.CF_TASKQUEUE, key).putColumn("indexingdebug", 1, ttl);
	}
	
	void pullFromDatabase(String key, ColumnList<String> columns) throws ParseException {
		this.key = key;
		name = columns.getStringValue("name", "");
		max_date_to_wait_processing = columns.getLongValue("max_date_to_wait_processing", 0l);
		create_date = columns.getLongValue("create_date", 0l);
		task_key_require_done = columns.getStringValue("task_key_require_done", "");
		creator_hostname = columns.getStringValue("creator_hostname", "");
		creator_classname = columns.getStringValue("creator_classname", "");
		creator_instancename = columns.getStringValue("creator_instancename", "");
		priority = columns.getIntegerValue("priority", 0);
		delete_after_done = columns.getBooleanValue("delete_after_done", false);
		cyclic_source = columns.getBooleanValue("cyclic_source", false);
		trigger_source = columns.getBooleanValue("trigger_source", false);
		updatedate = columns.getLongValue("updatedate", 0l);
		
		profile = new Profile();
		profile.pullFromDatabase(columns);
		
		JSONParser jp = new JSONParser();
		context = (JSONObject) jp.parse(columns.getStringValue("context", "{}"));
		status = TaskJobStatus.pullFromDatabase(columns);
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("key", key);
		dump.add("name", name);
		dump.add("status", status);
		if (profile != null) {
			dump.add("profile", profile.category + ":" + profile.name);
		}
		dump.addDate("create_date", create_date);
		dump.addDate("max_age_to_wait_processing", max_date_to_wait_processing);
		if (updatedate != 0) {
			dump.addDate("updatedate", updatedate);
		}
		dump.add("task_key_require_done", task_key_require_done);
		dump.add("context", context);
		dump.add("priority", priority);
		dump.add("creator_classname", creator_classname);
		dump.add("creator_hostname", creator_hostname);
		dump.add("creator_instancename", creator_instancename);
		dump.add("delete_after_done", delete_after_done);
		dump.add("cyclic_source", cyclic_source);
		dump.add("trigger_source", trigger_source);
		return dump;
	}
	
	static void selectWaitingForProfileCategory(IndexQuery<String, String> index_query, String category) {
		TaskJobStatus.selectByName(index_query, TaskJobStatus.WAITING);
		Profile.selectProfileByCategory(index_query, category);
	}
	
	static void selectTooOldWaitingTasks(IndexQuery<String, String> index_query, String hostname) {
		TaskJobStatus.selectByName(index_query, TaskJobStatus.WAITING);
		index_query.addExpression().whereColumn("creator_hostname").equals().value(hostname);
		index_query.addExpression().whereColumn("max_date_to_wait_processing").lessThan().value(System.currentTimeMillis());
	}
	
	static void selectPostponedTasksWithMaxAge(IndexQuery<String, String> index_query, String hostname) {
		TaskJobStatus.selectByName(index_query, TaskJobStatus.POSTPONED);
		index_query.addExpression().whereColumn("creator_hostname").equals().value(hostname);
		index_query.addExpression().whereColumn("max_date_to_wait_processing").lessThan().value(Long.MAX_VALUE);
	}
	
	static void selectAllLastTasksAndJobs(IndexQuery<String, String> index_query, long since_date) {
		index_query.addExpression().whereColumn("indexingdebug").equals().value(1);
		index_query.addExpression().whereColumn("updatedate").greaterThanEquals().value(since_date);
	}
	
	static boolean isEmptyDbEntry(ColumnList<String> columns) {
		try {
			return (columns.getColumnByName("name").getStringValue().equals(""));
		} catch (NullPointerException e) {
			return true;
		}
	}
	
	static JSONObject pullJSONFromDatabase(ColumnList<String> columns) throws ParseException {
		JSONObject jo = new JSONObject();
		jo.put("name", columns.getStringValue("name", ""));
		jo.put("max_date_to_wait_processing", columns.getLongValue("max_date_to_wait_processing", 0l));
		jo.put("create_date", columns.getLongValue("create_date", 0l));
		jo.put("task_key_require_done", columns.getStringValue("task_key_require_done", ""));
		jo.put("creator_hostname", columns.getStringValue("creator_hostname", ""));
		jo.put("creator_classname", columns.getStringValue("creator_classname", ""));
		jo.put("creator_instancename", columns.getStringValue("creator_instancename", ""));
		jo.put("priority", columns.getIntegerValue("priority", 0));
		jo.put("delete_after_done", columns.getBooleanValue("delete_after_done", false));
		jo.put("cyclic_source", columns.getBooleanValue("cyclic_source", false));
		jo.put("trigger_source", columns.getBooleanValue("trigger_source", false));
		jo.put("updatedate", columns.getLongValue("updatedate", 0l));
		
		Profile.pullJSONFromDatabase(jo, columns);
		JSONParser jp = new JSONParser();
		jo.put("context", (JSONObject) jp.parse(columns.getStringValue("context", "{}")));
		jo.put("status", TaskJobStatus.pullFromDatabase(columns).name().toUpperCase());
		return jo;
	}
}
