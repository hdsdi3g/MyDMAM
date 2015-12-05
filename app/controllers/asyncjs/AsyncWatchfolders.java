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
package controllers.asyncjs;

import java.util.Arrays;
import java.util.List;

import hd3gtv.mydmam.web.AJSController;

public class AsyncWatchfolders extends AJSController {// TODO refactoring
	
	public String getRequestName() {
		return "watchfolders";
	}
	
	/*	@SuppressWarnings("unchecked")
		public <V extends AsyncJSControllerVerb<Rq, Rp>, Rq extends AsyncJSRequestObject, Rp extends AsyncJSResponseObject> List<V> getManagedVerbs() {
			return (List<V>) Arrays.asList(new AsyncJSWatchfolderListVerb(), new AsyncJSWatchfolderRemoveVerb());
		}*/
	
	public List<String> getMandatoryPrivileges() {
		return Arrays.asList("showJobs");
	}
	
}
