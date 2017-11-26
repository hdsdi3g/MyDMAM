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

import junit.framework.TestCase;

public class FrameTest extends TestCase {
	
	public void testFramePrologue() throws IOException {
		long session_id = ThreadLocalRandom.current().nextLong();
		int playload_size = ThreadLocalRandom.current().nextInt(1, 1000000);
		int chunk_count = ThreadLocalRandom.current().nextInt(1, 100);
		CompressionFormat compress_format = CompressionFormat.values()[ThreadLocalRandom.current().nextInt(CompressionFormat.values().length)];
		
		FramePrologue in = new FramePrologue(session_id, playload_size, chunk_count, compress_format);
		
		ByteBuffer bb = in.output();
		
		FramePrologue out = new FramePrologue(bb);
		
		assertEquals(session_id, out.session_id);
		assertEquals(playload_size, out.playload_size);
		assertEquals(chunk_count, out.chunk_count);
		assertEquals(compress_format, out.compress_format);
	}
	
	public void testFramePayload() throws IOException {
		long session_id = ThreadLocalRandom.current().nextLong();
		int chunk = ThreadLocalRandom.current().nextInt(5000000);
		byte[] compressed_raw_content = new byte[ThreadLocalRandom.current().nextInt(50000)];
		int offset = ThreadLocalRandom.current().nextInt(compressed_raw_content.length);
		int length = ThreadLocalRandom.current().nextInt(compressed_raw_content.length - offset);
		
		FramePayload in = new FramePayload(session_id, chunk, compressed_raw_content, offset, length);
		ByteBuffer bb = in.output();
		FramePayload out = new FramePayload(bb);
		
		assertEquals(session_id, out.session_id);
		assertEquals(chunk, out.chunk);
		
		byte[] result_raw_content = new byte[out.internal_compressed_raw_content.remaining()];
		assertEquals(length, result_raw_content.length);
		out.internal_compressed_raw_content.get(result_raw_content);
		
		for (int i = 0; i < result_raw_content.length; i++) {
			assertEquals(compressed_raw_content[i + offset], result_raw_content[i]);
		}
	}
	
	public void testFrameEpilogue() throws IOException {
		long session_id = ThreadLocalRandom.current().nextLong();
		
		FrameEpilogue in = new FrameEpilogue(session_id);
		ByteBuffer bb = in.output();
		FrameEpilogue out = new FrameEpilogue(bb);
		assertEquals(session_id, out.session_id);
	}
	
}
