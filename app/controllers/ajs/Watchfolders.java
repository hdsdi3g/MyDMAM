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

import java.util.ArrayList;
import java.util.HashMap;

import controllers.Check;
import hd3gtv.mydmam.manager.JobNG;
import hd3gtv.mydmam.transcode.watchfolder.AsyncJSWatchfolderRequestRemove;
import hd3gtv.mydmam.transcode.watchfolder.AsyncJSWatchfolderResponseList;
import hd3gtv.mydmam.transcode.watchfolder.WatchFolderDB;
import hd3gtv.mydmam.web.AJSController;

public class Watchfolders extends AJSController {
	
	@Check("showJobs")
	public static AsyncJSWatchfolderResponseList list() throws Exception {
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
	
	@Check("showJobs")
	public static void remove(AsyncJSWatchfolderRequestRemove request) throws Exception {
		WatchFolderDB.remove(request.key);
		return;
	}
	
}
