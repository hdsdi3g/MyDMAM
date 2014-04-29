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
import java.util.zip.CRC32;

import models.UserProfile;

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

public class Notification {
	
	public static final String ES_INDEX = "notifications";
	public static final String ES_DEFAULT_TYPE = "global";
	
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
	private List<UserProfile> notify_if_error;// TODO wrapped in a NotifyReason Map
	private List<UserProfile> notify_if_done;// TODO wrapped in a NotifyReason Map
	private List<UserProfile> notify_if_readed;// TODO wrapped in a NotifyReason Map
	private List<UserProfile> notify_if_closed;// TODO wrapped in a NotifyReason Map
	private List<UserProfile> notify_if_commented;// TODO wrapped in a NotifyReason Map
	
	private CrudOrmEngine<UserProfile> user_profile_orm_engine;
	
	private List<UserProfile> getUsersFromDb(JSONArray list_user_profile_record) throws ConnectionException {
		if (list_user_profile_record.size() == 0) {
			return new ArrayList<UserProfile>(1);
		}
		String[] key_list = new String[list_user_profile_record.size()];
		for (int pos = 0; pos < list_user_profile_record.size(); pos++) {
			key_list[pos] = (String) list_user_profile_record.get(pos);
		}
		return user_profile_orm_engine.read(key_list);
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
		notify_if_error = new ArrayList<UserProfile>(1);
		notify_if_done = new ArrayList<UserProfile>(1);
		notify_if_readed = new ArrayList<UserProfile>(1);
		notify_if_closed = new ArrayList<UserProfile>(1);
		notify_if_commented = new ArrayList<UserProfile>(1);
	}
	
	private Notification importFromDb(JSONObject record) throws ConnectionException {
		observers = getUsersFromDb((JSONArray) record.get("observers"));
		creator = user_profile_orm_engine.read((String) record.get("creator"));
		
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
		first_reader = user_profile_orm_engine.read((String) record.get("first_reader"));
		closed_at = (Long) record.get("closed_at");
		is_close = (Boolean) record.get("is_close");
		closed_by = user_profile_orm_engine.read((String) record.get("closed_by"));
		commented_at = (Long) record.get("commented_at");
		users_comment = (String) record.get("users_comment");
		notify_if_error = getUsersFromDb((JSONArray) record.get("notify_if_error"));
		notify_if_done = getUsersFromDb((JSONArray) record.get("notify_if_done"));
		notify_if_readed = getUsersFromDb((JSONArray) record.get("notify_if_readed"));
		notify_if_closed = getUsersFromDb((JSONArray) record.get("notify_if_closed"));
		notify_if_commented = getUsersFromDb((JSONArray) record.get("notify_if_commented"));
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
		record.put("notify_if_error", getUsersToSetInDb(notify_if_error));
		record.put("notify_if_done", getUsersToSetInDb(notify_if_done));
		record.put("notify_if_readed", getUsersToSetInDb(notify_if_readed));
		record.put("notify_if_closed", getUsersToSetInDb(notify_if_closed));
		record.put("notify_if_commented", getUsersToSetInDb(notify_if_commented));
		return this;
	}
	
	private Notification() throws ConnectionException, IOException {
		user_profile_orm_engine = new CrudOrmEngine<UserProfile>(new UserProfile());
	}
	
