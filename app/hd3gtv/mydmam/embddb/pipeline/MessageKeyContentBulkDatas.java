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
 * Copyright (C) hdsdi3g for hd3g.tv 13 nov. 2017
 * 
*/
package hd3gtv.mydmam.embddb.pipeline;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.embddb.network.DataBlock;
import hd3gtv.mydmam.embddb.network.Protocol;
import hd3gtv.mydmam.embddb.store.Item;
import hd3gtv.mydmam.gson.GsonIgnore;

@GsonIgnore
public class MessageKeyContentBulkDatas implements MessageDStoreMapper {
	private static Logger log = Logger.getLogger(MessageKeyContentBulkDatas.class);
	
	String database;
	String class_name;
	List<Item> items;
	
	public String getClassName() {
		return class_name;
	}
	
	public String getDatabase() {
		return database;
	}
	
	private transient ByteBuffer header;
	
	MessageKeyContentBulkDatas(DistributedStore<?> store, Stream<Item> items_to_send) {
		if (store == null) {
			throw new NullPointerException("\"store\" can't to be null");
		}
		database = store.getDatabaseName();
		class_name = store.getGenericClassName();
		
		byte[] db_name = database.getBytes(MyDMAM.UTF8);
		byte[] cls_name = class_name.getBytes(MyDMAM.UTF8);
		
		header = ByteBuffer.allocate(4 + db_name.length + 4 + cls_name.length);
		Item.writeNextBlock(header, db_name);
		Item.writeNextBlock(header, cls_name);
		
		AtomicInteger remain_payload_size = new AtomicInteger(Protocol.BUFFER_SIZE - (DataBlock.HEADER_SIZE + header.capacity() + RequestHandlerKeyContentBulkDatas.HANDLE_NAME.getBytes(MyDMAM.UTF8).length + 4/** item size */
				+ 1 /** 0 separator */
		));
		
		items = items_to_send.filter(item -> {
			return remain_payload_size.get() > item.getByteBufferWriteSize();
		}).peek(item -> {
			remain_payload_size.addAndGet(item.getByteBufferWriteSize());
		}).collect(Collectors.toList());
	}
	
	DataBlock makeDatablock(RequestHandlerKeyContentBulkDatas requester) {
		int total_space = items.stream().mapToInt(item -> item.getByteBufferWriteSize()).sum();
		
		ByteBuffer full_payload = ByteBuffer.allocate(header.capacity() + total_space + 4 + 1);
		
		header.flip();
		full_payload.put(header);
		full_payload.putInt(items.size());
		full_payload.put((byte) 0);
		
		items.forEach(item -> {
			try {
				item.toByteBuffer(full_payload);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		if (full_payload.hasRemaining()) {
			throw new RuntimeException("Invalid remaining: " + full_payload.remaining() + " instead of 0");
		}
		
		full_payload.flip();
		byte[] datas = new byte[full_payload.remaining()];
		full_payload.get(datas);
		return new DataBlock(requester, datas);
	}
	
	MessageKeyContentBulkDatas(DataBlock source) {
		ByteBuffer full_payload = ByteBuffer.wrap(source.getDatas());
		database = new String(Item.readNextBlock(full_payload), MyDMAM.UTF8);
		class_name = new String(Item.readNextBlock(full_payload), MyDMAM.UTF8);
		int item_size = full_payload.getInt();
		
		if (full_payload.get() != 0) {
			throw new RuntimeException("Invalid payload (bad separator)");
		}
		items = new ArrayList<>(item_size);
		
		while (full_payload.hasRemaining()) {
			items.add(new Item(full_payload));
		}
		
		if (items.size() != item_size) {
			throw new RuntimeException("Invalid payload (bad expected item count)");
		}
	}
	
}
