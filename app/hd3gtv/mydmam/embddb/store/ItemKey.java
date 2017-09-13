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
package hd3gtv.mydmam.embddb.store;

import java.util.Arrays;

import com.google.common.hash.Hashing;

import hd3gtv.mydmam.MyDMAM;

/**
 * A byte[] wrapper
 */
final class ItemKey {
	
	final byte[] key;
	
	ItemKey(byte[] key) {
		this.key = key;
	}
	
	/**
	 * MurmurHash3_x64_128 (Murmur3F) on _id
	 */
	ItemKey(String _id) {
		this.key = Hashing.murmur3_128().hashString(_id).asBytes();
	}
	
	public String toString() {
		return MyDMAM.byteToString(key);
	}
	
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(key);
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
		ItemKey other = (ItemKey) obj;
		if (!Arrays.equals(key, other.key)) {
			return false;
		}
		return true;
	}
	
}
