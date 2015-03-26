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
package hd3gtv.mydmam.web.acaddr;

import java.util.ArrayList;

public abstract class AccessControlAddresses {
	
	public enum AccessControlAddressesStatus {
		BLACK, WHITE, NEVERBLOCK
	}
	
	// SubnetUtils su = new SubnetUtils(cidrNotation)
	
	private final static ArrayList<AccessControlAddresses> access_control_list;
	
	static {
		access_control_list = new ArrayList<AccessControlAddresses>();
		
		/*
		access_control_addresses:
		-
		    list: black
		    addr: 10.0.0.0/8 172.16.0.0/12 fe80::/10
		 * 
		 * 
		 */
		
	}
	
	public static AccessControlAddressesStatus getAddrStatus(String address) {
		// TODO
		return AccessControlAddressesStatus.NEVERBLOCK;
	}
	
}
