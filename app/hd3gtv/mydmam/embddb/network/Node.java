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
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadPendingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ssl.SSLEngine;

import org.apache.log4j.Logger;

import com.google.gson.JsonObject;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonIgnore;
import hd3gtv.mydmam.gson.GsonKit;
import hd3gtv.tools.ActivityScheduledAction;
import hd3gtv.tools.PressureMeasurement;
import hd3gtv.tools.TableList;

public class Node extends NodeIO {
	
	private static final Logger log = Logger.getLogger(Node.class);
	
	@GsonIgnore
	private final PoolManager pool_manager;
	
	/**
	 * Can be empty
	 */
	@GsonIgnore // boring...
	private ArrayList<InetSocketAddress> local_server_node_addr;
	
	private UUID uuid_ref;
	private long server_delta_time;
	private final long create_date;
	private InetSocketAddress socket_addr;
	
	@GsonIgnore
	private final SocketProvider provider;
	@GsonIgnore
	private final PressureMeasurement pressure_measurement_sended;
	@GsonIgnore
	private final PressureMeasurement pressure_measurement_recevied;
	private final AtomicLong last_activity;
	
	Node(SocketProvider provider, PoolManager pool_manager, AsynchronousSocketChannel channel) throws IOException {
		super(channel, createSSLEngine(pool_manager, provider), pool_manager.getExecutor());
		
		this.provider = provider;
		if (provider == null) {
			throw new NullPointerException("\"provider\" can't to be null");
		}
		this.pool_manager = pool_manager;
		
		this.pressure_measurement_recevied = pool_manager.getPressureMeasurementRecevied();
		if (pressure_measurement_recevied == null) {
			throw new NullPointerException("\"pressure_measurement_recevied\" can't to be null");
		}
		this.pressure_measurement_sended = pool_manager.getPressureMeasurementSended();
		if (pressure_measurement_sended == null) {
			throw new NullPointerException("\"pressure_measurement_sended\" can't to be null");
		}
		last_activity = new AtomicLong(System.currentTimeMillis());
		
		socket_addr = getRemoteAddress();
		server_delta_time = 0;
		create_date = System.currentTimeMillis();
		
		handshake(pool_manager.getProtocol().getKeystoreTool());
	}
	
	// TODO check same passwords challenge (with hash + random salt)...
	
	private static SSLEngine createSSLEngine(PoolManager pool_manager, SocketProvider provider) {
		return provider.getType().initSSLEngine(pool_manager.getProtocol().getSSLContext().createSSLEngine());
	}
	
	public InetSocketAddress getSocketAddr() {
		if (socket_addr == null) {
			if (provider instanceof SocketClient) {
				socket_addr = ((SocketClient) provider).getDistantServerAddr();
			} else
				socket_addr = getRemoteAddress();
		}
		return socket_addr;
	}
	
	public boolean isOpenSocket() {
		return isOpen();
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
			return getSocketAddr().getHostString() + "/" + getSocketAddr().getPort() + " [" + provider.getTypeName() + "]";
		} else {
			return getSocketAddr().getHostString() + "/" + getSocketAddr().getPort() + " #" + uuid_ref.toString().substring(0, 6) + " [" + provider.getTypeName() + "]";
		}
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
	 * Sync send
	 * @see sendBlock()
	 */
	public <O, T extends RequestHandler<O>> void sendRequest(Class<T> request_class, O options) {
		T request = pool_manager.getAllRequestHandlers().getRequestByClass(request_class);
		if (request == null) {
			throw new NullPointerException("No requests to send");
		}
		request.sendRequest(options, this);
	}
	
	/**
	 * Sync send
	 */
	void sendBlock(DataBlock data, boolean close_channel_after_send) {
		final long start_time = System.currentTimeMillis();
		
		try {
			checkIfOpen();
		} catch (IOException e) {
			throw new RuntimeException("Closed channel for node " + this.toString(), e);
		}
		
		try {
			int total_size = syncSend(data.getFramePayloadContent(), data.getRequestName().name, close_channel_after_send);
			pressure_measurement_sended.onDatas(total_size, System.currentTimeMillis() - start_time);
		} catch (Exception e) {
			log.error("Can't send datas to " + toString() + " > " + data.getRequestName() + ". Closing connection");
			close(getClass());
		}
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
		if (isOpen() == false) {
			throw new IOException("Channel for " + toString() + " is closed");
		}
	}
	
	public void close(Class<?> by) {
		if (log.isDebugEnabled()) {
			log.debug("Want to close node " + toString() + ", asked by " + by.getSimpleName());
		}
		
		close();
		pool_manager.remove(this);
	}
	
	protected void onCloseButChannelWasClosed(ClosedChannelException e) {
		log.debug("Node " + toString() + " was closed: " + e.getMessage());
	}
	
	protected void onCloseException(IOException e) {
		log.warn("Can't close properly channel " + toString(), e);
	}
	
	protected void onBeforeSendRawDatas(String request_name, int length, int total_size, CompressionFormat compress_format) {
		if (log.isTraceEnabled()) {
			log.trace("Send to " + toString() + " \"" + request_name + "\" " + length + " bytes raw, " + total_size + " bytes for real size (compress: " + compress_format + ")");
		}
	}
	
	protected void onReadPendingException(ReadPendingException e) {
		log.warn("No two reads at the same time for " + toString(), e);
	}
	
	protected void onRemoveOldStoredDataFrame(long session_id) {
		if (log.isTraceEnabled()) {
			log.trace("Remove old frame: session id: " + session_id);
		}
	}
	
	protected void onManualCloseAfterSend() {
		log.debug("Manual close socket channel \"" + toString() + "\" after send datas to other node");
	}
	
	protected void onManualCloseAfterRecevied() {
		log.debug("Manual close socket channel \"" + toString() + "\" after recevied datas from the other node");
	}
	
	protected void onIOButClosed(AsynchronousCloseException e) {
		log.debug("Channel \"" + toString() + "\" was closed, so can't do IO.");
	}
	
	protected void onIOExceptionCauseClosing(Throwable e) {
		log.error("Channel \"" + toString() + "\" failed, close socket because " + e.getMessage());
	}
	
	protected void onAfterSend(Integer size) {
		if (log.isTraceEnabled()) {
			log.trace("Sended to \"" + toString() + "\" " + size + " bytes");
		}
	}
	
	protected void onAfterReceviedDatas(Integer size) {
		last_activity.set(System.currentTimeMillis());
		
		if (size < 1) {
			log.warn("Recevied from " + toString() + " an invalid buffer: " + size + " bytes");
		} else if (log.isTraceEnabled()) {
			log.trace("Recevied from " + toString() + " " + size + " bytes");
		}
	}
	
	protected boolean onGetDataBlock(DataBlock block, long create_date) {
		try {
			pool_manager.getAllRequestHandlers().onReceviedNewBlock(block, this);
			pressure_measurement_recevied.onDatas(block.getDataSize(), System.currentTimeMillis() - create_date);
		} catch (IOException e) {
			if (e instanceof WantToCloseLinkException) {
				log.debug("Handler want to close link");
				pressure_measurement_recevied.onDatas(block.getDataSize(), System.currentTimeMillis() - create_date);
				return true;
			} else {
				log.error("Can't extract sended blocks " + toString(), e);
				return true;
			}
		}
		return false;
	}
	
	protected void onCantExtractFrame(IOException e) {
		log.error("Channel \"" + toString() + "\" failed, can't extract data frames " + e.getMessage());
	}
	
}
