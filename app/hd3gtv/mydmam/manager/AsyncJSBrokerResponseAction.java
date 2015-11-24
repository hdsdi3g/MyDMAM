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
package hd3gtv.mydmam.manager;

import java.lang.reflect.Type;
import java.util.HashMap;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;

import hd3gtv.mydmam.web.AsyncJSResponseObject;
import hd3gtv.mydmam.web.AsyncJSSerializer;
import hd3gtv.tools.GsonIgnore;

public class AsyncJSBrokerResponseAction implements AsyncJSResponseObject {
	
	@GsonIgnore
	HashMap<String, JobNG> modified_jobs;
	
	static class Serializer implements AsyncJSSerializer<AsyncJSBrokerResponseAction> {
		
		private static Type hm_StringJob_typeOfT = new TypeToken<HashMap<String, JobNG>>() {
		}.getType();
		
		public Class<AsyncJSBrokerResponseAction> getEnclosingClass() {
			return AsyncJSBrokerResponseAction.class;
		}
		
		public JsonElement serialize(AsyncJSBrokerResponseAction src, Type typeOfSrc, JsonSerializationContext context) {
			return AppManager.getGson().toJsonTree(src.modified_jobs, hm_StringJob_typeOfT);
		}
		
	}
	
}
