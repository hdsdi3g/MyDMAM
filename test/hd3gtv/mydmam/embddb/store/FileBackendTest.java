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
 * Copyright (C) hdsdi3g for hd3g.tv 4 oct. 2017
 * 
*/
package hd3gtv.mydmam.embddb.store;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;

import hd3gtv.mydmam.embddb.store.FileBackend.StoreBackend;
import junit.framework.TestCase;

public class FileBackendTest extends TestCase {
	
	private final File backend_basedir;
	private final FileBackend all_backends;
	private static final String DB_NAME = "DB";
	
	public FileBackendTest() throws IOException {
		backend_basedir = new File(System.getProperty("user.home") + File.separator + "mydmam-debug");
		if (backend_basedir.exists()) {
			FileUtils.forceDelete(backend_basedir);
		}
		all_backends = new FileBackend(backend_basedir, UUID.fromString("00000000-0000-0000-0000-000000000000"));
	}
	
	public void testAll() throws Exception {
		StoreBackend backend = all_backends.get(DB_NAME, "testAll", 1000);
		
		int size = 10000;
		
		long start_time = System.currentTimeMillis();
		
		final String DEFAULT_PATH = "/default";
		
		/**
		 * Push datas in journal
		 */
		IntStream.range(0, size).parallel().forEach(i -> {
			ThreadLocalRandom random = ThreadLocalRandom.current();
			byte[] payload = new byte[random.nextInt(1, 100)];
			random.nextBytes(payload);
			try {
				Item item = new Item(DEFAULT_PATH, String.valueOf(i), payload);
				backend.writeInJournal(item, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		
		/**
		 * From https://www.mkyong.com/java/how-to-determine-a-prime-number-in-java/
		 */
		boolean[] primes = new boolean[10000];
		Arrays.fill(primes, true); // assume all integers are prime.
		primes[0] = primes[1] = false; // we know 0 and 1 are not prime.
		for (int i = 2; i < primes.length; i++) {
			// if the number is prime,
			// then go through all its multiples and make their values false.
			if (primes[i]) {
				for (int j = 2; i * j < primes.length; j++) {
					primes[i * j] = false;
				}
			}
		}
		
		final int PRIME_COUNT = (int) IntStream.range(0, size).parallel().filter(i -> {
			return primes[i];
		}).count();
		
		final String UPDATE_PATH = "/updated";
		final String DELETE_PATH = "/deleted";
		
		/**
		 * Update some datas in journal.
		 */
		IntStream.range(0, size).parallel().filter(i -> {
			return primes[i];
		}).forEach(i_prime -> {
			ThreadLocalRandom random = ThreadLocalRandom.current();
			byte[] payload = new byte[random.nextInt(1, 100)];
			random.nextBytes(payload);
			try {
				Item item = new Item(UPDATE_PATH, String.valueOf(i_prime), payload);
				backend.writeInJournal(item, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		
		/**
		 * Flip the journal
		 */
		backend.doDurableWritesAndRotateJournal();
		
		/**
		 * Now, read the datas
		 */
		int all_items = (int) backend.getAllDatas().parallel().map(entry -> {
			return new Item(entry);
		}).peek(item -> {
			assertFalse("Item " + item.getId() + " was deleted", item.isDeleted());
			int i = Integer.parseInt(item.getId());
			if (primes[i]) {
				assertEquals("Invalid path update for item " + item.getId(), UPDATE_PATH, item.getPath());
			} else {
				assertEquals("Invalid path update for item " + item.getId(), DEFAULT_PATH, item.getPath());
			}
		}).count();
		
		assertEquals("Invalid items retrived count", size, all_items);
		
		assertEquals("Bad path founded", PRIME_COUNT, (int) backend.getDatasByPath(UPDATE_PATH).count());
		assertEquals("Bad path founded", 0, backend.getDatasByPath(DELETE_PATH).count());
		assertEquals("Can't found some objects", size - PRIME_COUNT, backend.getDatasByPath(DEFAULT_PATH).count());
		
		backend.getDatasByPath(UPDATE_PATH).forEach(value -> {
			assertEquals(UPDATE_PATH, new Item(value).getPath());
		});
		backend.getDatasByPath(DEFAULT_PATH).forEach(value -> {
			assertEquals(DEFAULT_PATH, new Item(value).getPath());
		});
		
		long estimated_ttl = (System.currentTimeMillis() - start_time) + 300l;
		
		/**
		 * New update (delete non prime entries)
		 */
		IntStream.range(0, size).parallel().filter(i -> {
			return primes[i] == false;
		}).forEach(i_prime -> {
			try {
				Item item = new Item(DELETE_PATH, String.valueOf(i_prime), new byte[0]);
				item.setTTL(-1);
				backend.writeInJournal(item, System.currentTimeMillis() + estimated_ttl);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		
		/**
		 * Flip the journal
		 */
		backend.doDurableWritesAndRotateJournal();
		
		/**
		 * Check deleted items (non primes)
		 */
		IntStream.range(0, size).parallel().filter(i -> {
			return primes[i] == false;
		}).forEach(i_prime -> {
			try {
				ItemKey key = new ItemKey(String.valueOf(i_prime));
				assertTrue("Not contain " + i_prime, backend.contain(key));
				ByteBuffer value = backend.read(key);
				assertNotNull("Expired item: " + i_prime, value);
				assertTrue("Not marked deleted: " + i_prime, new Item(value).isDeleted());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		
		/**
		 * Wait expiration items...
		 */
		Thread.sleep(estimated_ttl);
		
		/**
		 * Check expired items (non primes)
		 */
		IntStream.range(0, size).parallel().filter(i -> {
			return primes[i] == false;
		}).forEach(i_prime -> {
			try {
				ItemKey key = new ItemKey(String.valueOf(i_prime));
				assertFalse("Contain " + i_prime, backend.contain(key));
				assertNull("Not deleted item: " + i_prime, backend.read(key));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		
		/**
		 * Check non expired items (primes)
		 */
		IntStream.range(0, size).parallel().filter(i -> {
			return primes[i];
		}).forEach(i_prime -> {
			try {
				ItemKey key = new ItemKey(String.valueOf(i_prime));
				assertTrue("Not contain " + i_prime, backend.contain(key));
				ByteBuffer value = backend.read(key);
				assertNotNull("Null: " + i_prime, value);
				assertNotSame("Non empty: " + i_prime, 0, value.remaining());
				assertEquals("Invalid path for item " + i_prime, UPDATE_PATH, new Item(value).getPath());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		
		assertEquals("Bad path founded", PRIME_COUNT, (int) backend.getDatasByPath(UPDATE_PATH).count());
		assertEquals("Found deleted objects", 0, backend.getDatasByPath(DELETE_PATH).count());
		assertEquals("Found deleted objects", 0, backend.getDatasByPath(DEFAULT_PATH).count());
		
		long data_file_size = backend.getDataFileSize();
		long index_path_file_size = backend.getIndexPathFileSize();
		
		backend.cleanUpFiles();
		
		assertTrue("Invalid cleanup", data_file_size > backend.getDataFileSize());
		assertTrue("Invalid cleanup", index_path_file_size > backend.getIndexPathFileSize());
		
		assertEquals("Bad path founded", PRIME_COUNT, (int) backend.getDatasByPath(UPDATE_PATH).count());
		assertEquals("Found deleted objects", 0, backend.getDatasByPath(DELETE_PATH).count());
		assertEquals("Found deleted objects", 0, backend.getDatasByPath(DEFAULT_PATH).count());
		
		backend.getDatasByPath(UPDATE_PATH).forEach(value -> {
			assertEquals("Can't found some objects", UPDATE_PATH, new Item(value).getPath());
		});
		
		try {
			backend.purge();
			FileUtils.forceDelete(backend_basedir);
		} catch (IOException e) {
			if (SystemUtils.IS_OS_WINDOWS == false) {
				throw e;
			}
		}
	}
	
	public void testOpenExistantJournal() throws Exception {
		StoreBackend backend = all_backends.get(DB_NAME, "testOpenExistantJournal", 1000);
		
		int size = 10000;
		
		/**
		 * Push datas in journal
		 */
		IntStream.range(0, size).parallel().forEach(i -> {
			ThreadLocalRandom random = ThreadLocalRandom.current();
			byte[] payload = new byte[random.nextInt(1, 100)];
			random.nextBytes(payload);
			try {
				Item item = new Item(null, String.valueOf(i), payload);
				backend.writeInJournal(item, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		
		backend.close();
		
		StoreBackend backend2 = all_backends.get(DB_NAME, "testOpenExistantJournal", 1000);
		
		/**
		 * Now, read the datas
		 */
		int all_items = (int) backend2.getAllDatas().parallel().map(entry -> {
			return new Item(entry);
		}).count();
		
		assertEquals("Invalid items retrived count", size, all_items);
	}
}
