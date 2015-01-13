/*
 * This file is part of MyDMAM
 * 
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
*/
package hd3gtv.mydmam.db.orm;

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.db.CassandraDb;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ConsistencyLevel;
import com.netflix.astyanax.recipes.locks.BusyLockException;
import com.netflix.astyanax.recipes.locks.ColumnPrefixDistributedRowLock;
import com.netflix.astyanax.recipes.locks.StaleLockException;
import com.netflix.astyanax.serializers.StringSerializer;

public class CrudOrmEngine<T extends CrudOrmModel> {
	
	private T element;
	private CassandraOrm<T> cassandra;
	
	public final ColumnFamily<String, String> getCF(T model) {
		return new ColumnFamily<String, String>(model.getCF_Name(), StringSerializer.get(), StringSerializer.get());
	}
	
	/**
	 * @param element Define internal element
	 */
	@SuppressWarnings("unchecked")
	public CrudOrmEngine(T element) throws ConnectionException, IOException {
		this.element = element;
		if (element == null) {
			throw new NullPointerException("\"element\" can't to be null");
		}
		cassandra = new CassandraOrm<T>((Class<T>) element.getClassInstance(), getCF(element));
	}
	
	public static CrudOrmEngine<CrudOrmModel> get(Class<?> entityclass) throws Exception {
		Constructor<?> constructor = entityclass.getDeclaredConstructor();
		constructor.setAccessible(true);
		CrudOrmModel object = (CrudOrmModel) constructor.newInstance();
		return new CrudOrmEngine<CrudOrmModel>(object);
	}
	
	/**
	 * Dynamic call
	 */
	public final void saveInternalElement() throws IOException, ConnectionException {
		if (element.key == null) {
			element.key = UUID.randomUUID().toString();
			Log2.log.debug("Generate manual key", new Log2Dump("key", element.key));
		}
		
		if (element.createdate == null) {
			element.createdate = new Date();
		}
		
		element.updatedate = new Date();
		
		cassandra.resetMutator();
		cassandra.pushObject(element);
		cassandra.executeMutation();
		
		element.onAfterSave();
	}
	
	/**
	 * Dynamic call
	 */
	public T getInternalElement() {
		return element;
	}
	
	/**
	 * Dynamic call, reaffect internal element if not null.
	 */
	public final T read(String key) throws ConnectionException {
		T element = cassandra.pullObject(key);
		if (element != null) {
			this.element = element;
		}
		return element;
	}
	
	/**
	 * Static call don't reaffect internal element.
	 */
	public final T staticRead(String key) throws ConnectionException {
		return cassandra.pullObject(key);
	}
	
	/**
	 * Static call
	 */
	public final List<T> read(String... keys) throws ConnectionException {
		if (keys.length == 0) {
			return null;
		}
		return cassandra.pullObjects(keys);
	}
	
	/**
	 * Static call
	 */
	public final List<T> read(Collection<String> keys) throws ConnectionException {
		if (keys.size() == 0) {
			return null;
		}
		return read(keys.toArray(new String[keys.size()]));
	}
	
	/**
	 * Dynamic call, reaffect internal element.
	 */
	@SuppressWarnings("unchecked")
	public final T create() throws Exception {
		Constructor<?> constructor = element.getClass().getDeclaredConstructor();
		constructor.setAccessible(true);
		element = (T) constructor.newInstance();
		return element;
	}
	
	/**
	 * Static call
	 */
	public final boolean exists(String key) throws ConnectionException {
		return cassandra.exists(key);
	}
	
	/**
	 * Static call
	 */
	public final List<T> list() throws Exception {
		return cassandra.pullAllObjects();
	}
	
	/**
	 * Static call
	 */
	public final int count() throws Exception {
		return cassandra.countAllRows();
	}
	
	/**
	 * Static call
	 */
	public final void delete(String key) throws ConnectionException {
		cassandra.delete(key);
	}
	
	/**
	 * Static call
	 */
	public final void truncate() throws ConnectionException {
		cassandra.truncateColumnFamily();
	}
	
	private ColumnPrefixDistributedRowLock<String> lock;
	
	/**
	 * 1 second
	 */
	public boolean aquireLock() throws ConnectionException {
		return aquireLock(1, TimeUnit.SECONDS);
	}
	
	public boolean aquireLock(long timeout, TimeUnit unit) {
		try {
			if (lock != null) {
				releaseLock();
			}
			lock = new ColumnPrefixDistributedRowLock<String>(CassandraDb.getkeyspace(), cassandra.getColumnfamily(), "CRUD_ORM_LOCK_FOR_" + element.getClass().getSimpleName());
			lock.withConsistencyLevel(ConsistencyLevel.CL_ALL);
			lock.expireLockAfter(timeout, unit);
			lock.failOnStaleLock(false);
			lock.acquire();
			return true;
		} catch (StaleLockException e) {
			// The row contains a stale or these can either be manually clean up or automatically cleaned up (and ignored) by calling failOnStaleLock(false)
			Log2.log.error("Can't lock : abandoned lock...", e, new Log2Dump("class", element.getClass().getName()));
		} catch (BusyLockException e) {
			Log2.log.error("Can't lock, this category is currently locked...", e, new Log2Dump("class", element.getClass().getName()));
		} catch (Exception e) {
			Log2.log.error("Generic error", e, new Log2Dump("class", element.getClass().getName()));
		} finally {
			releaseLock();
		}
		return false;
	}
	
	public void releaseLock() {
		try {
			if (lock != null) {
				lock.release();
			}
			lock = null;
		} catch (Exception e) {
			Log2.log.error("Can't relase properly lock", e, new Log2Dump("class", element.getClass().getName()));
		}
	}
	
}
