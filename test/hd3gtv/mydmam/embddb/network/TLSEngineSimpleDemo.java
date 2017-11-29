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
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLSession;

import hd3gtv.tools.TableList;

/**
 * https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/samples/sslengine/SSLEngineSimpleDemo.java
 */
public class TLSEngineSimpleDemo {
	
	// private static final Logger log = Logger.getLogger(TLSEngineSimpleDemo.class);
	
	static {
		/**
		 * Enables the JSSE system debugging system property
		 */
		// System.setProperty("javax.net.debug", "all");
	}
	
	private TableList table = new TableList();
	
	private final SSLContext sslc;
	
	private final SSLEngine client_engine;
	private final ByteBuffer bb_out_write_client;
	private final ByteBuffer bb_in_read_client;
	
	private final SSLEngine server_engine;
	private final ByteBuffer bb_out_write_server;
	private final ByteBuffer bb_int_read_server;
	
	private final ByteBuffer bb_reliable_transport_cli_srv;
	private final ByteBuffer bb_reliable_transport_srv_cli;
	
	private void checkSecurityPolicyString(String[] list, String expected, boolean only_one) throws IOException {
		if (only_one && list.length > 1) {
			throw new IOException("Missing value, expected: " + expected + ", real: " + Arrays.asList(list).toString());
		}
		
		if (Arrays.asList(list).stream().filter(value -> {
			return value.equals(expected);
		}).findFirst().isPresent() == false) {
			throw new IOException("Missing value, expected: " + expected + ", real: " + Arrays.asList(list).toString());
		}
	}
	
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
	public TLSEngineSimpleDemo(SSLContext sslCtx) throws Exception {
		sslc = sslCtx;
		
		boolean dataDone = false;
		/**
		 * Using the SSLContext created during object creation,
		 * create/configure the SSLEngines we'll use for this demo.
		 * -
		 * Configure the serverEngine to act as a server in the TLS
		 * handshake. Also, require SSL client authentication.
		 */
		server_engine = sslc.createSSLEngine();
		server_engine.setUseClientMode(false);
		server_engine.setNeedClientAuth(true);
		server_engine.setEnabledProtocols(new String[] { TestTLS.PROTOCOL });
		server_engine.setEnabledCipherSuites(TestTLS.CIPHER_SUITE);
		
		/**
		 * Similar to above, but using client mode instead.
		 */
		client_engine = sslc.createSSLEngine();
		client_engine.setUseClientMode(true);
		client_engine.setEnabledProtocols(new String[] { TestTLS.PROTOCOL });
		client_engine.setEnabledCipherSuites(TestTLS.CIPHER_SUITE);
		
		/**
		 * We'll assume the buffer sizes are the same
		 * between client and server.
		 */
		SSLSession session = client_engine.getSession();
		int appBufferMax = session.getApplicationBufferSize();
		int netBufferMax = session.getPacketBufferSize();
		
		/**
		 * We'll make the input buffers a bit bigger than the max needed
		 * size, so that unwrap()s following a successful data transfer
		 * won't generate BUFFER_OVERFLOWS.
		 */
		bb_in_read_client = ByteBuffer.allocateDirect(appBufferMax + 50);
		bb_int_read_server = ByteBuffer.allocateDirect(appBufferMax + 50);
		
		bb_reliable_transport_cli_srv = ByteBuffer.allocateDirect(netBufferMax);
		bb_reliable_transport_srv_cli = ByteBuffer.allocateDirect(netBufferMax);
		
		bb_out_write_client = ByteBuffer.wrap("Hi Server, I'm Client".getBytes());
		bb_out_write_server = ByteBuffer.wrap("Hello Client, I'm Server".getBytes());
		
		/**
		 * results from client's last operation
		 */
		SSLEngineResult client_result;
		/**
		 * results from server's last operation
		 */
		SSLEngineResult server_result;
		
		/**
		 * Examining the SSLEngineResults could be much more involved,
		 * and may alter the overall flow of the application.
		 * For example, if we received a BUFFER_OVERFLOW when trying
		 * to write to the output pipe, we could reallocate a larger
		 * pipe, but instead we wait for the peer to drain it.
		 */
		while (!isEngineClosed(client_engine) || !isEngineClosed(server_engine)) {
			client_result = client_engine.wrap(bb_out_write_client, bb_reliable_transport_cli_srv);
			log("client wrap: ", client_result);
			runDelegatedTasks(client_result, client_engine);
			
			server_result = server_engine.wrap(bb_out_write_server, bb_reliable_transport_srv_cli);
			log("server wrap: ", server_result);
			runDelegatedTasks(server_result, server_engine);
			
			bb_reliable_transport_cli_srv.flip();
			bb_reliable_transport_srv_cli.flip();
			
			client_result = client_engine.unwrap(bb_reliable_transport_srv_cli, bb_in_read_client);
			log("client unwrap: ", client_result);
			runDelegatedTasks(client_result, client_engine);
			
			server_result = server_engine.unwrap(bb_reliable_transport_cli_srv, bb_int_read_server);
			log("server unwrap: ", server_result);
			runDelegatedTasks(server_result, server_engine);
			
			bb_reliable_transport_cli_srv.compact();
			bb_reliable_transport_srv_cli.compact();
			
			checkSecurityPolicyString(server_engine.getEnabledProtocols(), TestTLS.PROTOCOL, true);
			checkSecurityPolicyString(client_engine.getEnabledProtocols(), TestTLS.PROTOCOL, true);
			
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
			if (!dataDone && (bb_out_write_client.limit() == bb_int_read_server.position()) && (bb_out_write_server.limit() == bb_in_read_client.position())) {
				
				checkTransfer(bb_out_write_server, bb_in_read_client);
				checkTransfer(bb_out_write_client, bb_int_read_server);
				
				table.addSimpleCellRow("Closing clientEngine's *OUTBOUND*...");
				client_engine.closeOutbound();
				server_engine.closeOutbound();
				dataDone = true;
			}
		}
		
		table.print();
	}
	
