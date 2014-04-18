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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnFamily;
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
		}
		
		if (element.createdate == null) {
			element.createdate = new Date();
		}
		
		element.updatedate = new Date();
		
		cassandra.resetMutator();
		cassandra.pushObject(element, 0);
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
	 * Dynamic call, reaffect internal element.
	 */
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
	
}
