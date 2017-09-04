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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.apache.log4j.Logger;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.JsonObject;

import hd3gtv.mydmam.embddb.network.DataBlock;
import hd3gtv.mydmam.embddb.network.Node;
import hd3gtv.mydmam.embddb.network.PoolManager;
import hd3gtv.mydmam.embddb.network.RequestHandler;

public final class LockEngine {
	
	private static final long GRACE_TIME_FOR_GET_LOCK = TimeUnit.SECONDS.toMillis(2);
	private static Logger log = Logger.getLogger(LockEngine.class);
	
	private final ArrayList<DistributedLock> active_locks;// TODO replace by ConcurrentHashMap ?
	private final PoolManager poolmanager;
	private final LoadingCache<String, ArrayList<NodeEvent>> node_events_by_target_id;
	
	private ScheduledExecutorService scheduled_ex_service;
	
	LockEngine(PoolManager poolmanager) {
		this.poolmanager = poolmanager;
		if (poolmanager == null) {
			throw new NullPointerException("\"poolmanager\" can't to be null");
		}
		
		active_locks = new ArrayList<>();
		
		poolmanager.addRequestHandler(new RequestLockAcquire(poolmanager));
		poolmanager.addRequestHandler(new RequestLockRelease(poolmanager));
		poolmanager.addRequestHandler(new RequestLockAlreadyBusy(poolmanager));
		poolmanager.addRequestHandler(new RequestLockAccept(poolmanager));
		
		poolmanager.addRemoveNodeCallback(node -> {
			synchronized (active_locks) {
				active_locks.removeIf(lock -> {
					return node.equals(lock.node);
				});
			}
		});
		
		scheduled_ex_service = Executors.newSingleThreadScheduledExecutor();
		scheduled_ex_service.scheduleAtFixedRate(() -> {
			synchronized (active_locks) {
				active_locks.removeIf(lock -> {
					return lock.hasExpired();
				});
			}
		}, 60, 60, TimeUnit.SECONDS);
		
		CacheLoader<String, ArrayList<NodeEvent>> c = new CacheLoader<String, ArrayList<NodeEvent>>() {
			public ArrayList<NodeEvent> load(String target_id) throws Exception {
				return new ArrayList<>();
			}
		};
		node_events_by_target_id = CacheBuilder.newBuilder().weakKeys().maximumSize(10000).expireAfterWrite(GRACE_TIME_FOR_GET_LOCK * 2, TimeUnit.MILLISECONDS).build(c);
	}
	
	enum NodeEventType {
		ACCEPT, REFUSE
	};
	
	private class NodeEvent {
		Node node;
		NodeEventType type;
		long expiration_date;
		
		private NodeEvent(Node node) {
			this.node = node;
			this.type = NodeEventType.ACCEPT;
			this.expiration_date = 0;
		}
		
		private NodeEvent(Node node, long expiration_date) {
			this.node = node;
			this.type = NodeEventType.REFUSE;
			this.expiration_date = expiration_date;
		}
		
		boolean refuse() {
			return type == NodeEventType.REFUSE;
		}
		
