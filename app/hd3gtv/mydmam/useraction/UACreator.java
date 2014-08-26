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

import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.taskqueue.Broker;
import hd3gtv.mydmam.taskqueue.Profile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import models.UserProfile;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

public class UACreator {
	
	private UserProfile userprofile;
	private String basket_name;
	private UARange range;
	private UAFinisherConfiguration finisher;
	
	private ArrayList<Functionalities> functionalities;
	
	private class Functionalities {
		UAFunctionality functionality;
		UAConfigurator associated_user_configuration;
	}
	
	private LinkedHashMap<String, ArrayList<String>> storageindexname_to_itemlist;
	
	public UACreator(UserProfile userprofile, String basket_name, ArrayList<SourcePathIndexerElement> items_spie, UARange range, UAFinisherConfiguration finisher) {
		if (items_spie.isEmpty()) {
			throw new NullPointerException("Items can't to be empty");
		}
		this.userprofile = userprofile;
		this.basket_name = basket_name;
		this.range = range;
		this.finisher = finisher;
		
		functionalities = new ArrayList<UACreator.Functionalities>();
		
		storageindexname_to_itemlist = new LinkedHashMap<String, ArrayList<String>>();
		SourcePathIndexerElement item;
		for (int pos = 0; pos < items_spie.size(); pos++) {
			item = items_spie.get(pos);
			if (storageindexname_to_itemlist.containsKey(item.storagename) == false) {
				storageindexname_to_itemlist.put(item.storagename, new ArrayList<String>());
			}
			storageindexname_to_itemlist.get(item.storagename).add(item.prepare_key());
		}
		// TODO add Notifications param
	}
	
	public UACreator addFunctionality(UAFunctionality functionality, UAConfigurator associated_user_configuration) {
		Functionalities new_func = new Functionalities();
		new_func.functionality = functionality;
		new_func.associated_user_configuration = associated_user_configuration;
		functionalities.add(new_func);
		return this;
	}
	
	/**
	 * @return task key
	 */
	private String createSingleTaskWithRequire(String require, Functionalities configured_functionality, ArrayList<String> items, String storage_name) throws ConnectionException {
		UAJobContext context = new UAJobContext();
		context.functionality_name = configured_functionality.functionality.getName();
		context.user_configuration = configured_functionality.associated_user_configuration;
		context.creator_user_key = userprofile.key;
		context.basket_name = basket_name;
		context.items = items;
		context.range = range;
		context.finisher = finisher;
		
		StringBuffer name = new StringBuffer();
		name.append(configured_functionality.functionality.getLongName());
		name.append(" for ");
		name.append(userprofile.longname);
		name.append(" (");
		name.append(context.items.size());
		name.append(" items)");
		
		Profile profile = new Profile("useraction", configured_functionality.functionality.getName() + "=" + storage_name);
		return Broker.publishTask(name.toString(), profile, context.toContext(), UAJobContext.class, false, 0, require, false);
	}
	
	public void createTasks() throws ConnectionException {
		if (functionalities.isEmpty()) {
			return;
		}
		
		String storage_name;
		ArrayList<String> items;
		String last_require = null;
		if ((range == UARange.ONE_USER_ACTION_BY_STORAGE_AND_BASKET) | (range == UARange.ONE_USER_ACTION_BY_BASKET_ITEM)) {
			for (Map.Entry<String, ArrayList<String>> entry : storageindexname_to_itemlist.entrySet()) {
				storage_name = entry.getKey();
				items = entry.getValue();
				last_require = null;
				for (int pos = 0; pos < functionalities.size(); pos++) {
					last_require = createSingleTaskWithRequire(last_require, functionalities.get(pos), items, storage_name);
				}
				// TODO create Finisher task for last_require
				if (range == UARange.ONE_USER_ACTION_BY_BASKET_ITEM) {
					// TODO create notification
				}
			}
			if ((range == UARange.ONE_USER_ACTION_BY_STORAGE_AND_BASKET) & (last_require != null)) {
				// TODO create notification
			}
		} else if (range == UARange.ONE_USER_ACTION_BY_FUNCTIONALITY) {
			for (int pos = 0; pos < functionalities.size(); pos++) {
				for (Map.Entry<String, ArrayList<String>> entry : storageindexname_to_itemlist.entrySet()) {
					storage_name = entry.getKey();
					items = entry.getValue();
					last_require = createSingleTaskWithRequire(last_require, functionalities.get(pos), items, storage_name);
					// TODO create Finisher task for last_require
				}
				// TODO create notification
			}
		}
		
	}
}
