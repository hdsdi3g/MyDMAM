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

/**
 * Not thread safe !
 */
public class FileHashTableLong extends FileHashTable<Long> {
	
	final static BiConsumer<Long, ByteBuffer> factory_to_bytes = (pointer, buffer) -> {
		buffer.putLong(pointer);
	};
	
	final static Function<ByteBuffer, Long> factory_from_bytes = buffer -> {
		return buffer.getLong();
	};
	
	public FileHashTableLong(File index_file, int default_table_size) throws IOException {
		super(index_file, factory_to_bytes, factory_from_bytes, 8, default_table_size);
	}
	
	private static final long checkLong(Long v) {
		if (v == null) {
			return 0l;
		}
		return v.longValue();
	}
	
	/**
	 * @return never null
	 * @see hd3gtv.mydmam.embddb.store.FileHashTable#remove(hd3gtv.mydmam.embddb.store.ItemKey)
	 */
	public Long remove(ItemKey item_key) throws IOException {
		return checkLong(super.remove(item_key));
	}
	
	/**
	 * @return never null
	 */
	public Long getEntry(ItemKey key) throws IOException {
		return checkLong(super.getEntry(key));
	}
	
	/**
	 * @return only values > 0
	 */
	public Stream<FileHashTable<Long>.EntryValue> forEach() throws IOException {
		return super.forEach().filter(entry -> {
			return checkLong(entry.value) > 0;
		});
	}
	
}
