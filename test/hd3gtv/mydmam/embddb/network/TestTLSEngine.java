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
import java.util.concurrent.ThreadLocalRandom;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;

import hd3gtv.mydmam.embddb.network.SocketProvider.SocketType;
import hd3gtv.tools.TableList;
import junit.framework.TestCase;

public class TestTLSEngine extends TestCase {
	
	public void test() throws Exception {
		KeystoreTool kt_tool = new KeystoreTool(new File("test.jks"), "test", "me");
		new TLSEngineSimpleDemo(kt_tool);
	}
	
	static {
		/**
		 * Enables the JSSE system debugging system property
		 */
		// System.setProperty("javax.net.debug", "all");
	}
	
	/*
	 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
	 *
	 * Redistribution and use in source and binary forms, with or without
	 * modification, are permitted provided that the following conditions
	 * are met:
	 *
	 * -Redistribution of source code must retain the above copyright
	 *  notice, this list of conditions and the following disclaimer.
	 *
	 * -Redistribution in binary form must reproduce the above copyright
	 *  notice, this list of conditions and the following disclaimer in the
	 *  documentation and/or other materials provided with the
	 *  distribution.
	 *
	 * Neither the name of Oracle nor the names of
	 * contributors may be used to endorse or promote products derived from
	 * this software without specific prior written permission.
	 *
	 * This software is provided "AS IS," without a warranty of any kind.
	 * ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
	 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
	 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
	 * MIDROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
	 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
	 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN
	 * OR ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR
	 * FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
	 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
	 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
	 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
	 *
	 * You acknowledge that this software is not designed, licensed or
	 * intended for use in the design, construction, operation or
	 * maintenance of any nuclear facility.
	 */
	/**
	 * https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/samples/sslengine/SSLEngineSimpleDemo.java
	 */
	static class TLSEngineSimpleDemo {
		private final SSLContext ssl_context;
		
		private final SessionWrapper sw_client;
		private final SessionWrapper sw_server;
		
		private ByteBuffer createPayload() {
			byte[] payload = new byte[ThreadLocalRandom.current().nextInt(1, MAX_PAYLOAD_SIZE)];
			ThreadLocalRandom.current().nextBytes(payload);
			return ByteBuffer.wrap(payload);
		}
		
		/**
		 * Create an initialized SSLContext to use for this demo.
		 * -
		 * Sit in a tight loop, both engines calling wrap/unwrap regardless
		 * of whether data is available or not. We do this until both engines
		 * report back they are closed.
		 * The main loop handles all of the I/O phases of the SSLEngine's
		 * lifetime:
		 * - initial handshaking
		 * - application data transfer
		 * - engine closing
		 */
		public TLSEngineSimpleDemo(KeystoreTool kt_tool) throws Exception {
			ssl_context = kt_tool.createTLSContext();
			
			boolean dataDone = false;
			
			sw_client = new SessionWrapper(ssl_context, SocketType.CLIENT);
			sw_server = new SessionWrapper(ssl_context, SocketType.SERVER);
			
			ByteBuffer payload_to_send_cli_srv = createPayload();
			ByteBuffer payload_to_send_srv_cli = createPayload();
			
			int loops_remaining = 10;
			
			while (sw_client.isEngineClosed() == false || sw_server.isEngineClosed() == false) {
				System.out.println();
				System.out.println("SEND: C:" + payload_to_send_cli_srv.remaining() + " S:" + payload_to_send_srv_cli.remaining());
				System.out.println("RECE: C:" + sw_client.recevied_payload.remaining() + " S:" + sw_server.recevied_payload.remaining());
				
				sw_client.wrap(payload_to_send_cli_srv);
				sw_server.wrap(payload_to_send_srv_cli);
				
				sw_client.unwrap(sw_server.transport);
				sw_server.unwrap(sw_client.transport);
				
				/**
				 * After we've transfered all application data between the client
				 * and server, we close the clientEngine's outbound stream.
				 * This generates a close_notify handshake message, which the
				 * server engine receives and responds by closing itself.
				 * In normal operation, each SSLEngine should call
				 * closeOutbound(). To protect against truncation attacks,
				 * SSLEngine.closeInbound() should be called whenever it has
				 * determined that no more input data will ever be
				 * available (say a closed input stream).
				 */
				if (!dataDone && (payload_to_send_cli_srv.hasRemaining() == false) && (payload_to_send_srv_cli.hasRemaining() == false) && (loops_remaining > 0)) {
					
					checkTransfer(payload_to_send_srv_cli, sw_client.recevied_payload);
					checkTransfer(payload_to_send_cli_srv, sw_server.recevied_payload);
					
					loops_remaining -= 1;
					
					System.out.println("LOOP: " + loops_remaining);
					
					payload_to_send_cli_srv = createPayload();
					payload_to_send_srv_cli = createPayload();
					
					sw_client.recevied_payload.clear();
					sw_server.recevied_payload.clear();
				}
				
				if (!dataDone && loops_remaining <= 0) {
					dataDone = true;
					
					kt_tool.checkSecurity(sw_client.engine);
					kt_tool.checkSecurity(sw_server.engine);
					
					System.out.println("Closing engines *OUTBOUND*...");
					sw_client.closeOutbound();
					sw_server.closeOutbound();
				}
				
			}
		}
		
