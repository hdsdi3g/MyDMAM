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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
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
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonIgnore;
import hd3gtv.tools.Hexview;
import hd3gtv.tools.PressureMeasurement;

/**
 * Detect and find instances via multicast groups.
 */
class NetDiscover {
	
	private static final Logger log = Logger.getLogger(NetDiscover.class);
	
	@GsonIgnore
	private final PoolManager pool_manager;
	
	private final ArrayList<InetSocketAddress> declared_groups;
	private List<Engine> engines;
	
	@GsonIgnore
	private ScheduledFuture<?> sch_future;
	@GsonIgnore
	private transient byte[] datagram_to_send;
	
	@GsonIgnore
	private final String hashed_password_key;
	@GsonIgnore
	private final UUID uuid;
	@GsonIgnore
	private final PressureMeasurement pressure_measurement;
	
	NetDiscover(PoolManager pool_manager, List<InetSocketAddress> multicast_groups, PressureMeasurement pressure_measurement) {
		this.pool_manager = pool_manager;
		if (pool_manager == null) {
			throw new NullPointerException("\"pool_manager\" can't to be null");
		}
		if (multicast_groups == null) {
			throw new NullPointerException("\"multicast_groups\" can't to be null");
		}
		this.pressure_measurement = pressure_measurement;
		if (pressure_measurement == null) {
			throw new NullPointerException("\"pressure_measurement\" can't to be null");
		}
		
		declared_groups = new ArrayList<>(multicast_groups);
		if (declared_groups.isEmpty()) {
			Protocol protocol = pool_manager.getProtocol();
			declared_groups.add(new InetSocketAddress(protocol.getDefaulMulticastIPv4Addr(), protocol.getDefaultUDPMulticastPort()));
			declared_groups.add(new InetSocketAddress(protocol.getDefaulMulticastIPv6Addr(), protocol.getDefaultUDPMulticastPort()));
		}
		
		engines = pool_manager.getAddressMaster().getAllPhysicalInterfaces().map(physicalnetwork_interface -> {
			return new Engine(physicalnetwork_interface);
		}).collect(Collectors.toList());
		
		hashed_password_key = pool_manager.getProtocol().getHashedPasswordKey();
		uuid = pool_manager.getUUIDRef();
		
		updatePayload();
	}
	
	private static class Payload {
		/**
		 * uuid
		 */
		UUID u;
		/**
		 * hashed_password_key
		 */
		String hk;
		/**
		 * host_public_listened_addrs
		 */
		ArrayList<InetSocketAddress> a;
		
		transient PoolManager pool_manager;
		transient String ref_hashed_password_key;
		transient UUID ref_uuid;
		transient PressureMeasurement pressure_measurement;
		
