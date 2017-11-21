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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.tools.Hexview;

public final class DataBlock {
	
	private static final Logger log = Logger.getLogger(DataBlock.class);
	
	public static final int HEADER_SIZE = Protocol.APP_EMBDDB_SOCKET_HEADER_TAG.length + 4 /** VERSION */
			+ 1 /** seed content */
			+ 4 /** random int */
			+ 1 /** tag */
			+ 4 /** raw datas size */
			+ 1 /** tag */
			+ 4 /** data_size */
			+ 8 /** create_date */
	;
	
	@Deprecated
	private byte[] datas;
	private String request_name;
	private long create_date;
	
	/**
	 * Create mode
	 */
	public DataBlock(RequestHandler<?> requester, JsonElement datas) {
		this(requester, datas.toString().getBytes(MyDMAM.UTF8));
	}
	
	/**
	 * Create mode
	 */
	public DataBlock(RequestHandler<?> requester, byte[] datas) {
		this.request_name = requester.getHandleName();
		if (request_name == null) {
			throw new NullPointerException("\"HandleName\" can't to be null");
		}
		if (datas == null) {
			throw new NullPointerException("\"datas\" can't to be null");
		}
		this.datas = datas;
		create_date = System.currentTimeMillis();
		if (log.isTraceEnabled()) {
			log.trace("Set datas to block " + request_name + ": " + Hexview.LINESEPARATOR + Hexview.tracelog(datas));
		}
	}
	
	/**
	 * Create mode
	 */
	public DataBlock(RequestHandler<?> requester, String datas) {
		this(requester, datas.getBytes(MyDMAM.UTF8));
	}
	
	/**
	 * Import mode
	 */
	DataBlock(Protocol protocol, byte[] request_raw_datas) throws IOException {
		if (log.isTraceEnabled()) {
			log.trace("Get raw datas" + Hexview.LINESEPARATOR + Hexview.tracelog(request_raw_datas));
		}
		
		ByteArrayInputStream inputstream_client_request = new ByteArrayInputStream(request_raw_datas);
		GZIPInputStream gzin = new GZIPInputStream(inputstream_client_request, Protocol.BUFFER_SIZE);
		
		DataInputStream dis = new DataInputStream(gzin);
		byte[] app_socket_header_tag = new byte[Protocol.APP_EMBDDB_SOCKET_HEADER_TAG.length];
		dis.readFully(app_socket_header_tag, 0, Protocol.APP_EMBDDB_SOCKET_HEADER_TAG.length);
		
		if (Arrays.equals(Protocol.APP_EMBDDB_SOCKET_HEADER_TAG, app_socket_header_tag) == false) {
			throw new IOException("Protocol error with app_socket_header_tag");
		}
		
		int version = dis.readInt();
		if (version != Protocol.VERSION) {
			throw new IOException("Protocol error with version, this = " + Protocol.VERSION + " and dest = " + version);
		}
		
		byte tag = dis.readByte();
		if (tag != 0) {
			throw new IOException("Protocol error, can't found seed content");
		}
		dis.readInt();
		
		tag = dis.readByte();
		if (tag != 0) {
			throw new IOException("Protocol error, can't found tag");
		}
		
		int size = dis.readInt();
		if (size < 1) {
			throw new IOException("Protocol error, can't found request_name raw datas size is too short (" + size + ")");
		}
		
		byte[] request_name_raw = new byte[size];
		dis.read(request_name_raw);
		request_name = new String(request_name_raw, MyDMAM.UTF8);
		
		tag = dis.readByte();
		if (tag != 0) {
			throw new IOException("Protocol error, can't found gzip raw datas");
		}
		
		int data_size = dis.readInt();
		
		create_date = dis.readLong();
		long now = System.currentTimeMillis();
		if (Math.abs(now - create_date) > Protocol.MAX_DELTA_AGE_BLOCK) {
			throw new IOException("Protocol error, invalid date for block, now: " + Loggers.dateLog(now) + ", distant block: " + Loggers.dateLog(create_date));
		}
		
		if (data_size == 0) {
			datas = new byte[0];
		} else {
			datas = new byte[data_size];
			dis.read(datas);
		}
	}
	
	byte[] getBytes(Protocol protocol) throws IOException {
		ByteArrayOutputStream byte_array_out_stream = new ByteArrayOutputStream(Protocol.BUFFER_SIZE);
		GZIPOutputStream gzout = new GZIPOutputStream(byte_array_out_stream, Protocol.BUFFER_SIZE);
		
		DataOutputStream dos = new DataOutputStream(gzout);
		dos.write(Protocol.APP_EMBDDB_SOCKET_HEADER_TAG);
		dos.writeInt(Protocol.VERSION);
		
		/**
		 * Start seed content
		 */
		dos.writeByte(0);
		dos.writeInt(ThreadLocalRandom.current().nextInt());
		
		/**
		 * Start header name
		 */
		dos.writeByte(0);
		byte[] request_name_data = request_name.getBytes(MyDMAM.UTF8);
		dos.writeInt(request_name_data.length);
		dos.write(request_name_data);
		
		/**
		 * Start datas payload
		 */
		dos.writeByte(0);
		dos.writeInt(datas.length);
		dos.writeLong(create_date);
		if (datas.length > 0) {
			dos.write(datas);
		}
		
		dos.flush();
		gzout.finish();
		gzout.flush();
		
		byte[] result = byte_array_out_stream.toByteArray();
		
		if (log.isTraceEnabled()) {
			log.trace("Make raw datas for " + request_name + Hexview.LINESEPARATOR + Hexview.tracelog(result));
		}
		
		return result;
	}
	
	public String getRequestName() {
		return request_name;
	}
	
	public byte[] getDatas() {
		return datas;
	}
	
	public String getStringDatas() {
		return new String(getDatas(), MyDMAM.UTF8);
	}
	
	private static final JsonParser parser = new JsonParser();
	
	public JsonElement getJsonDatas() throws JsonParseException, JsonSyntaxException {
		return parser.parse(getStringDatas());
	}
	
	public long getCreateDate() {
		return create_date;
	}
}
