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
package hd3gtv.mydmam.web.stat;

import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.web.AsyncJSSerializer;
import hd3gtv.tools.GsonIgnore;

import java.lang.reflect.Type;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

public class AsyncStatResultSubElement {
	
	/**
	 * Referer to "this" element
	 */
	@GsonIgnore
	SourcePathIndexerElement reference;
	
	Map<String, Object> mtdsummary;
	
	static class Serializer implements AsyncJSSerializer<AsyncStatResultSubElement> {
		public JsonElement serialize(AsyncStatResultSubElement src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject result = Stat.gson_simple.toJsonTree(src).getAsJsonObject();
			if (src.reference != null) {
				result.add("reference", src.reference.toGson());
			}
			return result;
		}
		
		public Class<AsyncStatResultSubElement> getEnclosingClass() {
			return AsyncStatResultSubElement.class;
		}
	}
	
	boolean isEmpty() {
		if (reference != null) {
			return false;
		}
		if (mtdsummary == null) {
			return true;
		}
		return mtdsummary.isEmpty();
	}
	
}
