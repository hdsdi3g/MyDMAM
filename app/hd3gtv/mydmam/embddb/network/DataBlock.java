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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import hd3gtv.tools.Hexview;

public final class DataBlock {
	
	private static final Logger log = Logger.getLogger(DataBlock.class);
	
	private ArrayList<RequestEntry> entries;
	private String request_name;
	
	/**
	 * Create mode
	 */
	public DataBlock(String request_name) {
		entries = new ArrayList<>();
		this.request_name = request_name;
		if (request_name == null) {
			throw new NullPointerException("\"request_name\" can't to be null");
		}
	}
	
	/**
	 * Import mode
	 */
	DataBlock(Protocol protocol, byte[] request_raw_datas) throws IOException {
		if (log.isTraceEnabled()) {
			log.trace("Get raw datas" + Hexview.LINESEPARATOR + Hexview.tracelog(request_raw_datas));
		}
		
		ByteArrayInputStream inputstream_client_request = new ByteArrayInputStream(request_raw_datas);
		
		DataInputStream dis = new DataInputStream(inputstream_client_request);
		
		byte[] app_socket_header_tag = new byte[Protocol.APP_SOCKET_HEADER_TAG.length];
		dis.readFully(app_socket_header_tag, 0, Protocol.APP_SOCKET_HEADER_TAG.length);
		
		if (Arrays.equals(Protocol.APP_SOCKET_HEADER_TAG, app_socket_header_tag) == false) {
			throw new IOException("Protocol error with app_socket_header_tag");
		}
		
		int version = dis.readInt();
		if (version != Protocol.VERSION) {
			throw new IOException("Protocol error with version, this = " + Protocol.VERSION + " and dest = " + version);
		}
		
		byte tag = dis.readByte();
		if (tag != 0) {
			throw new IOException("Protocol error, can't found request_name raw datas");
		}
		
		int size = dis.readInt();
		if (size < 1) {
			throw new IOException("Protocol error, can't found request_name raw datas size is too short (" + size + ")");
		}
		
		byte[] request_name_raw = new byte[size];
		dis.read(request_name_raw);
		request_name = new String(request_name_raw, Protocol.UTF8);
		
		tag = dis.readByte();
		if (tag != 1) {
			throw new IOException("Protocol error, can't found zip raw datas");
		}
		
		entries = new ArrayList<>(1);
		
		ZipInputStream request_zip = new ZipInputStream(dis);
		
		ZipEntry entry;
		while ((entry = request_zip.getNextEntry()) != null) {
			entries.add(new RequestEntry(entry, request_zip));
		}
		request_zip.close();
	}
	
	public void checkIfNotEmpty() {
		if (entries.isEmpty()) {
			throw new IndexOutOfBoundsException("No data entries in block");
		}
	}
	
	byte[] getBytes(Protocol protocol) throws IOException {
		checkIfNotEmpty();
		
		ByteArrayOutputStream byte_array_out_stream = new ByteArrayOutputStream(Protocol.BUFFER_SIZE);
		
		DataOutputStream dos = new DataOutputStream(byte_array_out_stream);
		dos.write(Protocol.APP_SOCKET_HEADER_TAG);
		dos.writeInt(Protocol.VERSION);
		
		/**
		 * Start header name
		 */
		dos.writeByte(0);
		byte[] request_name_data = request_name.getBytes(Protocol.UTF8);
		dos.writeInt(request_name_data.length);
		dos.write(request_name_data);
		
		/**
		 * Start datas payload
		 */
		dos.writeByte(1);
		
		/**
		 * Get datas from zip
		 */
		ZipOutputStream zos = new ZipOutputStream(dos);
		zos.setLevel(3);
		entries.forEach(entry -> {
			try {
				entry.toZip(zos);
			} catch (IOException e) {
				log.error("Can't add to zip", e);
			}
		});
		zos.flush();
		zos.finish();
		zos.close();
		
		dos.flush();
		dos.close();
		
		byte[] result = byte_array_out_stream.toByteArray();
		
		if (log.isTraceEnabled()) {
			log.trace("Make raw datas for " + request_name + Hexview.LINESEPARATOR + Hexview.tracelog(result));
		}
		
		return result;
	}
	
