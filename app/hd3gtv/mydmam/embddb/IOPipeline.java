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
 * Copyright (C) hdsdi3g for hd3g.tv 19 oct. 2017
 * 
*/
package hd3gtv.mydmam.embddb;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import hd3gtv.mydmam.MyDMAM;

/**
 * Mergue all IOs from Stores and all IOs from Network.
 */
class IOPipeline {
	private static Logger log = Logger.getLogger(IOPipeline.class);
	
	private final ConcurrentHashMap<Class<?>, DistributedStore<?>> all_stores;
	
	// TODO RequestHandler
	
	public IOPipeline() {
		// TODO Auto-generated constructor stub
		all_stores = new ConcurrentHashMap<>();
		// TODO use executor ?
	}
	
	/**
	 * Thread safe
	 */
	<T> void storeRegister(Class<T> wrapped_class, DistributedStore<T> store) {
		DistributedStore<?> current_store = all_stores.putIfAbsent(wrapped_class, store);
		if (current_store != null) {
			throw new RuntimeException("A store was previousely added for class " + wrapped_class);
		}
	}
	
	/**
	 * Thread safe
	 */
	CompletableFuture<Void> storeUnregister(Class<?> wrapped_class) {
		all_stores.remove(wrapped_class);
		return CompletableFuture.completedFuture(null); // TODO unregister for all Nodes instead of nothing
	}
	
	@SuppressWarnings("unchecked")
	private <T> DistributedStore<T> getStoreByClass(Class<T> wrapped_class) {
		DistributedStore<?> result = all_stores.get(wrapped_class);
		if (result == null) {
			return null;
		}
		return (DistributedStore<T>) result;
	}
	
	private DistributedStore<?> getStoreByClassName(String wrapped_class_name) {
		Class<?> wrapped_class = MyDMAM.factory.getClassByName(wrapped_class_name);
		if (wrapped_class == null) {
			return null;
		}
		return all_stores.get(wrapped_class);
	}
	
}
