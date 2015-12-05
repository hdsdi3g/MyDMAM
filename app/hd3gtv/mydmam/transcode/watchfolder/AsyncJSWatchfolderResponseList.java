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
package hd3gtv.mydmam.transcode.watchfolder;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Map;

import com.google.common.reflect.TypeToken;

import hd3gtv.mydmam.manager.JobNG;
import hd3gtv.tools.GsonIgnore;

public class AsyncJSWatchfolderResponseList {
	
	@GsonIgnore
	ArrayList<AbstractFoundedFile> items;
	
	@GsonIgnore
	Map<String, JobNG> jobs;
	
	static Type type_List_AbstractFoundedFile = new TypeToken<ArrayList<AbstractFoundedFile>>() {
	}.getType();
	
	static Type type_List_JobsNG = new TypeToken<Map<String, JobNG>>() {
	}.getType();
	
	/*static class Serializer implements AsyncJSSerializer<AsyncJSWatchfolderResponseList> {// TODO ADD to AJS
		
		public JsonElement serialize(AsyncJSWatchfolderResponseList src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject result = new JsonObject();
			result.add("items", WatchFolderDB.gson.toJsonTree(src.items, type_List_AbstractFoundedFile));
			result.add("jobs", AppManager.getGson().toJsonTree(src.jobs, type_List_JobsNG));
			return result;
		}
		
		public Class<AsyncJSWatchfolderResponseList> getEnclosingClass() {
			return AsyncJSWatchfolderResponseList.class;
		}
		
	}*/
}
