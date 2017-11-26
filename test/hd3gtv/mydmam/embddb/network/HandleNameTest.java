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

import junit.framework.TestCase;

public class HandleNameTest extends TestCase {
	
	public void testNames() {
		assertEquals("SimpleName", new HandleName("SimpleName").name);
		assertEquals("SimpleName", new HandleName("Simple Name").name);
		assertEquals("SimpleName", new HandleName("Simple-Name").name);
		assertEquals("Simple_Name", new HandleName("Simple_Name").name);
		assertEquals("1234567890123456789012345678901234567890123456789012345678901234", new HandleName("1234567890123456789012345678901234567890123456789012345678901234A").name);
	}
	
	public void testBuffers() throws IOException {
		String name = "SimpleName";
		HandleName h = new HandleName(name);
		
		ByteBuffer bb = ByteBuffer.allocate(HandleName.SIZE);
		h.toByteBuffer(bb);
		assertFalse(bb.hasRemaining());
		
		byte[] bb_content = new byte[HandleName.SIZE];
		bb.flip();
		bb.get(bb_content);
		assertEquals(name, new String(bb_content).trim());
		bb.flip();
		HandleName h2 = new HandleName(bb);
		assertEquals(h2, h);
	}
}
