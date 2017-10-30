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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.embddb.network.PoolManager;
import hd3gtv.mydmam.embddb.store.FileBackend;
import hd3gtv.mydmam.embddb.store.ItemFactory;
import hd3gtv.mydmam.embddb.store.ReadCache;
import hd3gtv.mydmam.embddb.store.ReadCacheEhcache;
import hd3gtv.mydmam.gson.GsonIgnore;
import hd3gtv.tools.ThreadPoolExecutorFactory;

/**
 * Mergue all IOs from Stores and all IOs from Network.
 */
@GsonIgnore
public class IOPipeline {
	private static Logger log = Logger.getLogger(IOPipeline.class);
	
	private final PoolManager poolmanager;
	private final ConcurrentHashMap<Class<?>, InternalStore> all_stores;
	private final String database_name;
	private final ReadCache read_cache;
	private final FileBackend store_file_backend;
	private final ThreadPoolExecutorFactory io_executor; // TODO re-do all tests with single Thread executor + can set executor size in configuration
	private final ThreadPoolExecutorFactory history_journal_write_executor;// TODO stop with IO_executor
	private final ScheduledExecutorService maintenance_scheduled_ex_service;
	
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
		
		history_journal_write_executor = new ThreadPoolExecutorFactory("Write History Journal", Thread.MIN_PRIORITY).setSimplePoolSize();
		store_file_backend = new FileBackend(durable_store_directory, poolmanager.getUUIDRef(), key -> {
			read_cache.remove(key);
		}, history_journal_write_executor);
		
		io_executor = new ThreadPoolExecutorFactory("EMBDDB Store", Thread.MIN_PRIORITY + 1, e -> {
			log.error("Genric error for EMBDDB Store", e);
		});
		
		maintenance_scheduled_ex_service = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setDaemon(false);
				t.setName("EMBDDB Database Maintenance");
				t.setPriority(Thread.MIN_PRIORITY);
				return t;
			}
		});
	}
	
	private class InternalStore {
		
		final DistributedStore<?> store;
		ScheduledFuture<?> maintenance_timer;
		
		InternalStore(DistributedStore<?> store) {
			this.store = store;
			
			maintenance_timer = maintenance_scheduled_ex_service.scheduleAtFixedRate(() -> {
				if (store.getRunningState() != RunningState.ON_THE_FLY) {
					return;
				}
				try {
					if (store.isJournalWriteCacheIsTooBig()) {
						store.doDurableWrites(); // <<<< this is global blocking
					}
					// TODO do cleanup, when ?
				} catch (Exception e) {
					log.error("Can't do maintenance operations for " + store + ". Cancel next operations.", e); // TODO terminate Store ?!
					cancelTimer();
				}
			}, 1, 10, TimeUnit.SECONDS);
		}
		
		private void cancelTimer() {
			if (maintenance_timer != null) {
				maintenance_timer.cancel(false);
			}
		}
		
		public int hashCode() {
			return store.hashCode();
		}
		
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			InternalStore other = (InternalStore) obj;
			if (!getOuterType().equals(other.getOuterType())) {
				return false;
			}
			if (store == null) {
				if (other.store != null) {
					return false;
				}
			} else if (!store.equals(other.store)) {
				return false;
			}
			return true;
		}
		
		private IOPipeline getOuterType() {
			return IOPipeline.this;
		}
	}
	
	/**
	 * Thread safe
	 */
	private <T> void storeRegister(Class<T> wrapped_class, DistributedStore<T> store) {
		InternalStore current_store = all_stores.putIfAbsent(wrapped_class, new InternalStore(store)); // TODO register for all Nodes
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
		InternalStore result = all_stores.get(wrapped_class);
		if (result == null) {
			return null;
		}
		@SuppressWarnings("unchecked")
		DistributedStore<T> r = (DistributedStore<T>) result.store;
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
	
	ThreadPoolExecutorFactory getIOExecutor() {
		return io_executor;
	}
	
	/**
	 * Thread safe
	 */
	private DistributedStore<?> getStoreByClassName(String wrapped_class_name) {
		Class<?> wrapped_class = MyDMAM.factory.getClassByName(wrapped_class_name);
		if (wrapped_class == null) {
			return null;
		}
		return all_stores.get(wrapped_class).store;
	}
	
	// TODO RequestHandler: register Store<T> & Node
	// TODO RequestHandler: unregister Store<T> & Node
	// TODO RequestHandler: keylist: Store<T>,get#page
	// TODO RequestHandler: keylist: Store<T>,#total,#actual,#page,page_count,key+taille,... stored by DStore and node
	// TODO RequestHandler: Key update pool list: #total,key,...
	// TODO RequestHandler: Key update pool content: #total,item,item,item,...
	
}
