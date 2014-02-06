/*
 * This file is part of Java Tools by hdsdi3g'.
 * 
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2009-2013
 * 
*/
package hd3gtv.tools;

public class ApplicationArgsParam {
	
	String name;
	String value;
	
	public ApplicationArgsParam(String name, String value) {
		this.name = name;
		this.value = value;
	}
	
	public ApplicationArgsParam(String name) {
		this.name = name;
		this.value = null;
	}
	
	public boolean isSimpleParam() {
		return (value == null);
	}
	
	public String getName() {
		return name;
	}
	
	public String getValue() {
		return value;
	}
	
}
