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
import java.lang.Thread.State;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.LongBinaryOperator;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import com.google.common.reflect.TypeToken;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.tools.ThreadPoolExecutorFactory;

/**
 * Polyvalent and agnostic object storage
 * Bind Backend and StoreItemFactory
 */
public final class Store<T> {
	private static Logger log = Logger.getLogger(Store.class);
	
	protected final String database_name;
	private final Cache read_cache;
	private final Backend backend;
	private final ItemFactory<T> item_factory;
	private final Queue queue;
	
	private final ConcurrentHashMap<ItemKey, Item> commit_log_cache;
	private final long max_size_for_cached_commit_log;
	private final long grace_period_for_expired_items;
	private final ThreadPoolExecutorFactory executor;
	private final String generic_class_name;
	
	/**
	 * @param read_cache dedicated cache for this store
	 */
	public Store(String database_name, ItemFactory<T> item_factory, Backend backend, Cache read_cache, long max_size_for_cached_commit_log, long grace_period_for_expired_items) throws IOException {
		this.database_name = database_name;
		if (database_name == null) {
			throw new NullPointerException("\"database_name\" can't to be null");
		}
		this.item_factory = item_factory;
		if (item_factory == null) {
			throw new NullPointerException("\"item_factory\" can't to be null");
		}
		this.backend = backend;
		if (backend == null) {
			throw new NullPointerException("\"backend\" can't to be null");
		}
		TypeToken<T> type_factory = new TypeToken<T>() {
		};
		generic_class_name = type_factory.getRawType().getSimpleName();
		backend.init(database_name, generic_class_name);
		
		this.read_cache = read_cache;
		if (read_cache == null) {
			throw new NullPointerException("\"read_cache\" can't to be null");
		}
		commit_log_cache = new ConcurrentHashMap<>();
		this.max_size_for_cached_commit_log = max_size_for_cached_commit_log;
		if (max_size_for_cached_commit_log < 1l) {
			throw new NullPointerException("\"max_size_for_cached_commit_log\" can't to be < 1");
		}
		this.grace_period_for_expired_items = grace_period_for_expired_items;
		if (grace_period_for_expired_items < 1l) {
			throw new NullPointerException("\"grace_period_for_expired_items\" can't to be < 1");
		}
		
		executor = new ThreadPoolExecutorFactory("EMBDDB-Store-" + database_name + "_" + generic_class_name, Thread.MIN_PRIORITY + 1, e -> {
			log.error("Genric error for " + database_name + "/" + generic_class_name, e);
		});
		
		queue = new Queue();
		
		backend.rotateAndReadCommitlog((key, content) -> {
			injectCurrentRawDatasInDatabase(key, content);
		});
		backend.removeOutdatedRecordsInDatabase(grace_period_for_expired_items);
	}
	
	public void put(T element, long ttl, TimeUnit unit, Consumer<T> onDone, BiConsumer<T, IOException> onError) {
		put(item_factory.toItem(element), unit.toMillis(ttl), () -> {
			onDone.accept(element);
		}, e -> {
			onError.accept(element, e);
		});
	}
	
	/**
	 * @param ttl in ms
	 */
	private void put(Item item, long ttl, Runnable onDone, Consumer<IOException> onError) {
		queue.put(() -> {
			try {
				item.setTTL(ttl);
				ItemKey key = item.getKey();
				commit_log_cache.put(key, item);
				read_cache.remove(key);
				
				backend.writeInCommitlog(key, item.toRawContent());
				checkIfNeedToRotateCommitLog();
				executor.execute(onDone);
			} catch (IOException e) {
				executor.execute(() -> {
					onError.accept(e);
				});
			}
		});
	}
	
	public void get(String _id, BiConsumer<String, T> onDone, Consumer<String> onNotFound, BiConsumer<String, IOException> onError) {
		queue.put(() -> {
			try {
				ItemKey key = new ItemKey(_id);
				
				Item item = commit_log_cache.get(key);
				if (item != null) {
					if (item.isDeleted() == false) {
						final Item _item = item;
						executor.execute(() -> {
							onDone.accept(_id, item_factory.getFromItem(_item));
						});
					} else {
						executor.execute(() -> {
							onNotFound.accept(_id);
						});
					}
					return;
				}
				
				byte[] datas = read_cache.get(key);
				if (datas != null) {
					final byte[] _datas = datas;
					executor.execute(() -> {
						onDone.accept(_id, item_factory.getFromItem(Item.fromRawContent(_datas)));
					});
					return;
				}
				
				datas = backend.read(key);
				if (datas != null) {
					item = Item.fromRawContent(datas);
					read_cache.put(key, datas, item.getActualTTL());
					final Item _item = item;
					executor.execute(() -> {
						onDone.accept(_id, item_factory.getFromItem(_item));
					});
					return;
				}
				
				executor.execute(() -> {
					onNotFound.accept(_id);
				});
			} catch (IOException e) {
				executor.execute(() -> {
					onError.accept(_id, e);
				});
			}
		});
	}
	
