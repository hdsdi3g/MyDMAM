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
 * Copyright (C) hdsdi3g for hd3g.tv 28 oct. 2017
 * 
*/
package hd3gtv.tools;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.stream.IntStream;

import hd3gtv.mydmam.MyDMAM;
import junit.framework.TestCase;

public class RandomChannelByteBufferTest extends TestCase {
	
	public void testRCBB() throws IOException {
		File file = File.createTempFile("mydmam-test", ".bin");
		file.delete();
		FileChannel channel = FileChannel.open(file.toPath(), MyDMAM.OPEN_OPTIONS_FILE_NOT_EXISTS);
		
		RandomChannelByteBuffer byte_buffer = new RandomChannelByteBuffer(channel, MapMode.READ_WRITE, 0, 1_000);
		assertEquals(file.length(), byte_buffer.getByteBuffer().capacity());
		
		int sum = IntStream.range(0, 1_000).map(i -> {
			try {
				byte[] data = new byte[i + 1];
				byte_buffer.getByteBuffer(data.length).put(data);
				return i + 1;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}).sum();
		
		assertEquals(file.length(), sum);
		
		channel.close();
		file.deleteOnExit();
	}
	
}
