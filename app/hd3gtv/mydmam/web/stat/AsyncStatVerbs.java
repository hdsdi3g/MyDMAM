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

import hd3gtv.mydmam.metadata.container.ContainerPreview;
import hd3gtv.mydmam.web.AsyncJSControllerVerb;
import hd3gtv.mydmam.web.AsyncJSDeserializer;
import hd3gtv.mydmam.web.AsyncJSGsonProvider;
import hd3gtv.mydmam.web.AsyncJSSerializer;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class AsyncStatVerbs extends AsyncJSControllerVerb<AsyncStatRequest, AsyncStatResult> {
	
	public String getVerbName() {
		return "cache";
	}
	
	public Class<AsyncStatRequest> getRequestClass() {
		return AsyncStatRequest.class;
	}
	
	public Class<AsyncStatResult> getResponseClass() {
		return AsyncStatResult.class;
	}
	
	public AsyncStatResult onRequest(AsyncStatRequest request) throws Exception {
		return new Stat(request).getResult();
	}
	
	public List<? extends AsyncJSSerializer<?>> getJsonSerializers(AsyncJSGsonProvider gson_provider) {
		return Arrays.asList(new AsyncJSSerializer<ContainerPreview>() {
			
			JsonSerializer<ContainerPreview> internal_s = new ContainerPreview.Serializer();
			
			public JsonElement serialize(ContainerPreview src, Type typeOfSrc, JsonSerializationContext context) {
				return internal_s.serialize(src, typeOfSrc, context);
			}
			
			public Class<ContainerPreview> getEnclosingClass() {
				return ContainerPreview.class;
			}
			
		}, new AsyncStatResult.Serializer(), new AsyncStatResultElement.Serializer(), new AsyncStatResultSubElement.Serializer());
	}
	
	public List<? extends AsyncJSDeserializer<?>> getJsonDeserializers(AsyncJSGsonProvider gson_provider) {
		return Arrays.asList(new AsyncStatRequest.Deserializer());
	}
	
}