	public void exists(String _id, BiConsumer<String, Boolean> onDone, BiConsumer<String, IOException> onError) {
		queue.put(() -> {
			try {
				ItemKey key = new ItemKey(_id);
				
				if (commit_log_cache.containsKey(key)) {
					executor.execute(() -> {
						onDone.accept(_id, true);
					});
				} else if (read_cache.has(key)) {
					executor.execute(() -> {
						onDone.accept(_id, true);
					});
				} else if (backend.contain(key)) {
					executor.execute(() -> {
						onDone.accept(_id, true);
					});
				} else {
					executor.execute(() -> {
						onDone.accept(_id, true);
					});
				}
			} catch (IOException e) {
				executor.execute(() -> {
					onError.accept(_id, e);
				});
			}
		});
	}
	
	public void removeById(String _id, Consumer<String> onDone, Consumer<String> onNotFound, BiConsumer<String, IOException> onError) {
		queue.put(() -> {
			try {
				ItemKey key = new ItemKey(_id);
				Item actual_item = null;
				
				if (commit_log_cache.containsKey(key)) {
					actual_item = commit_log_cache.get(key);
					if (actual_item.isDeleted()) {
						executor.execute(() -> {
							onNotFound.accept(_id);
						});
						return;
					}
				} else {
					byte[] datas = read_cache.get(key);
					if (datas == null) {
						datas = backend.read(key);
					}
					if (datas == null) {
						executor.execute(() -> {
							onNotFound.accept(_id);
						});
						return;
					}
					actual_item = Item.fromRawContent(datas);
				}
				
				put(actual_item.setPayload(new byte[0]), -1, () -> {
					executor.execute(() -> {
						onDone.accept(_id);
					});
				}, e -> {
					executor.execute(() -> {
						onError.accept(_id, e);
					});
				});
			} catch (IOException e) {
				executor.execute(() -> {
					onError.accept(_id, e);
				});
			}
		});
	}
	
