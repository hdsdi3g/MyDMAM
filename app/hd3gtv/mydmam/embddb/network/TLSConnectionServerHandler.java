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
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.apache.log4j.Logger;

class TLSConnectionServerHandler implements CompletionHandler<AsynchronousSocketChannel, Void> {
	
	private static Logger log = Logger.getLogger(TLSConnectionServerHandler.class);
	
	final SSLEngine engine;
	
	TLSConnectionServerHandler(SSLContext ssl_context) {
		engine = ssl_context.createSSLEngine();
		engine.setUseClientMode(false);
		engine.setNeedClientAuth(true);
	}
	
	/**
	 * On after distant client was open socket in this local server.
	 */
	public void completed(AsynchronousSocketChannel channel, Void attachment) {
		TLSSocketHandler s_h = new TLSSocketHandler(channel, engine, "DClient->Lserver");
		s_h.getPayloadToSend().put("Hello client, it's server !".getBytes());
		s_h.getPayloadToSend().flip();
		
		try {
			log.debug("Connection to client " + channel.getRemoteAddress() + " established");
			s_h.wrap();
		} catch (IOException e) {
			log.error("Session error, cancel connection", e);
			try {
				channel.close();
			} catch (IOException e1) {
				failed(e, null);
			}
		}
	}
	
	public void failed(Throwable exc, Void attachment) {
		log.error("Channel error", exc);
	}
	
}
