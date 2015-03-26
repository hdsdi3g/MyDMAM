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

import hd3gtv.configuration.Configuration;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;
import hd3gtv.tools.CIDRUtils;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public abstract class AccessControlAddresses implements Log2Dumpable {
	
	public enum AccessControlAddressesStatus {
		BLACK, WHITE, NEVERBLOCK;
		
		AccessControlAddresses getACA(List<String> addrlist) {
			switch (this) {
			case BLACK:
				return new AccessControlAddressesBlack(addrlist);
			case WHITE:
				return new AccessControlAddressesWhite(addrlist);
			case NEVERBLOCK:
				return new AccessControlAddressesNeverblock(addrlist);
			}
			return null;
		}
		
		static AccessControlAddressesStatus getByClass(Class<? extends AccessControlAddresses> classref) {
			if (classref.isAssignableFrom(AccessControlAddressesBlack.class)) {
				return BLACK;
			}
			if (classref.isAssignableFrom(AccessControlAddressesWhite.class)) {
				return WHITE;
			}
			if (classref.isAssignableFrom(AccessControlAddressesNeverblock.class)) {
				return NEVERBLOCK;
			}
			return null;
		}
	}
	
	AccessControlAddresses(List<String> addrlist) {
		addr_list = new ArrayList<CIDRUtils>();
		for (int pos = 0; pos < addrlist.size(); pos++) {
			try {
				addr_list.add(new CIDRUtils(addrlist.get(pos)));
			} catch (UnknownHostException e) {
				Log2.log.error("Invalid addr notation (CIDR IPv4/IPv6 only)", e, new Log2Dump("addr", addrlist.get(pos)));
			}
		}
	}
	
	private ArrayList<CIDRUtils> addr_list;
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		for (int pos = 0; pos < addr_list.size(); pos++) {
			dump.add(this.getClass().getSimpleName(), addr_list.get(pos).getNetworkAddress() + " -> " + addr_list.get(pos).getBroadcastAddress());
		}
		return dump;
	}
	
	boolean isInRange(String addr) {
		for (int pos = 0; pos < addr_list.size(); pos++) {
			try {
				if (addr_list.get(pos).isInRange(addr)) {
					return true;
				}
			} catch (UnknownHostException e) {
				Log2.log.error("Invalid addr", e, new Log2Dump("addr", addr));
			}
		}
		return false;
	}
	
	private final static ArrayList<AccessControlAddresses> access_control_list;
	
	static {
		access_control_list = new ArrayList<AccessControlAddresses>();
		
		if (Configuration.global.isElementKeyExists("auth", "access_control_addresses")) {
			List<LinkedHashMap<String, ?>> access_control_addresses_list = Configuration.global.getListMapValues("auth", "access_control_addresses");
			
			LinkedHashMap<String, ?> aca_map;
			for (int pos = 0; pos < access_control_addresses_list.size(); pos++) {
				aca_map = access_control_addresses_list.get(pos);
				AccessControlAddressesStatus status = AccessControlAddressesStatus.valueOf(((String) aca_map.get("list")).toUpperCase());
				access_control_list.add(status.getACA(Arrays.asList(((String) aca_map.get("addr")).split(" "))));
				// SubnetUtils su = new SubnetUtils(cidrNotation)
			}
			
			if (access_control_list.size() > 0) {
				Log2Dump dump = new Log2Dump();
				for (int pos = 0; pos < access_control_list.size(); pos++) {
					dump.addAll(access_control_list.get(pos));
				}
				Log2.log.info("Set access control addresses", dump);
			}
		}
	}
	
	public static AccessControlAddressesStatus getAddrStatus(String address) {
		AccessControlAddresses aca;
		for (int pos = 0; pos < access_control_list.size(); pos++) {
			aca = access_control_list.get(pos);
			if (aca.isInRange(address)) {
				return AccessControlAddressesStatus.getByClass(aca.getClass());
			}
		}
		return AccessControlAddressesStatus.NEVERBLOCK;
	}
}
