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
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

/**
 * Bind StoreBackend and StoreItemFactory
 */
public final class Store<T> {
	
	private static Logger log = Logger.getLogger(Store.class);
	
	private StoreBackend backend;
	private StoreItemFactory<T> item_factory;
	
	public Store(StoreBackend backend, StoreItemFactory<T> item_factory) {
		this.backend = backend;
		if (backend == null) {
			throw new NullPointerException("\"backend\" can't to be null");
		}
	}
	
	public Store<T> put(T item) throws IOException {
		if (item == null) {
			throw new NullPointerException("\"item\" can't to be null");
		}
		backend.put(item_factory.toItem(item), 0);
		return this;
	}
	
	public Store<T> put(T item, long ttl, TimeUnit unit) throws IOException {
		if (item == null) {
			throw new NullPointerException("\"item\" can't to be null");
		}
		backend.put(item_factory.toItem(item), unit.toMillis(ttl));
		return this;
	}
	
	public Store<T> removeById(String _id) throws IOException {
		if (_id == null) {
			throw new NullPointerException("\"_id\" can't to be null");
		}
		backend.removeById(_id);
		return this;
	}
	
	public Store<T> removeAllByPath(String path) throws IOException {
		if (path == null) {
			throw new NullPointerException("\"path\" can't to be null");
		}
		backend.removeAllByPath(path);
		return this;
	}
	
	public Store<T> truncateDatabase() throws IOException {
		backend.truncateDatabase();
		return this;
	}
	
	public boolean exists(String _id) throws IOException {
		if (_id == null) {
			throw new NullPointerException("\"_id\" can't to be null");
		}
		return backend.exists(_id);
	}
	
	public Stream<T> getAll() throws IOException {
		return backend.getAll().map(item -> {
			return item_factory.getFromItem(item);
		});
	}
	
	public T get(String _id) throws IOException {
		if (_id == null) {
			throw new NullPointerException("\"_id\" can't to be null");
		}
		return item_factory.getFromItem(backend.get(_id));
	}
	
	public Stream<T> getByPath(String path) throws IOException {
		if (path == null) {
			throw new NullPointerException("\"path\" can't to be null");
		}
		return backend.getByPath(path).map(item -> {
			return item_factory.getFromItem(item);
		});
	}
	
	// TODO async I/O calls
	// TODO transfert data in nodes
	
}