	/**
	 * Sorted by created_at (recent first)
	 */
	public static List<Notification> getAllFromDatabase(int from, int size) throws ConnectionException, IOException {
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
			notification.importFromDb(Elasticsearch.getJSONFromSimpleResponse(hits[pos]));
			all_notifications.add(notification);
		}
		return all_notifications;
	}
	
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
	
	public Notification updateNotifyErrorForUser(UserProfile user, boolean notify) {
		if (user == null) {
			throw new NullPointerException("\"user\" can't to be null");
		}
		updateUserList(user, notify_if_error, notify);
		return this;
	}
	
	public Notification updateNotifyDoneForUser(UserProfile user, boolean notify) {
		if (user == null) {
			throw new NullPointerException("\"user\" can't to be null");
		}
		updateUserList(user, notify_if_done, notify);
		return this;
	}
	
	public Notification updateNotifyClosedForUser(UserProfile user, boolean notify) {
		if (user == null) {
			throw new NullPointerException("\"user\" can't to be null");
		}
		updateUserList(user, notify_if_closed, notify);
		return this;
	}
	
	public Notification updateNotifyReadedForUser(UserProfile user, boolean notify) {
		if (user == null) {
			throw new NullPointerException("\"user\" can't to be null");
		}
		updateUserList(user, notify_if_readed, notify);
		return this;
	}
	
	public Notification updateNotifyCommentedForUser(UserProfile user, boolean notify) {
		if (user == null) {
			throw new NullPointerException("\"user\" can't to be null");
		}
		updateUserList(user, notify_if_commented, notify);
		return this;
	}
	
	public Notification updateObserversForUser(UserProfile user, boolean observer) {
		if (user == null) {
			throw new NullPointerException("\"user\" can't to be null");
		}
		updateUserList(user, observers, observer);
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
	
	/**
	 * Compare with previous saved element, and create (if needed) db elements for alerting.
	 * @throws Exception
	 */
	public void save() throws Exception {
		/**
		 * Compare & update: prepare updater
		 */
		CrudOrmEngine<CrudOrmModel> orm_engine = CrudOrmEngine.get(NotificationUpdate.class);
		NotificationUpdate nu = (NotificationUpdate) orm_engine.create();
		nu.key = key;
		
		/**
		 * Compare & update: compare tasks/jobs status
		 */
		LinkedHashMap<String, TaskJobStatus> new_status = Broker.getStatusForTasksOrJobsByKeys(linked_tasksjobs.keySet());
		TaskJobStatus new_status_summary = getSummaryTaskJobStatus(new_status);
		TaskJobStatus previous_status_summary = getSummaryTaskJobStatus(linked_tasksjobs);
		
		if (new_status_summary != previous_status_summary) {
			if (new_status_summary == TaskJobStatus.ERROR) {
				nu.is_new_error = true;
			} else if (new_status_summary == TaskJobStatus.DONE) {
				nu.is_new_done = true;
			} else if (new_status_summary == TaskJobStatus.CANCELED) {
				nu.is_new_done = true;
			}
		}
		linked_tasksjobs = new_status;
		
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
		
		/**
		 * Export & save
		 */
		Client client = Elasticsearch.getClient();
		JSONObject record = new JSONObject();
		exportToDb(record);
		
		IndexRequest ir = new IndexRequest(ES_INDEX, ES_DEFAULT_TYPE, key);
		ir.source(record.toJSONString());
		client.index(ir);
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
	 * @return never null
	 */
	public static Map<UserProfile, Map<NotifyReason, List<Notification>>> getUsersNotifyList() throws Exception {
		Map<UserProfile, Map<NotifyReason, List<Notification>>> usersnotifylist = new HashMap<UserProfile, Map<NotifyReason, List<Notification>>>();
		
		CrudOrmEngine<CrudOrmModel> orm_engine = CrudOrmEngine.get(NotificationUpdate.class);
		// TODO add lock
		List<CrudOrmModel> raw_notify_list = orm_engine.list();
		if (raw_notify_list == null) {
			// TODO remove lock
			return usersnotifylist;
		}
		if (raw_notify_list.size() == 0) {
			// TODO remove lock
			return usersnotifylist;
		}
		Map<Notification, List<NotifyReason>> globalnotifylists = new HashMap<Notification, List<NotifyReason>>();
		
		NotificationUpdate nu;
		for (int pos = 0; pos < raw_notify_list.size(); pos++) {
			nu = (NotificationUpdate) raw_notify_list.get(pos);
			Notification notification = getFromDatabase(nu.key);
			List<NotifyReason> reasons = nu.getReasons();
			if (reasons.size() > 0) {
				globalnotifylists.put(notification, reasons);
			}
			orm_engine.delete(nu.key);
		}
		// TODO remove lock
		
		if (globalnotifylists.isEmpty()) {
			return usersnotifylist;
		}
		
		Notification notification;
		List<NotifyReason> reasons_list;
		NotifyReason reason;
		List<UserProfile> user_list;
		UserProfile user;
		Map<NotifyReason, List<Notification>> notify_list_map;
		List<Notification> notification_list;
		
		for (Map.Entry<Notification, List<NotifyReason>> notifylist : globalnotifylists.entrySet()) {
			notification = notifylist.getKey();
			reasons_list = notifylist.getValue();
			for (int pos_rlist = 0; pos_rlist < reasons_list.size(); pos_rlist++) {
				reason = reasons_list.get(pos_rlist);
				if (reason == NotifyReason.ERROR) {
					user_list = notification.notify_if_error;
				} else if (reason == NotifyReason.DONE) {
					user_list = notification.notify_if_done;
				} else if (reason == NotifyReason.READED) {
					user_list = notification.notify_if_readed;
				} else if (reason == NotifyReason.CLOSED) {
					user_list = notification.notify_if_closed;
				} else if (reason == NotifyReason.COMMENTED) {
					user_list = notification.notify_if_commented;
				} else {
					continue;
				}
				
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
	
	public static Notification getFromDatabase(String key) throws ConnectionException, IOException {
		if (key == null) {
			throw new NullPointerException("\"key\" can't to be null");
		}
		Client client = Elasticsearch.getClient();
		GetResponse response = client.get(new GetRequest(ES_INDEX, ES_DEFAULT_TYPE, key)).actionGet();
		if (response.isExists() == false) {
			return null;
		}
		Notification notification = new Notification();
		notification.importFromDb(Elasticsearch.getJSONFromSimpleResponse(response));
		return notification;
	}
	
	public static List<Notification> getFromDatabaseByObserver(UserProfile user) throws ConnectionException, IOException {
		if (user == null) {
			throw new NullPointerException("\"user\" can't to be null");
		}
		Client client = Elasticsearch.getClient();
		SearchRequestBuilder request = client.prepareSearch();
		request.setIndices(ES_INDEX);
		request.setTypes(ES_DEFAULT_TYPE);
		request.addSort("created_at", SortOrder.DESC);
		request.setQuery(QueryBuilders.termQuery("observers", user.key));
		
		/*
		 * QueryBuilders.boolQuery().must()
			query.should(QueryBuilders.termQuery("origin.key", pathelementskeys[pos]));
		 * QueryStringQueryBuilder sqqb = new QueryStringQueryBuilder("\"" + pathfilename + "\"");
		sqqb.defaultField("path");
		request.setQuery(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("storagename", storagename.toLowerCase())).must(sqqb));
		*/
		
		SearchResponse response = request.execute().actionGet();
		if (response.getHits().totalHits() == 0) {
			return new ArrayList<Notification>(1);
		}
		SearchHit[] hits = response.getHits().hits();
		ArrayList<Notification> notifications = new ArrayList<Notification>(hits.length);
		for (int pos = 0; pos < hits.length; pos++) {
			Notification notification = new Notification();
			notifications.add(notification.importFromDb(Elasticsearch.getJSONFromSimpleResponse(hits[pos])));
		}
		return notifications;
	}
}
