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
package controllers.asyncjs.demoreact;

import hd3gtv.mydmam.web.AsyncJSControllerVerb;
import hd3gtv.mydmam.web.AsyncJSGsonProvider;
import hd3gtv.mydmam.web.AsyncJSSerializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

public class VerbGetCommentList extends AsyncJSControllerVerb<GetList, CommentList> {
	
	public String getVerbName() {
		return "get";
	}
	
	public List<String> getMandatoryPrivileges() {
		return Collections.emptyList();
	}
	
	public CommentList onRequest(GetList request) throws Exception {
		CommentList response = new CommentList();
		response.commentlist = FakeDB.getAll();
		return response;
	}
	
	private static final Type type_CommentList = new TypeToken<ArrayList<Comment>>() {
	}.getType();
	
	public List<AsyncJSSerializer<?>> getJsonSerializers(final AsyncJSGsonProvider gson_provider) {
		List<AsyncJSSerializer<?>> result = new ArrayList<AsyncJSSerializer<?>>();
		result.add(new AsyncJSSerializer<CommentList>() {
			
			public Class<CommentList> getEnclosingClass() {
				return CommentList.class;
			}
			
			public JsonElement serialize(CommentList src, Type typeOfSrc, JsonSerializationContext context) {
				JsonObject result = gson_provider.getGsonSimple().toJsonTree(src).getAsJsonObject();
				result.add("commentlist", gson_provider.getGsonSimple().toJsonTree(src.commentlist, type_CommentList));
				return result;
			}
			
		});
		return result;
	}
	
	public Class<GetList> getRequestClass() {
		return GetList.class;
	}
	
	public Class<CommentList> getResponseClass() {
		return CommentList.class;
	}
	
	public CommentList failResponse() {
		System.out.println("Do fail...");
		return new CommentList();
	}
	
	/*VerbGetRq result = gson_provider.getGsonSimple().fromJson(json, VerbGetRq.class);
	result.somecontent = result.somecontent + "... via Gson";
	result.vals = gson_provider.getGson().fromJson(json.getAsJsonObject().get("vals"), type_ETList);*/
	
}