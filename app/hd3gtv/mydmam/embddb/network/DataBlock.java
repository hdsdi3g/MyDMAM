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
 * Copyright (C) hdsdi3g for hd3g.tv 21 nov. 2016
 * 
*/
package hd3gtv.mydmam.embddb.network;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.embddb.store.Item;

public final class DataBlock {
	
	private static final Logger log = Logger.getLogger(DataBlock.class);
	
	private final HandleName handle_name;
	private final long create_date;
	private final ByteBuffer datas_buffer;
	
	/**
	 * Create mode
	 */
	public DataBlock(RequestHandler<?> requester, JsonElement datas) {
		this(requester, datas.toString());
	}
	
	/**
	 * Create mode
	 * @param datas, a read-only copy will be created, without touch limit/position of datas, but actual datas limit/position will be keeped for this internal bytebuffer.
	 */
	public DataBlock(RequestHandler<?> requester, ByteBuffer datas) {
		this.handle_name = requester.getHandleName();
		if (handle_name == null) {
			throw new NullPointerException("\"handle_name\" can't to be null");
		}
		if (datas == null) {
			throw new NullPointerException("\"datas\" can't to be null");
		}
		datas_buffer = datas.asReadOnlyBuffer();
		create_date = System.currentTimeMillis();
		if (log.isTraceEnabled()) {
			log.trace("Set datas to block \"" + handle_name + "\" with " + datas_buffer.remaining() + " bytes");
		}
	}
	
	/**
	 * Create mode
	 */
	public DataBlock(RequestHandler<?> requester, String datas) {
		this.handle_name = requester.getHandleName();
		if (handle_name == null) {
			throw new NullPointerException("\"handle_name\" can't to be null");
		}
		if (datas == null) {
			throw new NullPointerException("\"datas\" can't to be null");
		}
		datas_buffer = ByteBuffer.wrap(datas.getBytes(MyDMAM.UTF8));
		create_date = System.currentTimeMillis();
		if (log.isTraceEnabled()) {
			log.trace("Set string datas to block \"" + handle_name + "\" " + datas);
		}
	}
	
	@Deprecated
	ByteBuffer getFramePayloadContent() throws IOException {// XXX remove
		/*ByteBuffer result = ByteBuffer.allocate(HEADER_SIZE + datas_buffer.remaining());
		
		handle_name.toByteBuffer(result);
		result.putLong(create_date);
		
		**
		 * Spacer
		 *
		result.put((byte) 0);
		result.putInt(datas_buffer.remaining());
		result.put(datas_buffer);
		datas_buffer.flip();
		result.flip();
		*/
		return null;
	}
	
	/**
	 * Import mode (receving)
	 */
	@Deprecated
	DataBlock(ByteBuffer full_datas) throws IOException {// XXX remove
		handle_name = new HandleName(full_datas);
		create_date = full_datas.getLong();
		
		long now = System.currentTimeMillis();
		if (Math.abs(now - create_date) > Protocol.MAX_DELTA_AGE_BLOCK) {
			throw new IOException("Protocol error, invalid date for block, now: " + Loggers.dateLog(now) + ", distant block: " + Loggers.dateLog(create_date));
		}
		
		Item.readByteAndEquals(full_datas, (byte) 0, sep -> {
			return new IOException("Protocol error with 0 separator, this = " + 0 + " and dest = " + sep);
		});
		
		int size = full_datas.getInt();
		if (size != full_datas.remaining()) {
			throw new IOException("Invalid internal data size: " + size + " bytes (remaining = " + full_datas.remaining() + ")");
		}
		
		datas_buffer = ByteBuffer.allocate(size);
		datas_buffer.put(full_datas);
	}
	
	public HandleName getRequestName() {
		return handle_name;
	}
	
	/**
	 * @return asReadOnlyBuffer
	 */
	public ByteBuffer getDatas() {
		ByteBuffer result = datas_buffer.asReadOnlyBuffer();
		result.flip();
		result.position(0);
		result.limit(result.capacity());
		return result;
	}
	
	public String getStringDatas() {
		ByteBuffer internal = getDatas();
		byte[] content = new byte[internal.remaining()];
		internal.get(content);
		return new String(content, MyDMAM.UTF8);
	}
	
	private static final JsonParser parser = new JsonParser();
	
	public JsonElement getJsonDatas() throws JsonParseException, JsonSyntaxException {
		return parser.parse(getStringDatas());
	}
	
	public long getCreateDate() {
		return create_date;
	}
	
	/**
	 * @return capacity
	 */
	public int getDataSize() {
		return datas_buffer.capacity();
	}
	
}
