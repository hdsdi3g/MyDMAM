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

/**
 * This is what a worker can do, and this is what a job need.
 */
public abstract class JobDefinition {
	
	public abstract List<String> getStoragesAvaliable();
	
	public abstract String getName();// TODO need to keep this ?
	
	public abstract String getCategory();// TODO need to keep this ?
	
	// TODO don't use equals ! (@see JobContext)
	public final boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (this.getClass().isAssignableFrom(obj.getClass()) == false) {
			return false;
		}
		
		JobDefinition item = (JobDefinition) obj;
		String item_name = item.getName();
		if (item_name == null) {
			return false;
		}
		String item_category = item.getCategory();
		if (item_category == null) {
			return false;
		}
		if (item_category.equals(getCategory()) == false) {
			return false;
		}
		if (item_name.equals(getName()) == false) {
			return false;
		}
		
		/**
		 * Need to test Storages ?
		 */
		List<String> this_storages_avaliable = getStoragesAvaliable();
		if (this_storages_avaliable == null) {
			return true;
		}
		if (this_storages_avaliable.isEmpty()) {
			return true;
		}
		
		/**
		 * item need some Storages
		 */
		List<String> item_storages_avaliable = item.getStoragesAvaliable();
		if (item_storages_avaliable == null) {
			return false;
		}
		if (item_storages_avaliable.isEmpty()) {
			return false;
		}
		
		return this_storages_avaliable.containsAll(item_storages_avaliable); // TODO don't sure for this implementation...
		/*for (int pos = 0; pos < this_storages_avaliable.size(); pos++) {
			if (item_storages_avaliable.contains(this_storages_avaliable.get(pos)) == false) {
				return false;
			}
		}
		return true;*/
	}
	
	final JsonObject toJson() {
		JsonObject result = new JsonObject();
		// TODO for web site display...
		return result;
	}
	
}
