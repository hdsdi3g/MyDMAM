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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import hd3gtv.mydmam.manager.JobNG;
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
	
	public AsyncJSWatchfolderResponseList onRequest(AsyncJSWatchfolderRequestList request, String caller) throws Exception {
		AsyncJSWatchfolderResponseList list = new AsyncJSWatchfolderResponseList();
		list.items = WatchFolderDB.getAll();
		if (list.items.isEmpty()) {
			list.jobs = new HashMap<String, JobNG>(1);
			return list;
		}
		
		ArrayList<String> all_jobs = new ArrayList<String>(list.items.size());
		for (int pos = 0; pos < list.items.size(); pos++) {
			all_jobs.addAll(list.items.get(pos).map_job_target.keySet());
		}
		list.jobs = JobNG.Utility.getJobsMapByKeys(all_jobs);
		
		return list;
	}
	
	public List<? extends AsyncJSSerializer<?>> getJsonSerializers(AsyncJSGsonProvider gson_provider) {
		return Arrays.asList(new AsyncJSWatchfolderResponseList.Serializer());
	}
	
}
