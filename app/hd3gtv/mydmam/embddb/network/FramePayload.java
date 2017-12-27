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

class FramePayload {
	// private static Logger log = Logger.getLogger(FramePayload.class);
	
	static final int HEADER_SIZE = Protocol.FRAME_HEADER_SIZE + 1 /** seed */
			+ 8 /** session_id */
			+ 4 /** chunk */
			+ 4 /** raw content size */
	;
	
	final long session_id;
	final int chunk;
	final ByteBuffer internal_compressed_raw_content;
	
	/**
	 * Write/Send
	 */
	FramePayload(long session_id, int chunk, byte[] compressed_raw_content, int offset, int length) {
		this.session_id = session_id;
		this.chunk = chunk;
		internal_compressed_raw_content = ByteBuffer.wrap(compressed_raw_content, offset, length);
	}
	
	/**
	 * Write/Send
	 */
	ByteBuffer output() {
		ByteBuffer result = ByteBuffer.allocate(HEADER_SIZE + internal_compressed_raw_content.remaining());
		
		/**
		 * Generic Headers
		 */
		result.put(Protocol.APP_EMBDDB_SOCKET_HEADER_TAG);
		result.put(Protocol.VERSION);
		result.put(Protocol.FRAME_TYPE_PAYLOAD);
		
		/**
		 * Seed content
		 */
		result.put((byte) ThreadLocalRandom.current().nextInt());
		
		/**
		 * Content
		 */
		result.putLong(session_id);
		result.putInt(chunk);
		result.putInt(internal_compressed_raw_content.remaining());
		result.put(internal_compressed_raw_content);
		
		result.flip();
		return result;
	}
	
	/**
	 * Read/Get
	 */
	FramePayload(ByteBuffer recevied) throws IOException {
		if (recevied.remaining() < HEADER_SIZE) {
			throw new IOException("Invalid frame: too short (" + recevied.remaining() + "/" + HEADER_SIZE + ")");
		}
		
		Item.readAndEquals(recevied, Protocol.APP_EMBDDB_SOCKET_HEADER_TAG, b -> {
			return new IOException("Protocol error with app_socket_header_tag");
		});
		Item.readByteAndEquals(recevied, Protocol.VERSION, version -> {
			return new IOException("Protocol error with version, this = " + Protocol.VERSION + " and dest = " + version);
		});
		Item.readByteAndEquals(recevied, Protocol.FRAME_TYPE_PAYLOAD, type -> {
			return new IOException("Protocol error with frame type, this = " + Protocol.FRAME_TYPE_PAYLOAD + " and dest = " + type);
		});
		
		/**
		 * Seed
		 */
		recevied.get();
		
		/**
		 * Content
		 */
		session_id = recevied.getLong();
		chunk = recevied.getInt();
		if (chunk < 0) {
			throw new IOException("Invalid chunk: " + chunk);
		}
		
		int size = recevied.getInt();
		if (size < 0) {
			throw new IOException("Invalid size: " + size);
		}
		
		if (recevied.remaining() < size) {
			throw new IOException("Invalid payload, expected " + size + ", real " + recevied.remaining());
		}
		
		internal_compressed_raw_content = ByteBuffer.allocate(size);
		for (int i = 0; i < size; i++) {
			internal_compressed_raw_content.put(recevied.get());
		}
		internal_compressed_raw_content.flip();
	}
	
}
