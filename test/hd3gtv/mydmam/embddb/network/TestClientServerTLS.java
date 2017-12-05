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
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

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
		server.accept(null, new TLSConnectionServerHandler(ssl_context));
		
		AsynchronousSocketChannel client_channel = AsynchronousSocketChannel.open(channel_group);
		client_channel.connect(server_addr, client_channel, new TLSConnectionClientHandler(ssl_context));
		
		Thread.sleep(100);
		while (client_channel.isOpen()) {
			Thread.sleep(1);
		}
		
		server.close();
		channel_group.shutdown();
		channel_group.awaitTermination(100, TimeUnit.MILLISECONDS);
	}
	
}
