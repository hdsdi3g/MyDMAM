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
 * Copyright (C) hdsdi3g for hd3g.tv 9 août 2017
 * 
*/
package hd3gtv.mydmam.embddb.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.google.common.reflect.TypeToken;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.tools.Hexview;
import hd3gtv.tools.StoppableThread;

public class NetDiscover {
	
	private static Logger log = Logger.getLogger(NetDiscover.class);
	
	/**
	 * NETDISCOVER
	 */
	private static final byte[] HEADER = "NETDISCOVER".getBytes(Protocol.UTF8);
	
	private PoolManager manager;
	
	/**
	 * Listen + send
	 */
	private List<Listener> listeners;
	private ScheduledFuture<?> sch_future;
	private byte[] json_host_public_listened_addrs;
	private byte[] uuid_string;
	
	NetDiscover(PoolManager manager) throws IOException {
		this.manager = manager;
		
		listeners = manager.getAddressMaster().getBroadcastAddresses().map(addr -> {
			return new InetSocketAddress(addr, manager.getProtocol().getDefaultUDPBroadcastPort());
		}).map(socket -> {
			try {
				return new Listener(socket);
			} catch (IOException e) {
				log.error("Can't create socket listener (" + socket + "), " + e.getMessage());
				return null;
			}
		}).filter(listener -> {
			return listener != null;
		}).peek(listener -> {
			listener.start();
		}).collect(Collectors.toList());
		
		if (log.isInfoEnabled()) {
			List<InetSocketAddress> addr = listeners.stream().map(listen -> {
				return listen.addr_to_listen;
			}).collect(Collectors.toList());
			if (addr.size() == 1) {
				log.info("Start node discover via broadcast address " + addr.get(0));
			} else if (addr.size() > 1) {
				log.info("Start node discover via broadcast addresses " + addr);
			}
		}
	}
	
	/**
	 * return json_host_public_listened_addrs;
	 */
	private byte[] refreshListenAddrList() {
		counter_for_regular_refresh_listened_addrs.set(0);
		
		List<InetSocketAddress> addrs = manager.getListenedServerAddress().filter(socket -> {
			return manager.getAddressMaster().isPublicAndPhysicalAddress(socket.getAddress());
		}).collect(Collectors.toList());
		
		if (addrs.isEmpty()) {
			json_host_public_listened_addrs = null;
		} else {
			json_host_public_listened_addrs = MyDMAM.gson_kit.getGsonSimple().toJson(addrs).getBytes(Protocol.UTF8);
		}
		return json_host_public_listened_addrs;
	}
	
	void startRegularSend() {
		refreshListenAddrList();
		uuid_string = manager.getUUIDRef().toString().getBytes(Protocol.UTF8);
		sch_future = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(getPacketSender(), 3, 30, TimeUnit.SECONDS);
	}
	
	private class Listener extends StoppableThread {
		
		DatagramChannel channel_send;
		DatagramChannel channel_rece;
		InetSocketAddress addr_to_listen;
		ByteBuffer buffer;
		
		public Listener(InetSocketAddress addr_to_listen) throws IOException {
			super("Listen on UDP/" + addr_to_listen);
			this.addr_to_listen = addr_to_listen;
			buffer = ByteBuffer.allocate(Protocol.BUFFER_SIZE);
			
			channel_send = DatagramChannel.open();
			channel_send.socket().setBroadcast(true);
			
			channel_rece = DatagramChannel.open();
			DatagramSocket socket_server = channel_rece.socket();
			socket_server.setBroadcast(true);
			socket_server.bind(addr_to_listen);
		}
		
		public void run() {
			while (isWantToRun()) {
				buffer.clear();
				InetSocketAddress sender = null;
				try {
					final InetSocketAddress _sender = (InetSocketAddress) channel_rece.receive(buffer);
					
					if (manager.getAddressMaster().getAddresses().anyMatch(addr -> {
						return addr.equals(_sender.getAddress());
					})) {
						continue;
					}
					
					sender = _sender;
				} catch (IOException e) {
					log.error("Can't listen on UDP/" + addr_to_listen + ", cancel listening", e);
					return;
				}
				
				try {
					int size = buffer.position();
					if (size == 0) {
						continue;
					}
					log.trace("Receive datagram from " + sender.toString() + " (" + size + " bytes)");
					buffer.rewind();
					byte[] raw = new byte[size];
					buffer.get(raw);
					readDatagram(raw);
				} catch (GeneralSecurityException e) {
					log.trace("Can't decode datagram (bad password/corrupted) from" + sender.toString(), e);
				} catch (Exception e) {
					log.warn("Can't read datagram from" + sender.toString(), e);
				}
			}
		}
		
		public void waitToStop() {
			try {
				channel_rece.close();
				channel_send.close();
			} catch (IOException e) {
				log.warn("Can't stop socket " + toString(), e);
			}
			super.waitToStop();
		}
		
		void send(ByteBuffer datagram) throws IOException {
			datagram.rewind();
			channel_send.send(datagram, addr_to_listen);
		}
		
		public String toString() {
			return addr_to_listen.toString();
		}
	}
	
	private final AtomicInteger counter_for_regular_refresh_listened_addrs = new AtomicInteger(0);
	
	public Runnable getPacketSender() {
		return () -> {
			if (counter_for_regular_refresh_listened_addrs.incrementAndGet() == 1) {
				refreshListenAddrList();
			}
			
			if (json_host_public_listened_addrs == null) {
				if (refreshListenAddrList() == null) {
					return;
				}
			}
			
			try {
				ByteBuffer datagram = ByteBuffer.wrap(createDatagram());
				log.trace("Send broadcast datagram (" + datagram.capacity() + " bytes) to " + listeners);
				
				listeners.forEach(listener -> {
					try {
						listener.send(datagram);
					} catch (IOException e) {
						log.error("Can't send broadcast datagram to " + listener, e);
					}
				});
				datagram.clear();
			} catch (IOException | GeneralSecurityException e) {
				log.error("Can't create datagram, cancel net discover", e);
			}
		};
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
		ArrayList<InetSocketAddress> socket_list = MyDMAM.gson_kit.getGsonSimple().fromJson(json_addrs, type_ArrayList_InetSocketAddress);
		
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
	
	public final static Type type_ArrayList_InetSocketAddress = new TypeToken<ArrayList<InetSocketAddress>>() {
	}.getType();
	
	private static String readString(DataInputStream dis, String name) throws IOException {
		int size = dis.readInt();
		if (size < 1) {
			throw new IOException("Protocol error, can't found \"" + name + "\" (" + size + ")");
		}
		
		byte[] value = new byte[size];
		dis.read(value);
		return new String(value, Protocol.UTF8);
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
	
	void close() {
		if (sch_future != null) {
			sch_future.cancel(true);
		}
		
		listeners.forEach(l -> {
			l.waitToStop();
		});
	}
	
}
