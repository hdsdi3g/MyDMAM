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
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
*/
package hd3gtv.mydmam.dareport;

import java.util.ArrayList;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.dareport.DARDB.Panel;
import hd3gtv.mydmam.dareport.DARDB.PanelType;
import hd3gtv.mydmam.web.AJSController;
import hd3gtv.mydmam.web.PlayBootstrap;
import play.data.validation.Validation;

public class AJS_DAR_ReportNew {
	
	String event_name;
	JsonArray content;
	
	public void create() throws Exception {
		PlayBootstrap.validate(Validation.required("event_name", event_name), Validation.required("content", content));
		
		DAREvent event = DAREvent.get(event_name);
		if (event == null) {
			throw new Exception("Can't found event \"" + event_name + "\" for report creation.");
		}
		if (event.planned_date < DARDB.get().getPreviousSendTime()) {
			throw new Exception("Event \"" + event_name + "\" is too in past (" + Loggers.dateLog(event.planned_date) + "). Report creation time window is closed for it.");
		}
		if (event.planned_date > DARDB.get().getNextSendTime()) {
			throw new Exception("Event \"" + event_name + "\" is not yet open for report creation (" + Loggers.dateLog(event.planned_date) + ").");
		}
		
		String account_user_key = AJSController.getUserProfile().getKey();
		String account_user_name = AJSController.getUserProfileLongName();
		
		DARReport report = DARReport.get(account_user_key, event_name);
		if (report != null) {
			throw new Exception("User " + account_user_name + " has already send it's report for event \"" + event_name + "\" the (" + Loggers.dateLog(report.created_at) + ").");
		}
		DARAccount account = DARAccount.get(account_user_key);
		if (account == null) {
			throw new Exception("Can't found a valid account for " + AJSController.getUserProfileLongName());
		}
		ArrayList<Panel> panels = DARDB.get().getPanelsForJob(account.jobkey);
		if (content.size() != panels.size()) {
			throw new Exception("This report has not the same item count (" + content.size() + ") as the job declaration (" + panels.size() + "). This is the raw content: " + content.toString());
		}
		
		report = new DARReport();
		report.account_user_key = account_user_key;
		report.account_user_name = account_user_name;
		report.account_job = account.jobkey;
		report.account_job_name = DARDB.get().getJobLongName(account.jobkey);
		report.created_at = System.currentTimeMillis();
		report.event_name = event_name;
		
		for (int pos = 0; pos < panels.size(); pos++) {
			Panel panel = panels.get(pos);
			if (panel.type != PanelType.radiobox) {
				throw new Exception("Can't manage panel type " + panel.type);
			}
			
			JsonObject report_entry = content.get(pos).getAsJsonObject();
			boolean check = report_entry.get("check").getAsBoolean();
			
			if (check != panel.reverse_boolean) {
				/**
				 * Nothing to report
				 */
				continue;
			}
			
			report.addContent(panel.label, check, report_entry.get("comment").getAsString());
		}
		
		report.save();
		
		Loggers.DAReport.info("User " + AJSController.getUserProfileLongName() + " has just sent report for " + event_name);
	}
	
}
