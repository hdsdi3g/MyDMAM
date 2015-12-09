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

public class AJSRequestRecent {
	
	public String user_session_ref;
	
	public int max_items;
	
	public String searched_text;
	
	public SearchBySelectActionType searched_action_type;
	
	public enum SearchBySelectActionType {
		ALL, DELETE, RESTOR, STORE, IO, RENAME, MKDIR;
		
		List<String> toActionString() {
			switch (this) {
			case ALL:
				return null;
			case DELETE:
				return Arrays.asList("rmd", "dele");
			case RESTOR:
				return Arrays.asList("rest", "retr");
			case STORE:
				return Arrays.asList("stor", "appe");
			case IO:
				return Arrays.asList("rest", "retr", "stor", "appe");
			case RENAME:
				return Arrays.asList("rnto", "rnfr");
			case MKDIR:
				return Arrays.asList("mkd");
			}
			return null;
		}
	}
	
}
