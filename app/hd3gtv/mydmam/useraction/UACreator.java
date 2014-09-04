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
package hd3gtv.mydmam.useraction;

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;
import hd3gtv.mydmam.db.orm.CrudOrmEngine;
import hd3gtv.mydmam.mail.notification.Notification;
import hd3gtv.mydmam.mail.notification.NotifyReason;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.taskqueue.Broker;
import hd3gtv.mydmam.taskqueue.Profile;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import models.UserProfile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

public class UACreator {
	
	private static Gson gson;
	
	static {
		GsonBuilder builder = new GsonBuilder();
		// builder.registerTypeAdapter(UAConfigurator.class, new UAConfigurator.JsonUtils());
		gson = builder.create();
	}
	
	private CrudOrmEngine<UserProfile> user_profile_orm;
	private UserProfile userprofile;
	private String basket_name;
	private UARange range;
	private UAFinisherConfiguration global_finisher;
	private ArrayList<UACreatorConfiguredFunctionality> configured_functionalities;
	private ArrayList<UANotificationDestinator> notificationdestinations;
	private LinkedHashMap<String, ArrayList<String>> storageindexname_to_itemlist;
	private boolean one_click;
	private String usercomment;
	
	private class UACreatorConfiguredFunctionality {
		String functionality_name;
		JsonElement raw_associated_user_configuration;
		
		transient UAConfigurator associated_user_configuration;
		transient UAFunctionality functionality;
		
		void prepare() throws NullPointerException {
			functionality = UAManager.getByName(functionality_name);
			if (functionality == null) {
				throw new NullPointerException("Can't found functionality " + functionality_name + ".");
			}
			if (one_click) {
				associated_user_configuration = functionality.createOneClickDefaultUserConfiguration();
			} else {
				associated_user_configuration = functionality.createEmptyConfiguration();
				if (associated_user_configuration != null) {
					associated_user_configuration.object = gson.fromJson(raw_associated_user_configuration, associated_user_configuration.getObjectClass());
				}
			}
		}
	}
	
	private class UANotificationDestinator implements Log2Dumpable {
		String user_key;
		String reason;
		
		transient UserProfile userprofile;
		transient NotifyReason n_reason;
		
		void prepare() throws NullPointerException, ConnectionException {
			n_reason = NotifyReason.getFromString(reason);
			if (n_reason == null) {
				throw new NullPointerException("Invalid reason " + reason + ".");
			}
			if (userprofile != null) {
				return;
			}
			userprofile = user_profile_orm.read(user_key);
			if (userprofile == null) {
				throw new NullPointerException("Can't found userprofile " + user_key + ".");
			}
		}
		
		public Log2Dump getLog2Dump() {
			Log2Dump dump = new Log2Dump();
			dump.add("user_key", user_key);
			dump.add("reason", reason);
			return dump;
		}
	}
	
	public UACreator(ArrayList<SourcePathIndexerElement> items_spie) throws ConnectionException, IOException {
		user_profile_orm = new CrudOrmEngine<UserProfile>(new UserProfile());
		
		if (items_spie.isEmpty()) {
			throw new NullPointerException("Items can't to be empty");
		}
		storageindexname_to_itemlist = new LinkedHashMap<String, ArrayList<String>>();
		SourcePathIndexerElement item;
		for (int pos = 0; pos < items_spie.size(); pos++) {
			item = items_spie.get(pos);
			if (storageindexname_to_itemlist.containsKey(item.storagename) == false) {
				storageindexname_to_itemlist.put(item.storagename, new ArrayList<String>());
			}
			storageindexname_to_itemlist.get(item.storagename).add(item.prepare_key());
		}
		configured_functionalities = new ArrayList<UACreator.UACreatorConfiguredFunctionality>();
		notificationdestinations = new ArrayList<UACreator.UANotificationDestinator>();
		this.one_click = true;
	}
	
	public void setRange_Finisher_NotOneClick(UAFinisherConfiguration finisher, UARange range) {
		this.global_finisher = finisher;
		this.range = range;
		this.one_click = false;
	}
	
	public UACreator setUserprofile(UserProfile userprofile) {
		this.userprofile = userprofile;
		return this;
	}
	
	public UACreator setBasket_name(String basket_name) {
		this.basket_name = basket_name;
		return this;
	}
	
