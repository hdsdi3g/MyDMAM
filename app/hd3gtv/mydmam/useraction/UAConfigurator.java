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

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;
import hd3gtv.mydmam.db.orm.ORMFormField;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public final class UAConfigurator implements Log2Dumpable {
	
	UAConfigurator() {
	}
	
	UAConfigurator(Serializable object) {
		if (object == null) {
			throw new NullPointerException("\"object\" can't to be null");
		}
		this.object = object;
		Class<?> entityclass = object.getClass();
		try {
			fields = ORMFormField.getFields(entityclass);
		} catch (SecurityException e) {
			Log2.log.error("Can't to access some fields", e);
			fields = new ArrayList<ORMFormField>();
		} catch (NoSuchFieldException e) {
			Log2.log.error("Can't to load some fields", e);
			fields = new ArrayList<ORMFormField>();
		}
		type = entityclass.getSimpleName().toLowerCase();
		origin = entityclass.getName();
	}
	
	private String type;
	private String origin;
	private Object object;
	private List<ORMFormField> fields;
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("type", type);
		dump.add("type", origin);
		Gson gson = UAManager.getGson();
		dump.add("object", gson.toJson(object));
		dump.add("fields", gson.toJson(fields, ORMFormField.TYPE_AL_FIELDS));
		return dump;
	}
	
	void setObject(Object object) {
		this.object = object;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Serializable> T getObject(Class<T> class_of_T) throws ClassNotFoundException {
		if (class_of_T.isAssignableFrom(Class.forName(origin))) {
			return (T) object;
		} else {
			return null;
		}
	}
	
	public Class<?> getObjectClass() {
		return object.getClass();
	}
	
	public static class JsonUtils implements JsonSerializer<UAConfigurator>, JsonDeserializer<UAConfigurator> {
		
		public JsonElement serialize(UAConfigurator src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject je = new JsonObject();
			je.addProperty("type", src.type);
			je.addProperty("origin", src.origin);
			je.add("object", UAManager.getGson().toJsonTree(src.object));
			je.add("fields", ORMFormField.getJsonFields(src.fields));
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
				result.object = UAManager.getGson().fromJson(je.get("object"), Class.forName(result.origin));
				return result;
			} catch (Exception e) {
				throw new JsonParseException(e);
			}
		}
	}
	
}
