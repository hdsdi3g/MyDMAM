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

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.db.AllRowsFoundRow;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.db.DeployColumnDef;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.ConsistencyLevel;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.query.IndexQuery;
import com.netflix.astyanax.recipes.locks.BusyLockException;
import com.netflix.astyanax.recipes.locks.ColumnPrefixDistributedRowLock;
import com.netflix.astyanax.recipes.locks.StaleLockException;
import com.netflix.astyanax.serializers.StringSerializer;

@SuppressWarnings("unchecked")
@Deprecated
public class Broker {
	
	static final ColumnFamily<String, String> CF_TASKQUEUE = new ColumnFamily<String, String>("taskqueue", StringSerializer.get(), StringSerializer.get());
	static final ColumnFamily<String, String> CF_WORKERGROUPS = new ColumnFamily<String, String>("workergroups", StringSerializer.get(), StringSerializer.get());
	static final ColumnFamily<String, String> CF_DONEPROFILES = new ColumnFamily<String, String>("doneprofiles", StringSerializer.get(), StringSerializer.get());
	static final ColumnFamily<String, String> CF_QUEUETRIGGER = new ColumnFamily<String, String>("triggerqueue", StringSerializer.get(), StringSerializer.get());
	
	static Keyspace keyspace = null;
	static String hostname;
	static int ttl_task_duration = 3600 * 24 * 7;
	static int ttl_job_process_duration = 120;
	static int ttl_job_ended_duration = 3600 * 24;
	static int ttl_job_cyclic_endlife_duration = 30;
	static int ttl_worker_duration = (int) (WorkerGroup.sleep_refresh_engine / 1000) * 10;
	