	private List<Item> mapToItemAndPushToReadCacheAndIsActuallyNotExistsInCommitLog(Stream<byte[]> source) {
		return source.map(datas -> {
			Item item = Item.fromRawContent(datas);
			read_cache.put(item.getKey(), datas, item.getActualTTL());
			return item;
		}).filter(item -> {
			return commit_log_cache.containsKey(item.getKey()) == false;
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
	
	public void getAll(Consumer<List<T>> onDone, Consumer<IOException> onError) {
		queue.put(() -> {
			try {
				List<Item> stored_items = mapToItemAndPushToReadCacheAndIsActuallyNotExistsInCommitLog(backend.getAllDatas());
				List<T> result = accumulateWithCommitLog(stored_items, new HashSet<>(commit_log_cache.values())).map(item -> {
					return item_factory.getFromItem(item);
				}).collect(Collectors.toList());
				
				executor.execute(() -> {
					onDone.accept(result);
				});
			} catch (IOException e) {
				executor.execute(() -> {
					onError.accept(e);
				});
			}
		});
	}
	
	private void internalGetByPath(String path, Consumer<Stream<Item>> onDone, Consumer<IOException> onError) {
		queue.put(() -> {
			try {
				List<Item> stored_items = mapToItemAndPushToReadCacheAndIsActuallyNotExistsInCommitLog(backend.getDatasByPath(path));
				HashSet<Item> commit_log_filtered = new HashSet<Item>(commit_log_cache.values().stream().filter(item -> {
					return item.getPath().startsWith(path);
				}).collect(Collectors.toSet()));
				
				onDone.accept(accumulateWithCommitLog(stored_items, commit_log_filtered));
			} catch (IOException e) {
				executor.execute(() -> {
					onError.accept(e);
				});
			}
		});
	}
	
	public void getByPath(String path, Consumer<List<T>> onDone, Consumer<IOException> onError) {
		internalGetByPath(path, item_stream -> {
			List<T> result = item_stream.map(item -> {
				return item_factory.getFromItem(item);
			}).collect(Collectors.toList());
			
			executor.execute(() -> {
				onDone.accept(result);
			});
		}, onError);
	}
	
	private void remove(Stream<Item> items) throws IOException {
		try {
			items.forEach(item -> {
				item.setPayload(new byte[0]);
				item.setTTL(-1);
				ItemKey key = item.getKey();
				commit_log_cache.put(key, item);
				read_cache.remove(key);
				try {
					backend.writeInCommitlog(key, new byte[0]);
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
		checkIfNeedToRotateCommitLog();
	}
	
	public void removeAllByPath(String path, Consumer<String> onDone, BiConsumer<String, IOException> onError) {
		queue.put(() -> {
			internalGetByPath(path, item_stream -> {
				try {
					remove(item_stream);
					executor.execute(() -> {
						onDone.accept(path);
					});
				} catch (IOException e1) {
					executor.execute(() -> {
						onError.accept(path, e1);
					});
				}
			}, e -> {
				executor.execute(() -> {
					onError.accept(path, e);
				});
			});
		});
		
	}
	
	public void truncate(Runnable onDone, Consumer<IOException> onError) {
		queue.put(() -> {
			try {
				List<Item> stored_items = mapToItemAndPushToReadCacheAndIsActuallyNotExistsInCommitLog(backend.getAllDatas());
				remove(stored_items.stream());
				remove(commit_log_cache.values().stream());
				read_cache.purgeAll();
				doDurableWrite();
				executor.execute(onDone);
			} catch (IOException e) {
				executor.execute(() -> {
					onError.accept(e);
				});
			}
		});
	}
	
	/**
	 * Stop the world
	 */
	public void doDurableWrite() throws IOException {
		queue.stopTheWorld();
		
		try {
			/**
			 * commit_log_cache >> writeInDatabase
			 */
			commit_log_cache.forEach((key, item) -> {
				if (item.isDeletedPurgable(grace_period_for_expired_items)) {
					return;
				}
				try {
					backend.writeInDatabase(key, item.toRawContent(), item.getId(), item.getPath(), item.getDeleteDate());
				} catch (IOException e) {
					throw new RuntimeException("Can't write in database", e);
				}
			});
			
			/**
			 * rotateAndReadCommitlog >> remove commit_log_cache item | writeInDatabase
			 */
			backend.rotateAndReadCommitlog((key, content) -> {
				if (commit_log_cache.remove(key) != null) {
					return;
				}
				injectCurrentRawDatasInDatabase(key, content);
			});
			
			commit_log_cache.clear();
			
			backend.removeOutdatedRecordsInDatabase(grace_period_for_expired_items);
			
			queue.releaseTheWorld();
		} catch (IOException e) {
			queue.releaseTheWorld();
			throw e;
		}
	}
	
	private void injectCurrentRawDatasInDatabase(ItemKey key, byte[] content) {
		Item item = Item.fromRawContent(content);
		if (item.isDeletedPurgable(grace_period_for_expired_items) == false) {
			try {
				backend.writeInDatabase(key, content, item.getId(), item.getPath(), item.getDeleteDate());
			} catch (IOException e) {
				throw new RuntimeException("Can't write in database", e);
			}
		}
	}
	
	public String getDatabaseName() {
		return database_name;
	}
	
	private void checkIfNeedToRotateCommitLog() throws IOException {
		ToLongFunction<Item> transformer = item -> {
			return (long) item.estimateSize();
		};
		
		LongBinaryOperator reducer = (l, r) -> {
			return l + r;
		};
		long estimate_commit_log_cache_size = commit_log_cache.reduceValuesToLong(MyDMAM.CPU_COUNT, transformer, 0, reducer);
		
		if (estimate_commit_log_cache_size > max_size_for_cached_commit_log) {
			doDurableWrite(); // TODO protect with parallel exec (not needs to be called a lot of time...)
		}
	}
	
	private class Queue {
		final LinkedBlockingQueue<Runnable> waiting = new LinkedBlockingQueue<>();
		volatile boolean stop_the_world = false;
		final Object lock = new Object();
		
		Thread loop_processor;
		
		public Queue() {
			createThread();
		}
		
		private void createThread() {
			if (loop_processor != null) {
				if (loop_processor.getState() != State.TERMINATED) {
					return;
				}
			}
			loop_processor = new Thread(queue_process_loop);
			loop_processor.setDaemon(true);
			loop_processor.setName("EMBDDBStoreQueueExecutorFor_" + getDatabaseName() + "/" + generic_class_name);
			loop_processor.setPriority(Thread.MIN_PRIORITY + 1);
			loop_processor.start();
		}
		
		final Runnable queue_process_loop = () -> {
			try {
				while (stop_the_world == false) {
					Runnable next_task = waiting.take();
					
					synchronized (lock) {
						if (stop_the_world) {
							waiting.add(next_task);
							return;
						} else {
							executor.execute(next_task);
						}
					}
				}
			} catch (InterruptedException e) {
				log.error("Cancel Store queue", e);
			}
		};
		
		void put(Runnable r) {
			waiting.add(r);
		}
		
		void stopTheWorld() {
			synchronized (lock) {
				stop_the_world = true;
			}
		}
		
		void releaseTheWorld() {
			synchronized (lock) {
				stop_the_world = false;
			}
			createThread();
		}
		
	}
	
	// TODO network IO
}
