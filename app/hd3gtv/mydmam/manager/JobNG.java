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

import hd3gtv.mydmam.MyDMAM;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Use AppManager to for create job.
 */
public final class JobNG {
	
	public enum JobStatus {
		TOO_OLD, CANCELED, POSTPONED, WAITING, DONE, PROCESSING, STOPPED, ERROR, PREPARING;
		
		/*void pushToDatabase(MutationBatch mutator, String key, int ttl) {
		 * 			mutator.withRow(Broker.CF_TASKQUEUE, key).putColumnIfNotNull("status", this.name().toUpperCase(), ttl);		}
		static TaskJobStatus pullFromDatabase(ColumnList<String> columns) {
			return fromString(columns.getStringValue("status", "WAITING"));		}
		static void selectByName(IndexQuery<String, String> index_query, TaskJobStatus status) {
			index_query.addExpression().whereColumn("status").equals().value(status.name().toUpperCase());
		}
		*/
	}
	
	JobNG() {
		/**
		 * For Gson
		 */
	}
	
	@GsonIgnore
	private AppManager manager;
	
	/**
	 * Declaration & configuration vars
	 */
	private String key;
	private Class creator;
	private boolean urgent;
	private String name;
	private long expiration_date;
	private long max_execution_time;
	private String require_key;
	private long create_date;
	private boolean delete_after_completed;
	private String instance_status_creator_key;
	private String instance_status_creator_hostname;
	private int priority;
	
	@GsonIgnore
	private JobContext context;
	
	/**
	 * Activity vars
	 */
	private JobStatus status;
	private long update_date;
	private GsonThrowable processing_error;
	private Progression progression;
	private long start_date;
	private long end_date;
	private String worker_reference;
	private Class worker_class;
	private String instance_status_executor_key;
	private String instance_status_executor_hostname;
	
	JobNG(AppManager manager, JobContext context) throws ClassNotFoundException {
		this.manager = manager;
		this.context = context;
		MyDMAM.checkIsAccessibleClass(context.getClass(), false);
		key = "job:" + UUID.randomUUID().toString();
		name = "Generic job created at " + (new Date()).toString();
		urgent = false;
		priority = 0;
		delete_after_completed = false;
		expiration_date = -1;
		status = JobStatus.WAITING;
		
		instance_status_creator_key = manager.getInstance_status().getDatabaseKey();
		instance_status_creator_hostname = manager.getInstance_status().getHostName();
		progression = new Progression();
		processing_error = null;
		update_date = -1;
		start_date = -1;
		end_date = -1;
	}
	
	public JobNG setName(String name) {
		this.name = name;
		return this;
	}
	
	public JobNG setCreator(Class creator) {
		this.creator = creator;
		return this;
	}
	
	public JobNG setUrgent() {
		urgent = true;
		return this;
	}
	
	public JobNG setRequireCompletedJob(JobNG require) {
		require_key = require.key;
		return this;
	}
	
	public JobNG setExpirationTime(long duration, TimeUnit unit) {
		if (duration < 0) {
			return this;
		}
		expiration_date = System.currentTimeMillis() + unit.toMillis(duration);
		return this;
	}
	
	public JobNG setMaxExecutionTime(long duration, TimeUnit unit) {
		this.max_execution_time = unit.toMillis(duration);
		return this;
	}
	
	/**
	 * Doesn't change current status if POSTPONED, PROCESSING or PREPARING.
	 */
	public JobNG setPostponed() {
		if (status == JobStatus.POSTPONED) {
			return this;
		}
		if (status == JobStatus.PROCESSING) {
			return this;
		}
		if (status == JobStatus.PREPARING) {
			return this;
		}
		status = JobStatus.POSTPONED;
		return this;
	}
	
	public JobNG setDeleteAfterCompleted() {
		delete_after_completed = true;
		return this;
	}
	
	public Progression getProgression() {
		return progression;
	}
	
	public JobContext getContext() {
		return context;
	}
	
	public void publish() {
		create_date = System.currentTimeMillis();
		update_date = create_date;
		if (urgent) {
			// priority = countTasksFromStatus(TaskJobStatus.WAITING) + 1;//TODO compute priority
		}
		// TODO access to broker, and publish job...
	}
	
	/**
	 * To force changes (sync method) with set() functions.
	 * If job is actually POSTPONED, PROCESSING or PREPARING, changes will be canceled by the executor worker.
	 */
	public void saveChanges() {
		update_date = System.currentTimeMillis();
		// TODO access to broker, and push job...
	}
	
	// TODO manage job lifetime, and differents status
	
	static class Serializer implements JsonSerializer<JobNG>, JsonDeserializer<JobNG> {
		public JobNG deserialize(JsonElement jejson, Type typeOfT, JsonDeserializationContext jcontext) throws JsonParseException {
			JsonObject json = (JsonObject) jejson;
			String context_class = json.get("context_class").getAsString();
			json.remove("context_class");
			
			JobNG job = AppManager.getGson().fromJson(json, JobNG.class);
			try {
				job.context = (JobContext) Class.forName(context_class).newInstance();
				job.context.contextFromJson(json.getAsJsonObject("context"));
			} catch (Exception e) {
				throw new JsonParseException("Invalid context class", e);
			}
			return job;
		}
		
		public JsonElement serialize(JobNG src, Type typeOfSrc, JsonSerializationContext jcontext) {
			JsonObject result = (JsonObject) AppManager.getGson().toJsonTree(src);
			result.addProperty("context_class", src.getClass().getName());
			result.add("context", src.context.contextToJson());
			return null;
		}
		
	}
	
	public String toString() {
		return AppManager.getPrettyGson().toJson(this);
	}
	
	public class Progression {
		// TODO push/pull DB
		
		private int progress = 0;
		private int progress_size = 0;
		private int step = 0;
		private int step_count = 0;
		private String last_message;
		
		/**
		 * Async update.
		 * @param last_message can be null.
		 */
		public void update(String last_message) {
			update_date = System.currentTimeMillis();
			this.last_message = last_message;
		}
		
		/**
		 * Async update.
		 */
		public void update(int step, int step_count, int progress, int progress_size) {
			update_date = System.currentTimeMillis();
			this.progress = progress;
			this.progress_size = progress_size;
			this.step = step;
			this.step_count = step_count;
		}
	}
	
	// TODO push & pull db
	/*static void selectWaitingForProfileCategory(IndexQuery<String, String> index_query, String category) {
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
	
	 //Get not processing_error and worker
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
	
	}*/
	
}
