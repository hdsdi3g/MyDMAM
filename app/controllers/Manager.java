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

import org.apache.commons.io.IOUtils;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.db.status.ClusterStatus;
import hd3gtv.mydmam.db.status.ClusterStatus.ClusterType;
import hd3gtv.mydmam.db.status.StatusReport;
import hd3gtv.mydmam.ftpserver.FTPActivity;
import hd3gtv.mydmam.web.JSSourceManager;
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
		
		Boolean is_js_dev_mode = JSSourceManager.isJsDevMode();
		
		render(rawplaystatus, is_js_dev_mode);
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
	public static void switchjsdevmode() throws Exception {
		JSSourceManager.switchSetJsDevMode();
		Loggers.Play_JSSource.info("Switch JS dev mode to " + JSSourceManager.isJsDevMode());
		JSSourceManager.refreshAllSources();
		redirect("Manager.playjobs");
	}
	
	@Check("showManager")
	public static void purgejs() throws Exception {
		Loggers.Play_JSSource.info("Purge and remake all JS computed");
		JSSourceManager.purgeAll();
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
	
	@Check("adminFtpServer")
	public static void ftpserver_export_user_sessions(@Required String user_session_ref) throws Exception {
		if (Validation.hasErrors()) {
			notFound();
		}
		
		response.contentType = "text/csv";
		String contentDisposition = "%1$s; filename*=UTF-8''%2$s; filename=\"%2$s\"";
		
		response.setHeader("Content-Disposition", String.format(contentDisposition, "attachment", "FTP_activity_" + Loggers.dateFilename(System.currentTimeMillis()) + ".csv"));
		
		try {
			FTPActivity.getAllUserActivitiesCSV(user_session_ref, response.out);
		} catch (Exception e) {
			if (e.getMessage().equals("noindex")) {
				renderText("(No data)");
			}
		}
		IOUtils.closeQuietly(response.out);
	}
	
}
