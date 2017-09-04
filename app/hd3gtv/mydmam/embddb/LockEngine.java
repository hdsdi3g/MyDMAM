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
import java.util.function.Predicate;

import org.apache.log4j.Logger;

import com.google.gson.JsonObject;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.embddb.network.DataBlock;
import hd3gtv.mydmam.embddb.network.Node;
import hd3gtv.mydmam.embddb.network.PoolManager;
import hd3gtv.mydmam.embddb.network.RequestHandler;

public final class LockEngine {
	
	private static final long GRACE_TIME_FOR_GET_LOCK = TimeUnit.SECONDS.toMillis(2);
	private static Logger log = Logger.getLogger(LockEngine.class);
	
	private final ArrayList<DistributedLock> active_locks;
	private final PoolManager poolmanager;
	private final ConcurrentHashMap<String, ArrayList<NodeEvent>> node_events_by_target_id;
	
	private ScheduledExecutorService scheduled_ex_service;
	
	LockEngine(PoolManager poolmanager) {
		this.poolmanager = poolmanager;
		if (poolmanager == null) {
			throw new NullPointerException("\"poolmanager\" can't to be null");
		}
		
		active_locks = new ArrayList<>();
		node_events_by_target_id = new ConcurrentHashMap<>();
		
		poolmanager.addRequestHandler(new RequestLockAcquire(poolmanager));
		poolmanager.addRequestHandler(new RequestLockRelease(poolmanager));
		poolmanager.addRequestHandler(new RequestLockAlreadyBusy(poolmanager));
		poolmanager.addRequestHandler(new RequestLockAccept(poolmanager));
		
		poolmanager.addRemoveNodeCallback(node -> {
			synchronized (active_locks) {
				active_locks.removeIf(lock -> {
					if (node.equals(lock.node)) {
						node_events_by_target_id.remove(lock.target_id);
						return true;
					}
					return false;
				});
			}
		});
		
		scheduled_ex_service = Executors.newSingleThreadScheduledExecutor();
		scheduled_ex_service.scheduleAtFixedRate(() -> {
			synchronized (active_locks) {
				log.debug("Start GC expired locks for " + active_locks.size() + " lock(s)");
				active_locks.removeIf(lock -> {
					if (lock.hasExpired()) {
						node_events_by_target_id.remove(lock.target_id);
						return true;
					}
					return false;
				});
			}
		}, 60, 60, TimeUnit.SECONDS);
		
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
					active_locks.remove(actual_lock);
					node_events_by_target_id.remove(actual_lock.target_id);
				} else {
					return actual_lock;
				}
			} else {
				if (actual_lock.hasExpired() == false) {
					log.trace("Wanted an actually busy lock " + actual_lock + " is this host");
					throw actual_lock.createBusyException();
				} else {
					node_events_by_target_id.remove(actual_lock.target_id);
					active_locks.remove(actual_lock);
				}
			}
		}
		
		DistributedLock new_lock = new DistributedLock(target_id, unit.toMillis(duration));
		
		Predicate<Node> notForOuttimeNodes = node -> {
			return node.isOutOfTime(unit.toMillis(duration)) == false;
		};
		
		node_events_by_target_id.remove(target_id);
		
		ArrayList<Node> requested_nodes = new ArrayList<>(poolmanager.sayToAllNodes(RequestLockAcquire.class, new_lock, notForOuttimeNodes));
		if (requested_nodes.isEmpty()) {
			log.debug("No nodes for get lock " + target_id);
			active_locks.add(new_lock);
			return new_lock;
		}
		
		long end_time_to_wait = System.currentTimeMillis() + GRACE_TIME_FOR_GET_LOCK;
		
		try {
			while (true) {
				ArrayList<NodeEvent> responded_nodes = node_events_by_target_id.get(target_id);
				
				if (end_time_to_wait < System.currentTimeMillis()) {
					if (log.isDebugEnabled()) {
						String rsp = "0";
						if (responded_nodes != null) {
							rsp = String.valueOf(responded_nodes.size());
						}
						log.debug("Wait too long time for get all responses for \"" + target_id + "\". Responded: " + rsp + ", last requested nodes: " + requested_nodes.size());
					}
					break;
				}
				
				if (responded_nodes == null) {
					Thread.sleep(10);
					log.trace("Loop wait first response for \"" + target_id + "\"... until " + Loggers.dateLog(end_time_to_wait));
					continue;
				}
				if (log.isTraceEnabled()) {
					log.trace("Loop wait all response for \"" + target_id + "\". Responded: " + responded_nodes.size() + ", last requested nodes: " + requested_nodes.size() + " until " + Loggers.dateLog(end_time_to_wait));
				}
				
				Optional<NodeEvent> refuse_node = responded_nodes.stream().filter(ne -> {
					return ne.refuse();
				}).findFirst();
				
				if (refuse_node.isPresent()) {
					new_lock = refuse_node.get().createLock(target_id);
					active_locks.add(new_lock);
					log.debug("I want to lock a target \"" + new_lock.target_id + "\" already locked by " + new_lock.node);
					throw new_lock.createBusyException();
				}
				
				responded_nodes.forEach(n -> {
					requested_nodes.remove(n.node);
				});
				if (requested_nodes.isEmpty()) {
					break;
				}
				Thread.sleep(20);
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		active_locks.add(new_lock);
		return new_lock;
	}
	
	private synchronized void externalAcquireLock(String target_id, long expiration_date, Node locker_node) throws AlreadyBusyLock {
		if (expiration_date < System.currentTimeMillis()) {
			log.debug(locker_node + " try to push an expired lock \"" + target_id + "\" the " + Loggers.dateLog(expiration_date));
			return;
		}
		
		Optional<DistributedLock> o_lock = getLockByTarget(target_id);
		
		if (o_lock.isPresent()) {
			DistributedLock actual_lock = o_lock.get();
			if (actual_lock.local_locker != null) {
				if (actual_lock.hasExpired()) {
					active_locks.remove(actual_lock);
					node_events_by_target_id.remove(actual_lock.target_id);
				} else {
					log.debug(locker_node + " want to lock a target already locked by me (" + actual_lock + "). It's not possible");
					throw actual_lock.createBusyException();
				}
			} else if (locker_node.equalsThisUUID(actual_lock.node)) {
				actual_lock.expiration_date = expiration_date;
			} else {
				log.trace(locker_node + " try to get a lock previously acquired by another node (" + actual_lock + ")");
				/**
				 * But it not our problem.
				 */
			}
		} else {
			DistributedLock new_external_lock = new DistributedLock(target_id, expiration_date, locker_node);
			log.trace("Store an external lock (" + new_external_lock + ")");
			active_locks.add(new_external_lock);
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
			node_events_by_target_id.remove(target_id);
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
			
			if (log.isTraceEnabled()) {
				log.trace("Receive an Already busy for \"" + target_id + "\" until " + Loggers.dateLog(expiration_date) + " by " + source_node);
			}
			
			node_events_by_target_id.computeIfAbsent(target_id, _id -> {
				return new ArrayList<>(1);
			}).add(new NodeEvent(source_node, expiration_date));
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
			
			node_events_by_target_id.remove(target_id);
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
			if (log.isTraceEnabled()) {
				log.trace("Receive an Accept for \"" + target_id + "\" by " + source_node);
			}
			
			node_events_by_target_id.computeIfAbsent(target_id, _id -> {
				return new ArrayList<>(1);
			}).add(new NodeEvent(source_node));
		}
		
		public DataBlock createRequest(String target_id) {
			return new DataBlock(this, target_id);
		}
		
	}
	
}
