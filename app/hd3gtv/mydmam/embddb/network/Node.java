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
 * Copyright (C) hdsdi3g for hd3g.tv 7 janv. 2017
 * 
*/
package hd3gtv.mydmam.embddb.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.ReadPendingException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.google.gson.JsonObject;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.embddb.store.Item;
import hd3gtv.mydmam.gson.GsonIgnore;
import hd3gtv.mydmam.gson.GsonKit;
import hd3gtv.tools.ActivityScheduledAction;
import hd3gtv.tools.PressureMeasurement;
import hd3gtv.tools.TableList;

public class Node {
	
	private static final Logger log = Logger.getLogger(Node.class);
	private static final int SOCKET_BUFFER_SIZE = 0xFFFF;
	private static final long GRACE_PERIOD_TO_KEEP_OLD_RECEVIED_FRAMES = TimeUnit.HOURS.toMillis(8);
	private static final long MAX_RECEVIED_FRAMES_TO_KEEP_BEFORE_DO_GC = 100l;
	
	@GsonIgnore
	private PoolManager pool_manager;
	
	/**
	 * Can be empty
	 */
	@GsonIgnore // boring...
	private ArrayList<InetSocketAddress> local_server_node_addr;
	
	private UUID uuid_ref;
	private long server_delta_time;
	private final long create_date;
	private InetSocketAddress socket_addr;
	private final String provider_type;
	
	@GsonIgnore
	private final SocketProvider provider;
	@GsonIgnore
	private final AsynchronousSocketChannel channel;
	@GsonIgnore
	private final PressureMeasurement pressure_measurement_sended;
	@GsonIgnore
	private final PressureMeasurement pressure_measurement_recevied;
	private final AtomicLong last_activity;
	
	@GsonIgnore
	private final ConcurrentHashMap<Long, FrameContainer> recevied_frames;
	
	@GsonIgnore
	private final SocketHandlerReader handler_reader;
	
	Node(SocketProvider provider, PoolManager pool_manager, AsynchronousSocketChannel channel) {// TODO test me
		this.provider = provider;
		if (provider == null) {
			throw new NullPointerException("\"provider\" can't to be null");
		}
		this.pool_manager = pool_manager;
		if (pool_manager == null) {
			throw new NullPointerException("\"pool_manager\" can't to be null");
		}
		this.channel = channel;
		if (channel == null) {
			throw new NullPointerException("\"channel\" can't to be null");
		}
		
		handler_reader = new SocketHandlerReader(this);
		recevied_frames = new ConcurrentHashMap<>();
		
		this.pressure_measurement_recevied = pool_manager.getPressureMeasurementRecevied();
		if (pressure_measurement_recevied == null) {
			throw new NullPointerException("\"pressure_measurement_recevied\" can't to be null");
		}
		this.pressure_measurement_sended = pool_manager.getPressureMeasurementSended();
		if (pressure_measurement_sended == null) {
			throw new NullPointerException("\"pressure_measurement_sended\" can't to be null");
		}
		last_activity = new AtomicLong(System.currentTimeMillis());
		
		try {
			socket_addr = (InetSocketAddress) channel.getRemoteAddress();
		} catch (IOException e) {
		}
		server_delta_time = 0;
		create_date = System.currentTimeMillis();
		provider_type = provider.getTypeName();
	}
	
	public InetSocketAddress getSocketAddr() {
		if (socket_addr == null) {
			if (provider instanceof SocketClient) {
				socket_addr = ((SocketClient) provider).getDistantServerAddr();
			} else
				try {
					socket_addr = (InetSocketAddress) channel.getRemoteAddress();
				} catch (IOException e) {
					log.debug("Can't get addr", e);
				}
		}
		return socket_addr;
	}
	
	public boolean isOpenSocket() {
		return channel.isOpen();
	}
	
