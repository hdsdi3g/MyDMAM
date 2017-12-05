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
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;

import org.apache.log4j.Logger;

import hd3gtv.tools.ThreadPoolExecutorFactory;
import junit.framework.TestCase;

public class TestClientServerTLS extends TestCase {
	
	private static final Logger log = Logger.getLogger(TestClientServerTLS.class);
	
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
		server.accept(null, new ConnectionServerHandler(ssl_context));
		
		AsynchronousSocketChannel client_channel = AsynchronousSocketChannel.open(channel_group);
		client_channel.connect(server_addr, client_channel, new ConnectionClientHandler(ssl_context));
		
		Thread.sleep(10);
		while (client_channel.isOpen()) {
			Thread.sleep(1);
		}
		
		server.close();
		channel_group.shutdown();
		channel_group.awaitTermination(100, TimeUnit.MILLISECONDS);
	}
	
	private class ConnectionClientHandler implements CompletionHandler<Void, AsynchronousSocketChannel> {
		
		final SSLEngine engine;
		
		public ConnectionClientHandler(SSLContext ssl_context) {
			engine = ssl_context.createSSLEngine();
			engine.setUseClientMode(true);
			engine.setEnabledProtocols(new String[] { KeystoreTool.PROTOCOL });
			engine.setEnabledCipherSuites(TLSEngineSimpleDemo.CIPHER_SUITE);
		}
		
		/**
		 * On after this local client was open socket in distant server.
		 */
		public void completed(Void result, AsynchronousSocketChannel channel) {
			SocketHandler s_h = new SocketHandler(channel, engine, "LClient->Dserver");
			s_h.payload_to_send.put("Hello server, it's client !".getBytes());
			s_h.payload_to_send.flip();
			
			try {
				s_h.wrap();
			} catch (IOException e) {
				log.error("Session error, cancel connection", e);
				try {
					channel.close();
				} catch (IOException e1) {
					failed(e, null);
				}
			}
			
			/*ByteBuffer write_buffer = ByteBuffer.allocateDirect(0xFFFF);
			write_buffer.put("Hello".getBytes());
			write_buffer.flip();
			channel.write(write_buffer, 1, TimeUnit.SECONDS, HandlerType.AfterHasWrited, new SocketHandler(channel, write_buffer, engine));*/
		}
		
		public void failed(Throwable exc, AsynchronousSocketChannel channel) {
			log.error("Channel error", exc);
			try {
				channel.close();
			} catch (IOException e) {
			}
		}
		
	}
	
	private class ConnectionServerHandler implements CompletionHandler<AsynchronousSocketChannel, Void> {
		
		final SSLEngine engine;
		
		public ConnectionServerHandler(SSLContext ssl_context) {
			engine = ssl_context.createSSLEngine();
			engine.setUseClientMode(false);
			engine.setNeedClientAuth(true);
			engine.setEnabledProtocols(new String[] { KeystoreTool.PROTOCOL });
			engine.setEnabledCipherSuites(TLSEngineSimpleDemo.CIPHER_SUITE);
		}
		
		/**
		 * On after distant client was open socket in this local server.
		 */
		public void completed(AsynchronousSocketChannel result, Void attachment) {
			SocketHandler s_h = new SocketHandler(result, engine, "DClient->Lserver");
			s_h.payload_to_send.put("Hello client, it's server !".getBytes());
			s_h.payload_to_send.flip();
			
			try {
				s_h.wrap();
			} catch (IOException e) {
				log.error("Session error, cancel connection", e);
				try {
					result.close();
				} catch (IOException e1) {
					failed(e, null);
				}
			}
		}
		
		public void failed(Throwable exc, Void attachment) {
			log.error("Channel error", exc);
		}
		
	}
	
	enum HandlerType {
		AfterHasWrited, AfterHasReaded;
	}
	
	public static final int MAX_PAYLOAD_SIZE = 0xFFFFF;
	
	private class SocketHandler implements CompletionHandler<Integer, HandlerType> {
		
		final AsynchronousSocketChannel channel;
		final ByteBuffer recevied_payload;
		final ByteBuffer recevied_transport;
		final ByteBuffer transport_to_send;
		final ByteBuffer payload_to_send;
		final SSLEngine engine;
		final String name;
		
		SocketHandler(AsynchronousSocketChannel channel, SSLEngine engine, String name) {
			this.channel = channel;
			this.engine = engine;
			this.name = name;
			recevied_payload = ByteBuffer.allocateDirect(engine.getSession().getApplicationBufferSize() + MAX_PAYLOAD_SIZE);
			payload_to_send = ByteBuffer.allocateDirect(MAX_PAYLOAD_SIZE);
			recevied_transport = ByteBuffer.allocateDirect(engine.getSession().getPacketBufferSize());
			transport_to_send = ByteBuffer.allocateDirect(engine.getSession().getPacketBufferSize());
			
			channel.read(recevied_transport, 1, TimeUnit.SECONDS, HandlerType.AfterHasReaded, this);
		}
		
		private void wrap() throws IOException {
			System.out.println("Wrap for " + name + ", " + transport_to_send.position() + ", " + payload_to_send.remaining());// XXX
			
			if (payload_to_send.hasRemaining() == false) {
				log.info("All was sended !" + " for " + name);
				System.exit(0);// XXX
				return;
			}
			
			transport_to_send.clear();
			SSLEngineResult result = engine.wrap(payload_to_send, transport_to_send);
			HandshakeStatus hsStatus = result.getHandshakeStatus();
			log.trace("Status " + result.getStatus().toString() + ", HandshakeStatus: " + hsStatus.toString() + ", consumed: " + result.bytesConsumed() + "b , produced: " + result.bytesProduced() + "b" + " for " + name);
			
			switch (hsStatus) {
			case NOT_HANDSHAKING:
				// XXX
				break;
			case FINISHED:
				log.debug("Session is finished" + " for " + name);
				channel.close();
				break;
			case NEED_TASK:
				runDelegatedTasks(result);
				break;
			case NEED_UNWRAP:
				// XXX
				break;
			case NEED_WRAP:
				// XXX
				break;
			}
			
			transport_to_send.flip();
			channel.write(transport_to_send, 1, TimeUnit.SECONDS, HandlerType.AfterHasWrited, this);
		}
		
		private void unwrap() throws IOException {
			recevied_transport.flip();
			
			System.out.println("Unwrap for " + name + ", " + recevied_transport.remaining() + ", " + recevied_payload.position());// XXX
			
			SSLEngineResult result = engine.unwrap(recevied_transport, recevied_payload);
			HandshakeStatus hsStatus = result.getHandshakeStatus();
			log.trace("Status " + result.getStatus().toString() + ", HandshakeStatus: " + hsStatus.toString() + ", consumed: " + result.bytesConsumed() + "b , produced: " + result.bytesProduced() + "b" + " for " + name);
			
			switch (hsStatus) {
			case NOT_HANDSHAKING:
				// XXX
				break;
			case FINISHED:
				log.debug("Session is finished" + " for " + name);
				channel.close();
				break;
			case NEED_TASK:
				runDelegatedTasks(result);
				break;
			case NEED_UNWRAP:
				// XXX
				break;
			case NEED_WRAP:
				// XXX
				break;
			}
			recevied_transport.clear();
			
			// wrap();//XXX ???
			// TODO checkSecurityPolicyString();
		}
		
		private void runDelegatedTasks(SSLEngineResult result) throws SSLException {
			Runnable runnable;
			while ((runnable = engine.getDelegatedTask()) != null) {
				log.trace("Running delegated task" + " for " + name);
				runnable.run();
			}
			HandshakeStatus hsStatus = engine.getHandshakeStatus();
			if (hsStatus == HandshakeStatus.NEED_TASK) {
				throw new SSLException("Handshake shouldn't need additional tasks");
			}
		}
		
		public void completed(Integer result, HandlerType type) {
			if (type == HandlerType.AfterHasReaded) {
				try {
					unwrap();
				} catch (IOException e) {
					log.error("Can't read TLS payload", e);
					try {
						channel.close();
					} catch (IOException e1) {
						failed(e1, type);
					}
				}
				channel.read(recevied_transport, 1, TimeUnit.SECONDS, HandlerType.AfterHasReaded, this);
			} else if (type == HandlerType.AfterHasWrited) {
				log.debug("Write is done for " + name);
			}
			
			/*if (has_read & has_write) {
				channel.close();
			}*/
		}
		
		public void failed(Throwable exc, HandlerType type) {
			log.error("Socket error from " + type, exc);
			try {
				channel.close();
			} catch (IOException e) {
			}
		}
		
	}
	
}
