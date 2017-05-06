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

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import hd3gtv.mydmam.web.AJSController;
import hd3gtv.mydmam.web.PlayBootstrap;
import play.data.validation.Validation;

public class AJS_DAR_EventName {
	
	/**
	 * Event name
	 */
	String name;
	
	public void sendMail() throws Exception {
		PlayBootstrap.validate(Validation.required("name", name));
		
		DAREvent event = DAREvent.get(name);
		if (event == null) {
			throw new NullPointerException("Event " + name + " don't exists.");
		}
		if (DARReport.listByEventname(name).isEmpty()) {
			throw new IndexOutOfBoundsException("Event " + name + " has not reports.");
		}
		
		new DARMails().sendReportForAdmin(AJSController.getUserProfile(), event);
	}
	
	public void delete() throws ConnectionException {
		PlayBootstrap.validate(Validation.required("name", name));
		
		if (DARReport.listByEventname(name).isEmpty() == false) {
			throw new IndexOutOfBoundsException("Event " + name + " has reports: can't delete it.");
		}
		
		DAREvent.delete(name);
	}
	
}
