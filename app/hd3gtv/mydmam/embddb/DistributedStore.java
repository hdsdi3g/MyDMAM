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
 * Copyright (C) hdsdi3g for hd3g.tv 19 oct. 2017
 * 
*/
package hd3gtv.mydmam.embddb;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import hd3gtv.mydmam.embddb.store.FileBackend;
import hd3gtv.mydmam.embddb.store.ItemFactory;
import hd3gtv.mydmam.embddb.store.ReadCache;
import hd3gtv.mydmam.embddb.store.Store;
import hd3gtv.mydmam.gson.GsonIgnore;

/**
 * Durable storage of T for this node and all connected nodes
 */
@GsonIgnore
public class DistributedStore<T> extends Store<T> {
	private static Logger log = Logger.getLogger(DistributedStore.class);
	
	private final Consistency consistency;
	private final IOPipeline pipeline; // TODO raw push/pull
	
	DistributedStore(String database_name, ItemFactory<T> item_factory, FileBackend file_backend, ReadCache read_cache, long max_size_for_cached_commit_log, long grace_period_for_expired_items, int expected_item_count, Consistency consistency, IOPipeline pipeline) throws IOException {
		super(database_name, item_factory, file_backend, read_cache, max_size_for_cached_commit_log, grace_period_for_expired_items, expected_item_count);
		this.consistency = consistency;
		if (consistency == null) {
			throw new NullPointerException("\"consistency\" can't to be null");
		}
		this.pipeline = pipeline;
		if (pipeline == null) {
			throw new NullPointerException("\"pipeline\" can't to be null");
		}
		pipeline.storeRegister(item_factory.getType(), this);
	}
	
	public CompletableFuture<String> put(String _id, String path, T element, long ttl, TimeUnit unit) {
		// TODO Auto-generated method stub
		return super.put(_id, path, element, ttl, unit);
	}
	
	public CompletableFuture<String> put(String _id, T element, long ttl, TimeUnit unit) {
		// TODO Auto-generated method stub
		return super.put(_id, element, ttl, unit);
	}
	
	/**
	 * Blocking
	 */
	public void close() {
		CompletableFuture<Void> on_close = pipeline.storeUnregister(item_factory.getType());
		super.close();
		try {
			on_close.get();
		} catch (InterruptedException | ExecutionException e) {
			log.error("Can't close Store in pipeline", e);
		}
	}
	
	public CompletableFuture<Integer> removeAllByPath(String path) {
		// TODO Auto-generated method stub
		return super.removeAllByPath(path);
	}
	
	public CompletableFuture<String> removeById(String _id) {
		// TODO Auto-generated method stub
		return super.removeById(_id);
	}
	
	public void truncate() throws Exception {
		// TODO Auto-generated method stub
		super.truncate();
	}
	
	Consistency getConsistency() {
		return consistency;
	}
	
	// TODO delta database/publish nodes
	// TODO space management
}
