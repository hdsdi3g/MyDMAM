/*
 * This file is part of MyDMAM
 * 
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package controllers;

import hd3gtv.javasimpleservice.IsAlive;
import hd3gtv.mydmam.db.status.ClusterStatus;
import hd3gtv.mydmam.db.status.ClusterStatus.ClusterType;
import hd3gtv.mydmam.db.status.StatusReport;

import java.util.Map;

import play.Play;
import play.PlayPlugin;
import play.i18n.Messages;
import play.jobs.JobsPlugin;
import play.mvc.Controller;
import play.mvc.With;

@With(Secure.class)
public class Service extends Controller {
	
	@Check("showStatus")
	public static void index() {
		flash("pagename", Messages.all(play.i18n.Lang.get()).getProperty("service.pagename"));
		render();
	}
	
	@Check("showStatus")
	@Deprecated
	public static void laststatusworkers() throws Exception {
		renderJSON(IsAlive.getLastStatusWorkers().toJSONString());
	}
	
	@Check("showStatus")
	public static void playjobs() throws Exception {
		flash("pagename", Messages.all(play.i18n.Lang.get()).getProperty("service.playjobs.pagename"));
		
		PlayPlugin plugin = Play.pluginCollection.getPluginInstance(JobsPlugin.class);
		if (plugin == null) {
			throw new NullPointerException("No JobsPlugin enabled");
		}
		String rawplaystatus = plugin.getStatus();
		render(rawplaystatus);
	}
	
	@Check("showStatus")
	public static void clusterstatus() {
		flash("pagename", Messages.all(play.i18n.Lang.get()).getProperty("service.clusterstatus.report.pagename"));
		ClusterStatus cluster_status = new ClusterStatus();
		cluster_status.prepareReports();
		Map<ClusterType, Map<String, StatusReport>> all_reports = cluster_status.getAllReports();
		render(all_reports);
	}
	
}