		public static int MAX_PAYLOAD_SIZE = 0xFFFFF;
		
		private class SessionWrapper {
			final SSLEngine engine;
			final ByteBuffer recevied_payload;
			final ByteBuffer transport;
			final SocketProvider.SocketType session_side;
			
			SessionWrapper(SSLContext ssl_context, SocketType session_side) {
				this.session_side = session_side;
				if (session_side == null) {
					throw new NullPointerException("\"session_side\" can't to be null");
				}
				
				engine = session_side.initSSLEngine(ssl_context.createSSLEngine());
				recevied_payload = ByteBuffer.allocateDirect(engine.getSession().getApplicationBufferSize() + MAX_PAYLOAD_SIZE);
				transport = ByteBuffer.allocateDirect(engine.getSession().getPacketBufferSize());
			}
			
			private void wrap(ByteBuffer payload_to_send) throws SSLException {
				SSLEngineResult result = engine.wrap(payload_to_send, transport);
				log(session_side.name() + " wrap: ", result);
				runDelegatedTasks(result);
				transport.flip();
			}
			
			private void unwrap(ByteBuffer bb_reliable_transport_other_side) throws SSLException {
				SSLEngineResult result = engine.unwrap(bb_reliable_transport_other_side, recevied_payload);
				log(session_side.name() + " unwrap: ", result);
				runDelegatedTasks(result);
				bb_reliable_transport_other_side.compact();
			}
			
			private boolean isEngineClosed() {
				return (engine.isOutboundDone() && engine.isInboundDone());
			}
			
			private void runDelegatedTasks(SSLEngineResult result) throws SSLException {
				if (result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
					Runnable runnable;
					while ((runnable = engine.getDelegatedTask()) != null) {
						System.out.println(session_side.name() + "\tRunning delegated task");
						runnable.run();
					}
					HandshakeStatus hsStatus = engine.getHandshakeStatus();
					if (hsStatus == HandshakeStatus.NEED_TASK) {
						throw new SSLException(session_side.name() + " handshake shouldn't need additional tasks");
					}
					System.out.println(session_side.name() + "\tnew HandshakeStatus: " + hsStatus.toString());
				}
			}
			
			private void closeOutbound() {
				engine.closeOutbound();
			}
			
		}
		
	}
	
	/**
	 * A sanity check to ensure we got what was sent.
	 * Simple check to make sure everything came across as expected.
	 */
	private static void checkTransfer(ByteBuffer a, ByteBuffer b) throws Exception {
		a.flip();
		b.flip();
		
		if (!a.equals(b)) {
			throw new Exception("Data didn't transfer cleanly");
		} else {
			System.out.println("Data transferred cleanly");
		}
		
		a.position(a.limit());
		b.position(b.limit());
		a.limit(a.capacity());
		b.limit(b.capacity());
	}
	
	private static void log(String str, SSLEngineResult result) {
		HandshakeStatus hsStatus = result.getHandshakeStatus();
		TableList table = new TableList();
		table.addRow(str, "S " + result.getStatus().toString(), "HS " + hsStatus.toString(), "Csmd " + result.bytesConsumed() + "b", "Prod " + result.bytesProduced() + "b");
		if (hsStatus == HandshakeStatus.FINISHED) {
			table.addSimpleCellRow("ready for application data");
		}
		System.out.print(table.toString());
	}
	
}
