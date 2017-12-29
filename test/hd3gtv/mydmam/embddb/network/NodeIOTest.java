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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.apache.log4j.Logger;
import org.bouncycastle.operator.OperatorCreationException;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.embddb.network.SocketProvider.SocketType;
import hd3gtv.tools.ThreadPoolExecutorFactory;
import junit.framework.TestCase;

public class NodeIOTest extends TestCase {
	
	private static final Logger log = Logger.getLogger(NodeIOTest.class);
	
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
		
		Protocol protocol = new Protocol();
		SSLContext context = protocol.getSSLContext();
		
		byte[] message = "Hello World".getBytes(MyDMAM.UTF8);
		
		executor.execute(() -> {
			try {
				AsynchronousSocketChannel client_channel = AsynchronousSocketChannel.open(channel_group);
				client_channel.connect(server_addr).get(100, TimeUnit.MILLISECONDS);
				
				NodeTest node_alice_client = new NodeTest("AliceClient", protocol, client_channel, SocketType.CLIENT.initSSLEngine(context.createSSLEngine()), executor);
				node_alice_client.syncSend(ByteBuffer.wrap(message), new HandleName("test"), System.currentTimeMillis(), false);
				while (node_alice_client.payload.capacity() == 0) {
					Thread.sleep(1);
				}
				node_alice_client.close();
			} catch (Exception e) {
				e.printStackTrace();
				fail();
			}
		});
		
		AsynchronousSocketChannel server_channel = server.accept().get(100, TimeUnit.MILLISECONDS);
		NodeTest node_bob_server = new NodeTest("BobServer", protocol, server_channel, SocketType.SERVER.initSSLEngine(context.createSSLEngine()), executor);
		node_bob_server.syncSend(ByteBuffer.wrap(message), new HandleName("test"), System.currentTimeMillis(), false);
		while (node_bob_server.payload.capacity() == 0) {
			Thread.sleep(1);
		}
		node_bob_server.close();
		
