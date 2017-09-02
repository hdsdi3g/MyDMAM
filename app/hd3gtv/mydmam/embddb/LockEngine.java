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
 * Copyright (C) hdsdi3g for hd3g.tv 3 sept. 2017
 * 
*/
package hd3gtv.mydmam.embddb;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.google.gson.JsonObject;

import hd3gtv.mydmam.embddb.LockEngine.CategoryLock.DistributedLock;
import hd3gtv.mydmam.embddb.network.DataBlock;
import hd3gtv.mydmam.embddb.network.Node;
import hd3gtv.mydmam.embddb.network.PoolManager;
import hd3gtv.mydmam.embddb.network.RequestHandler;

public final class LockEngine {
	private static Logger log = Logger.getLogger(LockEngine.class);
	
	private ConcurrentHashMap<String, CategoryLock> active_categories_by_name;
	
	LockEngine(PoolManager poolmanager) {
		active_categories_by_name = new ConcurrentHashMap<>();
		poolmanager.addRequestHandler(new RequestLock(poolmanager));
	}
	
	/**
	 * Don't forget to release after use.
	 */
	public DistributedLock acquireLock(String category, String target_id) {
		return active_categories_by_name.computeIfAbsent(category, _cat -> {
			return new CategoryLock(category);
		}).acquireLock(target_id);
	}
	
	public LockEngine releaseAll() {
		active_categories_by_name.forEach((n, c) -> {
			c.releaseAll();
		});
		return this;
	}
	
	public synchronized LockEngine releaseAll(String category) {
		if (active_categories_by_name.containsKey(category) == false) {
			return this;
		}
		active_categories_by_name.get(category).releaseAll();
		return this;
	}
	
	// TODO gc old locks
	
	public final class CategoryLock {
		
		private final String name;
		
		private ConcurrentHashMap<String, DistributedLock> active_locks_by_target_id;
		
		private CategoryLock(String name) {
			this.name = name;
			if (name == null) {
				throw new NullPointerException("\"name\" can't to be null");
			}
			active_locks_by_target_id = new ConcurrentHashMap<>();
		}
		
		public String getName() {
			return name;
		}
		
		/**
		 * Don't forget to release after use.
		 */
		public DistributedLock acquireLock(String target_id) {
			return active_locks_by_target_id.computeIfAbsent(target_id, _id -> {
				return new DistributedLock(target_id);
			}).internalAcquire();
		}
		
		private DistributedLock injectLock(String target_id) {
			return active_locks_by_target_id.compute(target_id, (new_id, actual_lock) -> {
				if (actual_lock == null) {
					return new DistributedLock(new_id);
				} else {
					return actual_lock.recycle();
				}
			});
		}
		
		public void releaseAll() {
			active_locks_by_target_id.forEach((n, l) -> {
				l.release();
			});
			active_locks_by_target_id.clear();
		}
		
		public final class DistributedLock {
			
			private final String target_id;
			
			private DistributedLock(String target_id) {
				this.target_id = target_id;
				if (target_id == null) {
					throw new NullPointerException("\"target_id\" can't to be null");
				}
			}
			
			private DistributedLock internalAcquire() {
				/**
				 * TODO acquire: regular send to all the
				 */
				// TODO must be blocking
				return this;
			}
			
			public void release() {
				// TODO must be blocking
				active_locks_by_target_id.remove(target_id);
			}
			
			private DistributedLock recycle() {
				// TODO
				return this;
			}
			
			public String getTargetId() {
				return target_id;
			}
			
			public String getName() {
				return name;
			}
		}
	}
	
	public class RequestLock extends RequestHandler<DistributedLock> {
		
		public RequestLock(PoolManager pool_manager) {
			super(pool_manager);
		}
		
		public String getHandleName() {
			return "lock";
		}
		
		protected boolean isCloseChannelRequest(DistributedLock options) {
			return false;
		}
		
		public void onRequest(DataBlock block, Node source_node) {
			JsonObject jo = block.getJsonDatas().getAsJsonObject();
			String category = jo.get("name").getAsString();
			String target_id = jo.get("target_id").getAsString();
			
			active_categories_by_name.computeIfAbsent(category, _cat -> {
				return new CategoryLock(category);
			}).injectLock(target_id);
		}
		
		public DataBlock createRequest(DistributedLock lock) {
			JsonObject jo = new JsonObject();
			jo.addProperty("name", lock.getName());
			jo.addProperty("target_id", lock.getTargetId());
			return new DataBlock(this, jo);
		}
		
	}
}
