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

class TLSConnectionClientHandler implements CompletionHandler<Void, AsynchronousSocketChannel> {
	private static Logger log = Logger.getLogger(TLSConnectionClientHandler.class);
	
	final SSLEngine engine;
	
	TLSConnectionClientHandler(SSLContext ssl_context) {
		engine = ssl_context.createSSLEngine();
		engine.setUseClientMode(true);
	}
	
	/**
	 * On after this local client was open socket in distant server.
	 */
	public void completed(Void result, AsynchronousSocketChannel channel) {
		TLSSocketHandler s_h = new TLSSocketHandler(channel, engine, "LClient->Dserver");
		s_h.getPayloadToSend().put("Hello server, it's client !".getBytes());
		s_h.getPayloadToSend().flip();
		
		try {
			log.debug("Connection to server " + channel.getRemoteAddress() + " established");
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