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
package hd3gtv.mydmam.web;

import java.util.Collections;
import java.util.List;

public abstract class AsyncJSController {
	
	/**
	 * @return never null
	 */
	public abstract String getRequestName();
	
	/**
	 * @return never null
	 */
	public abstract <V extends AsyncJSControllerVerb<Rq, Rp>, Rq extends AsyncJSRequestObject, Rp extends AsyncJSResponseObject> List<V> getManagedVerbs();
	
	/**
	 * @return OR list, never null
	 */
	public List<String> getMandatoryPrivileges() {
		return Collections.emptyList();
	}
	
}