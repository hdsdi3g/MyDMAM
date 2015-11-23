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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam.module;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.search.SearchHit;

import hd3gtv.mydmam.cli.CliModule;
import hd3gtv.mydmam.manager.AppManager;
import hd3gtv.mydmam.manager.CyclicJobCreator;
import hd3gtv.mydmam.manager.InstanceActionReceiver;
import hd3gtv.mydmam.manager.InstanceStatusItem;
import hd3gtv.mydmam.manager.TriggerJobCreator;
import hd3gtv.mydmam.manager.WorkerNG;
import hd3gtv.mydmam.web.MenuEntry;
import hd3gtv.mydmam.web.search.SearchResult;
import hd3gtv.mydmam.web.search.SearchResultPreProcessor;
import play.PlayPlugin;

public abstract class MyDMAMModule extends PlayPlugin implements SearchResultPreProcessor {
	
	public List<CliModule> getCliModules() {
		return new ArrayList<CliModule>(1);
	}
	
	public List<CyclicJobCreator> getCyclicsCreateJobs(AppManager manager) {
		return new ArrayList<CyclicJobCreator>(1);
	}
	
	/**
	 * Don't add workers for user's actions.
	 */
	public List<WorkerNG> getWorkers() {
		return new ArrayList<WorkerNG>(1);
	}
	
	public List<TriggerJobCreator> getTriggersWorker(AppManager manager) {
		return new ArrayList<TriggerJobCreator>(1);
	}
	
	/**
	 * Search API
	 */
	public List<String> getESTypeForUserSearch() {
		return null;
	}
	
	public List<MenuEntry> getPublishedItemsToWebsiteUserMenu() {
		return null;
	}
	
	public List<MenuEntry> getPublishedItemsToWebsiteAdminMenu() {
		return null;
	}
	
	/**
	 * Search API
	 */
	public void prepareSearchResult(SearchHit hit, SearchResult result) {
	}
	
	public ArchivingTapeLocalisator getTapeLocalisator() {
		return null;
	}
	
	public List<InstanceActionReceiver> getSpecificInstanceActionReceiver() {
		return null;
	}
	
	public List<InstanceStatusItem> getSpecificInstanceStatusItem() {
		return null;
	}
}
