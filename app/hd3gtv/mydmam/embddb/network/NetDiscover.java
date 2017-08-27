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

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
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
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;

import hd3gtv.tools.AddressMaster;

/**
 * Detect and find instances via multicast groups.
 */
class NetDiscover {
	
	private static final Logger log = Logger.getLogger(NetDiscover.class);
	
	private ArrayList<InetSocketAddress> declared_groups;
	private List<Engine> engines;
	private ScheduledFuture<?> sch_future;
	
	NetDiscover(Protocol protocol, AddressMaster addr_master, List<InetSocketAddress> multicast_groups) {
		declared_groups = new ArrayList<>(multicast_groups);
		if (declared_groups.isEmpty()) {
			declared_groups.add(new InetSocketAddress(protocol.getDefaulMulticastIPv4Addr(), protocol.getDefaultUDPMulticastPort()));
			declared_groups.add(new InetSocketAddress(protocol.getDefaulMulticastIPv6Addr(), protocol.getDefaultUDPMulticastPort()));
		}
		
		engines = addr_master.getAllPhysicalInterfaces().map(physicalnetwork_interface -> {
			return new Engine(physicalnetwork_interface);
		}).collect(Collectors.toList());
		
	}
	
	void start() {
		if (engines.isEmpty()) {
			log.warn("Can't start discovering, no valid interfaces (up, physical and multicast enabled) founded");
			return;
		}
		
		engines.forEach(engine -> {
			try {
				engine.start();
			} catch (Exception e) {
				log.error("Can't start engine " + engine, e);
			}
		});
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
		}
		
		List<Engine> active_engines = engines.stream().filter(engine -> {
			return engine.groups.stream().anyMatch(group -> {
				if (group.channel == null) {
					return false;
				}
				if (group.channel.isOpen() == false) {
					return false;
				}
				if (group.receiver == null) {
					return false;
				}
				return group.receiver.isAlive();
			});
		}).collect(Collectors.toList());
		
		if (active_engines.isEmpty()) {
			log.warn("Can't start discovering, no group/interfaces to open and connect");
			return;
		}
		
		String groups_addr = active_engines.stream().flatMap(engine -> {
			return engine.groups.stream();
		}).map(group -> {
			return group.group_socket;
		}).distinct().map(group_socket -> {
			return group_socket.toString();
		}).collect(Collectors.joining(", "));
		
		String interfaces = active_engines.stream().map(engine -> {
			return engine.network_interface.getName();
		}).distinct().collect(Collectors.joining(", "));
		
		log.info("Start discovering and probing to " + groups_addr + " on " + interfaces);
		
