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
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
*/
package hd3gtv.mydmam.manager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import hd3gtv.mydmam.Loggers;

public class ClockProgrammedTasks implements InstanceStatusItem, InstanceActionReceiver {
	
	// TODO add to AppManager
	
	private static final int MAX_QUEUED_TASKS = 100;
	private ScheduledExecutorService scheduled_ex_service;
	private ThreadPoolExecutor executor_pool;
	private AppManager manager;
	private List<TaskWrapper> all_tasks;
	private BlockingQueue<Runnable> executor_pool_queue;
	
	ClockProgrammedTasks(AppManager manager) {
		this.manager = manager;
		all_tasks = Collections.synchronizedList(new ArrayList<>());
		
		scheduled_ex_service = Executors.newSingleThreadScheduledExecutor();
		executor_pool_queue = new LinkedBlockingQueue<Runnable>(MAX_QUEUED_TASKS);
		initThreadPoolExecutor();
		
		manager.registerInstanceStatusAction(this);
	}
	
	private void initThreadPoolExecutor() {
		executor_pool_queue.clear();
		executor_pool = new ThreadPoolExecutor(1, Runtime.getRuntime().availableProcessors(), MAX_QUEUED_TASKS, TimeUnit.MILLISECONDS, executor_pool_queue);
		executor_pool.setRejectedExecutionHandler((r, executor) -> {
			Loggers.Manager.warn("Too many task to be executed on the ClockProgrammedTasks at the same time ! This will not proceed: " + r);
		});
	}
	
	public String getReferenceKey() {
		return "ClPrTs:" + manager.getReferenceKey();
	}
	
	/**
	 * @param blocking wait some time to close current tasks
	 * @param force don't let the time to close current tasks
	 */
	void cancelAllProgrammed(long timeout, TimeUnit unit) {
		all_tasks.forEach(task -> {
			task.stopNextScheduling();
		});
		
		if (executor_pool.isShutdown()) {
			return;
		}
		
		executor_pool.shutdown();
		try {
			executor_pool.awaitTermination(timeout, unit);
		} catch (InterruptedException e) {
			Loggers.Manager.error("Can't stop executor", e);
			executor_pool.shutdownNow();
		}
	}
	
	void startAllProgrammed() {
		initThreadPoolExecutor();
		
		all_tasks.forEach(task -> {
			task.stopNextScheduling();
			task.setNextScheduling(false);
		});
	}
	
	public JsonElement getInstanceStatusItem() {// TODO publish to website
		// TODO Auto-generated method stub
		return null;
	}
	
	public Class<?> getInstanceStatusItemReferenceClass() {
		return ClockProgrammedTasks.class;
	}
	
	public void doAnAction(JsonObject order) throws Exception {// TODO publish to website
		if (order.has("disable")) {
			cancelAllProgrammed(5, TimeUnit.SECONDS);
		} else if (order.has("enable")) {
			startAllProgrammed();
		}
	}
	
	public Class<? extends InstanceActionReceiver> getClassToCallback() {
		return ClockProgrammedTasks.class;
	}
	
	public TaskWrapper createTask(String name, long start_time_after_midnight, TimeUnit unit, Runnable task) {
		return new TaskWrapper(name, start_time_after_midnight, unit, task);
	}
	
	public class TaskWrapper {
		private String name;
		private long start_time_after_midnight;
		private Logger logger;
		private Runnable task;
		private long retry_after;
		private boolean unschedule_if_error;
		private boolean log_regular_in_debug;
		
		private volatile ScheduledFuture<?> next_scheduled;
		
		private TaskWrapper(String name, long start_time_after_midnight, TimeUnit unit, Runnable task) {
			this.name = name;
			if (name == null) {
				throw new NullPointerException("\"name\" can't to be null");
			}
			if (unit == null) {
				throw new NullPointerException("\"unit\" can't to be null");
			}
			this.start_time_after_midnight = unit.toMillis(start_time_after_midnight);
			
			this.task = task;
			if (task == null) {
				throw new NullPointerException("\"task\" can't to be null");
			}
			logger = Loggers.Manager;
		}
		
		public TaskWrapper setLogger(Logger logger, boolean log_regular_in_debug) {
			this.logger = logger;
			this.log_regular_in_debug = log_regular_in_debug;
			return this;
		}
		
		public TaskWrapper retryAfter(long retry_after, TimeUnit unit) {
			this.retry_after = unit.toMillis(retry_after);
			return this;
		}
		
		public TaskWrapper setUnscheduleIfError(boolean remove_task_in_case_of_error) {
			this.unschedule_if_error = remove_task_in_case_of_error;
			return this;
		}
		
		private long getNextSendTime() {
			/**
			 * Local time
			 */
			Calendar c = Calendar.getInstance();
			c.set(Calendar.HOUR_OF_DAY, 0);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 0);
			c.set(Calendar.MILLISECOND, 0);
			c.setTimeInMillis(c.getTimeInMillis() + start_time_after_midnight);
			if (c.getTimeInMillis() < System.currentTimeMillis()) {
				c.setTimeInMillis(c.getTimeInMillis() + TimeUnit.DAYS.toMillis(1));
			}
			return c.getTimeInMillis();
		}
		
		private void executeAndSetNext() {
			try {
				if (log_regular_in_debug) {
					logger.debug("Start scheduled task \"" + name + "\"");
				} else {
					logger.info("Start scheduled task \"" + name + "\"");
				}
				task.run();
				setNextScheduling(false);
			} catch (Exception e) {
				if (unschedule_if_error) {
					logger.error("Schedule task \"" + name + "\" cause exception. Next executions will be canceled", e);
				} else {
					logger.warn("Schedule task \"" + name + "\" cause exception. Retry in " + retry_after / 1000 + " sec", e);
					setNextScheduling(true);
				}
			}
		}
		
		private void setNextScheduling(boolean add_retry_after) {
			if (executor_pool.isShutdown()) {
				return;
			}
			
			long time_to_wait = System.currentTimeMillis() - getNextSendTime();
			
			if (add_retry_after) {
				time_to_wait = retry_after;
			}
			
			next_scheduled = scheduled_ex_service.schedule(() -> {
				if (executor_pool.isShutdown()) {
					return;
				}
				executor_pool.execute(() -> {
					executeAndSetNext();
				});
			}, time_to_wait, TimeUnit.MILLISECONDS);
		}
		
		private void stopNextScheduling() {
			if (next_scheduled == null) {
				return;
			} else if (next_scheduled.isCancelled() | next_scheduled.isDone()) {
				return;
			}
			logger.info("Remove scheduled task \"" + name + "\"");
			next_scheduled.cancel(false);
		}
		
		public void schedule() {
			setNextScheduling(false);
			all_tasks.add(this);
			logger.info("Schedule task \"" + name + "\" every days, the next execution will be the " + Loggers.dateLog(getNextSendTime()));
		}
		
	}
	
}