	/**
	 * @param configured_functionalities_json List<UACreatorConfiguredFunctionality>
	 */
	public UACreator setConfigured_functionalities(String configured_functionalities_json, ArrayList<String> user_restricted_privileges) throws Exception {
		if (configured_functionalities_json == null) {
			throw new NullPointerException("\"configured_functionalities_json\" can't to be null");
		}
		if (configured_functionalities_json.isEmpty()) {
			throw new NullPointerException("\"configured_functionalities_json\" can't to be empty");
		}
		
		Type typeOfT = new TypeToken<ArrayList<UACreatorConfiguredFunctionality>>() {
		}.getType();
		configured_functionalities = gson.fromJson(configured_functionalities_json, typeOfT);
		
		try {
			for (int pos = 0; pos < configured_functionalities.size(); pos++) {
				configured_functionalities.get(pos).prepare();
				if (user_restricted_privileges != null) {
					if (user_restricted_privileges.contains(configured_functionalities.get(pos).functionality_name) == false) {
						throw new SecurityException("Functionality: " + configured_functionalities.get(pos).functionality_name);
					}
				}
			}
		} catch (Exception e) {
			Log2.log.error("Invalid configured_functionalities_json", null, new Log2Dump("associated_user_configuration", configured_functionalities_json));
			configured_functionalities = new ArrayList<UACreator.UACreatorConfiguredFunctionality>(1); // set empty...
			throw new Exception("Invalid configured_functionalities_json", e);
		}
		return this;
	}
	
	public UACreator setConfigured_functionalityForOneClick(String functionality_name) throws Exception {
		if (functionality_name == null) {
			throw new NullPointerException("\"functionality_name\" can't to be null");
		}
		
		configured_functionalities = new ArrayList<UACreator.UACreatorConfiguredFunctionality>(1);
		
		UACreatorConfiguredFunctionality configured_functionality = new UACreatorConfiguredFunctionality();
		configured_functionality.functionality_name = functionality_name;
		configured_functionality.prepare();
		configured_functionalities.add(configured_functionality);
		return this;
	}
	
	/**
	 * @param configured_functionalities_json List<UACreatorConfiguredFunctionality>
	 */
	public UACreator setNotificationdestinations(String notificationdestinations_json) throws Exception {
		if (notificationdestinations_json == null) {
			return this;
		}
		if (notificationdestinations_json.isEmpty()) {
			return this;
		}
		
		Type typeOfT = new TypeToken<ArrayList<UANotificationDestinator>>() {
		}.getType();
		notificationdestinations = gson.fromJson(notificationdestinations_json, typeOfT);
		
		try {
			for (int pos = 0; pos < notificationdestinations.size(); pos++) {
				notificationdestinations.get(pos).prepare();
			}
		} catch (Exception e) {
			Log2.log.error("Invalid notificationdestinations", null, new Log2Dump("notificationdestinations", notificationdestinations_json));
			notificationdestinations = new ArrayList<UACreator.UANotificationDestinator>(1);
			throw new Exception("Invalid configured_functionalities_json", e);
		}
		return this;
	}
	
	public UACreator addNotificationdestinationForCreator(String... reasons) throws NullPointerException, ConnectionException {
		if (reasons == null) {
			return this;
		}
		if (reasons.length == 0) {
			return this;
		}
		
		for (int pos = 0; pos < reasons.length; pos++) {
			UANotificationDestinator notificationdestination = new UANotificationDestinator();
			notificationdestination.reason = reasons[pos];
			if (notificationdestination.reason.isEmpty()) {
				continue;
			}
			notificationdestination.userprofile = userprofile;
			notificationdestination.prepare();
			notificationdestinations.add(notificationdestination);
		}
		
		return this;
	}
	
	/**
	 * @return task key
	 */
	private String createSingleTaskWithRequire(String require, UACreatorConfiguredFunctionality configured_functionality, ArrayList<String> items, String storage_name) throws ConnectionException {
		UAJobContext context = new UAJobContext();
		context.functionality_name = configured_functionality.functionality.getName();
		context.user_configuration = configured_functionality.associated_user_configuration;
		context.creator_user_key = userprofile.key;
		context.basket_name = basket_name;
		context.items = items;
		context.finisher = global_finisher;
		context.range = range;
		
		StringBuffer name = new StringBuffer();
		name.append(configured_functionality.functionality.getLongName());
		name.append(" for ");
		name.append(userprofile.longname);
		name.append(" (");
		name.append(context.items.size());
		name.append(" items in ");
		name.append(storage_name);
		name.append(")");
		
		Profile profile = new Profile("useraction", configured_functionality.functionality.getName() + "=" + storage_name);
		return Broker.publishTask(name.toString(), profile, context.toContext(), UAJobContext.class, false, 0, require, false);
	}
	
