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
import java.util.concurrent.ThreadLocalRandom;

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
	
	public void testSimpleOpenClose() throws IOException {
		FileChannel channel = createTempChannel();
		
		int payload_size = 100;
		AtomBlock block = new AtomBlock(channel, 0l, payload_size, "Tst1", (short) 1);
		assertEquals("Tst1", block.getFourCC());
		assertEquals(0, block.getPosition());
		assertEquals((short) 1, block.getVersion());
		
		byte[] payload = getPayload(payload_size);
		block.write(ByteBuffer.wrap(payload));
		assertEquals(payload_size, block.getPosition());
		
		ByteBuffer result = block.readAll();
		assertEquals(payload_size, result.remaining());
		
		for (int pos = 0; pos < payload.length; pos++) {
			assertEquals(payload[pos], result.get());
		}
		
		// XXX System.out.println(block.toDataMap());
		
		channel.close();
	}
	
	/*
	public AtomBlock(AtomBlock parent, long payload_size, String four_cc, short version) throws IOException {
	public AtomBlock(FileChannel channel, long start_pos) throws IOException {
	
	public AtomBlock createNextInChannel(long payload_size, String four_cc, short version) throws IOException {
	public long getPayloadSize() {
	public void write(ByteBuffer buffer, long payload_pos, boolean update_internal_pointer) throws IOException {
	public void read(ByteBuffer buffer, long payload_pos, boolean update_internal_pointer) throws IOException {
	public void read(ByteBuffer buffer) throws IOException {
	public AtomBlock(FileChannel channel, long payload_pos, String four_cc, short version, List<AtomBlock> from_import_list, long supplementary_space_to_add) throws IOException {
	public void bulkImport(List<AtomBlock> from_import_list) throws IOException {
	public MappedByteBuffer mapByteBuffer(MapMode mode) throws IOException {
	public MappedByteBuffer mapByteBuffer(MapMode mode, long payload_pos, long payload_size) throws IOException {
	public Stream<AtomBlock> parseSubBlocks() throws IOException {
	public void setPositionInPayload(long position) throws IOException {
	public FileLock lock(boolean shared) throws IOException {
	public JsonObject toDataMap() throws IOException {
	*/
}
