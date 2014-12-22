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
import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.db.AllRowsFoundRow;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.db.DeployColumnDef;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.json.simple.parser.ParseException;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.query.IndexQuery;
import com.netflix.astyanax.recipes.locks.ColumnPrefixDistributedRowLock;
import com.netflix.astyanax.serializers.StringSerializer;

/**
 * Use AppManager to for create job.
 */
public final class JobNG implements Log2Dumpable {
	
	private static final ColumnFamily<String, String> CF_QUEUE = new ColumnFamily<String, String>("mgrQueue", StringSerializer.get(), StringSerializer.get());
	private static Keyspace keyspace;
	
	static {
		try {
			keyspace = CassandraDb.getkeyspace();
			String default_keyspacename = CassandraDb.getDefaultKeyspacename();
			if (CassandraDb.isColumnFamilyExists(keyspace, CF_QUEUE.getName()) == false) {
				CassandraDb.createColumnFamilyString(default_keyspacename, CF_QUEUE.getName(), false);
				String queue_name = CF_QUEUE.getName();
				CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "status", queue_name + "_status", DeployColumnDef.ColType_AsciiType);
				CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "creator_hostname", queue_name + "_creator_hostname", DeployColumnDef.ColType_UTF8Type);
				CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "expiration_date", queue_name + "_expiration_date", DeployColumnDef.ColType_LongType);
				CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "update_date", queue_name + "_update_date", DeployColumnDef.ColType_LongType);
				// CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "delete", queue_name + "_delete", DeployColumnDef.ColType_Int32Type);
				CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "indexingdebug", queue_name + "_indexingdebug", DeployColumnDef.ColType_Int32Type);
			}
		} catch (Exception e) {
			Log2.log.error("Can't init database CFs", e);
		}
	}
	
	static ColumnPrefixDistributedRowLock<String> prepareLock() {
		return new ColumnPrefixDistributedRowLock<String>(keyspace, CF_QUEUE, "BROKER_LOCK");
	}
	
	final static int TTL = 3600 * 24 * 7;
	
	public enum JobStatus {
		TOO_OLD, CANCELED, POSTPONED, WAITING, DONE, PROCESSING, STOPPED, ERROR, PREPARING, TOO_LONG_DURATION;
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
	private Class<?> creator;
	private boolean urgent;
	private String name;
	private long expiration_date;
	private long max_execution_time;
	private static final long default_max_execution_time = 1000 * 3600 * 24;
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
	long update_date;
	private GsonThrowable processing_error;
	private JobProgression progression;
	private long start_date;
	private long end_date;
	private String worker_reference;
	private Class<?> worker_class;
	private String instance_status_executor_key;
	@SuppressWarnings("unused")
	private String instance_status_executor_hostname;
	
	JobNG(AppManager manager, JobContext context) throws ClassNotFoundException {
		this.context = context;
		MyDMAM.checkIsAccessibleClass(context.getClass(), false);
		key = "job:" + UUID.randomUUID().toString();
		name = "Generic job created at " + (new Date()).toString();
		urgent = false;
		priority = 0;
		delete_after_completed = false;
		expiration_date = System.currentTimeMillis() + (default_max_execution_time * 7);
		max_execution_time = default_max_execution_time;
		status = JobStatus.WAITING;
		
		instance_status_creator_key = manager.getInstance_status().getInstanceNamePid();
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
	
	public JobNG setCreator(Class<?> creator) {
		this.creator = creator;
		return this;
	}
	
	public JobNG setUrgent() {
		urgent = true;
		return this;
	}
	
	public JobNG setRequireCompletedJob(JobNG require) {
		if (require != null) {
			require_key = require.key;
		}
		return this;
	}
	
	boolean isRequireIsDone() throws ConnectionException {
		if (require_key == null) {
			return true;
		}
		ColumnList<String> cols = keyspace.prepareQuery(CF_QUEUE).getKey(require_key).withColumnSlice("status").execute().getResult();
		if (cols == null) {
			return false;
		}
		if (cols.isEmpty()) {
			return false;
		}
		if (cols.getStringValue("status", JobStatus.WAITING.name()).equals(JobStatus.DONE.name())) {
			return true;
		}
		return false;
	}
	
	boolean isTooOldjob() {
		return (expiration_date < System.currentTimeMillis());
	}
	
	int getPriority() {
		return priority;
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
	
	boolean isMaxExecutionTimeIsReached() {
		if (getStatus() != JobStatus.PROCESSING) {
			return false;
		}
		if (start_date == -1) {
			return false;
		}
		return start_date + max_execution_time < System.currentTimeMillis();
		
	}
	
	boolean hasAMaxExecutionTime() {
		return max_execution_time < default_max_execution_time;
	}
	
	/**
	 * Doesn't change current status if POSTPONED, PROCESSING or PREPARING.
	 */
	public JobNG setPostponed() {
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
	
	boolean isDeleteAfterCompleted() {
		return delete_after_completed;
	}
	
	public JobContext getContext() {
		return context;
	}
	
	public void publish() throws ConnectionException {
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		publish(mutator);
		mutator.execute();
	}
	
	public void publish(MutationBatch mutator) throws ConnectionException {
		create_date = System.currentTimeMillis();
		if (urgent) {
			/**
			 * Compute priority based on active wait job count.
			 */
			IndexQuery<String, String> index_query = keyspace.prepareQuery(CF_QUEUE).searchWithIndex();
			index_query.addExpression().whereColumn("status").equals().value(JobStatus.WAITING.name());
			index_query.withColumnSlice("status", "name");
			OperationResult<Rows<String, String>> rows = index_query.execute();
			priority = rows.getResult().size() + 1;
		}
		saveChanges(mutator);
	}
	
	/**
	 * To force changes (sync method) with set() functions.
	 * If job is actually POSTPONED, PROCESSING or PREPARING, changes will be canceled by the executor worker.
	 * @throws ConnectionException
	 */
	void saveChanges() throws ConnectionException {
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		saveChanges(mutator);
		mutator.execute();
	}
	
	/**
	 * To force changes (sync method) with set() functions.
	 * If job is actually POSTPONED, PROCESSING or PREPARING, changes will be canceled by the executor worker.
	 * @throws ConnectionException
	 */
	void saveChanges(MutationBatch mutator) {
		update_date = System.currentTimeMillis();
		exportToDatabase(mutator.withRow(CF_QUEUE, key));
	}
	
	static class Serializer implements JsonSerializer<JobNG>, JsonDeserializer<JobNG> {
		public JobNG deserialize(JsonElement jejson, Type typeOfT, JsonDeserializationContext jcontext) throws JsonParseException {
			JsonObject json = (JsonObject) jejson;
			JobNG job = AppManager.getSimpleGson().fromJson(json, JobNG.class);
			job.context = AppManager.getGson().fromJson(json.get("context"), JobContext.class);
			return job;
		}
		
		public JsonElement serialize(JobNG src, Type typeOfSrc, JsonSerializationContext jcontext) {
			JsonObject result = (JsonObject) AppManager.getSimpleGson().toJsonTree(src);
			result.add("context", AppManager.getGson().toJsonTree(src.context, JobContext.class));
			return result;
		}
	}
	
	public JsonObject toJson() {
		return AppManager.getGson().toJsonTree(this).getAsJsonObject();
	}
	
	/**
	 * @param mutator update new status and new update_date to DB, to protect new next job search to found it.
	 */
	void prepareProcessing(MutationBatch mutator) {
		update_date = System.currentTimeMillis();
		status = JobStatus.PREPARING;
		mutator.withRow(CF_QUEUE, key).putColumn("status", status.name(), TTL);
		mutator.withRow(CF_QUEUE, key).putColumn("update_date", update_date, TTL);
	}
	
	JobProgression startProcessing(AppManager manager, WorkerNG worker) {
		update_date = System.currentTimeMillis();
		start_date = update_date;
		status = JobStatus.PROCESSING;
		worker_class = worker.getClass();
		worker_reference = worker.getReferenceKey();
		instance_status_executor_key = manager.getInstance_status().getInstanceNamePid();
		instance_status_executor_hostname = manager.getInstance_status().getHostName();
		progression = new JobProgression(this);
		return progression;
	}
	
	synchronized void endProcessing_Done() {
		update_date = System.currentTimeMillis();
		end_date = update_date;
		status = JobStatus.DONE;
	}
	
	synchronized void endProcessing_Stopped() {
		update_date = System.currentTimeMillis();
		end_date = update_date;
		status = JobStatus.STOPPED;
	}
	
	synchronized void endProcessing_TooLongDuration() {
		update_date = System.currentTimeMillis();
		end_date = update_date;
		status = JobStatus.TOO_LONG_DURATION;
	}
	
	synchronized void endProcessing_Canceled() {
		update_date = System.currentTimeMillis();
		end_date = update_date;
		status = JobStatus.CANCELED;
	}
	
	synchronized void endProcessing_Error(Exception e) {
		update_date = System.currentTimeMillis();
		end_date = update_date;
		status = JobStatus.ERROR;
		processing_error = new GsonThrowable(e);
	}
	
	public JobStatus getStatus() {
		return status;
	}
	
	long getEndDate() {
		return end_date;
	}
	
	public void delete(MutationBatch mutator) throws ConnectionException {
		mutator.withRow(CF_QUEUE, key).delete();
	}
	
	boolean isThisStatus(JobStatus... statuses) {
		if (statuses == null) {
			return false;
		}
		if (statuses.length == 0) {
			return false;
		}
		for (int pos = 0; pos < statuses.length; pos++) {
			if (status == statuses[pos]) {
				return true;
			}
		}
		return false;
	}
	
	public String getKey() {
		return key;
	}
	
	private void exportToDatabase(ColumnListMutation<String> mutator) {
		mutator.putColumn("context_class", context.getClass().getName(), TTL);
		mutator.putColumn("status", status.name(), TTL);
		mutator.putColumn("creator_hostname", instance_status_creator_hostname, TTL);
		mutator.putColumn("expiration_date", expiration_date, TTL);
		mutator.putColumn("update_date", update_date, TTL);
		mutator.putColumn("delete_after_completed", delete_after_completed, TTL);
		/**
		 * Workaround for Cassandra index select bug.
		 */
		mutator.putColumn("indexingdebug", 1, TTL);
		mutator.putColumn("source", AppManager.getGson().toJson(this), TTL);
	}
	
	public static final class Utility {
		
		static JobNG importFromDatabase(ColumnList<String> columnlist) {
			return AppManager.getGson().fromJson(columnlist.getColumnByName("source").getStringValue(), JobNG.class);
		}
		
		static List<JobNG> watchOldAbandonedJobs(MutationBatch mutator, InstanceStatus instance_status) throws ConnectionException {
			IndexQuery<String, String> index_query = keyspace.prepareQuery(CF_QUEUE).searchWithIndex();
			index_query.addExpression().whereColumn("status").equals().value(JobStatus.WAITING.name());
			index_query.addExpression().whereColumn("creator_hostname").equals().value(instance_status.getHostName());
			index_query.addExpression().whereColumn("expiration_date").lessThan().value(System.currentTimeMillis());
			index_query.withColumnSlice("source");
			
			List<JobNG> result = new ArrayList<JobNG>();
			
			OperationResult<Rows<String, String>> rows = index_query.execute();
			for (Row<String, String> row : rows.getResult()) {
				JobNG job = JobNG.Utility.importFromDatabase(row.getColumns());
				job.status = JobStatus.TOO_OLD;
				job.update_date = System.currentTimeMillis();
				job.exportToDatabase(mutator.withRow(CF_QUEUE, job.key));
				result.add(job);
			}
			return result;
		}
		
		static void removeMaxDateForPostponedJobs(MutationBatch mutator, InstanceStatus instance_status) throws ConnectionException {
			IndexQuery<String, String> index_query = keyspace.prepareQuery(CF_QUEUE).searchWithIndex();
			index_query.addExpression().whereColumn("status").equals().value(JobStatus.POSTPONED.name());
			index_query.addExpression().whereColumn("creator_hostname").equals().value(instance_status.getHostName());
			index_query.addExpression().whereColumn("expiration_date").lessThan().value(Long.MAX_VALUE);
			index_query.withColumnSlice("source");
			
			OperationResult<Rows<String, String>> rows = index_query.execute();
			for (Row<String, String> row : rows.getResult()) {
				JobNG job = JobNG.Utility.importFromDatabase(row.getColumns());
				job.expiration_date = System.currentTimeMillis() + (default_max_execution_time * 7);
				job.update_date = System.currentTimeMillis();
				job.exportToDatabase(mutator.withRow(CF_QUEUE, job.key));
			}
		}
		
		public static void truncateAllJobs() throws ConnectionException {
			CassandraDb.truncateColumnFamilyString(keyspace, CF_QUEUE.getName());
		}
		
		public static void removeJobsByStatus(MutationBatch mutator, JobStatus status) throws ConnectionException {
			IndexQuery<String, String> index_query = keyspace.prepareQuery(CF_QUEUE).searchWithIndex();
			index_query.addExpression().whereColumn("status").equals().value(status.name());
			index_query.withColumnSlice("status");
			
			OperationResult<Rows<String, String>> rows = index_query.execute();
			for (Row<String, String> row : rows.getResult()) {
				mutator.withRow(CF_QUEUE, row.getKey()).delete();
			}
		}
		
		public static void dropJobsQueueCF() throws ConnectionException {
			CassandraDb.dropColumnFamilyString(keyspace, CF_QUEUE.getName());
		}
		
		/**
		 * @return never null if keys is not empty
		 */
		public static LinkedHashMap<String, JobStatus> getJobsStatusByKeys(Collection<String> keys) throws ConnectionException {
			if (keys == null) {
				return null;
			}
			if (keys.size() == 0) {
				return null;
			}
			LinkedHashMap<String, JobStatus> status = new LinkedHashMap<String, JobStatus>(keys.size());
			Rows<String, String> rows = keyspace.prepareQuery(CF_QUEUE).getKeySlice(keys).withColumnSlice("status").execute().getResult();
			if (rows.isEmpty()) {
				return status;
			}
			for (Row<String, String> row : rows) {
				if (row.getColumns().isEmpty()) {
					continue;
				}
				status.put(row.getKey(), JobStatus.valueOf(row.getColumns().getStringValue("status", JobStatus.POSTPONED.name())));
			}
			return status;
		}
		
		static List<JobNG> getJobsByStatus(JobStatus status) throws ConnectionException {
			IndexQuery<String, String> index_query = keyspace.prepareQuery(CF_QUEUE).searchWithIndex();
			index_query.addExpression().whereColumn("status").equals().value(status.name());
			index_query.withColumnSlice("source");
			
			OperationResult<Rows<String, String>> rows = index_query.execute();
			ArrayList<JobNG> result = new ArrayList<JobNG>();
			for (Row<String, String> row : rows.getResult()) {
				JobNG new_job = JobNG.Utility.importFromDatabase(row.getColumns());
				result.add(new_job);
			}
			return result;
		}
		
		/**
		 * @return never null if keys is not empty
		 */
		public static JsonObject getJsonJobsByKeys(Collection<String> keys) throws ConnectionException, ParseException {
			if (keys == null) {
				return null;
			}
			if (keys.size() == 0) {
				return null;
			}
			JsonObject result = new JsonObject();
			List<JobNG> jobs = getJobsByKeys(keys);
			for (int pos = 0; pos < jobs.size(); pos++) {
				result.add(jobs.get(pos).key, AppManager.getGson().toJsonTree(jobs.get(pos)));
			}
			return result;
		}
		
		/**
		 * @return never null if keys is not empty
		 */
		public static List<JobNG> getJobsByKeys(Collection<String> keys) throws ConnectionException, ParseException {
			if (keys == null) {
				return null;
			}
			if (keys.size() == 0) {
				return null;
			}
			List<JobNG> result = new ArrayList<JobNG>(keys.size());
			Rows<String, String> rows = keyspace.prepareQuery(CF_QUEUE).getKeySlice(keys).withColumnSlice("source").execute().getResult();
			for (Row<String, String> row : rows) {
				String source = row.getColumns().getStringValue("source", "{}");
				if (source.equals("{}")) {
					continue;
				}
				result.add(AppManager.getGson().fromJson(source, JobNG.class));
			}
			return result;
		}
		
		/**
		 * @param since_date set to 0 for disabled range selection
		 */
		public static JsonObject getJobsFromUpdateDate(long since_date) throws Exception {
			final JsonObject result = new JsonObject();
			if (since_date == 0) {
				CassandraDb.allRowsReader(CF_QUEUE, new AllRowsFoundRow() {
					public void onFoundRow(Row<String, String> row) throws Exception {
						String source = row.getColumns().getStringValue("source", "{}");
						if (source.equals("{}")) {
							return;
						}
						JobNG current = AppManager.getGson().fromJson(source, JobNG.class);
						result.add(current.key, AppManager.getGson().toJsonTree(current));
					}
				});
			} else {
				IndexQuery<String, String> index_query = keyspace.prepareQuery(CF_QUEUE).searchWithIndex();
				index_query.addExpression().whereColumn("indexingdebug").equals().value(1);
				index_query.addExpression().whereColumn("update_date").greaterThanEquals().value(since_date - 1000);
				OperationResult<Rows<String, String>> rows = index_query.execute();
				
				for (Row<String, String> row : rows.getResult()) {
					String source = row.getColumns().getStringValue("source", "{}");
					if (source.equals("{}")) {
						continue;
					}
					JobNG current = AppManager.getGson().fromJson(source, JobNG.class);
					result.add(current.key, AppManager.getGson().toJsonTree(current));
				}
			}
			return result;
		}
	}
	
	public String toString() {
		return AppManager.getPrettyGson().toJson(this);
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("key", key);
		dump.add("creator", creator.getName());
		dump.add("urgent", urgent);
		dump.add("max_execution_time", max_execution_time);
		dump.add("name", name);
		dump.add("priority", priority);
		dump.add("context", AppManager.getGson().toJson(context, JobContext.class));
		dump.add("status", status);
		dump.add("progression", progression);
		dump.addDate("create_date", create_date);
		dump.addDate("expiration_date", expiration_date);
		dump.addDate("update_date", update_date);
		dump.addDate("start_date", start_date);
		dump.addDate("end_date", end_date);
		dump.add("require_key", require_key);
		dump.add("delete_after_completed", delete_after_completed);
		dump.add("executor", instance_status_executor_key);
		dump.add("creator", instance_status_creator_key);
		if (processing_error != null) {
			dump.add("processing error", processing_error.getPrintedStackTrace());
		}
		dump.add("worker_reference", worker_reference);
		if (worker_class != null) {
			dump.add("worker_class", worker_class.getName());
		}
		return dump;
	}
}
