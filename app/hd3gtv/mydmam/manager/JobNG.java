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
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.model.ColumnList;

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
	
	@SuppressWarnings("unused")
	private JobNG() {
		/**
		 * For Gson, and from DB
		 */
	}
	
	/**
	 * Declaration & configuration vars
	 */
	private String key;
	@SuppressWarnings("unused")
	private Class creator;
	private boolean urgent;
	@SuppressWarnings("unused")
	private String name;
	private long expiration_date;
	@SuppressWarnings("unused")
	private long max_execution_time;
	private String require_key;
	private long create_date;
	@SuppressWarnings("unused")
	private boolean delete_after_completed;
	@SuppressWarnings("unused")
	private String instance_status_creator_key;
	@SuppressWarnings("unused")
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
		progression = null;
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
			result.addProperty("context_class", src.context.getClass().getName());
			result.add("context", src.context.contextToJson());
			return null;
		}
		
	}
	
	public String toString() {
		return AppManager.getPrettyGson().toJson(this);
	}
	
	public JsonObject toJson() {
		return AppManager.getGson().toJsonTree(this).getAsJsonObject();
	}
	
	public class Progression {
		private volatile int progress = 0;
		private volatile int progress_size = 0;
		private volatile int step = 0;
		private volatile int step_count = 0;
		private volatile String last_message;
		
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
		public void updateStep(int step, int step_count) {
			update_date = System.currentTimeMillis();
			this.step = step;
			this.step_count = step_count;
		}
		
		/**
		 * Async update.
		 */
		public void updateProgress(int progress, int progress_size) {
			update_date = System.currentTimeMillis();
			this.progress = progress;
			this.progress_size = progress_size;
		}
	}
	
	// TODO call by broker.
	void prepareProcessing(AppManager manager, WorkerNG worker) {
		update_date = System.currentTimeMillis();
		status = JobStatus.PREPARING;
		worker_class = worker.getClass();
		worker_reference = worker.getReference();
		instance_status_executor_key = manager.getInstance_status().getDatabaseKey();
		instance_status_executor_hostname = manager.getInstance_status().getHostName();
	}
	
	Progression startProcessing() {
		update_date = System.currentTimeMillis();
		start_date = update_date;
		status = JobStatus.PROCESSING;
		progression = new Progression();
		return progression;
	}
	
	void endProcessing_Done() {
		update_date = System.currentTimeMillis();
		end_date = update_date;
		status = JobStatus.DONE;
	}
	
	void endProcessing_Stopped() {
		update_date = System.currentTimeMillis();
		end_date = update_date;
		status = JobStatus.STOPPED;
	}
	
	public void endProcessing_Canceled() {
		update_date = System.currentTimeMillis();
		end_date = update_date;
		status = JobStatus.CANCELED;
	}
	
	void endProcessing_Error(Exception e) {
		update_date = System.currentTimeMillis();
		end_date = update_date;
		status = JobStatus.ERROR;
		processing_error = new GsonThrowable(e);
	}
	
	/*static void selectWaitingForProfileCategory(IndexQuery<String, String> index_query, String category) {
		index_query.addExpression().whereColumn("status").equals().value(TaskJobStatus.WAITING.name().toUpperCase());
		index_query.addExpression().whereColumn("profile_category").equals().value(category.toLowerCase());
	}
	
	static void selectTooOldWaitingTasks(IndexQuery<String, String> index_query, String hostname) {
		index_query.addExpression().whereColumn("status").equals().value(TaskJobStatus.WAITING.name().toUpperCase());
		index_query.addExpression().whereColumn("creator_hostname").equals().value(hostname);
		index_query.addExpression().whereColumn("max_date_to_wait_processing").lessThan().value(System.currentTimeMillis());
	}
	
	static void selectPostponedTasksWithMaxAge(IndexQuery<String, String> index_query, String hostname) {
		index_query.addExpression().whereColumn("status").equals().value(TaskJobStatus.POSTPONED.name().toUpperCase());
		index_query.addExpression().whereColumn("creator_hostname").equals().value(hostname);
		index_query.addExpression().whereColumn("max_date_to_wait_processing").lessThan().value(Long.MAX_VALUE);
	}
	
	static void selectAllLastTasksAndJobs(IndexQuery<String, String> index_query, long since_date) {
		index_query.addExpression().whereColumn("indexingdebug").equals().value(1);
		index_query.addExpression().whereColumn("updatedate").greaterThanEquals().value(since_date);
	}*/
	
	// TODO push & pull db
	/*
	 * */
	
	void exportToDatabase(ColumnListMutation<String> mutator) {
		mutator.putColumn("context_class", context.getClass().getName());
		mutator.putColumn("status", status.name());
		mutator.putColumn("creator_hostname", instance_status_creator_hostname);
		mutator.putColumn("expiration_date", expiration_date);
		mutator.putColumn("update_date", update_date);
		mutator.putColumn("delete_after_completed", delete_after_completed);
		mutator.putColumn("source", AppManager.getGson().toJson(this));
	}
	
	public String getDatabaseKey() {
		return key;
	}
	
	static JobNG importFromDatabase(ColumnList<String> columnlist) {
		return AppManager.getGson().fromJson(columnlist.getColumnByName("source").getStringValue(), JobNG.class);
	}
	/*
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
		
		processing_error = columns.getStringValue("processing_error", "");
		progress = columns.getIntegerValue("progress", 0);
		progress_size = columns.getIntegerValue("progress_size", 0);
		step = columns.getIntegerValue("step", 0);
		step_count = columns.getIntegerValue("step_count", 0);
		start_date = columns.getLongValue("start_date", 0l);
		end_date = columns.getLongValue("end_date", 0l);
		last_message = columns.getStringValue("last_message", "");
	}
	
	void pushToDatabaseEndLifejob(MutationBatch mutator, int ttl) {
		mutator.withRow(Broker.CF_TASKQUEUE, key).putColumn("delete", 1, ttl);
	}
	
	static void selectEndLifeJobs(IndexQuery<String, String> index_query) {
		index_query.addExpression().whereColumn("delete").equals().value(1);
		index_query.withColumnSlice("delete");
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
