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
 * Copyright (C) hdsdi3g for hd3g.tv 13 sept. 2017
 * 
*/
package hd3gtv.mydmam.embddb.store;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public abstract class Backend {// TODO set async
	
	// TODO Grand journal
	
	abstract void init(String database_name, String class_name);
	
	abstract void writeInCommitlog(ItemKey key, byte[] content) throws IOException;
	
	abstract void rotateAndReadCommitlog(BiConsumer<ItemKey, byte[]> all_reader) throws IOException;
	
	abstract void writeInDatabase(ItemKey key, byte[] content, String _id, String path, long delete_date) throws IOException;
	
	/**
	 * Remove all for delete_date < Now - grace_period
	 */
	abstract void removeOutdatedRecordsInDatabase(long grace_period) throws IOException;
	
	/**
	 * @return raw content
	 */
	abstract byte[] read(ItemKey key) throws IOException;
	
	abstract boolean contain(ItemKey key) throws IOException;
	
	/**
	 * @return raw content
	 */
	abstract Stream<byte[]> getAllDatas() throws IOException;
	
	/**
	 * @return raw content
	 */
	abstract Stream<byte[]> getDatasByPath(String path) throws IOException;
	
}