		channel_group.shutdown();
		channel_group.awaitTermination(100, TimeUnit.MILLISECONDS);
	}
	
	private class NodeTest extends NodeIO {
		
		final String name;
		private volatile ByteBuffer payload;
		
		NodeTest(String name, Protocol protocol, AsynchronousSocketChannel socket_channel, SSLEngine ssl_engine, ThreadPoolExecutorFactory executor) throws IOException {
			super(socket_channel, ssl_engine, executor);
			handshake(protocol.getKeystoreTool());
			
			payload = ByteBuffer.allocate(0);
			
			this.name = name;
			if (name == null) {
				throw new NullPointerException("\"name\" can't to be null");
			}
		}
		
		protected boolean onGetPayload(ByteBuffer payload, HandleName handle_name, long create_date) {
			log.info("On data in " + name + " (" + payload.remaining() + ")");
			assertEquals("test".toLowerCase(), handle_name.name.toLowerCase());
			this.payload = payload;
			assertTrue(create_date + TimeUnit.MINUTES.toMillis(2) > System.currentTimeMillis());
			assertTrue(create_date < System.currentTimeMillis());
			return false;
		}
		
		public void onCloseButChannelWasClosed(ClosedChannelException e) {
			log.error(name, e);
		}
		
		public void onCloseException(IOException e) {
			log.error(name, e);
		}
		
		public void onBeforeSendRawDatas(String request_name, int length, int total_size, CompressionFormat compress_format) {
			log.trace("Will send to " + name + " (" + request_name + ") " + length + "/" + total_size + " bytes in " + compress_format);
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
			log.info("Close is Ok");
		}
		
		public void onAfterSend(Integer size) {
			log.debug("To " + name + ", " + size + " bytes");
		}
		
		public void onAfterReceviedDatas(Integer size) {
			log.trace("From " + name + ", " + size + " bytes");
		}
		
		protected void onCantExtractFrame(IOException e) {
			log.error(name, e);
		}
		
	}
	
	private class NodeTests extends NodeTest {
		
		final List<ByteBuffer> all_recevied_payloads;
		final int payload_count;
		
		NodeTests(int payload_count, String name, Protocol protocol, AsynchronousSocketChannel socket_channel, SSLEngine ssl_engine, ThreadPoolExecutorFactory executor) throws IOException {
			super(name, protocol, socket_channel, ssl_engine, executor);
			this.payload_count = payload_count;
			all_recevied_payloads = Collections.synchronizedList(new ArrayList<>(payload_count));
		}
		
		protected boolean onGetPayload(ByteBuffer payload, HandleName handle_name, long create_date) {
			log.debug("On data in " + name + " (" + payload.remaining() + ")");
			all_recevied_payloads.add(payload);
			assertTrue(create_date + TimeUnit.MINUTES.toMillis(2) > System.currentTimeMillis());
			assertTrue(create_date <= System.currentTimeMillis());
			return isDone() == false;
		}
		
		boolean isDone() {
			return all_recevied_payloads.size() == payload_count;
		}
		
	}
	
	private static byte[] getRandomPayload() {
		int size = ThreadLocalRandom.current().nextInt(0xF, 0xFFFFF);
		byte[] payload = new byte[size];
		ThreadLocalRandom.current().nextBytes(payload);
		return payload;
	}
	
	public void testMultipleIOs() throws IOException, InterruptedException, ExecutionException, TimeoutException, GeneralSecurityException, SecurityException, OperatorCreationException {
		ThreadPoolExecutorFactory executor = createThreadPoolExecutorFactory();
		AsynchronousChannelGroup channel_group = executor.createAsynchronousChannelGroup();
		AsynchronousServerSocketChannel server = createServerChannel(channel_group, new InetSocketAddress("localhost", 0));
		InetSocketAddress server_addr = (InetSocketAddress) server.getLocalAddress();
		
		Protocol protocol = new Protocol();
		SSLContext context = protocol.getSSLContext();
		
		List<ByteBuffer> alice_send_to_bob = IntStream.range(0, ThreadLocalRandom.current().nextInt(10, 100)).parallel().mapToObj(i -> {
			return ByteBuffer.wrap(getRandomPayload());
		}).collect(Collectors.toList());
		
		List<ByteBuffer> bob_send_to_alice = IntStream.range(0, ThreadLocalRandom.current().nextInt(10, 100)).parallel().mapToObj(i -> {
			return ByteBuffer.wrap(getRandomPayload());
		}).collect(Collectors.toList());
		
		ArrayList<ByteBuffer> payloads_recevied_by_alice = new ArrayList<>(bob_send_to_alice.size());
		ArrayList<ByteBuffer> payloads_recevied_by_bob = new ArrayList<>(alice_send_to_bob.size());
		
		executor.execute(() -> {
			try {
				AsynchronousSocketChannel client_channel = AsynchronousSocketChannel.open(channel_group);
				client_channel.connect(server_addr).get(100, TimeUnit.MILLISECONDS);
				
				NodeTests node_alice_client = new NodeTests(bob_send_to_alice.size(), "AliceClient", protocol, client_channel, SocketType.CLIENT.initSSLEngine(context.createSSLEngine()), executor);
				
				alice_send_to_bob.forEach(payload -> {
					try {
						node_alice_client.syncSend(payload, new HandleName("test"), System.currentTimeMillis(), false);
					} catch (IOException | GeneralSecurityException e) {
						throw new RuntimeException(e);
					}
				});
				
				while (node_alice_client.isDone() == false) {
					Thread.sleep(1);
				}
				node_alice_client.close();
				
				payloads_recevied_by_alice.addAll(node_alice_client.all_recevied_payloads);
			} catch (Exception e) {
				e.printStackTrace();
				fail();
			}
		});
		
		AsynchronousSocketChannel server_channel = server.accept().get(100, TimeUnit.MILLISECONDS);
		NodeTests node_bob_server = new NodeTests(alice_send_to_bob.size(), "BobServer", protocol, server_channel, SocketType.SERVER.initSSLEngine(context.createSSLEngine()), executor);
		
		bob_send_to_alice.forEach(payload -> {
			try {
				node_bob_server.syncSend(payload, new HandleName("test"), System.currentTimeMillis(), false);
			} catch (IOException | GeneralSecurityException e) {
				throw new RuntimeException(e);
			}
		});
		
		while (node_bob_server.isDone() == false) {
			Thread.sleep(1);
		}
		node_bob_server.close();
		
		payloads_recevied_by_bob.addAll(node_bob_server.all_recevied_payloads);
		
		channel_group.shutdown();
		channel_group.awaitTermination(100, TimeUnit.MILLISECONDS);
		
		/**
		 * Check payloads: order & integrity
		 */
		assertEquals(alice_send_to_bob.size(), payloads_recevied_by_bob.size());
		assertEquals(bob_send_to_alice.size(), payloads_recevied_by_alice.size());
		
		BiConsumer<ByteBuffer, ByteBuffer> bytebufferEquals = (expected, actual) -> {
			expected.flip();
			assertEquals(expected.remaining(), actual.remaining());
			
			while (expected.hasRemaining()) {
				assertEquals(expected.get(), actual.get());
			}
		};
		
		for (int pos = 0; pos < alice_send_to_bob.size(); pos++) {
			bytebufferEquals.accept(alice_send_to_bob.get(pos), payloads_recevied_by_bob.get(pos));
		}
		for (int pos = 0; pos < bob_send_to_alice.size(); pos++) {
			bytebufferEquals.accept(bob_send_to_alice.get(pos), payloads_recevied_by_alice.get(pos));
		}
		
		log.info("Alice has sent " + alice_send_to_bob.stream().mapToInt(payload -> payload.capacity()).sum() + " bytes to Bob");
		log.info("Bob has sent " + bob_send_to_alice.stream().mapToInt(payload -> payload.capacity()).sum() + " bytes to Alice");
		
	}
	
}
