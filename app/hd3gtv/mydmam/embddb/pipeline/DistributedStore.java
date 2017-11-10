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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.embddb.network.Node;
import hd3gtv.mydmam.embddb.store.FileBackend;
import hd3gtv.mydmam.embddb.store.HistoryJournal.HistoryEntry;
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
	
	/**
	 * synchronizedList
	 */
	private final List<Node> external_dependant_nodes;
	private final ConcurrentHashMap<ItemKey, UpdateItem> update_list;
	
	private SavedStatus saved_status;
	private final File saved_status_file;
	// TODO2 item size > max block size > create a special exception for that
	
	// TODO3 check behavior if sync is outside grace period (make XML + destroy)
	
	DistributedStore(String database_name, ItemFactory<T> item_factory, FileBackend file_backend, ReadCache read_cache, long max_size_for_cached_commit_log, long grace_period_for_expired_items, int expected_item_count, Consistency consistency, IOPipeline pipeline) throws IOException {
		super(pipeline.getIOExecutor(), database_name, item_factory, file_backend, read_cache, max_size_for_cached_commit_log, grace_period_for_expired_items, expected_item_count);
		this.consistency = consistency;
		if (consistency == null) {
			throw new NullPointerException("\"consistency\" can't to be null");
		}
		
		this.pipeline = pipeline;
		
		external_dependant_nodes = Collections.synchronizedList(new ArrayList<>());
		running_state = RunningState.WAKE_UP;
		update_list = new ConcurrentHashMap<ItemKey, UpdateItem>();
		
		saved_status_file = backend.makeLocalFileName("saved_status.json");
		if (saved_status_file.exists()) {
			saved_status = MyDMAM.gson_kit.getGson().fromJson(FileUtils.readFileToString(saved_status_file, MyDMAM.UTF8), SavedStatus.class);
		} else {
			saved_status = new SavedStatus();
			saved_status.save(saved_status_file);
		}
		
		if (saved_status.last_sync_date + grace_period_for_expired_items < System.currentTimeMillis()) {
			/**
			 * Grace period expired (or first use of this DStore): sync all.
			 */
			clear();
			pipeline.doAClusterDataSync(item_factory.getType(), 0);
		} else {
			/**
			 * Grace period still valid, sync only items from last_sync_date
			 */
			pipeline.doAClusterDataSync(item_factory.getType(), saved_status.last_sync_date);
		}
		
	}
	
	/*
	 * =========== Tools zone ===========
	 * */
	
	public RunningState getRunningState() {
		return running_state;
	}
	
	boolean addExternalDependantNode(Node new_node) {
		synchronized (external_dependant_nodes) {
			if (external_dependant_nodes.contains(new_node) == false) {
				return external_dependant_nodes.add(new_node);
			}
		}
		return false;
	}
	
	boolean removeExternalDependantNode(Node old_node) {
		synchronized (external_dependant_nodes) {
			if (external_dependant_nodes.contains(old_node)) {
				return external_dependant_nodes.remove(old_node);
			}
		}
		return false;
	}
	
	Stream<Node> getExternalDependantNodes() {
		return external_dependant_nodes.stream();
	}
	
	String getGenericClassName() {
		return this.item_factory.getType().getName();
	}
	
	/*
	 * =========== RunningState.WAKE_UP ZONE ===========
	 * */
	
	static class SavedStatus {
		
		private long last_sync_date;// TODO update on state change
		
		private SavedStatus() {
		}
		
		private void save(File to_file) throws IOException {
			FileUtils.writeStringToFile(to_file, MyDMAM.gson_kit.getGsonSimple().toJson(this), MyDMAM.UTF8);
		}
		
		long getLastSyncDate() {
			return last_sync_date;
		}
	}
	
	SavedStatus getSavedStatus() {
		return saved_status;
	}
	
	void switchRunningState(RunningState new_state) {
		switch (running_state) {
		case WAKE_UP:
			running_state = new_state;
			break;
		case SYNC_LAST:
			if (new_state == RunningState.WAKE_UP) {
				throw new RuntimeException("Can't go back running state from SYNC_LAST to WAKE_UP");
			}
			running_state = RunningState.ON_THE_FLY;
			break;
		case ON_THE_FLY:
			if (new_state == RunningState.WAKE_UP) {
				throw new RuntimeException("Can't go back running state from ON_THE_FLY to WAKE_UP");
			} else if (new_state == RunningState.SYNC_LAST) {
				throw new RuntimeException("Can't go back running state from ON_THE_FLY to SYNC_LAST");
			}
		}
	}
	
	/*
	 * =========== RunningState.SYNC_LAST ZONE ===========
	 * */
	
	private class UpdateItem {
		long size;
		List<Node> requested_nodes;
	}
	
	/*
	 * =========== RunningState.ON_THE_FLY ZONE ===========
	 * */
	
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
		}, executor); // TODO2 add sync update (here) + add async update (IOPipeline queue, regular pushs, low priority)
	}
	
	public CompletableFuture<String> put(String _id, T element, long ttl, TimeUnit unit) {
		return waitToCanAccessToDatas().thenComposeAsync(v -> {
			return super.put(_id, element, ttl, unit);
		}, executor); // TODO2 add sync update (here) + add async update (IOPipeline queue, regular pushs, low priority)
	}
	
	public CompletableFuture<Integer> removeAllByPath(String path) {
		return waitToCanAccessToDatas().thenComposeAsync(v -> {
			return super.removeAllByPath(path);
		}, executor); // TODO2 add sync update (here) + add async update (IOPipeline queue, regular pushs, low priority)
	}
	
	public CompletableFuture<String> removeById(String _id) {
		return waitToCanAccessToDatas().thenComposeAsync(v -> {
			return super.removeById(_id);
		}, executor); // TODO2 add sync update (here) + add async update (IOPipeline queue, regular pushs, low priority)
	}
	
	public void truncate() throws Exception {
		waitToCanAccessToDatas().get();
		super.clear(); // TODO2 add sync update (here) + add async update (IOPipeline queue, regular pushs, low priority)
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
	
	Stream<HistoryEntry> getLastHistoryJournalEntries(long since_date) throws IOException {
		return backend.getHistoryJournal().getAllSince(since_date);
	}
	
}
