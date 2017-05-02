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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import hd3gtv.mydmam.web.AJSController;
import hd3gtv.mydmam.web.PlayBootstrap;
import play.data.validation.Validation;

public class AJS_DAR_EventNew {
	
	String name;
	long planned_date;
	
	public void create() throws Exception {
		PlayBootstrap.validate(Validation.required("name", name), Validation.min("planned_date", planned_date, DARDB.get().getPreviousSendTime()));
		
		DAREvent event = new DAREvent();
		event.creator = AJSController.getUserProfileLongName();
		event.created_at = System.currentTimeMillis();
		event.planned_date = planned_date;
		event.name = validateEventName();
		event.save();
	}
	
	/**
	 * Check before, and alterate the name, if this name exists
	 */
	private String validateEventName() throws ConnectionException {
		if (DAREvent.get(name) != null) {
			LocalDate date = LocalDate.now();
			name = name + " (" + date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)) + ")";
			return validateEventName();
		}
		
		return name;
	}
	
}
