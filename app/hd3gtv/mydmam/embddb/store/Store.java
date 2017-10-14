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
 * Copyright (C) hdsdi3g for hd3g.tv 11 sept. 2017
 * 
*/
package hd3gtv.mydmam.embddb.store;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.embddb.store.FileBackend.StoreBackend;
import hd3gtv.tools.ThreadPoolExecutorFactory;

/**
 * Polyvalent and agnostic object storage
 * Bind Backend and StoreItemFactory
 */
public final class Store<T> {
	private static Logger log = Logger.getLogger(Store.class);
	
	protected final String database_name;
	private final ReadCache read_cache;
	private final StoreBackend backend;
	private final ItemFactory<T> item_factory;
	// private final StoreQueue queue;
	private final Executor executor;
	
	private final ConcurrentHashMap<ItemKey, Item> journal_write_cache;
	private final long max_size_for_cached_commit_log;
	private final long grace_period_for_expired_items;
	private final String generic_class_name;
	
	/**
	 * @param read_cache dedicated cache for this store
	 */
	public Store(String database_name, ItemFactory<T> item_factory, FileBackend file_backend, ReadCache read_cache, long max_size_for_cached_commit_log, long grace_period_for_expired_items, int expected_item_count) throws IOException {
		this.database_name = database_name;
		if (database_name == null) {
			throw new NullPointerException("\"database_name\" can't to be null");
		}
		this.item_factory = item_factory;
		if (item_factory == null) {
			throw new NullPointerException("\"item_factory\" can't to be null");
		}
		if (file_backend == null) {
			throw new NullPointerException("\"file_backend\" can't to be null");
		}
		
		generic_class_name = item_factory.getType().getSimpleName();
		backend = file_backend.get(database_name, generic_class_name, expected_item_count);
		
		this.read_cache = read_cache;
		if (read_cache == null) {
			throw new NullPointerException("\"read_cache\" can't to be null");
		}
		journal_write_cache = new ConcurrentHashMap<>();
		this.max_size_for_cached_commit_log = max_size_for_cached_commit_log;
		if (max_size_for_cached_commit_log < 1l) {
			throw new NullPointerException("\"max_size_for_cached_commit_log\" can't to be < 1");
		}
		this.grace_period_for_expired_items = grace_period_for_expired_items;
		if (grace_period_for_expired_items < 1l) {
			throw new NullPointerException("\"grace_period_for_expired_items\" can't to be < 1");
		}
		
		ThreadPoolExecutorFactory thread_pool_executor = new ThreadPoolExecutorFactory("EMBDDB-Store-" + database_name + "_" + generic_class_name, Thread.MIN_PRIORITY + 1, e -> {
			log.error("Genric error for " + database_name + "/" + generic_class_name, e);
		});
		
		executor = thread_pool_executor;
		
		/* TODO queue = new StoreQueue(getDatabaseName() + "/" + generic_class_name, () -> {
			return elegiblityToCleanUp();
		}, () -> {
			try {
				journal_write_cache.clear();
				backend.doDurableWritesAndRotateJournal();
			} catch (IOException e1) {
				throw new RuntimeException("Can't do rotate", e1);
			}
		}, executor);*/
	}
	
	public CompletableFuture<Void> put(String _id, T element, long ttl, TimeUnit unit) {
		return put(_id, null, element, ttl, unit);
	}
	
