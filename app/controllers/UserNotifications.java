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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import com.google.gson.JsonObject;

import ext.MydmamExtensions;
import hd3gtv.mydmam.db.orm.CrudOrmEngine;
import hd3gtv.mydmam.mail.notification.Notification;
import hd3gtv.mydmam.mail.notification.NotifyReason;
import hd3gtv.mydmam.manager.JobNG;
import models.UserProfile;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.i18n.Messages;
import play.jobs.JobsPlugin;
import play.mvc.Controller;
import play.mvc.With;

/**
 * @deprecated
 */
@With(Secure.class)
@SuppressWarnings("unchecked")
public class UserNotifications extends Controller {
	
	private static class UpdateNotifications implements Callable<Boolean> {
		public UpdateNotifications() {
		}
		
		public Boolean call() throws Exception {
			Notification.updateJobsEvolutionsForNotifications();
			return true;
		}
	}
	
	public static void notificationslist() throws Exception {
		String title = Messages.all(play.i18n.Lang.get()).getProperty("userprofile.notifications.pagename");
		flash("pagename", title);
		
		UserProfile user = User.getUserProfile();
		
		JobsPlugin.executor.submit(new UpdateNotifications());
		
		ArrayList<Map<String, Object>> user_notifications = Notification.getRawFromDatabaseByObserver(user, false);
		render(title, user_notifications, user);
	}
	
	public static void associatedjobs() throws Exception {
		String[] job_keys = params.getAll("job_keys[]");
		if (job_keys == null) {
			renderJSON("[\"validation error\"]");
		}
		if (job_keys.length == 0) {
			renderJSON("[\"validation error\"]");
		}
		renderJSON(JobNG.Utility.getJsonJobsByKeys(Arrays.asList(job_keys), true).toString());
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
			redirect("UserNotifications.notificationslist");
		}
		if (notification.containsObserver(user) == false) {
			if (doredirect == false) {
				return null;
			}
			flash("error", Messages.all(play.i18n.Lang.get()).getProperty("userprofile.notifications.cantfoundvalid"));
			redirect("UserNotifications.notificationslist");
		}
		if (must_not_closed & notification.isClose()) {
			flash("error", Messages.all(play.i18n.Lang.get()).getProperty("userprofile.notifications.isclosed"));
			redirect("UserNotifications.notificationslist");
		}
		return notification;
	}
	
	public static void notificationclose(@Required String key) throws Exception {
		if (Validation.hasErrors()) {
			redirect("UserNotifications.notificationslist");
			return;
		}
		flash("lastkey", key);
		
		UserProfile user = User.getUserProfile();
		getNotification(user, key, true, true).switchCloseStatus(user).save();
		redirect("UserNotifications.notificationslist");
	}
	
	public static void notificationupdatealert(@Required String key, @Required String reason, @Required Boolean notify) throws Exception {
		if (Validation.hasErrors()) {
			redirect("UserNotifications.notificationslist");
			return;
		}
		flash("lastkey", key);
		
		UserProfile user = User.getUserProfile();
		NotifyReason n_resaon = NotifyReason.getFromDbRecordName(reason);
		if (n_resaon == null) {
			flash("error", Messages.all(play.i18n.Lang.get()).getProperty("userprofile.notifications.invalidreason"));
			redirect("UserNotifications.notificationslist");
		}
		
		getNotification(user, key, true, true).updateNotifyReasonForUser(user, n_resaon, notify).save();
		redirect("UserNotifications.notificationslist");
	}
	
	public static void notificationupdatecomment(@Required String key, String comment) throws Exception {
		if (Validation.hasErrors()) {
			redirect("UserNotifications.notificationslist");
			return;
		}
		flash("lastkey", key);
		UserProfile user = User.getUserProfile();
		
		getNotification(user, key, true, true).updateComment(user, comment).save();
		redirect("UserNotifications.notificationslist");
	}
	
	public static void notificationupdateread(@Required String key) throws Exception {
		if (Validation.hasErrors()) {
			error(new NullPointerException("Invalid key"));
		}
		
		UserProfile user = User.getUserProfile();
		getNotification(user, key, false, true).switchReadStatus(user).save();
		JsonObject jo = new JsonObject();
		jo.addProperty("result", true);
		renderJSON(jo.toString());
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
		
		JsonObject jo = new JsonObject();
		UserProfile user;
		for (int pos = 0; pos < users.size(); pos++) {
			user = users.get(pos);
			JsonObject jo_user = new JsonObject();
			jo_user.addProperty("mail", user.email);
			jo_user.addProperty("name", user.longname);
			jo.add(MydmamExtensions.encrypt(user.key), jo_user);
		}
		renderJSON(jo.toString());
	}
	
	@SuppressWarnings("rawtypes")
	@Check("adminUsers")
	public static void notificationsadminlist() throws Exception {
		JobsPlugin.executor.submit(new UpdateNotifications());
		
		String title = Messages.all(play.i18n.Lang.get()).getProperty("userprofile.notifications.admin.pagename");
		flash("pagename", title);
		
		ArrayList<Map<String, Object>> user_notifications = Notification.getAdminListFromDatabase();
		
		ArrayList<String> job_keys_to_resolve = new ArrayList<String>();
		
		String job_key;
		ArrayList<Object> current_linked_jobs;
		HashMap<String, Object> map_linked_jobs;
		for (int pos_un = 0; pos_un < user_notifications.size(); pos_un++) {
			current_linked_jobs = (ArrayList) user_notifications.get(pos_un).get("linked_jobs");
			for (int pos_lt = 0; pos_lt < current_linked_jobs.size(); pos_lt++) {
				map_linked_jobs = (HashMap) current_linked_jobs.get(pos_lt);
				job_key = (String) map_linked_jobs.get("jobkey");
				if (job_keys_to_resolve.contains(job_key) == false) {
					job_keys_to_resolve.add(job_key);
				}
			}
		}
		
		HashMap<String, JobNG.JobStatus> linked_jobs = new HashMap<String, JobNG.JobStatus>(1);
		if (job_keys_to_resolve.isEmpty() == false) {
			linked_jobs = JobNG.Utility.getJobsStatusByKeys(job_keys_to_resolve);
		}
		
		render(title, user_notifications, linked_jobs);
	}
	
	@Check("adminUsers")
	public static void notificationadminclose(@Required String key) throws Exception {
		if (Validation.hasErrors()) {
			redirect("UserNotifications.notificationsadminlist");
			return;
		}
		UserProfile user = User.getUserProfile();
		Notification notification = Notification.getFromDatabase(key);
		if (notification == null) {
			flash("error", Messages.all(play.i18n.Lang.get()).getProperty("userprofile.notifications.cantfoundselected"));
		} else {
			notification.switchCloseStatus(user).save();
		}
		
		redirect("UserNotifications.notificationsadminlist");
	}
	
}
