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

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import com.google.gson.JsonObject;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonIgnore;

@GsonIgnore
public class ThreadPoolExecutorFactory {
	
	private static Logger log = Logger.getLogger(ThreadPoolExecutorFactory.class);
	private final ThreadPoolExecutor executor;
	
	/**
	 * @param thread_priority @see Thread.MIN_PRIORITY and Thread.MAX_PRIORITY
	 * @param uncaughtException can be null
	 */
	public ThreadPoolExecutorFactory(String base_thread_name, int thread_priority, Consumer<Throwable> uncaughtException) {
		if (base_thread_name == null) {
			throw new NullPointerException("\"base_thread_name\" can't to be null");
		}
		if (thread_priority > Thread.MAX_PRIORITY) {
			throw new IndexOutOfBoundsException("thread_priority can be > " + Thread.MAX_PRIORITY);
		}
		if (thread_priority < Thread.MIN_PRIORITY) {
			throw new IndexOutOfBoundsException("thread_priority can be < " + Thread.MIN_PRIORITY);
		}
		executor = new ThreadPoolExecutor(MyDMAM.CPU_COUNT, MyDMAM.CPU_COUNT, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
		executor.setRejectedExecutionHandler((r, executor) -> {
			if (executor != null) {
				if (executor.isShutdown() | executor.isTerminated() | executor.isTerminating()) {
					log.error("Can't add newer task: executor for \"" + base_thread_name + "\" is closed/pending closing !");
					return;
				}
			}
			log.error("Too many task to be executed at the same time for \"" + base_thread_name + "\" ! This will not proceed: " + r);
		});
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
	
	public ThreadPoolExecutorFactory(String base_thread_name) {
		this(base_thread_name, Thread.NORM_PRIORITY, null);
	}
	
	/**
	 * @param uncaughtException can be null
	 */
	public ThreadPoolExecutorFactory(String base_thread_name, Consumer<Throwable> uncaughtException) {
		this(base_thread_name, Thread.NORM_PRIORITY, uncaughtException);
	}
	
	/**
	 * @param thread_priority @see Thread.MIN_PRIORITY and Thread.MAX_PRIORITY
	 */
	public ThreadPoolExecutorFactory(String base_thread_name, int thread_priority) {
		this(base_thread_name, thread_priority, null);
	}
	
	public ThreadPoolExecutorFactory setSimplePoolSize() {
		executor.setCorePoolSize(1);
		executor.setMaximumPoolSize(1);
		return this;
	}
	
	public ThreadPoolExecutor getThreadPoolExecutor() {
		return executor;
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
	
	public class SourcedCompletableFuture<T, R> {
		public final T processed_item;
		public final CompletableFuture<R> c_future;
		public final long queuing_date;
		private long start_date;
		private long end_date;
		private Throwable processing_error;
		
		private SourcedCompletableFuture(T processed_item, FunctionWithException<T, R> processor) {
			this.processed_item = processed_item;
			this.c_future = CompletableFuture.supplyAsync(() -> {
				try {
					start_date = System.currentTimeMillis();
					R result = processor.get(processed_item);
					end_date = System.currentTimeMillis();
					return result;
				} catch (Throwable e) {
					end_date = System.currentTimeMillis();
					processing_error = e;
					return null;
				}
			}, executor);
			queuing_date = System.currentTimeMillis();
		}
		
		/**
		 * queuing + processing
		 * @return -1 if no process
		 */
		public long getTotalProcessTime() {
			if (end_date > 0) {
				return end_date - queuing_date;
			} else {
				return -1;
			}
		}
		
		/**
		 * Only processing
		 * @return -1 if no process
		 */
		public long getProcessTime() {
			if (end_date > 0) {
				return end_date - start_date;
			} else {
				return -1;
			}
		}
		
		public boolean hasProcessingError() {
			if (c_future.isDone() == false) {
				return false;
			}
			return processing_error != null;
		}
		
		public Throwable getProcessingError() {
			return processing_error;
		}
	}
	
	public void execute(Runnable r) {
		executor.execute(r);
	}
	
	public Future<?> submit(Runnable r) {
		return executor.submit(r);
	}
	
	public <T> Future<T> submit(Callable<T> task) {
		return executor.submit(task);
	}
	
	/**
	 * @param items_to_process called by the CompletableFuture return.
	 * @return the CompletableFuture for preparation (queue stream processing)
	 * @throws RejectedExecutionException if the initial preparation can't to be done.
	 */
	public <T, R> CompletableFuture<Void> asyncProcessing(Supplier<Stream<T>> items_to_process, FunctionWithException<T, R> processor, Consumer<Stream<SourcedCompletableFuture<T, R>>> allProcess) {
		return CompletableFuture.supplyAsync(() -> {
			allProcess.accept(items_to_process.get().map(item -> {
				return new SourcedCompletableFuture<T, R>(item, processor);
			}));
			return null;
		}, executor);
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
			return new IntermediateProcessor<T, R>(item, doProcess);
		}).map(c -> {
			return c.get(timeout_for_each_task, unit);
		});
	}
	
	/**
	 * Blocking !
	 * @param timeout_for_each_task can be null
	 * @return all results, never null.
	 */
	public <T, R> Stream<ProcessingResult<T, R>> multipleProcessing(Stream<T> items_to_process, FunctionWithException<T, R> processor) {
		return multipleProcessing(items_to_process, processor, 0, TimeUnit.SECONDS);
	}
	
	public void toTableList(TableList table) {
		table.addRow("Active", String.valueOf(executor.getActiveCount()));
		table.addRow("Max capacity", String.valueOf(executor.getQueue().remainingCapacity()));
		table.addRow("Completed", String.valueOf(executor.getCompletedTaskCount()));
		table.addRow("Core pool", String.valueOf(executor.getCorePoolSize()));
		table.addRow("Pool", String.valueOf(executor.getPoolSize()));
		table.addRow("Largest pool", String.valueOf(executor.getLargestPoolSize()));
		table.addRow("Maximum pool", String.valueOf(executor.getMaximumPoolSize()));
	}
	
	public JsonObject actualStatustoJson() {
		JsonObject jo_executor_pool = new JsonObject();
		jo_executor_pool.addProperty("active", String.valueOf(executor.getActiveCount()));
		jo_executor_pool.addProperty("shutdown", executor.isShutdown());
		jo_executor_pool.addProperty("terminating", executor.isTerminating());
		jo_executor_pool.addProperty("terminated", executor.isTerminated());
		jo_executor_pool.addProperty("max_capacity", String.valueOf(executor.getQueue().remainingCapacity()));
		jo_executor_pool.addProperty("completed", String.valueOf(executor.getCompletedTaskCount()));
		jo_executor_pool.addProperty("core_pool", String.valueOf(executor.getCorePoolSize()));
		jo_executor_pool.addProperty("pool", String.valueOf(executor.getPoolSize()));
		jo_executor_pool.addProperty("largest_pool", String.valueOf(executor.getLargestPoolSize()));
		jo_executor_pool.addProperty("maximum_pool", String.valueOf(executor.getMaximumPoolSize()));
		return jo_executor_pool;
	}
	
}
