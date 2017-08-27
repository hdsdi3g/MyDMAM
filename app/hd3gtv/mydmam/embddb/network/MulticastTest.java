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
package hd3gtv.mydmam.embddb.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ProtocolFamily;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.util.Enumeration;

public class MulticastTest {
	
	public static void main(String[] args) throws Exception {
		/**
		 * https://www.iana.org/assignments/multicast-addresses/multicast-addresses.xhtml
		 */
		InetAddress group_v4 = InetAddress.getByName("239.0.0.1"); // Organization-Local Scope
		// 224.0.0.3 All Routers on this Subnet
		// 224.0.168.1 AD-HOC Block I
		// 224.0.0.130 IPv4 Multicast Address, Local Network Control Block, Unassigned
		
		/**
		 * http://www.iana.org/assignments/ipv6-multicast-addresses/ipv6-multicast-addresses.xhtml
		 */
		InetAddress group_v6 = InetAddress.getByName("FF02::110");// IPv6 Multicast Address, Link-Local Scope Multicast Addresses, variable scope allocation
		// FF05::15D IPv6 Multicast Address, Site-Local Scope, Unassigned
		int port = 9160;
		/*protocol.getDefaultUDPBroadcastPort()*/;
		
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		
		while (interfaces.hasMoreElements()) {
			NetworkInterface _interface = interfaces.nextElement();
			if (_interface.isUp() == false | _interface.supportsMulticast() == false | _interface.isLoopback() | _interface.isVirtual() | _interface.getHardwareAddress() == null) {
				continue;
			}
			if (_interface.getInterfaceAddresses().isEmpty()) {
				continue;
			}
			if (_interface.getDisplayName().startsWith("Intel(R) Ethernet Connection I217-LM")) {
				// continue;// TODO remove
			}
			
			ProtocolFamily _pf = StandardProtocolFamily.INET;
			InetAddress group = group_v4;
			if (_interface.getInterfaceAddresses().stream().noneMatch(intf -> {
				return intf.getAddress() instanceof Inet4Address;
			})) {
				/**
				 * Only IPv6 on here
				 */
				_pf = StandardProtocolFamily.INET6;
				group = group_v6;
				System.out.println("IPv6 !");
			}
			
			final ProtocolFamily pf = _pf;
			InetAddress first_addr_interface = _interface.getInterfaceAddresses().stream().map(inet -> {
				return inet.getAddress();
			}).filter(inet_addr -> {
				boolean ipv4_addr = inet_addr instanceof Inet4Address;
				boolean ipv4_itf = pf == StandardProtocolFamily.INET;
				return ipv4_addr == ipv4_itf;
			}).findFirst().orElseThrow(() -> new IOException("Can't found IPv4/6 addr in a IPv6/4 link"));
			
			InetSocketAddress bind_to = new InetSocketAddress(first_addr_interface, port);
			if (args.length > 0) {// TODO replace by if Linux
				bind_to = new InetSocketAddress(port);
			}
			
			try {
				DatagramChannel channel = DatagramChannel.open(pf).setOption(StandardSocketOptions.SO_REUSEADDR, true).bind(bind_to);
				MembershipKey key = channel.join(group, _interface);
				
				/*DatagramSocket socket_server = channel.socket();
				socket_server.setBroadcast(true);
				socket_server.bind(addr_to_listen);*/
				ByteBuffer buffer = ByteBuffer.allocate(100);
				Runnable r = () -> {
					String value;
					InetSocketAddress _sender;
					try {
						while (channel.isOpen()) {
							// System.out.println("Wait receive on " + _interface.getDisplayName() + "...");
							_sender = (InetSocketAddress) channel.receive(buffer);
							buffer.flip();
							value = new String(buffer.array());
							buffer.clear();
							System.out.println(">>> Receive-" + Thread.currentThread().getId() + " from " + _sender.getHostString() + " " + value);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					System.out.println("Close receive on " + _interface.getDisplayName());
				};
				new Thread(r).start();
				
				InetSocketAddress send_to = new InetSocketAddress(group, port);
				
				Runnable rsend = () -> {
					try {
						while (channel.isOpen()) {
							int size_sended = channel.send(ByteBuffer.wrap("AAAA".getBytes()), send_to);
							System.out.println("<<< Send-" + Thread.currentThread().getId() + " " + size_sended + " bytes on " + _interface.getName() + " (" + _interface.getDisplayName() + ")");
							Thread.sleep(1000);
						}
					} catch (Exception e1) {
						System.err.println("Error during send on " + _interface.getName() + " (" + _interface.getDisplayName() + ") ");
						e1.printStackTrace();
					}
					System.out.println("Close send on " + _interface.getDisplayName());
				};
				new Thread(rsend).start();
				
				/*Runtime.getRuntime().addShutdownHook(new Thread(() -> {
					try {
						System.out.println("Switch off sender " + _interface.getName());
						channel_send.close();
						multicast_send_member.drop();
						// System.out.println("Done for sender " + _interface.getName());
					} catch (IOException e) {
						e.printStackTrace();
					}
				}));*/
				
				Runtime.getRuntime().addShutdownHook(new Thread(() -> {
					try {
						System.out.println("Switch off " + _interface.getName());
						channel.close();
						key.drop();
						// System.out.println("Done for receiver " + _interface.getName());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}));
			} catch (Exception e1) {
				System.out.println("Error during open on " + _interface.getName() + " (" + _interface.getDisplayName() + ") ");
				e1.printStackTrace();
			}
		}
		
		System.out.println("Ready.");
		new BufferedReader(new InputStreamReader(System.in)).readLine();
		System.out.println("Exit");
		System.exit(0);
	}
	
}
