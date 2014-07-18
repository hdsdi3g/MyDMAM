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
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
*/

package controllers;

import hd3gtv.mydmam.db.orm.CrudOrmEngine;
import hd3gtv.mydmam.db.orm.CrudOrmModel;
import hd3gtv.mydmam.db.orm.ModelClassResolver;
import hd3gtv.mydmam.db.orm.ORMFormField;
import hd3gtv.mydmam.db.orm.annotations.PublishedMethod;
import hd3gtv.mydmam.mail.notification.Notification;
import hd3gtv.mydmam.mail.notification.NotifyReason;
import hd3gtv.mydmam.operation.Basket;
import hd3gtv.mydmam.taskqueue.Broker;
import hd3gtv.mydmam.taskqueue.TaskJobStatus;
import hd3gtv.mydmam.web.CurrentUserBasket;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import models.UserProfile;

import org.json.simple.JSONObject;

import play.data.binding.Binder;
import play.data.binding.ParamNode;
import play.data.binding.ParamNode.RemovedNode;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.i18n.Messages;
import play.jobs.JobsPlugin;
import play.mvc.Controller;
import play.mvc.With;

import com.google.gson.Gson;

import ext.MydmamExtensions;

@With(Secure.class)
public class User extends Controller {
	
	private static class UserModelClassResolver implements ModelClassResolver {
		public Class<? extends CrudOrmModel> loadModelClass(String name) throws ClassNotFoundException {
			if (name == null) {
				return null;
			}
			if (name.equalsIgnoreCase("userprofile")) {
				return UserProfile.class;
			} else {
				throw new ClassNotFoundException(name);
			}
		}
	}
	
	private static UserModelClassResolver usermodelclassresolver = new UserModelClassResolver();
	
	public static void index() throws Exception {
		String title = Messages.all(play.i18n.Lang.get()).getProperty("userprofile.usereditor.pagename");
		
		String username = Secure.connected();
		String key = UserProfile.prepareKey(username);
		
		Class<? extends CrudOrmModel> entityclass = UserProfile.class;
		
		String type = entityclass.getSimpleName().toLowerCase();
		
		UserProfile userprofile = new UserProfile();
		CrudOrmEngine<UserProfile> engine = new CrudOrmEngine<UserProfile>(userprofile);
		
		if (engine.exists(key)) {
			userprofile = engine.read(key);
		} else {
			userprofile = engine.create();
			userprofile.key = key;
			engine.saveInternalElement();
		}
		
		List<ORMFormField> fields = ORMFormField.removeFields(ORMFormField.getFields(entityclass), "createdate", "updatedate");
		
		HashMap<String, HashMap<String, String>> fieldspointers = ORMFormField.getFieldPointerTable(usermodelclassresolver, fields);
		
		Object object = userprofile;
		
		render(title, type, fields, entityclass, object, fieldspointers);
	}
	
	public static void update() throws Exception {
		String username = Secure.connected();
		String key = UserProfile.prepareKey(username);
		
		Class<? extends CrudOrmModel> entityclass = UserProfile.class;
		String type = entityclass.getSimpleName().toLowerCase();
		
		/**
		 * Protect to ingest readonly fields
		 */
		ParamNode object_param_nodes = params.getRootParamNode().getChild("object");
		notFoundIfNull(object_param_nodes);
		List<ORMFormField> fields = ORMFormField.removeFields(ORMFormField.getFields(entityclass), "createdate", "updatedate");
		List<RemovedNode> rn = new ArrayList<ParamNode.RemovedNode>();
		for (int pos = 0; pos < fields.size(); pos++) {
			if (fields.get(pos).readonly) {
				object_param_nodes.removeChild(fields.get(pos).name, rn);
			}
		}
		
		CrudOrmEngine<CrudOrmModel> engine = CrudOrmEngine.get(entityclass);
		CrudOrmModel object = engine.read(key);
		if (object == null) {
			notFound();
		}
		
		Binder.bindBean(params.getRootParamNode(), "object", object);
		
		validation.valid(object);
		
		HashMap<String, HashMap<String, String>> fieldspointers = ORMFormField.getFieldPointerTable(usermodelclassresolver, fields);
		
		if (Validation.hasErrors()) {
			renderArgs.put("error", Messages.get("crud.hasErrors"));
			render("User/index.html", type, fields, entityclass, object, fieldspointers);
		}
		engine.saveInternalElement();
		
		flash.success(Admin.getIsSavedFlashMessage(type));
		redirect("User.index");
	}
	
