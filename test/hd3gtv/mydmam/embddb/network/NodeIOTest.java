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
 * Copyright (C) hdsdi3g for hd3g.tv 28 nov. 2017
 * 
*/
package hd3gtv.mydmam.embddb.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.apache.log4j.Logger;
import org.bouncycastle.operator.OperatorCreationException;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.tools.ThreadPoolExecutorFactory;
import junit.framework.TestCase;

public class NodeIOTest extends TestCase {
	
	private static final Logger log = Logger.getLogger(NodeIOTest.class);
	
	private final Protocol protocol;
	
	public NodeIOTest() throws SecurityException, OperatorCreationException, GeneralSecurityException, IOException {
		protocol = new Protocol();
	}
	
	private ThreadPoolExecutorFactory createThreadPoolExecutorFactory() throws IOException {
		return new ThreadPoolExecutorFactory("TestSockets", Thread.NORM_PRIORITY);
	}
	
	private AsynchronousServerSocketChannel createServerChannel(AsynchronousChannelGroup channel_group, InetSocketAddress bind_to) throws IOException {
		AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel.open(channel_group);
		server.setOption(StandardSocketOptions.SO_RCVBUF, 4 * 1024);
		server.setOption(StandardSocketOptions.SO_REUSEADDR, true);
		server.bind(bind_to);
		return server;
	}
	
	public void testCreateChannel() throws IOException, InterruptedException {
		AsynchronousChannelGroup channel_group = createThreadPoolExecutorFactory().createAsynchronousChannelGroup();
		channel_group.shutdown();
		channel_group.awaitTermination(1, TimeUnit.SECONDS);
	}
	
	public void testcreateServerChannel() throws IOException, InterruptedException {
		createServerChannel(createThreadPoolExecutorFactory().createAsynchronousChannelGroup(), new InetSocketAddress("localhost", 0)).close();
	}
	
	public void testOpenClose() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		AsynchronousChannelGroup channel_group = createThreadPoolExecutorFactory().createAsynchronousChannelGroup();
		AsynchronousServerSocketChannel server = createServerChannel(channel_group, new InetSocketAddress("localhost", 0));
		InetSocketAddress server_addr = (InetSocketAddress) server.getLocalAddress();
		
		/**
		 * Client connect + disconnect
		 */
		AsynchronousSocketChannel client_channel = AsynchronousSocketChannel.open(channel_group);
		client_channel.connect(server_addr).get(100, TimeUnit.MILLISECONDS);
		client_channel.close();
		
		server.accept().get(100, TimeUnit.MILLISECONDS).close();
		
