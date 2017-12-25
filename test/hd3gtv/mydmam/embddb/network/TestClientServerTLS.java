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
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import hd3gtv.mydmam.embddb.network.tls.AsyncChannelWrapperSecure;
import hd3gtv.mydmam.embddb.network.tls.ReceiveDataEvent;
import hd3gtv.tools.ThreadPoolExecutorFactory;
import junit.framework.TestCase;

public class TestClientServerTLS extends TestCase {
	
	private KeystoreTool kt_tool;
	private SSLContext ssl_context;
	private ThreadPoolExecutorFactory executor;
	
	public TestClientServerTLS() throws Exception {
		kt_tool = new KeystoreTool(new File("test.jks"), "test", "me");
		ssl_context = kt_tool.createTLSContext();
		executor = new ThreadPoolExecutorFactory("TestSockets", Thread.NORM_PRIORITY);
		executor.setDisplayThreadCountInThreadNames(true);
		// executor.setSimplePoolSize() TODO test with it
	}
	
	public void testTransfert() throws Exception {
		AsynchronousChannelGroup channel_group = executor.createAsynchronousChannelGroup();
		
		AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel.open(channel_group);
		server.setOption(StandardSocketOptions.SO_RCVBUF, 4 * 1024);
		server.setOption(StandardSocketOptions.SO_REUSEADDR, true);
		server.bind(new InetSocketAddress("localhost", 0));
		InetSocketAddress server_addr = (InetSocketAddress) server.getLocalAddress();
		// new TLSConnectionServerHandler(ssl_context)
		ServerConnectionHandler server_hander = new ServerConnectionHandler();
		server.accept(null, server_hander);
		
		AsynchronousSocketChannel client_channel = AsynchronousSocketChannel.open(channel_group);
		// new TLSConnectionClientHandler(ssl_context)
		ClientConnectionHandler client_handler = new ClientConnectionHandler();
		client_channel.connect(server_addr, client_channel, client_handler);
		
		Thread.sleep(100);
		while (client_handler.allRecevied() == false | server_hander.allRecevied() == false) {
			Thread.sleep(10);
		}
		
		client_handler.checksRecevied(server_hander);
		server_hander.checksRecevied(client_handler);
		
		client_channel.close();
		server.close();
		channel_group.shutdown();
		channel_group.awaitTermination(100, TimeUnit.MILLISECONDS);
	}
	
	private static byte[] getRandomPayload() {
		int size = ThreadLocalRandom.current().nextInt(10, AsyncChannelWrapperSecure.MAX_PAYLOAD_SIZE);
		byte[] payload = new byte[size];
		ThreadLocalRandom.current().nextBytes(payload);
		return payload;
	}
	
	private static final byte[] END_TOKEN = "this is the end of stream".getBytes();
	
	class BulkDataEngine implements ReceiveDataEvent {
		AsyncChannelWrapperSecure acws;
		IOHistory io_history;
		volatile boolean all_recevied = false;
		
