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

import java.util.ArrayList;
import java.util.List;

public abstract class WorkerCapablities {
	
	/**
	 * Can be some bridged storages or not, dependent the Worker needs.
	 */
	public abstract List<String> getStoragesAvaliable();
	
	public abstract Class<? extends JobContext> getJobContextClass();
	
	/**
	 * Usefull to declare Profile names, process names... Use the same workflow as StoragesAvaliable.
	 * @return can be null or empty
	 */
	public List<String> getHookedNames() {
		return null;
	}
	
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
		if (context.neededstorages != null) {
			List<String> this_storages_avaliable = getStoragesAvaliable();
			if (this_storages_avaliable == null) {
				return false;
			}
			for (int pos = 0; pos < context.neededstorages.size(); pos++) {
				if (this_storages_avaliable.contains(context.neededstorages.get(pos)) == false) {
					return false;
				}
			}
		}
		
		/**
		 * Need to test Hooked names, item need some Hooked names.
		 */
		if (context.hookednames != null) {
			List<String> this_hooked_names = getHookedNames();
			if (this_hooked_names == null) {
				return false;
			}
			for (int pos = 0; pos < context.hookednames.size(); pos++) {
				if (this_hooked_names.contains(context.hookednames.get(pos)) == false) {
					return false;
				}
			}
		}
		
		return true;
	}
	
	final WorkerCapablitiesExporter getExporter() {
		return new WorkerCapablitiesExporter(this);
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		Class<? extends JobContext> cclass = getJobContextClass();
		if (cclass != null) {
			sb.append("JobContext:");
			sb.append(cclass.getName());
			sb.append(" ");
		}
		
		List<String> storages = getStoragesAvaliable();
		if (storages != null) {
			for (int pos = 0; pos < storages.size(); pos++) {
				sb.append("Storage:");
				sb.append(storages.get(pos));
				sb.append(" ");
			}
		}
		
		List<String> hookednames = getHookedNames();
		if (hookednames != null) {
			for (int pos = 0; pos < hookednames.size(); pos++) {
				sb.append("HookedName:");
				sb.append(hookednames.get(pos));
				sb.append(" ");
			}
		}
		return sb.toString().trim();
	}
	
	public static List<WorkerCapablities> createList(Class<? extends JobContext> context) {
		return createList(context, null);
	}
	
	public static List<WorkerCapablities> createList(final Class<? extends JobContext> context, final List<String> storages_avaliable) {
		ArrayList<WorkerCapablities> result = new ArrayList<WorkerCapablities>(1);
		result.add(new WorkerCapablities() {
			public List<String> getStoragesAvaliable() {
				return storages_avaliable;
			}
			
			public Class<? extends JobContext> getJobContextClass() {
				return context;
			}
		});
		return result;
	}
	
	public static List<WorkerCapablities> createList(final Class<? extends JobContext> context, final List<String> storages_avaliable, final List<String> hooked_names) {
		ArrayList<WorkerCapablities> result = new ArrayList<WorkerCapablities>(1);
		result.add(new WorkerCapablities() {
			public List<String> getStoragesAvaliable() {
				return storages_avaliable;
			}
			
			public Class<? extends JobContext> getJobContextClass() {
				return context;
			}
			
			public List<String> getHookedNames() {
				return hooked_names;
			}
		});
		return result;
	}
}
