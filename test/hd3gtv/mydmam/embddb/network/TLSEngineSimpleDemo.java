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
package hd3gtv.mydmam.embddb.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;

import hd3gtv.tools.TableList;

/**
 * https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/samples/sslengine/SSLEngineSimpleDemo.java
 */
public class TLSEngineSimpleDemo {
	
	// private static final Logger log = Logger.getLogger(TLSEngineSimpleDemo.class);
	
	private static final String[] CIPHER_SUITE;
	
	// TODO check same passwords (with hash + random salt)...
	
	static {
		/**
		 * Enables the JSSE system debugging system property
		 */
		// System.setProperty("javax.net.debug", "all");
		
		/**
		 * https://github.com/ssllabs/research/wiki/SSL-and-TLS-Deployment-Best-Practices
		 */
		ArrayList<String> la_CIPHER_SUITE = new ArrayList<>();
		la_CIPHER_SUITE.add("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256");// TODO why 128 ?!
		// NOPE Windows la_CIPHER_SUITE.add("TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384");
		la_CIPHER_SUITE.add("TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256");
		// NOPE Windows la_CIPHER_SUITE.add("TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384");
		// NOPE Windows la_CIPHER_SUITE.add("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");
		la_CIPHER_SUITE.add("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256");
		la_CIPHER_SUITE.add("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256");
		// NOPE Windows la_CIPHER_SUITE.add("TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384");
		la_CIPHER_SUITE.add("TLS_DHE_RSA_WITH_AES_128_GCM_SHA256");
		// NOPE Windows la_CIPHER_SUITE.add("TLS_DHE_RSA_WITH_AES_256_GCM_SHA384");
		la_CIPHER_SUITE.add("TLS_DHE_RSA_WITH_AES_128_CBC_SHA256");
		// NOPE Windows la_CIPHER_SUITE.add("TLS_DHE_RSA_WITH_AES_256_CBC_SHA256");
		
		// la_CIPHER_SUITE.add("TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA");
		// la_CIPHER_SUITE.add("TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA");
		// la_CIPHER_SUITE.add("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA");
		// la_CIPHER_SUITE.add("TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA");
		// la_CIPHER_SUITE.add("TLS_DHE_RSA_WITH_AES_128_CBC_SHA");
		// la_CIPHER_SUITE.add("TLS_DHE_RSA_WITH_AES_256_CBC_SHA");
		
		CIPHER_SUITE = new String[la_CIPHER_SUITE.size()];
		for (int pos = 0; pos < CIPHER_SUITE.length; pos++) {
			CIPHER_SUITE[pos] = la_CIPHER_SUITE.get(pos);
		}
	}
	
	private TableList table = new TableList();
	
	private final SSLContext ssl_context;
	
	private final SessionWrapper sw_client;
	private final SessionWrapper sw_server;
	
	public static final String CONTEXT_PROTOCOL = "TLSv1.2";
	
	/**
	 * Create an initialized SSLContext to use for this demo.
	 * -
	 * Sit in a tight loop, both engines calling wrap/unwrap regardless
	 * of whether data is available or not. We do this until both engines
	 * report back they are closed.
	 * The main loop handles all of the I/O phases of the SSLEngine's
	 * lifetime:
	 * initial handshaking
	 * application data transfer
	 * engine closing
	 * One could easily separate these phases into separate
	 * sections of code.
	 */
	public TLSEngineSimpleDemo(KeystoreTool kt_tool) throws Exception {
		ssl_context = kt_tool.createTLSContext(CONTEXT_PROTOCOL);
		
		boolean dataDone = false;
		
		sw_client = new SessionWrapper(ssl_context, SessionSide.client);
		sw_server = new SessionWrapper(ssl_context, SessionSide.server);
		
		/**
		 * Examining the SSLEngineResults could be much more involved,
		 * and may alter the overall flow of the application.
		 * For example, if we received a BUFFER_OVERFLOW when trying
		 * to write to the output pipe, we could reallocate a larger
		 * pipe, but instead we wait for the peer to drain it.
		 */
		while (!sw_client.isEngineClosed() || !sw_server.isEngineClosed()) {
			System.out.println();
			System.out.println("SEND: C:" + sw_client.payload_to_send.remaining() + " S:" + sw_server.payload_to_send.remaining());
			System.out.println("RECE: C:" + sw_client.recevied_payload.remaining() + " S:" + sw_server.recevied_payload.remaining());
			
			sw_client.wrap();
			sw_server.wrap();
			
			sw_client.transport.flip();
			sw_server.transport.flip();
			
			sw_client.unwrap(sw_server.transport);
			sw_server.unwrap(sw_client.transport);
			
			sw_client.transport.compact();
			sw_server.transport.compact();
			
			sw_client.checkSecurityPolicyString(CONTEXT_PROTOCOL, true);
			sw_server.checkSecurityPolicyString(CONTEXT_PROTOCOL, true);
			
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
			if (!dataDone && (sw_client.payload_to_send.limit() == sw_server.recevied_payload.position()) && (sw_server.payload_to_send.limit() == sw_client.recevied_payload.position())) {
				
				checkTransfer(sw_server.payload_to_send, sw_client.recevied_payload, table);
				checkTransfer(sw_client.payload_to_send, sw_server.recevied_payload, table);
				
				table.addSimpleCellRow("Closing clientEngine's *OUTBOUND*...");
				sw_client.engine.closeOutbound();
				sw_server.engine.closeOutbound();
				dataDone = true;
			}
		}
		
		table.print();
	}
	
