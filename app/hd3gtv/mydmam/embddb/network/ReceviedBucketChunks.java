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
 * Copyright (C) hdsdi3g for hd3g.tv 23 nov. 2017
 * 
*/
package hd3gtv.mydmam.embddb.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

class ReceviedBucketChunks {
	private static Logger log = Logger.getLogger(ReceviedBucketChunks.class);
	
	private final DataBlock data_block;
	private final int expected_recevied_chunks_size;
	private final ArrayList<ByteBuffer> recevied_chunks;
	private final Node from_node;
	private final Consumer<ByteBuffer> onAllReceviedDone;
	
	ReceviedBucketChunks(Node from_node, int expected_recevied_chunks_size, ByteBuffer first_chunk, Consumer<ByteBuffer> onAllReceviedDone) throws IOException {
		this.expected_recevied_chunks_size = expected_recevied_chunks_size;
		recevied_chunks = new ArrayList<>(1);
		this.from_node = from_node;
		if (from_node == null) {
			throw new NullPointerException("\"from_node\" can't to be null");
		}
		this.onAllReceviedDone = onAllReceviedDone;
		if (onAllReceviedDone == null) {
			throw new NullPointerException("\"onAllReceviedDone\" can't to be null");
		}
		data_block = new DataBlock(expected_recevied_chunks_size);
		update(first_chunk);
	}
	
	private int getCurrentBucketSize() {
		return recevied_chunks.stream().mapToInt(b_buffer -> b_buffer.remaining()).sum();
	}
	
	synchronized void update(ByteBuffer next_chunk) throws IOException {
		recevied_chunks.add(next_chunk);
		// data_block.recevieDatas(next_chunk); XXX
		
		int current_size = getCurrentBucketSize();
		
		if (current_size == expected_recevied_chunks_size) {
			/**
			 * From now, header is fully readed
			 */
			ByteBuffer all_datas = ByteBuffer.allocate(current_size);
			recevied_chunks.forEach(b -> {
				all_datas.put(b);
			});
			onAllReceviedDone.accept(all_datas);
			
		} else if (current_size > expected_recevied_chunks_size) {
			throw new IOException("Invalid chunk from " + from_node + ": to big " + current_size + " bytes > " + expected_recevied_chunks_size);
		}
	}
	
	/*try { TODO callback
		DataBlock block = new DataBlock(pool_manager.getProtocol(), datas);
		pool_manager.getAllRequestHandlers().onReceviedNewBlock(block, node);
		pressure_measurement_recevied.onDatas(datas.length, System.currentTimeMillis() - start_time);
	} catch (IOException e) {
		if (e instanceof WantToCloseLinkException) {
			log.debug("Handler want to close link");
			close(getClass());
			pressure_measurement_recevied.onDatas(datas.length, System.currentTimeMillis() - start_time);
			return;
		} else {
			log.error("Can't extract sended blocks " + toString(), e);
			close(getClass());
		}
	}*/
	
	/**
	 * Correctly or not.
	 */
	synchronized boolean isDone() {
		return getCurrentBucketSize() >= expected_recevied_chunks_size;
	}
	
}
