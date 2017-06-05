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
package hd3gtv.mydmam.db.status;

import java.util.List;

class StatusReportTable {
	
	String name;
	List<String> content;
	
	StatusReportTable(String name, List<String> content) {
		this.name = name;
		if (name == null) {
			throw new NullPointerException("\"name\" can't to be null");
		}
		this.content = content;
		if (content == null) {
			throw new NullPointerException("\"content\" can't to be null");
		}
	}
	
}