	/**
	 * If the result indicates that we have outstanding tasks to do,
	 * go ahead and run them in this thread.
	 */
	private void runDelegatedTasks(SSLEngineResult result, SSLEngine engine) throws Exception {
		if (result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
			Runnable runnable;
			while ((runnable = engine.getDelegatedTask()) != null) {
				table.addSimpleCellRow("Running delegated task");
				runnable.run();
			}
			HandshakeStatus hsStatus = engine.getHandshakeStatus();
			if (hsStatus == HandshakeStatus.NEED_TASK) {
				throw new Exception("handshake shouldn't need additional tasks");
			}
			table.addRow("new HandshakeStatus:", hsStatus.toString());
		}
	}
	
	private static boolean isEngineClosed(SSLEngine engine) {
		return (engine.isOutboundDone() && engine.isInboundDone());
	}
	
	/**
	 * A sanity check to ensure we got what was sent.
	 * Simple check to make sure everything came across as expected.
	 */
	private void checkTransfer(ByteBuffer a, ByteBuffer b) throws Exception {
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
	
	private boolean resultOnce = true;
	
	private void log(String str, SSLEngineResult result) {
		if (resultOnce) {
			resultOnce = false;
			table.addRow("", "getStatus()", "getHandshakeStatus()", "bytesConsumed()", "bytesProduced()");
		}
		HandshakeStatus hsStatus = result.getHandshakeStatus();
		
		table.addRow(str, result.getStatus().toString(), hsStatus.toString(), result.bytesConsumed() + "b", result.bytesProduced() + "b");
		if (hsStatus == HandshakeStatus.FINISHED) {
			table.addSimpleCellRow("ready for application data");
		}
	}
	
}
