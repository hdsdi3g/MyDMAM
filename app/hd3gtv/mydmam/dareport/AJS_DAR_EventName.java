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
	
	public void sendMain() {
		PlayBootstrap.validate(Validation.required("name", name));
		
		AJSController.getUserProfile().getEmailAddr();
		// TODO send mail, after check if the event is not in the future.
	}
	
	public void delete() throws ConnectionException {
		PlayBootstrap.validate(Validation.required("name", name));
		DAREvent.delete(name);
	}
	
}
