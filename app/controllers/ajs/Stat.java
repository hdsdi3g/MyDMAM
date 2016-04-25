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
package controllers.ajs;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import controllers.Check;
import hd3gtv.mydmam.metadata.container.ContainerOperations;
import hd3gtv.mydmam.metadata.container.ContainerPreview;
import hd3gtv.mydmam.web.AJSController;
import hd3gtv.mydmam.web.stat.AsyncMetadataAnalystRequest;
import hd3gtv.mydmam.web.stat.AsyncStatRequest;
import hd3gtv.mydmam.web.stat.AsyncStatResult;
import hd3gtv.mydmam.web.stat.AsyncStatResultElement;
import hd3gtv.mydmam.web.stat.AsyncStatResultSubElement;
import hd3gtv.mydmam.web.stat.PathElementStat;

public class Stat extends AJSController {
	
	static {
		AJSController.registerTypeAdapter(ContainerPreview.class, new JsonSerializer<ContainerPreview>() {
			JsonSerializer<ContainerPreview> internal_s = new ContainerPreview.Serializer();
			
			public JsonElement serialize(ContainerPreview src, Type typeOfSrc, JsonSerializationContext context) {
				return internal_s.serialize(src, typeOfSrc, context);
			}
		});
		
		AJSController.registerTypeAdapter(AsyncStatResult.class, new AsyncStatResult.Serializer());
		AJSController.registerTypeAdapter(AsyncStatResultElement.class, new AsyncStatResultElement.Serializer());
		AJSController.registerTypeAdapter(AsyncStatResultSubElement.class, new AsyncStatResultSubElement.Serializer());
		AJSController.registerTypeAdapter(AsyncStatRequest.class, new AsyncStatRequest.Deserializer());
	}
	
	@Check("navigate")
	public static AsyncStatResult cache(AsyncStatRequest request) {
		return new PathElementStat(request).getResult();
	}
	
	@Check("navigate")
	public static JsonObject metadataAnalystResults(AsyncMetadataAnalystRequest request) {
		if (request == null) {
			return null;
		} else if (request.pathelementkey == null) {
			return null;
		} else if (request.mtype == null) {
			return null;
		}
		JsonObject result = ContainerOperations.getRawByMtdKeyForOnlyOneTypeAndCheckedToBeSendedToWebclients(request.pathelementkey, request.mtype);
		if (result == null) {
			return null;
		}
		if (result.has("origin")) {
			result.remove("origin");
		}
		return result;
	}
	
}