		channel_group.shutdown();
		channel_group.awaitTermination(100, TimeUnit.MILLISECONDS);
	}
	
	public void testDummyTransfert() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		AsynchronousChannelGroup channel_group = createThreadPoolExecutorFactory().createAsynchronousChannelGroup();
		AsynchronousServerSocketChannel server = createServerChannel(channel_group, new InetSocketAddress("localhost", 0));
		InetSocketAddress server_addr = (InetSocketAddress) server.getLocalAddress();
		
		/**
		 * Client connect + send + receive + disconnect
		 */
		AsynchronousSocketChannel client_channel = AsynchronousSocketChannel.open(channel_group);
		client_channel.connect(server_addr).get(100, TimeUnit.MILLISECONDS);
		byte[] message = "Hello World".getBytes(MyDMAM.UTF8);
		int size = client_channel.write(ByteBuffer.wrap(message)).get(100, TimeUnit.MILLISECONDS);
		assertEquals(message.length, size);
		
		AsynchronousSocketChannel server_channel = server.accept().get(100, TimeUnit.MILLISECONDS);
		ByteBuffer bb_message = ByteBuffer.allocate(size);
		size = server_channel.read(bb_message).get(100, TimeUnit.MILLISECONDS);
		assertEquals(message.length, size);
		
		bb_message.flip();
		byte[] result = new byte[size];
		bb_message.get(result);
		assertTrue(Arrays.equals(message, result));
		
		client_channel.close();
		server_channel.close();
		
		channel_group.shutdown();
		channel_group.awaitTermination(100, TimeUnit.MILLISECONDS);
	}
	
	/*
	 * NOW, test MyDMAM NodeIO
	 * */
	
	public void testIO() throws IOException, InterruptedException, ExecutionException, TimeoutException, GeneralSecurityException, SecurityException, OperatorCreationException {
		ThreadPoolExecutorFactory executor = createThreadPoolExecutorFactory();
		AsynchronousChannelGroup channel_group = executor.createAsynchronousChannelGroup();
		AsynchronousServerSocketChannel server = createServerChannel(channel_group, new InetSocketAddress("localhost", 0));
		InetSocketAddress server_addr = (InetSocketAddress) server.getLocalAddress();
		
		AsynchronousSocketChannel client_channel = AsynchronousSocketChannel.open(channel_group);
		client_channel.connect(server_addr).get(100, TimeUnit.MILLISECONDS);
		
		Protocol protocol = new Protocol();
		SSLContext context = protocol.getSSLContext();
		SSLEngine ssl_engine = context.createSSLEngine();
		
		// TODO create async client...
		NodeTest node_alice_client = new NodeTest("AliceClient", protocol, client_channel, SocketProvider.SocketType.CLIENT.initSSLEngine(ssl_engine), executor);
		
		NodeTest node_bob_server = new NodeTest("BobServer", protocol, server.accept().get(100, TimeUnit.MILLISECONDS), SocketProvider.SocketType.SERVER.initSSLEngine(ssl_engine), executor);
		
		byte[] message = "Hello World".getBytes(MyDMAM.UTF8);
		node_alice_client.syncSend(ByteBuffer.wrap(message), "test", false);
		
		while (node_alice_client.isDone.get() == false | node_bob_server.isDone.get() == false) {
			Thread.sleep(1);
		}
		
		node_alice_client.close();
		node_bob_server.close();
		
		channel_group.shutdown();
		channel_group.awaitTermination(100, TimeUnit.MILLISECONDS);
	}
	
	private class NodeTest extends NodeIO {
		
		private final String name;
		private final AtomicBoolean isDone;
		
		NodeTest(String name, Protocol protocol, AsynchronousSocketChannel socket_channel, SSLEngine ssl_engine, ThreadPoolExecutorFactory executor) throws IOException {
			super(socket_channel, ssl_engine, executor);
			handshake(protocol.getKeystoreTool());
			
			isDone = new AtomicBoolean(false);
			
			this.name = name;
			if (name == null) {
				throw new NullPointerException("\"name\" can't to be null");
			}
		}
		
		protected boolean onGetDataBlock(DataBlock data_block, long create_date) {
			isDone.set(true);
			return false;
		}
		
		public void onCloseButChannelWasClosed(ClosedChannelException e) {
			log.error(name, e);
		}
		
		public void onCloseException(IOException e) {
			log.error(name, e);
		}
		
		public void onBeforeSendRawDatas(String request_name, int length, int total_size, CompressionFormat compress_format) {
			log.info("Will send to " + name + " (" + request_name + ") " + length + "/" + total_size + " bytes in " + compress_format);
		}
		
		public void onRemoveOldStoredDataFrame(long session_id) {
			log.debug(name + " " + session_id);
		}
		
		public void onManualCloseAfterSend() {
			log.debug(name);
		}
		
		public void onIOButClosed(AsynchronousCloseException e) {
			log.error(name, e);
		}
		
		public void onIOExceptionCauseClosing(Throwable e) {
			log.error(name, e);
		}
		
		public void onManualCloseAfterRecevied() {
			log.error(name);
		}
		
		public void onAfterSend(Integer size) {
			log.info("To " + name + ", " + size + " bytes");
		}
		
		public void onAfterReceviedDatas(Integer size) {
			log.info("From " + name + ", " + size + " bytes");
		}
		
		protected void onCantExtractFrame(IOException e) {
			log.error(name, e);
		}
		
	}
	
}
