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

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

import com.google.gson.JsonObject;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonIgnore;

@GsonIgnore
public class ThreadPoolExecutorFactory implements Executor {
	
	private static Logger log = Logger.getLogger(ThreadPoolExecutorFactory.class);
	private final PausableThreadPoolExecutor executor;
	
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
		executor = new PausableThreadPoolExecutor(MyDMAM.CPU_COUNT, MyDMAM.CPU_COUNT, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
		/*executor.setRejectedExecutionHandler((r, executor) -> {
			if (executor != null) {
				if (executor.isShutdown() | executor.isTerminated() | executor.isTerminating()) {
					log.error("Can't add newer task: executor for \"" + base_thread_name + "\" is closed/pending closing !");
					return;
				}
			}
			log.error("Too many task to be executed at the same time for \"" + base_thread_name + "\" ! This will not proceed: " + r);
		});*/
		executor.setThreadFactory(runnable -> {
			Thread t = new Thread(runnable);
			t.setDaemon(false);
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
	
	/*public ThreadPoolExecutor getThreadPoolExecutor() {
		return executor;
	}*/
	
	public AsynchronousChannelGroup createAsynchronousChannelGroup() throws IOException {
		return AsynchronousChannelGroup.withThreadPool(executor);
	}
	
	public boolean isRunning() {
		return executor.isShutdown() == false | executor.isTerminated() == false | executor.isTerminating() == false;
	}
	
	public int getTotalActive() {
		return executor.total_active.get();
	}
	
	public int getQueueSize() {
		return executor.getQueue().size();
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
	
	public void execute(Runnable r) {
		executor.execute(r);
	}
	
	public Future<?> submit(Runnable r) {
		return executor.submit(r);
	}
	
	public <T> Future<T> submit(Callable<T> task) {
		return executor.submit(task);
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
	
	/**
	 * Blocking.
	 * @param task will be run in this current Thread, not queued.
	 */
	public void insertPauseTask(Runnable task) {
		executor.pause_lock.lock();
		if (executor.is_paused) {
			executor.pause_lock.unlock();
			throw new IllegalMonitorStateException("Already locked");
		}
		
		executor.is_paused = true;
		
		try {
			while (getTotalActive() > 0) {
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
			task.run();
		} finally {
			try {
				executor.is_paused = false;
				executor.unpaused.signalAll();
			} finally {
				executor.pause_lock.unlock();
			}
		}
	}
	
	/**
	 * @see ThreadPoolExecutor (free Java inspiration)
	 */
	private class PausableThreadPoolExecutor extends ThreadPoolExecutor {
		
		private boolean is_paused;
		private ReentrantLock pause_lock = new ReentrantLock();
		private Condition unpaused = pause_lock.newCondition();
		private AtomicInteger total_active = new AtomicInteger(0);
		
		public PausableThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
			super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
		}
		
		protected void beforeExecute(Thread t, Runnable r) {
			super.beforeExecute(t, r);
			pause_lock.lock();
			try {
				while (is_paused) {
					unpaused.await();
				}
			} catch (InterruptedException ie) {
				t.interrupt();
			} finally {
				pause_lock.unlock();
			}
			total_active.incrementAndGet();
		}
		
		protected void afterExecute(Runnable r, Throwable t) {
			super.afterExecute(r, t);
			total_active.decrementAndGet();
		}
	}
	
	public void shutdownAndTerminate() {
		executor.shutdown();
		while (executor.isTerminated() == false) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		executor.total_active.set(0);
	}
	
}
