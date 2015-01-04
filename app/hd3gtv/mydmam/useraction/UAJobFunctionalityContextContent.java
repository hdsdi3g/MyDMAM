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

import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;
import hd3gtv.mydmam.manager.GsonIgnore;

import java.lang.reflect.Type;
import java.util.ArrayList;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

public final class UAJobFunctionalityContextContent implements Log2Dumpable {
	
	public Class<? extends UAFunctionalityContext> functionality_class;
	public UAConfigurator user_configuration;
	public String creator_user_key;
	public String basket_name;
	public @GsonIgnore ArrayList<String> items;
	
	public boolean remove_user_basket_item;// TODO set...
	public boolean soft_refresh_source_storage_index_item;// TODO set...
	public boolean force_refresh_source_storage_index_item;// TODO set...
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("functionality_class", functionality_class);
		dump.add("creator_user_key", creator_user_key);
		dump.add("basket_name", basket_name);
		dump.add("items", items);
		dump.add("remove_user_basket_item", remove_user_basket_item);
		dump.add("soft_refresh_source_storage_index_item", soft_refresh_source_storage_index_item);
		dump.add("force_refresh_source_storage_index_item", force_refresh_source_storage_index_item);
		dump.addAll(user_configuration);
		return dump;
	}
	
	public UAJobFunctionalityContextContent() {
	}
	
	public JsonObject contextToJson() {
		JsonObject result = UAManager.getGson().toJsonTree(this).getAsJsonObject();
		result.add("items", UAManager.getGson().toJsonTree(items));
		return result;
	}
	
	public static UAJobFunctionalityContextContent contextFromJson(JsonObject json_object) {
		UAJobFunctionalityContextContent result = UAManager.getGson().fromJson(json_object, UAJobFunctionalityContextContent.class);
		Type typeOfT = new TypeToken<ArrayList<String>>() {
		}.getType();
		result.items = UAManager.getGson().fromJson(json_object.get("items"), typeOfT);
		return result;
	}
}
