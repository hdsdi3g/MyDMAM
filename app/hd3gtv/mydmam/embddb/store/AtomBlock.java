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
import java.nio.MappedByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.FileLock;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonIgnore;
import hd3gtv.tools.Hexview;
import hd3gtv.tools.StreamMaker;

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
	
	@GsonIgnore
	private final FileChannel channel;
	
	private final long block_start_pos_in_file;
	private final long block_end_pos_in_file;
	private final String four_cc;
	private final short version;
	private final AtomicLong position;
	
	/**
	 * Create mode
	 */
	public AtomBlock(FileChannel channel, long start_pos, long payload_size, String four_cc, short version) throws IOException {
		this.channel = channel;
		if (channel == null) {
			throw new NullPointerException("\"channel\" can't to be null");
		} else if (channel.isOpen() == false) {
			throw new ClosedChannelException();
		} else if (start_pos > channel.size()) {
			throw new IOException("Invalid start_pos " + start_pos + ", file is too small");
		} else if (start_pos < 0) {
			throw new IOException("Invalid start_pos " + start_pos);
		} else if (payload_size < 1) {
			throw new IOException("Invalid payload_size " + payload_size);
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
		header.put(four_cc.getBytes(MyDMAM.US_ASCII));// XXX change order
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
	 * Don't check if overload some datas in channel.
	 */
	public AtomBlock createNextInChannel(long payload_size, String four_cc, short version) throws IOException {
		return new AtomBlock(channel, block_end_pos_in_file, payload_size, four_cc, version);
	}
	
	/**
	 * Don't check if overload some datas in channel.
	 * @return null if not founded/EOF.
	 */
	public AtomBlock getNextInChannel() throws IOException {
		if (channel.isOpen() == false) {
			throw new ClosedChannelException();
		} else if (channel.size() < block_end_pos_in_file + (long) (HEADER_SIZE + FOOTER_SIZE)) {
			/**
			 * EOF
			 */
			return null;
		}
		
		ByteBuffer header = ByteBuffer.allocateDirect(HEADER_TAG.length);
		int size = channel.read(header, block_end_pos_in_file);
		if (size != HEADER_TAG.length) {
			return null;
		}
		header.flip();
		byte[] real_value = new byte[HEADER_TAG.length];
		header.get(real_value);
		if (Arrays.equals(HEADER_TAG, real_value) == false) {
			return null;
		}
		
		return new AtomBlock(channel, block_end_pos_in_file);
	}
	
	/**
	 * Load mode
	 */
	public AtomBlock(FileChannel channel, long start_pos) throws IOException {
		this.channel = channel;
		if (channel == null) {
			throw new NullPointerException("\"channel\" can't to be null");
		} else if (channel.isOpen() == false) {
			throw new ClosedChannelException();
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
		footer.flip();
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
	
	public long getPosition() {
		return position.get();
	}
	
	public short getVersion() {
		return version;
	}
	
	public void write(ByteBuffer buffer, long payload_pos, boolean update_internal_pointer) throws IOException {
		long size = buffer.remaining();
		
		if (payload_pos + size > getPayloadSize()) {
			throw new EOFException("pos (" + payload_pos + ") + size (" + size + ") > payload (" + getPayloadSize() + ")");
		} else if (payload_pos < 0) {
			throw new EOFException("pos (" + payload_pos + ") can't to be < 0");
		} else if (size == 0) {
			log.warn("Wan't to write a 0 byte data buffer");
			return;
		}
		
		long file_start_pos = getPayloadStart() + payload_pos;
		
		if (update_internal_pointer) {
			position.set(payload_pos + size);
		}
		int writed = channel.write(buffer, file_start_pos);
		if (writed != size) {
			throw new IOException("Can't write " + size + ", only " + writed + " was writed");
		}
	}
	
	public void write(ByteBuffer buffer) throws IOException {
		write(buffer, position.getAndAdd(buffer.remaining()), false);
	}
	
	public void read(ByteBuffer buffer, long payload_pos, boolean update_internal_pointer) throws IOException {
		long size = buffer.remaining();
		
		if (payload_pos + size > getPayloadSize()) {
			throw new EOFException("pos (" + payload_pos + ") + size (" + size + ") > payload (" + getPayloadSize() + ")");
		} else if (payload_pos < 0) {
			throw new EOFException("pos (" + payload_pos + ") can't to be < 0");
		} else if (size == 0) {
			log.warn("Wan't to read a 0 byte data buffer");
			return;
		}
		
		long file_start_pos = getPayloadStart() + payload_pos;
		
		if (update_internal_pointer) {
			position.set(payload_pos + size);
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
	 * Don't touch to internal position marker.
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
	 * @param external bulk importation, setPositionInPayload at the end
	 */
	public AtomBlock(FileChannel channel, long payload_pos, String four_cc, short version, List<AtomBlock> from_import_list, long supplementary_space_to_add) throws IOException {
		this(channel, payload_pos, from_import_list.stream().mapToLong(block -> {
			return block.block_end_pos_in_file - block.block_start_pos_in_file;
		}).sum() + supplementary_space_to_add, four_cc, version);
		bulkImport(from_import_list);
	}
	
	/*
	 * Need set to public ?
	 */
	private void bulkImport(List<AtomBlock> from_import_list) throws IOException {
		long expected_size = from_import_list.stream().mapToLong(block -> block.block_end_pos_in_file - block.block_start_pos_in_file).sum();
		
		if (block_start_pos_in_file + position.get() + expected_size > block_end_pos_in_file) {
			throw new IOException("Too large datas to copy " + expected_size + " from position " + position.get());
		}
		
		from_import_list.forEach(block -> {
			try {
				FileLock lock = block.lock(false);
				try {
					block.channel.position(block.block_start_pos_in_file);
					long size = block.block_end_pos_in_file - block.block_start_pos_in_file;
					long w_size = channel.transferFrom(block.channel, getPayloadStart() + position.getAndAdd(size), size);
					if (w_size != size) {
						throw new IOException("Can't write all datas " + size + "/" + w_size);
					}
				} catch (IOException e) {
					throw new RuntimeException("Can't transfert block " + block.four_cc + " (payload: " + block.getPayloadSize() + ")", e);
				} finally {
					lock.release();
				}
			} catch (IOException e_lock) {
				throw new RuntimeException("Can't lock block " + block.four_cc + " (payload: " + block.getPayloadSize() + ")", e_lock);
			}
		});
	}
	
	public MappedByteBuffer mapByteBuffer(MapMode mode) throws IOException {
		if (channel.isOpen() == false) {
			throw new ClosedChannelException();
		}
		return channel.map(mode, getPayloadStart(), getPayloadSize());
	}
	
	public MappedByteBuffer mapByteBuffer(MapMode mode, long payload_pos, long payload_size) throws IOException {
		if (channel.isOpen() == false) {
			throw new ClosedChannelException();
		}
		long pos = block_start_pos_in_file + HEADER_SIZE + payload_pos;
		long end = block_start_pos_in_file + HEADER_SIZE + payload_pos + payload_size;
		if (pos < 0) {
			throw new IOException("Invalid payload_pos " + payload_pos);
		} else if (pos > block_end_pos_in_file) {
			throw new IOException("Invalid payload_pos " + payload_pos);
		} else if (end > block_end_pos_in_file) {
			throw new IOException("Invalid payload_size " + payload_size);
		} else if (end < pos) {
			throw new IOException("Invalid payload_size " + payload_size);
		}
		return channel.map(mode, pos, end - pos);
	}
	
	public Stream<AtomBlock> parseSubBlocks() throws IOException {
		if (channel.isOpen() == false) {
			throw new ClosedChannelException();
		}
		final ByteBuffer header_tag = ByteBuffer.allocateDirect(HEADER_TAG.length);
		final byte[] header_tag_real = new byte[HEADER_TAG.length];
		
		return StreamMaker.create(() -> {
			try {
				if (position.get() == getPayloadSize()) {
					return null;
				} else if (position.get() > getPayloadSize()) {
					throw new EOFException("Invalid position: " + position.get() + "/" + getPayloadSize());
				}
				header_tag.clear();
				read(header_tag, position.get(), false);
				header_tag.flip();
				header_tag.get(header_tag_real);
				
				if (Arrays.equals(HEADER_TAG, header_tag_real) == false) {
					return null;
				}
				
				AtomBlock found = new AtomBlock(channel, HEADER_SIZE + position.get());
				position.addAndGet(found.block_end_pos_in_file - found.block_start_pos_in_file);
				return found;
			} catch (IOException e) {
				throw new RuntimeException("Can't read file", e);
			}
		}).stream();
	}
	
	public void setPositionInPayload(long position) throws IOException {
		if (channel.isOpen() == false) {
			throw new ClosedChannelException();
		}
		if (position < 0) {
			throw new IOException("Invalid payload_pos " + position);
		} else if (position > getPayloadSize()) {
			throw new IOException("Invalid payload_pos " + position);
		}
		this.position.set(position);
	}
	
	public String toString() {
		if (channel.isOpen() == false) {
			return "Closed channel: " + four_cc;
		}
		return four_cc + " v" + version + " p" + position.get() + "/" + getPayloadSize() + " [" + block_start_pos_in_file + "-" + block_end_pos_in_file + "]";
	}
	
	public FileLock lock(boolean shared) throws IOException {
		return channel.tryLock(block_start_pos_in_file, block_end_pos_in_file - block_start_pos_in_file, shared);
	}
	
	public JsonObject toDataMap() throws IOException {
		JsonObject jo = new JsonObject();
		jo.addProperty("four_cc", four_cc);
		jo.addProperty("version", version);
		jo.addProperty("payload_size", getPayloadSize());
		jo.addProperty("from", block_start_pos_in_file);
		jo.addProperty("to", block_end_pos_in_file);
		
		JsonArray content = new JsonArray();
		position.set(0);
		parseSubBlocks().forEach(block -> {
			try {
				content.add(block.toDataMap());
			} catch (IOException e) {
				throw new RuntimeException("Can't read block", e);
			}
		});
		
		if (content.size() > 0) {
			jo.add("content", content);
		}
		
		return jo;
	}
	
}
