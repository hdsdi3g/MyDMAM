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
package hd3gtv.mydmam.web.search;

import hd3gtv.mydmam.web.AsyncJSControllerVerb;
import hd3gtv.mydmam.web.AsyncJSGsonProvider;
import hd3gtv.mydmam.web.AsyncJSSerializer;

import java.util.Arrays;
import java.util.List;

public class AsyncSearchVerb extends AsyncJSControllerVerb<AsyncSearchRequest, AsyncSearchQuery> {
	
	public String getVerbName() {
		return "query";
	}
	
	public Class<AsyncSearchRequest> getRequestClass() {
		return AsyncSearchRequest.class;
	}
	
	public Class<AsyncSearchQuery> getResponseClass() {
		return AsyncSearchQuery.class;
	}
	
	public AsyncSearchQuery onRequest(AsyncSearchRequest request) throws Exception {
		return new AsyncSearchQuery().search(request);
	}
	
	public List<? extends AsyncJSSerializer<?>> getJsonSerializers(AsyncJSGsonProvider gson_provider) {
		return Arrays.asList(AsyncSearchQuery.serializer);
	}
	
}