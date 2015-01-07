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
package hd3gtv.mydmam.web;

import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.useraction.UAFinisherConfiguration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import models.UserProfile;

import org.elasticsearch.client.Client;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

@Deprecated
public class UserActionCreator {
	
	private UserProfile userprofile;
	private String basket_name;
	private UAFinisherConfiguration finisher;
	private ArrayList<UserActionCreatorNotificationDestinator> notificationdestinations;
	private LinkedHashMap<String, ArrayList<String>> storageindexname_to_itemlist;
	private String usercomment;
	private ArrayList<String> new_tasks;
	private transient Client client;
	
	public UserActionCreator(ArrayList<SourcePathIndexerElement> items_spie) throws ConnectionException, IOException {
		storageindexname_to_itemlist = new LinkedHashMap<String, ArrayList<String>>();
		SourcePathIndexerElement item;
		for (int pos = 0; pos < items_spie.size(); pos++) {
			item = items_spie.get(pos);
			if (storageindexname_to_itemlist.containsKey(item.storagename) == false) {
				storageindexname_to_itemlist.put(item.storagename, new ArrayList<String>());
			}
			storageindexname_to_itemlist.get(item.storagename).add(item.prepare_key());
		}
		notificationdestinations = new ArrayList<UserActionCreatorNotificationDestinator>();
		new_tasks = new ArrayList<String>();
	}
	
	public void setRangeFinishing(UAFinisherConfiguration finisher) {
		this.finisher = finisher;
	}
	
	public UserActionCreator setUserprofile(UserProfile userprofile) {
		this.userprofile = userprofile;
		return this;
	}
	
	public UserActionCreator setBasket_name(String basket_name) {
		this.basket_name = basket_name;
		return this;
	}
	
	public UserActionCreator addNotificationdestinationForCreator(String... reasons) throws NullPointerException, ConnectionException, IOException {
		if (reasons == null) {
			return this;
		}
		if (reasons.length == 0) {
			return this;
		}
		
		for (int pos = 0; pos < reasons.length; pos++) {
			UserActionCreatorNotificationDestinator notificationdestination = new UserActionCreatorNotificationDestinator();
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
	private String createSingleTaskWithRequire(String require, ArrayList<String> items, String storage_name) throws ConnectionException {
		/*
		UAJobContext context = new UAJobContext();
		context.functionality_class = configured_functionality.functionality.getClass();
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
		*/
		/*Profile profile = new Profile("useraction", configured_functionality.functionality.getSimpleName() + "=" + storage_name);
		return Broker.publishTask(name.toString(), profile, context.toContext(), UAJobContext.class, false, 0, require, false);*/
		return "";
	}
	
	public void setUsercomment(String usercomment) {
		this.usercomment = usercomment;
	}
	
	public void createTasks() throws Exception {
		
	}
	
}
