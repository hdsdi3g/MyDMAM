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
import java.util.concurrent.ThreadLocalRandom;

import org.apache.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.embddb.store.Item;

public final class DataBlock {// TODO test me !
	
	private static final Logger log = Logger.getLogger(DataBlock.class);
	
	private static final int HEADER_SIZE = Protocol.APP_EMBDDB_SOCKET_HEADER_TAG.length /** Generic Header */
			+ 4 /** VERSION */
			+ 4 /** random int */
			+ HandleName.SIZE /** handle name size */
			+ 1 /** tag */
			+ 4 /** payload size */
			+ 8 /** create_date */
	;
	
	private final HandleName handle_name;
	private final long create_date;
	private final ByteBuffer datas_buffer;
	private final IOMode io_mode;
	
	private enum IOMode {
		
		GET_READ_FROM {
			void checkIsGetReadFrom() {
			}
			
			void checkIsPutWriteTo() {
				throw new RuntimeException("Invalid internal state ! You can't use write functions in read mode.");
			}
		},
		PUT_WRITE_TO {
			void checkIsGetReadFrom() {
				throw new RuntimeException("Invalid internal state ! You can't use read functions in write mode.");
			}
			
			void checkIsPutWriteTo() {
			}
		};
		
		abstract void checkIsGetReadFrom();
		
		abstract void checkIsPutWriteTo();
	}
	
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
		io_mode = IOMode.PUT_WRITE_TO;
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
		io_mode = IOMode.PUT_WRITE_TO;
	}
	
	/**
	 * Import mode (receving)
	 * It will call recevieDatas.
	 */
	DataBlock(ByteBuffer header_and_first_block) throws IOException {
		
		Item.readAndEquals(header_and_first_block, Protocol.APP_EMBDDB_SOCKET_HEADER_TAG, b -> {
			return new IOException("Protocol error with app_socket_header_tag");
		});
		
		Item.readAndEquals(header_and_first_block, Protocol.VERSION, version -> {
			return new IOException("Protocol error with version, this = " + Protocol.VERSION + " and dest = " + version);
		});
		
		/**
		 * From a ThreadLocalRandom
		 */
		header_and_first_block.getInt();
		
		handle_name = new HandleName(header_and_first_block);
		
		Item.readByteAndEquals(header_and_first_block, (byte) 0, sep -> {
			return new IOException("Protocol error with 0 separator, this = " + 0 + " and dest = " + sep);
		});
		
		int payload_size = header_and_first_block.getInt();
		if (payload_size < 0) {
			throw new IOException("Protocol error, invalid payload_size: " + payload_size);
		}
		
		create_date = header_and_first_block.getLong();
		
		long now = System.currentTimeMillis();
		if (Math.abs(now - create_date) > Protocol.MAX_DELTA_AGE_BLOCK) {
			throw new IOException("Protocol error, invalid date for block, now: " + Loggers.dateLog(now) + ", distant block: " + Loggers.dateLog(create_date));
		}
		
		datas_buffer = ByteBuffer.allocate(payload_size);
		
		io_mode = IOMode.GET_READ_FROM;
		
		recevieDatas(header_and_first_block);
	}
	
	void recevieDatas(ByteBuffer bucket_block) throws IOException {
		io_mode.checkIsGetReadFrom();
		
		if (datas_buffer.remaining() < bucket_block.remaining()) {
			datas_buffer.clear();
			throw new IOException("Invalid bucket_block size (" + bucket_block.remaining() + " is upper than " + datas_buffer.remaining());
		}
		datas_buffer.put(bucket_block);
	}
	
	public HandleName getRequestName() {
		return handle_name;
	}
	
	/**
	 * @return asReadOnlyBuffer
	 */
	public ByteBuffer getDatas() {
		io_mode.checkIsGetReadFrom();
		
		ByteBuffer result = datas_buffer.asReadOnlyBuffer();
		result.flip();
		result.position(0);
		result.limit(result.capacity());
		return result;
	}
	
	public String getStringDatas() {
		io_mode.checkIsGetReadFrom();
		
		synchronized (datas_buffer) {
			datas_buffer.flip();
			byte[] content = new byte[datas_buffer.remaining()];
			datas_buffer.get(content);
			return new String(content, MyDMAM.UTF8);
		}
	}
	
	private static final JsonParser parser = new JsonParser();
	
	public JsonElement getJsonDatas() throws JsonParseException, JsonSyntaxException {
		return parser.parse(getStringDatas());
	}
	
	public long getCreateDate() {
		return create_date;
	}
	
	/**
	 * Only use this in "In" Node operation
	 */
	boolean isFullyRecevied() {
		io_mode.checkIsGetReadFrom();
		return datas_buffer.hasRemaining() == false;
	}
	
	/**
	 * It will flip result.
	 */
	ByteBuffer getDatasForSend() throws IOException {
		io_mode.checkIsPutWriteTo();
		
		ByteBuffer result = ByteBuffer.allocate(HEADER_SIZE + datas_buffer.remaining());
		
		/**
		 * Generic Headers
		 */
		result.put(Protocol.APP_EMBDDB_SOCKET_HEADER_TAG);
		result.putInt(Protocol.VERSION);
		
		/**
		 * Seed content
		 */
		result.putInt(ThreadLocalRandom.current().nextInt());
		
		/**
		 * Handle name
		 **/
		handle_name.toByteBuffer(result);
		
		/**
		 * Separator
		 */
		result.put((byte) 0);
		
		/**
		 * Payload size + date
		 */
		result.putInt(datas_buffer.remaining());
		result.putLong(create_date);
		
		/**
		 * Full payload
		 */
		result.put(datas_buffer);
		
		if (result.hasRemaining() == true) {
			throw new IOException("Invalid bytebuffer prepare: missing " + result.remaining() + " bytes to write");
		}
		result.flip();
		return result;
	}
	
	/**
	 * Only use this for get from external datas.
	 * @return capacity
	 */
	public int getDataSize() {
		io_mode.checkIsGetReadFrom();
		
		return datas_buffer.capacity();
	}
	
}
