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

import org.apache.commons.io.IOUtils;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.ftpserver.FTPActivity;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

@With(Secure.class)
public class Manager extends Controller {
	
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
