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

import hd3gtv.log2.Log2;
import hd3gtv.mydmam.db.orm.CrudOrmEngine;
import hd3gtv.mydmam.db.orm.CrudOrmModel;
import hd3gtv.mydmam.db.orm.ModelClassResolver;
import hd3gtv.mydmam.db.orm.ORMFormField;
import hd3gtv.mydmam.db.orm.annotations.PublishedMethod;
import hd3gtv.mydmam.mail.notification.Notification;
import hd3gtv.mydmam.mail.notification.NotifyReason;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.taskqueue.Broker;
import hd3gtv.mydmam.taskqueue.TaskJobStatus;
import hd3gtv.mydmam.useraction.Basket;
import hd3gtv.mydmam.web.stat.Stat;
import hd3gtv.mydmam.web.stat.StatElement;

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
@SuppressWarnings("unchecked")
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
	
	static UserProfile getUserProfile() throws Exception {
		String username = Secure.connected();
		String key = UserProfile.prepareKey(username);
		CrudOrmEngine<UserProfile> engine = UserProfile.getORMEngine(key);
		return engine.getInternalElement();
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
		if (Validation.hasErrors()) {
			redirect("User.notificationslist");
			return;
		}
		flash("lastkey", key);
		
		UserProfile user = getUserProfile();
		getNotification(user, key, true, true).switchCloseStatus(user).save();
		redirect("User.notificationslist");
	}
	
	public static void notificationupdatealert(@Required String key, @Required String reason, @Required Boolean notify) throws Exception {
		if (Validation.hasErrors()) {
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
		if (Validation.hasErrors()) {
			redirect("User.notificationslist");
			return;
		}
		flash("lastkey", key);
		UserProfile user = getUserProfile();
		
		getNotification(user, key, true, true).updateComment(user, comment).save();
		redirect("User.notificationslist");
	}
	
	public static void notificationupdateread(@Required String key) throws Exception {
		if (Validation.hasErrors()) {
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
	
	@SuppressWarnings("rawtypes")
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
		if (Validation.hasErrors()) {
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
		try {
			renderJSON(Basket.getBasketForCurrentPlayUser().getSelectedContentJson());
		} catch (Exception e) {
			Log2.log.error("Can't access to basket", e);
			renderJSON("[]");
		}
	}
	
	/**
	 * Get new basket content from client.
	 */
	@Check("navigate")
	public static void basket_push() {
		try {
			Basket basket = Basket.getBasketForCurrentPlayUser();
			if (params.get("empty") != null) {
				basket.deleteSelected();
			} else {
				basket.setSelectedContent(params.getAll("current[]"));
			}
		} catch (Exception e) {
			Log2.log.error("Can't access to basket", e);
		}
		renderJSON("[]");
	}
	
	@Check("navigate")
	public static void baskets() throws Exception {
		String title = Messages.all(play.i18n.Lang.get()).getProperty("userprofile.baskets.pagename");
		
		Basket basket = Basket.getBasketForCurrentPlayUser();
		if (basket.isKeepIndexDeletedBasketItems() == false) {
			JobsPlugin.executor.submit(new User.AsyncCleanBasket(basket));
		}
		
		Gson gson = new Gson();
		String all_baskets = gson.toJson(basket.getAll());
		String basket_selected_name = basket.getSelectedName();
		render(title, all_baskets, basket_selected_name);
	}
	
	@Check("navigate")
	public static void basket_delete(@Required String name) throws Exception {
		if (Validation.hasErrors()) {
			renderJSON("[\"validation error\"]");
		}
		
		Basket basket = Basket.getBasketForCurrentPlayUser();
		basket.delete(name);
		
		JSONObject jo = new JSONObject();
		jo.put("delete", name);
		renderJSON(jo.toJSONString());
	}
	
	@Check("navigate")
	public static void basket_truncate(@Required String name) throws Exception {
		if (Validation.hasErrors()) {
			renderJSON("[\"validation error\"]");
		}
		
		Basket.getBasketForCurrentPlayUser().setContent(name, new ArrayList<String>(1));
		
		JSONObject jo = new JSONObject();
		jo.put("truncate", name);
		renderJSON(jo.toJSONString());
	}
	
	/**
	 * Response JSON Object
	 */
	@Check("navigate")
	public static void basket_get_selected() throws Exception {
		JSONObject jo = new JSONObject();
		jo.put("selected", Basket.getBasketForCurrentPlayUser().getSelectedName());
		renderJSON(jo.toJSONString());
	}
	
	@Check("navigate")
	public static void basket_rename(@Required String name, @Required String newname) throws Exception {
		if (Validation.hasErrors()) {
			renderJSON("[\"validation error\"]");
		}
		
		Basket.getBasketForCurrentPlayUser().rename(name, cleanName(newname));
		
		JSONObject jo = new JSONObject();
		jo.put("rename_from", name);
		jo.put("rename_to", cleanName(newname));
		renderJSON(jo.toJSONString());
	}
	
	private static String cleanName(String rawname) {
		char chr;
		StringBuffer result = new StringBuffer();
		for (int pos = 0; pos < rawname.length(); pos++) {
			chr = rawname.charAt(pos);
			if (Character.isLetterOrDigit(chr)) {
				result.append(chr);
			}
		}
		return result.toString();
	}
	
	@Check("navigate")
	public static void basket_create(@Required String name, @Required Boolean switch_to_selected) throws Exception {
		if (Validation.hasErrors()) {
			renderJSON("[\"validation error\"]");
		}
		
		Basket basket = Basket.getBasketForCurrentPlayUser();
		basket.create(cleanName(name), switch_to_selected);
		
		JSONObject jo = new JSONObject();
		jo.put("create", cleanName(name));
		jo.put("switch_to_selected", switch_to_selected);
		renderJSON(jo.toJSONString());
	}
	
	/**
	 * Response JSON Object
	 */
	@Check("navigate")
	public static void basket_get_all_user() {
		Basket basket;
		try {
			basket = Basket.getBasketForCurrentPlayUser();
			renderJSON(new Gson().toJson(basket.getAll()));
		} catch (Exception e) {
			renderJSON("[]");
		}
	}
	
	/**
	 * Response basket_pull() with new content.
	 */
	@Check("navigate")
	public static void basket_switch_selected(@Required String name) throws Exception {
		if (Validation.hasErrors()) {
			JSONObject jo = new JSONObject();
			jo.put("notselected", true);
			renderJSON(jo.toJSONString());
		}
		
		Basket.getBasketForCurrentPlayUser().setSelected(name);
		basket_pull();
	}
	
	@Check("adminUsers")
	public static void basketsadmin() throws Exception {
		JobsPlugin.executor.submit(new User.AsyncAllUsersCleanBaskets());
		
		String title = Messages.all(play.i18n.Lang.get()).getProperty("userprofile.baskets.admin.pagename");
		
		Gson gson = new Gson();
		Map<String, List<Map<String, Object>>> all_users_all_baskets = Basket.getAllBasketsForAllUsers();
		
		/**
		 * Get user list, and resolve names. Crypt user keys.
		 */
		CrudOrmEngine<UserProfile> user_profile_orm_engine = new CrudOrmEngine<UserProfile>(new UserProfile());
		HashMap<String, String> map_all_users = new HashMap<String, String>();
		
		List<UserProfile> selected_users = user_profile_orm_engine.read(all_users_all_baskets.keySet());
		if (selected_users != null) {
			for (int pos = 0; pos < selected_users.size(); pos++) {
				if (selected_users.get(pos).longname != null) {
					if (selected_users.get(pos).longname.equals("") == false) {
						map_all_users.put(MydmamExtensions.encrypt(selected_users.get(pos).key), selected_users.get(pos).longname);
						continue;
					}
				}
				map_all_users.put(MydmamExtensions.encrypt(selected_users.get(pos).key), selected_users.get(pos).key);
			}
		}
		
		/**
		 * Crypt user keys, and resolve all baskets content pathindexkeys to pathindexelements.
		 */
		Map<String, Object> map_all_users_baskets = new HashMap<String, Object>();
		List<String> list_pathindexkeys = new ArrayList<String>();
		
		List<Map<String, Object>> baskets;
		List<String> content;
		for (Map.Entry<String, List<Map<String, Object>>> entry : all_users_all_baskets.entrySet()) {
			baskets = entry.getValue();
			map_all_users_baskets.put(MydmamExtensions.encrypt(entry.getKey()), baskets);
			/**
			 * Extract all baskets elements
			 */
			for (int pos_baskets = 0; pos_baskets < baskets.size(); pos_baskets++) {
				content = (List<String>) baskets.get(pos_baskets).get("content");
				for (int pos_content = 0; pos_content < content.size(); pos_content++) {
					if (list_pathindexkeys.contains(content.get(pos_content)) == false) {
						list_pathindexkeys.add(content.get(pos_content));
					}
				}
			}
		}
		
		String all_pathindexelements = "{}";
		if (list_pathindexkeys.isEmpty() == false) {
			String[] array_scopes_element = new String[1];
			array_scopes_element[0] = StatElement.SCOPE_PATHINFO;
			Stat stat = new Stat(list_pathindexkeys.toArray(new String[list_pathindexkeys.size()]), array_scopes_element, null);
			all_pathindexelements = stat.toJSONString();
		}
		
		String all_baskets = gson.toJson(map_all_users_baskets);
		String all_users = gson.toJson(map_all_users);
		boolean hasusers = map_all_users.isEmpty() == false;
		
		render(title, all_baskets, all_users, all_pathindexelements, hasusers);
	}
	
	/**
	 * @param userkey is encrypt by Play !
	 */
	@Check("adminUsers")
	public static void basket_admin_action(@Required String userkey, @Required String basketname, @Required String actiontodo, String elementkey, List<String> newcontent) throws Exception {
		if (Validation.hasErrors()) {
			JSONObject jo = new JSONObject();
			jo.put("error", true);
			renderJSON(jo.toJSONString());
		}
		
		HashMap<String, String> result = new HashMap<String, String>();
		result.put("userkey", userkey);
		result.put("basketname", basketname);
		result.put("actiontodo", actiontodo);
		
		String remote_username = MydmamExtensions.decrypt(userkey);
		Basket remote_basket = new Basket(remote_username);
		
		if (actiontodo.equals("importbasket") | actiontodo.equals("exportbasket")) {
			Basket user_basket = Basket.getBasketForCurrentPlayUser();
			List<String> user_basketcontent = user_basket.getSelectedContent();
			List<String> remote_basketcontent = remote_basket.getContent(basketname);
			String item;
			
			if (actiontodo.equals("importbasket")) {
				/**
				 * remote -> me
				 */
				for (int pos = 0; pos < remote_basketcontent.size(); pos++) {
					item = remote_basketcontent.get(pos);
					if (user_basketcontent.contains(item) == false) {
						user_basketcontent.add(item);
					}
				}
				user_basket.setSelectedContent(remote_basketcontent);
			} else {
				/**
				 * me -> remote
				 */
				for (int pos = 0; pos < user_basketcontent.size(); pos++) {
					item = user_basketcontent.get(pos);
					if (remote_basketcontent.contains(item) == false) {
						remote_basketcontent.add(item);
					}
				}
				remote_basket.setSelected(basketname);
				remote_basket.setSelectedContent(remote_basketcontent);
			}
			
		} else if (actiontodo.equals("truncatebasket")) {
			remote_basket.setContent(basketname, new ArrayList<String>(1));
		} else if (actiontodo.equals("removebasket")) {
			remote_basket.delete(basketname);
		} else if (actiontodo.equals("removebasketcontent")) {
			if (elementkey == null) {
				JSONObject jo = new JSONObject();
				jo.put("error", true);
				renderJSON(jo.toJSONString());
			}
			if (elementkey.equals("")) {
				JSONObject jo = new JSONObject();
				jo.put("error", true);
				renderJSON(jo.toJSONString());
			}
			List<String> content = remote_basket.getContent(basketname);
			int pos = content.indexOf(elementkey);
			if (pos > -1) {
				content.remove(pos);
			}
			remote_basket.setContent(basketname, content);
		} else if (actiontodo.equals("overwritebasket")) {
			if (newcontent == null) {
				remote_basket.setContent(basketname, new ArrayList<String>(1));
			} else {
				remote_basket.setContent(basketname, newcontent);
			}
		} else if (actiontodo.equals("destroybaskets")) {
			remote_basket.deleteAll();
		}
		
		Gson g = new Gson();
		renderJSON(g.toJson(result));
	}
	
	static class AsyncCleanBasket implements Callable<Boolean> {
		
		private Basket basket;
		
		public AsyncCleanBasket(Basket basket) {
			this.basket = basket;
			if (basket == null) {
				throw new NullPointerException("\"basket\" can't to be null");
			}
		}
		
		public Boolean call() throws Exception {
			List<Map<String, Object>> baskets = basket.getAll();
			
			List<String> items_to_resolve = new ArrayList<String>();
			
			List<String> basket_items;
			for (int pos_b = 0; pos_b < baskets.size(); pos_b++) {
				basket_items = (List<String>) baskets.get(pos_b).get("content");
				for (int pos_bi = 0; pos_bi < basket_items.size(); pos_bi++) {
					if (items_to_resolve.contains(basket_items.get(pos_bi)) == false) {
						items_to_resolve.add(basket_items.get(pos_bi));
					}
				}
			}
			
			if (items_to_resolve.isEmpty()) {
				return true;
			}
			
			Explorer explorer = new Explorer();
			List<String> items_resolved = explorer.getelementIfExists(items_to_resolve);
			
			String basket_name;
			boolean modified_basket;
			for (int pos_b = 0; pos_b < baskets.size(); pos_b++) {
				basket_name = (String) baskets.get(pos_b).get("name");
				basket_items = (List<String>) baskets.get(pos_b).get("content");
				modified_basket = false;
				for (int pos_bi = basket_items.size() - 1; pos_bi > -1; pos_bi--) {
					if (items_resolved.contains(basket_items.get(pos_bi)) == false) {
						modified_basket = true;
						basket_items.remove(pos_bi);
					}
				}
				if (modified_basket) {
					basket.setContent(basket_name, basket_items);
				}
			}
			
			return true;
		}
	}
	
	private static class AsyncAllUsersCleanBaskets implements Callable<Boolean> {
		
		public Boolean call() throws Exception {
			for (Map.Entry<String, List<Map<String, Object>>> entry : Basket.getAllBasketsForAllUsers().entrySet()) {
				new AsyncCleanBasket(new Basket(entry.getKey())).call();
			}
			return true;
		}
	}
	
}
