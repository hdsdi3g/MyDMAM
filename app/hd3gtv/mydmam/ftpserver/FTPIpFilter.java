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

import java.net.InetAddress;

import org.apache.ftpserver.ipfilter.IpFilter;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.accesscontrol.AccessControl;

public class FTPIpFilter implements IpFilter {
	
	private static final FTPIpFilter filter;
	
	static {
		filter = new FTPIpFilter();
	}
	
	public static FTPIpFilter getFilter() {
		return filter;
	}
	
	private FTPIpFilter() {
	}
	
	public boolean accept(InetAddress address) {
		Loggers.FTPserver.trace("IPFilter: " + address.getHostAddress());
		return AccessControl.validThisIP(address.getHostAddress());
	}
	
}
