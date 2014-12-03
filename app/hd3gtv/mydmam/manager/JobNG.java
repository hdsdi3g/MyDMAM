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
public final class JobNG {
	
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
				CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "updatedate", queue_name + "_updatedate", DeployColumnDef.ColType_LongType);
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
	
	private static int TTL = 3600 * 24 * 7;
	
	public enum JobStatus {
		TOO_OLD, CANCELED, POSTPONED, WAITING, DONE, PROCESSING, STOPPED, ERROR, PREPARING;
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
	private Class<?> creator;
	private boolean urgent;
	@SuppressWarnings("unused")
	private String name;
	private long expiration_date;
	@SuppressWarnings("unused")
	private long max_execution_time;
	private String require_key;
	@SuppressWarnings("unused")
	private long create_date;
	private boolean delete_after_completed;
	@SuppressWarnings("unused")
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
	@SuppressWarnings("unused")
	private GsonThrowable processing_error;
	@SuppressWarnings("unused")
	private JobProgression progression;
	@SuppressWarnings("unused")
	private long start_date;
	@SuppressWarnings("unused")
	private long end_date;
	@SuppressWarnings("unused")
	private String worker_reference;
	@SuppressWarnings("unused")
	private Class<?> worker_class;
	@SuppressWarnings("unused")
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
		expiration_date = Long.MAX_VALUE;
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
		require_key = require.key;
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
	
	boolean isDeleteAfterCompleted() {
		return delete_after_completed;
	}
	
	public JobContext getContext() {
		return context;
	}
	
	public void publish() throws ConnectionException {
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
		saveChanges();
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
	void saveChanges(MutationBatch mutator) throws ConnectionException {
		update_date = System.currentTimeMillis();
		exportToDatabase(mutator.withRow(CF_QUEUE, key));
		mutator.execute();
	}
	
	static class Serializer implements JsonSerializer<JobNG>, JsonDeserializer<JobNG> {
		public JobNG deserialize(JsonElement jejson, Type typeOfT, JsonDeserializationContext jcontext) throws JsonParseException {
			JsonObject json = (JsonObject) jejson;
			String context_class = json.get("context_class").getAsString();
			json.remove("context_class");
			
			JobNG job = AppManager.getGson().fromJson(json, JobNG.class);
			try {
				job.context = AppManager.instanceClassForName(context_class, JobContext.class);
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
		return new JobProgression(this);
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
	
	static JobNG importFromDatabase(ColumnList<String> columnlist) {
		return AppManager.getGson().fromJson(columnlist.getColumnByName("source").getStringValue(), JobNG.class);
	}
	
	static List<JobNG> watchOldAbandonedJobs(MutationBatch mutator, String hostname) throws ConnectionException {
		IndexQuery<String, String> index_query = keyspace.prepareQuery(CF_QUEUE).searchWithIndex();
		index_query.addExpression().whereColumn("status").equals().value(JobStatus.WAITING.name());
		index_query.addExpression().whereColumn("creator_hostname").equals().value(hostname);
		index_query.addExpression().whereColumn("expiration_date").lessThan().value(System.currentTimeMillis());
		index_query.withColumnSlice("source");
		
		List<JobNG> result = new ArrayList<JobNG>();
		
		OperationResult<Rows<String, String>> rows = index_query.execute();
		for (Row<String, String> row : rows.getResult()) {
			JobNG job = JobNG.importFromDatabase(row.getColumns());
			job.status = JobStatus.TOO_OLD;
			job.update_date = System.currentTimeMillis();
			job.exportToDatabase(mutator.withRow(CF_QUEUE, job.key));
			result.add(job);
		}
		return result;
	}
	
	static List<JobNG> removeMaxDateForPostponedJobs(MutationBatch mutator, String hostname) throws ConnectionException {
		IndexQuery<String, String> index_query = keyspace.prepareQuery(CF_QUEUE).searchWithIndex();
		index_query.addExpression().whereColumn("status").equals().value(JobStatus.POSTPONED.name());
		index_query.addExpression().whereColumn("creator_hostname").equals().value(hostname);
		index_query.addExpression().whereColumn("expiration_date").lessThan().value(Long.MAX_VALUE);
		index_query.withColumnSlice("source");
		
		List<JobNG> result = new ArrayList<JobNG>();
		
		OperationResult<Rows<String, String>> rows = index_query.execute();
		for (Row<String, String> row : rows.getResult()) {
			JobNG job = JobNG.importFromDatabase(row.getColumns());
			job.expiration_date = Long.MAX_VALUE;
			job.update_date = System.currentTimeMillis();
			job.exportToDatabase(mutator.withRow(CF_QUEUE, job.key));
			result.add(job);
		}
		return result;
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
	
	public void delete(MutationBatch mutator) throws ConnectionException {
		mutator.withRow(CF_QUEUE, key).delete();
	}
	
	public static void dropJobsQueueCF() throws ConnectionException {
		CassandraDb.dropColumnFamilyString(keyspace, CF_QUEUE.getName());
	}
	
	public JobStatus getStatus() {
		return status;
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
			result.add(JobNG.importFromDatabase(row.getColumns()));
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
			index_query.addExpression().whereColumn("updatedate").greaterThanEquals().value(since_date);
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
