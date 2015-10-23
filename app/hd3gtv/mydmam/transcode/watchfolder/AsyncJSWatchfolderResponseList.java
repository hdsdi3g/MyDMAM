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

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;

import hd3gtv.mydmam.web.AsyncJSResponseObject;
import hd3gtv.mydmam.web.AsyncJSSerializer;
import hd3gtv.tools.GsonIgnore;

public class AsyncJSWatchfolderResponseList implements AsyncJSResponseObject {
	
	@GsonIgnore
	ArrayList<AbstractFoundedFile> items;
	// TODO add List<> jobs (status, progress, steps)
	
	static Type type_List_AbstractFoundedFile = new TypeToken<ArrayList<AbstractFoundedFile>>() {
	}.getType();
	
	static class Serializer implements AsyncJSSerializer<AsyncJSWatchfolderResponseList> {
		
		public JsonElement serialize(AsyncJSWatchfolderResponseList src, Type typeOfSrc, JsonSerializationContext context) {
			return WatchFolderDB.gson_simple.toJsonTree(src.items, type_List_AbstractFoundedFile);
		}
		
		public Class<AsyncJSWatchfolderResponseList> getEnclosingClass() {
			return AsyncJSWatchfolderResponseList.class;
		}
		
	}
}
