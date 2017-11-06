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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.embddb.network.Node;
import hd3gtv.mydmam.embddb.network.PoolManager;
import hd3gtv.mydmam.embddb.pipeline.MessageRegisterStore.Action;
import hd3gtv.mydmam.embddb.store.FileBackend;
import hd3gtv.mydmam.embddb.store.HistoryJournal.HistoryEntry;
import hd3gtv.mydmam.embddb.store.ItemFactory;
import hd3gtv.mydmam.embddb.store.ReadCache;
import hd3gtv.mydmam.embddb.store.ReadCacheEhcache;
import hd3gtv.mydmam.gson.GsonIgnore;
import hd3gtv.tools.ThreadPoolExecutorFactory;

/**
 * Mergue all IOs from Stores and all IOs from Network.
 */
public class IOPipeline {
	private static Logger log = Logger.getLogger(IOPipeline.class);
	
	@GsonIgnore
	private final PoolManager poolmanager;
	@GsonIgnore
	private final ReadCache read_cache;
	
	private final ConcurrentHashMap<Class<?>, InternalStore> all_stores;
	private final String database_name;
	private final FileBackend store_file_backend;
	private final ThreadPoolExecutorFactory io_executor; // TODO4 re-do all tests with single Thread executor + can set executor size in configuration
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
		
		store_file_backend = new FileBackend(durable_store_directory, key -> {
			read_cache.remove(key);
		});
		
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
		
