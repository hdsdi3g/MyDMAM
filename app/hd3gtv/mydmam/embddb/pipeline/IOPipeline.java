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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.embddb.network.PoolManager;
import hd3gtv.mydmam.embddb.store.FileBackend;
import hd3gtv.mydmam.embddb.store.ItemFactory;
import hd3gtv.mydmam.embddb.store.ReadCache;
import hd3gtv.mydmam.embddb.store.ReadCacheEhcache;
import hd3gtv.mydmam.gson.GsonIgnore;

/**
 * Mergue all IOs from Stores and all IOs from Network.
 */
@GsonIgnore
public class IOPipeline {
	private static Logger log = Logger.getLogger(IOPipeline.class);
	
	private final PoolManager poolmanager;
	private final ConcurrentHashMap<Class<?>, DistributedStore<?>> all_stores;
	private final String database_name;
	private final FileBackend store_file_backend;
	private final ReadCache read_cache;
	
	public IOPipeline(PoolManager poolmanager, String database_name, File durable_store_directory) throws IOException {
		this.poolmanager = poolmanager;
		if (poolmanager == null) {
			throw new NullPointerException("\"poolmanager\" can't to be null");
		}
		this.database_name = database_name;
		if (database_name == null) {
			throw new NullPointerException("\"database_name\" can't to be null");
		}
		all_stores = new ConcurrentHashMap<>();
		
		read_cache = ReadCacheEhcache.getCache();
		store_file_backend = new FileBackend(durable_store_directory, poolmanager.getUUIDRef(), key -> {
			read_cache.remove(key);
		});
		
		// TODO use executor ?
	}
	
	/**
	 * Thread safe
	 */
	private <T> void storeRegister(Class<T> wrapped_class, DistributedStore<T> store) {
		DistributedStore<?> current_store = all_stores.putIfAbsent(wrapped_class, store); // TODO register for all Nodes
		if (current_store != null) {
			throw new RuntimeException("A store was previousely added for class " + wrapped_class);
		}
	}
	
	/**
	 * Thread safe
	 */
	CompletableFuture<Void> storeUnregister(Class<?> wrapped_class) {
		all_stores.remove(wrapped_class);
		return CompletableFuture.completedFuture(null); // TODO unregister for all Nodes
	}
	
	/**
	 * Thread safe
	 */
	private <T> DistributedStore<T> getStoreByClass(Class<T> wrapped_class) {
		DistributedStore<?> result = all_stores.get(wrapped_class);
		if (result == null) {
			return null;
		}
		@SuppressWarnings("unchecked")
		DistributedStore<T> r = (DistributedStore<T>) result;
		return r;
	}
	
	public <T> DistributedStore<T> createStore(ItemFactory<T> item_factory, long max_size_for_cached_commit_log, long grace_period_for_expired_items, int expected_item_count, Consistency consistency) throws IOException {
		DistributedStore<T> previous = getStoreByClass(item_factory.getType());
		if (previous != null) {
			return previous;
		} else {
			DistributedStore<T> result = new DistributedStore<>(database_name, item_factory, store_file_backend, read_cache, max_size_for_cached_commit_log, grace_period_for_expired_items, expected_item_count, consistency, this);
			storeRegister(item_factory.getType(), result);
			return result;
		}
	}
	
	/**
	 * Thread safe
	 */
	private DistributedStore<?> getStoreByClassName(String wrapped_class_name) {
		Class<?> wrapped_class = MyDMAM.factory.getClassByName(wrapped_class_name);
		if (wrapped_class == null) {
			return null;
		}
		return all_stores.get(wrapped_class);
	}
	
	// TODO RequestHandler: register Store<T> & Node
	// TODO RequestHandler: unregister Store<T> & Node
	// TODO RequestHandler: keylist: Store<T>,get#page
	// TODO RequestHandler: keylist: Store<T>,#total,#actual,#page,page_count,key+taille,... stored by DStore and node
	// TODO RequestHandler: Key update pool list: #total,key,...
	// TODO RequestHandler: Key update pool content: #total,item,item,item,...
	
}
