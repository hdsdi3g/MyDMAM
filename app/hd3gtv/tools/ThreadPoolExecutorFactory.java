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
 * Copyright (C) hdsdi3g for hd3g.tv 9 sept. 2017
 * 
*/
package hd3gtv.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import com.google.gson.JsonObject;

import hd3gtv.mydmam.gson.GsonIgnore;

@GsonIgnore
public class ThreadPoolExecutorFactory {
	
	private static Logger log = Logger.getLogger(ThreadPoolExecutorFactory.class);
	private final LinkedBlockingQueue<Runnable> queue;
	private final ThreadPoolExecutor executor;
	private final static int MAX_POOL_SIZE = Runtime.getRuntime().availableProcessors();
	
	/**
	 * @param thread_priority @see Thread.MIN_PRIORITY and Thread.MAX_PRIORITY
	 * @param uncaughtException can be null
	 */
	public ThreadPoolExecutorFactory(String base_thread_name, int thread_priority, int queue_max_size, Consumer<Throwable> uncaughtException) {
		if (base_thread_name == null) {
			throw new NullPointerException("\"base_thread_name\" can't to be null");
		}
		if (thread_priority > Thread.MAX_PRIORITY) {
			throw new IndexOutOfBoundsException("thread_priority can be > " + Thread.MAX_PRIORITY);
		}
		if (thread_priority < Thread.MIN_PRIORITY) {
			throw new IndexOutOfBoundsException("thread_priority can be < " + Thread.MIN_PRIORITY);
		}
		if (queue_max_size < 1) {
			queue_max_size = MAX_POOL_SIZE;
		}
		queue = new LinkedBlockingQueue<Runnable>(queue_max_size);
		
		executor = new ThreadPoolExecutor(MAX_POOL_SIZE, MAX_POOL_SIZE, 0L, TimeUnit.MILLISECONDS, queue);
		/*executor.setRejectedExecutionHandler((r, executor) -> {
			log.error("Too many task to be executed at the same time for \"" + base_thread_name + "\" ! This will not proceed: " + r);
		});*/
		executor.setThreadFactory(runnable -> {
			Thread t = new Thread(runnable);
			t.setDaemon(true);
			t.setName(base_thread_name);
			t.setPriority(thread_priority);
			t.setUncaughtExceptionHandler((thread, throwable) -> {
				if (uncaughtException == null) {
					log.error("Uncaught exception for a Thread \"" + base_thread_name + "\"", throwable);
				} else {
					uncaughtException.accept(throwable);
				}
			});
			return t;
		});
	}
	
	public ThreadPoolExecutorFactory(String base_thread_name, int queue_max_size) {
		this(base_thread_name, Thread.NORM_PRIORITY, queue_max_size, null);
	}
	
	/**
	 * @param uncaughtException can be null
	 */
	public ThreadPoolExecutorFactory(String base_thread_name, int queue_max_size, Consumer<Throwable> uncaughtException) {
		this(base_thread_name, Thread.NORM_PRIORITY, queue_max_size, uncaughtException);
	}
	
	/**
	 * @param thread_priority @see Thread.MIN_PRIORITY and Thread.MAX_PRIORITY
	 */
	public ThreadPoolExecutorFactory(String base_thread_name, int thread_priority, int queue_max_size) {
		this(base_thread_name, thread_priority, queue_max_size, null);
	}
	
	public ThreadPoolExecutor getThreadPoolExecutor() {
		return executor;
	}
	
	public boolean waitForRun(Runnable r) {// TODO rename waitForRun
		if (r == null) {
			return false;
		}
		if (executor.isShutdown() | executor.isTerminating() | executor.isTerminated()) {
			return false;
		}
		waitToCanToAdd();
		try {
			executor.execute(r);
			return true;
		} catch (RejectedExecutionException e) {
			log.error("Rejected execution for " + r);
			return false;
		}
	}
	
	public boolean isRunning() {
		return executor.isShutdown() == false && executor.isTerminated() == false && executor.isTerminating() == false;
	}
	
