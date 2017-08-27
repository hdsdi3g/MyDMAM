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
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.util.ArrayList;
import java.util.Enumeration;

import org.apache.commons.lang.SystemUtils;

import hd3gtv.tools.AddressMaster;

@Deprecated
public class MulticastTest {
	
	public static void main(String[] args) throws Exception {
		NetDiscover nd = new NetDiscover(new Protocol("12132131"), new AddressMaster(), new ArrayList<>());
		nd.start();
		
		new BufferedReader(new InputStreamReader(System.in)).readLine();
		System.exit(0);
		
		/**
		 * https://www.iana.org/assignments/multicast-addresses/multicast-addresses.xhtml
		 * http://www.iana.org/assignments/ipv6-multicast-addresses/ipv6-multicast-addresses.xhtml
		 */
		/**
		 * IPv4 Multicast Address, Organization-Local Scope
		 */
		InetAddress group_v4 = InetAddress.getByName("239.0.0.1");
		/**
		 * IPv6 Multicast Address, Link-Local Scope Multicast Addresses, variable scope allocation
		 */
		InetAddress group_v6 = InetAddress.getByName("FF02::110");
		int port = 9160;
		
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		
		while (interfaces.hasMoreElements()) {
			NetworkInterface _interface = interfaces.nextElement();
			if (_interface.isUp() == false | _interface.supportsMulticast() == false | _interface.isLoopback() | _interface.isVirtual() | _interface.getHardwareAddress() == null) {
				continue;
			}
			if (_interface.getInterfaceAddresses().isEmpty()) {
				continue;
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
			}
			
			final ProtocolFamily pf = _pf;
			InetAddress first_addr_interface = _interface.getInterfaceAddresses().stream().map(inet -> {
				return inet.getAddress();
			}).filter(inet_addr -> {
				boolean ipv4_addr = inet_addr instanceof Inet4Address;
				boolean ipv4_itf = pf == StandardProtocolFamily.INET;
				return ipv4_addr == ipv4_itf;
			}).findFirst().orElseThrow(() -> new IOException("Can't found IPv4/6 addr in a IPv6/4 link"));
			
			InetSocketAddress bind_to = new InetSocketAddress(port);
			if (SystemUtils.IS_OS_WINDOWS) {
				bind_to = new InetSocketAddress(first_addr_interface, port);
			}
			
			try {
				DatagramChannel channel = DatagramChannel.open(pf).setOption(StandardSocketOptions.SO_REUSEADDR, true).bind(bind_to);
				MembershipKey key = channel.join(group, _interface);
				
				ByteBuffer buffer = ByteBuffer.allocate(100);
				Runnable r = () -> {
					String value;
					InetSocketAddress _sender;
					try {
						while (channel.isOpen()) {
							_sender = (InetSocketAddress) channel.receive(buffer);
							buffer.flip();
							value = new String(buffer.array());
							buffer.clear();
							System.out.println(">>> Receive-" + Thread.currentThread().getId() + " from " + _sender.getHostString() + " " + value);
						}
					} catch (AsynchronousCloseException e) {
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
				
				Runtime.getRuntime().addShutdownHook(new Thread(() -> {
					try {
						System.out.println("Switch off " + _interface.getName());
						channel.close();
						key.drop();
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
