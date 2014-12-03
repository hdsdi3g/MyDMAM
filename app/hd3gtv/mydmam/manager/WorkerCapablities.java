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
package hd3gtv.mydmam.manager;

import java.util.List;

import com.google.gson.JsonObject;

public abstract class WorkerCapablities {
	
	/**
	 * Can be some bridged storages or not, dependent the Worker need.
	 */
	public abstract List<String> getStoragesAvaliable();
	
	public abstract Class<? extends JobContext> getJobContextClass();
	
	// public abstract String getName();
	
	// public abstract String getCategory();
	
	/**
	 * @return can be null, only for serialization.
	 */
	public abstract JsonObject getParameters();
	
	final boolean isAssignableFrom(JobContext context) {
		if (context == null) {
			return false;
		}
		
		/**
		 * Can handle context class ?
		 */
		Class<? extends JobContext> this_job_context_class = getJobContextClass();
		if (this_job_context_class == null) {
			return false;
		}
		if (this_job_context_class.isAssignableFrom(context.getClass()) == false) {
			return false;
		}
		
		/**
		 * Need to test Storages, item need some Storages
		 */
		List<String> context_needed_storages = context.getNeededIndexedStoragesNames();
		if (context_needed_storages != null) {
			List<String> this_storages_avaliable = getStoragesAvaliable();
			if (this_storages_avaliable == null) {
				return false;
			}
			if (this_storages_avaliable.contains(context_needed_storages) == false) {
				return false;
			}
		}
		
		return true;
	}
	
	final WorkerCapablitiesExporter getStatus() {
		return new WorkerCapablitiesExporter(this);
	}
	
}