		poolmanager.addRequestHandler(new RequestHandlerRegisterStore(poolmanager, this));
		poolmanager.addRequestHandler(new RequestHandlerKeylistBuild(poolmanager, this));
		poolmanager.addRequestHandler(new RequestHandlerKeyListUpdate(poolmanager, this));
	}
	
	private class InternalStore {
		
		final DistributedStore<?> store;
		ScheduledFuture<?> maintenance_timer;
		ScheduledFuture<?> sync_timeout;
		
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
					// TODO3 do cleanup, when ?
				} catch (Exception e) {
					log.error("Can't do maintenance operations for " + store + ". Cancel next operations.", e); // TODO3 terminate Store ?!
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
		InternalStore current_store = all_stores.putIfAbsent(wrapped_class, new InternalStore(store));
		if (current_store != null) {
			throw new RuntimeException("A store was previousely added for class " + wrapped_class);
		}
	}
	
	/**
	 * Thread safe
	 */
	CompletableFuture<Void> storeUnregister(Class<?> wrapped_class) {
		InternalStore old_store = all_stores.remove(wrapped_class);
		if (old_store != null) {
			return CompletableFuture.runAsync(() -> {
				// TODO unregister for all Nodes: old_store.store.getExternalDependantNodes()
			}, this.io_executor);
		}
		return CompletableFuture.completedFuture(null);
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
	
	/*
	 * Thread safe
	private DistributedStore<?> getStoreByClassName(String wrapped_class_name) {
		Class<?> wrapped_class = MyDMAM.factory.getClassByName(wrapped_class_name);
		if (wrapped_class == null) {
			return null;
		}
		return all_stores.get(wrapped_class).store;
	}*/
	
	/**
	 * @return can be null
	 */
	private InternalStore getInternalStoreFromRequestHandlerMessage(MessageDStoreMapper message, Node from_node) {
		if (message == null) {
			throw new NullPointerException("\"message\" can't to be null");
		}
		if (from_node == null) {
			throw new NullPointerException("\"from_node\" can't to be null");
		}
		
		String database = message.getDatabase();
		if (database == null) {
			throw new NullPointerException("\"database\" can't to be null");
		}
		
		String class_name = message.getClassName();
		if (class_name == null) {
			throw new NullPointerException("\"class_name\" can't to be null");
		}
		
		if (database.equals(database_name) == false) {
			if (log.isDebugEnabled()) {
				log.debug("Get message form " + from_node + " and if want to sync with database \"" + database + "\". This instance is configured with database \"" + database_name + "\"");
			}
			return null;
		}
		
		final Class<?> registed_sync_class = MyDMAM.factory.getClassByName(class_name);
		if (registed_sync_class == null) {
			log.warn("Get message form " + from_node + " to sync with class " + class_name + ", but this class is not accessible by this JVM instance. Ignore it.");
			return null;
		}
		
		if (all_stores.contains(registed_sync_class) == false) {
			if (log.isTraceEnabled()) {
				log.trace("Get message form " + from_node + " to sync with class " + registed_sync_class + ", but it's not actually used here. Ignore it.");
			}
			return null;
		}
		
		return all_stores.get(registed_sync_class);
	}
	
	void onExternalRegisterStore(MessageRegisterStore message, Node from_node) {
		InternalStore i_store = getInternalStoreFromRequestHandlerMessage(message, from_node);
		if (i_store == null) {
			return;
		}
		
		/**
		 * TODO Karnaugh table message.running_state / DStore.running_state, REGISTER/UNREGISTER
		 * L'idée est de savoir si on déclenche ou non un update list depuis ce node.
		 * ~~~~~~If actual DStore is in WAKE_UP:~~~~~~
		 * - add from_node in DStore ExternalDependantNodes
		 * ...
		 * Else
		 * ...
		 */
		
		/**
		 * if WAKE_UP !
		 * Once has a return, release sync_timeout
		 */
		if (i_store.sync_timeout != null) {
			if (i_store.sync_timeout.isDone() == false) {
				i_store.sync_timeout.cancel(true);
			}
		}
		
		/**
		 * TODO Si on est en SYNC_LAST, choisir le node qui a le ping le plus petit (ou le premier), la liste des items depuis i_store.store.getSavedStatus().getLastSyncDate()
		 * TODO Lancer un sync_timeout après la premiere demande.
		 */
	}
	
	void doAClusterDataSync(Class<?> store_class, long since_date) {
		if (all_stores.contains(store_class) == false) {
			throw new IndexOutOfBoundsException("Can't found registed class " + store_class);
		}
		InternalStore internal = all_stores.get(store_class);
		if (internal.store.getRunningState() == RunningState.WAKE_UP) {
			List<Node> requested_nodes = poolmanager.sayToAllNodes(RequestHandlerRegisterStore.class, new MessageRegisterStore(Action.REGISTER, database_name, store_class, RunningState.WAKE_UP));
			if (requested_nodes.isEmpty()) {
				log.info("Simplified wake up procedure for " + store_class.getSimpleName() + ": no active nodes");
				// TODO switch to RunningState.ON_THE_FLY
				return;
			}
			
			log.info("Wake up procedure for " + store_class.getSimpleName() + " on " + requested_nodes.size() + " node(s)...");
			
			if (internal.sync_timeout != null) {
				if (internal.sync_timeout.isDone() == false) {
					internal.sync_timeout.cancel(true);
				}
			}
			
			long slower_node_delay = requested_nodes.stream().mapToLong(n -> {
				return Math.abs(n.getLastDeltaTime());
			}).max().orElse(1000);
			
			if (log.isTraceEnabled()) {
				log.trace("Arm sync_timeout schedule function in " + slower_node_delay * 2l + " ms");
			}
			
			internal.sync_timeout = maintenance_scheduled_ex_service.schedule(() -> {
				log.info("No one other node wants to sync with " + database_name + "/" + store_class.getSimpleName());
				// TODO switch to RunningState.ON_THE_FLY
			}, slower_node_delay * 2l, TimeUnit.MILLISECONDS);
		} else {
			// TODO send to all ExternalDependantNodes a cluster sync
		}
	}
	
	void onExternalKeylistBuild(MessageKeylistBuild message, Node source_node) throws IOException {
		InternalStore i_store = getInternalStoreFromRequestHandlerMessage(message, source_node);
		if (i_store == null) {
			return;
		}
		Stream<HistoryEntry> h_entries = i_store.store.getLastHistoryJournalEntries(message.since_date);
		MessageKeylistUpdate response = new MessageKeylistUpdate(message.getDatabase(), message.getClassName(), h_entries.collect(Collectors.toList()));
		source_node.sendRequest(RequestHandlerKeyListUpdate.class, response);
	}
	
	void onExternalKeylistUpdate(MessageKeylistUpdate message, Node source_node) {
		InternalStore i_store = getInternalStoreFromRequestHandlerMessage(message, source_node);
		if (i_store == null) {
			return;
		}
		// TODO get an update list: compare with actual items, and prepare an bulk update data
		
	}
	
	// TODO RequestHandler: bulk update data list: #total,key,key,key,key,key,...
	// TODO RequestHandler: bulk update data content: #total,item,item,item,...
	
}
