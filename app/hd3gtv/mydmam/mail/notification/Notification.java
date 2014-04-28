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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import models.UserProfile;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
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
	
	// TODO add indicator for pending notify actions ?
	
	private String key;
	private List<UserProfile> observers;
	private UserProfile creator;
	private List<String> linked_tasks_keys;
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
	private List<UserProfile> notify_if_error;
	private List<UserProfile> notify_if_done;
	private List<UserProfile> notify_if_readed;
	private List<UserProfile> notify_if_closed;
	private List<UserProfile> notify_if_commented;
	
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
	
	private static List<String> convertToListString(JSONArray list) {
		List<String> result = new ArrayList<String>();
		for (int pos = 0; pos < list.size(); pos++) {
			result.add((String) list.get(pos));
		}
		return result;
	}
	
	private static JSONArray convertToJSONArray(List<String> list) {
		JSONArray result = new JSONArray();
		for (int pos = 0; pos < list.size(); pos++) {
			result.add((String) list.get(pos));
		}
		return result;
	}
	
	public void initDefault() {
		key = UUID.randomUUID().toString();
		observers = new ArrayList<UserProfile>(1);
		creator = null;
		linked_tasks_keys = new ArrayList<String>(1);
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
	
	public void importFromDb(JSONObject record) throws ConnectionException {
		observers = getUsersFromDb((JSONArray) record.get("observers"));
		creator = user_profile_orm_engine.read((String) record.get("creator"));
		linked_tasks_keys = convertToListString((JSONArray) record.get("linked_tasks_keys"));
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
	}
	
	public void exportToDb(JSONObject record) {
		record.put("observers", getUsersToSetInDb(observers));
		
		if (creator == null) {
			record.put("creator", "");
		} else {
			record.put("creator", creator.key);
		}
		
		record.put("linked_tasks_keys", convertToJSONArray(linked_tasks_keys));
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
	}
	
	private Notification() throws ConnectionException, IOException {
		user_profile_orm_engine = new CrudOrmEngine<UserProfile>(new UserProfile());
	}
	
	/**
	 * Sorted by created_at (recent first)
	 */
	public static List<Notification> getAllFromDatabase(int from, int size) throws ConnectionException, IOException {
		ArrayList<Notification> all_notifications = new ArrayList<Notification>(size);
		Client client = Elasticsearch.getClient();
		SearchRequestBuilder request = client.prepareSearch();
		request.setIndices(ES_INDEX);
		request.setTypes(ES_DEFAULT_TYPE);
		
		/*BoolQueryBuilder query = QueryBuilders.boolQuery();
		for (int pos = 0; pos < pathelementskeys.length; pos++) {
			query.should(QueryBuilders.termQuery("origin.key", pathelementskeys[pos]));
		}*/
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
	
	public static Notification create(UserProfile creator, List<String> linked_tasks_keys, String creating_comment) throws ConnectionException, IOException {
		Notification notification = new Notification();
		notification.observers.add(creator);
		notification.creator = creator;
		notification.linked_tasks_keys = linked_tasks_keys;
		notification.creating_comment = creating_comment;
		notification.created_at = System.currentTimeMillis();
		return notification;
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
		updateUserList(user, notify_if_error, notify);
		return this;
	}
	
	public Notification updateNotifyDoneForUser(UserProfile user, boolean notify) {
		updateUserList(user, notify_if_done, notify);
		return this;
	}
	
	public Notification updateNotifyClosedForUser(UserProfile user, boolean notify) {
		updateUserList(user, notify_if_closed, notify);
		return this;
	}
	
	public Notification updateNotifyReadedForUser(UserProfile user, boolean notify) {
		updateUserList(user, notify_if_readed, notify);
		return this;
	}
	
	public Notification updateNotifyCommentedForUser(UserProfile user, boolean notify) {
		updateUserList(user, notify_if_commented, notify);
		return this;
	}
	
	public Notification updateObserversForUser(UserProfile user, boolean observer) {
		updateUserList(user, observers, observer);
		return this;
	}
	
	public Notification switchReadStatus(UserProfile user) {
		readed_at = System.currentTimeMillis();
		first_reader = user;
		is_read = true;
		return this;
	}
	
	public Notification switchCloseStatus(UserProfile user) {
		closed_at = System.currentTimeMillis();
		closed_by = user;
		is_close = true;
		return this;
	}
	
	public Notification updateComment(UserProfile user, String comment) {
		if (comment == null) {
			return this;
		}
		commented_at = System.currentTimeMillis();
		users_comment = comment.trim();
		return this;
	}
	
	// TODO search linked_tasks_keys global status ?
	// private List<String> linked_tasks_keys;
	
	public void save() {
		Client client = Elasticsearch.getClient();
		JSONObject record = new JSONObject();
		exportToDb(record);
		
		IndexRequest ir = new IndexRequest(ES_INDEX, ES_DEFAULT_TYPE, key);
		ir.source(record.toJSONString());
		client.index(ir);
	}
	
	public static Notification readFromDatabase() throws ConnectionException, IOException {
		Notification notification = new Notification();
		notification.initDefault();
		// TODO
		return notification;
	}
	
}
