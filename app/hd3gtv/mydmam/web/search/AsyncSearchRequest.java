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
package hd3gtv.mydmam.web.search;

import hd3gtv.mydmam.web.AsyncJSRequestObject;

public class AsyncSearchRequest implements AsyncJSRequestObject {
	
	String q;
	
	/**
	 * Is the page number
	 */
	int from;
	
	public AsyncSearchRequest() {
	}
	
	/**
	 * @param from is the page number
	 */
	public AsyncSearchRequest(String q, int from) {
		this.q = q;
		this.from = from;
	}
	
}