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
 * Copyright (C) hdsdi3g for hd3g.tv 1 oct. 2017
 * 
*/
package hd3gtv.mydmam.embddb.store;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import hd3gtv.mydmam.embddb.store.FileData.Entry;

/**
 * Not thread safe !
 */
public class FileHashTableData {
	
	private final FileHashTable<Long> hash_table;
	private final FileData data;
	
	public FileHashTableData(File index_file, File data_file, int default_table_size) throws IOException {
		data = new FileData(data_file);
		
		BiConsumer<Long, ByteBuffer> factory_to_bytes = (pointer, buffer) -> {
			buffer.putLong(pointer);
		};
		
		Function<ByteBuffer, Long> factory_from_bytes = buffer -> {
			return buffer.getLong();
		};
		hash_table = new FileHashTable<>(index_file, factory_to_bytes, factory_from_bytes, 8, default_table_size);
	}
	
	public void clear() throws IOException {
		data.clear();
		hash_table.clear();
	}
	
	public int size() throws IOException {
		return hash_table.size();
	}
	
	public boolean isEmpty() throws IOException {
		return hash_table.isEmpty();
	}
	
	public boolean has(ItemKey key) throws IOException {
		return hash_table.has(key);
	}
	
	private long p(Long v) {
		if (v == null) {
			return 0l;
		}
		return v.longValue();
	}
	
	/**
	 * Internal datas will not removed (just tagged). Only references are removed.
	 */
	public void remove(ItemKey item_key) throws IOException {
		long old_pointer = p(hash_table.remove(item_key));
		if (old_pointer < 1) {
			return;
		}
		data.markDelete(old_pointer, item_key);
	}
	
	/**
	 * @return entry can be null if not found.
	 */
	public Entry getEntry(ItemKey key) throws IOException {
		long pointer = p(hash_table.getEntry(key));
		if (pointer < 1) {
			return null;
		}
		return data.read(pointer, key);
	}
	
	public void put(ItemKey item_key, byte[] user_data) throws IOException {
		long pointer = data.write(item_key, user_data);
		hash_table.put(item_key, pointer);
	}
	
	public Stream<ItemKey> forEachKeys() throws IOException {
		return hash_table.forEach().filter(v -> {
			return p(v.value) > 0;
		}).map(v -> {
			return v.key;
		});
	}
	
	public Stream<Entry> forEachKeyValue() throws IOException {
		return hash_table.forEach().filter(v -> {
			return p(v.value) > 0;
		}).map(v -> {
			try {
				return data.read(p(v.value), v.key);
			} catch (IOException e) {
				throw new RuntimeException("Can't read from data file", e);
			}
		});
	}
	
}
