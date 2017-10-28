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
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.attribute.FileAttribute;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.log4j.Logger;

import hd3gtv.mydmam.MyDMAM;
import junit.framework.TestCase;

public class NIOPerfTest extends TestCase {
	private static Logger log = Logger.getLogger(NIOPerfTest.class);
	
	public void testNIO() throws IOException {
		File file = File.createTempFile("mydmam-test", ".bin");
		file.delete();
		
		FileChannel channel = FileChannel.open(file.toPath(), MyDMAM.OPEN_OPTIONS_FILE_NOT_EXISTS);
		
		byte[] bytes = new byte[payload];
		DoubleSummaryStatistics dss = IntStream.range(0, size)/*.parallel()*/.mapToDouble(i -> {
			try {
				// ThreadLocalRandom random = ThreadLocalRandom.current();
				// random.nextBytes(bytes);
				
				long start_sub = System.nanoTime();
				channel.write(ByteBuffer.wrap(bytes));
				return (double) (System.nanoTime() - start_sub) / 1_000_000_000d;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}).summaryStatistics();
		System.out.println("testNIO: " + dss);
		/*
		 * testNIO: DoubleSummaryStatistics{count=1000000, sum=3,422862, min=0,000002, average=0,000003, max=0,002184}
		 */
		channel.close();
		file.deleteOnExit();
	}
	
	private static final int size = 1_000_000;
	private static final int payload = 100;
	
	public void testNIO2() throws IOException {
		File file = File.createTempFile("mydmam-test", ".bin");
		file.delete();
		
		AsynchronousFileChannel channel = AsynchronousFileChannel.open(file.toPath(), MyDMAM.OPEN_OPTIONS_FILE_NOT_EXISTS, null, new FileAttribute[0]);
		
		byte[] bytes = new byte[payload];
		AtomicLong last_pos = new AtomicLong(0);
		List<Future<Integer>> all_futures = IntStream.range(0, size)/*.parallel()*/.mapToObj(i -> {
			// ThreadLocalRandom random = ThreadLocalRandom.current();
			// random.nextBytes(bytes);
			return channel.write(ByteBuffer.wrap(bytes), last_pos.getAndAdd(bytes.length));
		}).collect(Collectors.toList());
		
		DoubleSummaryStatistics dss = all_futures.stream().mapToDouble(future -> {
			try {
				long start_sub = System.nanoTime();
				future.get();
				return (double) (System.nanoTime() - start_sub) / 1_000_000_000d;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}).summaryStatistics();
		
		System.out.println("testNIO2: " + dss);
		
		channel.close();
		file.deleteOnExit();
	}
	
	public void testNIOMap() throws IOException {
		File file = File.createTempFile("mydmam-test", ".bin");
		file.delete();
		
		FileChannel channel = FileChannel.open(file.toPath(), MyDMAM.OPEN_OPTIONS_FILE_NOT_EXISTS);
		
		MappedByteBuffer byte_buffer0 = channel.map(MapMode.READ_WRITE, 0, Integer.MAX_VALUE);
		// System.out.println(byte_buffer0.capacity() + "\t" + file.length());
		MappedByteBuffer byte_buffer = channel.map(MapMode.READ_WRITE, 0, size * payload);
		// System.out.println(byte_buffer.capacity() + "\t" + file.length());
		
		byte[] bytes = new byte[payload];
		DoubleSummaryStatistics dss = IntStream.range(0, size)/*.parallel()*/.mapToDouble(i -> {
			// ThreadLocalRandom random = ThreadLocalRandom.current();
			// random.nextBytes(bytes);
			
			long start_sub = System.nanoTime();
			// channel.write(ByteBuffer.wrap(bytes));
			byte_buffer.put(bytes);
			
			return (double) (System.nanoTime() - start_sub) / 1_000_000_000d;
		}).summaryStatistics();
		byte_buffer.force();
		System.out.println("testNIO: " + dss);
		/*
		 * testNIO: DoubleSummaryStatistics{count=1000000, sum=3,422862, min=0,000002, average=0,000003, max=0,002184}
		 */
		channel.close();
		file.deleteOnExit();
	}
	
}
