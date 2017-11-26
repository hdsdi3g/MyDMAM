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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

class FrameContainer {
	private static Logger log = Logger.getLogger(FrameContainer.class);
	
	final FramePrologue prologue;
	final AtomicBoolean in_fatal_error;
	final ByteBuffer[] compressed_chunks;
	
	FrameContainer(FramePrologue prologue) {
		this.prologue = prologue;
		in_fatal_error = new AtomicBoolean(false);
		compressed_chunks = new ByteBuffer[prologue.chunk_count];
	}
	
	void appendPayload(FramePayload frame_payload) throws IOException {
		if (in_fatal_error.get()) {
			return;
		}
		try {
			if (compressed_chunks[frame_payload.chunk] != null) {
				throw new IOException("Chunk #" + frame_payload.chunk + " for session " + prologue.session_id + " was previously set");
			}
			compressed_chunks[frame_payload.chunk] = frame_payload.internal_compressed_raw_content;
		} catch (Exception e) {
			in_fatal_error.set(true);
			if (e instanceof IOException) {
				throw e;
			} else {
				throw new IOException(e);
			}
		}
	}
	
	/**
	 * @return can be null if in previous error
	 */
	ByteBuffer close() throws IOException {
		if (in_fatal_error.get()) {
			return null;
		}
		List<ByteBuffer> compressed_chunk_list = Arrays.asList(compressed_chunks);
		
		if (compressed_chunk_list.stream().anyMatch(c -> {
			return c == null;
		})) {
			throw new IOException("Some chunks are missing for session " + prologue.session_id);
		}
		
		int real_payload_size = compressed_chunk_list.stream().mapToInt(c -> c.remaining()).sum();
		if (prologue.playload_size != real_payload_size) {
			throw new IOException("Invalid payload size for session " + prologue.session_id + ", expected: " + prologue.playload_size + " bytes, real: " + real_payload_size + " bytes");
		}
		
		ByteBuffer compressed_payload = ByteBuffer.allocate(real_payload_size);
		compressed_chunk_list.forEach(c -> {
			compressed_payload.put(c);
		});
		compressed_payload.flip();
		
		if (prologue.compress_format != CompressionFormat.NONE) {
			byte[] compressed = new byte[real_payload_size];
			compressed_payload.get(compressed);
			byte[] uncompressed_content = prologue.compress_format.expand(compressed);
			return ByteBuffer.wrap(uncompressed_content);
		} else {
			return compressed_payload;
		}
	}
	
}
