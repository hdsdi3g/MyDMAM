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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongBinaryOperator;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import hd3gtv.mydmam.MyDMAM;

/**
 * Polyvalent and agnostic object storage
 */
public abstract class StoreBackend {
	private static Logger log = Logger.getLogger(StoreBackend.class);
	
	protected final String database_name;
	private StoreCache read_cache;
	
	private ConcurrentHashMap<StoreItemKey, StoreItem> commit_log_cache;
	private long max_size_for_cached_commit_log;
	private long grace_period_for_expired_items;
	
	/**
	 * @param read_cache dedicated cache for this store
	 */
	public StoreBackend(String database_name, StoreCache read_cache, long max_size_for_cached_commit_log, long grace_period_for_expired_items) throws IOException {
		this.database_name = database_name;
		if (database_name == null) {
			throw new NullPointerException("\"database_name\" can't to be null");
		}
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
		
		_rotateAndReadCommitlog(injectCurrentRawDatasInDatabase);
		_removeOutdatedRecordsInDatabase(grace_period_for_expired_items);
	}
	
	private long getEstimateCommitLogCacheSize() {
		ToLongFunction<StoreItem> transformer = item -> {
			return (long) item.estimateSize();
		};
		
		LongBinaryOperator reducer = (l, r) -> {
			return l + r;
		};
		
		return commit_log_cache.reduceValuesToLong(MyDMAM.CPU_COUNT, transformer, 0, reducer);
	}
	
	/**
	 * @param ttl in ms
	 * @throws IOException
	 */
	void put(StoreItem item, long ttl) throws IOException {
		item.setTTL(ttl);
		StoreItemKey key = item.getKey();
		commit_log_cache.put(key, item);
		read_cache.remove(key);
		
		_writeInCommitlog(key, item.toRawContent());
		if (getEstimateCommitLogCacheSize() > max_size_for_cached_commit_log) {
			doDurableWrite();
		}
	}
	
	StoreItem get(String _id) throws IOException {
		StoreItemKey key = new StoreItemKey(_id);
		
		StoreItem item = commit_log_cache.get(key);
		if (item != null) {
			if (item.isDeleted() == false) {
				return item;
			} else {
				return null;
			}
		}
		
		byte[] datas = read_cache.get(key);
		if (datas != null) {
			return StoreItem.fromRawContent(datas);
		}
		
		datas = _read(key);
		if (datas != null) {
			item = StoreItem.fromRawContent(datas);
			read_cache.put(key, datas, item.getActualTTL());
			return item;
		}
		
		return null;
	}
	
	boolean exists(String _id) throws IOException {
		StoreItemKey key = new StoreItemKey(_id);
		
		if (commit_log_cache.containsKey(key)) {
			return true;
		}
		if (read_cache.has(key)) {
			return true;
		}
		if (_contain(key)) {
			return true;
		}
		return false;
	}
	
	void removeById(String _id) throws IOException {
		StoreItemKey key = new StoreItemKey(_id);
		
		if (commit_log_cache.containsKey(key)) {
			StoreItem actual_item = commit_log_cache.get(key);
			if (actual_item.isDeleted() == false) {
				put(actual_item.setPayload(new byte[0]), -1);
			}
		} else {
			StoreItem actual_item = get(_id);
			if (actual_item != null) {
				put(actual_item.setPayload(new byte[0]), -1);
			}
		}
	}
	
	private Function<byte[], StoreItem> mapToStoreItemAndPushToReadCache = datas -> {
		StoreItem item = StoreItem.fromRawContent(datas);
		read_cache.put(item.getKey(), datas, item.getActualTTL());
		return item;
	};
	
	private Predicate<StoreItem> isActuallyNotExistsInCommitLog = item -> {
		return commit_log_cache.containsKey(item.getKey()) == false;
	};
	
	Stream<StoreItem> getAll() throws IOException {
		List<StoreItem> stored_items = _getAllDatas().map(mapToStoreItemAndPushToReadCache).filter(isActuallyNotExistsInCommitLog).collect(Collectors.toList());
		
		ArrayList<StoreItem> items = new ArrayList<>(commit_log_cache.values());
		items.addAll(stored_items);
		
		return items.stream().distinct();
	}
	
