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

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Supplier;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.sun.org.apache.xerces.internal.util.XMLChar;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonIgnore;
import hd3gtv.tools.Ascii85;
import hd3gtv.tools.Hexview;

@GsonIgnore
public final class Item implements ByteBufferExporter, Serializable {
	
	private static Logger log = Logger.getLogger(Item.class);
	
	public static final HashFunction CRC32 = Hashing.crc32();
	public static final int CRC32_SIZE = 4;
	
	private String path;
	private String _id;
	private byte[] payload;
	private long created;
	private long updated;
	private long deleted;
	
	private transient ItemKey key;
	private transient byte[] b_id;
	private transient byte[] b_path;
	private transient byte[] b_digest;
	
	ItemKey getKey() {
		if (key == null) {
			key = new ItemKey(_id);
		}
		return key;
	}
	
	public static final Supplier<byte[]> DATA_DIGEST_INST = () -> new byte[CRC32_SIZE];
	
	/**
	 * crc32 on payload
	 */
	byte[] getDigest() {
		return CRC32.hashBytes(payload).asBytes();
	}
	
	void checkDigest(byte[] data) {
		byte[] this_digest = getDigest();
		if (Arrays.equals(data, this_digest) == false) {
			if (log.isTraceEnabled()) {
				log.trace("Invalid raw datas: " + Hexview.LINESEPARATOR + this.getPayloadHexview());
			}
			throw new RuntimeException("Invalid digest !");
		}
	}
	
	public boolean sameDigest(byte[] data) {
		return Arrays.equals(data, getDigest());
	}
	
	Item(String path, String _id, byte[] payload) {
		setId(_id).setPath(path).setPayload(payload);
		created = System.currentTimeMillis();
		deleted = Long.MAX_VALUE - (System.currentTimeMillis() * 10l);
	}
	
	public Item(byte[] payload) {
		setPayload(payload);
		created = System.currentTimeMillis();
		deleted = Long.MAX_VALUE - (System.currentTimeMillis() * 10l);
	}
	
	Item(ByteBuffer read_buffer) {
		_id = new String(TransactionJournal.readNextBlock(read_buffer), MyDMAM.UTF8);
		path = new String(TransactionJournal.readNextBlock(read_buffer), MyDMAM.UTF8);
		created = read_buffer.getLong();
		updated = read_buffer.getLong();
		deleted = read_buffer.getLong();
		payload = TransactionJournal.readNextBlock(read_buffer);
		checkDigest(TransactionJournal.readNextBlock(read_buffer));
	}
	
	public Item setId(String _id) {
		this._id = requireNonEmpty(Objects.requireNonNull(_id, "\"_id\" can't to be null"), "\"_id\" can't to be empty");
		updated = System.currentTimeMillis();
		b_id = null;
		return this;
	}
	
	public Item setPath(String path) {
		if (path == null) {
			this.path = "";
		} else {
			this.path = path;
		}
		updated = System.currentTimeMillis();
		b_path = null;
		return this;
	}
	
