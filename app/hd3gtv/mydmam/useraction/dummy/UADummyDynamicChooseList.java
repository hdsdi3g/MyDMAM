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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.mydmam.useraction.dummy;

import hd3gtv.log2.Log2;
import hd3gtv.mydmam.useraction.UASelectAsyncOptions;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class UADummyDynamicChooseList implements UASelectAsyncOptions {
	
	public List<String> getSelectOptionsList() {
		ArrayList<String> list = new ArrayList<String>();
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface currentInterface = interfaces.nextElement();
				Enumeration<InetAddress> addresses = currentInterface.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress currentAddress = addresses.nextElement();
					list.add(currentInterface.getName() + " " + currentAddress.getHostAddress());
				}
			}
		} catch (Exception e) {
			Log2.log.error("Can't list network interfaces", e);
		}
		return list;
	}
	
}
