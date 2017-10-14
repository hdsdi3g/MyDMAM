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
 * Copyright (C) hdsdi3g for hd3g.tv 14 oct. 2017
 * 
*/
package hd3gtv.tools;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import junit.framework.TestCase;

public class ThreadPoolExecutorFactoryTest extends TestCase {
	
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
	
}