	public Item setPayload(byte[] payload) {
		this.payload = Objects.requireNonNull(payload, "\"payload\" can't to be null");
		updated = System.currentTimeMillis();
		b_digest = null;
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
	
	public boolean isDeleted() {
		return deleted < System.currentTimeMillis();
	}
	
	/**
	 * @return can be in past or future
	 */
	long getDeleteDate() {
		return deleted;
	}
	
	/**
	 * @return 0 if deleted
	 */
	long getActualTTL() {
		if (isDeleted()) {
			return 0;
		}
		return deleted - System.currentTimeMillis();
	}
	
	/**
	 * @param ttl if 0, no TTL, else real TTL (positive or negative)
	 */
	Item setTTL(long ttl) {
		if (ttl == 0) {
			deleted = Long.MAX_VALUE - (System.currentTimeMillis() * 10l);
		} else {
			deleted = System.currentTimeMillis() + ttl;
		}
		return this;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(_id);
		sb.append(":");
		sb.append(path);
		if (isDeleted()) {
			sb.append(" DELETED");
		} else {
			sb.append(" (");
			sb.append(payload.length);
			sb.append(" bytes)");
		}
		return sb.toString();
	}
	
	public String getPayloadHexview() {
		return Hexview.tracelog(payload);
	}
	
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_id == null) ? 0 : _id.hashCode());
		return result;
	}
	
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Item other = (Item) obj;
		if (_id == null) {
			if (other._id != null) {
				return false;
			}
		} else if (!_id.equals(other._id)) {
			return false;
		}
		return true;
	}
	
	public void toByteBuffer(ByteBuffer write_buffer) throws IOException {
		if (b_id == null | b_path == null | b_digest == null) {
			getByteBufferWriteSize();
		}
		TransactionJournal.writeNextBlock(write_buffer, b_id);
		TransactionJournal.writeNextBlock(write_buffer, b_path);
		write_buffer.putLong(created);
		write_buffer.putLong(updated);
		write_buffer.putLong(deleted);
		TransactionJournal.writeNextBlock(write_buffer, payload);
		TransactionJournal.writeNextBlock(write_buffer, b_digest);
	}
	
	public int getByteBufferWriteSize() {
		if (b_id == null) {
			b_id = _id.getBytes(MyDMAM.UTF8);
		}
		if (b_path == null) {
			b_path = path.getBytes(MyDMAM.UTF8);
		}
		if (b_digest == null) {
			b_digest = getDigest();
		}
		return (4 + b_id.length) + (4 + b_path.length) + 8 + 8 + 8 + (4 + payload.length) + (4 + b_digest.length);// 48
	}
	
	static final String XML_ITEM_ELEMENT = "item";
	static final String XML_ITEM_CHUNK = "chunk";
	private static final int XML_CHUNK_MAX_SIZE = 600;
	
	private static final String XML_ATTR_ID = "_id";
	private static final String XML_ATTR_PATH = "path";
	private static final String XML_ATTR_SIZE = "size";
	private static final String XML_ATTR_CREATED = "created";
	private static final String XML_ATTR_UPDATED = "updated";
	private static final String XML_ATTR_DELETED = "deleted";
	private static final String XML_ATTR_PAYLOAD_TYPE = "payload_type";
	private static final String XML_ATTR_DIGEST = "digest";
	
	private enum XMLPayloadType {
		UTF8, /*BASE64,*/ HEXSTRING, ASCII85;
	}
	
	public void toXML(XMLSerializer serializer) throws SAXException, IOException {
		AttributesImpl atts = new AttributesImpl();
		atts.addAttribute("", "", XML_ATTR_ID, "CDATA", _id);
		atts.addAttribute("", "", XML_ATTR_PATH, "CDATA", path);
		atts.addAttribute("", "", XML_ATTR_SIZE, "CDATA", String.valueOf(payload.length));
		atts.addAttribute("", "", XML_ATTR_CREATED, "CDATA", String.valueOf(created));
		atts.addAttribute("", "", XML_ATTR_UPDATED, "CDATA", String.valueOf(updated));
		atts.addAttribute("", "", XML_ATTR_DELETED, "CDATA", String.valueOf(deleted));
		atts.addAttribute("", "", XML_ATTR_DIGEST, "CDATA", MyDMAM.byteToString(getDigest()));
		
		XMLPayloadType payload_type = XMLPayloadType.UTF8;
		if (validChars(payload) == false) {
			if (payload.length > 256) {
				// if (payload.length > 10000000) {
				payload_type = XMLPayloadType.ASCII85;
				// } else {
				// payload_type = XMLPayloadType.BASE64;
				// }
			} else {
				payload_type = XMLPayloadType.HEXSTRING;
			}
		}
		
		atts.addAttribute("", "", XML_ATTR_PAYLOAD_TYPE, "CDATA", payload_type.name());
		
		serializer.startElement("", "", XML_ITEM_ELEMENT, atts);
		serializer.comment("Key: " + MyDMAM.byteToString(getKey().key));
		serializer.comment("Created: " + Loggers.dateLog(created));
		serializer.comment("Updated: " + Loggers.dateLog(updated));
		serializer.comment("Deleted: " + Loggers.dateLog(deleted) + " (was deleted: " + String.valueOf(isDeleted()) + ")");
		
		String value = null;
		switch (payload_type) {
		case UTF8:
			value = new String(payload, MyDMAM.UTF8);
			break;
		/*case BASE64:
			value = new String(new Base64().encode(payload));
			break;*/
		case ASCII85:
			value = Ascii85.encode(payload);
			break;
		case HEXSTRING:
			value = MyDMAM.byteToString(payload);
			break;
		}
		
		for (int pos = 0; pos < value.length(); pos += XML_CHUNK_MAX_SIZE) {
			serializer.startElement("", "", XML_ITEM_CHUNK, new AttributesImpl());
			serializer.startCDATA();
			serializer.characters(value.toCharArray(), pos, Math.min(value.length() - pos, XML_CHUNK_MAX_SIZE));
			serializer.endCDATA();
			serializer.endElement("", "", XML_ITEM_CHUNK);
		}
		
		serializer.endElement("", "", XML_ITEM_ELEMENT);
	}
	
	/**
	 * Import from XML
	 */
	Item(HashMap<String, String> attributes, String coded_payload) {
		_id = requireNonEmpty(Objects.requireNonNull(attributes.get(XML_ATTR_ID), "\"_id\" can't to be null"), "\"_id\" can't to be empty");
		path = attributes.get(XML_ATTR_PATH);
		if (path == null) {
			this.path = "";
		}
		created = Long.valueOf(attributes.get(XML_ATTR_CREATED));
		updated = Long.valueOf(attributes.get(XML_ATTR_UPDATED));
		deleted = Long.valueOf(attributes.get(XML_ATTR_DELETED));
		int size = Integer.valueOf(attributes.get(XML_ATTR_SIZE));
		byte[] xml_element_digest = MyDMAM.hexStringToByteArray(attributes.get(XML_ATTR_DIGEST));
		
		XMLPayloadType payload_type = XMLPayloadType.valueOf(attributes.get(XML_ATTR_PAYLOAD_TYPE));
		
		switch (payload_type) {
		case UTF8:
			payload = coded_payload.getBytes(MyDMAM.UTF8);
			break;
		/*case BASE64:
			payload = new Base64().decode(coded_payload);
			break;*/
		case ASCII85:
			payload = Ascii85.decode(coded_payload);
			break;
		case HEXSTRING:
			payload = MyDMAM.hexStringToByteArray(coded_payload);
			break;
		}
		
		if (payload.length != size) {
			throw new RuntimeException("Invalid payload size ! Declared: " + size + ", real: " + payload.length);
		}
		checkDigest(xml_element_digest);
	}
	
	public static boolean validChars(byte[] text) {
		String t = new String(text);
		
		for (int i = 0; i < t.length(); i++) {
			if (XMLChar.isValid(t.charAt(i)) == false) {
				return false;
			}
		}
		return true;
	}
}
