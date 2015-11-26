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
package hd3gtv.mydmam.ftpserver;

import java.util.Arrays;
import java.util.List;

import hd3gtv.mydmam.web.AsyncJSControllerVerb;

public class AJSVerbAdminExportSessions extends AsyncJSControllerVerb<AJSRequestAdminExportSessions, AJSResponseAdminExportSessions> {
	
	public String getVerbName() {
		return "adminexportsession";
	}
	
	public Class<AJSRequestAdminExportSessions> getRequestClass() {
		return AJSRequestAdminExportSessions.class;
	}
	
	public Class<AJSResponseAdminExportSessions> getResponseClass() {
		return AJSResponseAdminExportSessions.class;
	}
	
	public AJSResponseAdminExportSessions onRequest(AJSRequestAdminExportSessions request, String caller) throws Exception {
		AJSResponseAdminExportSessions result = new AJSResponseAdminExportSessions();
		result.raw_sessions = FTPActivity.getAllUserActivitiesCSV(request.user_id);
		return result;
	}
	
	public List<String> getMandatoryPrivileges() {
		return Arrays.asList("adminFtpServer");
	}
	
}
