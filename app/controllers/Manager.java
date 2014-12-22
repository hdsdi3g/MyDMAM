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
package controllers;

import hd3gtv.mydmam.manager.AppManager;
import hd3gtv.mydmam.manager.InstanceStatus;
import hd3gtv.mydmam.manager.JobNG;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.With;

@With(Secure.class)
public class Manager extends Controller {
	
	@Check("showManager")
	public static void index() {
		flash("pagename", Messages.all(play.i18n.Lang.get()).getProperty("manager.pagename"));
		render();
	}
	
	@Check("showManager")
	public static void allinstances() throws Exception {
		renderJSON(InstanceStatus.Gatherer.getAllInstancesJsonString());
	}
	
	@Check("showManager")
	public static void allworkers() throws Exception {
		String result = InstanceStatus.Gatherer.getAllWorkersJsonString();
		// result = "[]";
		renderJSON(result);
	}
	
	@Check("showJobs")
	public static void jobs() throws Exception {
		flash("pagename", Messages.all(play.i18n.Lang.get()).getProperty("manager.jobs.pagename"));
		String actual_jobs = AppManager.getGson().toJson(JobNG.Utility.getJobsFromUpdateDate(0));
		render(actual_jobs);
	}
	
	@Check("showJobs")
	public static void alljobs() throws Exception {
		renderJSON(AppManager.getGson().toJson(JobNG.Utility.getJobsFromUpdateDate(0)));
	}
	
	@Check("showJobs")
	public static void recentupdatedjobs(@Required Long since) throws Exception {
		if (Validation.hasErrors()) {
			renderJSON("[\"validation error\"]");
		}
		renderJSON(AppManager.getGson().toJson(JobNG.Utility.getJobsFromUpdateDate(since)));
	}
}