	enum SessionSide {
		client, server;
	}
	
	public static int MAX_PAYLOAD_SIZE = 0xFFFFF;
	
	private class SessionWrapper {
		final SSLEngine engine;
		final ByteBuffer payload_to_send;
		final ByteBuffer recevied_payload;
		final ByteBuffer transport;
		final SessionSide session_side;
		
		SessionWrapper(SSLContext ssl_context, SessionSide session_side) {
			this.session_side = session_side;
			if (session_side == null) {
				throw new NullPointerException("\"session_side\" can't to be null");
			}
			
			engine = ssl_context.createSSLEngine();
			if (session_side == SessionSide.client) {
				engine.setUseClientMode(true);
				engine.setEnabledProtocols(new String[] { CONTEXT_PROTOCOL });
				engine.setEnabledCipherSuites(CIPHER_SUITE);
			} else if (session_side == SessionSide.server) {
				engine.setUseClientMode(false);
				engine.setNeedClientAuth(true);
				engine.setEnabledProtocols(new String[] { CONTEXT_PROTOCOL });
				engine.setEnabledCipherSuites(CIPHER_SUITE);
			}
			
			recevied_payload = ByteBuffer.allocateDirect(engine.getSession().getApplicationBufferSize() + MAX_PAYLOAD_SIZE);
			transport = ByteBuffer.allocateDirect(engine.getSession().getPacketBufferSize());
			
			byte[] payload = new byte[ThreadLocalRandom.current().nextInt(1, MAX_PAYLOAD_SIZE)];
			ThreadLocalRandom.current().nextBytes(payload);
			payload_to_send = ByteBuffer.wrap(payload);
		}
		
		private void wrap() throws SSLException {
			SSLEngineResult result = engine.wrap(payload_to_send, transport);
			log(session_side.name() + " wrap: ", result);
			runDelegatedTasks(result);
		}
		
		private void unwrap(ByteBuffer bb_reliable_transport_other_side) throws SSLException {
			SSLEngineResult result = engine.unwrap(bb_reliable_transport_other_side, recevied_payload);
			log(session_side.name() + " unwrap: ", result);
			runDelegatedTasks(result);
		}
		
		private boolean isEngineClosed() {
			return (engine.isOutboundDone() && engine.isInboundDone());
		}
		
		private void runDelegatedTasks(SSLEngineResult result) throws SSLException {
			if (result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
				Runnable runnable;
				while ((runnable = engine.getDelegatedTask()) != null) {
					table.addRow(session_side.name(), "Running delegated task");
					runnable.run();
				}
				HandshakeStatus hsStatus = engine.getHandshakeStatus();
				if (hsStatus == HandshakeStatus.NEED_TASK) {
					throw new SSLException(session_side.name() + " handshake shouldn't need additional tasks");
				}
				table.addRow(session_side.name(), "new HandshakeStatus:", hsStatus.toString());
			}
		}
		
		private void checkSecurityPolicyString(String expected, boolean only_one) throws IOException {
			String[] list = engine.getEnabledProtocols();
			if (only_one && list.length > 1) {
				throw new IOException(session_side.name() + ", missing value, expected: " + expected + ", real: " + Arrays.asList(list).toString());
			}
			
			if (Arrays.asList(list).stream().filter(value -> {
				return value.equals(expected);
			}).findFirst().isPresent() == false) {
				throw new IOException(session_side.name() + ", missing value, expected: " + expected + ", real: " + Arrays.asList(list).toString());
			}
		}
		
	}
	
	/**
	 * A sanity check to ensure we got what was sent.
	 * Simple check to make sure everything came across as expected.
	 */
	private static void checkTransfer(ByteBuffer a, ByteBuffer b, TableList table) throws Exception {
		a.flip();
		b.flip();
		
		if (!a.equals(b)) {
			throw new Exception("Data didn't transfer cleanly");
		} else {
			table.addRow("Data transferred cleanly");
		}
		
		a.position(a.limit());
		b.position(b.limit());
		a.limit(a.capacity());
		b.limit(b.capacity());
	}
	
	private static void log(String str, SSLEngineResult result) {
		/*if (resultOnce) {
			resultOnce = false;
			table.addRow("", "getStatus()", "getHandshakeStatus()", "bytesConsumed()", "bytesProduced()");
		}*/
		HandshakeStatus hsStatus = result.getHandshakeStatus();
		TableList table = new TableList();
		table.addRow(str, "S " + result.getStatus().toString(), "HS " + hsStatus.toString(), "Csmd " + result.bytesConsumed() + "b", "Prod " + result.bytesProduced() + "b");
		if (hsStatus == HandshakeStatus.FINISHED) {
			table.addSimpleCellRow("ready for application data");
		}
		System.out.print(table.toString());
	}
	
}
