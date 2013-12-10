/*
 * This file is part of MyDMAM
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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.javasimpleservice;

public class ServiceException extends Exception {
	
	private static final long serialVersionUID = 5549661245559502290L;
	
	boolean mustwarning;
	boolean isfatal;
	
	public ServiceException(Throwable cause, boolean mustwarning, boolean isfatal) {
		super(cause);
		this.mustwarning = mustwarning;
		this.isfatal = isfatal;
	}
	
	public ServiceException(String message, Throwable cause, boolean mustwarning, boolean isfatal) {
		super(message, cause);
		this.mustwarning = mustwarning;
		this.isfatal = isfatal;
	}
	
}
