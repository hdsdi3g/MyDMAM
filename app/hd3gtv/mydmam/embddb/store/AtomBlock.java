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
 * Copyright (C) hdsdi3g for hd3g.tv 31 déc. 2017
 * 
*/
package hd3gtv.mydmam.embddb.store;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.Normalizer;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.tools.Hexview;

public class AtomBlock {
	
	private static Logger log = Logger.getLogger(AtomBlock.class);
	
	private static final byte[] HEADER_TAG = "EmbBLOCK".getBytes(MyDMAM.US_ASCII);
	private static final byte[] FOOTER_TAG = "END_".getBytes(MyDMAM.US_ASCII);
	
	private static final int HEADER_SIZE = HEADER_TAG.length /** Prefix */
			+ 4 /** fourcc */
			+ 2 /** version */
			+ 8 /** payload_size */
	;
	
	private static final int FOOTER_SIZE = FOOTER_TAG.length;
	/*
		ByteBuffer footer = ByteBuffer.allocateDirect(FOOTER_SIZE);
		footer.put(FOOTER_TAG);
		footer.flip();
	
	*/
	
	private final FileChannel channel;
	private final long block_start_pos_in_file;
	private final long block_end_pos_in_file;
	private final String four_cc;
	private final short version;
	private final AtomicLong position;
	
	/**
	 * Create mode, in a parent block
	 */
	public AtomBlock(AtomBlock parent, long payload_size, String four_cc, short version) throws IOException {
		this(parent.channel, parent.getPayloadStart(), payload_size, four_cc, version);
		
		if (block_end_pos_in_file > parent.getPayloadEnd()) {
			long max_size = (parent.getPayloadEnd() - parent.getPayloadStart()) - (HEADER_SIZE + FOOTER_SIZE);
			throw new IOException("Too big payload_size " + payload_size + ", parent block can't contain this block (max " + max_size + ")");
		}
	}
	
	/**
	 * Create mode
	 */
	public AtomBlock(FileChannel channel, long start_pos, long payload_size, String four_cc, short version) throws IOException {
		this.channel = channel;
		if (channel == null) {
			throw new NullPointerException("\"channel\" can't to be null");
		} else if (channel.isOpen() == false) {
			throw new IOException("Channel is closed");
		} else if (start_pos > channel.size()) {
			throw new IOException("Invalid start_pos " + start_pos + ", file is too small");
		} else if (start_pos < 0) {
			throw new IOException("Invalid start_pos " + start_pos);
		} else if (payload_size < 1) {
			throw new IOException("Invalid payload_size " + payload_size);
		} else if (payload_size <= HEADER_SIZE + FOOTER_SIZE) {
			throw new IOException("Too low payload_size " + payload_size);
		} else if (four_cc == null) {
			throw new NullPointerException("\"four_cc\" can't to be null");
		}
		
		String temp_name = four_cc;
		temp_name = MyDMAM.PATTERN_Combining_Diacritical_Marks_Spaced.matcher(Normalizer.normalize(temp_name, Normalizer.Form.NFD)).replaceAll("");
		temp_name = MyDMAM.PATTERN_Special_Chars.matcher(Normalizer.normalize(temp_name, Normalizer.Form.NFD)).replaceAll("");
		this.four_cc = temp_name;
		if (four_cc.length() != 4) {
			throw new IndexOutOfBoundsException("four_cc must equals 4 in size: " + four_cc);
		}
		
		this.version = version;
		block_start_pos_in_file = start_pos;
		block_end_pos_in_file = start_pos + HEADER_SIZE + payload_size + FOOTER_SIZE;
		
		ByteBuffer header = ByteBuffer.allocateDirect(HEADER_SIZE);
		header.put(HEADER_TAG);
		header.put(four_cc.getBytes(MyDMAM.US_ASCII));
		header.putShort(version);
		header.putLong(payload_size);
		header.flip();
		
		if (log.isDebugEnabled()) {
			log.debug("Prepare to write init block \"" + four_cc + "\", v" + version + " from " + start_pos + ", payload size " + payload_size);
		}
		
		channel.write(header, start_pos);
		
		ByteBuffer footer = ByteBuffer.allocateDirect(FOOTER_SIZE);
		footer.put(FOOTER_TAG);
		footer.flip();
		
		if (log.isTraceEnabled()) {
			log.trace("Prepare to write end block \"" + four_cc + "\", from " + getPayloadEnd());
		}
		channel.write(footer, getPayloadEnd());
		
		position = new AtomicLong(0);
	}
	
