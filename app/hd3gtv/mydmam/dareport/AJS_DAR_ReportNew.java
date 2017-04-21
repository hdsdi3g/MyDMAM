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

import hd3gtv.mydmam.web.AJSController;
import hd3gtv.mydmam.web.PlayBootstrap;
import play.data.validation.Validation;

public class AJS_DAR_ReportNew {
	
	String event_name;
	JsonArray content;
	
	public void create() throws Exception {
		PlayBootstrap.validate(Validation.required("event_name", event_name), Validation.required("content", content));
		
		DARReport report = new DARReport();
		report.account_user_key = AJSController.getUserProfile().getKey();
		report.created_at = System.currentTimeMillis();
		report.event_name = event_name;
		report.content = content;
		report.save();
	}
	
}
