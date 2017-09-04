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

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.google.gson.JsonObject;

import hd3gtv.mydmam.embddb.network.DataBlock;
import hd3gtv.mydmam.embddb.network.Node;
import hd3gtv.mydmam.embddb.network.PoolManager;
import hd3gtv.mydmam.embddb.network.RequestHandler;

public final class LockEngine {
	
	private static final long GRACE_TIME_FOR_GET_LOCK = TimeUnit.SECONDS.toMillis(2);
	private static Logger log = Logger.getLogger(LockEngine.class);
	
	private ArrayList<DistributedLock> active_locks;// TODO replace by hashmap ?
	private ConcurrentHashMap<String, Node> node_by_already_busy_lock_target_id;
	
	private ScheduledExecutorService scheduled_ex_service;
	
	LockEngine(PoolManager poolmanager) {
		active_locks = new ArrayList<>();
		node_by_already_busy_lock_target_id = new ConcurrentHashMap<>(1);
		
		poolmanager.addRequestHandler(new RequestLockAcquire(poolmanager));
		poolmanager.addRequestHandler(new RequestLockRelease(poolmanager));
		poolmanager.addRequestHandler(new RequestLockAlreadyBusy(poolmanager));
		// TODO + create a callback if a node is disconnected >> gc it here
		
		scheduled_ex_service = Executors.newSingleThreadScheduledExecutor();
		scheduled_ex_service.scheduleAtFixedRate(() -> {
			synchronized (active_locks) {
				active_locks.removeIf(lock -> {
					return lock.hasExpired();
				});
			}
		}, 60, 60, TimeUnit.SECONDS);
	}
	
	private synchronized Optional<DistributedLock> getLockByTarget(String target_id) {
		return active_locks.stream().filter(lock -> {
			return lock.target_id.equals(target_id);
		}).findFirst();
	}
	
