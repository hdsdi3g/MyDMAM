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

public class AJSVerbAdminOperationUser extends AsyncJSControllerVerb<AJSRequestAdminOperationUser, AJSResponseAdminOperationUser> {
	
	public String getVerbName() {
		return "adminoperationuser";
	}
	
	public Class<AJSRequestAdminOperationUser> getRequestClass() {
		return AJSRequestAdminOperationUser.class;
	}
	
	public Class<AJSResponseAdminOperationUser> getResponseClass() {
		return AJSResponseAdminOperationUser.class;
	}
	
	public AJSResponseAdminOperationUser onRequest(AJSRequestAdminOperationUser request) throws Exception {
		AJSResponseAdminOperationUser response = new AJSResponseAdminOperationUser();
		
		switch (request.operation) {
		case CREATE:
			response.user_name = request.createFTPUser().getName();
			response.done = true;
			break;
		case DELETE:
			request.delete();
			response.done = true;
			break;
		case CH_PASSWORD:
			request.chPassword();
			response.done = true;
			break;
		case TOGGLE_ENABLE:
			request.toggleEnable();
			response.done = true;
			break;
		default:
			break;
		}
		
		return response;
	}
	
	public List<String> getMandatoryPrivileges() {
		return Arrays.asList("adminFtpServer");
	}
	
}
