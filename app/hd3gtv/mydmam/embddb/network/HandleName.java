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
 * Copyright (C) hdsdi3g for hd3g.tv 12 sept. 2017
 * 
*/
package hd3gtv.mydmam.embddb.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.Normalizer;

import org.apache.commons.lang.StringUtils;

import hd3gtv.mydmam.MyDMAM;

/**
 * A String wrapper, with fixed length.
 */
public final class HandleName {// TODO test me !
	
	final String name;
	
	final static int SIZE = 64;
	
	/**
	 * @param original_name remove special chars, non ascii chars, spaces, and limit to SIZE
	 */
	public HandleName(String original_name) {
		String temp_name = original_name;
		temp_name = MyDMAM.PATTERN_Combining_Diacritical_Marks_Spaced.matcher(Normalizer.normalize(temp_name, Normalizer.Form.NFD)).replaceAll("");
		temp_name = MyDMAM.PATTERN_Special_Chars.matcher(Normalizer.normalize(temp_name, Normalizer.Form.NFD)).replaceAll("");
		name = temp_name.substring(0, Math.min(temp_name.length(), SIZE));
		if (name.isEmpty()) {
			throw new IndexOutOfBoundsException("original_name can't to be empty");
		}
	}
	
	HandleName(ByteBuffer input_buffer) throws IOException {
		byte[] raw_name = new byte[SIZE];
		input_buffer.get(raw_name);
		name = new String(raw_name, MyDMAM.US_ASCII).trim();
		if (name.isEmpty()) {
			throw new IOException("name can't to be empty");
		}
	}
	
	/**
	 * Put SIZE bytes to output_buffer
	 */
	void toByteBuffer(ByteBuffer output_buffer) {
		output_buffer.put(StringUtils.rightPad(name, SIZE).getBytes(MyDMAM.US_ASCII));
	}
	
	public String toString() {
		return name;
	}
	
	public int hashCode() {
		return name.hashCode();
	}
	
	public boolean equals(Object obj) {
		return name.equals(obj);
	}
	
}
