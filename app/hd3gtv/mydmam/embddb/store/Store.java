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
	private final StoreQueue queue;
	
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
		
		ThreadPoolExecutorFactory executor = new ThreadPoolExecutorFactory("EMBDDB-Store-" + database_name + "_" + generic_class_name, Thread.MIN_PRIORITY + 1, e -> {
			log.error("Genric error for " + database_name + "/" + generic_class_name, e);
		});
		
		queue = new StoreQueue(getDatabaseName() + "/" + generic_class_name, () -> {
			return elegiblityToCleanUp();
		}, () -> {
			try {
				journal_write_cache.clear();
				backend.doDurableWritesAndRotateJournal();
			} catch (IOException e1) {
				throw new RuntimeException("Can't do rotate", e1);
			}
		}, executor);
	}
	
	public CompletableFuture<Void> put(String _id, T element, long ttl, TimeUnit unit) {
		return put(_id, null, element, ttl, unit);
	}
	
	public CompletableFuture<Void> put(String _id, String path, T element, long ttl, TimeUnit unit) {
		CompletableFuture<Void> result = new CompletableFuture<>();
		
		queue.put(() -> {
			try {
				put(item_factory.toItem(element).setPath(path).setId(_id).setTTL(unit.toMillis(ttl)));
				result.complete(null);
			} catch (Exception e) {
				result.completeExceptionally(e);
			}
		});
		
		return result;
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
		CompletableFuture<T> result = new CompletableFuture<>();
		
		queue.put(() -> {
			try {
				ItemKey key = new ItemKey(_id);
				
				Item item = journal_write_cache.get(key);
				if (item != null) {
					if (item.isDeleted() == false) {
						result.complete(item_factory.getFromItem(item));
					} else {
						result.complete(null);
					}
					return;
				}
				
				item = read_cache.get(key);
				if (item != null) {
					result.complete(item_factory.getFromItem(item));
					return;
				}
				
				ByteBuffer read_buffer = backend.read(key);
				if (read_buffer != null) {
					item = new Item(read_buffer);
					read_cache.put(item);
					result.complete(item_factory.getFromItem(item));
					return;
				}
				
				result.complete(null);
			} catch (IOException e) {
				result.completeExceptionally(e);
			}
		});
		
		return result;
	}
	
	/**
	 * Don't check item TTL/deleted, just reference presence.
	 */
	public CompletableFuture<Boolean> exists(String _id) {
		CompletableFuture<Boolean> result = new CompletableFuture<>();
		
		queue.put(() -> {
			try {
				ItemKey key = new ItemKey(_id);
				
				if (journal_write_cache.containsKey(key)) {
					result.complete(true);
				} else if (read_cache.has(key)) {
					result.complete(true);
				} else if (backend.contain(key)) {
					result.complete(true);
				} else {
					result.complete(false);
				}
			} catch (Exception e) {
				result.completeExceptionally(e);
			}
		});
		
		return result;
	}
	
	public CompletableFuture<Void> removeById(String _id) {
		CompletableFuture<Void> result = new CompletableFuture<>();
		
		queue.put(() -> {
			try {
				ItemKey key = new ItemKey(_id);
				Item actual_item = null;
				
				if (journal_write_cache.containsKey(key)) {
					actual_item = journal_write_cache.get(key);
					if (actual_item.isDeleted()) {
						result.complete(null);
						return;
					}
				} else {
					actual_item = read_cache.get(key);
					if (actual_item == null) {
						ByteBuffer read_buffer = backend.read(key);
						if (read_buffer == null) {
							result.complete(null);
							return;
						}
						actual_item = new Item(read_buffer);
					}
				}
				
				put(actual_item.setPayload(new byte[0]).setTTL(-1l));
				result.complete(null);
			} catch (Exception e) {
				result.completeExceptionally(e);
			}
		});
		
		return result;
		
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
		CompletableFuture<List<T>> result = new CompletableFuture<>();
		queue.put(() -> {
			try {
				List<Item> stored_items = mapToItemAndPushToReadCacheAndIsActuallyNotExistsInCommitLog(backend.getAllDatas());
				
				result.complete(accumulateWithCommitLog(stored_items, new HashSet<>(journal_write_cache.values())).map(item -> {
					return item_factory.getFromItem(item);
				}).collect(Collectors.toList()));
			} catch (Exception e) {
				result.completeExceptionally(e);
			}
		});
		return result;
	}
	
	private Stream<Item> internalGetByPath(String path) throws IOException {
		List<Item> stored_items = mapToItemAndPushToReadCacheAndIsActuallyNotExistsInCommitLog(backend.getDatasByPath(path));
		HashSet<Item> commit_log_filtered = new HashSet<Item>(journal_write_cache.values().stream().filter(item -> {
			return item.getPath().startsWith(path);
		}).collect(Collectors.toSet()));
		
		return accumulateWithCommitLog(stored_items, commit_log_filtered);
	}
	
	public CompletableFuture<List<T>> getByPath(String path) {
		CompletableFuture<List<T>> result = new CompletableFuture<List<T>>();
		queue.put(() -> {
			try {
				result.complete(internalGetByPath(path).map(item -> {
					return item_factory.getFromItem(item);
				}).collect(Collectors.toList()));
			} catch (Exception e) {
				result.completeExceptionally(e);
			}
		});
		return result;
	}
	
	private void remove(Stream<Item> items) throws IOException {
		try {
			items.forEach(item -> {
				item.setPayload(new byte[0]);
				item.setTTL(-1);
				item.setPath(null);
				ItemKey key = item.getKey();
				journal_write_cache.put(key, item);
				read_cache.remove(key);
				try {
					backend.writeInJournal(item, System.currentTimeMillis() + grace_period_for_expired_items);
				} catch (IOException e1) {
					throw new RuntimeException(e1);
				}
			});
		} catch (RuntimeException e2) {
			if (e2.getCause() instanceof IOException) {
				throw (IOException) e2.getCause();
			} else {
				throw new IOException(e2);
			}
		}
	}
	
	public CompletableFuture<Void> removeAllByPath(String path) {
		CompletableFuture<Void> result = new CompletableFuture<>();
		queue.put(() -> {
			try {
				remove(internalGetByPath(path));
				result.complete(null);
			} catch (Exception e) {
				result.completeExceptionally(e);
			}
		});
		return result;
	}
	
	public CompletableFuture<Void> truncate() {
		CompletableFuture<Void> result = new CompletableFuture<>();
		
		queue.put(() -> {
			try {
				List<Item> stored_items = mapToItemAndPushToReadCacheAndIsActuallyNotExistsInCommitLog(backend.getAllDatas());
				remove(stored_items.stream());
				remove(journal_write_cache.values().stream());
				read_cache.purgeAll();
				doDurableWrite();
				result.complete(null);
			} catch (Exception e) {
				result.completeExceptionally(e);
			}
		});
		
		return result;
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
		queue.cleanUp();
	}
	
	// TODO network I/O
	// TODO do full file GC (with pause)
	
}