	private static class AsyncAction implements Callable<Boolean> {
		
		private CrudOrmModel element;
		private Method method;
		
		public AsyncAction(CrudOrmModel element, Method method) {
			this.element = element;
			if (element == null) {
				throw new NullPointerException("\"element\" can't to be null");
			}
			this.method = method;
			if (method == null) {
				throw new NullPointerException("\"method\" can't to be null");
			}
		}
		
		public Boolean call() throws Exception {
			method.invoke(element);
			return true;
		}
	}
	
	public static void action(String objtype, String key, String targetmethod) throws Exception {
		String username = Secure.connected();
		String real_key = UserProfile.prepareKey(username);
		
		if (key.equalsIgnoreCase(real_key) == false) {
			forbidden();
		}
		
		Class<? extends CrudOrmModel> entityclass = UserProfile.class;
		// String type = entityclass.getSimpleName().toLowerCase();
		
		CrudOrmEngine<CrudOrmModel> engine = CrudOrmEngine.get(entityclass);
		
		Method method = entityclass.getMethod(targetmethod);
		if (method == null) {
			notFound();
		}
		if (method.isAnnotationPresent(PublishedMethod.class) == false) {
			notFound();
		}
		CrudOrmModel element = engine.read(real_key);
		if (element == null) {
			notFound();
		}
		
		JobsPlugin.executor.submit(new AsyncAction(element, method));
		flash.success(Admin.getActionFlashMessage(objtype, targetmethod, null));
		
		/*try {
			method.invoke(element);
			flash.success(Admin.getActionFlashMessage(objtype, targetmethod, null));
		} catch (InvocationTargetException e) {
			Log2.log.error("Error during remove invoke", e.getTargetException());
			flash.error(Admin.getActionFlashMessage(objtype, targetmethod, e.getTargetException()));
		} catch (Exception e) {
			Log2.log.error("Error during invoke", e);
			flash.error(Admin.getActionFlashMessage(objtype, targetmethod, e));
		}*/
		redirect("User.index", objtype);
	}
	
	private static UserProfile getUserProfile() throws Exception {
		String username = Secure.connected();
		String key = UserProfile.prepareKey(username);
		
		UserProfile userprofile = new UserProfile();
		CrudOrmEngine<UserProfile> engine = new CrudOrmEngine<UserProfile>(userprofile);
		
		if (engine.exists(key)) {
			userprofile = engine.read(key);
		} else {
			userprofile = engine.create();
			userprofile.key = key;
			engine.saveInternalElement();
		}
		return userprofile;
	}
	
	public static void notificationslist() throws Exception {
		String title = Messages.all(play.i18n.Lang.get()).getProperty("userprofile.notifications.pagename");
		UserProfile user = getUserProfile();
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
		if (validation.hasErrors()) {
			redirect("User.notificationslist");
			return;
		}
		flash("lastkey", key);
		
		UserProfile user = getUserProfile();
		getNotification(user, key, true, true).switchCloseStatus(user).save();
		redirect("User.notificationslist");
	}
	
	public static void notificationupdatealert(@Required String key, @Required String reason, @Required Boolean notify) throws Exception {
		if (validation.hasErrors()) {
			redirect("User.notificationslist");
			return;
		}
		flash("lastkey", key);
		
		UserProfile user = getUserProfile();
		NotifyReason n_resaon = NotifyReason.getFromDbRecordName(reason);
		if (n_resaon == null) {
			flash("error", Messages.all(play.i18n.Lang.get()).getProperty("userprofile.notifications.invalidreason"));
			redirect("User.notificationslist");
		}
		
		getNotification(user, key, true, true).updateNotifyReasonForUser(user, n_resaon, notify).save();
		redirect("User.notificationslist");
	}
	
	public static void notificationupdatecomment(@Required String key, String comment) throws Exception {
		if (validation.hasErrors()) {
			redirect("User.notificationslist");
			return;
		}
		flash("lastkey", key);
		UserProfile user = getUserProfile();
		
		getNotification(user, key, true, true).updateComment(user, comment).save();
		redirect("User.notificationslist");
	}
	
