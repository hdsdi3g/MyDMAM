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
import java.util.HashMap;

public class FileIndexDates {
	
	private final FileHashTableLong hash_table;
	
	public FileIndexDates(File index_file, int default_table_size) throws IOException {
		hash_table = new FileHashTableLong(index_file, default_table_size);
	}
	
	public void close() throws IOException {
		hash_table.close();
	}
	
	public void clear() throws IOException {
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
	
	/**
	 * Internal datas will not removed (just tagged). Only references are removed.
	 */
	public void remove(ItemKey item_key) throws IOException {
		hash_table.remove(item_key);
	}
	
	public long get(ItemKey key) throws IOException {
		return hash_table.getEntry(key);
	}
	
	public void put(ItemKey item_key, long date) throws IOException {
		hash_table.put(item_key, date);
	}
	
	public HashMap<ItemKey, Long> getAll() throws IOException {
		HashMap<ItemKey, Long> result = new HashMap<>();
		hash_table.stream().forEach(v -> {
			result.put(v.key, v.value);
		});
		return result;
	}
	
	/*public List<ItemKey> getPastKeys(long relative_date) throws IOException {
		return hash_table.stream().filter(v -> {
			return v.value < relative_date;
		}).map(v -> {
			return v.key;
		}).collect(Collectors.toList());** Less risky during a GC session *
	}*/
	
	/*public List<ItemKey> getFutureKeys(long relative_date) throws IOException {
		return hash_table.stream().filter(v -> {
			return v.value > relative_date;
		}).map(v -> {
			return v.key;
		}).collect(Collectors.toList());** Less risky during a GC session *
	}*/
	
	void purge() throws IOException {
		hash_table.purge();
	}
	
}
