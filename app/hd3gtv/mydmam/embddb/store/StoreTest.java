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
 * Copyright (C) hdsdi3g for hd3g.tv 11 oct. 2017
 * 
*/
package hd3gtv.mydmam.embddb.store;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.tools.ThreadPoolExecutorFactory;
import junit.framework.TestCase;

public class StoreTest extends TestCase {
	
	public void testPause() throws Exception {
		
		ThreadPoolExecutorFactory exec = new ThreadPoolExecutorFactory("test", Thread.NORM_PRIORITY);
		AtomicBoolean is_locked = new AtomicBoolean(false);
		
		IntStream.range(0, 100).forEach(i -> {
			exec.execute(() -> {
				if (is_locked.get()) {
					throw new RuntimeException("WAS LOCKED !");
				}
				System.out.print("<"/* + Thread.currentThread().getId() + ">"*/);
				try {
					Thread.sleep(ThreadLocalRandom.current().nextInt(100, 1000));
				} catch (InterruptedException e) {
				}
				System.out.print(/*"</" + Thread.currentThread().getId() +*/ ">");
				if (is_locked.get()) {
					throw new RuntimeException("WAS LOCKED !");
				}
			});
		});
		
		IntStream.range(0, 10).forEach(i -> {
			try {
				Thread.sleep(ThreadLocalRandom.current().nextInt(500, 1000));
				
				exec.insertPauseTask(() -> {
					is_locked.set(true);
					System.out.println(i);
					is_locked.set(false);
				});
			} catch (Exception e) {
			}
		});
		
		exec.awaitTerminationAndShutdown(1, TimeUnit.HOURS);
	}
	
	public void AtestAll() throws Exception {// XXX
		
		ItemFactory<UUID> factory = new ItemFactory<UUID>() {
			
			public Item toItem(UUID element) {
				return new Item(MyDMAM.gson_kit.getGsonSimple().toJson(element).getBytes());
			};
			
			public UUID getFromItem(Item item) {
				return MyDMAM.gson_kit.getGsonSimple().fromJson(new String(item.getPayload()), UUID.class);
			}
			
			public Class<UUID> getType() {
				return UUID.class;
			}
			
		};
		
		File backend_basedir = new File(System.getProperty("user.home") + File.separator + "mydmam-debug");
		if (backend_basedir.exists()) {
			FileUtils.forceDelete(backend_basedir);
		}
		FileBackend backend = new FileBackend(backend_basedir, UUID.fromString("00000000-0000-0000-0000-000000000000"));
		
		Store<UUID> store = new Store<>("test", factory, backend, ReadCacheEhcache.getCache(), 1_000_000, 10_000, 10000);
		
		UUID newer = UUID.randomUUID();
		store.put(String.valueOf(0), newer, 1, TimeUnit.DAYS).thenRun(() -> {
			store.get(String.valueOf(0)).thenAccept(actual -> {
				assertNotSame(newer, actual);
			});
		}).get();// XXX do blocking...
		System.out.println("+");
		
		// TODO tests:
		// check NPE with completable
		
		// store.doDurableWrite();
		// store.exists(_id, onDone, onError);
		// store.get(_id, onDone, onNotFound, onError);
		// store.getAll(onDone, onError);
		// store.getByPath(path, onDone, onError);
		// store.put(element, ttl, unit, onDone, onError);
		// store.removeAllByPath(path, onDone, onError);
		// store.removeById(_id, onDone, onNotFound, onError);
		// store.truncate(onDone, onError);
		// open + close + open
	}
	
}
