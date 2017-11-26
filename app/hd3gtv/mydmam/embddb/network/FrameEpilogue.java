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
 * Copyright (C) hdsdi3g for hd3g.tv 26 nov. 2017
 * 
*/
package hd3gtv.mydmam.embddb.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;

import hd3gtv.mydmam.embddb.store.Item;

class FrameEpilogue {
	// private static Logger log = Logger.getLogger(FrameEpilogue.class);
	
	private static final int SIZE = Protocol.FRAME_HEADER_SIZE + 1 /** seed */
			+ 8 /** session_id */
	;
	
	final long session_id;
	
	/**
	 * Write/Send
	 */
	FrameEpilogue(long session_id) {
		this.session_id = session_id;
	}
	
	/**
	 * Write/Send
	 */
	ByteBuffer output() {
		ByteBuffer result = ByteBuffer.allocate(SIZE);
		
		/**
		 * Generic Headers
		 */
		result.put(Protocol.APP_EMBDDB_SOCKET_HEADER_TAG);
		result.put(Protocol.VERSION);
		result.put(Protocol.FRAME_TYPE_EPILOGUE);
		
		/**
		 * Seed content
		 */
		result.put((byte) ThreadLocalRandom.current().nextInt());
		
		/**
		 * Content
		 */
		result.putLong(session_id);
		
		result.flip();
		return result;
	}
	
	/**
	 * Read/Get
	 */
	FrameEpilogue(ByteBuffer recevied) throws IOException {
		if (recevied.remaining() < SIZE) {
			throw new IOException("Invalid frame: too short (" + recevied.remaining() + "/" + SIZE + ")");
		}
		
		Item.readAndEquals(recevied, Protocol.APP_EMBDDB_SOCKET_HEADER_TAG, b -> {
			return new IOException("Protocol error with app_socket_header_tag");
		});
		Item.readByteAndEquals(recevied, Protocol.VERSION, version -> {
			return new IOException("Protocol error with version, this = " + Protocol.VERSION + " and dest = " + version);
		});
		Item.readByteAndEquals(recevied, Protocol.FRAME_TYPE_EPILOGUE, type -> {
			return new IOException("Protocol error with frame type, this = " + Protocol.FRAME_TYPE_EPILOGUE + " and dest = " + type);
		});
		
		/**
		 * Seed
		 */
		recevied.get();
		
		session_id = recevied.getLong();
	}
	
}
