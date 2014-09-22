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
import hd3gtv.mydmam.db.orm.CrudOrmEngine;
import hd3gtv.mydmam.operation.Basket;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.taskqueue.Job;
import hd3gtv.mydmam.taskqueue.Profile;
import hd3gtv.mydmam.taskqueue.Worker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.UserProfile;

public class UAFinisherWorker extends Worker {
	
	private UAWorker referer;
	private List<Profile> managed_profiles;
	
	public UAFinisherWorker(UAWorker referer) {
		this.referer = referer;
		if (referer == null) {
			throw new NullPointerException("\"referer\" can't to be null");
		}
		
		managed_profiles = new ArrayList<Profile>();
		List<UAFunctionality> functionalities_list = referer.getFunctionalities_list();
		for (int pos = 0; pos < functionalities_list.size(); pos++) {
			managed_profiles.addAll(functionalities_list.get(pos).getFinisherProfiles());
		}
	}
	
	/**
	 * @see UAWorker.process()
	 */
	public void process(Job job) throws Exception {
		UAJobContext context = UAJobContext.importFromJob(job.getContext());
		
		if (context == null) {
			throw new NullPointerException("No \"context\" for job");
		}
		
		if (context.functionality_class == null) {
			throw new NullPointerException("\"context.functionality_class\" can't to be null");
		}
		
		UAFinisherConfiguration finisher = context.finisher;
		if (finisher == null) {
			throw new NullPointerException("\"context.finisher\" can't to be null");
		}
		
		if (context.creator_user_key == null) {
			throw new NullPointerException("\"context.creator_user_key\" can't to be null");
		}
		UserProfile user_profile = new UserProfile();
		CrudOrmEngine<UserProfile> engine = new CrudOrmEngine<UserProfile>(user_profile);
		if (engine.exists(context.creator_user_key)) {
			user_profile = engine.read(context.creator_user_key);
		}
		
		HashMap<String, SourcePathIndexerElement> elements = new HashMap<String, SourcePathIndexerElement>(1);
		Explorer explorer = new Explorer();
		if (context.items != null) {
			elements = explorer.getelementByIdkeys(context.items);
		}
		
		job.progress = 0;
		job.step = 0;
		job.progress_size = 1;
		job.step_count = 1;
		doFinishUserAction(elements, user_profile, context.basket_name, explorer, finisher);
		job.progress = 1;
		job.step = 1;
	}
	
	static void doFinishUserAction(HashMap<String, SourcePathIndexerElement> elements, UserProfile user_profile, String basket_name, Explorer explorer, UAFinisherConfiguration configuration)
			throws Exception {
		
		if (configuration.remove_user_basket_item) {
			try {
				Basket basket = new Basket(user_profile.key);
				List<String> basket_content = basket.getBasketContent(basket_name);
				for (Map.Entry<String, SourcePathIndexerElement> entry : elements.entrySet()) {
					basket_content.remove(entry.getKey());
				}
				basket.setBasketContent(basket_name, basket_content);
			} catch (NullPointerException e) {
				Log2.log.error("Invalid finishing", e);
			}
		}
		
		if (configuration.soft_refresh_source_storage_index_item | configuration.force_refresh_source_storage_index_item) {
			List<SourcePathIndexerElement> items = new ArrayList<SourcePathIndexerElement>();
			
			for (Map.Entry<String, SourcePathIndexerElement> entry : elements.entrySet()) {
				items.add(entry.getValue());
			}
			
			if (configuration.soft_refresh_source_storage_index_item) {
				explorer.refreshStoragePath(items, false);
			}
			
			if (configuration.force_refresh_source_storage_index_item) {
				explorer.refreshStoragePath(items, true);
			}
		}
	}
	
	public String getShortWorkerName() {
		return "useraction_finisher";
	}
	
	public String getLongWorkerName() {
		return "Finish User Action jobs";
	}
	
	public List<Profile> getManagedProfiles() {
		return managed_profiles;
	}
	
	public void forceStopProcess() throws Exception {
	}
	
	public boolean isConfigurationAllowToEnabled() {
		return referer.isConfigurationAllowToEnabled();
	}
	
}