		DistributedLock createLock(String target_id) {
			return new DistributedLock(target_id, expiration_date, node);
		}
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
				if (actual_lock.hasExpired()) {
					actual_lock.setNewTTL(unit.toMillis(duration));
				}
				log.trace("Wanted a lock already locked by me " + actual_lock);
				return actual_lock;
			} else {
				if (actual_lock.hasExpired() == false) {
					log.trace("Wanted an actually busy lock " + actual_lock + " is this host");
					throw actual_lock.createBusyException();
				} else {
					active_locks.remove(actual_lock);
				}
			}
		}
		
		DistributedLock new_lock = new DistributedLock(target_id, unit.toMillis(duration));
		
		Predicate<Node> notForOuttimeNodes = node -> {
			return node.isOutOfTime(unit.toMillis(duration), unit.toMillis(duration)) == false;
		};
		
		ArrayList<Node> requested_nodes = new ArrayList<>(poolmanager.sayToAllNodes(RequestLockAcquire.class, new_lock, notForOuttimeNodes));
		if (requested_nodes.isEmpty()) {
			// log.warn("No nodes for get lock " + target_id); //TODO re set
			active_locks.add(new_lock);
			return new_lock;
		}
		
		long end_time_to_wait = System.currentTimeMillis() + GRACE_TIME_FOR_GET_LOCK;
		
		try {
			while (true) {
				ArrayList<NodeEvent> responded_nodes = node_events_by_target_id.getIfPresent(target_id);
				if (responded_nodes == null) {
					Thread.sleep(10);
					continue;
				}
				
				Optional<NodeEvent> refuse_node = responded_nodes.stream().filter(ne -> {
					return ne.refuse();
				}).findFirst();
				
				if (refuse_node.isPresent()) {
					new_lock = refuse_node.get().createLock(target_id);
					active_locks.add(new_lock);
					throw new_lock.createBusyException();
				}
				
				requested_nodes.removeIf(node -> {
					return responded_nodes.stream().anyMatch(n -> {
						return n.equals(node);
					});
				});
				if (requested_nodes.isEmpty()) {
					break;
				}
				
				if (end_time_to_wait < System.currentTimeMillis()) {
					break;
				}
				Thread.sleep(50);
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		active_locks.add(new_lock);
		return new_lock;
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
		
		private DistributedLock setNewTTL(long ttl) {
			expiration_date = System.currentTimeMillis() + ttl;
			return this;
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
		
		/**
		 * Warning: lock has maybe expired !
		 */
		public boolean isAlreadyForMe() {
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
			poolmanager.sayToAllNodes(RequestLockRelease.class, this, null);
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
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("\"");
			sb.append(target_id);
			sb.append("\" by ");
			if (local_locker != null) {
				sb.append(local_locker.getName());
				sb.append("#");
				sb.append(local_locker.getId());
			}
			if (node != null) {
				sb.append(node);
			}
			if (hasExpired()) {
				sb.append(" EXPIRED");
			} else {
				sb.append(" up to ");
				sb.append(expiration_date - System.currentTimeMillis());
				sb.append(" ms");
			}
			return sb.toString();
		}
		
	}
	
	public class RequestLockAcquire extends RequestHandler<DistributedLock> {
		
		public RequestLockAcquire(PoolManager pool_manager) {
			super(pool_manager);
		}
		
		public String getHandleName() {
			return "lock-acquire";
		}
		
		public void onRequest(DataBlock block, Node source_node) {
			JsonObject jo = block.getJsonDatas().getAsJsonObject();
			String target_id = jo.get("target_id").getAsString();
			long expiration_date = jo.get("expiration_date").getAsLong();
			
			try {
				externalAcquireLock(target_id, expiration_date, source_node);
				source_node.sendRequest(RequestLockAccept.class, target_id);
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
		
		public void onRequest(DataBlock block, Node source_node) {
			JsonObject jo = block.getJsonDatas().getAsJsonObject();
			String target_id = jo.get("target_id").getAsString();
			long expiration_date = jo.get("expiration_date").getAsLong();
			
			try {
				node_events_by_target_id.get(target_id, () -> {
					return new ArrayList<>();
				}).add(new NodeEvent(source_node, expiration_date));
			} catch (ExecutionException e) {
				log.error("Can't get target lock " + target_id, e);
			}
		}
		
		public DataBlock createRequest(AlreadyBusyLock busy_lock) {
			JsonObject jo = new JsonObject();
			jo.addProperty("target_id", busy_lock.target_id);
			jo.addProperty("expiration_date", busy_lock.expiration_date);
			return new DataBlock(this, jo);
		}
		
	}
	
	public class RequestLockRelease extends RequestHandler<DistributedLock> {
		
		public RequestLockRelease(PoolManager pool_manager) {
			super(pool_manager);
		}
		
		public String getHandleName() {
			return "lock-release";
		}
		
		public void onRequest(DataBlock block, Node source_node) {
			String target_id = block.getStringDatas();
			getLockByTarget(target_id).ifPresent(lock -> {
				if (lock.node.equalsThisUUID(source_node)) {
					lock.externalRelease();
				}
			});
			
			node_events_by_target_id.invalidate(target_id);
		}
		
		public DataBlock createRequest(DistributedLock lock) {
			return new DataBlock(this, lock.getTargetId());
		}
		
	}
	
	public class RequestLockAccept extends RequestHandler<String> {
		
		public RequestLockAccept(PoolManager pool_manager) {
			super(pool_manager);
		}
		
		public String getHandleName() {
			return "lock-accept";
		}
		
		public void onRequest(DataBlock block, Node source_node) {
			String target_id = block.getStringDatas();
			try {
				node_events_by_target_id.get(target_id, () -> {
					return new ArrayList<>();
				}).add(new NodeEvent(source_node));
			} catch (ExecutionException e) {
				log.error("Can't get target lock " + target_id, e);
			}
		}
		
		public DataBlock createRequest(String target_id) {
			return new DataBlock(this, target_id);
		}
		
	}
	
}
