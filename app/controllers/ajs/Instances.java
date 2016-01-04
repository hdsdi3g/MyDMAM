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
 * Copyright (C) hdsdi3g for hd3g.tv 2015
 * 
*/
package controllers.ajs;

import java.lang.reflect.Type;
import java.util.ArrayList;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import controllers.Check;
import hd3gtv.configuration.GitInfo;
import hd3gtv.mydmam.manager.AJSgetItems;
import hd3gtv.mydmam.manager.AppManager;
import hd3gtv.mydmam.manager.InstanceStatus;
import hd3gtv.mydmam.web.AJSController;

public class Instances extends AJSController {
	
	private static Type al_String_typeOfT = new TypeToken<ArrayList<String>>() {
	}.getType();
	
	/*private static Type hm_StringJob_typeOfT = new TypeToken<HashMap<String, JobNG>>() {
	}.getType();*/
	
	private static final InstanceStatus current;
	
	static {
		current = InstanceStatus.getStatic();
		/*AJSController.registerTypeAdapter(AsyncJSBrokerResponseList.class, new JsonSerializer<AsyncJSBrokerResponseList>() {
			public JsonElement serialize(AsyncJSBrokerResponseList src, Type typeOfSrc, JsonSerializationContext context) {
				return src.list;
			}
		});*/
		
		AJSController.registerTypeAdapter(AJSgetItems.class, new JsonDeserializer<AJSgetItems>() {
			public AJSgetItems deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
				AJSgetItems result = new AJSgetItems();
				result.refs = AppManager.getSimpleGson().fromJson(json.getAsJsonObject().get("refs").getAsJsonArray(), al_String_typeOfT);
				return result;
			}
		});
	}
	
	/**
	 * @return instance ref -> raw JS
	 */
	@Check("showInstances")
	public static JsonObject allSummaries() {
		JsonObject result = current.getAll(InstanceStatus.CF_COLS.COL_SUMMARY);
		result.add(current.summary.getInstanceNamePid(), AppManager.getSimpleGson().toJsonTree(current.summary));
		return result;
	}
	
	/**
	 * @return instance ref -> raw JS
	 */
	@Check("showInstances")
	public static JsonObject allThreads() {
		JsonObject result = current.getAll(InstanceStatus.CF_COLS.COL_THREADS);
		result.add(current.summary.getInstanceNamePid(), InstanceStatus.getThreadstacktraces());
		return result;
	}
	
	/**
	 * @return instance ref -> raw JS
	 */
	@Check("showInstances")
	public static JsonObject allClasspaths() {
		JsonObject result = current.getAll(InstanceStatus.CF_COLS.COL_CLASSPATH);
		result.add(current.summary.getInstanceNamePid(), current.getClasspath());
		return result;
	}
	
	/**
	 * @return instance ref -> raw JS
	 */
	@Check("showInstances")
	public static JsonObject allPerfStats() {
		JsonObject result = current.getAll(InstanceStatus.CF_COLS.COL_PERFSTATS);
		result.add(current.summary.getInstanceNamePid(), current.getPerfStats());
		return result;
	}
	
	/**
	 * @return instance ref -> raw JS
	 */
	@Check("showInstances")
	public static JsonObject allItems() {
		JsonObject result = current.getAll(InstanceStatus.CF_COLS.COL_ITEMS);
		result.add(current.summary.getInstanceNamePid(), current.getItems());
		return result;
	}
	
	/**
	 * @return instance ref -> CF -> raw JS
	 */
	/*@Check("showInstances")
	public static JsonObject byrefs(AJSgetItems items) {
		JsonObject result = current.getByKeys(items.refs);
		return result;
	}*/
	
	@Check("showInstances")
	public static void truncate() throws Exception {
		InstanceStatus.truncate();
		Thread.sleep(300);
	}
	
	@Check("showInstances")
	public static String appversion() throws Exception {
		return GitInfo.getFromRoot().getActualRepositoryInformation();
	}
	
}
