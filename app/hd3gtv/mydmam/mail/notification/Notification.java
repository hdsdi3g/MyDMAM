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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.CRC32;

import javax.mail.internet.InternetAddress;

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.db.ElasticsearchBulkOperation;
import hd3gtv.mydmam.db.ElastisearchCrawlerHit;
import hd3gtv.mydmam.db.ElastisearchCrawlerReader;
import hd3gtv.mydmam.db.orm.CrudOrmEngine;
import hd3gtv.mydmam.db.orm.CrudOrmModel;
import hd3gtv.mydmam.mail.EndUserBaseMail;
import hd3gtv.mydmam.mail.MailPriority;
import hd3gtv.mydmam.manager.JobNG;
import hd3gtv.mydmam.manager.JobNG.JobStatus;
import models.UserProfile;
import play.i18n.Lang;

public class Notification {
	
	public static final String ES_INDEX = "notifications";
	static final String ES_DEFAULT_TYPE = "global";
	private static final long MAXIMAL_NOTIFICATION_LIFETIME = 3600 * 24 * 7 * 2; // 2 weeks
	
	private String key;
	private List<UserProfile> observers;
	private UserProfile creator;
	private Map<String, JobStatus> linked_jobs;
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
	private String creator_reference;
	private List<String> profile_references;
	
	private Map<NotifyReason, List<UserProfile>> notify_list;
	
	private List<UserProfile> getUsersFromDb(JsonArray list_user_profile_record) throws Exception {
		if (list_user_profile_record.size() == 0) {
			return new ArrayList<UserProfile>(1);
		}
		String[] key_list = new String[list_user_profile_record.size()];
		for (int pos = 0; pos < list_user_profile_record.size(); pos++) {
			key_list[pos] = list_user_profile_record.get(pos).getAsString();
		}
		return getUsers(key_list);
	}
	
	private static List<String> getUsersToSetInDb(List<UserProfile> user_list) {
		ArrayList<String> list_user_profile_record = new ArrayList<String>();
		for (int pos = 0; pos < user_list.size(); pos++) {
			list_user_profile_record.add(user_list.get(pos).key);
		}
		return list_user_profile_record;
	}
	
