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
 * Copyright (C) hdsdi3g for hd3g.tv 30 nov. 2017
 * 
*/
package hd3gtv.mydmam.embddb.network;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import hd3gtv.mydmam.embddb.network.tls.AsyncChannelWrapperSecure;
import hd3gtv.tools.ThreadPoolExecutorFactory;
import junit.framework.TestCase;

public class TestClientServerTLS extends TestCase {
	
	public void testTransfert() throws Exception {
		ThreadPoolExecutorFactory executor = new ThreadPoolExecutorFactory("TestSockets", Thread.NORM_PRIORITY);
		executor.setDisplayThreadCountInThreadNames(true);
		AsynchronousChannelGroup channel_group = executor.createAsynchronousChannelGroup();
		
		KeystoreTool kt_tool = new KeystoreTool(new File("test.jks"), "test", "me");
		SSLContext ssl_context = kt_tool.createTLSContext();
		
		AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel.open(channel_group);
		server.setOption(StandardSocketOptions.SO_RCVBUF, 4 * 1024);
		server.setOption(StandardSocketOptions.SO_REUSEADDR, true);
		server.bind(new InetSocketAddress("localhost", 0));
		InetSocketAddress server_addr = (InetSocketAddress) server.getLocalAddress();
		// new TLSConnectionServerHandler(ssl_context)
		
		AtomicInteger step = new AtomicInteger(0);
		
		/**
		 * SERVER
		 */
		server.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
			
			public void completed(AsynchronousSocketChannel socket_channel, Void attachment) {
				SSLEngine engine = ssl_context.createSSLEngine();
				engine.setUseClientMode(false);
				engine.setNeedClientAuth(true);
				
				AsyncChannelWrapperSecure acws = new AsyncChannelWrapperSecure(socket_channel, engine, (channel_wrapper, data_payload_received_buffer) -> {
					byte[] datas = new byte[data_payload_received_buffer.remaining()];
					data_payload_received_buffer.get(datas);
					System.out.println(new String(datas));
					
					try {
						step.incrementAndGet();
						channel_wrapper.asyncWrite(ByteBuffer.wrap("Client, I am server".getBytes())).get(200, TimeUnit.MILLISECONDS);
						step.incrementAndGet();
						channel_wrapper.asyncWrite(ByteBuffer.wrap("And you ?".getBytes())).get(200, TimeUnit.MILLISECONDS);
					} catch (InterruptedException | ExecutionException | TimeoutException e1) {
						e1.printStackTrace();
					}
					
					try {
						socket_channel.close();
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(0);
					}
					
					return false;
				}, executor);
				
				try {
					acws.asyncHandshake().get(200, TimeUnit.MILLISECONDS);
					kt_tool.checkSecurity(engine);
					
					step.incrementAndGet();
					acws.asyncWrite(ByteBuffer.wrap("Welcome from server".getBytes())).get(200, TimeUnit.MILLISECONDS);
					acws.readNext();
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(0);
				}
			}
			
			public void failed(Throwable exc, Void attachment) {
				exc.printStackTrace();
				System.exit(1);
			}
		});
		
		/**
		 * CLIENT
		 */
		AsynchronousSocketChannel client_channel = AsynchronousSocketChannel.open(channel_group);
		// new TLSConnectionClientHandler(ssl_context)
		client_channel.connect(server_addr, null, new CompletionHandler<Void, Void>() {
			
			public void completed(Void result, Void attachment) {
				SSLEngine engine = ssl_context.createSSLEngine();
				engine.setUseClientMode(true);
				
				AsyncChannelWrapperSecure acws = new AsyncChannelWrapperSecure(client_channel, engine, (channel_wrapper, data_payload_received_buffer) -> {
					byte[] datas = new byte[data_payload_received_buffer.remaining()];
					data_payload_received_buffer.get(datas);
					System.out.println(new String(datas));
					
					if (step.decrementAndGet() == 0) {
						try {
							client_channel.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						return false;
					} else {
						return true;
					}
				}, executor);
				
				try {
					acws.asyncHandshake().get(200, TimeUnit.MILLISECONDS);
					kt_tool.checkSecurity(engine);
					
					acws.asyncWrite(ByteBuffer.wrap("Hello from client".getBytes())).get(200, TimeUnit.MILLISECONDS);
					acws.readNext();
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(0);
				}
			}
			
			public void failed(Throwable exc, Void attachment) {
				exc.printStackTrace();
				System.exit(1);
			}
		});
		
		Thread.sleep(100);
		while (client_channel.isOpen()) {
			Thread.sleep(1);
		}
		
		server.close();
		channel_group.shutdown();
		channel_group.awaitTermination(100, TimeUnit.MILLISECONDS);
	}
	
}