		sch_future = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(regular_send, 0, 30, TimeUnit.SECONDS);
	}
	
	void close() {
		if (sch_future != null) {
			sch_future.cancel(false);
		}
		
		engines.forEach(engine -> {
			engine.close();
		});
		
		List<Engine> active_engines = engines.stream().filter(engine -> {
			return engine.groups.isEmpty() == false;
		}).collect(Collectors.toList());
		
		if (active_engines.isEmpty() == false) {
			String groups_addr = active_engines.stream().flatMap(engine -> {
				return engine.groups.stream();
			}).map(group -> {
				return group.group_socket;
			}).distinct().map(group_socket -> {
				return group_socket.toString();
			}).collect(Collectors.joining(", "));
			
			String interfaces = active_engines.stream().map(engine -> {
				return engine.network_interface.getName();
			}).distinct().collect(Collectors.joining(", "));
			
			log.info("Stop discovering and probing from " + groups_addr + " on " + interfaces);
		}
	}
	
	private static final Predicate<NetworkInterface> isIPv4Configured = network_interface -> {
		return network_interface.getInterfaceAddresses().stream().anyMatch(intf -> {
			return intf.getAddress() instanceof Inet4Address;
		});
	};
	
	private static final Predicate<NetworkInterface> isIPv6Configured = network_interface -> {
		return network_interface.getInterfaceAddresses().stream().anyMatch(intf -> {
			return intf.getAddress() instanceof Inet6Address;
		});
	};
	
	private class Engine {
		
		final List<Group> groups;
		final NetworkInterface network_interface;
		
		Engine(NetworkInterface network_interface) {
			this.network_interface = network_interface;
			
			boolean is_ipv4_configured = isIPv4Configured.test(network_interface);
			/**
			 * If IPv4, don't configure IPv6
			 */
			boolean is_ipv6_configured = is_ipv4_configured == false & isIPv6Configured.test(network_interface);
			
			groups = declared_groups.stream().filter(group_socket -> {
				return (is_ipv4_configured && group_socket.getAddress() instanceof Inet4Address) | (is_ipv6_configured && group_socket.getAddress() instanceof Inet6Address);
			}).map(group_socket -> {
				try {
					return new Group(group_socket);
				} catch (IOException e) {
					log.debug("Can't create group", e);
					return null;
				}
			}).filter(group -> {
				return group != null;
			}).collect(Collectors.toList());
		}
		
		public String toString() {
			String disp_name = network_interface.getDisplayName();
			if (disp_name.length() > 10) {
				disp_name = disp_name.substring(0, 10);
			}
			String net_name = network_interface.getName();
			if (net_name.equalsIgnoreCase(disp_name)) {
				return net_name;
			} else {
				return net_name + " (via " + disp_name + ")";
			}
		}
		
		private String toSuperString() {
			return toString();
		}
		
		void start() throws IOException {
			try {
				groups.forEach(group -> {
					try {
						group.start();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				});
			} catch (RuntimeException e) {
				throw new IOException(e.getCause());
			}
		}
		
		void close() {
			groups.forEach(group -> {
				group.close();
			});
		}
		
		private class Group {
			
			final InetSocketAddress group_socket;
			InetSocketAddress bind_to;
			ProtocolFamily pf;
			Thread receiver;
			DatagramChannel channel;
			MembershipKey key;
			
			Group(InetSocketAddress group_socket) throws IOException {
				this.group_socket = group_socket;
				
				if (group_socket.getAddress() instanceof Inet4Address) {
					pf = StandardProtocolFamily.INET;
				} else {
					pf = StandardProtocolFamily.INET6;
				}
				
				InetAddress first_addrnetwork_interface = network_interface.getInterfaceAddresses().stream().map(inet -> {
					return inet.getAddress();
				}).filter(inet_addr -> {
					return pf == StandardProtocolFamily.INET & inet_addr instanceof Inet4Address | pf == StandardProtocolFamily.INET6 & inet_addr instanceof Inet6Address;
				}).findFirst().orElseThrow(() -> new IOException("Can't found IPv4/6 addr in a IPv6/4 link"));
				
				bind_to = new InetSocketAddress(group_socket.getPort());
				if (SystemUtils.IS_OS_WINDOWS) {
					bind_to = new InetSocketAddress(first_addrnetwork_interface, group_socket.getPort());
				}
			}
			
			public String toString() {
				return group_socket + " on " + toSuperString();
			}
			
			void start() throws IOException {
				channel = DatagramChannel.open(pf).setOption(StandardSocketOptions.SO_REUSEADDR, true).bind(bind_to);
				key = channel.join(group_socket.getAddress(), network_interface);
				
				ByteBuffer buffer = ByteBuffer.allocate(Protocol.BUFFER_SIZE);
				receiver = new Thread(() -> {
					InetSocketAddress sender;
					try {
						while (channel.isOpen()) {
							sender = (InetSocketAddress) channel.receive(buffer);
							buffer.flip();
							/*String value = */new String(buffer.array());// TODO real protocol
							buffer.clear();
							
							if (log.isTraceEnabled()) {
								log.trace("Receive datagram from " + sender.getHostString() + " on " + toString());
							}
						}
					} catch (AsynchronousCloseException e) {
					} catch (IOException e) {
						log.error("Trouble with " + toString(), e);
					}
					log.debug("Close receive on " + toString());
				});
				receiver.setDaemon(true);
				receiver.setName("NetDiscover-" + toString());
				receiver.start();
			}
			
			void close() {
				try {
					if (channel != null) {
						channel.close();
					}
				} catch (IOException e) {
					log.debug("Can't close socket " + toString(), e);
				}
				if (key != null) {
					key.drop();
				}
			}
			
		}
		
	}
	
	private Runnable regular_send = () -> {
		engines.stream().flatMap(engine -> {
			return engine.groups.stream();
		}).filter(group -> {
			return group.channel != null;
		}).filter(group -> {
			return group.channel.isOpen();
		}).forEach(group -> {
			try {
				int size_sended = group.channel.send(ByteBuffer.wrap("AAAA".getBytes()), group.group_socket);// TODO real protocol
				if (log.isTraceEnabled()) {
					log.trace("Send datagram (" + size_sended + " bytes) to " + group.toString());
				}
			} catch (IOException e) {
				log.warn("Can't send datagram to " + group.toString(), e);
			}
		});
	};
	
	/*
	//TODO send UUID, public & open IPs/ports, "mydmam + url", password hash, version 
	
	
	 * return json_host_public_listened_addrs;
	private byte[] refreshListenAddrList() {
		counter_for_regular_refresh_listened_addrs.set(0);
		
		List<InetSocketAddress> addrs = manager.getListenedServerAddress().filter(socket -> {
			return manager.getAddressMaster().isPublicAndPhysicalAddress(socket.getAddress());
		}).collect(Collectors.toList());
		
		if (addrs.isEmpty()) {
			json_host_public_listened_addrs = null;
		} else {
			json_host_public_listened_addrs = MyDMAM.gson_kit.getGsonSimple().toJson(addrs).getBytes(MyDMAM.UTF8);
		}
		return json_host_public_listened_addrs;
	}
	
	void startRegularSend() {
		refreshListenAddrList();
		uuid_string = manager.getUUIDRef().toString().getBytes(MyDMAM.UTF8);
	}
	
	
	
	
		private void readDatagram(byte[] raw_datas) throws IOException, GeneralSecurityException {
		byte[] datas = manager.getProtocol().decrypt(raw_datas, 0, raw_datas.length);
		
		if (log.isTraceEnabled()) {
			log.trace("Get raw datas after decrypt" + Hexview.LINESEPARATOR + Hexview.tracelog(datas));
		}
		
		ByteArrayInputStream byte_array_in_stream = new ByteArrayInputStream(datas);
		DataInputStream dis = new DataInputStream(byte_array_in_stream);
		
		byte[] app_socket_header_tag = new byte[Protocol.APP_SOCKET_HEADER_TAG.length];
		dis.readFully(app_socket_header_tag, 0, Protocol.APP_SOCKET_HEADER_TAG.length);
		if (Arrays.equals(Protocol.APP_SOCKET_HEADER_TAG, app_socket_header_tag) == false) {
			throw new IOException("Protocol error with app_socket_header_tag");
		}
		
		int version = dis.readInt();
		if (version != Protocol.VERSION) {
			throw new IOException("Protocol error with version, this = " + Protocol.VERSION + " and dest = " + version);
		}
		
		byte tag = dis.readByte();
		if (tag != 0) {
			throw new IOException("Protocol error, missing 0x0: " + tag);
		}
		
		byte[] header_tag = new byte[HEADER.length];
		dis.readFully(header_tag, 0, HEADER.length);
		if (Arrays.equals(HEADER, header_tag) == false) {
			throw new IOException("Protocol error with header_tag");
		}
		
		tag = dis.readByte();
		if (tag != 0) {
			throw new IOException("Protocol error, missing 0x0: " + tag);
		}
		
		UUID distant = UUID.fromString(readString(dis, "uuid"));
		
		if (manager.getUUIDRef().equals(distant)) {
			log.trace("Datagram is from... me (" + distant + ")");
			return;
		}
		
		if (manager.isConnectedTo(distant)) {
			log.trace("Already connected to " + distant);
			return;
		}
		
		tag = dis.readByte();
		if (tag != 0) {
			throw new IOException("Protocol error, missing 0x0: " + tag);
		}
		
		long date = dis.readLong();
		long now = System.currentTimeMillis();
		if (Math.abs(now - date) > 10000l) {
			log.trace("To big time delta with the distant datagram sender " + distant + " is the " + Loggers.dateLog(date) + ", now it's the " + Loggers.dateLog(now));
			return;
		}
		
		tag = dis.readByte();
		if (tag != 0) {
			throw new IOException("Protocol error, missing 0x0: " + tag);
		}
		
		String json_addrs = readString(dis, "json_addr");
		ArrayList<InetSocketAddress> socket_list = MyDMAM.gson_kit.getGsonSimple().fromJson(json_addrs, GsonKit.type_ArrayList_InetSocketAddr);
		
		socket_list.stream().sorted((l, r) -> {
			boolean r_l = manager.getAddressMaster().isInNetworkRange(l.getAddress());
			boolean r_r = manager.getAddressMaster().isInNetworkRange(r.getAddress());
			if (r_l & r_r) {
				return 0;
			} else if (r_l == false & r_r == false) {
				return 0;
			} else if (r_l & r_r == false) {
				return 1;
			} else {
				return -1;
			}
		}).forEach(addr -> {
			try {
				manager.declareNewPotentialDistantServer(addr, new ConnectionCallback() {
					
					public void onNewConnectedNode(Node node) {
						log.info("Connected to node: " + node + " by net discover");
					}
					
					public void onLocalServerConnection(InetSocketAddress server) {
						log.warn("Can't add server (" + server.getHostString() + "/" + server.getPort() + ") not node list, by net discover");
					}
					
					public void alreadyConnectedNode(Node node) {
					}
				});
			} catch (Exception e) {
				log.error("Can't create node: " + addr + ", by net discover", e);
			}
		});
	}
	
	private static String readString(DataInputStream dis, String name) throws IOException {
		int size = dis.readInt();
		if (size < 1) {
			throw new IOException("Protocol error, can't found \"" + name + "\" (" + size + ")");
		}
		
		byte[] value = new byte[size];
		dis.read(value);
		return new String(value, MyDMAM.UTF8);
	}
	
	private byte[] createDatagram() throws IOException, GeneralSecurityException {
		ByteArrayOutputStream byte_array_out_stream = new ByteArrayOutputStream(Protocol.BUFFER_SIZE);
		
		DataOutputStream dos = new DataOutputStream(byte_array_out_stream);
		dos.write(Protocol.APP_SOCKET_HEADER_TAG);
		dos.writeInt(Protocol.VERSION);
		dos.writeByte(0);
		dos.write(HEADER);
		dos.writeByte(0);
		dos.writeInt(uuid_string.length);
		dos.write(uuid_string);
		dos.writeByte(0);
		dos.writeLong(System.currentTimeMillis());
		dos.writeByte(0);
		dos.writeInt(json_host_public_listened_addrs.length);
		dos.write(json_host_public_listened_addrs);
		dos.flush();
		dos.close();
		
		byte[] datas = byte_array_out_stream.toByteArray();
		return manager.getProtocol().encrypt(datas, 0, datas.length);
	}
	
	
	*/
}
