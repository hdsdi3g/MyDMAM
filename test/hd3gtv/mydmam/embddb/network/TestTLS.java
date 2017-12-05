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

import java.io.File;
import java.nio.ByteBuffer;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLSession;

import org.apache.log4j.Logger;

public class TestTLS /*extends TestCase*/ {
	
	private static final Logger log = Logger.getLogger(TestTLS.class);
	
	/*Arrays.asList(Security.getProviders()).forEach(p -> {
	System.out.println(p.getName());
	p.entrySet().forEach(entry -> {
		System.out.println("\t" + entry.getKey() + "\t\t" + entry.getValue().toString());
	});
	});*/
	
	/*SSLContext context = SSLContext.getInstance("TLSv1.2");
	System.out.println(context.getProtocol()); // TLSv1.2
	System.out.println(context.getProvider());// SunJSSE version 1.8
	
	*/
	
	public static void main(String[] args) throws Exception {
		
		KeystoreTool kt_tool = new KeystoreTool(new File("test.jks"), "test", "me");
		new TLSEngineSimpleDemo(kt_tool);
		
		System.exit(0);
		
		/**
		 * https://docs.oracle.com/javase/6/docs/technotes/guides/security/jsse/JSSERefGuide.html#SSLENG
		 */
		SSLEngine engine_client = kt_tool.createTLSContext().createSSLEngine();
		engine_client.setUseClientMode(true);
		
		SSLSession session = engine_client.getSession();
		/**
		 * http://www.onjava.com/2004/11/03/ssl-nio.html
		 * inNetData: Stores data received directly from the network. This consists of encrypted data and handshake information. This buffer is filled with data read from the socket and emptied by SSLEngine.unwrap().
		 * inAppData: Stores decrypted data received from the peer. This buffer is filled by SSLEngine.unwrap() with decrypted application data and emptied by the application.
		 * outAppData: Stores decrypted application data that is to be sent to the other peer. The application fills this buffer, which is then emptied by SSLEngine.wrap().
		 * outNetData: Stores data that is to be sent to the network, including handshake and encrypted application data. This buffer is filled by SSLEngine.wrap() and emptied by writing it to the network.
		 */
		
		ByteBuffer myAppData = ByteBuffer.allocate(session.getApplicationBufferSize());
		ByteBuffer myNetData = ByteBuffer.allocate(session.getPacketBufferSize());
		ByteBuffer peerAppData = ByteBuffer.allocate(session.getApplicationBufferSize());
		ByteBuffer peerNetData = ByteBuffer.allocate(session.getPacketBufferSize());
		
		SocketChannel socketChannel = new SocketChannel();
		// Do initial handshake
		doHandshake(socketChannel, engine_client, myNetData, peerNetData);
		
		myAppData.put("hello".getBytes());
		myAppData.flip();
		
		while (myAppData.hasRemaining()) {
			// Generate SSL/TLS encoded data (handshake or application data)
			SSLEngineResult res = engine_client.wrap(myAppData, myNetData);
			
			switch (res.getStatus()) {
			case BUFFER_OVERFLOW:
				// Maybe need to enlarge the peer application data buffer.
				if (engine_client.getSession().getApplicationBufferSize() > peerAppData.capacity()) {
					// enlarge the peer application data buffer
				} else {
					// compact or clear the buffer
				}
				// retry the operation
				break;
			
			case BUFFER_UNDERFLOW:
				// Maybe need to enlarge the peer network packet buffer
				if (engine_client.getSession().getPacketBufferSize() > peerNetData.capacity()) {
					// enlarge the peer network packet buffer
				} else {
					// compact or clear the buffer
				}
				// obtain more inbound network data and then retry the operation
				break;
			case CLOSED:
				return;
			case OK:
				myAppData.compact();
				
				// Send SSL/TLS encoded data to peer
				while (myNetData.hasRemaining()) {
					int num = socketChannel.write(myNetData);
					if (num == -1) {
						// handle closed channel
					} else if (num == 0) {
						// no bytes written; try again later
					}
				}
				break;
			}
		}
		
		// Read TLS encoded data from peer
		int num = socketChannel.read(peerNetData);
		if (num == -1) {
			// Handle closed channel
		} else if (num == 0) {
			// No bytes read; try again ...
		} else {
			// Process incoming data
			peerNetData.flip();
			SSLEngineResult res = engine_client.unwrap(peerNetData, peerAppData);
			
			switch (res.getStatus()) {
			case BUFFER_OVERFLOW:
				// Maybe need to enlarge the peer application data buffer.
				if (engine_client.getSession().getApplicationBufferSize() > peerAppData.capacity()) {
					// enlarge the peer application data buffer
				} else {
					// compact or clear the buffer
				}
				// retry the operation
				break;
			
			case BUFFER_UNDERFLOW:
				// Maybe need to enlarge the peer network packet buffer
				if (engine_client.getSession().getPacketBufferSize() > peerNetData.capacity()) {
					// enlarge the peer network packet buffer
				} else {
					// compact or clear the buffer
				}
				// obtain more inbound network data and then retry the operation
				break;
			case CLOSED:
				return;
			case OK:
				peerNetData.compact();
				
				if (peerAppData.hasRemaining()) {
					// Use peerAppData
				}
				break;
			}
		}
	}
	
