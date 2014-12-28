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
import hd3gtv.mydmam.manager.InstanceAction;
import hd3gtv.mydmam.manager.InstanceStatus;
import hd3gtv.mydmam.manager.JobAction;
import hd3gtv.mydmam.manager.JobNG;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.With;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

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
	
	private static String getCaller() throws Exception {
		StringBuffer caller = new StringBuffer();
		caller.append(User.getUserProfile().key);
		caller.append(" ");
		if (request.isLoopback == false) {
			caller.append(request.remoteAddress);
		} else {
			caller.append("loopback");
		}
		return caller.toString();
	}
	
	@Check("actionManager")
	public static void instanceaction(@Required String target_class_name, @Required String target_reference_key, @Required String json_order) throws Exception {
		if (Validation.hasErrors()) {
			renderJSON("[\"validation error\"]");
		}
		InstanceAction.addNew(target_class_name, target_reference_key, new JsonParser().parse(json_order).getAsJsonObject(), getCaller());
		renderJSON("[]");
	}
	
	@Check("actionJobs")
	public static void jobaction(@Required String requestactions) throws Exception {
		if (Validation.hasErrors()) {
			renderJSON("[\"validation error\"]");
		}
		JobAction action = AppManager.getGson().fromJson(requestactions, JobAction.class);
		JsonElement result = action.doAction(getCaller());
		renderJSON(result.toString());
	}
	
}
