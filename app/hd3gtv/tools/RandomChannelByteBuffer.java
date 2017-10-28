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

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import hd3gtv.mydmam.gson.GsonIgnore;

@GsonIgnore
public class RandomChannelByteBuffer {// TODO usable ?
	// private static Logger log = Logger.getLogger(RandomChannelByteBuffer.class);
	
	private final FileChannel channel;
	private final MapMode map_mode;
	
	private MappedByteBuffer m_bytebuffer;
	private long real_file_position_offset;
	private int ensure_min_capacity;
	
	public RandomChannelByteBuffer(FileChannel channel, MapMode map_mode, long startup_position, int startup_size) throws IOException {
		this.channel = channel;
		this.map_mode = map_mode;
		this.real_file_position_offset = startup_position;
		// this.size = startup_size;
		m_bytebuffer = channel.map(map_mode, startup_position, startup_size - startup_position);
		ensure_min_capacity = 0;
	}
	
	public void setEnsureMinCapacity(int ensure_min_capacity) {
		this.ensure_min_capacity = ensure_min_capacity;
	}
	
	/**
	 * Thread safe (locked).
	 * @param ensure_capacity automatic expand internal MappedByteBuffer and FileChannel if needed.
	 */
	public MappedByteBuffer getByteBuffer(long position, int ensure_capacity) throws IOException {
		synchronized (channel) {
			if (position < real_file_position_offset) {
				real_file_position_offset = position;
				m_bytebuffer = channel.map(map_mode, real_file_position_offset, Math.max(ensure_min_capacity, ensure_capacity));
			} else if (position > real_file_position_offset) {
				m_bytebuffer.limit(m_bytebuffer.capacity());
				
				long new_virtual_pos = position - real_file_position_offset;
				if (new_virtual_pos + (long) ensure_capacity > (long) m_bytebuffer.remaining()) {
					real_file_position_offset = position;
					m_bytebuffer = channel.map(map_mode, real_file_position_offset, Math.max(ensure_min_capacity, ensure_capacity));
				} else {
					m_bytebuffer.position((int) new_virtual_pos);
				}
			}
			return m_bytebuffer;
		}
	}
	
	/**
	 * Thread safe (locked).
	 * @param ensure_capacity automatic expand internal MappedByteBuffer and FileChannel if needed.
	 */
	public MappedByteBuffer getByteBuffer(int ensure_capacity) throws IOException {
		synchronized (channel) {
			ensureCapacity(ensure_capacity);
			return m_bytebuffer;
		}
	}
	
	/**
	 * Thread safe (locked).
	 */
	public MappedByteBuffer getByteBuffer() throws IOException {
		synchronized (channel) {
			return m_bytebuffer;
		}
	}
	
	/**
	 * Thread safe (locked)
	 */
	public long getRealFilePositionOffset() {
		synchronized (channel) {
			return real_file_position_offset;
		}
	}
	
	/**
	 * Thread safe (locked)
	 * Reset limit and position.
	 */
	public void changeByteBuffer(long new_offset, int new_capacity) throws IOException {
		synchronized (channel) {
			if (new_offset == real_file_position_offset && new_capacity == m_bytebuffer.capacity()) {
				return;
			}
			real_file_position_offset = new_offset;
			m_bytebuffer = channel.map(map_mode, real_file_position_offset, Math.max(ensure_min_capacity, new_capacity));
		}
	}
	
	/**
	 * Thread safe (locked)
	 * Reset limit and position.
	 */
	public void setRealFilePositionOffset(long new_offset) throws IOException {
		synchronized (channel) {
			if (new_offset == real_file_position_offset) {
				return;
			}
			changeByteBuffer(new_offset, m_bytebuffer.capacity());
		}
	}
	
	/**
	 * Thread safe (locked)
	 */
	public void ensureCapacity(int new_capacity) throws IOException {
		synchronized (channel) {
			if (new_capacity > m_bytebuffer.remaining()) {
				/**
				 * Reset limit
				 */
				m_bytebuffer.limit(m_bytebuffer.capacity());
				
				/**
				 * Keep actual position
				 */
				int last_pos = m_bytebuffer.position();
				int growup = new_capacity - m_bytebuffer.remaining();
				if (growup > 0) {
					/**
					 * Needs to grow-up m_bytebuffer
					 */
					m_bytebuffer = channel.map(map_mode, real_file_position_offset, Math.max(ensure_min_capacity, m_bytebuffer.capacity() + growup));
					m_bytebuffer.position(last_pos);
				}
			}
		}
	}
	
	/**
	 * Thread safe (locked)
	 */
	public int capacity() {
		synchronized (channel) {
			return m_bytebuffer.capacity();
		}
	}
	
	/**
	 * Thread safe (locked)
	 */
	public int position() {
		synchronized (channel) {
			return m_bytebuffer.position();
		}
	}
	
	/**
	 * Thread safe (locked)
	 */
	public int remaining() {
		synchronized (channel) {
			return m_bytebuffer.remaining();
		}
	}
	
	/**
	 * Thread safe (locked)
	 */
	public boolean hasRemaining() {
		synchronized (channel) {
			return m_bytebuffer.hasRemaining();
		}
	}
	
	public FileChannel getChannel() {
		return channel;
	}
	
	public void truncate(long new_size, long new_pos_after_truncate) throws IOException {
		synchronized (channel) {
			channel.truncate(new_size);
			m_bytebuffer = channel.map(map_mode, new_pos_after_truncate, new_size - new_pos_after_truncate);
		}
	}
	
}