	private static class SocketChannel {
		
		private ByteBuffer internal = null;
		
		int read(ByteBuffer buffer) {
			return 0;// XXX
		}
		
		public int write(ByteBuffer buffer) {
			return 0;// XXX
		}
	}
	
	/**
	 * https://docs.oracle.com/javase/6/docs/technotes/guides/security/jsse/JSSERefGuide.html#SSLENG
	 */
	static void doHandshake(SocketChannel socketChannel, SSLEngine engine_client, ByteBuffer myNetData, ByteBuffer peerNetData) throws Exception {
		int appBufferSize = engine_client.getSession().getApplicationBufferSize();
		ByteBuffer myAppData = ByteBuffer.allocate(appBufferSize);
		ByteBuffer peerAppData = ByteBuffer.allocate(appBufferSize);
		
		engine_client.beginHandshake();
		SSLEngineResult.HandshakeStatus hs = engine_client.getHandshakeStatus();
		
		while (true) {
			switch (hs) {
			case NEED_UNWRAP:
				if (socketChannel.read(peerNetData) < 0) {
					log.info("Channel wil be closed");
					System.exit(0);
				}
				
				// Process incoming handshaking data
				peerNetData.flip();
				SSLEngineResult res = engine_client.unwrap(peerNetData, peerAppData);
				peerNetData.compact();
				hs = res.getHandshakeStatus();
				
				// Check status
				switch (res.getStatus()) {
				case OK:
					// Handle OK status
					break;
				case BUFFER_OVERFLOW:
					// Maybe need to enlarge the peer application data buffer.
					if (engine_client.getSession().getApplicationBufferSize() > peerAppData.capacity()) {
						// enlarge the peer application data buffer
					} else {
						// compact or clear the buffer
					}
					// retry the operation
					break;
				
				case BUFFER_UNDERFLOW:
					// Maybe need to enlarge the peer network packet buffer
					if (engine_client.getSession().getPacketBufferSize() > peerNetData.capacity()) {
						// enlarge the peer network packet buffer
					} else {
						// compact or clear the buffer
					}
					// obtain more inbound network data and then retry the operation
					break;
				case CLOSED:
					break;
				}
				break;
			case NEED_WRAP:
				// Empty the local network packet buffer.
				myNetData.clear();
				
				// Generate handshaking data
				res = engine_client.wrap(myAppData, myNetData);
				hs = res.getHandshakeStatus();
				
				// Check status
				switch (res.getStatus()) {
				case OK:
					myNetData.flip();
					
					// Send the handshaking data to peer
					while (myNetData.hasRemaining()) {
						if (socketChannel.write(myNetData) < 0) {
							// Handle closed channel
						}
					}
					break;
				case BUFFER_OVERFLOW:
					// Maybe need to enlarge the peer application data buffer.
					if (engine_client.getSession().getApplicationBufferSize() > peerAppData.capacity()) {
						// enlarge the peer application data buffer
					} else {
						// compact or clear the buffer
					}
					// retry the operation
					break;
				
				case BUFFER_UNDERFLOW:
					// Maybe need to enlarge the peer network packet buffer
					if (engine_client.getSession().getPacketBufferSize() > peerNetData.capacity()) {
						// enlarge the peer network packet buffer
					} else {
						// compact or clear the buffer
					}
					// obtain more inbound network data and then retry the operation
					break;
				case CLOSED:
					break;
				}
				break;
			case NEED_TASK:
				Runnable task;
				while ((task = engine_client.getDelegatedTask()) != null) {
					// new Thread(task).start();
					task.run();
				}
				break;
			case FINISHED:
				return;
			case NOT_HANDSHAKING:
				return;
			}
		}
	}
}
