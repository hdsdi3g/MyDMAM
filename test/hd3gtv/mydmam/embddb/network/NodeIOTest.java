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
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadPendingException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import org.apache.log4j.Logger;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.tools.ThreadPoolExecutorFactory;
import junit.framework.TestCase;

public class NodeIOTest extends TestCase {
	
	private static final Logger log = Logger.getLogger(NodeIOTest.class);
	
	private final Protocol protocol;
	
	public NodeIOTest() throws NoSuchAlgorithmException, NoSuchProviderException, UnsupportedEncodingException {
		protocol = new Protocol("InternalTest");
	}
	
	/*ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
	final KeyPair keyPair = ...
	final Certificate bcCert = new Certificate(new org.spongycastle.asn1.x509.Certificate[] {
	new X509V3CertificateStrategy().selfSignedCertificateHolder(keyPair).toASN1Structure()}); 
	while (true) {
	Socket socket = serverSocket.accept();
	TlsServerProtocol tlsServerProtocol = new TlsServerProtocol(
	socket.getInputStream(), socket.getOutputStream(), secureRandom);
	tlsServerProtocol.accept(new DefaultTlsServer() {
	    protected TlsSignerCredentials getRSASignerCredentials() throws IOException {
	        return tlsSignerCredentials(context);
	    }               
	});      
	new PrintStream(tlsServerProtocol.getOutputStream()).println("Hello TLS");
	}*/
	
	private AsynchronousChannelGroup createAsynchronousChannelGroup() throws IOException {
		ThreadPoolExecutorFactory executor = new ThreadPoolExecutorFactory("TestSockets", Thread.NORM_PRIORITY);
		return executor.createAsynchronousChannelGroup();
	}
	
	private AsynchronousServerSocketChannel createServerChannel(AsynchronousChannelGroup channel_group, InetSocketAddress bind_to) throws IOException {
		AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel.open(channel_group);
		server.setOption(StandardSocketOptions.SO_RCVBUF, 4 * 1024);
		server.setOption(StandardSocketOptions.SO_REUSEADDR, true);
		server.bind(bind_to);
		return server;
	}
	
	public void testCreateChannel() throws IOException, InterruptedException {
		AsynchronousChannelGroup channel_group = createAsynchronousChannelGroup();
		channel_group.shutdown();
		channel_group.awaitTermination(1, TimeUnit.SECONDS);
	}
	
	public void testcreateServerChannel() throws IOException, InterruptedException {
		createServerChannel(createAsynchronousChannelGroup(), new InetSocketAddress("localhost", 0)).close();
	}
	
	public void testOpenClose() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		AsynchronousChannelGroup channel_group = createAsynchronousChannelGroup();
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
		AsynchronousChannelGroup channel_group = createAsynchronousChannelGroup();
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
	
	public void testIO() throws IOException, InterruptedException, ExecutionException, TimeoutException, GeneralSecurityException {
		AsynchronousChannelGroup channel_group = createAsynchronousChannelGroup();
		AsynchronousServerSocketChannel server = createServerChannel(channel_group, new InetSocketAddress("localhost", 0));
		InetSocketAddress server_addr = (InetSocketAddress) server.getLocalAddress();
		
		AtomicBoolean isDone = new AtomicBoolean(false);
		
		BiFunction<DataBlock, Long, Boolean> onGetDataBlock = (block, date) -> {
			isDone.set(true);
			return false;
		};
		
		AsynchronousSocketChannel client_channel = AsynchronousSocketChannel.open(channel_group);
		client_channel.connect(server_addr).get(100, TimeUnit.MILLISECONDS);
		NodeIO node_alice_client = new NodeIO(client_channel, protocol, onGetDataBlock, new Events("AliceClient"));
		NodeIO node_bob_server = new NodeIO(server.accept().get(100, TimeUnit.MILLISECONDS), protocol, onGetDataBlock, new Events("BobServer"));
		node_bob_server.asyncRead();
		
		byte[] message = "Hello World".getBytes(MyDMAM.UTF8);
		node_alice_client.syncSend(ByteBuffer.wrap(message), "test", false);
		
		while (isDone.get() == false) {
			Thread.sleep(1);
		}
		
		node_alice_client.close();
		node_bob_server.close();
		
		channel_group.shutdown();
		channel_group.awaitTermination(100, TimeUnit.MILLISECONDS);
	}
	
	// TODO test with real cipher
	
	/*private class SimpleCompletionHandler implements CompletionHandler<Void, Void> {
		
		private final Runnable onConnected;
		
		SimpleCompletionHandler(Runnable onConnected) {
			this.onConnected = onConnected;
			if (onConnected == null) {
				throw new NullPointerException("\"onConnected\" can't to be null");
			}
		}
		
		public void completed(Void result, Void attachment) {
			onConnected.run();
		}
		
		public void failed(Throwable e, Void attachment) {
			e.printStackTrace();
			System.exit(1);
		}
		
	}*/
	
	private class Events implements NodeIOEvent {
		
		private final String name;
		
		Events(String name) {
			this.name = name;
			if (name == null) {
				throw new NullPointerException("\"name\" can't to be null");
			}
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
		
		public void onReadPendingException(ReadPendingException e) {
			log.error(name, e);
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
		
	}
	
}
