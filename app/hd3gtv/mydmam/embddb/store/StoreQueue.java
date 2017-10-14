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
 * Copyright (C) hdsdi3g for hd3g.tv 15 sept. 2017
 * 
*/
package hd3gtv.mydmam.embddb.store;

import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.log4j.Logger;

import hd3gtv.tools.ThreadPoolExecutorFactory;

@Deprecated
class StoreQueue {
	
	private static Logger log = Logger.getLogger(StoreQueue.class);
	
	private final LinkedBlockingQueue<Runnable> waiting = new LinkedBlockingQueue<>();
	private final LinkedBlockingQueue<Runnable> cleanup = new LinkedBlockingQueue<>();
	private final Runnable cleanUpTask;
	// private final LoopProcessor loop_processor;
	private String name;
	private final ThreadPoolExecutorFactory executor;
	private ScheduledExecutorService scheduled_ex_service;
	
	StoreQueue(String name, Supplier<Boolean> elegiblityToCleanUp, Runnable cleanUpTask, ThreadPoolExecutorFactory executor) {
		this.name = name;
		if (name == null) {
			throw new NullPointerException("\"name\" can't to be null");
		}
		if (elegiblityToCleanUp == null) {
			throw new NullPointerException("\"elegiblityToCleanUp\" can't to be null");
		}
		this.cleanUpTask = cleanUpTask;
		if (cleanUpTask == null) {
			throw new NullPointerException("\"cleanUpTask\" can't to be null");
		}
		this.executor = executor;
		if (executor == null) {
			throw new NullPointerException("\"executor\" can't to be null");
		}
		
		new LoopProcessor();
		
		scheduled_ex_service = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setDaemon(false);
				t.setName("Store cleaner for " + name);
				t.setPriority(Thread.MIN_PRIORITY + 2);
				return t;
			}
		});
		scheduled_ex_service.scheduleAtFixedRate(() -> {
			if (elegiblityToCleanUp.get()) {
				log.debug("Store " + name + " needs to be clean");
				cleanUp();
			}
		}, 1, 1, TimeUnit.SECONDS);
	}
	
	private class LoopProcessor extends Thread {
		public LoopProcessor() {
			setDaemon(false);
			setName("EMBDDB Store Queue " + name);
			setPriority(Thread.MIN_PRIORITY + 1);
			start();
		}
		
		public void run() {
			Predicate<Future<?>> thisTaskisDone = task -> {
				return task.isDone();
			};
			
			try {
				while (true) {
					Runnable clean_task = null;
					
					/**
					 * LinkedList for manage random deleting.
					 */
					LinkedList<Future<?>> all_pending = new LinkedList<>();
					
					while ((clean_task = cleanup.poll()) == null) {
						log.trace("Wait new task for " + name);
						
						all_pending.add(executor.submit(waiting.take()));
						
						/**
						 * Regular clean to avoid big list
						 */
						if (all_pending.size() > 1000) {
							log.trace("Clean pending list (remove done) for " + name);
							all_pending.removeIf(thisTaskisDone);
						}
					}
					
					if (all_pending.size() > 1 && log.isTraceEnabled()) {
						log.trace("Prepare clean task operation for " + name + ", " + all_pending.size() + " pending item(s) to wait the process ends");
					}
					
					/**
					 * Wait all are done
					 */
					while (all_pending.isEmpty() == false) {
						all_pending.stream().filter(task -> {
							return task.isDone() == false;
						}).findFirst().ifPresent(notYetDoneTask -> {
							try {
								notYetDoneTask.get();
							} catch (InterruptedException | ExecutionException e) {
								log.debug("Error during task wait", e);
							}
						});
						
						all_pending.removeIf(thisTaskisDone);
						
						if (all_pending.isEmpty() == false && log.isTraceEnabled()) {
							log.trace("Needs to wait again to start the clean task for " + name + " (" + all_pending.size() + " pending item(s) to wait the process ends)");
						}
					}
					
					log.debug("Start clean task operation for " + name);
					clean_task.run();
				}
			} catch (InterruptedException e) {
				log.error("Cancel Store queue", e);
			}
		}
	}
	
	void put(Runnable r) {
		waiting.add(r);
	}
	
	void cleanUp() {
		cleanup.add(() -> {
			cleanUpTask.run();
			cleanup.clear();
		});
	}
	
}
