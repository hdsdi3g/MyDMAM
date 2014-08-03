/*
 * This file is part of MyDMAM
 * 
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/

package hd3gtv.mydmam.mail.notification;

import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.db.orm.CrudOrmEngine;
import hd3gtv.mydmam.db.orm.CrudOrmModel;
import hd3gtv.mydmam.taskqueue.Broker;
import hd3gtv.mydmam.taskqueue.TaskJobStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

import models.UserProfile;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

@SuppressWarnings("unchecked")
public class Notification {
	
	public static final String ES_INDEX = "notifications";
	public static final String ES_DEFAULT_TYPE = "global";
	private static final long MAXIMAL_NOTIFICATION_LIFETIME = 3600 * 24 * 7 * 2; // 2 weeks
	
	private String key;
	private List<UserProfile> observers;
	private UserProfile creator;
	private Map<String, TaskJobStatus> linked_tasksjobs;
	private String creating_comment;
	private long created_at;
	private boolean is_read;
	private long readed_at;
	private UserProfile first_reader;
	private long closed_at;
	private boolean is_close;
	private UserProfile closed_by;
	private long commented_at;
	private String users_comment;
	
	private Map<NotifyReason, List<UserProfile>> notify_list;
	
	private List<UserProfile> getUsersFromDb(JSONArray list_user_profile_record) throws Exception {
		if (list_user_profile_record.size() == 0) {
			return new ArrayList<UserProfile>(1);
		}
		String[] key_list = new String[list_user_profile_record.size()];
		for (int pos = 0; pos < list_user_profile_record.size(); pos++) {
			key_list[pos] = (String) list_user_profile_record.get(pos);
		}
		return getUsers(key_list);
	}
	
	private static JSONArray getUsersToSetInDb(List<UserProfile> user_list) {
		JSONArray list_user_profile_record = new JSONArray();
		for (int pos = 0; pos < user_list.size(); pos++) {
			list_user_profile_record.add(user_list.get(pos).key);
		}
		return list_user_profile_record;
	}
	
	private void initDefault() {
		key = UUID.randomUUID().toString();
		observers = new ArrayList<UserProfile>(1);
		creator = null;
		linked_tasksjobs = new HashMap<String, TaskJobStatus>(1);
		creating_comment = "";
		created_at = System.currentTimeMillis();
		is_read = false;
		readed_at = -1;
		first_reader = null;
		closed_at = -1;
		is_close = false;
		closed_by = null;
		commented_at = -1;
		users_comment = "";
		
		notify_list = new HashMap<NotifyReason, List<UserProfile>>();
		NotifyReason[] reasons = NotifyReason.values();
		for (int pos = 0; pos < reasons.length; pos++) {
			notify_list.put(reasons[pos], new ArrayList<UserProfile>(1));
		}
	}
	
	private transient Map<String, UserProfile> user_cache;
	private transient List<String> not_found_users;
	private transient CrudOrmEngine<UserProfile> user_profile_orm_engine;
	
	private synchronized List<UserProfile> getUsers(String... user_keys) throws Exception {
		if (user_cache == null) {
			user_cache = new HashMap<String, UserProfile>();
		}
		if (not_found_users == null) {
			not_found_users = new ArrayList<String>();
		}
		if (user_profile_orm_engine == null) {
			user_profile_orm_engine = new CrudOrmEngine<UserProfile>(new UserProfile());
		}
		if (user_keys == null) {
			return new ArrayList<UserProfile>(1);
		}
		if (user_keys.length == 0) {
			return new ArrayList<UserProfile>(1);
		}
		
		List<String> keys_to_resolve = new ArrayList<String>(user_keys.length);
		List<UserProfile> result = new ArrayList<UserProfile>(user_keys.length);
		
		for (int pos_u = 0; pos_u < user_keys.length; pos_u++) {
			String user_key = user_keys[pos_u];
			if (user_cache.containsKey(user_key) == false) {
				if (not_found_users.contains(user_key)) {
					continue;
				}
				keys_to_resolve.add(user_key);
			} else {
				result.add(user_cache.get(user_key));
			}
		}
		
		if (keys_to_resolve.isEmpty() == false) {
			List<UserProfile> new_users = user_profile_orm_engine.read(keys_to_resolve);
			if (new_users != null) {
				for (int pos_u = 0; pos_u < new_users.size(); pos_u++) {
					UserProfile user = new_users.get(pos_u);
					user_cache.put(user.key, user);
					result.add(user);
					keys_to_resolve.remove(user.key);
				}
			}
			if (keys_to_resolve.isEmpty() == false) {
				not_found_users.addAll(keys_to_resolve);
			}
		}
		return result;
	}
	
	private UserProfile getUser(String user_key) throws Exception {
		List<UserProfile> list = getUsers(user_key);
		if (list.isEmpty()) {
			return null;
		} else {
			return list.get(0);
		}
	}
	
	private Notification importFromDb(String key, JSONObject record) throws Exception {
		this.key = key;
		
		observers = getUsersFromDb((JSONArray) record.get("observers"));
		creator = getUser((String) record.get("creator"));
		
		JSONArray ja_linked_tasks = (JSONArray) record.get("linked_tasks");
		if (ja_linked_tasks.size() > 0) {
			linked_tasksjobs = new HashMap<String, TaskJobStatus>(ja_linked_tasks.size());
			for (int pos = 0; pos < ja_linked_tasks.size(); pos++) {
				JSONObject jo = (JSONObject) ja_linked_tasks.get(pos);
				linked_tasksjobs.put((String) jo.get("taskjobkey"), TaskJobStatus.fromString((String) jo.get("status")));
			}
		} else {
			linked_tasksjobs = new HashMap<String, TaskJobStatus>(1);
		}
		
		creating_comment = (String) record.get("creating_comment");
		created_at = (Long) record.get("created_at");
		is_read = (Boolean) record.get("is_read");
		readed_at = (Long) record.get("readed_at");
		
		first_reader = getUser((String) record.get("first_reader"));
		closed_at = (Long) record.get("closed_at");
		is_close = (Boolean) record.get("is_close");
		closed_by = getUser((String) record.get("closed_by"));
		commented_at = (Long) record.get("commented_at");
		users_comment = (String) record.get("users_comment");
		
		notify_list = new HashMap<NotifyReason, List<UserProfile>>();
		NotifyReason[] reasons = NotifyReason.values();
		for (int pos = 0; pos < reasons.length; pos++) {
			notify_list.put(reasons[pos], getUsersFromDb((JSONArray) record.get(reasons[pos].getDbRecordName())));
		}
		return this;
	}
	
	private Notification exportToDb(JSONObject record) {
		record.put("observers", getUsersToSetInDb(observers));
		
		if (creator == null) {
			record.put("creator", "");
		} else {
			record.put("creator", creator.key);
		}
		
		JSONArray ja_linked_tasks = new JSONArray();
		for (Map.Entry<String, TaskJobStatus> entry : linked_tasksjobs.entrySet()) {
			JSONObject jo = new JSONObject();
			jo.put("taskjobkey", entry.getKey());
			jo.put("status", entry.getValue().toString());
			ja_linked_tasks.add(jo);
		}
		record.put("linked_tasks", ja_linked_tasks);
		
		record.put("creating_comment", creating_comment);
		record.put("created_at", created_at);
		record.put("is_read", is_read);
		record.put("readed_at", readed_at);
		
		if (first_reader == null) {
			record.put("first_reader", "");
		} else {
			record.put("first_reader", first_reader.key);
		}
		
		record.put("closed_at", closed_at);
		record.put("is_close", is_close);
		
		if (closed_by == null) {
			record.put("closed_by", "");
		} else {
			record.put("closed_by", closed_by.key);
		}
		
		record.put("commented_at", commented_at);
		record.put("users_comment", users_comment);
		
		NotifyReason[] reasons = NotifyReason.values();
		if (notify_list == null) {
			notify_list = new HashMap<NotifyReason, List<UserProfile>>();
			for (int pos = 0; pos < reasons.length; pos++) {
				notify_list.put(reasons[pos], new ArrayList<UserProfile>(1));
			}
		}
		for (int pos = 0; pos < reasons.length; pos++) {
			record.put(reasons[pos].getDbRecordName(), getUsersToSetInDb(notify_list.get(reasons[pos])));
		}
		return this;
	}
	
	Map<String, Object> exportToMailVars() {
		HashMap<String, Object> mail_vars = new HashMap<String, Object>();
		mail_vars.put("creating_comment", creating_comment);
		mail_vars.put("is_read", is_read);
		mail_vars.put("is_close", is_close);
		mail_vars.put("users_comment", users_comment);
		mail_vars.put("creator", creator.longname);
		return mail_vars;
	}
	
	/**
	 * Sorted by created_at (recent first)
	 * Unused...
	 */
	/*public static List<Notification> getAllFromDatabase(int from, int size) throws Exception {
		if (size < 1) {
			throw new IndexOutOfBoundsException("size must to be up to 0: " + size);
		}
		ArrayList<Notification> all_notifications = new ArrayList<Notification>(size);
		Client client = Elasticsearch.getClient();
		SearchRequestBuilder request = client.prepareSearch();
		request.setIndices(ES_INDEX);
		request.setTypes(ES_DEFAULT_TYPE);
		request.setQuery(QueryBuilders.matchAllQuery());
		request.addSort("created_at", SortOrder.DESC);
		request.setFrom(from);
		request.setSize(size);
		
		SearchHit[] hits = request.execute().actionGet().getHits().hits();
		JSONParser parser = new JSONParser();
		for (int pos = 0; pos < hits.length; pos++) {
			parser.reset();
			Notification notification = new Notification();
			notification.importFromDb(hits[pos].getId(), Elasticsearch.getJSONFromSimpleResponse(hits[pos]));
			all_notifications.add(notification);
		}
		return all_notifications;
	}*/
	
	public static Notification create(UserProfile creator, String creating_comment) throws ConnectionException, IOException {
		if (creator == null) {
			throw new NullPointerException("\"creator\" can't to be null");
		}
		if (creating_comment == null) {
			throw new NullPointerException("\"creating_comment\" can't to be null");
		}
		Notification notification = new Notification();
		notification.initDefault();
		notification.observers.add(creator);
		notification.creator = creator;
		notification.creating_comment = creating_comment;
		notification.created_at = System.currentTimeMillis();
		return notification;
	}
	
	public boolean containsObserver(UserProfile candidate) {
		if (candidate == null) {
			return false;
		}
		return observers.contains(candidate);
	}
	
	public boolean isClose() {
		return is_close;
	}
	
	public Notification addLinkedTasksJobs(String... taskjobkey) throws ConnectionException {
		LinkedHashMap<String, TaskJobStatus> all_actual_status = Broker.getStatusForTasksOrJobsByKeys(taskjobkey);
		if (all_actual_status == null) {
			return this;
		}
		if (all_actual_status.size() == 0) {
			return this;
		}
		linked_tasksjobs.putAll(all_actual_status);
		return this;
	}
	
	/**
	 * @param add if false: remove
	 */
	private static void updateUserList(UserProfile user, List<UserProfile> user_list, boolean add) {
		int present_pos = -1;
		for (int pos = 0; pos < user_list.size(); pos++) {
			if (user.key.equals(user_list.get(pos).key)) {
				present_pos = pos;
				break;
			}
		}
		if (((present_pos > -1) & add) | (((present_pos > -1) == false) & (add == false))) {
			/**
			 * Present AND want add
			 * OR
			 * Not present AND want remove
			 */
			return;
		}
		if (add) {
			user_list.add(user);
		} else {
			user_list.remove(present_pos);
		}
	}
	
	public Notification updateNotifyReasonForUser(UserProfile user, NotifyReason reason, boolean notify) {
		if (user == null) {
			throw new NullPointerException("\"user\" can't to be null");
		}
		if (reason == null) {
			throw new NullPointerException("\"reason\" can't to be null");
		}
		if (notify_list == null) {
			NotifyReason[] reasons = NotifyReason.values();
			notify_list = new HashMap<NotifyReason, List<UserProfile>>();
			for (int pos = 0; pos < reasons.length; pos++) {
				notify_list.put(reasons[pos], new ArrayList<UserProfile>(1));
			}
		}
		updateUserList(user, notify_list.get(reason), notify);
		return this;
	}
	
	public Notification switchReadStatus(UserProfile user) {
		if (user == null) {
			throw new NullPointerException("\"user\" can't to be null");
		}
		readed_at = System.currentTimeMillis();
		first_reader = user;
		is_read = true;
		return this;
	}
	
	public Notification switchCloseStatus(UserProfile user) {
		if (user == null) {
			throw new NullPointerException("\"user\" can't to be null");
		}
		closed_at = System.currentTimeMillis();
		closed_by = user;
		is_close = true;
		return this;
	}
	
	public Notification updateComment(UserProfile user, String comment) {
		if (user == null) {
			throw new NullPointerException("\"user\" can't to be null");
		}
		if (comment == null) {
			throw new NullPointerException("\"comment\" can't to be null");
		}
		commented_at = System.currentTimeMillis();
		users_comment = comment.trim();
		return this;
	}
	
	private static TaskJobStatus getSummaryTaskJobStatus(Map<String, TaskJobStatus> status) {
		boolean has_waiting = false;
		boolean has_done = false;
		boolean has_processing = false;
		for (Map.Entry<String, TaskJobStatus> entry : status.entrySet()) {
			if (entry.getValue() == TaskJobStatus.TOO_OLD) {
				return TaskJobStatus.ERROR;// Case 1
			} else if (entry.getValue() == TaskJobStatus.ERROR) {
				return TaskJobStatus.ERROR;// Case 1
			} else if (entry.getValue() == TaskJobStatus.POSTPONED) {
				has_waiting = true;
			} else if (entry.getValue() == TaskJobStatus.WAITING) {
				has_waiting = true;
			} else if (entry.getValue() == TaskJobStatus.DONE) {
				has_done = true;
			} else if (entry.getValue() == TaskJobStatus.CANCELED) {
				has_done = true;
			} else if (entry.getValue() == TaskJobStatus.STOPPED) {
				has_done = true;
			} else if (entry.getValue() == TaskJobStatus.PROCESSING) {
				has_processing = true;
			} else if (entry.getValue() == TaskJobStatus.PREPARING) {
				has_processing = true;
			}
		}
		/*
		problems	waiting	done	processing	Return
		0			0		0		0			canceled: Case 5
		0			0		1		0			done: Case 4
		0			1		0		0			waiting: Case 3
		0			1		1		0			waiting: Case 3
		0			0		0		1			processing: Case 2
		0			0		1		1			processing: Case 2
		0			1		0		1			processing: Case 2
		0			1		1		1			processing: Case 2
		1			0		0		0			problem: Case 1
		1			0		1		0			problem: Case 1
		1			1		0		0			problem: Case 1
		1			1		1		0			problem: Case 1
		1			0		0		1			problem: Case 1
		1			0		1		1			problem: Case 1
		1			1		0		1			problem: Case 1
		1			1		1		1			problem: Case 1
		*/
		if (has_processing) {// Case 2
			return TaskJobStatus.PROCESSING;
		}
		if (has_waiting) {// Case 3
			return TaskJobStatus.WAITING;
		}
		if (has_done) {// Case 4
			return TaskJobStatus.DONE;
		} else {// Case 5
			return TaskJobStatus.CANCELED;
		}
	}
	
	/**
	 * Don't refresh internal status.
	 */
	public TaskJobStatus getActualSummaryTaskJobStatus() throws ConnectionException {
		return getSummaryTaskJobStatus(linked_tasksjobs);
	}
	
	public static void updateTasksJobsEvolutionsForNotifications() throws Exception {
		Client client = Elasticsearch.getClient();
		
		/**
		 * Get all non-closed notifications
		 */
		SearchRequestBuilder request = client.prepareSearch();
		request.setIndices(ES_INDEX);
		request.setTypes(ES_DEFAULT_TYPE);
		request.setQuery(QueryBuilders.termQuery("is_close", false));
		request.addSort("created_at", SortOrder.ASC);
		request.setSize(20);
		
		SearchHit[] hits = request.execute().actionGet().getHits().hits();
		
		if (hits.length == 0) {
			return;
		}
		
		BulkRequestBuilder bulkrequest = client.prepareBulk();
		
		CrudOrmEngine<CrudOrmModel> orm_engine = CrudOrmEngine.get(NotificationUpdate.class);
		JSONObject record;
		TaskJobStatus new_status_summary;
		TaskJobStatus previous_status_summary;
		boolean must_update_notification;
		
		JSONParser parser = new JSONParser();
		for (int pos = 0; pos < hits.length; pos++) {
			/**
			 * For all found notifications
			 */
			parser.reset();
			Notification notification = new Notification();
			notification.importFromDb(hits[pos].getId(), Elasticsearch.getJSONFromSimpleResponse(hits[pos]));
			must_update_notification = false;
			
			if (notification.linked_tasksjobs.isEmpty()) {
				continue;
			}
			
			NotificationUpdate nu = (NotificationUpdate) orm_engine.create();
			nu.key = notification.key;
			
			/**
			 * Compare tasks/jobs status and push new notification update if needed
			 */
			LinkedHashMap<String, TaskJobStatus> new_status = Broker.getStatusForTasksOrJobsByKeys(notification.linked_tasksjobs.keySet());
			new_status_summary = getSummaryTaskJobStatus(new_status);
			previous_status_summary = getSummaryTaskJobStatus(notification.linked_tasksjobs);
			
			if (new_status_summary != previous_status_summary) {
				if (new_status_summary == TaskJobStatus.ERROR) {
					nu.is_new_error = true;
				} else if (new_status_summary == TaskJobStatus.DONE) {
					nu.is_new_done = true;
				} else if (new_status_summary == TaskJobStatus.CANCELED) {
					nu.is_new_done = true;
				}
				if (nu.isNeedUpdate()) {
					orm_engine.saveInternalElement();
				}
				must_update_notification = true;
			}
			
			/**
			 * Update Tasks and Jobs status cache in notification
			 */
			for (Map.Entry<String, TaskJobStatus> entry : notification.linked_tasksjobs.entrySet()) {
				String taskjobkey = entry.getKey();
				TaskJobStatus current_taskjob = entry.getValue();
				if (new_status.containsKey(taskjobkey) == false) {
					/**
					 * If task/job is referenced in notification and deleted from Broker.
					 */
					entry.setValue(TaskJobStatus.DONE);
					must_update_notification = true;
				} else if (current_taskjob != new_status.get(taskjobkey)) {
					entry.setValue(new_status.get(taskjobkey));
					must_update_notification = true;
				}
			}
			
			if (must_update_notification) {
				record = new JSONObject();
				notification.exportToDb(record);
				bulkrequest.add(client.prepareIndex(ES_INDEX, ES_DEFAULT_TYPE, notification.key).setSource(record.toJSONString()).setTTL(MAXIMAL_NOTIFICATION_LIFETIME));
			}
		}
		
		/**
		 * Record all notifications updates
		 */
		if (bulkrequest.numberOfActions() > 0) {
			bulkrequest.execute().actionGet();
		}
		
	}
	
	/**
	 * Switch done if terminated and old (in regulary calls) but without Notify
	 * @param grace_period_duration in ms for search windows
	 */
	public static int updateOldsAndNonClosedNotifications(long grace_period_duration) throws Exception {
		Client client = Elasticsearch.getClient();
		int count = 0;
		/**
		 * Get all non-closed notifications
		 */
		SearchRequestBuilder request = client.prepareSearch();
		request.setIndices(ES_INDEX);
		request.setTypes(ES_DEFAULT_TYPE);
		
		BoolQueryBuilder query = QueryBuilders.boolQuery();
		query.must(QueryBuilders.termQuery("is_close", false));
		query.must(QueryBuilders.rangeQuery("created_at").from(0l).to(System.currentTimeMillis() - grace_period_duration));
		request.setQuery(query);
		request.addSort("created_at", SortOrder.ASC);
		request.setSize(20);
		
		SearchHit[] hits = request.execute().actionGet().getHits().hits();
		
		if (hits.length == 0) {
			return 0;
		}
		
		BulkRequestBuilder bulkrequest = client.prepareBulk();
		
		JSONObject record;
		TaskJobStatus status_summary;
		boolean will_close_notification;
		
		JSONParser parser = new JSONParser();
		for (int pos = 0; pos < hits.length; pos++) {
			/**
			 * For all found notifications
			 */
			parser.reset();
			Notification notification = new Notification();
			notification.importFromDb(hits[pos].getId(), Elasticsearch.getJSONFromSimpleResponse(hits[pos]));
			will_close_notification = false;
			
			if (notification.linked_tasksjobs.isEmpty() == false) {
				status_summary = getSummaryTaskJobStatus(notification.linked_tasksjobs);
				if (status_summary == TaskJobStatus.ERROR) {
					will_close_notification = true;
				} else if (status_summary == TaskJobStatus.DONE) {
					will_close_notification = true;
				} else if (status_summary == TaskJobStatus.CANCELED) {
					will_close_notification = true;
				}
			} else {
				will_close_notification = true;
			}
			
			if (will_close_notification) {
				notification.closed_at = System.currentTimeMillis();
				notification.is_close = true;
				record = new JSONObject();
				notification.exportToDb(record);
				bulkrequest.add(client.prepareIndex(ES_INDEX, ES_DEFAULT_TYPE, notification.key).setSource(record.toJSONString()).setTTL(MAXIMAL_NOTIFICATION_LIFETIME));
				count++;
			}
		}
		
		/**
		 * Record all notifications updates
		 */
		if (bulkrequest.numberOfActions() > 0) {
			bulkrequest.execute().actionGet();
		}
		return count;
	}
	
	/**
	 * @param bulkrequest add element here, but save directly new NotificationUpdate if needed. Don't wait too long time for exec bulkrequest else you will have orphan NotificationUpdates!
	 * @throws Exception
	 */
	public void save(BulkRequest bulkrequest) throws Exception {
		if (is_read | is_close | (users_comment.equals("") == false)) {
			/**
			 * Compare & update: prepare updater
			 */
			CrudOrmEngine<CrudOrmModel> orm_engine = CrudOrmEngine.get(NotificationUpdate.class);
			NotificationUpdate nu = (NotificationUpdate) orm_engine.create();
			nu.key = key;
			
			/**
			 * Compare & update: set read ? set closed ? update commented ?
			 */
			Notification previous = getFromDatabase(key);
			if (previous != null) {
				nu.is_new_readed = (is_read & (previous.is_read == false));
				nu.is_new_closed = (is_close & (previous.is_close == false));
				nu.is_new_commented = (users_comment.equals(previous.users_comment)) == false;
			}
			
			if (nu.isNeedUpdate()) {
				orm_engine.saveInternalElement();
			}
		}
		
		/**
		 * Export
		 */
		JSONObject record = new JSONObject();
		exportToDb(record);
		
		IndexRequest ir = new IndexRequest(ES_INDEX, ES_DEFAULT_TYPE, key);
		ir.source(record.toJSONString());
		ir.ttl(MAXIMAL_NOTIFICATION_LIFETIME);
		
		if (bulkrequest == null) {
			Client client = Elasticsearch.getClient();
			ir.refresh(true);
			client.index(ir);
		} else {
			bulkrequest.add(ir);
		}
	}
	
	/**
	 * Compare with previous saved element, and create (if needed) db elements for alerting. ES atomic writing.
	 * @throws Exception
	 */
	public void save() throws Exception {
		save(null);
	}
	
	/**
	 * Key based
	 */
	public int hashCode() {
		if (key != null) {
			CRC32 crc = new CRC32();
			crc.update(key.getBytes());
			return (int) crc.getValue();
		} else {
			return super.hashCode();
		}
	}
	
	/**
	 * Don't update notifications, juste return current Notify bulks.
	 * @return never null
	 */
	public static Map<UserProfile, Map<NotifyReason, List<Notification>>> getUsersNotifyList() throws Exception {
		Map<UserProfile, Map<NotifyReason, List<Notification>>> usersnotifylist = new HashMap<UserProfile, Map<NotifyReason, List<Notification>>>();
		
		CrudOrmEngine<CrudOrmModel> orm_engine = CrudOrmEngine.get(NotificationUpdate.class);
		if (orm_engine.aquireLock(10, TimeUnit.SECONDS) == false) {
			return usersnotifylist;
		}
		
		List<CrudOrmModel> raw_notify_list = orm_engine.list();
		if (raw_notify_list == null) {
			orm_engine.releaseLock();
			return usersnotifylist;
		}
		if (raw_notify_list.size() == 0) {
			orm_engine.releaseLock();
			return usersnotifylist;
		}
		Map<Notification, List<NotifyReason>> globalnotifylists = new HashMap<Notification, List<NotifyReason>>();
		
		NotificationUpdate nu;
		for (int pos = 0; pos < raw_notify_list.size(); pos++) {
			nu = (NotificationUpdate) raw_notify_list.get(pos);
			Notification notification = getFromDatabase(nu.key);
			if (notification == null) {
				/**
				 * Can't found notification
				 */
				continue;
			}
			List<NotifyReason> reasons = nu.getReasons();
			if (reasons.size() > 0) {
				globalnotifylists.put(notification, reasons);
			}
			orm_engine.delete(nu.key);
		}
		
		orm_engine.releaseLock();
		
		if (globalnotifylists.isEmpty()) {
			return usersnotifylist;
		}
		
		Notification notification;
		List<NotifyReason> reasons;
		NotifyReason reason;
		List<UserProfile> user_list;
		UserProfile user;
		Map<NotifyReason, List<Notification>> notify_list_map;
		List<Notification> notification_list;
		
		for (Map.Entry<Notification, List<NotifyReason>> notifylist : globalnotifylists.entrySet()) {
			notification = notifylist.getKey();
			reasons = notifylist.getValue();
			for (int pos_rlist = 0; pos_rlist < reasons.size(); pos_rlist++) {
				reason = reasons.get(pos_rlist);
				user_list = notification.notify_list.get(reason);
				
				for (int pos_userp = 0; pos_userp < user_list.size(); pos_userp++) {
					user = user_list.get(pos_userp);
					if (usersnotifylist.containsKey(user)) {
						notify_list_map = usersnotifylist.get(user);
					} else {
						notify_list_map = new HashMap<NotifyReason, List<Notification>>();
						usersnotifylist.put(user, notify_list_map);
					}
					if (notify_list_map.containsKey(reason)) {
						notification_list = notify_list_map.get(reason);
						if (notification_list.contains(notification) == false) {
							notification_list.add(notification);
						}
					} else {
						notification_list = new ArrayList<Notification>();
						notification_list.add(notification);
						notify_list_map.put(reason, notification_list);
					}
				}
			}
		}
		return usersnotifylist;
	}
	
	public static Notification getFromDatabase(String key) throws Exception {
		if (key == null) {
			throw new NullPointerException("\"key\" can't to be null");
		}
		Client client = Elasticsearch.getClient();
		GetResponse response = client.get(new GetRequest(ES_INDEX, ES_DEFAULT_TYPE, key)).actionGet();
		if (response.isExists() == false) {
			return null;
		}
		Notification notification = new Notification();
		notification.importFromDb(key, Elasticsearch.getJSONFromSimpleResponse(response));
		return notification;
	}
	
	@SuppressWarnings("rawtypes")
	public static ArrayList<Map<String, Object>> getRawFromDatabaseByObserver(UserProfile user, boolean can_is_closed) throws ConnectionException, IOException {
		if (user == null) {
			throw new NullPointerException("\"user\" can't to be null");
		}
		Client client = Elasticsearch.getClient();
		SearchRequestBuilder request = client.prepareSearch();
		request.setIndices(ES_INDEX);
		request.setTypes(ES_DEFAULT_TYPE);
		request.addSort("created_at", SortOrder.DESC);
		
		QueryBuilder select_key = QueryBuilders.matchPhraseQuery("observers", user.key);
		if (can_is_closed) {
			request.setQuery(select_key);
		} else {
			BoolQueryBuilder query = QueryBuilders.boolQuery();
			query.must(select_key);
			query.must(QueryBuilders.termQuery("is_close", false));
			request.setQuery(query);
		}
		
		try {
			SearchResponse response = request.execute().actionGet();
			if (response.getHits().totalHits() == 0) {
				return new ArrayList<Map<String, Object>>(1);
			}
			SearchHit[] hits = response.getHits().hits();
			
			ArrayList<Map<String, Object>> notifications = new ArrayList<Map<String, Object>>(hits.length);
			Map<String, Object> source;
			ArrayList<Object> linked_tasks;
			HashMap<String, TaskJobStatus> linked_tasksjobs;
			HashMap<String, Object> map_linked_tasksjobs;
			Map<String, Boolean> notify_list_for_user;
			NotifyReason[] reasons = NotifyReason.values();
			
			for (int pos = 0; pos < hits.length; pos++) {
				source = hits[pos].getSource();
				
				linked_tasks = (ArrayList) source.get("linked_tasks");
				linked_tasksjobs = new HashMap<String, TaskJobStatus>();
				for (int pos_lt = 0; pos_lt < linked_tasks.size(); pos_lt++) {
					map_linked_tasksjobs = (HashMap) linked_tasks.get(pos_lt);
					linked_tasksjobs.put((String) map_linked_tasksjobs.get("taskjobkey"), TaskJobStatus.fromString((String) map_linked_tasksjobs.get("status")));
				}
				source.put("summary_status", getSummaryTaskJobStatus(linked_tasksjobs));
				source.put("key", hits[pos].getId());
				
				notify_list_for_user = new HashMap<String, Boolean>();
				for (int pos_r = 0; pos_r < reasons.length; pos_r++) {
					ArrayList<Object> user_list_for_reason = (ArrayList) source.get(reasons[pos_r].getDbRecordName());
					notify_list_for_user.put(reasons[pos_r].getDbRecordName(), user_list_for_reason.contains(user.key));
				}
				source.put("notify_list", notify_list_for_user);
				
				notifications.add(source);
			}
			return notifications;
		} catch (IndexMissingException e) {
			return new ArrayList<Map<String, Object>>(0);
		}
	}
	
	public static ArrayList<Map<String, Object>> getAdminListFromDatabase() throws ConnectionException, IOException {
		Client client = Elasticsearch.getClient();
		SearchRequestBuilder request = client.prepareSearch();
		request.setIndices(ES_INDEX);
		request.setTypes(ES_DEFAULT_TYPE);
		request.addSort("created_at", SortOrder.DESC);
		request.setSize(50);
		request.setQuery(QueryBuilders.matchAllQuery());
		
		try {
			SearchResponse response = request.execute().actionGet();
			if (response.getHits().totalHits() == 0) {
				return new ArrayList<Map<String, Object>>(1);
			}
			SearchHit[] hits = response.getHits().hits();
			
			ArrayList<Map<String, Object>> notifications = new ArrayList<Map<String, Object>>(hits.length);
			Map<String, Object> source;
			
			for (int pos = 0; pos < hits.length; pos++) {
				source = hits[pos].getSource();
				source.put("key", hits[pos].getId());
				notifications.add(source);
			}
			return notifications;
		} catch (IndexMissingException e) {
			return new ArrayList<Map<String, Object>>(1);
		}
	}
	
}
