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
 * Copyright (C) hdsdi3g for hd3g.tv 5 déc. 2017
 * 
*/
package hd3gtv.mydmam.embddb.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;

import org.apache.log4j.Logger;

import hd3gtv.mydmam.embddb.network.TLSSocketHandler.HandlerType;

class TLSSocketHandler implements CompletionHandler<Integer, HandlerType> {
	
	public static final int MAX_PAYLOAD_SIZE = 0xFFFFF;
	
	private static Logger log = Logger.getLogger(TLSSocketHandler.class);
	
	enum HandlerType {
		AfterHasWrited, AfterHasReaded;
	}
	
	private final AsynchronousSocketChannel channel;
	private final ByteBuffer recevied_payload;
	private final ByteBuffer recevied_transport;
	private final ByteBuffer transport_to_send;
	private final ByteBuffer payload_to_send;
	private final SSLEngine engine;
	private final String name;
	
	TLSSocketHandler(AsynchronousSocketChannel channel, SSLEngine engine, String name) {
		this.channel = channel;
		this.engine = engine;
		this.name = name;
		recevied_payload = ByteBuffer.allocateDirect(engine.getSession().getApplicationBufferSize() + MAX_PAYLOAD_SIZE);
		payload_to_send = ByteBuffer.allocateDirect(MAX_PAYLOAD_SIZE);
		recevied_transport = ByteBuffer.allocateDirect(engine.getSession().getPacketBufferSize());
		transport_to_send = ByteBuffer.allocateDirect(engine.getSession().getPacketBufferSize());
		
		channel.read(recevied_transport, 1, TimeUnit.SECONDS, HandlerType.AfterHasReaded, this);
	}
	
	void wrap() throws IOException {
		if (payload_to_send.hasRemaining() == false) {
			log.info("All was sended !" + " for " + name);
			System.exit(0);// XXX
			return;
		}
		
		transport_to_send.clear();
		SSLEngineResult result = engine.wrap(payload_to_send, transport_to_send);
		HandshakeStatus hsStatus = result.getHandshakeStatus();
		log.trace("After wrap for " + name + ", status " + result.getStatus().toString() + ", HandshakeStatus: " + hsStatus.toString() + ", consumed: " + result.bytesConsumed() + "b , produced: " + result.bytesProduced() + "b, " + transport_to_send.position() + ", " + payload_to_send.remaining());
		
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
		
		SSLEngineResult result = engine.unwrap(recevied_transport, recevied_payload);
		HandshakeStatus hsStatus = result.getHandshakeStatus();
		log.trace("After unwrap for " + name + ", status " + result.getStatus().toString() + ", HandshakeStatus: " + hsStatus.toString() + ", consumed: " + result.bytesConsumed() + "b , produced: " + result.bytesProduced() + "b, " + recevied_payload.position() + ", " + recevied_transport.remaining());
		
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
	}
	
	public void failed(Throwable exc, HandlerType type) {
		log.error("Socket error from " + type, exc);
		try {
			channel.close();
		} catch (IOException e) {
		}
	}
	
	ByteBuffer getPayloadToSend() {
		return payload_to_send;
	}
	
	ByteBuffer getReceviedPayload() {
		return recevied_payload;
	}
}