	public static void notificationupdateread(@Required String key) throws Exception {
		if (validation.hasErrors()) {
			error(new NullPointerException("Invalid key"));
		}
		
		UserProfile user = getUserProfile();
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
	
	@Check("adminUsers")
	public static void notificationsadminlist() throws Exception {
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
		if (validation.hasErrors()) {
			redirect("User.notificationsadminlist");
			return;
		}
		UserProfile user = getUserProfile();
		Notification notification = Notification.getFromDatabase(key);
		if (notification == null) {
			flash("error", Messages.all(play.i18n.Lang.get()).getProperty("userprofile.notifications.cantfoundselected"));
		} else {
			notification.switchCloseStatus(user).save();
		}
		
		redirect("User.notificationsadminlist");
	}
	
	/**
	 * Send to client actual basket content.
	 */
	@Check("navigate")
	public static void basket_pull() {
		renderJSON(CurrentUserBasket.getBasket());
	}
	
	/**
	 * Get new basket content from client.
	 */
	@Check("navigate")
	public static void basket_push() {
		if (params.get("empty") != null) {
			CurrentUserBasket.dropBasket();
		} else {
			CurrentUserBasket.setBasket(params.getAll("current[]"));
		}
		renderJSON("[]");
	}
	
	@Check("navigate")
	public static void baskets() throws Exception {
		String user_key = UserProfile.prepareKey(Secure.connected());
		Basket basket = new Basket(user_key);
		basket.importSelectedContent();
		
		String title = Messages.all(play.i18n.Lang.get()).getProperty("userprofile.baskets.pagename");
		
		Gson gson = new Gson();
		String all_baskets = gson.toJson(basket.getAllBaskets());
		String basket_selected_name = basket.getSelected();
		render(title, all_baskets, basket_selected_name);
	}
	
	@Check("navigate")
	public static void basket_delete(@Required String name) throws Exception {
		if (validation.hasErrors()) {
			renderJSON("[\"validation error\"]");
		}
		
		String user_key = UserProfile.prepareKey(Secure.connected());
		Basket basket = new Basket(user_key);
		basket.delete(name);
		
		JSONObject jo = new JSONObject();
		jo.put("delete", name);
		renderJSON(jo.toJSONString());
	}
	
	/**
	 * Response JSON Object
	 */
	@Check("navigate")
	public static void basket_get_selected() throws Exception {
		String user_key = UserProfile.prepareKey(Secure.connected());
		Basket basket = new Basket(user_key);
		
		JSONObject jo = new JSONObject();
		jo.put("selected", basket.getSelected());
		renderJSON(jo.toJSONString());
	}
	
	@Check("navigate")
	public static void basket_rename(@Required String name, @Required String newname) throws Exception {
		if (validation.hasErrors()) {
			renderJSON("[\"validation error\"]");
		}
		
		String user_key = UserProfile.prepareKey(Secure.connected());
		Basket basket = new Basket(user_key);
		basket.rename(name, newname);
		
		JSONObject jo = new JSONObject();
		jo.put("rename_from", name);
		jo.put("rename_to", newname);
		renderJSON(jo.toJSONString());
	}
	
	@Check("navigate")
	public static void basket_create(@Required String name, @Required Boolean switch_to_selected) throws Exception {
		if (validation.hasErrors()) {
			renderJSON("[\"validation error\"]");
		}
		
		String user_key = UserProfile.prepareKey(Secure.connected());
		Basket basket = new Basket(user_key);
		basket.createNew(name, switch_to_selected);
		
		JSONObject jo = new JSONObject();
		jo.put("create", name);
		jo.put("switch_to_selected", switch_to_selected);
		renderJSON(jo.toJSONString());
	}
	
	/**
	 * Response JSON Object
	 */
	@Check("navigate")
	public static void basket_get_all_user() {
		String user_key = UserProfile.prepareKey(Secure.connected());
		Basket basket = new Basket(user_key);
		Gson gson = new Gson();
		renderJSON(gson.toJson(basket.getAllBaskets()));
	}
	
	/**
	 * Response basket_pull() with new content.
	 */
	@Check("navigate")
	public static void basket_switch_selected(@Required String name) throws Exception {
		if (validation.hasErrors()) {
			JSONObject jo = new JSONObject();
			jo.put("notselected", true);
			renderJSON(jo.toJSONString());
		}
		
		String user_key = UserProfile.prepareKey(Secure.connected());
		Basket basket = new Basket(user_key);
		basket.switchSelectedBasket(name);
		basket_pull();
	}
	
}