	static {
		try {
			if (CassandraDb.isColumnFamilyExists(CassandraDb.getkeyspace(), CF_TASKQUEUE.getName()) == false) {
				CassandraDb.createColumnFamilyString(CassandraDb.getDefaultKeyspacename(), CF_TASKQUEUE.getName(), false);
				CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_TASKQUEUE, "status", CF_TASKQUEUE.getName() + "_status", DeployColumnDef.ColType_AsciiType);
				CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_TASKQUEUE, "profile_category", CF_TASKQUEUE.getName() + "_profile_category", DeployColumnDef.ColType_UTF8Type);
				CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_TASKQUEUE, "max_date_to_wait_processing", CF_TASKQUEUE.getName() + "_max_date_to_wait_processing",
						DeployColumnDef.ColType_LongType);
				CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_TASKQUEUE, "creator_hostname", CF_TASKQUEUE.getName() + "_creator_hostname", DeployColumnDef.ColType_UTF8Type);
				CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_TASKQUEUE, "updatedate", CF_TASKQUEUE.getName() + "_updatedate", DeployColumnDef.ColType_LongType);
				CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_TASKQUEUE, "delete", CF_TASKQUEUE.getName() + "_delete", DeployColumnDef.ColType_Int32Type);
				CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_TASKQUEUE, "indexingdebug", CF_TASKQUEUE.getName() + "_indexingdebug", DeployColumnDef.ColType_Int32Type);
			}
			if (CassandraDb.isColumnFamilyExists(CassandraDb.getkeyspace(), CF_WORKERGROUPS.getName()) == false) {
				CassandraDb.createColumnFamilyString(CassandraDb.getDefaultKeyspacename(), CF_WORKERGROUPS.getName(), false);
			}
			if (CassandraDb.isColumnFamilyExists(CassandraDb.getkeyspace(), CF_DONEPROFILES.getName()) == false) {
				CassandraDb.createColumnFamilyString(CassandraDb.getDefaultKeyspacename(), CF_DONEPROFILES.getName(), true);
			}
			if (CassandraDb.isColumnFamilyExists(CassandraDb.getkeyspace(), CF_QUEUETRIGGER.getName()) == false) {
				CassandraDb.createColumnFamilyString(CassandraDb.getDefaultKeyspacename(), CF_QUEUETRIGGER.getName(), false);
			}
			
			keyspace = CassandraDb.getkeyspace();
		} catch (ConnectionException e) {
			Log2.log.error("Can't prepare Cassandra connection", e);
		}
		
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			Log2.log.error("Can't resolve localshost name", e);
			hostname = "UnknownHost-" + System.currentTimeMillis();
		}
	}
	
	private BrokerQueue queue;
	
	Keyspace getKeyspace() {
		return keyspace;
	}
	
	/**
	 * Enable queue management.
	 * Use static methods for not.
	 */
	public Broker() throws ConnectionException {
	}
	
	public void stop() {
		if (queue == null) {
			return;
		}
		queue.stopDemand();
	}
	
	public void start() {
		if (queue == null) {
			queue = new BrokerQueue(this);
			queue.start();
		} else {
			if (queue.isAlive()) {
				return;
			}
			queue = new BrokerQueue(this);
			queue.start();
		}
	}
	
	public static void truncateTaskqueue() throws ConnectionException {
		CassandraDb.truncateColumnFamilyString(keyspace, CF_TASKQUEUE.getName());
		CassandraDb.truncateColumnFamilyString(keyspace, CF_DONEPROFILES.getName());
		CassandraDb.truncateColumnFamilyString(keyspace, CF_QUEUETRIGGER.getName());
		Log2Dump dump = new Log2Dump();
		dump.add("CF", CF_TASKQUEUE.getName());
		dump.add("CF", CF_DONEPROFILES.getName());
		dump.add("CF", CF_QUEUETRIGGER.getName());
		Log2.log.info("Truncate", dump);
	}
	
	public static void truncateWorkergroups() throws ConnectionException {
		CassandraDb.truncateColumnFamilyString(keyspace, CF_WORKERGROUPS.getName());
		Log2.log.info("Truncate", new Log2Dump("CF", CF_WORKERGROUPS.getName()));
	}
	
	static void dropCFs() throws ConnectionException {
		CassandraDb.dropColumnFamilyString(keyspace, CF_WORKERGROUPS.getName());
		Log2.log.info("DROP", new Log2Dump("CF", CF_WORKERGROUPS.getName()));
		
		CassandraDb.dropColumnFamilyString(keyspace, CF_TASKQUEUE.getName());
		Log2.log.info("DROP", new Log2Dump("CF", CF_TASKQUEUE.getName()));
		
		CassandraDb.dropColumnFamilyString(keyspace, CF_DONEPROFILES.getName());
		Log2.log.info("DROP", new Log2Dump("CF", CF_DONEPROFILES.getName()));
		
		CassandraDb.dropColumnFamilyString(keyspace, CF_QUEUETRIGGER.getName());
		Log2.log.info("DROP", new Log2Dump("CF", CF_QUEUETRIGGER.getName()));
	}
	
	static Job getTaskOrJobByKey(String taskjob_key) throws ConnectionException, ParseException {
		if (taskjob_key == null) {
			throw new NullPointerException("\"taskjob_key\" can't to be null");
		}
		if (taskjob_key.equals("")) {
			throw new NullPointerException("\"taskjob_key\" can't to be empty");
		}
		ColumnList<String> columns = keyspace.prepareQuery(CF_TASKQUEUE).getKey(taskjob_key).execute().getResult();
		if (Task.isEmptyDbEntry(columns)) {
			return null;
		}
		Job job = new Job();
		job.pullFromDatabase(taskjob_key, columns);
		return job;
	}
	
	private static TaskJobStatus getStatusTaskOrJobByKey(String taskjob_key) throws ConnectionException {
		if (taskjob_key == null) {
			throw new NullPointerException("\"taskjob_key\" can't to be null");
		}
		if (taskjob_key.equals("")) {
			return null;
		}
		ColumnList<String> columns = keyspace.prepareQuery(CF_TASKQUEUE).getKey(taskjob_key).withColumnSlice("status").execute().getResult();
		if (columns.isEmpty()) {
			return null;
		}
		return TaskJobStatus.pullFromDatabase(columns);
	}
	
	void updateWorkersStatus(ArrayList<Worker> workerlist, ArrayList<WorkerCyclicEngine> workercycliclist) throws ConnectionException {
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		for (int pos = 0; pos < workerlist.size(); pos++) {
			workerlist.get(pos).pushToDatabase(mutator, hostname, ttl_worker_duration);
		}
		for (int pos = 0; pos < workercycliclist.size(); pos++) {
			workercycliclist.get(pos).pushToDatabase(mutator, hostname, ttl_worker_duration);
		}
		mutator.execute();
	}
	
	void doneJob(Job job) throws ConnectionException {
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		
		String profile_key = job.profile.computeKey();
		mutator.withRow(CF_DONEPROFILES, profile_key).putColumnIfNotNull("name", job.name);
		mutator.withRow(CF_DONEPROFILES, profile_key).putColumnIfNotNull("profile_name", job.profile.name.toLowerCase());
		mutator.withRow(CF_DONEPROFILES, profile_key).putColumnIfNotNull("profile_category", job.profile.category.toLowerCase());
		if (job.context != null) {
			mutator.withRow(CF_DONEPROFILES, profile_key).putColumnIfNotNull("context", job.context.toJSONString());
		} else {
			mutator.withRow(CF_DONEPROFILES, profile_key).putColumnIfNotNull("context", "{}");
		}
		mutator.withRow(CF_DONEPROFILES, profile_key).putColumnIfNotNull("end_date", job.end_date);
		mutator.withRow(CF_DONEPROFILES, profile_key).putColumnIfNotNull("last_message", job.last_message);
		
		mutator.execute();
	}
	
	boolean isRecentJobIsEnded(Profile profile, long precedent_date) throws ConnectionException {
		if (profile == null) {
			throw new NullPointerException("\"profile\" can't to be null");
		}
		ColumnList<String> columns = keyspace.prepareQuery(CF_DONEPROFILES).getKey(profile.computeKey()).execute().getResult();
		if (columns.isEmpty()) {
			return false;
		}
		return (columns.getLongValue("end_date", 0l) > precedent_date);
	}
	
	public static JSONObject getAllEndedJobs() throws Exception {
		final JSONObject result = new JSONObject();
		CassandraDb.allRowsReader(CF_DONEPROFILES, new AllRowsFoundRow() {
			public void onFoundRow(Row<String, String> row) throws Exception {
				JSONObject element = new JSONObject();
				
				element.put("name", row.getColumns().getStringValue("name", ""));
				element.put("profile_name", row.getColumns().getStringValue("profile_name", ""));
				element.put("profile_category", row.getColumns().getStringValue("profile_category", ""));
				
				JSONParser parser = new JSONParser();
				JSONObject context = (JSONObject) parser.parse(row.getColumns().getStringValue("context", "{}"));
				element.put("context", context);
				
				element.put("last_message", row.getColumns().getStringValue("last_message", ""));
				element.put("end_date", row.getColumns().getLongValue("end_date", 0l));
				
				result.put(row.getKey(), element);
			}
		});
		return result;
	}
	
	/**
	 * @param name Name to display.
	 * @param profile Profile to process.
	 * @param context All process metadatas.
	 * @param creator Task creator name to display.
	 * @param urgent Set to max priority (on the top of the list).
	 * @param max_date Max date to wait the processing, unix time in msec; 0 to default.
	 * @param require Task key require done to start this new.
	 * @param postponed Wait before process task.
	 * @return task key for eventually set a require.
	 */
	public static String publishTask(String name, Profile profile, JSONObject context, Object creator, boolean urgent, long max_date, String require, boolean postponed) throws ConnectionException {
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		Task task = new Task();
		task.create_date = System.currentTimeMillis();
		task.name = name;
		task.profile = profile;
		task.max_date_to_wait_processing = max_date;
		task.task_key_require_done = require;
		if (context == null) {
			task.context = new JSONObject();
		}
		task.context = context;
		task.creator_hostname = hostname;
		task.creator_instancename = "";
		
		if (creator instanceof Class) {
			task.creator_classname = ((Class<?>) creator).getName();
		} else {
			task.creator_classname = creator.getClass().getName();
		}
		task.cyclic_source = (creator instanceof CyclicCreateTasks);
		task.trigger_source = (creator instanceof TriggerWorker);
		
		task.delete_after_done = task.cyclic_source | task.trigger_source;
		
		if (urgent) {
			task.priority = countTasksFromStatus(TaskJobStatus.WAITING) + 1;
		} else {
			task.priority = 0;
		}
		if (postponed) {
			task.status = TaskJobStatus.POSTPONED;
		} else {
			task.status = TaskJobStatus.WAITING;
		}
		task.pushToDatabase(mutator, ttl_task_duration);
		mutator.execute();
		
		if (task.cyclic_source == false) {
			Log2.log.debug("Publish Task", task.getLog2Dump());
		}
		return task.key;
	}
	
	private static int countTasksFromStatus(TaskJobStatus status) throws ConnectionException {
		IndexQuery<String, String> index_query = keyspace.prepareQuery(CF_TASKQUEUE).searchWithIndex();
		TaskJobStatus.selectByName(index_query, status);
		index_query.withColumnSlice("status", "name");
		OperationResult<Rows<String, String>> rows = index_query.execute();
		
		int count = 0;
		for (Row<String, String> row : rows.getResult()) {
			if (Task.isEmptyDbEntry(row.getColumns()) == false) {
				count++;
			}
		}
		return count;
	}
	
	static String createKey(String prefix, String salt) {
		StringBuffer sb = new StringBuffer();
		sb.append(System.currentTimeMillis());
		sb.append(System.nanoTime());
		if (salt != null) {
			sb.append(salt);
		}
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(sb.toString().getBytes());
			return prefix + ":" + MyDMAM.byteToString(md.digest());
		} catch (NoSuchAlgorithmException e) {
			throw new NullPointerException("NoSuchAlgorithmException ! " + e.getMessage());
		}
	}
	
	Job getNextJob(ArrayList<Profile> available_workersprofile) throws Exception {
		if (available_workersprofile.isEmpty()) {
			return null;
		}
		
		String profile_category;
		ArrayList<String> l_categories = new ArrayList<String>();
		ArrayList<String> l_profiles_for_cat;
		
		/**
		 * Prepare profile categories list
		 * It's a non mutiple item list.
		 */
		boolean exists;
		for (int pos_availcat = 0; pos_availcat < available_workersprofile.size(); pos_availcat++) {
			exists = false;
			for (int pos_cat = 0; pos_cat < l_categories.size(); pos_cat++) {
				if (available_workersprofile.get(pos_availcat).category.equalsIgnoreCase(l_categories.get(pos_cat))) {
					exists = true;
					break;
				}
			}
			if (exists == false) {
				l_categories.add(available_workersprofile.get(pos_availcat).category);
			}
		}
		
		ColumnPrefixDistributedRowLock<String> lock = null;
		int bestpriority;
		Task best_task = new Task();
		TaskJobStatus status_require;
		
		for (int pos_cat = 0; pos_cat < l_categories.size(); pos_cat++) {
			/**
			 * For all available categories for a worker group.
			 */
			profile_category = l_categories.get(pos_cat);
			
			/**
			 * Prepare profile names list by category.
			 * It's normally not a mutiple item list, but it's not a problem if there are mutiple.
			 */
			l_profiles_for_cat = new ArrayList<String>();
			for (int pos_availcat = 0; pos_availcat < available_workersprofile.size(); pos_availcat++) {
				if (available_workersprofile.get(pos_availcat).category.equalsIgnoreCase(profile_category)) {
					l_profiles_for_cat.add(available_workersprofile.get(pos_availcat).name);
				}
			}
			
			try {
				/**
				 * Prepare and acquire lock
				 */
				lock = new ColumnPrefixDistributedRowLock<String>(keyspace, CF_TASKQUEUE, "BROKER_LOCK_FOR_" + profile_category);
				lock.withConsistencyLevel(ConsistencyLevel.CL_ALL);
				lock.expireLockAfter(1, TimeUnit.SECONDS);
				lock.failOnStaleLock(false);
				lock.acquire();
				
				/**
				 * Get all waiting task for this category profile.
				 */
				IndexQuery<String, String> index_query = keyspace.prepareQuery(CF_TASKQUEUE).searchWithIndex();
				Task.selectWaitingForProfileCategory(index_query, profile_category);
				OperationResult<Rows<String, String>> rows = index_query.execute();
				
				best_task = null;
				bestpriority = Integer.MIN_VALUE;
				for (Row<String, String> row : rows.getResult()) {
					/**
					 * For this Tasks, found the best to start.
					 */
					Task task = new Task();
					task.pullFromDatabase(row.getKey(), row.getColumns());
					
					status_require = getStatusTaskOrJobByKey(task.task_key_require_done);
					if (status_require != null) {
						/**
						 * If the task require the processing of another task.
						 */
						if (status_require != TaskJobStatus.DONE) {
							continue;
						}
					}
					if (task.max_date_to_wait_processing < System.currentTimeMillis()) {
						/**
						 * This task is to old !
						 */
						continue;
					}
					
					if (task.priority < bestpriority) {
						/**
						 * This task priority is not the best for the moment
						 */
						continue;
					}
					for (int pos = 0; pos < l_profiles_for_cat.size(); pos++) {
						if (l_profiles_for_cat.get(pos).equalsIgnoreCase(task.profile.name)) {
							/**
							 * Profile name is handled
							 */
							bestpriority = task.priority;
							best_task = task;
							break;
						}
					}
				}
				
				if (best_task == null) {
					/**
					 * Not found a valid Task
					 */
					lock.release();
					continue;
				}
				
				/**
				 * Prepare Job from valid Task
				 */
				Job job = best_task.toJob();
				job.status = TaskJobStatus.PREPARING;
				MutationBatch mutator = CassandraDb.prepareMutationBatch();
				job.pushToDatabase(mutator, ttl_job_process_duration);
				mutator.execute();
				
				/**
				 * Job is ready to start, release lock.
				 */
				lock.release();
				Log2.log.debug("Get next job", job);
				
				if (queue != null) {
					/**
					 * Add Job to active list for watching it.
					 */
					queue.getActivejobs().add(job);
				}
				
				return job;
			} catch (StaleLockException e) {
				/** The row contains a stale or these can either be manually clean up or automatically cleaned up (and ignored) by calling failOnStaleLock(false) */
				Log2.log.error("Can't lock task : abandoned lock...", e, new Log2Dump("category", profile_category));
			} catch (BusyLockException e) {
				Log2.log.error("Can't lock task, this category is currently locked....", e, new Log2Dump("category", profile_category));
			} finally {
				if (lock != null) {
					lock.release();
				}
			}
		}
		return null;
	}
	
	/**
	 * Does not checks
	 */
	public static boolean changeTaskStatus(String task_key, TaskJobStatus status) throws ConnectionException, ParseException {
		Job job = getTaskOrJobByKey(task_key);
		if (job == null) {
			return false;
		}
		job.status = status;
		if (status == TaskJobStatus.WAITING) {
			job.max_date_to_wait_processing = Long.MAX_VALUE;
		}
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		job.pushToDatabase(mutator, ttl_task_duration);
		mutator.execute();
		Log2.log.info("Change task", job);
		return true;
	}
	
	/**
	 * Checks if task is waiting or postponed.
	 */
	public static boolean changeTaskPriority(String task_key, int priority) throws ConnectionException, ParseException {
		Job job = getTaskOrJobByKey(task_key);
		if (job == null) {
			return false;
		}
		if (job.status != TaskJobStatus.WAITING & job.status != TaskJobStatus.POSTPONED) {
			return false;
		}
		job.priority = priority;
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		job.pushToDatabase(mutator, ttl_task_duration);
		mutator.execute();
		Log2.log.info("Change task", job);
		return true;
	}
	
	/**
	 * Checks if task is waiting or postponed and if date_max_age is in the future.
	 */
	public static boolean changeTaskMaxAge(String task_key, long date_max_age) throws ConnectionException, ParseException {
		Job job = getTaskOrJobByKey(task_key);
		if (job == null) {
			Log2.log.error("No task for this key", null, new Log2Dump("key", task_key));
			return false;
		}
		if (job.status != TaskJobStatus.WAITING & job.status != TaskJobStatus.POSTPONED & job.status != TaskJobStatus.TOO_OLD) {
			Log2.log.error("Task is no waiting or postponed", null, new Log2Dump("status", job.status.name()));
			return false;
		}
		if (date_max_age < System.currentTimeMillis()) {
			Log2.log.error("Date can't to be set to the past", null, new Log2Dump("date_max_age", date_max_age));
			return false;
		}
		job.max_date_to_wait_processing = date_max_age;
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		job.pushToDatabase(mutator, ttl_task_duration);
		mutator.execute();
		Log2.log.info("Change task", job);
		return true;
	}
	
	/**
	 * @param period In seconds
	 * @throws ConnectionException
	 */
	public static boolean changeWorkerCyclicPeriod(String worker_ref, int period) throws ConnectionException {
		ColumnList<String> columns = keyspace.prepareQuery(CF_WORKERGROUPS).getKey(worker_ref).execute().getResult();
		if (columns.isEmpty() == false) {
			MutationBatch mutator = CassandraDb.prepareMutationBatch();
			mutator.withRow(Broker.CF_WORKERGROUPS, worker_ref).putColumnIfNotNull(WorkerGroup.COL_NAME_CHANGECYCLICPERIOD, period, ttl_worker_duration * 10);
			mutator.execute();
			
			Log2Dump dump = new Log2Dump();
			dump.add("worker_ref", worker_ref);
			dump.add("period", period);
			Log2.log.info("Change cyclic period", dump);
			
			return true;
		}
		return false;
	}
	
	public static boolean changeWorkerState(String worker_ref, WorkerStatusChange newstate) throws ConnectionException {
		ColumnList<String> columns = keyspace.prepareQuery(CF_WORKERGROUPS).getKey(worker_ref).execute().getResult();
		if (columns.isEmpty() == false) {
			MutationBatch mutator = CassandraDb.prepareMutationBatch();
			newstate.pushToDatabase(mutator, worker_ref, ttl_worker_duration * 10);
			mutator.execute();
			
			Log2Dump dump = new Log2Dump();
			dump.add("worker_ref", worker_ref);
			dump.add("newstate", newstate.name().toUpperCase());
			Log2.log.info("Change state", dump);
			
			return true;
		}
		return false;
	}
	
	/**
	 * @return all present workers status from database
	 */
	public static JSONObject getWorkers() throws Exception {
		final JSONObject jo = new JSONObject();
		CassandraDb.allRowsReader(CF_WORKERGROUPS, new AllRowsFoundRow() {
			public void onFoundRow(Row<String, String> row) throws Exception {
				jo.put(row.getKey(), Worker.pullJSONFromDatabase(row.getColumns()));
			}
		});
		return jo;
	}
	
	@Deprecated
	public static JSONObject getAllTasksAndJobsStatusCount() throws Exception {
		final JSONObject all_status_count = new JSONObject();
		CassandraDb.allRowsReader(CF_TASKQUEUE, new AllRowsFoundRow() {
			public void onFoundRow(Row<String, String> row) throws Exception {
				String status = row.getColumns().getStringValue("status", "");
				if (all_status_count.containsKey(status)) {
					all_status_count.put(status, (Integer) all_status_count.get(status) + 1);
				} else {
					all_status_count.put(status, 1);
				}
			}
		}, "status");
		return all_status_count;
	}
	
	/**
	 * @param since_date set to 0 for disabled range selection
	 */
	public static JSONArray getEndlifeJobs() throws ConnectionException {
		final JSONArray jo = new JSONArray();
		IndexQuery<String, String> index_query = keyspace.prepareQuery(CF_TASKQUEUE).searchWithIndex();
		Job.selectEndLifeJobs(index_query);
		OperationResult<Rows<String, String>> rows = index_query.execute();
		for (Row<String, String> row : rows.getResult()) {
			if (row.getColumns().getIntegerValue("delete", 0) == 1) {
				jo.add(row.getKey());
			}
		}
		return jo;
	}
	
	/**
	 * @return never null if keys is not empty
	 */
	public static LinkedHashMap<String, TaskJobStatus> getStatusForTasksOrJobsByKeys(String... keys) throws ConnectionException {
		if (keys == null) {
			return null;
		}
		if (keys.length == 0) {
			return null;
		}
		ArrayList<String> list = new ArrayList<String>(keys.length);
		for (int pos = 0; pos < keys.length; pos++) {
			list.add(keys[pos]);
		}
		return getStatusForTasksOrJobsByKeys(list);
	}
	
	/**
	 * @return never null if keys is not empty
	 */
	public static LinkedHashMap<String, TaskJobStatus> getStatusForTasksOrJobsByKeys(Collection<String> keys) throws ConnectionException {
		if (keys == null) {
			return null;
		}
		if (keys.size() == 0) {
			return null;
		}
		LinkedHashMap<String, TaskJobStatus> status = new LinkedHashMap<String, TaskJobStatus>(keys.size());
		
		Rows<String, String> rows = keyspace.prepareQuery(CF_TASKQUEUE).getKeySlice(keys).withColumnSlice("status").execute().getResult();
		if (rows.isEmpty()) {
			return status;
		}
		for (Row<String, String> row : rows) {
			if (row.getColumns().isEmpty()) {
				continue;
			}
			status.put(row.getKey(), TaskJobStatus.pullFromDatabase(row.getColumns()));
		}
		
		return status;
	}
	
	/**
	 * @return never null if keys is not empty
	 */
	public static JSONObject getTasksAndJobsByKeys(String... keys) throws ConnectionException, ParseException {
		if (keys == null) {
			return null;
		}
		if (keys.length == 0) {
			return null;
		}
		ArrayList<String> list = new ArrayList<String>(keys.length);
		for (int pos = 0; pos < keys.length; pos++) {
			list.add(keys[pos]);
		}
		return getTasksAndJobsByKeys(list);
	}
	
	/**
	 * @return never null if keys is not empty
	 */
	public static JSONObject getTasksAndJobsByKeys(Collection<String> keys) throws ConnectionException, ParseException {
		if (keys == null) {
			return null;
		}
		if (keys.size() == 0) {
			return null;
		}
		
		JSONObject result = new JSONObject();
		Rows<String, String> rows = keyspace.prepareQuery(CF_TASKQUEUE).getKeySlice(keys).execute().getResult();
		for (Row<String, String> row : rows) {
			if (Task.isEmptyDbEntry(row.getColumns())) {
				continue;
			}
			if (Job.isAJob(row.getColumns())) {
				result.put(row.getKey(), Job.pullJSONFromDatabase(row.getColumns()));
			} else {
				result.put(row.getKey(), Task.pullJSONFromDatabase(row.getColumns()));
			}
		}
		return result;
	}
	
	/**
	 * @param since_date set to 0 for disabled range selection
	 */
	public static JSONObject getTasksAndJobs(long since_date) throws Exception {
		final JSONObject jo = new JSONObject();
		if (since_date == 0) {
			CassandraDb.allRowsReader(CF_TASKQUEUE, new AllRowsFoundRow() {
				public void onFoundRow(Row<String, String> row) throws Exception {
					if (Task.isEmptyDbEntry(row.getColumns())) {
						return;
					}
					if (Job.isAJob(row.getColumns())) {
						jo.put(row.getKey(), Job.pullJSONFromDatabase(row.getColumns()));
					} else {
						jo.put(row.getKey(), Task.pullJSONFromDatabase(row.getColumns()));
					}
				}
			});
		} else {
			IndexQuery<String, String> index_query = keyspace.prepareQuery(CF_TASKQUEUE).searchWithIndex();
			Task.selectAllLastTasksAndJobs(index_query, since_date);
			OperationResult<Rows<String, String>> rows = index_query.execute();
			
			for (Row<String, String> row : rows.getResult()) {
				if (Task.isEmptyDbEntry(row.getColumns())) {
					continue;
				}
				if (Job.isAJob(row.getColumns())) {
					jo.put(row.getKey(), Job.pullJSONFromDatabase(row.getColumns()));
				} else {
					jo.put(row.getKey(), Task.pullJSONFromDatabase(row.getColumns()));
				}
			}
		}
		return jo;
	}
	
	public static JSONObject getProcessingTasks() throws Exception {
		final JSONObject jo = new JSONObject();
		IndexQuery<String, String> index_query = keyspace.prepareQuery(CF_TASKQUEUE).searchWithIndex();
		TaskJobStatus.selectByName(index_query, TaskJobStatus.PROCESSING);
		OperationResult<Rows<String, String>> rows = index_query.execute();
		
		for (Row<String, String> row : rows.getResult()) {
			if (Task.isEmptyDbEntry(row.getColumns())) {
				continue;
			}
			jo.put(row.getKey(), Job.pullJSONFromDatabase(row.getColumns()));
		}
		return jo;
	}
	
	/**
	 * Simple row delete. No tests.
	 */
	public static void deleteTaskJob(String key) throws ConnectionException {
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		mutator.withRow(Broker.CF_TASKQUEUE, key).delete();
		mutator.execute();
	}
	
	void declareProfilesForTriggerWorker(TriggerWorker trigger) {
		queue.addTrigger(trigger);
	}
	
	void removeProfilesForTriggerWorker(TriggerWorker trigger) {
		queue.removeTrigger(trigger);
	}
	
	public static JSONObject getActiveTriggerWorkers() throws Exception {
		final JSONObject jo = new JSONObject();
		CassandraDb.allRowsReader(Broker.CF_QUEUETRIGGER, new AllRowsFoundRow() {
			public void onFoundRow(Row<String, String> row) throws Exception {
				JSONObject profile = new JSONObject();
				JSONParser jsonparser = new JSONParser();
				for (int pos = 0; pos < row.getColumns().size(); pos++) {
					jsonparser.reset();
					profile.put(row.getColumns().getColumnByIndex(pos).getName(), jsonparser.parse(row.getColumns().getColumnByIndex(pos).getStringValue()));
				}
				jo.put(row.getKey(), profile);
			}
		});
		return jo;
	}
	
}
