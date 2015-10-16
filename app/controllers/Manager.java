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

import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.db.status.ClusterStatus;
import hd3gtv.mydmam.db.status.ClusterStatus.ClusterType;
import hd3gtv.mydmam.db.status.StatusReport;
import hd3gtv.mydmam.manager.AppManager;
import hd3gtv.mydmam.manager.InstanceAction;
import hd3gtv.mydmam.manager.InstanceStatus;
import hd3gtv.mydmam.manager.JobAction;
import hd3gtv.mydmam.manager.JobNG;
import hd3gtv.mydmam.manager.WorkerExporter;
import play.Play;
import play.PlayPlugin;
import play.cache.Cache;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.i18n.Messages;
import play.jobs.JobsPlugin;
import play.mvc.Controller;
import play.mvc.With;

@With(Secure.class)
public class Manager extends Controller {
	
	@Check("showManager")
	public static void playjobs() throws Exception {
		flash("pagename", Messages.all(play.i18n.Lang.get()).getProperty("service.playjobs.pagename"));
		
		PlayPlugin plugin = Play.pluginCollection.getPluginInstance(JobsPlugin.class);
		if (plugin == null) {
			throw new NullPointerException("No JobsPlugin enabled");
		}
		String rawplaystatus = plugin.getStatus();
		
		render(rawplaystatus);
	}
	
	@Check("showManager")
	public static void purgeplaycache() throws Exception {
		Loggers.Play.info("Purge Play cache");
		Cache.clear();
		redirect("Manager.playjobs");
	}
	
	@Check("showManager")
	public static void refreshlogconf() throws Exception {
		Loggers.Play.info("Manual refresh log configuration");
		Loggers.refreshLogConfiguration();
		redirect("Manager.playjobs");
	}
	
	@Check("showManager")
	public static void clusterstatus() {
		flash("pagename", Messages.all(play.i18n.Lang.get()).getProperty("service.clusterstatus.report.pagename"));
		ClusterStatus cluster_status = new ClusterStatus();
		cluster_status.prepareReports();
		Map<ClusterType, Map<String, StatusReport>> all_reports = cluster_status.getAllReports();
		render(all_reports);
	}
	
	@Check("showManager")
	public static void index() {
		flash("pagename", Messages.all(play.i18n.Lang.get()).getProperty("manager.pagename"));
		render();
	}
	
	@Check("showManager")
	public static void allinstances() throws Exception {
		renderJSON(InstanceStatus.Gatherer.getAllInstancesJsonString());
		
		/**
		 * Only for accelerate debugging and remove get data time from db
		 */
		String result = Cache.get("InstanceStatus.Gatherer.getAllInstancesJsonString", String.class);
		if (result == null) {
			result = InstanceStatus.Gatherer.getAllInstancesJsonString();
			Cache.set("InstanceStatus.Gatherer.getAllInstancesJsonString", result, "30mn");
		}
		renderJSON(result);
	}
	
	@Check("showManager")
	public static void allworkers() throws Exception {
		renderJSON(WorkerExporter.getAllWorkerStatusJson());
		
		/**
		 * Only for accelerate debugging and remove get data time from db
		 */
		String result = Cache.get("WorkerExporter.getAllWorkerStatusJson", String.class);
		if (result == null) {
			result = WorkerExporter.getAllWorkerStatusJson();
			Cache.set("WorkerExporter.getAllWorkerStatusJson", result, "30mn");
		}
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