	/**
	 * Plot twist: this current host may be out of time and the node can be at the right date.
	 * @return false the case if you should not communicate with it if you needs date accuracy.
	 */
	public boolean isOutOfTime(long max_delta_time) {
		return Math.abs(server_delta_time) > Math.abs(max_delta_time);
	}
	
	/**
	 * @return can be positive or negative
	 */
	public long getLastDeltaTime() {
		return server_delta_time;
	}
	
	public String toString() {
		if (uuid_ref == null) {
			return getSocketAddr().getHostString() + "/" + getSocketAddr().getPort() + " [" + provider_type + "]";
		} else {
			return getSocketAddr().getHostString() + "/" + getSocketAddr().getPort() + " #" + uuid_ref.toString().substring(0, 6) + " [" + provider_type + "]";
		}
	}
	
	private String thisToString() {
		return toString();
	}
	
	/**
	 * Via SocketAddr and uuid
	 */
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		
		Node other = (Node) obj;
		
		if (this.uuid_ref != null) {
			if (this.uuid_ref.equals(other.uuid_ref) == false) {
				return false;
			}
		}
		
		return this.getSocketAddr().equals(other.getSocketAddr());
	}
	
	void onErrorReturnFromNode(ErrorReturn error) {
		if (error.isDisconnectme()) {
			log.warn("Node (" + error.getNode() + ") say: \"" + error.getMessage() + "\"" + " by " + error.getCaller() + " at " + new Date(error.getDate()) + " and want to disconnect");
			close(getClass());
		} else {
			log.info("Node (" + error.getNode() + ") say: \"" + error.getMessage() + "\"" + " by " + error.getCaller() + " at " + new Date(error.getDate()));
		}
	}
	
	/**
	 * It will add to queue
	 */
	public <O, T extends RequestHandler<O>> void sendRequest(Class<T> request_class, O options) {
		T request = pool_manager.getAllRequestHandlers().getRequestByClass(request_class);
		if (request == null) {
			throw new NullPointerException("No requests to send");
		}
		request.sendRequest(options, this);
	}
	
	ByteBuffer cypher(ByteBuffer source_content) throws IOException, GeneralSecurityException {
		byte[] source_data_to_send = new byte[source_content.remaining()];
		source_content.get(source_data_to_send);
		return ByteBuffer.wrap(pool_manager.getProtocol().encrypt(source_data_to_send, 0, source_data_to_send.length));
	}
	
	ByteBuffer deCypher(ByteBuffer source_content) throws IOException, GeneralSecurityException {
		byte[] received_source_data = new byte[source_content.remaining()];
		source_content.get(received_source_data);
		return ByteBuffer.wrap(pool_manager.getProtocol().decrypt(received_source_data, 0, received_source_data.length));
	}
	
	/*static ByteBuffer ByteBufferJoiner(List<ByteBuffer> items) {
		int size = items.stream().mapToInt(item -> item.remaining()).sum();
		ByteBuffer result = ByteBuffer.allocate(size);
		items.forEach(item -> result.put(item));
		result.flip();
		return result;
	}*/
	
	/**
	 * It will add to queue
	 */
	public void sendBlock(DataBlock data, boolean close_channel_after_send) {
		final long start_time = System.currentTimeMillis();
		
		try {
			checkIfOpen();
		} catch (IOException e) {
			throw new RuntimeException("Closed channel for node " + this.toString(), e);
		}
		
		try {
			ByteBuffer source_to_send = data.getFramePayloadContent();
			
			CompressionFormat compress_format = CompressionFormat.NONE;
			if (source_to_send.remaining() > 0xFFF) {
				compress_format = CompressionFormat.GZIP;
			}
			
			byte[] b_source_to_send = new byte[source_to_send.remaining()];
			source_to_send.get(b_source_to_send);
			byte[] prepared_source_to_send = compress_format.shrink(b_source_to_send);
			
			long session_id = ThreadLocalRandom.current().nextLong();
			
			/** == ceilDiv */
			final int MAX_CHUNK_SIZE = SOCKET_BUFFER_SIZE - FramePayload.HEADER_SIZE;
			int chunk_count = -Math.floorDiv(-prepared_source_to_send.length, MAX_CHUNK_SIZE);
			
			ArrayList<FramePayload> frame_payloads = new ArrayList<>(chunk_count);
			for (int i = 0; i < chunk_count; i++) {
				int pos = i * MAX_CHUNK_SIZE;
				int size = Math.min(prepared_source_to_send.length - pos, MAX_CHUNK_SIZE);
				frame_payloads.add(new FramePayload(session_id, i, prepared_source_to_send, pos, size));
			}
			
			FramePrologue frame_prologue = new FramePrologue(session_id, prepared_source_to_send.length, chunk_count, compress_format);
			FrameEpilogue frame_epilogue = new FrameEpilogue(session_id);
			
			ByteBuffer prologue_to_send = cypher(frame_prologue.output());
			ByteBuffer epilogue_to_send = cypher(frame_epilogue.output());
			
			List<ByteBuffer> frame_payloads_to_send = frame_payloads.stream().map(frame_payload -> {
				try {
					return cypher(frame_payload.output());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}).collect(Collectors.toList());
			
			int total_size = prologue_to_send.remaining() + frame_payloads_to_send.stream().mapToInt(payload_to_send -> payload_to_send.remaining()).sum() + epilogue_to_send.remaining();
			
			if (log.isTraceEnabled()) {
				log.trace("Send to " + toString() + " \"" + data.getRequestName() + "\" " + b_source_to_send.length + " bytes raw, " + total_size + " bytes for real size (compress: " + compress_format + ")");
			}
			
			channel.write(prologue_to_send, new SocketWriter(false), null);
			frame_payloads_to_send.forEach(payload_to_send -> {
				channel.write(payload_to_send, new SocketWriter(false), null);
			});
			channel.write(epilogue_to_send, new SocketWriter(close_channel_after_send), null);
			
			pressure_measurement_sended.onDatas(total_size, System.currentTimeMillis() - start_time);
		} catch (Exception e) {
			log.error("Can't send datas to " + toString() + " > " + data.getRequestName() + ". Closing connection");
			close(getClass());
		}
	}
	
	/**
	 * It will add to queue
	 */
	public void sendBlock(DataBlock to_send) {
		sendBlock(to_send, false);
	}
	
	void setUUIDRef(UUID uuid) throws IOException {
		if (uuid == null) {
			throw new NullPointerException("\"uuid_ref\" can't to be null");
		}
		if (uuid.equals(pool_manager.getUUIDRef())) {
			throw new IOException("Invalid UUID for " + toString() + ", it's the same as local manager ! (" + uuid_ref.toString() + ")");
		}
		if (uuid_ref == null) {
			uuid_ref = uuid;
		}
		check(uuid);
	}
	
	void check(UUID uuid) throws IOException {
		if (this.uuid_ref == null) {
			return;
		}
		if (uuid.equals(uuid_ref) == false) {
			throw new IOException("Invalid UUID for " + toString() + ", you should disconnect now (this = " + uuid_ref.toString() + " and dest = " + uuid.toString() + ")");
		}
		if (uuid.equals(pool_manager.getUUIDRef())) {
			throw new IOException("Invalid UUID for " + toString() + ", it's the same as local manager ! (" + uuid_ref.toString() + ")");
		}
		Node n = pool_manager.get(uuid);
		if (n == null) {
			throw new IOException("This node " + toString() + " was not declared in node_list");
		} else if (equals(n) == false) {
			throw new IOException("Another node (" + n.toString() + ") was previousely add with this UUID, " + toString());
		}
	}
	
	public boolean isUUIDSet() {
		return uuid_ref != null;
	}
	
	public boolean equalsThisUUID(UUID uuid) {
		if (uuid == null) {
			return false;
		}
		if (uuid_ref == null) {
			return false;
		}
		return uuid.equals(uuid_ref);
	}
	
	public boolean equalsThisUUID(Node node) {
		if (node == null) {
			return false;
		}
		if (uuid_ref == null) {
			return false;
		}
		return node.uuid_ref.equals(uuid_ref);
	}
	
	/**
	 * Use equalsThisUUID, isUUIDSet or check for compare.
	 * @return Maybe null.
	 */
	public UUID getUUID() {
		return uuid_ref;
	}
	
	void setLocalServerNodeAddresses(ArrayList<InetSocketAddress> local_server_node_addr) {
		this.local_server_node_addr = local_server_node_addr;
		if (local_server_node_addr == null) {
			throw new NullPointerException("\"local_server_node_addr\" can't to be null");
		}
		if (local_server_node_addr.isEmpty()) {
			log.info("Node " + toString() + " hasn't sockets addresses for its local server (client only node, maybe a console)");
		} else {
			log.debug("Node " + toString() + " has some sockets addresses for its local server: " + local_server_node_addr);
		}
	}
	
	/**
	 * @return null if not uuid or closed
	 */
	JsonObject getAutodiscoverIDCard() {
		if (uuid_ref == null | isOpenSocket() == false) {
			return null;
		}
		if (local_server_node_addr == null) {
			return null;
		}
		if (local_server_node_addr.isEmpty()) {
			return null;
		}
		JsonObject jo = new JsonObject();
		jo.addProperty("uuid", uuid_ref.toString());
		jo.add("server_addr", MyDMAM.gson_kit.getGsonSimple().toJsonTree(local_server_node_addr));
		return jo;
	}
	
	static UUID getUUIDFromAutodiscoverIDCard(JsonObject item) throws NullPointerException {
		if (item.has("uuid")) {
			return UUID.fromString(item.get("uuid").getAsString());
		}
		throw new NullPointerException("Missing uuid item in json " + item.toString());
	}
	
	static ArrayList<InetSocketAddress> getAddressFromAutodiscoverIDCard(PoolManager pool_manager, JsonObject item) throws NullPointerException, UnknownHostException {
		if (item.has("server_addr") == false) {
			throw new NullPointerException("Missing addr/port items in json " + item.toString());
		}
		return MyDMAM.gson_kit.getGsonSimple().fromJson(item.get("server_addr"), GsonKit.type_ArrayList_InetSocketAddr);
	}
	
	void setDistantDate(long server_date) {
		long new_delay = server_date - System.currentTimeMillis();
		
		if (log.isTraceEnabled()) {
			log.trace("Node " + toString() + " delay: " + server_delta_time + " ms before, now is " + new_delay + " ms");
		}
		
		server_delta_time = new_delay;
	}
	
	/**
	 * Console usage.
	 */
	void addToActualStatus(TableList table) {
		String host = getSocketAddr().getHostString();
		if (getSocketAddr().getPort() != pool_manager.getProtocol().getDefaultTCPPort()) {
			host = host + "/" + getSocketAddr().getPort();
		}
		String provider = this.provider.getClass().getSimpleName();
		String isopen = "open";
		if (isOpenSocket() == false) {
			isopen = "CLOSE";
		}
		String deltatime = server_delta_time + " ms";
		String uuid = "<no uuid>";
		if (uuid_ref != null) {
			uuid = uuid_ref.toString();
		}
		
		String _create_date = Loggers.dateLog(create_date);
		
		table.addRow(host, provider, isopen, deltatime, uuid, _create_date);
	}
	
	ActivityScheduledAction<Node> getScheduledAction() {
		Node current_node = this;
		return new ActivityScheduledAction<Node>() {
			
			public String getScheduledActionName() {
				return "Check if the socket is open and do pings";
			}
			
			public boolean onScheduledActionError(Exception e) {
				log.warn("Can't execute node scheduled actions");
				pool_manager.remove(current_node);
				return false;
			}
			
			public TimeUnit getScheduledActionPeriodUnit() {
				return TimeUnit.SECONDS;
			}
			
			public long getScheduledActionPeriod() {
				return 60;
			}
			
			public long getScheduledActionInitialDelay() {
				return 10;
			}
			
			public Runnable getRegularScheduledAction() {
				return () -> {
					try {
						current_node.checkIfOpen();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					pool_manager.getAllRequestHandlers().getRequestByClass(RequestPoke.class).sendRequest(null, current_node);
				};
			}
		};
	}
	
	public long getLastActivityDate() {
		return last_activity.get();
	}
	
	private void checkIfOpen() throws IOException {
		if (channel.isOpen() == false) {
			throw new IOException("Channel for " + toString() + " is closed");
		}
	}
	
	void asyncRead() {
		try {
			ByteBuffer read_buffer = ByteBuffer.allocateDirect(SOCKET_BUFFER_SIZE);
			channel.read(read_buffer, read_buffer, handler_reader);
		} catch (ReadPendingException e) {
			log.warn("No two reads at the same time for " + toString(), e);
		}
	}
	
	public void close(Class<?> by) {
		if (log.isDebugEnabled()) {
			log.debug("Want to close node " + toString() + ", asked by " + by.getSimpleName());
		}
		
		if (channel.isOpen()) {
			try {
				channel.shutdownInput();
			} catch (Exception e) {
				log.debug("Can't shutdown input reader: " + e.getMessage());
			}
			try {
				channel.shutdownOutput();
			} catch (Exception e) {
				log.debug("Can't shutdown output reader: " + e.getMessage());
			}
			
			try {
				channel.close();
			} catch (ClosedChannelException e) {
				log.debug("Node was closed: " + e.getMessage());
			} catch (IOException e) {
				log.warn("Can't close properly channel " + toString(), e);
			}
		}
		pool_manager.remove(this);
	}
	
	/**
	 * @return channel.hashCode
	 */
	public int hashCode() {
		return channel.hashCode();
	}
	
	private class SocketHandlerReader implements CompletionHandler<Integer, ByteBuffer> {
		
		private Node node;
		
		SocketHandlerReader(Node node) {
			this.node = node;
		}
		
		public void completed(Integer size, ByteBuffer read_buffer) {
			if (size == -1) {
				return;
			} else if (size < 1) {
				log.debug("Get empty datas from " + node.toString() + ", size = " + size);
				return;
			}
			
			if (log.isTraceEnabled()) {
				log.trace("Recevied from " + node + " " + size + " bytes");
			}
			
			if (node.isOpenSocket()) {
				try {
					final long start_time = System.currentTimeMillis();
					last_activity.set(start_time);
					
					read_buffer.flip();
					
					ByteBuffer sended_content = deCypher(read_buffer);
					
					if (sended_content.remaining() < Protocol.FRAME_HEADER_SIZE) {
						throw new IOException("Invalid header remaining size: " + sended_content.remaining());
					}
					Item.readAndEquals(sended_content, Protocol.APP_EMBDDB_SOCKET_HEADER_TAG, b -> {
						return new IOException("Protocol error with app_socket_header_tag");
					});
					Item.readByteAndEquals(sended_content, Protocol.VERSION, version -> {
						return new IOException("Protocol error with version, this = " + Protocol.VERSION + " and dest = " + version);
					});
					int frame_type = sended_content.get();
					sended_content.position(0);
					
					if (frame_type == Protocol.FRAME_TYPE_PROLOGUE) {
						FramePrologue prologue = new FramePrologue(sended_content);
						recevied_frames.putIfAbsent(prologue.session_id, new FrameContainer(prologue));
						
						if (recevied_frames.mappingCount() > MAX_RECEVIED_FRAMES_TO_KEEP_BEFORE_DO_GC) {
							List<Long> delete_list_too_old = recevied_frames.reduceEntries(1, entry -> {
								Long key = entry.getKey();
								FramePrologue p = entry.getValue().prologue;
								
								if (p.create_date + GRACE_PERIOD_TO_KEEP_OLD_RECEVIED_FRAMES < System.currentTimeMillis()) {
									return Arrays.asList(key);
								}
								return new ArrayList<Long>(0);
							}, (l, r) -> {
								int l_size = l.size();
								int r_size = r.size();
								
								if (l_size + r_size == 0) {
									return Collections.emptyList();
								}
								List<Long> result = new ArrayList<Long>();
								result.addAll(l);
								result.addAll(r);
								return result;
							});
							
							delete_list_too_old.forEach(session_id -> {
								recevied_frames.remove(session_id);
								if (log.isTraceEnabled()) {
									log.trace("Remove old frame: session id: " + session_id);
								}
							});
						}
					} else if (frame_type == Protocol.FRAME_TYPE_PAYLOAD) {
						FramePayload frame_payload = new FramePayload(sended_content);
						FrameContainer f_container = recevied_frames.get(frame_payload.session_id);
						if (f_container == null) {
							throw new IOException("Can't found frame with session id: " + frame_payload.session_id);
						}
						f_container.appendPayload(frame_payload);
						
					} else if (frame_type == Protocol.FRAME_TYPE_EPILOGUE) {
						FrameEpilogue epilogue = new FrameEpilogue(sended_content);
						FrameContainer old_f_container = recevied_frames.remove(epilogue.session_id);
						if (old_f_container == null) {
							throw new IOException("Can't found frame with session id: " + epilogue.session_id);
						}
						
						ByteBuffer raw_datas = old_f_container.close();
						if (raw_datas == null) {
							node.asyncRead();
							return;
						}
						
						DataBlock block = new DataBlock(raw_datas);
						try {
							pool_manager.getAllRequestHandlers().onReceviedNewBlock(block, node);
							pressure_measurement_recevied.onDatas(block.getDataSize(), System.currentTimeMillis() - old_f_container.prologue.create_date);
						} catch (IOException e) {
							if (e instanceof WantToCloseLinkException) {
								log.debug("Handler want to close link");
								close(getClass());
								pressure_measurement_recevied.onDatas(block.getDataSize(), System.currentTimeMillis() - old_f_container.prologue.create_date);
							} else {
								log.error("Can't extract sended blocks " + toString(), e);
								close(getClass());
							}
							return;
						}
					} else {
						throw new IOException("Invalid header, unknown frame_type: " + frame_type);
					}
				} catch (Exception e) {
					failed(e, read_buffer);
				}
				
				if (node.isOpenSocket()) {
					node.asyncRead();
				}
			}
		}
		
		public void failed(Throwable e, ByteBuffer buffer) {
			if (e instanceof AsynchronousCloseException) {
				log.debug("Channel " + node + " was closed, so can't close it.");
			} else {
				log.error("Channel " + node + " failed, close socket because " + e.getMessage());
				node.close(getClass());
			}
		}
		
	}
	
	private class SocketWriter implements CompletionHandler<Integer, Void> {
		
		final boolean close_channel_after_send;
		
		SocketWriter(boolean close_channel_after_send) {
			this.close_channel_after_send = close_channel_after_send;
		}
		
		public void completed(Integer size, Void v) {
			if (size == -1) {
				return;
			}
			if (log.isTraceEnabled()) {
				log.trace("Sended to " + thisToString() + " " + size + " bytes");
			}
			if (close_channel_after_send) {
				log.debug("Manual close socket after send datas to other node " + thisToString());
				close(getClass());
			}
		}
		
		public void failed(Throwable e, Void v) {
			if (e instanceof AsynchronousCloseException) {
				log.debug("Channel " + thisToString() + " was closed, so can't close it.");
			} else {
				log.error("Channel " + thisToString() + " failed, close socket because " + e.getMessage());
				close(getClass());
			}
		}
		
	}
	
}
