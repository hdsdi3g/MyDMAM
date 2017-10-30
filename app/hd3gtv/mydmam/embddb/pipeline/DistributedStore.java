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
package hd3gtv.mydmam.embddb.pipeline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import hd3gtv.mydmam.embddb.network.Node;
import hd3gtv.mydmam.embddb.store.FileBackend;
import hd3gtv.mydmam.embddb.store.Item;
import hd3gtv.mydmam.embddb.store.ItemFactory;
import hd3gtv.mydmam.embddb.store.ItemKey;
import hd3gtv.mydmam.embddb.store.ReadCache;
import hd3gtv.mydmam.embddb.store.Store;
import hd3gtv.mydmam.gson.GsonIgnore;

/**
 * Durable storage of T for this node and all connected nodes
 */
@GsonIgnore
public class DistributedStore<T> extends Store<T> {
	private static Logger log = Logger.getLogger(DistributedStore.class);
	
	public final Consistency consistency;
	private volatile RunningState running_state;
	private final IOPipeline pipeline;
	private final ArrayList<Node> external_dependant_nodes;
	private final ConcurrentHashMap<ItemKey, UpdateItem> update_list;
	
	// TODO block items size > max block size > create a special exception for that
	
	DistributedStore(String database_name, ItemFactory<T> item_factory, FileBackend file_backend, ReadCache read_cache, long max_size_for_cached_commit_log, long grace_period_for_expired_items, int expected_item_count, Consistency consistency, IOPipeline pipeline) throws IOException {
		super(pipeline.getIOExecutor(), database_name, item_factory, file_backend, read_cache, max_size_for_cached_commit_log, grace_period_for_expired_items, expected_item_count);
		this.consistency = consistency;
		if (consistency == null) {
			throw new NullPointerException("\"consistency\" can't to be null");
		}
		
		this.pipeline = pipeline;
		external_dependant_nodes = new ArrayList<>();// TODO update registed nodes
		running_state = RunningState.WAKE_UP;// TODO update RunningState
		update_list = new ConcurrentHashMap<ItemKey, UpdateItem>();
	}
	
	public RunningState getRunningState() {
		return running_state;
	}
	
	private CompletableFuture<Void> waitToCanAccessToDatas() {
		return CompletableFuture.runAsync(() -> {
			while (running_state.canAccessToDatas() == false) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}, executor);
	}
	
	public CompletableFuture<String> put(String _id, String path, T element, long ttl, TimeUnit unit) {
		return waitToCanAccessToDatas().thenComposeAsync(v -> {
			return super.put(_id, path, element, ttl, unit);
		}, executor); // TODO add sync update (here) + add async update (IOPipeline queue, regular pushs, low priority)
	}
	
	public CompletableFuture<String> put(String _id, T element, long ttl, TimeUnit unit) {
		return waitToCanAccessToDatas().thenComposeAsync(v -> {
			return super.put(_id, element, ttl, unit);
		}, executor); // TODO add sync update (here) + add async update (IOPipeline queue, regular pushs, low priority)
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
		return waitToCanAccessToDatas().thenComposeAsync(v -> {
			return super.removeAllByPath(path);
		}, executor); // TODO add sync update (here) + add async update (IOPipeline queue, regular pushs, low priority)
	}
	
	public CompletableFuture<String> removeById(String _id) {
		return waitToCanAccessToDatas().thenComposeAsync(v -> {
			return super.removeById(_id);
		}, executor); // TODO add sync update (here) + add async update (IOPipeline queue, regular pushs, low priority)
	}
	
	public void truncate() throws Exception {
		waitToCanAccessToDatas().get();
		super.truncate(); // TODO add sync update (here) + add async update (IOPipeline queue, regular pushs, low priority)
	}
	
	private class UpdateItem {
		long size;
		List<Node> requested_nodes;
	}
	
	public CompletableFuture<T> get(String _id) {
		return waitToCanAccessToDatas().thenComposeAsync(v -> {
			return super.get(_id);
		}, executor);
	}
	
	public CompletableFuture<Map<String, T>> getAllIDs() {
		return waitToCanAccessToDatas().thenComposeAsync(v -> {
			return super.getAllIDs();
		}, executor);
	}
	
	public CompletableFuture<Map<ItemKey, T>> getAllkeys() {
		return waitToCanAccessToDatas().thenComposeAsync(v -> {
			return super.getAllkeys();
		}, executor);
	}
	
	public CompletableFuture<List<T>> getAllValues() {
		return waitToCanAccessToDatas().thenComposeAsync(v -> {
			return super.getAllValues();
		}, executor);
	}
	
	public CompletableFuture<List<T>> getByPath(String path) {
		return waitToCanAccessToDatas().thenComposeAsync(v -> {
			return super.getByPath(path);
		}, executor);
	}
	
	public CompletableFuture<Item> getItem(String _id) {
		return waitToCanAccessToDatas().thenComposeAsync(v -> {
			return super.getItem(_id);
		}, executor);
	}
	
	public CompletableFuture<Stream<Item>> getItemsByPath(String path) {
		return waitToCanAccessToDatas().thenComposeAsync(v -> {
			return super.getItemsByPath(path);
		}, executor);
	}
	
	public CompletableFuture<Boolean> hasPresence(String _id) {
		return waitToCanAccessToDatas().thenComposeAsync(v -> {
			return super.hasPresence(_id);
		}, executor);
	}
	
}