	private void initDefault() {
		key = UUID.randomUUID().toString();
		observers = new ArrayList<UserProfile>(1);
		creator = null;
		linked_jobs = new HashMap<String, JobStatus>(1);
		creating_comment = "";
		creator_reference = "";
		profile_references = new ArrayList<String>();
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
	
	private Notification importFromDb(String key, JsonObject record) throws Exception {
		this.key = key;
		
		observers = getUsersFromDb(record.get("observers").getAsJsonArray());
		creator = getUser(record.get("creator").getAsString());
		
		JsonArray ja_linked_jobs = record.get("linked_jobs").getAsJsonArray();
		if (ja_linked_jobs.size() > 0) {
			linked_jobs = new HashMap<String, JobStatus>(ja_linked_jobs.size());
			for (int pos = 0; pos < ja_linked_jobs.size(); pos++) {
				JsonObject jo = ja_linked_jobs.get(pos).getAsJsonObject();
				linked_jobs.put(jo.get("jobkey").getAsString(), JobStatus.valueOf(jo.get("status").getAsString()));
			}
		} else {
			linked_jobs = new HashMap<String, JobStatus>(1);
		}
		
		creating_comment = record.get("creating_comment").getAsString();
		
		profile_references = new ArrayList<String>();
		JsonArray ja_profile_references = record.get("profile_references").getAsJsonArray();
		for (int pos = 0; pos < ja_profile_references.size(); pos++) {
			profile_references.add(ja_profile_references.get(pos).getAsString());
		}
		
		creator_reference = record.get("creator_reference").getAsString();
		created_at = record.get("created_at").getAsLong();
		is_read = record.get("is_read").getAsBoolean();
		readed_at = record.get("readed_at").getAsLong();
		
		first_reader = getUser(record.get("first_reader").getAsString());
		closed_at = record.get("closed_at").getAsLong();
		is_close = record.get("is_close").getAsBoolean();
		closed_by = getUser(record.get("closed_by").getAsString());
		commented_at = record.get("commented_at").getAsLong();
		users_comment = record.get("users_comment").getAsString();
		
		notify_list = new HashMap<NotifyReason, List<UserProfile>>();
		NotifyReason[] reasons = NotifyReason.values();
		for (int pos = 0; pos < reasons.length; pos++) {
			notify_list.put(reasons[pos], getUsersFromDb(record.get(reasons[pos].getDbRecordName()).getAsJsonArray()));
		}
		return this;
	}
	
	private Notification exportToDb(JsonObject record) {
		Gson gson = new Gson();
		
		record.add("observers", gson.toJsonTree(getUsersToSetInDb(observers)));
		
		if (creator == null) {
			record.addProperty("creator", "");
		} else {
			record.addProperty("creator", creator.key);
		}
		
		JsonArray ja_linked_jobs = new JsonArray();
		for (Map.Entry<String, JobStatus> entry : linked_jobs.entrySet()) {
			JsonObject jo = new JsonObject();
			jo.addProperty("jobkey", entry.getKey());
			jo.addProperty("status", entry.getValue().toString());
			ja_linked_jobs.add(jo);
		}
		record.add("linked_jobs", ja_linked_jobs);
		
		record.addProperty("creating_comment", creating_comment);
		
		if (profile_references != null) {
			record.add("profile_references", gson.toJsonTree(profile_references));
		} else {
			record.add("profile_references", gson.toJsonTree(new ArrayList<String>()));
		}
		
		record.addProperty("creator_reference", creator_reference);
		record.addProperty("created_at", created_at);
		record.addProperty("is_read", is_read);
		record.addProperty("readed_at", readed_at);
		
		if (first_reader == null) {
			record.addProperty("first_reader", "");
		} else {
			record.addProperty("first_reader", first_reader.key);
		}
		
		record.addProperty("closed_at", closed_at);
		record.addProperty("is_close", is_close);
		
		if (closed_by == null) {
			record.addProperty("closed_by", "");
		} else {
			record.addProperty("closed_by", closed_by.key);
		}
		
		record.addProperty("commented_at", commented_at);
		record.addProperty("users_comment", users_comment);
		
		NotifyReason[] reasons = NotifyReason.values();
		if (notify_list == null) {
			notify_list = new HashMap<NotifyReason, List<UserProfile>>();
			for (int pos = 0; pos < reasons.length; pos++) {
				notify_list.put(reasons[pos], new ArrayList<UserProfile>(1));
			}
		}
		for (int pos = 0; pos < reasons.length; pos++) {
			record.add(reasons[pos].getDbRecordName(), gson.toJsonTree(getUsersToSetInDb(notify_list.get(reasons[pos]))));
		}
		return this;
	}
	
	Map<String, Object> exportToMailVars() {
		HashMap<String, Object> mail_vars = new HashMap<String, Object>();
		mail_vars.put("creating_comment", creating_comment);
		mail_vars.put("creator_reference", creator_reference);
		mail_vars.put("profile_references", profile_references);
		mail_vars.put("is_read", is_read);
		mail_vars.put("is_close", is_close);
		mail_vars.put("users_comment", users_comment);
		mail_vars.put("creator", creator.longname);
		return mail_vars;
	}
	
	public static Notification create(UserProfile creator, String creating_comment, String creator_reference) throws ConnectionException, IOException {
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
		notification.creator_reference = creator_reference;
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
	
	public Notification addLinkedJob(JobNG job) throws ConnectionException {
		if (job == null) {
			return this;
		}
		linked_jobs.put(job.getKey(), job.getStatus());
		return this;
	}
	
	public Notification addProfileReference(String... references) {
		if (references != null) {
			for (int pos = 0; pos < references.length; pos++) {
				if (profile_references.contains(references[pos]) == false) {
					profile_references.add(references[pos]);
				}
			}
		}
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
	
	private static JobStatus getSummaryJobStatus(Map<String, JobStatus> status) {
		boolean has_waiting = false;
		boolean has_done = false;
		boolean has_processing = false;
		for (Map.Entry<String, JobStatus> entry : status.entrySet()) {
			if (entry.getValue() == JobStatus.TOO_OLD) {
				return JobStatus.ERROR;// Case 1
			} else if (entry.getValue() == JobStatus.ERROR) {
				return JobStatus.ERROR;// Case 1
			} else if (entry.getValue() == JobStatus.POSTPONED) {
				has_waiting = true;
			} else if (entry.getValue() == JobStatus.WAITING) {
				has_waiting = true;
			} else if (entry.getValue() == JobStatus.DONE) {
				has_done = true;
			} else if (entry.getValue() == JobStatus.CANCELED) {
				has_done = true;
			} else if (entry.getValue() == JobStatus.STOPPED) {
				has_done = true;
			} else if (entry.getValue() == JobStatus.PROCESSING) {
				has_processing = true;
			} else if (entry.getValue() == JobStatus.PREPARING) {
				has_processing = true;
			} else if (entry.getValue() == JobStatus.TOO_LONG_DURATION) {
				return JobStatus.ERROR;// Case 1
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
			return JobStatus.PROCESSING;
		}
		if (has_waiting) {// Case 3
			return JobStatus.WAITING;
		}
		if (has_done) {// Case 4
			return JobStatus.DONE;
		} else {// Case 5
			return JobStatus.CANCELED;
		}
	}
	
	/**
	 * Don't refresh internal status.
	 */
	public JobStatus getActualSummaryJobStatus() throws ConnectionException {
		return getSummaryJobStatus(linked_jobs);
	}
	
	public static int updateJobsEvolutionsForNotifications() throws Exception {
		/**
		 * Get all non-closed notifications
		 */
		ElastisearchCrawlerReader crawler_reader = Elasticsearch.createCrawlerReader();
		crawler_reader.setIndices(ES_INDEX);
		crawler_reader.setTypes(ES_DEFAULT_TYPE);
		crawler_reader.setQuery(QueryBuilders.termQuery("is_close", false));
		crawler_reader.addSort("created_at", SortOrder.ASC);
		
		final ElasticsearchBulkOperation bulk = Elasticsearch.prepareBulk();
		crawler_reader.allReader(new ElastisearchCrawlerHit() {
			
			boolean must_update_notification;
			CrudOrmEngine<CrudOrmModel> orm_engine = CrudOrmEngine.get(NotificationUpdate.class);
			JsonObject record;
			JobStatus new_status_summary;
			JobStatus previous_status_summary;
			
			public boolean onFoundHit(SearchHit hit) {
				try {
					
					/**
					 * For all found notifications
					 */
					Notification notification = new Notification();
					notification.importFromDb(hit.getId(), Elasticsearch.getJSONFromSimpleResponse(hit));
					must_update_notification = false;
					
					if (notification.linked_jobs.isEmpty()) {
						return true;
					}
					
					NotificationUpdate nu = (NotificationUpdate) orm_engine.create();
					nu.key = notification.key;
					
					/**
					 * Compare jobs status and push new notification update if needed
					 */
					LinkedHashMap<String, JobStatus> new_status = JobNG.Utility.getJobsStatusByKeys(notification.linked_jobs.keySet());
					new_status_summary = getSummaryJobStatus(new_status);
					previous_status_summary = getSummaryJobStatus(notification.linked_jobs);
					
					if (new_status_summary != previous_status_summary) {
						if (new_status_summary == JobStatus.ERROR) {
							nu.is_new_error = true;
						} else if (new_status_summary == JobStatus.DONE) {
							nu.is_new_done = true;
						} else if (new_status_summary == JobStatus.CANCELED) {
							nu.is_new_done = true;
						}
						if (nu.isNeedUpdate()) {
							orm_engine.saveInternalElement();
						}
						must_update_notification = true;
					}
					
					/**
					 * Update Jobs status cache in notification
					 */
					for (Map.Entry<String, JobStatus> entry : notification.linked_jobs.entrySet()) {
						String jobkey = entry.getKey();
						JobStatus current_job = entry.getValue();
						if (new_status.containsKey(jobkey) == false) {
							/**
							 * If job is referenced in notification and deleted from Broker.
							 */
							entry.setValue(JobStatus.DONE);
							must_update_notification = true;
						} else if (current_job != new_status.get(jobkey)) {
							entry.setValue(new_status.get(jobkey));
							must_update_notification = true;
						}
					}
					
					if (must_update_notification) {
						record = new JsonObject();
						notification.exportToDb(record);
						bulk.add(bulk.getClient().prepareIndex(ES_INDEX, ES_DEFAULT_TYPE, notification.key).setSource(record.toString()).setRefresh(true).setTTL(MAXIMAL_NOTIFICATION_LIFETIME));
					}
				} catch (Exception e) {
					Loggers.Mail.error("Can't import Notification", e);
					return false;
				}
				return true;
			}
		});
		
		/**
		 * Record all notifications updates
		 */
		bulk.terminateBulk();
		
		/**
		 * Request pending notifications for user
		 */
		Map<UserProfile, Map<NotifyReason, List<Notification>>> users_notify_list = new HashMap<UserProfile, Map<NotifyReason, List<Notification>>>();
		
		CrudOrmEngine<CrudOrmModel> orm_engine = CrudOrmEngine.get(NotificationUpdate.class);
		if (orm_engine.aquireLock(10, TimeUnit.SECONDS) == false) {
			return 0;
		}
		
		List<CrudOrmModel> raw_notify_list = orm_engine.list();
		if (raw_notify_list == null) {
			orm_engine.releaseLock();
			return 0;
		}
		if (raw_notify_list.size() == 0) {
			orm_engine.releaseLock();
			return 0;
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
			return 0;
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
					if (users_notify_list.containsKey(user)) {
						notify_list_map = users_notify_list.get(user);
					} else {
						notify_list_map = new HashMap<NotifyReason, List<Notification>>();
						users_notify_list.put(user, notify_list_map);
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
		
		/**
		 * Send mails
		 */
		int count = 0;
		
		List<Notification> notifications;
		HashMap<String, Object> mail_vars;
		HashMap<Object, Object> reasons_vars;
		
		for (Map.Entry<UserProfile, Map<NotifyReason, List<Notification>>> users_notify_entry : users_notify_list.entrySet()) {
			user = users_notify_entry.getKey();
			EndUserBaseMail usermail = new EndUserBaseMail(Lang.getLocale(user.language), new InternetAddress(user.email), "notification");
			mail_vars = new HashMap<String, Object>();
			reasons_vars = new HashMap<Object, Object>();
			
			for (Map.Entry<NotifyReason, List<Notification>> entry_notifyreason : users_notify_entry.getValue().entrySet()) {
				reason = entry_notifyreason.getKey();
				
				notifications = entry_notifyreason.getValue();
				List<Map<String, Object>> mail_var_notifications = new ArrayList<Map<String, Object>>();
				
				for (int pos_ntf = 0; pos_ntf < notifications.size(); pos_ntf++) {
					mail_var_notifications.add(notifications.get(pos_ntf).exportToMailVars());
					if (notifications.get(pos_ntf).getActualSummaryJobStatus() == JobStatus.ERROR) {
						usermail.setMailPriority(MailPriority.HIGHEST);
					}
				}
				reasons_vars.put(reason.toString().toLowerCase(), mail_var_notifications);
			}
			mail_vars.put("reasons", reasons_vars);
			usermail.send(mail_vars);
			count++;
		}
		return count;
	}
	
	/**
	 * Switch done if terminated and old (in regulary calls) but without Notify
	 * @param grace_period_duration in ms for search windows
	 */
	public static int updateOldsAndNonClosedNotifications(long grace_period_duration) throws Exception {
		final AtomicInteger count = new AtomicInteger(0);
		/**
		 * Get all non-closed notifications
		 */
		ElastisearchCrawlerReader crawler_reader = Elasticsearch.createCrawlerReader();
		
		crawler_reader.setIndices(ES_INDEX);
		crawler_reader.setTypes(ES_DEFAULT_TYPE);
		BoolQueryBuilder query = QueryBuilders.boolQuery();
		query.must(QueryBuilders.termQuery("is_close", false));
		query.must(QueryBuilders.rangeQuery("created_at").from(0l).to(System.currentTimeMillis() - grace_period_duration));
		crawler_reader.setQuery(query);
		crawler_reader.addSort("created_at", SortOrder.ASC);
		
		final ElasticsearchBulkOperation bulk = Elasticsearch.prepareBulk();
		
		crawler_reader.allReader(new ElastisearchCrawlerHit() {
			JsonObject record;
			JobStatus status_summary;
			boolean will_close_notification;
			
			public boolean onFoundHit(SearchHit hit) {
				/**
				 * For all found notifications
				 */
				Notification notification = new Notification();
				try {
					notification.importFromDb(hit.getId(), Elasticsearch.getJSONFromSimpleResponse(hit));
				} catch (Exception e) {
					Loggers.Mail.error("Can't import from ES", e);
					return false;
				}
				will_close_notification = false;
				
				if (notification.linked_jobs.isEmpty() == false) {
					status_summary = getSummaryJobStatus(notification.linked_jobs);
					if (status_summary == JobStatus.ERROR) {
						will_close_notification = true;
					} else if (status_summary == JobStatus.DONE) {
						will_close_notification = true;
					} else if (status_summary == JobStatus.CANCELED) {
						will_close_notification = true;
					}
				} else {
					will_close_notification = true;
				}
				
				if (will_close_notification) {
					notification.closed_at = System.currentTimeMillis();
					notification.is_close = true;
					record = new JsonObject();
					notification.exportToDb(record);
					bulk.add(bulk.getClient().prepareIndex(ES_INDEX, ES_DEFAULT_TYPE, notification.key).setSource(record.toString()).setTTL(MAXIMAL_NOTIFICATION_LIFETIME));
					count.incrementAndGet();
				}
				return true;
			}
		});
		
		bulk.terminateBulk();
		
		return count.get();
	}
	
	/**
	 * @param bulkrequest add element here, but save directly new NotificationUpdate if needed. Don't wait too long time for exec bulkrequest else you will have orphan NotificationUpdates!
	 * @throws Exception
	 */
	public void save(ElasticsearchBulkOperation bulk_op) throws Exception {
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
		JsonObject record = new JsonObject();
		exportToDb(record);
		
		IndexRequest ir = new IndexRequest(ES_INDEX, ES_DEFAULT_TYPE, key);
		ir.source(record.toString());
		ir.ttl(MAXIMAL_NOTIFICATION_LIFETIME);
		
		if (bulk_op == null) {
			ir.refresh(true);
			Elasticsearch.index(ir);
		} else {
			bulk_op.add(ir);
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
	
	public static Notification getFromDatabase(String key) throws Exception {
		if (key == null) {
			throw new NullPointerException("\"key\" can't to be null");
		}
		GetResponse response = Elasticsearch.get(new GetRequest(ES_INDEX, ES_DEFAULT_TYPE, key));
		if (response.isExists() == false) {
			return null;
		}
		Notification notification = new Notification();
		notification.importFromDb(key, Elasticsearch.getJSONFromSimpleResponse(response));
		return notification;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static ArrayList<Map<String, Object>> getRawFromDatabaseByObserver(final UserProfile user, boolean can_is_closed) throws ConnectionException, Exception {
		if (user == null) {
			throw new NullPointerException("\"user\" can't to be null");
		}
		ElastisearchCrawlerReader request = Elasticsearch.createCrawlerReader();
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
			final ArrayList<Map<String, Object>> notifications = new ArrayList<Map<String, Object>>();
			
			request.allReader(new ElastisearchCrawlerHit() {
				
				Map<String, Object> source;
				ArrayList<Object> raw_linked_jobs;
				HashMap<String, JobStatus> linked_jobs;
				HashMap<String, Object> map_linked_jobs;
				Map<String, Boolean> notify_list_for_user;
				NotifyReason[] reasons = NotifyReason.values();
				
				public boolean onFoundHit(SearchHit hit) throws Exception {
					source = hit.getSource();
					raw_linked_jobs = (ArrayList) source.get("linked_jobs");
					linked_jobs = new HashMap<String, JobStatus>();
					for (int pos_lt = 0; pos_lt < raw_linked_jobs.size(); pos_lt++) {
						map_linked_jobs = (HashMap) raw_linked_jobs.get(pos_lt);
						linked_jobs.put((String) map_linked_jobs.get("jobkey"), JobStatus.valueOf((String) map_linked_jobs.get("status")));
					}
					source.put("summary_status", getSummaryJobStatus(linked_jobs));
					source.put("key", hit.getId());
					
					notify_list_for_user = new HashMap<String, Boolean>();
					for (int pos_r = 0; pos_r < reasons.length; pos_r++) {
						ArrayList<Object> user_list_for_reason = (ArrayList) source.get(reasons[pos_r].getDbRecordName());
						notify_list_for_user.put(reasons[pos_r].getDbRecordName(), user_list_for_reason.contains(user.key));
					}
					source.put("notify_list", notify_list_for_user);
					
					notifications.add(source);
					return true;
				}
			});
			
			return notifications;
		} catch (IndexMissingException e) {
			return new ArrayList<Map<String, Object>>(1);
		} catch (SearchPhaseExecutionException e) {
			return new ArrayList<Map<String, Object>>(1);
		}
	}
	
	public static ArrayList<Map<String, Object>> getAdminListFromDatabase() throws ConnectionException, Exception {
		ElastisearchCrawlerReader request = Elasticsearch.createCrawlerReader();
		request.setIndices(ES_INDEX);
		request.setTypes(ES_DEFAULT_TYPE);
		request.addSort("created_at", SortOrder.DESC);
		
		try {
			final ArrayList<Map<String, Object>> notifications = new ArrayList<Map<String, Object>>();
			
			request.allReader(new ElastisearchCrawlerHit() {
				public boolean onFoundHit(SearchHit hit) throws Exception {
					Map<String, Object> source;
					source = hit.getSource();
					source.put("key", hit.getId());
					notifications.add(source);
					return true;
				}
			});
			
			return notifications;
		} catch (IndexMissingException e) {
			return new ArrayList<Map<String, Object>>(1);
		} catch (SearchPhaseExecutionException e) {
			return new ArrayList<Map<String, Object>>(1);
		}
	}
	
}