	Stream<StoreItem> getByPath(String path) throws IOException {
		List<StoreItem> stored_items = _getDatasByPath(path).map(mapToStoreItemAndPushToReadCache).filter(isActuallyNotExistsInCommitLog).collect(Collectors.toList());
		
		List<StoreItem> commited_items = commit_log_cache.values().stream().filter(item -> {
			return item.getPath().startsWith(path);
		}).collect(Collectors.toList());
		
		ArrayList<StoreItem> items = new ArrayList<>(commited_items);
		items.addAll(stored_items);
		
		return items.stream().distinct();
	}
	
	void removeAllByPath(String path) throws IOException {
		getByPath(path).forEach(item -> {
			try {
				removeById(item.getId());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}
	
	private Consumer<StoreItem> removeItem = item -> {
		try {
			put(item, -1);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	};
	
	void truncateDatabase() throws IOException {
		List<StoreItem> stored_items = _getAllDatas().map(mapToStoreItemAndPushToReadCache).filter(isActuallyNotExistsInCommitLog).collect(Collectors.toList());
		stored_items.forEach(removeItem);
		commit_log_cache.values().forEach(removeItem);
		read_cache.purgeAll();
		doDurableWrite();
	}
	
	/**
	 * Stop the world
	 */
	void doDurableWrite() throws IOException {
		// TODO semaphore "Stop the world"
		// TODO semaphore "I works now, don't do a durable write"
		/**
		 * commit_log_cache >> writeInDatabase
		 */
		commit_log_cache.forEach((key, item) -> {
			if (item.isDeletedPurgable(grace_period_for_expired_items)) {
				return;
			}
			try {
				_writeInDatabase(key, item.toRawContent(), item.getId(), item.getPath(), item.getDeleteDate());
			} catch (IOException e) {
				throw new RuntimeException("Can't write in database", e);
			}
		});
		
		/**
		 * rotateAndReadCommitlog >> remove commit_log_cache item | writeInDatabase
		 */
		_rotateAndReadCommitlog((key, content) -> {
			if (commit_log_cache.remove(key) != null) {
				return;
			}
			injectCurrentRawDatasInDatabase.accept(key, content);
		});
		
		commit_log_cache.clear();
		
		_removeOutdatedRecordsInDatabase(grace_period_for_expired_items);
	}
	
	private BiConsumer<StoreItemKey, byte[]> injectCurrentRawDatasInDatabase = (key, content) -> {
		StoreItem item = StoreItem.fromRawContent(content);
		if (item.isDeletedPurgable(grace_period_for_expired_items) == false) {
			try {
				_writeInDatabase(key, content, item.getId(), item.getPath(), item.getDeleteDate());
			} catch (IOException e) {
				throw new RuntimeException("Can't write in database", e);
			}
		}
	};
	
	protected abstract void _writeInCommitlog(StoreItemKey key, byte[] content) throws IOException;
	
	protected abstract void _rotateAndReadCommitlog(BiConsumer<StoreItemKey, byte[]> all_reader) throws IOException;
	
	protected abstract void _writeInDatabase(StoreItemKey key, byte[] content, String _id, String path, long delete_date) throws IOException;
	
	/**
	 * Remove all for delete_date < Now - grace_period
	 */
	protected abstract void _removeOutdatedRecordsInDatabase(long grace_period) throws IOException;
	
	/**
	 * @return raw content
	 */
	protected abstract byte[] _read(StoreItemKey key) throws IOException;
	
	protected abstract boolean _contain(StoreItemKey key) throws IOException;
	
	/**
	 * @return raw content
	 */
	protected abstract Stream<byte[]> _getAllDatas() throws IOException;
	
	/**
	 * @return raw content
	 */
	protected abstract Stream<byte[]> _getDatasByPath(String path) throws IOException;
	
	public String getDatabaseName() {
		return database_name;
	}
}
