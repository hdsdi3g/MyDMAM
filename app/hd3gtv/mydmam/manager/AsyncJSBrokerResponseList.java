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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import hd3gtv.mydmam.web.AsyncJSResponseObject;
import hd3gtv.mydmam.web.AsyncJSSerializer;

public class AsyncJSBrokerResponseList implements AsyncJSResponseObject {
	
	JsonObject list;
	
	static class Serializer implements AsyncJSSerializer<AsyncJSBrokerResponseList> {
		
		public Class<AsyncJSBrokerResponseList> getEnclosingClass() {
			return AsyncJSBrokerResponseList.class;
		}
		
		public JsonElement serialize(AsyncJSBrokerResponseList src, Type typeOfSrc, JsonSerializationContext context) {
			return src.list;
		}
		
	}
	
}
