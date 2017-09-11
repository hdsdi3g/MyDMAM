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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.LongBinaryOperator;
import java.util.function.ToLongFunction;
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
	
	/**
	 * key -> Item
	 */
	private final ConcurrentHashMap<byte[], StoreItem> commit_log_cache;
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
		
		rotateAndReadCommitlog(injectCurrentRawDatasInDatabase);
		removeOutdatedRecordsInDatabase(grace_period_for_expired_items);
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
	
	// TODO use cache
	// TODO grace period with no store in commit log
	
	/**
	 * @param ttl in ms
	 * @throws IOException
	 */
	void put(StoreItem item, long ttl) throws IOException {
		item.setTTL(ttl);
		byte[] key = item.getKey();
		commit_log_cache.put(key, item);
		
		writeInCommitlog(key, item.toRawContent());
		if (getEstimateCommitLogCacheSize() > max_size_for_cached_commit_log) {
			doDurableWrite();
		}
	}
	
	void removeById(String _id) {
		// TODO
	}
	
	void removeAllByPath(String path) {
		// TODO
	}
	
	void truncateDatabase() {
		// TODO
		read_cache.purgeAll();
	}
	
	Stream<String> listAll() {
		return null;// TODO
	}
	
	Stream<String> listByPath(String path) {
		return null;// TODO
	}
	
	boolean exists(String _id) {
		return false;// TODO
	}
	
	Stream<StoreItem> getAll() {
		return null;// TODO
	}
	
	StoreItem get(String _id) {
		return null;// TODO
	}
	
	Stream<StoreItem> getByPath(String path) {
		return null;// TODO
	}
	
	void checkConsistency() {
		// TODO
	}
	
	/**
	 * Stop the world
	 */
	void doDurableWrite() throws IOException {
		// TODO semaphore "Stop the world"
		
		/**
		 * commit_log_cache >> writeInDatabase
		 */
		commit_log_cache.forEach((key, item) -> {
			if (item.getRemainingTTL() < grace_period_for_expired_items) {
				return;
			}
			try {
				writeInDatabase(key, item.toRawContent(), item.getId(), item.getPath(), item.getDeleted());
			} catch (IOException e) {
				throw new RuntimeException("Can't write in database", e);
			}
		});
		
		/**
		 * rotateAndReadCommitlog >> remove commit_log_cache item | writeInDatabase
		 */
		rotateAndReadCommitlog((key, content) -> {
			if (commit_log_cache.remove(key) != null) {
				return;
			}
			injectCurrentRawDatasInDatabase.accept(key, content);
		});
		
		commit_log_cache.clear();
		
		removeOutdatedRecordsInDatabase(grace_period_for_expired_items);
	}
	
	private BiConsumer<byte[], byte[]> injectCurrentRawDatasInDatabase = (key, content) -> {
		StoreItem item = StoreItem.fromRawContent(content);
		if (item.getRemainingTTL() < grace_period_for_expired_items) {
			try {
				writeInDatabase(key, content, item.getId(), item.getPath(), item.getDeleted());
			} catch (IOException e) {
				throw new RuntimeException("Can't write in database", e);
			}
		}
	};
	
	protected abstract void writeInCommitlog(byte[] key, byte[] content) throws IOException;
	
	protected abstract void rotateAndReadCommitlog(BiConsumer<byte[], byte[]> all_reader) throws IOException;
	
	protected abstract void writeInDatabase(byte[] key, byte[] content, String _id, String path, long delete_date) throws IOException;
	
	/**
	 * Remove all for delete_date < Now - grace_period
	 */
	protected abstract void removeOutdatedRecordsInDatabase(long grace_period) throws IOException;
	
	/**
	 * @return raw content
	 */
	protected abstract byte[] read(byte[] key) throws IOException;
	
	/**
	 * @return raw content
	 */
	protected abstract Stream<byte[]> readAll() throws IOException;
	
	/**
	 * @return data key
	 */
	protected abstract byte[] getKeyById(String _id) throws IOException;
	
	/**
	 * @return raw content
	 */
	protected abstract Stream<byte[]> getKeysByPath(String path) throws IOException;
	
}
