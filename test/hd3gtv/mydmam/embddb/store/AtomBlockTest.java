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
 * Copyright (C) hdsdi3g for hd3g.tv 1 janv. 2018
 * 
*/
package hd3gtv.mydmam.embddb.store;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import hd3gtv.mydmam.MyDMAM;
import junit.framework.TestCase;

public class AtomBlockTest extends TestCase {
	
	private static Logger log = Logger.getLogger(AtomBlockTest.class);
	
	private byte[] getPayload(int size) {
		byte[] payload = new byte[size];
		ThreadLocalRandom.current().nextBytes(payload);
		return payload;
	}
	
	private FileChannel createTempChannel() throws IOException {
		File f = File.createTempFile("mydmam-test-atomblock", ".bin");
		f.delete();
		log.info("Create temp file " + f.getAbsolutePath());
		return FileChannel.open(f.toPath(), MyDMAM.OPEN_OPTIONS_FILE_NOT_EXISTS);
	}
	
	private FileChannel createTempChannel(String name) throws IOException {
		File f = File.createTempFile("mydmam-test-atomblock-" + name, ".bin");
		f.delete();
		log.info("Create temp file " + f.getAbsolutePath());
		return FileChannel.open(f.toPath(), MyDMAM.OPEN_OPTIONS_FILE_NOT_EXISTS);
	}
	
	public void testCreateSave() throws IOException {
		FileChannel channel = createTempChannel();
		
		int payload_size = 100;
		AtomBlock block = new AtomBlock(channel, 0l, payload_size, "Tst1", (short) 1);
		assertEquals("Tst1", block.getFourCC());
		assertEquals(0, block.getPosition());
		assertEquals((short) 1, block.getVersion());
		assertEquals(payload_size, block.getPayloadSize());
		
		byte[] payload = getPayload(payload_size);
		block.write(ByteBuffer.wrap(payload));
		assertEquals(payload_size, block.getPosition());
		
		ByteBuffer result = block.readAll();
		assertEquals(payload_size, result.remaining());
		
		for (int pos = 0; pos < payload.length; pos++) {
			assertEquals(payload[pos], result.get());
		}
		
		channel.close();
	}
	
	public void testCreateSaveOpen() throws IOException {
		FileChannel channel = createTempChannel();
		
		int payload_size = 100;
		AtomBlock block1 = new AtomBlock(channel, 0l, payload_size, "Tst1", (short) 1);
		byte[] payload = getPayload(payload_size);
		block1.write(ByteBuffer.wrap(payload));
		
		AtomBlock block2 = new AtomBlock(channel, 0l);
		assertEquals("Tst1", block2.getFourCC());
		assertEquals(0, block2.getPosition());
		assertEquals((short) 1, block2.getVersion());
		assertEquals(payload_size, block2.getPayloadSize());
		
		ByteBuffer result = block2.readAll();
		assertEquals(payload_size, result.remaining());
		
		for (int pos = 0; pos < payload.length; pos++) {
			assertEquals(payload[pos], result.get());
		}
		
		channel.close();
	}
	
	public void testConsecutive() throws IOException {
		FileChannel channel = createTempChannel();
		
		int count = ThreadLocalRandom.current().nextInt(10, 100);
		
		AtomBlock current_block = null;
		ArrayList<byte[]> added = new ArrayList<>(count);
		
		for (int pos = 0; pos < count; pos++) {
			byte[] payload = getPayload(ThreadLocalRandom.current().nextInt(1000, 10000));
			if (current_block == null) {
				current_block = new AtomBlock(channel, 0l, payload.length, "Tst1", (short) pos);
			} else {
				current_block = current_block.createNextInChannel(payload.length, "Tst1", (short) pos);
			}
			current_block.write(ByteBuffer.wrap(payload));
			added.add(payload);
		}
		
		current_block = null;
		for (int pos = 0; pos < count; pos++) {
			if (current_block == null) {
				current_block = new AtomBlock(channel, 0l);
			} else {
				current_block = current_block.getNextInChannel();
				assertNotNull("Pos: " + pos, current_block);
			}
			
			ByteBuffer payload = current_block.readAll();
			assertEquals(added.get(pos).length, payload.remaining());
			byte[] result = new byte[payload.remaining()];
			payload.get(result);
			
			assertTrue(Arrays.equals(added.get(pos), result));
		}
		
		channel.close();
	}
	
	public void testBulkTransfert() throws IOException {
		FileChannel channel1 = createTempChannel("PRE");
		
		int count = ThreadLocalRandom.current().nextInt(10, 100);
		
		AtomBlock current_block = null;
		ArrayList<byte[]> added_payloads = new ArrayList<>(count);
		ArrayList<AtomBlock> added_blocks = new ArrayList<>(count);
		
		for (int pos = 0; pos < count; pos++) {
			byte[] payload = getPayload(ThreadLocalRandom.current().nextInt(1000, 10000));
			if (current_block == null) {
				current_block = new AtomBlock(channel1, 0l, payload.length, "Tst1", (short) pos);
			} else {
				current_block = current_block.createNextInChannel(payload.length, "Tst1", (short) pos);
			}
			current_block.write(ByteBuffer.wrap(payload));
			
			added_payloads.add(payload);
			added_blocks.add(current_block);
		}
		
		FileChannel channel2 = createTempChannel("POST");
		AtomBlock big_block2 = new AtomBlock(channel2, 0, "Tst2", (short) 0, added_blocks, 0);
		
		big_block2.setPositionInPayload(0);
		assertEquals(0, big_block2.getPosition());
		
		List<AtomBlock> writed_blocks = big_block2.parseSubBlocks().collect(Collectors.toList());
		assertEquals(count, writed_blocks.size());
		
		for (int pos = 0; pos < count; pos++) {
			ByteBuffer content = writed_blocks.get(pos).readAll();
			byte[] result = new byte[content.remaining()];
			content.get(result);
			assertTrue(Arrays.equals(added_payloads.get(pos), result));
		}
		
		channel1.close();
		channel2.close();
	}
	
	public void testSequentialIO() throws IOException {
		FileChannel channel = createTempChannel();
		
		// TODO align many writes and reads with small buffers
		/*	public void write(ByteBuffer buffer, long payload_pos, boolean update_internal_pointer) throws IOException {
			public void read(ByteBuffer buffer, long payload_pos, boolean update_internal_pointer) throws IOException {
			public void read(ByteBuffer buffer) throws IOException {
		*/
		/*int payload_size = 100;
		AtomBlock block = new AtomBlock(channel, 0l, payload_size, "Tst1", (short) 1);
		assertEquals("Tst1", block.getFourCC());
		assertEquals(0, block.getPosition());
		assertEquals((short) 1, block.getVersion());
		assertEquals(payload_size, block.getPayloadSize());
		
		byte[] payload = getPayload(payload_size);
		block.write(ByteBuffer.wrap(payload));
		assertEquals(payload_size, block.getPosition());
		
		ByteBuffer result = block.readAll();
		assertEquals(payload_size, result.remaining());
		
		for (int pos = 0; pos < payload.length; pos++) {
			assertEquals(payload[pos], result.get());
		}*/
		
		channel.close();
	}
	
	/*
	TODO test toString
	TODO test public MappedByteBuffer mapByteBuffer(MapMode mode) throws IOException {
	TODO test public MappedByteBuffer mapByteBuffer(MapMode mode, long payload_pos, long payload_size) throws IOException {
	TODO test public FileLock lock(boolean shared) throws IOException {
	TODO test public JsonObject toDataMap() throws IOException {
	*/
}
