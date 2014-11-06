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
package controllers;

import hd3gtv.mydmam.db.orm.CrudOrmEngine;
import hd3gtv.mydmam.mail.notification.Notification;
import hd3gtv.mydmam.mail.notification.NotifyReason;
import hd3gtv.mydmam.taskqueue.Broker;
import hd3gtv.mydmam.taskqueue.TaskJobStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import models.UserProfile;

import org.json.simple.JSONObject;

import play.data.validation.Required;
import play.data.validation.Validation;
import play.i18n.Messages;
import play.jobs.JobsPlugin;
import play.mvc.Controller;
import play.mvc.With;
import ext.MydmamExtensions;

@With(Secure.class)
@SuppressWarnings("unchecked")
public class UserNotifications extends Controller {
	
	private static class UpdateNotifications implements Callable<Boolean> {
		public UpdateNotifications() {
		}
		
		public Boolean call() throws Exception {
			Notification.updateTasksJobsEvolutionsForNotifications();
			return true;
		}
	}
	
	public static void notificationslist() throws Exception {
		String title = Messages.all(play.i18n.Lang.get()).getProperty("userprofile.notifications.pagename");
		UserProfile user = User.getUserProfile();
		
		JobsPlugin.executor.submit(new UpdateNotifications());
		
		ArrayList<Map<String, Object>> user_notifications = Notification.getRawFromDatabaseByObserver(user, false);
		render(title, user_notifications, user);
	}
	
	/**
	 * @return valid notification for user, or (flash error + redirect to list) | (or if doredirect: return null)
	 */
	private static Notification getNotification(UserProfile user, String key, boolean doredirect, boolean must_not_closed) throws Exception {
		Notification notification = Notification.getFromDatabase(key);
		
		if (notification == null) {
			if (doredirect == false) {
				return null;
			}
			flash("error", Messages.all(play.i18n.Lang.get()).getProperty("userprofile.notifications.cantfoundselected"));
			redirect("User.notificationslist");
		}
		if (notification.containsObserver(user) == false) {
			if (doredirect == false) {
				return null;
			}
			flash("error", Messages.all(play.i18n.Lang.get()).getProperty("userprofile.notifications.cantfoundvalid"));
			redirect("User.notificationslist");
		}
		if (must_not_closed & notification.isClose()) {
			flash("error", Messages.all(play.i18n.Lang.get()).getProperty("userprofile.notifications.isclosed"));
			redirect("User.notificationslist");
		}
		return notification;
	}
	
	public static void notificationclose(@Required String key) throws Exception {
		if (Validation.hasErrors()) {
			redirect("User.notificationslist");
			return;
		}
		flash("lastkey", key);
		
		UserProfile user = User.getUserProfile();
		getNotification(user, key, true, true).switchCloseStatus(user).save();
		redirect("User.notificationslist");
	}
	
	public static void notificationupdatealert(@Required String key, @Required String reason, @Required Boolean notify) throws Exception {
		if (Validation.hasErrors()) {
			redirect("User.notificationslist");
			return;
		}
		flash("lastkey", key);
		
		UserProfile user = User.getUserProfile();
		NotifyReason n_resaon = NotifyReason.getFromDbRecordName(reason);
		if (n_resaon == null) {
			flash("error", Messages.all(play.i18n.Lang.get()).getProperty("userprofile.notifications.invalidreason"));
			redirect("User.notificationslist");
		}
		
		getNotification(user, key, true, true).updateNotifyReasonForUser(user, n_resaon, notify).save();
		redirect("User.notificationslist");
	}
	
	public static void notificationupdatecomment(@Required String key, String comment) throws Exception {
		if (Validation.hasErrors()) {
			redirect("User.notificationslist");
			return;
		}
		flash("lastkey", key);
		UserProfile user = User.getUserProfile();
		
		getNotification(user, key, true, true).updateComment(user, comment).save();
		redirect("User.notificationslist");
	}
	
