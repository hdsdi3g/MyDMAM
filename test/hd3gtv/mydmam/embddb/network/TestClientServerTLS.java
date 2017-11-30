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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import hd3gtv.tools.ThreadPoolExecutorFactory;
import junit.framework.TestCase;

public class TestClientServerTLS extends TestCase {
	
	public void testTransfert() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		ThreadPoolExecutorFactory executor = new ThreadPoolExecutorFactory("TestSockets", Thread.NORM_PRIORITY);
		AsynchronousChannelGroup channel_group = executor.createAsynchronousChannelGroup();
		
		AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel.open(channel_group);
		server.setOption(StandardSocketOptions.SO_RCVBUF, 4 * 1024);
		server.setOption(StandardSocketOptions.SO_REUSEADDR, true);
		server.bind(new InetSocketAddress("localhost", 0));
		InetSocketAddress server_addr = (InetSocketAddress) server.getLocalAddress();
		server.accept(null, new ConnectionServerHandler());
		
		AsynchronousSocketChannel client_channel = AsynchronousSocketChannel.open(channel_group);
		client_channel.connect(server_addr, client_channel, new ConnectionClientHandler());
		
		Thread.sleep(10);
		while (client_channel.isOpen()) {
			Thread.sleep(1);
		}
		
		server.close();
		channel_group.shutdown();
		channel_group.awaitTermination(100, TimeUnit.MILLISECONDS);
	}
	
	private class ConnectionClientHandler implements CompletionHandler<Void, AsynchronousSocketChannel> {
		
		public void completed(Void result, AsynchronousSocketChannel channel) {
			// TODO init TLS first
			
			/*byte[] message = "Hello World".getBytes(MyDMAM.UTF8);
			int size = channel.write(ByteBuffer.wrap(message)).get(100, TimeUnit.MILLISECONDS);
			assertEquals(message.length, size);
			
			ByteBuffer bb_message = ByteBuffer.allocate(size);
			size = server_channel.read(bb_message).get(100, TimeUnit.MILLISECONDS);
			assertEquals(message.length, size);
			
			bb_message.flip();
			byte[] result = new byte[size];
			bb_message.get(result);
			assertTrue(Arrays.equals(message, result));
			*/
		}
		
		public void failed(Throwable exc, AsynchronousSocketChannel channel) {
			exc.printStackTrace();
			try {
				channel.close();
			} catch (IOException e) {
			}
		}
		
	}
	
	private class ConnectionServerHandler implements CompletionHandler<AsynchronousSocketChannel, Void> {
		
		public void completed(AsynchronousSocketChannel result, Void attachment) {
			// TODO init TLS after read
		}
		
		public void failed(Throwable exc, Void attachment) {
			exc.printStackTrace();
		}
		
	}
	
}
