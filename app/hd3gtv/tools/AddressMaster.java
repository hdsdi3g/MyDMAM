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
 * Copyright (C) hdsdi3g for hd3g.tv 3 déc. 2016
 * 
*/
package hd3gtv.tools;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

public class AddressMaster {
	
	private static final Logger log = Logger.getLogger(AddressMaster.class);
	
	private ArrayList<AddressEntry> all_addresses;
	
	public AddressMaster() throws IOException {
		all_addresses = new ArrayList<>();
		
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		
		while (interfaces.hasMoreElements()) {
			NetworkInterface current_interface = interfaces.nextElement();
			
			current_interface.getInterfaceAddresses().forEach(current_address -> {
				try {
					all_addresses.add(new AddressEntry(current_interface, current_address));
				} catch (Exception e) {
					log.warn("Can't get interface information for " + current_interface.getName(), e);
				}
			});
			
			Enumeration<NetworkInterface> sub_interfaces = current_interface.getSubInterfaces();
			while (sub_interfaces.hasMoreElements()) {
				NetworkInterface current_sub_interface = sub_interfaces.nextElement();
				current_interface.getInterfaceAddresses().forEach(current_address -> {
					try {
						all_addresses.add(new AddressEntry(current_sub_interface, current_address));
					} catch (Exception e) {
						log.warn("Can't get interface informations for " + current_interface.getName(), e);
					}
				});
			}
			
			all_addresses.removeIf(addr -> {
				log.debug("Ignore down interface: " + addr);
				return addr.up == false;
			});
			
			all_addresses.sort((l, r) -> {
				int val = l.compare_by_hwaddr(r);
				if (val == 0) {
					val = l.compare_by_ipv6(r);
					if (val == 0) {
						val = l.compare_by_loopback(r);
						if (val == 0) {
							val = l.compare_by_ptp(r);
							if (val == 0) {
								val = l.compare_by_name(r);
							}
						}
					}
				}
				return val;
			});
		}
		
		if (all_addresses.stream().allMatch(entry -> {
			return isLocalAddress(entry.address);
		})) {
			throw new IOException("No external addresses !");
		}
		
		log.debug("Add host external address: " + all_addresses);
	}
	
	private class AddressEntry {
		byte[] hw_addr;
		int mtu;
		String name;
		InetAddress address;
		InetAddress broadcast;
		short cidr;
		boolean loopback;
		boolean point_to_point;
		boolean up;
		boolean virtual;
		boolean is_ipv6;
		CIDRUtils cidr_tool;
		
		public AddressEntry(NetworkInterface current_interface, InterfaceAddress interface_address) throws SocketException {
			hw_addr = current_interface.getHardwareAddress();
			mtu = current_interface.getMTU();
			name = current_interface.getName();
			loopback = current_interface.isLoopback();
			point_to_point = current_interface.isPointToPoint();
			up = current_interface.isUp();
			virtual = current_interface.isVirtual();
			
			address = interface_address.getAddress();
			broadcast = interface_address.getBroadcast();
			cidr = interface_address.getNetworkPrefixLength();
			
			is_ipv6 = address instanceof Inet6Address;
		}
		
		public String toString() {
			return address.getHostAddress();
		}
		
		public void dump(TableList table) {
			String s_virtual = "";
			if (virtual) {
				s_virtual = "Virtual";
			}
			
			String s_broadcast = "";
			if (broadcast != null) {
				s_broadcast = " > " + broadcast.getHostAddress();
			}
			
			String s_hw_addr = "";
			if (hw_addr != null) {
				s_hw_addr = byteToMac(hw_addr);
			} else if (loopback) {
				s_hw_addr = "Loopback";
			} else if (point_to_point) {
				s_hw_addr = "Point to point";
			}
			
			String protocol = "";
			if (is_ipv6) {
				protocol = "IPv6";
			}
			
			table.addRow(s_hw_addr, name, protocol, address.getHostAddress() + s_broadcast, "/" + String.valueOf(cidr), s_virtual, mtu + "b");
		}
		
		private int compare_by_hwaddr(AddressEntry other) {
			if ((hw_addr != null && other.hw_addr != null) || (hw_addr == null && other.hw_addr == null)) {
				return 0;
			} else if (hw_addr != null) {
				return -1;
			} else {
				return 1;
			}
		}
		
		private int compare_by_ipv6(AddressEntry other) {
			if (is_ipv6 == other.is_ipv6) {
				return 0;
			} else if (is_ipv6) {
				return 1;
			} else {
				return -1;
			}
		}
		
		private int compare_by_loopback(AddressEntry other) {
			if (loopback == other.loopback) {
				return 0;
			} else if (loopback) {
				return -1;
			} else {
				return 1;
			}
		}
		
		private int compare_by_ptp(AddressEntry other) {
			if (point_to_point == other.point_to_point) {
				return 0;
			} else if (point_to_point) {
				return -1;
			} else {
				return 1;
			}
		}
		
		private int compare_by_name(AddressEntry other) {
			return name.compareTo(other.name);
		}
		
		private boolean isInNetworkRange(InetAddress addr) throws UnknownHostException {
			if (cidr_tool == null) {
				cidr_tool = new CIDRUtils(address, cidr);
			}
			return cidr_tool.isInRange(addr);
		}
		
	}
	
	public static boolean isLocalAddress(InetAddress addr) {
		return addr.isLoopbackAddress();
	}
	
	/**
	 * @return local and distant mergued.
	 */
	public Stream<InetAddress> getAddresses() {
		return all_addresses.stream().map(addr -> {
			return addr.address;
		});
	}
	
	public boolean isPublicAndPhysicalAddress(InetAddress this_host_addr_candidate) {
		return all_addresses.stream().anyMatch(addr -> {
			return addr.broadcast != null & addr.address.equals(this_host_addr_candidate);
		});
	}
	
	public Stream<InetAddress> getBroadcastAddresses() {
		return all_addresses.stream().filter(addr -> {
			return addr.broadcast != null;
		}).map(addr -> {
			return addr.broadcast;
		});
	}
	
	/**
	 * @param table w = 7
	 */
	public void dump(TableList table) {
		all_addresses.forEach(addr -> {
			addr.dump(table);
		});
	}
	
	public static final String byteToMac(byte[] b) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < b.length; i++) {
			int v = b[i] & 0xFF;
			
			if (i > 0) {
				sb.append(":");
			}
			if (v < 16) {
				sb.append(0);
			}
			sb.append(Integer.toString(v, 16).toLowerCase());
		}
		return sb.toString();
	}
	
	/**
	 * @return true if can fit in any current connected networks.
	 */
	public boolean isInNetworkRange(InetAddress candidate) {
		return all_addresses.stream().anyMatch(addr_entry -> {
			try {
				return addr_entry.isInNetworkRange(candidate);
			} catch (Exception e) {
				log.error("Can't create CIDR for " + candidate.toString(), e);
				return false;
			}
		});
	}
	
}