		BulkDataEngine(AsynchronousSocketChannel socket_channel, SSLEngine engine, IOHistory io_history) {
			this.io_history = io_history;
			acws = new AsyncChannelWrapperSecure(socket_channel, engine, this, executor);
			
			try {
				acws.asyncHandshake().get(200, TimeUnit.MILLISECONDS);
				kt_tool.checkSecurity(engine);
				
				int loop = 2;/* ThreadLocalRandom.current().nextInt(5, 50);*/ // XXX
				
				for (int pos = 0; pos < loop; pos++) {
					byte[] payload = getRandomPayload();
					System.out.println("send " + payload.length + " to " + socket_channel.getRemoteAddress());
					io_history.sended.add(payload);
					int size = acws.asyncWrite(ByteBuffer.wrap(payload)).get(200, TimeUnit.MILLISECONDS);
					assertEquals(size, payload.length);
				}
				
				System.out.println("send " + END_TOKEN.length + " to " + socket_channel.getRemoteAddress());
				int size = acws.asyncWrite(ByteBuffer.wrap(END_TOKEN)).get(200, TimeUnit.MILLISECONDS);
				assertEquals(size, END_TOKEN.length);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		public boolean onGetDatas(AsyncChannelWrapperSecure channel_wrapper, ByteBuffer data_payload_received_buffer) {
			byte[] datas = new byte[data_payload_received_buffer.remaining()];
			data_payload_received_buffer.get(datas);
			
			SocketAddress local_addr = null;
			try {
				local_addr = channel_wrapper.getChannel().getLocalAddress();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			System.out.println("recevie " + datas.length + " from " + local_addr);
			
			if (datas.length >= END_TOKEN.length) {
				boolean done = false;
				for (int pos = 0; pos < END_TOKEN.length; pos++) {
					byte token = END_TOKEN[pos];
					byte raw = datas[datas.length - END_TOKEN.length + pos];
					
					if (token == raw) {
						done = true;
					} else {
						done = false;
						break;
					}
				}
				
				if (done) {
					System.out.println("Done for " + local_addr);
					all_recevied = true;
					return false;
				} /*else {
					System.err.println("recevie bad end \"" + Hexview.tracelog(datas, datas.length - END_TOKEN.length, datas.length) + "\" from " + local_addr);
					}*/
			}
			
			io_history.recevied.add(datas);
			
			/*try {
				channel_wrapper.asyncWrite(ByteBuffer.wrap("Client, I am server".getBytes())).get(200, TimeUnit.MILLISECONDS);
				channel_wrapper.asyncWrite(ByteBuffer.wrap("And you ?".getBytes())).get(200, TimeUnit.MILLISECONDS);
			} catch (InterruptedException | ExecutionException | TimeoutException e1) {
				e1.printStackTrace();
			}*/
			return true;
		}
		
	}
	
	abstract class IOHistory {
		List<byte[]> sended = Collections.synchronizedList(new ArrayList<>());
		List<byte[]> recevied = Collections.synchronizedList(new ArrayList<>());
		
		void checksRecevied(IOHistory sender) {
			int total_recevied = recevied.stream().mapToInt(buffer -> buffer.length).sum();
			int total_sended = sender.sended.stream().mapToInt(buffer -> buffer.length).sum();
			assertEquals(total_sended, total_recevied);
			
			Function<byte[], Stream<Byte>> explode_lists = buffer -> {
				ArrayList<Byte> result = new ArrayList<>(buffer.length);
				for (int pos = 0; pos < buffer.length; pos++) {
					result.add(buffer[pos]);
				}
				return result.stream();
			};
			
			List<Byte> all_recevied = recevied.stream().flatMap(explode_lists).collect(Collectors.toList());
			List<Byte> all_sended = sender.sended.stream().flatMap(explode_lists).collect(Collectors.toList());
			
			assertEquals(all_sended.size(), all_recevied.size());
			
			for (int pos = 0; pos < all_sended.size(); pos++) {
				assertEquals(all_sended.get(pos), all_recevied.get(pos));
			}
		}
		
		abstract boolean allRecevied();
	}
	
	class ServerConnectionHandler extends IOHistory implements CompletionHandler<AsynchronousSocketChannel, Void> {
		
		private volatile BulkDataEngine bda;
		
		boolean allRecevied() {
			if (bda == null) {
				return false;
			}
			return bda.all_recevied;
		}
		
		public void completed(AsynchronousSocketChannel socket_channel, Void attachment) {
			SSLEngine engine = ssl_context.createSSLEngine();
			engine.setUseClientMode(false);
			engine.setNeedClientAuth(true);
			
			bda = new BulkDataEngine(socket_channel, engine, this);
		}
		
		public void failed(Throwable exc, Void attachment) {
			exc.printStackTrace();
		}
		
	}
	
	class ClientConnectionHandler extends IOHistory implements CompletionHandler<Void, AsynchronousSocketChannel> {
		
		private volatile BulkDataEngine bda;
		
		boolean allRecevied() {
			if (bda == null) {
				return false;
			}
			return bda.all_recevied;
		}
		
		public void completed(Void result, AsynchronousSocketChannel client_channel) {
			SSLEngine engine = ssl_context.createSSLEngine();
			engine.setUseClientMode(true);
			
			bda = new BulkDataEngine(client_channel, engine, this);
		}
		
		public void failed(Throwable exc, AsynchronousSocketChannel attachment) {
			exc.printStackTrace();
		}
	}
	
}
