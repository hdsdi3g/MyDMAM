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
import java.util.ArrayList;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import hd3gtv.mydmam.web.AsyncJSDeserializer;
import hd3gtv.mydmam.web.AsyncJSRequestObject;
import hd3gtv.tools.GsonIgnore;

public class AsyncJSBrokerRequestAction implements AsyncJSRequestObject {
	
	enum Order {
		delete, stop, setinwait, cancel, hipriority, noexpiration, postponed
	}
	
	@GsonIgnore
	ArrayList<String> jobs_keys;
	
	Order order;
	
	static class Deserializer implements AsyncJSDeserializer<AsyncJSBrokerRequestAction> {
		
		private static Type al_String_typeOfT = new TypeToken<ArrayList<String>>() {
		}.getType();
		
		public AsyncJSBrokerRequestAction deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonObject jo = json.getAsJsonObject();
			AsyncJSBrokerRequestAction result = AppManager.getSimpleGson().fromJson(json, AsyncJSBrokerRequestAction.class);
			result.jobs_keys = AppManager.getGson().fromJson(jo.get("jobs_keys"), al_String_typeOfT);
			return result;
		}
		
		public Class<AsyncJSBrokerRequestAction> getEnclosingClass() {
			return AsyncJSBrokerRequestAction.class;
		}
	}
	
}
