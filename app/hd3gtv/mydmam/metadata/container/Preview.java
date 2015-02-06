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
package hd3gtv.mydmam.metadata.container;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public final class Preview {
	
	String type;
	String file;
	JsonObject options;
	
	public String getFile() {
		return file;
	}
	
	public String getESType() {
		return type;
	}
	
	public JsonObject getOptions() {
		return options;
	}
	
	Preview() {
	}
	
	public static class Serializer implements JsonSerializer<Preview> {
		public JsonElement serialize(Preview src, Type typeOfSrc, JsonSerializationContext context) {
			Preview p = (Preview) src;
			JsonObject result = new JsonObject();
			result.addProperty("file", p.file);
			result.addProperty("type", p.type);
			result.add("options", p.options);
			return result;
		}
	}
	
	public static class Deserializer implements JsonDeserializer<Preview> {
		public Preview deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			Preview p = new Preview();
			JsonObject source = json.getAsJsonObject();
			p.file = source.get("file").getAsString();
			p.type = source.get("type").getAsString();
			p.options = source.get("options").getAsJsonObject();
			return p;
		}
	}
	
}