	/**
	 * Blocking
	 * Never null
	 */
	public synchronized DistributedLock acquireLock(String target_id, long duration, TimeUnit unit) throws AlreadyBusyLock {
		Optional<DistributedLock> o_lock = getLockByTarget(target_id);
		if (o_lock.isPresent()) {
			DistributedLock actual_lock = o_lock.get();
			if (actual_lock.isAlreadyForMe()) {
				return actual_lock;
			} else {
				throw actual_lock.createBusyException();
			}
		} else {
			DistributedLock new_lock = new DistributedLock(target_id, unit.toMillis(duration));
			// TODO send to all nodes
			// source_node.isOutOfTime(min_delta_time, max_delta_time)
			// source_node.isUUIDSet()
			// source_node.isOpenSocket()
			
			/**
			 * Start to wait some bad responses...
			 */
			Node current_locker;
			long end_time_to_wait = System.currentTimeMillis() + GRACE_TIME_FOR_GET_LOCK;
			while (((current_locker = node_by_already_busy_lock_target_id.get(target_id)) == null)) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				if (end_time_to_wait < System.currentTimeMillis()) {
					break;
				}
			}
			if (current_locker != null) {
				throw new AlreadyBusyLock(target_id, end_time_to_wait, current_locker.toString());
			}
			
			active_locks.add(new_lock);
			return new_lock;
		}
	}
	
	private synchronized void externalAcquireLock(String target_id, long expiration_date, Node locker_node) throws AlreadyBusyLock {
		if (expiration_date < System.currentTimeMillis()) {
			return;
		}
		
		Optional<DistributedLock> o_lock = getLockByTarget(target_id);
		
		if (o_lock.isPresent()) {
			DistributedLock actual_lock = o_lock.get();
			if (actual_lock.local_locker != null) {
				if (actual_lock.hasExpired()) {
					active_locks.remove(actual_lock);
				} else {
					throw actual_lock.createBusyException();
				}
			} else if (locker_node.equalsThisUUID(actual_lock.node)) {
				actual_lock.expiration_date = expiration_date;
			} else {
				/**
				 * Somebody try to get a lock previously acquired by another node... but it not our problem.
				 */
			}
		} else {
			active_locks.add(new DistributedLock(target_id, expiration_date, locker_node));
		}
	}
	
	public final class DistributedLock {
		
		private final String target_id;
		private long expiration_date;
		private final Thread local_locker;
		private final Node node;
		
		/**
		 * For me
		 */
		private DistributedLock(String target_id, long ttl) {
			this.target_id = target_id;
			if (target_id == null) {
				throw new NullPointerException("\"target_id\" can't to be null");
			}
			this.expiration_date = System.currentTimeMillis() + ttl;
			if (ttl == 0) {
				throw new NullPointerException("\"ttl\" can't to equals to 0");
			}
			local_locker = Thread.currentThread();
			node = null;
		}
		
		/**
		 * For external
		 */
		private DistributedLock(String target_id, long expiration_date, Node node) {
			this.target_id = target_id;
			if (target_id == null) {
				throw new NullPointerException("\"target_id\" can't to be null");
			}
			this.expiration_date = expiration_date;
			if (expiration_date == 0) {
				throw new NullPointerException("\"expiration_date\" can't to equals to 0");
			}
			this.node = node;
			if (node == null) {
				throw new NullPointerException("\"node\" can't to be null");
			}
			local_locker = null;
		}
		
		public boolean isAlreadyForMe() {
			if (hasExpired()) {
				return false;
			}
			if (node != null) {
				return false;
			}
			return local_locker == Thread.currentThread();
		}
		
		public boolean hasExpired() {
			return expiration_date < System.currentTimeMillis();
		}
		
		public synchronized void manualRelease() {
			if (hasExpired()) {
				return;
			}
			expiration_date = System.currentTimeMillis();
			// TODO send to all the lock ends
		}
		
		private synchronized void externalRelease() {
			if (hasExpired() == false) {
				expiration_date = System.currentTimeMillis();
			}
			active_locks.remove(this);
		}
		
		public String getTargetId() {
			return target_id;
		}
		
		private AlreadyBusyLock createBusyException() {
			if (local_locker != null) {
				return new AlreadyBusyLock(target_id, expiration_date, local_locker.getName() + " [" + local_locker.getId() + "]");
			} else if (node != null) {
				return new AlreadyBusyLock(target_id, expiration_date, node.toString());
			} else {
				return null;
			}
		}
	}
	
	public class RequestLockAcquire extends RequestHandler<DistributedLock> {
		
		public RequestLockAcquire(PoolManager pool_manager) {
			super(pool_manager);
		}
		
		public String getHandleName() {
			return "lock-acquire";
		}
		
		protected boolean isCloseChannelRequest(DistributedLock options) {
			return false;
		}
		
		public void onRequest(DataBlock block, Node source_node) {
			JsonObject jo = block.getJsonDatas().getAsJsonObject();
			String target_id = jo.get("target_id").getAsString();
			long expiration_date = jo.get("expiration_date").getAsLong();
			
			try {
				externalAcquireLock(target_id, expiration_date, source_node);
			} catch (AlreadyBusyLock e) {
				source_node.sendRequest(RequestLockAlreadyBusy.class, e);
			}
		}
		
		public DataBlock createRequest(DistributedLock lock) {
			JsonObject jo = new JsonObject();
			jo.addProperty("target_id", lock.getTargetId());
			jo.addProperty("expiration_date", lock.expiration_date);
			return new DataBlock(this, jo);
		}
		
	}
	
	public class RequestLockAlreadyBusy extends RequestHandler<AlreadyBusyLock> {
		
		public RequestLockAlreadyBusy(PoolManager pool_manager) {
			super(pool_manager);
		}
		
		public String getHandleName() {
			return "lock-alreadybusy";
		}
		
		protected boolean isCloseChannelRequest(AlreadyBusyLock options) {
			return false;
		}
		
		public void onRequest(DataBlock block, Node source_node) {
			String target_id = block.getStringDatas();
			
			node_by_already_busy_lock_target_id.put(target_id, source_node);
			
			scheduled_ex_service.schedule(() -> {
				node_by_already_busy_lock_target_id.remove(target_id);
			}, GRACE_TIME_FOR_GET_LOCK, TimeUnit.MILLISECONDS);
		}
		
		public DataBlock createRequest(AlreadyBusyLock busy_lock) {
			return new DataBlock(this, busy_lock.target_id);
		}
		
	}
	
	public class RequestLockRelease extends RequestHandler<DistributedLock> {
		
		public RequestLockRelease(PoolManager pool_manager) {
			super(pool_manager);
		}
		
		public String getHandleName() {
			return "lock-release";
		}
		
		protected boolean isCloseChannelRequest(DistributedLock options) {
			return false;
		}
		
		public void onRequest(DataBlock block, Node source_node) {
			String target_id = block.getStringDatas();
			getLockByTarget(target_id).ifPresent(lock -> {
				if (lock.node.equalsThisUUID(source_node)) {
					lock.externalRelease();
				}
			});
		}
		
		public DataBlock createRequest(DistributedLock lock) {
			return new DataBlock(this, lock.getTargetId());
		}
		
	}
	
}
