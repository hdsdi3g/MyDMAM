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
package hd3gtv.mydmam.transcode.watchfolder;

import java.util.Arrays;
import java.util.List;

import hd3gtv.mydmam.web.AsyncJSControllerVerb;
import hd3gtv.mydmam.web.AsyncJSGsonProvider;
import hd3gtv.mydmam.web.AsyncJSSerializer;

public class AsyncJSWatchfolderListVerb extends AsyncJSControllerVerb<AsyncJSWatchfolderRequestList, AsyncJSWatchfolderResponseList> {
	
	public String getVerbName() {
		return "list";
	}
	
	public Class<AsyncJSWatchfolderRequestList> getRequestClass() {
		return AsyncJSWatchfolderRequestList.class;
	}
	
	public Class<AsyncJSWatchfolderResponseList> getResponseClass() {
		return AsyncJSWatchfolderResponseList.class;
	}
	
	public AsyncJSWatchfolderResponseList onRequest(AsyncJSWatchfolderRequestList request) throws Exception {
		AsyncJSWatchfolderResponseList list = new AsyncJSWatchfolderResponseList();
		list.items = WatchFolderDB.getAll();
		// list. TODO request and store all jobs linked to all items
		return list;
	}
	
	public List<? extends AsyncJSSerializer<?>> getJsonSerializers(AsyncJSGsonProvider gson_provider) {
		return Arrays.asList(new AsyncJSWatchfolderResponseList.Serializer());
	}
	
}
