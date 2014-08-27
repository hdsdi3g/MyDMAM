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
import hd3gtv.mydmam.db.orm.ORMFormField;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

public final class UAConfigurator implements Log2Dumpable {
	
	private String type;
	private String origin;
	Object object;
	private List<ORMFormField> fields;
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("type", type);
		dump.add("type", origin);
		Gson gson = UAJobContext.makeGson();
		dump.add("object", gson.toJson(object));
		dump.add("fields", gson.toJson(fields, new TypeToken<ArrayList<ORMFormField>>() {
		}.getType()));
		return dump;
	}
	
	/**
	 * Annoted with hd3gtv.mydmam.db.orm.annotations, and inspected by ORMFormField to extract fields.
	 */
	public void setObject(Object object) throws SecurityException, NoSuchFieldException {
		this.object = object;
		Class<?> entityclass = object.getClass();
		fields = ORMFormField.getFields(entityclass);
		type = entityclass.getSimpleName().toLowerCase();
		origin = entityclass.getName();
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getObject(Class<T> class_of_T) throws ClassNotFoundException {
		if (class_of_T.isAssignableFrom(Class.forName(origin))) {
			return (T) object;
		} else {
			return null;
		}
	}
	
	public Class<?> getObjectClass() {
		return object.getClass();
	}
	
	static class JsonUtils implements JsonSerializer<UAConfigurator>, JsonDeserializer<UAConfigurator> {
		
		public JsonElement serialize(UAConfigurator src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject je = new JsonObject();
			je.add("type", new JsonPrimitive(src.type));
			je.add("origin", new JsonPrimitive(src.origin));
			Gson gson = UAJobContext.makeGson();
			je.add("object", gson.toJsonTree(src.object));
			je.add("fields", gson.toJsonTree(src.fields, new TypeToken<ArrayList<ORMFormField>>() {
			}.getType()));
			return je;
		}
		
		/**
		 * Ignore type and fields
		 */
		public UAConfigurator deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			try {
				JsonObject je = (JsonObject) json;
				UAConfigurator result = new UAConfigurator();
				result.origin = je.get("origin").getAsString();
				result.object = UAJobContext.makeGson().fromJson(je.get("object"), Class.forName(result.origin));
				return result;
			} catch (Exception e) {
				throw new JsonParseException(e);
			}
		}
	}
	
}