	public synchronized DataBlock createEntry(String name, byte[] datas) {
		entries.add(new RequestEntry(name, datas, System.currentTimeMillis()));
		return this;
	}
	
	public synchronized DataBlock createEntry(String name, String datas) {
		entries.add(new RequestEntry(name, datas.getBytes(Protocol.UTF8), System.currentTimeMillis()));
		
		if (log.isTraceEnabled()) {
			log.trace("Add string datas to block " + name + " \"" + datas + "\"");
		}
		return this;
	}
	
	public String getRequestName() {
		return request_name;
	}
	
	public Stream<RequestEntry> getEntries() {
		return entries.stream();
	}
	
	public int getSize() {
		return entries.size();
	}
	
	public RequestEntry getByName(String name) throws IOException {
		Optional<RequestEntry> o_entry = entries.stream().filter(entry -> {
			return entry.getName().equalsIgnoreCase(name);
		}).findFirst();
		
		if (o_entry.isPresent()) {
			return o_entry.get();
		}
		throw new IOException("Can't found " + name + " in entries names from " + request_name + " request");
	}
	
	public boolean hasName(String name) {
		return entries.stream().anyMatch(entry -> {
			return entry.getName().equalsIgnoreCase(name);
		});
	}
	
	public String toString() {
		AtomicInteger all_size = new AtomicInteger(0);
		entries.forEach(block -> {
			all_size.addAndGet(block.getLen());
		});
		
		if (entries.size() == 1) {
			return request_name + " (" + all_size.get() + " bytes in 1 item)";
		} else {
			return request_name + " (" + all_size.get() + " bytes in " + entries.size() + " items)";
		}
	}
	
	public class RequestEntry {
		
		private String name;
		private byte[] datas;
		private long date;
		private int len;
		
		private RequestEntry(String name, byte[] datas, long date) {
			this.name = name;
			if (name == null) {
				throw new NullPointerException("\"name\" can't to be null");
			}
			this.datas = datas;
			if (datas == null) {
				throw new NullPointerException("\"datas\" can't to be null");
			}
			this.date = date;
			len = datas.length;
		}
		
		private RequestEntry(ZipEntry entry, ZipInputStream zipdatas) throws IOException {
			ByteArrayOutputStream bias = new ByteArrayOutputStream(Protocol.BUFFER_SIZE);
			IOUtils.copy(zipdatas, bias);
			
			datas = bias.toByteArray();
			len = datas.length;
			date = entry.getTime();
			name = entry.getName();
			
			if (log.isTraceEnabled()) {
				log.trace("Get datas from zip: \"" + name + "\"" + Hexview.LINESEPARATOR + Hexview.tracelog(datas, 0, len));
			}
		}
		
		public byte[] getDatas() {
			return datas;
		}
		
		public long getDate() {
			return date;
		}
		
		public int getLen() {
			return len;
		}
		
		public String getName() {
			return name;
		}
		
		public String getDatasAsString() {
			return new String(datas, 0, len, Protocol.UTF8);
		}
		
		private void toZip(ZipOutputStream zipdatas) throws IOException {
			if (log.isTraceEnabled()) {
				log.trace("Add entry to Zip: \"" + name + "\"" + Hexview.LINESEPARATOR + Hexview.tracelog(datas, 0, len));
			}
			
			ZipEntry entry = new ZipEntry(name);
			entry.setTime(date);
			// entry.setSize(len);
			zipdatas.putNextEntry(entry);
			zipdatas.write(datas, 0, len);
			zipdatas.flush();
			zipdatas.closeEntry();
		}
		
	}
	
}
