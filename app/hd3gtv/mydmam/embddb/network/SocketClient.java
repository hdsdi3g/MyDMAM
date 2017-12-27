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
 * Copyright (C) hdsdi3g for hd3g.tv 6 janv. 2017
 * 
*/
package hd3gtv.mydmam.embddb.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

public class SocketClient implements SocketProvider {
	
	private static final Logger log = Logger.getLogger(SocketClient.class);
	
	private final AsynchronousSocketChannel channel;
	private final SocketConnect handler_connect;
	private final Consumer<Node> callback_on_connection;
	private final InetSocketAddress distant_server_addr;
	private final PoolManager pool_manager;
	
	public SocketClient(PoolManager pool_manager, InetSocketAddress server, Consumer<Node> callback_on_connection) throws IOException {
		this.pool_manager = pool_manager;
		if (pool_manager == null) {
			throw new NullPointerException("\"pool_manager\" can't to be null");
		}
		this.distant_server_addr = server;
		if (server == null) {
			throw new NullPointerException("\"server\" can't to be null");
		}
		this.callback_on_connection = callback_on_connection;
		if (callback_on_connection == null) {
			throw new NullPointerException("\"callback_on_connection\" can't to be null");
		}
		
		handler_connect = new SocketConnect();
		
		channel = AsynchronousSocketChannel.open(pool_manager.getChannelGroup());
		// TODO manage pending cxt list...
		channel.connect(server, null, handler_connect);
	}
	
	private SocketClient getThis() {
		return this;
	}
	
	private class SocketConnect implements CompletionHandler<Void, Void> {
		
		public void completed(Void result, Void nothing) {
			try {
				Node new_node = new Node(getThis(), pool_manager, channel);
				log.info("Connected to " + new_node);
				callback_on_connection.accept(new_node);
			} catch (IOException e) {
				log.warn("Can't load node connection to " + distant_server_addr, e);
			}
		}
		
		public void failed(Throwable e, Void nothing) {
			log.warn("Can't create TCP Client to " + distant_server_addr + " " + e.getMessage().trim());
		}
		
	}
	
	InetSocketAddress getDistantServerAddr() {
		return distant_server_addr;
	}
	
	public String getTypeName() {
		return "Lclient>Dserver";
	}
	
	public SocketType getType() {
		return SocketType.CLIENT;
	}
}