		void asyncProcessor(InetAddress source) {
			pool_manager.executeInThePool(() -> {
				long start_time = System.currentTimeMillis();
				try {
					if (hk == null) {
						throw new NullPointerException("\"hk\" can't to be null");
					}
					if (u == null) {
						throw new NullPointerException("\"u\" can't to be null");
					}
					if (a == null) {
						throw new NullPointerException("\"a\" can't to be null");
					}
					
					/**
					 * Test hashed_password_key
					 */
					if (hk.equals(ref_hashed_password_key) == false) {
						if (log.isDebugEnabled()) {
							log.debug("This payload as not the same password from " + source + ", payload hash: " + hk + ", instance hash: " + ref_hashed_password_key);
						}
					}
					
					/**
					 * Test UUID
					 */
					if (u.equals(ref_uuid)) {
						/**
						 * It's me !
						 */
						if (log.isTraceEnabled()) {
							log.trace("I have just to get payload from me...");
						}
						return;
					}
					if (pool_manager.isConnectedTo(u)) {
						/**
						 * It was connected.
						 */
						if (log.isTraceEnabled()) {
							log.trace("I have just to get payload from an actual connected node from " + source + " " + u);
						}
						return;
					}
					
					a.stream().sorted((l, r) -> {
						boolean r_l = pool_manager.getAddressMaster().isInNetworkRange(l.getAddress());
						boolean r_r = pool_manager.getAddressMaster().isInNetworkRange(r.getAddress());
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
							pool_manager.declareNewPotentialDistantServer(addr, new ConnectionCallback() {
								
								public void onNewConnectedNode(Node node) {
									log.info("Connected to node: " + node);
								}
								
								public void onLocalServerConnection(InetSocketAddress server) {
									log.warn("Can't add server (" + server.getHostString() + "/" + server.getPort() + ") not node list");
								}
								
								public void alreadyConnectedNode(Node node) {
								}
							});
						} catch (Exception e) {
							log.error("Can't create node: " + addr, e);
						}
					});
					
					pressure_measurement.onProcess(System.currentTimeMillis() - start_time);
				} catch (Exception e) {
					if (log.isTraceEnabled()) {
						log.trace("Invalid payload format from " + source, e);
					}
				}
			});
		}
	}
	
	// TODO split net hw and public ip addr (to all for all)
	
	void updatePayload() {
		try {
			List<InetSocketAddress> addrs = pool_manager.getListenedServerAddress().filter(socket -> {
				return pool_manager.getAddressMaster().isPublicAndPhysicalAddress(socket.getAddress());
			}).collect(Collectors.toList());
			if (addrs.isEmpty()) {
				datagram_to_send = null;
				return;
			}
			
			Payload payload = new Payload();
			payload.u = uuid;
			payload.hk = hashed_password_key;
			payload.a = new ArrayList<>(addrs);
			
			byte[] json_byted_payload = MyDMAM.gson_kit.getGsonSimple().toJson(payload).getBytes(MyDMAM.UTF8);
			
			ByteArrayOutputStream byte_array_out_stream = new ByteArrayOutputStream(Protocol.BUFFER_SIZE);
			DataOutputStream dos = new DataOutputStream(byte_array_out_stream);
			
			dos.write(Protocol.APP_NETDISCOVER_SOCKET_HEADER_TAG);
			dos.writeInt(Protocol.VERSION);
			dos.writeByte(0);
			dos.writeInt(json_byted_payload.length);
			dos.write(json_byted_payload);
			dos.flush();
			
			datagram_to_send = byte_array_out_stream.toByteArray();
			
			if (log.isTraceEnabled()) {
				log.trace("Create Payload" + Hexview.LINESEPARATOR + Hexview.tracelog(datagram_to_send));
			}
			
		} catch (Exception e) {
			log.error("Can't create Payload", e);
			datagram_to_send = null;
		}
	}
	
	private void logBadPayload(byte[] content, InetAddress source, String reason) {
		if (log.isTraceEnabled() == false) {
			return;
		}
		
		log.trace(reason + " from " + source + ": " + Hexview.LINESEPARATOR + Hexview.tracelog(content));
	}
	
	private void readPayload(InetAddress source, byte[] content) {
		int protocol_head_tag_len = Protocol.APP_NETDISCOVER_SOCKET_HEADER_TAG.length;
		if (content.length < protocol_head_tag_len + 1) {
			logBadPayload(content, source, "Invalid datagram (too small)");
			return;
		}
		
		for (int pos = 0; pos < protocol_head_tag_len; pos++) {
			if (Protocol.APP_NETDISCOVER_SOCKET_HEADER_TAG[pos] != content[pos]) {
				logBadPayload(content, source, "Invalid datagram (bad header)");
				return;
			}
		}
		
		ByteBuffer byte_buffer = ByteBuffer.wrap(content, protocol_head_tag_len, content.length - protocol_head_tag_len);
		
		if (byte_buffer.getInt() != Protocol.VERSION) {
			logBadPayload(content, source, "Invalid datagram (bad version)");
			return;
		}
		
		if (byte_buffer.get() != 0) {
			logBadPayload(content, source, "Invalid datagram (bad first 0)");
			return;
		}
		
		int payload_size = byte_buffer.getInt();
		if (payload_size < 1) {
			logBadPayload(content, source, "Invalid datagram (bad payload size: " + payload_size + ")");
			return;
		}
		
		byte[] json_byted_payload = new byte[payload_size];
		byte_buffer.get(json_byted_payload);
		
		try {
			Payload payload = MyDMAM.gson_kit.getGsonSimple().fromJson(new String(json_byted_payload), Payload.class);
			payload.pool_manager = pool_manager;
			payload.ref_hashed_password_key = hashed_password_key;
			payload.ref_uuid = uuid;
			payload.pressure_measurement = pressure_measurement;
			payload.asyncProcessor(source);
		} catch (Exception e) {
			if (log.isTraceEnabled()) {
				log.warn("Error during payload import from " + source + ": " + Hexview.LINESEPARATOR + Hexview.tracelog(content), e);
			} else {
				log.warn("Error during payload import from " + source, e);
			}
		}
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
		
		sch_future = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(regular_send, 1, 30, TimeUnit.SECONDS);
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
			@GsonIgnore
			Thread receiver;
			@GsonIgnore
			DatagramChannel channel;
			@GsonIgnore
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
					long start_time;
					int buffer_size;
					try {
						while (channel.isOpen()) {
							sender = (InetSocketAddress) channel.receive(buffer);
							start_time = System.currentTimeMillis();
							buffer.flip();
							buffer_size = buffer.remaining();
							if (buffer_size == 0) {
								log.warn("Receive invalid datagram (" + buffer_size + " bytes) from " + sender.getHostString() + " on " + toString());
							} else {
								try {
									if (log.isTraceEnabled()) {
										log.trace("Receive datagram (" + buffer_size + " bytes) from " + sender.getHostString() + " on " + toString());
									}
									byte[] content = new byte[buffer_size];
									buffer.get(content);
									readPayload(sender.getAddress(), content);
									pressure_measurement.onDatas(buffer_size, System.currentTimeMillis() - start_time);
								} catch (Exception e) {
									log.error("Trouble payload processing from " + sender.getHostString() + " on " + toString(), e);
								}
							}
							buffer.clear();
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
	
	@GsonIgnore
	private Runnable regular_send = () -> {
		if (datagram_to_send == null) {
			return;
		}
		engines.stream().flatMap(engine -> {
			return engine.groups.stream();
		}).filter(group -> {
			return group.channel != null;
		}).filter(group -> {
			return group.channel.isOpen();
		}).forEach(group -> {
			try {
				ByteBuffer to_send = ByteBuffer.wrap(datagram_to_send);
				int size_sended = group.channel.send(to_send, group.group_socket);
				if (size_sended == 0) {
					throw new IOException("Send empty packet");
				}
				if (log.isTraceEnabled()) {
					log.trace("Send datagram (" + size_sended + " bytes) to " + group.toString());
				}
			} catch (IOException e) {
				log.warn("Can't send datagram to " + group.toString(), e);
			}
		});
	};
	
}
