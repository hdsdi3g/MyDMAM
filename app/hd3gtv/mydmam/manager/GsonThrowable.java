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
package hd3gtv.mydmam.manager;

import java.lang.reflect.Type;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonIgnore;

public final class GsonThrowable {
	
	@GsonIgnore
	private JsonObject json;
	
	public GsonThrowable(Throwable t) {
		if (t == null) {
			throw new NullPointerException("\"t\" can't to be null");
		}
		json = toJson(t);
	}
	
	private GsonThrowable(JsonObject json) {
		this.json = json;
	}
	
	private static JsonObject toJson(Throwable src) {
		JsonObject result = new JsonObject();
		result.addProperty("message", src.getMessage());
		result.addProperty("class", src.getClass().getName());
		if (src.getCause() != null) {
			result.add("cause", toJson(src.getCause()));
		} else {
			result.add("cause", JsonNull.INSTANCE);
		}
		
		JsonArray ja_stacktrace = new JsonArray();
		StackTraceElement[] stack_trace = src.getStackTrace();
		StackTraceElement trace;
		for (int pos = 0; pos < stack_trace.length; pos++) {
			trace = stack_trace[pos];
			JsonObject jo_trace = new JsonObject();
			jo_trace.addProperty("class", trace.getClassName());
			jo_trace.addProperty("method", trace.getMethodName());
			if (trace.getFileName() != null) {
				jo_trace.addProperty("file", trace.getFileName());
			}
			jo_trace.addProperty("line", trace.getLineNumber());
			ja_stacktrace.add(jo_trace);
		}
		result.add("stacktrace", ja_stacktrace);
		return result;
	}
	
	private static String toJsonString(JsonObject src) {
		StringBuffer sb = new StringBuffer();
		
		sb.append(src.get("class").getAsString());
		if (src.has("message")) {
			if (src.get("message").isJsonNull() == false) {
				sb.append(": ");
				sb.append(src.get("message").getAsString());
			}
		}
		sb.append(MyDMAM.LINESEPARATOR);
		
		JsonArray ja_stack = src.get("stacktrace").getAsJsonArray();
		JsonObject jo_trace;
		for (int pos = 0; pos < ja_stack.size(); pos++) {
			jo_trace = ja_stack.get(pos).getAsJsonObject();
			sb.append("\tat ");
			sb.append(jo_trace.get("class").getAsString());
			sb.append(".");
			sb.append(jo_trace.get("method").getAsString());
			
			if (jo_trace.get("line").getAsInt() == -2) {
				sb.append("(Native Method)");
			} else if (jo_trace.has("file")) {
				if (jo_trace.get("line").getAsInt() >= 0) {
					sb.append("(");
					sb.append(jo_trace.get("file").getAsString());
					sb.append(":");
					sb.append(jo_trace.get("line").getAsInt());
					sb.append(")");
				} else {
					sb.append("(");
					sb.append(jo_trace.get("file").getAsString());
					sb.append(")");
				}
			} else {
				sb.append("(Unknown Source)");
			}
			sb.append(MyDMAM.LINESEPARATOR);
		}
		if (src.get("cause").isJsonNull() == false) {
			sb.append("Caused by: ");
			sb.append(toJsonString(src.get("cause").getAsJsonObject()));
		}
		return sb.toString();
	}
	
	public String getPrintedStackTrace() {
		return toJsonString(json);
	}
	
	public static class Serializer implements JsonSerializer<GsonThrowable>, JsonDeserializer<GsonThrowable> {
		
		public GsonThrowable deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			return new GsonThrowable(json.getAsJsonObject());
		}
		
		public JsonElement serialize(GsonThrowable gt, Type typeOfSrc, JsonSerializationContext context) {
			return gt.json;
		}
		
	}
	
}
