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
package hd3gtv.mydmam.manager;

import java.util.Arrays;
import java.util.List;

import hd3gtv.mydmam.web.AsyncJSControllerVerb;
import hd3gtv.mydmam.web.AsyncJSGsonProvider;
import hd3gtv.mydmam.web.AsyncJSSerializer;

public class AsyncJSBrokerVerbList extends AsyncJSControllerVerb<AsyncJSBrokerRequestList, AsyncJSBrokerResponseList> {
	
	public String getVerbName() {
		return "list";
	}
	
	public Class<AsyncJSBrokerRequestList> getRequestClass() {
		return AsyncJSBrokerRequestList.class;
	}
	
	public Class<AsyncJSBrokerResponseList> getResponseClass() {
		return AsyncJSBrokerResponseList.class;
	}
	
	public AsyncJSBrokerResponseList onRequest(AsyncJSBrokerRequestList request, String caller) throws Exception {
		AsyncJSBrokerResponseList result = new AsyncJSBrokerResponseList();
		result.list = JobNG.Utility.getJobsFromUpdateDate(request.since);
		return result;
	}
	
	public List<? extends AsyncJSSerializer<?>> getJsonSerializers(AsyncJSGsonProvider gson_provider) {
		return Arrays.asList(new AsyncJSBrokerResponseList.Serializer());
	}
}