	/**
	 * @return task key
	 */
	private String createSingleFinisherTask(String require, ArrayList<String> items, String storage_name) throws ConnectionException {
		UAJobContext context = new UAJobContext();
		context.functionality_name = null;
		context.user_configuration = null;
		context.creator_user_key = userprofile.key;
		context.basket_name = basket_name;
		context.items = items;
		context.finisher = global_finisher;
		context.range = range;
		
		StringBuffer name = new StringBuffer();
		name.append("Finisher for ");
		name.append(userprofile.longname);
		name.append(" (");
		name.append(context.items.size());
		name.append(" items in ");
		name.append(storage_name);
		name.append(")");
		
		Profile profile = new Profile("useraction-finisher", storage_name);
		return Broker.publishTask(name.toString(), profile, context.toContext(), UAJobContext.class, false, 0, require, false);
	}
	
	public void setUsercomment(String usercomment) {
		this.usercomment = usercomment;
	}
	
	public void createTasks() throws Exception {
		if (configured_functionalities.isEmpty()) {
			return;
		}
		if (storageindexname_to_itemlist.isEmpty()) {
			return;
		}
		if (one_click) {
			global_finisher = configured_functionalities.get(0).functionality.getFinisherForOneClick();
			range = configured_functionalities.get(0).functionality.getRangeForOneClick();
		}
		
		String storage_name;
		ArrayList<String> items;
		String last_require = null;
		Notification notification;
		if (range == UARange.ONE_USER_ACTION_BY_STORAGE_AND_BASKET) {
			notification = createNotification();
			for (Map.Entry<String, ArrayList<String>> entry : storageindexname_to_itemlist.entrySet()) {
				storage_name = entry.getKey();
				items = entry.getValue();
				last_require = null;
				for (int pos = 0; pos < configured_functionalities.size(); pos++) {
					last_require = createSingleTaskWithRequire(last_require, configured_functionalities.get(pos), items, storage_name);
					notification.addLinkedTasksJobs(last_require);
				}
				notification.addLinkedTasksJobs(createSingleFinisherTask(last_require, items, storage_name));
			}
			notification.save();
			addUALogEntry(notification);
		} else if (range == UARange.ONE_USER_ACTION_BY_BASKET_ITEM) {
			for (Map.Entry<String, ArrayList<String>> entry : storageindexname_to_itemlist.entrySet()) {
				notification = createNotification();
				storage_name = entry.getKey();
				items = entry.getValue();
				last_require = null;
				for (int pos = 0; pos < configured_functionalities.size(); pos++) {
					last_require = createSingleTaskWithRequire(last_require, configured_functionalities.get(pos), items, storage_name);
					notification.addLinkedTasksJobs(last_require);
				}
				notification.addLinkedTasksJobs(createSingleFinisherTask(last_require, items, storage_name));
				notification.save();
				addUALogEntry(notification);
			}
		} else if (range == UARange.ONE_USER_ACTION_BY_FUNCTIONALITY) {
			for (int pos = 0; pos < configured_functionalities.size(); pos++) {
				notification = createNotification();
				for (Map.Entry<String, ArrayList<String>> entry : storageindexname_to_itemlist.entrySet()) {
					storage_name = entry.getKey();
					items = entry.getValue();
					last_require = createSingleTaskWithRequire(last_require, configured_functionalities.get(pos), items, storage_name);
					notification.addLinkedTasksJobs(last_require);
				}
				notification.save();
				addUALogEntry(notification);
			}
		}
	}
	
	private Notification createNotification() throws ConnectionException, IOException {
		if (usercomment == null) {
			usercomment = "User action by " + userprofile.longname;
		}
		Notification n = Notification.create(userprofile, usercomment + " (by " + userprofile.longname + ")");
		for (int pos = 0; pos < notificationdestinations.size(); pos++) {
			n.updateNotifyReasonForUser(notificationdestinations.get(pos).userprofile, notificationdestinations.get(pos).n_reason, true);
		}
		return n;
	}
	
	private void addUALogEntry(Notification notification) {
		// TODO log in database the UA
		// usercomment
		// userprofile
		// configured_functionalities
		// storageindexname_to_itemlist
		// one_click
		// global_finisher;
		// range;
	}
}
