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
 * Copyright (C) hdsdi3g for hd3g.tv 11 sept. 2017
 * 
*/
package hd3gtv.mydmam.embddb.store;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import org.apache.log4j.Logger;

import com.google.common.hash.Hashing;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonIgnore;
import hd3gtv.tools.Hexview;

@GsonIgnore
public final class StoreItem {
	
	private static Logger log = Logger.getLogger(StoreItem.class);
	
	private String path;
	private String _id;
	private byte[] payload;
	private long created;
	private long updated;
	private long deleted;
	
	/**
	 * MurmurHash3_x64_128 (Murmur3F) on _id
	 */
	byte[] getKey() {
		return computeKey(_id);
	}
	
	static byte[] computeKey(String _id) {
		return Hashing.murmur3_128().hashString(_id).asBytes();
	}
	
	/**
	 * crc32 on payload
	 */
	byte[] getDigest() {
		return Hashing.crc32().hashBytes(payload).asBytes();
	}
	
	int estimateSize() {
		return _id.length() + path.length() + payload.length + 3 * 8;
	}
	
	void checkDigest(byte[] data) throws IOException {
		byte[] this_digest = getDigest();
		if (Arrays.equals(data, this_digest) == false) {
			if (log.isTraceEnabled()) {
				log.trace("Invalid raw datas: " + Hexview.LINESEPARATOR + this.getPayloadHexview());
			}
			throw new IOException("Invalid digest !");
		}
	}
	
	public static void writeNextBlock(DataOutputStream daos, byte[] value) throws IOException {
		daos.writeInt(value.length);
		daos.write(value);
	}
	
	public static byte[] readNextBlock(DataInputStream dis) throws IOException {
		int size = dis.readInt();
		byte[] b = new byte[size];
		dis.read(b);
		return b;
	}
	
	byte[] toRawContent() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(path.length() + _id.length() + payload.length);
		try {
			DataOutputStream daos = new DataOutputStream(baos);
			writeNextBlock(daos, _id.getBytes(MyDMAM.UTF8));
			writeNextBlock(daos, path.getBytes(MyDMAM.UTF8));
			daos.writeLong(created);
			daos.writeLong(updated);
			daos.writeLong(deleted);
			writeNextBlock(daos, payload);
			writeNextBlock(daos, getDigest());
			daos.flush();
			daos.close();
		} catch (IOException e) {
			log.error("Can't create raw StoreItem", e);
			return null;
		}
		return baos.toByteArray();
	}
	
	private StoreItem() {
	}
	
	static StoreItem fromRawContent(byte[] data) {
		ByteArrayInputStream bias = new ByteArrayInputStream(data);
		DataInputStream dis = new DataInputStream(bias);
		
		StoreItem item = new StoreItem();
		try {
			item._id = new String(readNextBlock(dis), MyDMAM.UTF8);
			item.path = new String(readNextBlock(dis), MyDMAM.UTF8);
			item.created = dis.readLong();
			item.updated = dis.readLong();
			item.deleted = dis.readLong();
			item.payload = readNextBlock(dis);
			item.checkDigest(readNextBlock(dis));
		} catch (IOException e) {
			log.error("Can't read raw StoreItem", e);
			return null;
		}
		
		return item;
	}
	
	public StoreItem(String path, String _id, byte[] payload) {
		setId(_id).setPath(path).setPayload(payload);
		created = System.currentTimeMillis();
		deleted = 0;
	}
	
	public StoreItem setId(String _id) {
		this._id = requireNonEmpty(Objects.requireNonNull(_id, "\"_id\" can't to be null"), "\"_id\" can't to be empty");
		updated = System.currentTimeMillis();
		return this;
	}
	
	public StoreItem setPath(String path) {
		this.path = requireNonEmpty(Objects.requireNonNull(path, "\"path\" can't to be null"), "\"path\" can't to be empty");
		updated = System.currentTimeMillis();
		return this;
	}
	
	public StoreItem setPayload(byte[] payload) {
		this.payload = requireNonEmpty(Objects.requireNonNull(payload, "\"payload\" can't to be null"), "\"payload\" can't to be empty");
		updated = System.currentTimeMillis();
		return this;
	}
	
	public static String requireNonEmpty(String value, String message) {
		if (value.trim().isEmpty()) {
			throw new IndexOutOfBoundsException(message);
		}
		return value;
	}
	
	public static byte[] requireNonEmpty(byte[] value, String message) {
		if (value.length == 0) {
			throw new IndexOutOfBoundsException(message);
		}
		return value;
	}
	
	public String getId() {
		return _id;
	}
	
	public long getCreated() {
		return created;
	}
	
	public String getPath() {
		return path;
	}
	
	public byte[] getPayload() {
		return payload;
	}
	
	public long getUpdated() {
		return updated;
	}
	
	/**
	 * Or Long.MAX if not TTL, or Long.MIN if totally expired
	 */
	public long getRemainingTTL() {// TODO badly used !!
		if (deleted == 0) {
			return Long.MAX_VALUE;
		}
		long rem = deleted - System.currentTimeMillis();
		if (rem > 0) {
			return rem;
		} else {
			return Long.MIN_VALUE;
		}
	}
	
	long getDeleted() {
		return deleted;
	}
	
	void setTTL(long ttl) {
		if (ttl != 0) {
			deleted = System.currentTimeMillis() + ttl;
		} else {
			deleted = 0;
		}
		
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(_id);
		sb.append(":");
		sb.append(path);
		sb.append(" (");
		sb.append(payload.length);
		sb.append(" bytes)");
		return sb.toString();
	}
	
	public String getPayloadHexview() {
		return Hexview.tracelog(payload);
	}
	
}
