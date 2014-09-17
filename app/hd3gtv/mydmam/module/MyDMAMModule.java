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

import hd3gtv.mydmam.cli.CliModule;
import hd3gtv.mydmam.metadata.Generator;
import hd3gtv.mydmam.taskqueue.CyclicCreateTasks;
import hd3gtv.mydmam.taskqueue.TriggerWorker;
import hd3gtv.mydmam.taskqueue.Worker;
import hd3gtv.mydmam.web.MenuEntry;
import hd3gtv.mydmam.web.SearchResultItem;

import java.util.ArrayList;
import java.util.List;

import play.PlayPlugin;

public abstract class MyDMAMModule extends PlayPlugin {
	
	public List<CliModule> getCliModules() {
		return new ArrayList<CliModule>(1);
	}
	
	public List<CyclicCreateTasks> getCyclicsCreateTasks() {
		return new ArrayList<CyclicCreateTasks>(1);
	}
	
	public List<Worker> getWorkers() {
		return new ArrayList<Worker>(1);
	}
	
	public List<TriggerWorker> getTriggersWorker() {
		return new ArrayList<TriggerWorker>(1);
	}
	
	public List<String> getESTypeForUserSearch() {
		return null;
	}
	
	public List<MenuEntry> getPublishedItemsToWebsiteUserMenu() {
		return null;
	}
	
	public List<MenuEntry> getPublishedItemsToWebsiteAdminMenu() {
		return null;
	}
	
	public String getTemplateNameForSearchResultItem(SearchResultItem item) {
		return null;
	}
	
	public ArchivingTapeLocalisator getTapeLocalisator() {
		return null;
	}
	
	public List<Generator> getMetadataGenerator() {
		return null;
	}
}
