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
package controllers.asyncjs;

import hd3gtv.mydmam.web.AsyncJSRequestController;
import hd3gtv.mydmam.web.AsyncJSRequestControllerVerb;
import hd3gtv.mydmam.web.AsyncJSRequestDeserializer;
import hd3gtv.mydmam.web.AsyncJSRequestGsonProvider;
import hd3gtv.mydmam.web.AsyncJSRequestSerializer;
import hd3gtv.tools.GsonIgnore;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

public class DemoAsync implements AsyncJSRequestController {
	
	public String getRequestName() {
		return "demosasync";
	}
	
	@Override
	public List<String> getMandatoryPrivileges() {
		return Collections.emptyList();
	}
	
	public static class ExternalTest {
		String aa;
	}
	
	public static class VerbGetRq implements Serializable {
		String somecontent;
		@GsonIgnore
		List<ExternalTest> vals;
	}
	
	public static class VerbGetRp implements Serializable {
		String somereturnvalue;
	}
	
	private static final Type type_ETList = new TypeToken<ArrayList<ExternalTest>>() {
	}.getType();
	
	public class VerbGet implements AsyncJSRequestControllerVerb<VerbGetRq, VerbGetRp> {
		
		public String getVerbName() {
			return "get";
		}
		
		public List<String> getMandatoryPrivileges() {
			return Collections.emptyList();
		}
		
		public VerbGetRp onRequest(VerbGetRq request) throws Exception {
			System.out.println("User send me this ! " + request.somecontent + " " + request.vals.get(0).aa);
			VerbGetRp response = new VerbGetRp();
			response.somereturnvalue = "Thank you: " + new Date();
			return response;
		}
		
		public List<AsyncJSRequestDeserializer<?>> getJsonDeserializers(final AsyncJSRequestGsonProvider gson_provider) {
			List<AsyncJSRequestDeserializer<?>> result = new ArrayList<AsyncJSRequestDeserializer<?>>();
			
			result.add(new AsyncJSRequestDeserializer<VerbGetRq>() {
				
				public VerbGetRq deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
					VerbGetRq result = gson_provider.getGsonSimple().fromJson(json, VerbGetRq.class);
					result.somecontent = result.somecontent + "... via Gson";
					result.vals = gson_provider.getGson().fromJson(json.getAsJsonObject().get("vals"), type_ETList);
					return result;
				}
				
				public Class<VerbGetRq> getEnclosingClass() {
					return VerbGetRq.class;
				}
				
			});
			
			result.add(new AsyncJSRequestDeserializer<ExternalTest>() {
				
				public ExternalTest deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
					ExternalTest result = gson_provider.getGsonSimple().fromJson(json, ExternalTest.class);
					result.aa = result.aa + " also via GSON";
					return result;
				}
				
				public Class<ExternalTest> getEnclosingClass() {
					return ExternalTest.class;
				}
				
			});
			
			return result;
		}
		
		@Override
		public List<AsyncJSRequestSerializer<?>> getJsonSerializers(final AsyncJSRequestGsonProvider gson_provider) {
			List<AsyncJSRequestSerializer<?>> result = new ArrayList<AsyncJSRequestSerializer<?>>();
			result.add(new AsyncJSRequestSerializer<VerbGetRp>() {
				
				public Class<VerbGetRp> getEnclosingClass() {
					return VerbGetRp.class;
				}
				
				public JsonElement serialize(VerbGetRp src, Type typeOfSrc, JsonSerializationContext context) {
					JsonObject result = gson_provider.getGsonSimple().toJsonTree(src).getAsJsonObject();
					result.addProperty("info", "passed by Gson !");
					return result;
				}
				
			});
			return result;
		}
		
		public Class<VerbGetRq> getRequestClass() {
			return VerbGetRq.class;
		}
		
		public Class<VerbGetRp> getResponseClass() {
			return VerbGetRp.class;
		}
		
		public VerbGetRp failResponse() {
			System.out.println("Do fail...");
			VerbGetRp response = new VerbGetRp();
			response.somereturnvalue = "Failure ! " + new Date();
			return response;
		}
		
	}
	
	@SuppressWarnings("unchecked")
	public <V extends AsyncJSRequestControllerVerb<Rq, Rp>, Rq extends Serializable, Rp extends Serializable> List<V> getManagedVerbs() {
		return (List<V>) Arrays.asList(new VerbGet());
	}
	
}