	/**
	 * Load mode
	 */
	public AtomBlock(FileChannel channel, long start_pos) throws IOException {
		this.channel = channel;
		if (channel == null) {
			throw new NullPointerException("\"channel\" can't to be null");
		} else if (channel.isOpen() == false) {
			throw new IOException("Channel is closed");
		} else if (start_pos > channel.size()) {
			throw new IOException("Invalid start_pos " + start_pos + ", file is too small");
		} else if (start_pos < 0) {
			throw new IOException("Invalid start_pos " + start_pos);
		}
		block_start_pos_in_file = start_pos;
		
		ByteBuffer header = ByteBuffer.allocateDirect(HEADER_SIZE);
		channel.read(header, start_pos);
		if (header.hasRemaining()) {
			throw new IOException("Not enough of data: EOF before the end of the block (remain " + header.remaining() + ")");
		}
		header.flip();
		Item.readAndEquals(header, HEADER_TAG, b -> {
			return new IOException("Invalid header for block: " + Hexview.tracelog(b));
		});
		byte[] fourcc = new byte[4];
		header.get(fourcc);
		this.four_cc = new String(fourcc, MyDMAM.US_ASCII);
		version = header.getShort();
		
		long payload_size = header.getLong();
		if (payload_size < 1) {
			throw new IOException("Invalid payload_size " + payload_size);
		} else if (start_pos + HEADER_SIZE + payload_size + FOOTER_SIZE > channel.size()) {
			throw new IOException("Too big payload_size " + payload_size);
		}
		block_end_pos_in_file = start_pos + HEADER_SIZE + payload_size + FOOTER_SIZE;
		
		if (header.hasRemaining()) {
			throw new IOException("Error during buffer reader (remain " + header.remaining() + ")");
		}
		
		if (log.isDebugEnabled()) {
			log.debug("Loaded block \"" + four_cc + "\", v" + version + " from " + start_pos + ", payload size " + payload_size);
		}
		
		ByteBuffer footer = ByteBuffer.allocateDirect(FOOTER_SIZE);
		channel.read(footer, getPayloadEnd());
		if (header.hasRemaining()) {
			throw new IOException("Not enough of data: EOF before the end of the block (remain " + header.remaining() + ")");
		}
		Item.readAndEquals(footer, FOOTER_TAG, b -> {
			return new IOException("Invalid header for block: " + Hexview.tracelog(b));
		});
		if (log.isTraceEnabled()) {
			log.trace("Block is valid \"" + four_cc + "\", from " + getPayloadEnd());
		}
		
		position = new AtomicLong(0);
	}
	
	private long getPayloadStart() {
		return block_start_pos_in_file + HEADER_SIZE;
	}
	
	private long getPayloadEnd() {
		return block_end_pos_in_file - FOOTER_SIZE;
	}
	
	public long getPayloadSize() {
		return getPayloadEnd() - getPayloadStart();
	}
	
	public String getFourCC() {
		return four_cc;
	}
	
	public long getPositionInPayload() {
		return position.get();
	}
	
	public short getVersion() {
		return version;
	}
	
	public void write(ByteBuffer buffer, long block_start_pos, boolean update_internal_pointer) throws IOException {
		long size = buffer.remaining();
		
		if (block_start_pos + size > getPayloadSize()) {
			throw new EOFException("pos (" + block_start_pos + ") + size (" + size + ") > payload (" + getPayloadSize() + ")");
		} else if (block_start_pos < 0) {
			throw new EOFException("pos (" + block_start_pos + ") can't to be < 0");
		} else if (size == 0) {
			log.warn("Wan't to write a 0 byte data buffer");
			return;
		}
		
		long file_start_pos = getPayloadStart() + block_start_pos;
		
		if (update_internal_pointer) {
			position.set(block_start_pos + size);
		}
		int writed = channel.write(buffer, file_start_pos);
		if (writed != size) {
			throw new IOException("Can't write " + size + ", only " + writed + " was writed");
		}
	}
	
	public void write(ByteBuffer buffer) throws IOException {
		write(buffer, position.getAndAdd(buffer.remaining()), false);
	}
	
	public void read(ByteBuffer buffer, long block_start_pos, boolean update_internal_pointer) throws IOException {
		long size = buffer.remaining();
		
		if (block_start_pos + size > getPayloadSize()) {
			throw new EOFException("pos (" + block_start_pos + ") + size (" + size + ") > payload (" + getPayloadSize() + ")");
		} else if (block_start_pos < 0) {
			throw new EOFException("pos (" + block_start_pos + ") can't to be < 0");
		} else if (size == 0) {
			log.warn("Wan't to read a 0 byte data buffer");
			return;
		}
		
		long file_start_pos = getPayloadStart() + block_start_pos;
		
		if (update_internal_pointer) {
			position.set(block_start_pos + size);
		}
		int readed = channel.read(buffer, file_start_pos);
		if (readed != size) {
			throw new IOException("Can't read " + size + ", only " + readed + " was readed");
		}
	}
	
	public void read(ByteBuffer buffer) throws IOException {
		read(buffer, position.getAndAdd(buffer.remaining()), false);
	}
	
	/**
	 * @return buffer will be flipped
	 */
	public ByteBuffer readAll() throws IOException {
		int size = (int) getPayloadSize();
		ByteBuffer buffer = ByteBuffer.allocateDirect(size);
		int readed = channel.read(buffer, getPayloadStart());
		if (readed != size) {
			throw new IOException("Can't read " + size + ", only " + readed + " was readed");
		}
		buffer.flip();
		
		return buffer;
	}
	
	/**
	 * @param external bulk importation
	 */
	public AtomBlock(FileChannel channel, long start_pos, String four_cc, short version, List<AtomBlock> from_import_list, long supplementary_space_to_add) throws IOException {
		this(channel, start_pos, from_import_list.stream().mapToLong(block -> {
			return block.block_end_pos_in_file - block.block_start_pos_in_file;
		}).sum() + supplementary_space_to_add, four_cc, version);
		
		from_import_list.forEach(block -> {
			try {
				// XXX use tryLock
				channel.transferFrom(block.channel, block.block_start_pos_in_file, block.block_end_pos_in_file - block.block_start_pos_in_file);
			} catch (IOException e) {
				throw new RuntimeException("Can't transfert block " + block.four_cc + " (payload: " + block.getPayloadSize() + ")", e);
			}
		});
	}
	
	// XXX channel.tryLock()
	// XXX channel.map(mode, position, size)
	
	public Stream<AtomBlock> parseSubBlocks() {
		// XXX use map
		return Stream.empty();
	}
	
	// XXX create from source Bytebuffer + supplementary_space_to_add
}
