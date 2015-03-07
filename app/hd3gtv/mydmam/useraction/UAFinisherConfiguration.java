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
package hd3gtv.mydmam.useraction;

import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public final class UAFinisherConfiguration implements Log2Dumpable {
	
	boolean remove_user_basket_item;
	
	static class Serializer implements JsonSerializer<UAFinisherConfiguration>, JsonDeserializer<UAFinisherConfiguration> {
		
		private Gson gson_simple = new Gson();
		
		public UAFinisherConfiguration deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			return gson_simple.fromJson(json, UAFinisherConfiguration.class);
		}
		
		public JsonElement serialize(UAFinisherConfiguration src, Type typeOfSrc, JsonSerializationContext context) {
			return gson_simple.toJsonTree(src);
		}
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("remove_user_basket_item", remove_user_basket_item);
		return dump;
	}
	
}