	public void awaitTerminationAndShutdown(long timeout, TimeUnit unit) {
		executor.shutdown();
		try {
			executor.awaitTermination(timeout, unit);
		} catch (InterruptedException e) {
			log.error("Can't wait to stop executor...", e);
			executor.shutdownNow();
		}
	}
	
	private class IntermediateProcessor<T, R> {
		T source;
		CompletableFuture<ProcessingResult<T, R>> c_future;
		
		IntermediateProcessor(T source, Function<T, ProcessingResult<T, R>> processor) {
			this.source = source;
			c_future = CompletableFuture.supplyAsync(() -> {
				return (ProcessingResult<T, R>) (processor.apply(source));
			}, executor);
		}
		
		ProcessingResult<T, R> get(long timeout, TimeUnit unit) {
			try {
				if (timeout > 0) {
					return c_future.get(timeout, unit);
				} else {
					return c_future.get();
				}
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				return new ProcessingResult<T, R>(source, e);
			}
		}
		
	}
	
	/**
	 * Blocking
	 */
	private void waitToCanToAdd() {
		while (queue.remainingCapacity() < MAX_POOL_SIZE | executor.isTerminated()) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				throw new RuntimeException("Can't insert items in executor", e);
			}
			if (executor.isTerminated()) {
				return;
			}
		}
	}
	
	/**
	 * Blocking !
	 * @param timeout_for_each_task can be null
	 * @return all results, never null.
	 */
	public <T, R> Stream<ProcessingResult<T, R>> multipleProcessing(Stream<T> items_to_process, FunctionWithException<T, R> processor, long timeout_for_each_task, TimeUnit unit) {
		Function<T, ProcessingResult<T, R>> doProcess = item -> {
			try {
				return new ProcessingResult<T, R>(item, processor.get(item));
			} catch (Throwable e) {
				return new ProcessingResult<T, R>(item, e);
			}
		};
		
		return items_to_process.parallel().map(item -> {
			waitToCanToAdd();
			return new IntermediateProcessor<T, R>(item, doProcess);
		}).map(c -> {
			return c.get(timeout_for_each_task, unit);
		});
	}
	
	public void toTableList(TableList table) {
		table.addRow("Active", String.valueOf(executor.getActiveCount()));
		table.addRow("Max capacity", String.valueOf(queue.remainingCapacity()));
		table.addRow("Completed", String.valueOf(executor.getCompletedTaskCount()));
		table.addRow("Core pool", String.valueOf(executor.getCorePoolSize()));
		table.addRow("Pool", String.valueOf(executor.getPoolSize()));
		table.addRow("Largest pool", String.valueOf(executor.getLargestPoolSize()));
		table.addRow("Maximum pool", String.valueOf(executor.getMaximumPoolSize()));
	}
	
	/**
	 * @return a copy (unmodifiable queue list), never null
	 */
	public List<Runnable> getActualQueue() {
		synchronized (queue) {
			return Collections.unmodifiableList(new ArrayList<Runnable>(queue));
		}
	}
	
	public JsonObject actualStatustoJson() {
		JsonObject jo_executor_pool = new JsonObject();
		jo_executor_pool.addProperty("active", String.valueOf(executor.getActiveCount()));
		jo_executor_pool.addProperty("shutdown", executor.isShutdown());
		jo_executor_pool.addProperty("terminating", executor.isTerminating());
		jo_executor_pool.addProperty("terminated", executor.isTerminated());
		
		if (queue != null) {
			jo_executor_pool.addProperty("max_capacity", String.valueOf(queue.remainingCapacity()));
		} else {
			jo_executor_pool.addProperty("max_capacity", -1);
		}
		jo_executor_pool.addProperty("completed", String.valueOf(executor.getCompletedTaskCount()));
		jo_executor_pool.addProperty("core_pool", String.valueOf(executor.getCorePoolSize()));
		jo_executor_pool.addProperty("pool", String.valueOf(executor.getPoolSize()));
		jo_executor_pool.addProperty("largest_pool", String.valueOf(executor.getLargestPoolSize()));
		jo_executor_pool.addProperty("maximum_pool", String.valueOf(executor.getMaximumPoolSize()));
		return jo_executor_pool;
	}
	
}