	public CompletableFuture<Void> put(String _id, String path, T element, long ttl, TimeUnit unit) {
		return CompletableFuture.runAsync(() -> {
			try {
				put(item_factory.toItem(element).setPath(path).setId(_id).setTTL(unit.toMillis(ttl)));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, executor);
	}
	
	private void put(Item item) throws IOException {
		ItemKey key = item.getKey();
		journal_write_cache.put(key, item);
		read_cache.remove(key);
		backend.writeInJournal(item, item.getDeleteDate() + grace_period_for_expired_items);
	}
	
	/**
	 * @return null if not found
	 */
	public CompletableFuture<T> get(String _id) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				ItemKey key = new ItemKey(_id);
				
				Item item = journal_write_cache.get(key);
				if (item != null) {
					if (item.isDeleted() == false) {
						return item_factory.getFromItem(item);
					}
					return null;
				}
				
				item = read_cache.get(key);
				if (item != null) {
					return item_factory.getFromItem(item);
				}
				
				ByteBuffer read_buffer = backend.read(key);
				if (read_buffer != null) {
					item = new Item(read_buffer);
					read_cache.put(item);
					return item_factory.getFromItem(item);
				}
				
				return null;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}, executor);
	}
	
	/**
	 * Don't check item TTL/deleted, just reference presence.
	 */
	public CompletableFuture<Boolean> exists(String _id) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				ItemKey key = new ItemKey(_id);
				if (journal_write_cache.containsKey(key)) {
					return true;
				} else if (read_cache.has(key)) {
					return true;
				} else if (backend.contain(key)) {
					return true;
				} else {
					return false;
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, executor);
	}
	
	public CompletableFuture<Void> removeById(String _id) {
		return CompletableFuture.runAsync(() -> {
			try {
				ItemKey key = new ItemKey(_id);
				Item actual_item = null;
				
				if (journal_write_cache.containsKey(key)) {
					actual_item = journal_write_cache.get(key);
					if (actual_item.isDeleted()) {
						return;
					}
				} else {
					actual_item = read_cache.get(key);
					if (actual_item == null) {
						ByteBuffer read_buffer = backend.read(key);
						if (read_buffer == null) {
							return;
						}
						actual_item = new Item(read_buffer);
					}
				}
				
				put(actual_item.setPayload(new byte[0]).setTTL(-1l));
				return;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, executor);
	}
	
	private List<Item> mapToItemAndPushToReadCacheAndIsActuallyNotExistsInCommitLog(Stream<ByteBuffer> source) {
		return source.map(read_buffer -> {
			Item item = new Item(read_buffer);
			read_cache.put(item);
			return item;
		}).filter(item -> {
			return journal_write_cache.containsKey(item.getKey()) == false;
		}).collect(Collectors.toList());
	}
	
	private Stream<Item> accumulateWithCommitLog(List<Item> stored_items, HashSet<Item> commit_log_filtered) {
		stored_items.forEach(item -> {
			if (commit_log_filtered.contains(item) == false) {
				commit_log_filtered.add(item);
			}
		});
		return commit_log_filtered.stream();
	}
	
	public CompletableFuture<List<T>> getAll() {
		return CompletableFuture.supplyAsync(() -> {
			try {
				List<Item> stored_items = mapToItemAndPushToReadCacheAndIsActuallyNotExistsInCommitLog(backend.getAllDatas());
				
				return accumulateWithCommitLog(stored_items, new HashSet<>(journal_write_cache.values())).map(item -> {
					return item_factory.getFromItem(item);
				}).collect(Collectors.toList());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, executor);
	}
	
	private Stream<Item> internalGetByPath(String path) throws IOException {
		List<Item> stored_items = mapToItemAndPushToReadCacheAndIsActuallyNotExistsInCommitLog(backend.getDatasByPath(path));
		HashSet<Item> commit_log_filtered = new HashSet<Item>(journal_write_cache.values().stream().filter(item -> {
			return item.getPath().startsWith(path);
		}).collect(Collectors.toSet()));
		
		return accumulateWithCommitLog(stored_items, commit_log_filtered);
	}
	
	public CompletableFuture<List<T>> getByPath(String path) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return internalGetByPath(path).map(item -> {
					return item_factory.getFromItem(item);
				}).collect(Collectors.toList());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, executor);
	}
	
	private void remove(Stream<Item> items) {
		items.forEach(item -> {
			item.setPayload(new byte[0]);
			item.setTTL(-1);
			item.setPath(null);
			ItemKey key = item.getKey();
			journal_write_cache.put(key, item);
			read_cache.remove(key);
			try {
				backend.writeInJournal(item, System.currentTimeMillis() + grace_period_for_expired_items);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}
	
	public CompletableFuture<Void> removeAllByPath(String path) {
		return CompletableFuture.runAsync(() -> {
			try {
				remove(internalGetByPath(path));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, executor);
	}
	
	public CompletableFuture<Void> truncate() {
		return CompletableFuture.runAsync(() -> {
			try {
				List<Item> stored_items = mapToItemAndPushToReadCacheAndIsActuallyNotExistsInCommitLog(backend.getAllDatas());
				remove(stored_items.stream());
				remove(journal_write_cache.values().stream());
				read_cache.purgeAll();
				doDurableWrite();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, executor);
	}
	
	public String getDatabaseName() {
		return database_name;
	}
	
	private boolean elegiblityToCleanUp() {
		long estimate_commit_log_cache_size = journal_write_cache.reduceValuesToLong(MyDMAM.CPU_COUNT, item -> {
			return (long) item.estimateSize();
		}, 0, (l, r) -> {
			return l + r;
		});
		return estimate_commit_log_cache_size > max_size_for_cached_commit_log;
	}
	
	/**
	 * Blocking: this request is put in a pool, and will be executed after the next Store request.
	 */
	public void doDurableWrite() {
		// XXX queue.cleanUp();
	}
	
	// TODO network I/O
	// TODO do full file GC (with pause)
	
}