	public static void notificationupdateread(@Required String key) throws Exception {
		if (Validation.hasErrors()) {
			error(new NullPointerException("Invalid key"));
		}
		
		UserProfile user = User.getUserProfile();
		getNotification(user, key, false, true).switchReadStatus(user).save();
		JSONObject jo = new JSONObject();
		jo.put("result", true);
		renderJSON(jo.toJSONString());
	}
	
	public static void notificationresolveusers() throws Exception {
		String[] users_crypted_keys = params.getAll("users_crypted_keys[]");
		if (users_crypted_keys == null) {
			renderJSON("{}");
			return;
		}
		if (users_crypted_keys.length == 0) {
			renderJSON("{}");
			return;
		}
		
		String[] keys = new String[users_crypted_keys.length];
		
		for (int pos = 0; pos < users_crypted_keys.length; pos++) {
			keys[pos] = MydmamExtensions.decrypt(users_crypted_keys[pos]);
		}
		
		CrudOrmEngine<UserProfile> user_profile_orm_engine = new CrudOrmEngine<UserProfile>(new UserProfile());
		List<UserProfile> users = user_profile_orm_engine.read(keys);
		if (users == null) {
			renderJSON("{}");
		}
		
		JSONObject jo = new JSONObject();
		UserProfile user;
		for (int pos = 0; pos < users.size(); pos++) {
			user = users.get(pos);
			JSONObject jo_user = new JSONObject();
			jo_user.put("mail", user.email);
			jo_user.put("name", user.longname);
			jo.put(MydmamExtensions.encrypt(user.key), jo_user);
		}
		renderJSON(jo.toJSONString());
	}
	
	@SuppressWarnings("rawtypes")
	@Check("adminUsers")
	public static void notificationsadminlist() throws Exception {
		JobsPlugin.executor.submit(new UpdateNotifications());
		
		String title = Messages.all(play.i18n.Lang.get()).getProperty("userprofile.notifications.admin.pagename");
		ArrayList<Map<String, Object>> user_notifications = Notification.getAdminListFromDatabase();
		
		ArrayList<String> tasks_job_to_resolve = new ArrayList<String>();
		
		String task_job_key;
		ArrayList<Object> linked_tasks;
		HashMap<String, Object> map_linked_tasksjobs;
		for (int pos_un = 0; pos_un < user_notifications.size(); pos_un++) {
			linked_tasks = (ArrayList) user_notifications.get(pos_un).get("linked_tasks");
			for (int pos_lt = 0; pos_lt < linked_tasks.size(); pos_lt++) {
				map_linked_tasksjobs = (HashMap) linked_tasks.get(pos_lt);
				task_job_key = (String) map_linked_tasksjobs.get("taskjobkey");
				if (tasks_job_to_resolve.contains(task_job_key) == false) {
					tasks_job_to_resolve.add(task_job_key);
				}
			}
		}
		
		HashMap<String, TaskJobStatus> linked_tasksjobs = new HashMap<String, TaskJobStatus>(1);
		if (tasks_job_to_resolve.isEmpty() == false) {
			linked_tasksjobs = Broker.getStatusForTasksOrJobsByKeys(tasks_job_to_resolve);
		}
		
		render(title, user_notifications, linked_tasksjobs);
	}
	
	@Check("adminUsers")
	public static void notificationadminclose(@Required String key) throws Exception {
		if (Validation.hasErrors()) {
			redirect("User.notificationsadminlist");
			return;
		}
		UserProfile user = User.getUserProfile();
		Notification notification = Notification.getFromDatabase(key);
		if (notification == null) {
			flash("error", Messages.all(play.i18n.Lang.get()).getProperty("userprofile.notifications.cantfoundselected"));
		} else {
			notification.switchCloseStatus(user).save();
		}
		
		redirect("User.notificationsadminlist");
	}
	
}
