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

import com.google.gson.JsonArray;

import hd3gtv.mydmam.Loggers;
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
		
		DARReport report = DARReport.get(account_user_key, event_name);
		if (report != null) {
			throw new Exception("User " + AJSController.getUserProfileLongName() + " has already send it's report for event \"" + event_name + "\" the (" + Loggers.dateLog(report.created_at) + ").");
		}
		
		report = new DARReport();
		report.account_user_key = account_user_key;
		report.created_at = System.currentTimeMillis();
		report.event_name = event_name;
		report.content = content; // TODO IN PRIORITY compact report > keep only checked questions, and its responses. So, if some questions change, sended reports don't.
		report.save();
		
		Loggers.DAReport.info("User " + AJSController.getUserProfileLongName() + " has just sent report for " + event_name);
	}
	
}
