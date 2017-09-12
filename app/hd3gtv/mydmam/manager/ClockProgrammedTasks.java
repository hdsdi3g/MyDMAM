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
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import hd3gtv.mydmam.Loggers;
import hd3gtv.tools.ThreadPoolExecutorFactory;

/**
 * Execute tasks daily.
 */
public class ClockProgrammedTasks implements InstanceStatusItem, InstanceActionReceiver {
	
	private static final int MAX_QUEUED_TASKS = 100;
	private ScheduledExecutorService scheduled_ex_service;
	private ThreadPoolExecutorFactory executor;
	private AppManager manager;
	private List<TaskWrapper> all_tasks;
	
	ClockProgrammedTasks(AppManager manager) {
		this.manager = manager;
		all_tasks = Collections.synchronizedList(new ArrayList<>());
		
		scheduled_ex_service = Executors.newSingleThreadScheduledExecutor();
		initThreadPoolExecutor();
		
		manager.registerInstanceStatusAction(this);
	}
	
	private void initThreadPoolExecutor() {
		executor = new ThreadPoolExecutorFactory("ClockProgrammedTask", MAX_QUEUED_TASKS);
	}
	
	public String getReferenceKey() {
		return "clkprgmtsk:" + manager.getReferenceKey();
	}
	
	/**
	 * @param blocking wait some time to close current tasks
	 * @param force don't let the time to close current tasks
	 */
	void cancelAllProgrammed(long timeout, TimeUnit unit) {
		Loggers.ClkPrgmTsk.info("Stop regular scheduling for " + all_tasks.size() + " task(s)");
		
		all_tasks.forEach(task -> {
			task.stopNextScheduling();
		});
		
		executor.awaitTerminationAndShutdown(timeout, unit);
	}
	
	boolean isActive() {
		return executor.isRunning();
	}
	
	void startAllProgrammed() {
		Loggers.ClkPrgmTsk.info("Start regular scheduling for " + all_tasks.size() + "task(s)");
		
		initThreadPoolExecutor();
		
		all_tasks.forEach(task -> {
			task.stopNextScheduling();
			task.setNextScheduling(false);
		});
	}
	
	public JsonElement getInstanceStatusItem() {
		JsonObject result = new JsonObject();
		
		JsonObject jo_tasks = new JsonObject();
		all_tasks.forEach(task -> {
			jo_tasks.add(task.key, task.toJson());
		});
		result.add("executor", executor.actualStatustoJson());
		result.add("tasks", jo_tasks);
		return result;
	}
	
	public Class<?> getInstanceStatusItemReferenceClass() {
		return ClockProgrammedTasks.class;
	}
	
	public void doAnAction(JsonObject order) throws Exception {
		if (order.has("executor")) {
			String s_order = order.get("executor").getAsString();
			if (s_order.equalsIgnoreCase("enable")) {
				startAllProgrammed();
			} else if (s_order.equalsIgnoreCase("disable")) {
				cancelAllProgrammed(5, TimeUnit.SECONDS);
			}
		} else if (order.has("task") && order.has("action")) {
			String task_key = order.get("task").getAsString();
			String action = order.get("action").getAsString();
			
			all_tasks.stream().filter(task -> {
				return task.key.equalsIgnoreCase(task_key);
			}).findFirst().orElseThrow(() -> {
				return new NullPointerException("Can't found task: " + task_key);
			}).doAnAction(action);
		}
	}
	
	public Class<? extends InstanceActionReceiver> getClassToCallback() {
		return ClockProgrammedTasks.class;
	}
	
	public TaskWrapper createTask(String name, long start_time_after_midnight, TimeUnit unit, ClockProgrammedRunnable task) {
		return new TaskWrapper(name, start_time_after_midnight, unit, task);
	}
	
	public class TaskWrapper {
		private String key;
		private String name;
		private long start_time_after_midnight;
		private ClockProgrammedRunnable task;
		private long retry_after;
		private boolean unschedule_if_error;
		private volatile ScheduledFuture<?> next_scheduled;
		private JsonObject last_status;
		private volatile long last_execute_date;
		private volatile long last_execute_duration;
		
		private TaskWrapper(String name, long start_time_after_midnight, TimeUnit unit, ClockProgrammedRunnable task) {
			key = UUID.randomUUID().toString();
			
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
			last_execute_date = -1;
			last_execute_duration = -1;
			
			last_status = new JsonObject();
			last_status.addProperty("key", key);
			last_status.addProperty("name", name);
			last_status.addProperty("start_time_after_midnight", start_time_after_midnight);
			last_status.addProperty("task_class", task.getClass().getName());
			last_status.addProperty("retry_after", -1);
			last_status.addProperty("unschedule_if_error", false);
			last_status.addProperty("last_execute_date", last_execute_date);
			last_status.addProperty("last_execute_duration", last_execute_duration);
			if (next_scheduled == null) {
				last_status.addProperty("scheduled", false);
			} else {
				last_status.addProperty("scheduled", next_scheduled.isCancelled() == false && next_scheduled.isDone() == false);
			}
		}
		
		private JsonObject toJson() {
			if (next_scheduled != null) {
				last_status.addProperty("next_scheduled", System.currentTimeMillis() + next_scheduled.getDelay(TimeUnit.MILLISECONDS));
			} else {
				last_status.addProperty("next_scheduled", -1);
			}
			last_status.addProperty("last_execute_date", last_execute_date);
			last_status.addProperty("last_execute_duration", last_execute_duration);
			last_status.addProperty("unschedule_if_error", unschedule_if_error);
			
			if (next_scheduled == null) {
				last_status.addProperty("scheduled", false);
			} else {
				last_status.addProperty("scheduled", next_scheduled.isCancelled() == false && next_scheduled.isDone() == false);
			}
			return last_status;
		}
		
		public TaskWrapper retryAfter(long retry_after, TimeUnit unit) {
			this.retry_after = unit.toMillis(retry_after);
			last_status.addProperty("retry_after", retry_after);
			return this;
		}
		
		public TaskWrapper setUnscheduleIfError(boolean remove_task_in_case_of_error) {
			this.unschedule_if_error = remove_task_in_case_of_error;
			last_status.addProperty("unschedule_if_error", unschedule_if_error);
			return this;
		}
		
		private void doAnAction(String order) throws Exception {
			if (order.equalsIgnoreCase("start_now")) {
				if (executor.isRunning() == false) {
					return;
				}
				Loggers.ClkPrgmTsk.info("Manual start task \"" + name + "\" via InstanceAction");
				
				executor.execute(() -> {
					executeAndSetNext();
				});
			} else if (order.equalsIgnoreCase("unschedule")) {
				Loggers.ClkPrgmTsk.info("Manual stop scheduling for task \"" + name + "\" via InstanceAction");
				stopNextScheduling();
			} else if (order.equalsIgnoreCase("schedule")) {
				Loggers.ClkPrgmTsk.info("Manual start scheduling for task \"" + name + "\" via InstanceAction");
				setNextScheduling(false);
			} else if (order.equalsIgnoreCase("toggle_unschedule_if_error")) {
				unschedule_if_error = !unschedule_if_error;
				Loggers.ClkPrgmTsk.info("Set task unschedule_if_error status for task \"" + name + "\" to " + unschedule_if_error);
			}
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
			last_execute_date = System.currentTimeMillis();
			try {
				Loggers.ClkPrgmTsk.info("Start scheduled task \"" + name + "\"");
				task.run();
				Loggers.ClkPrgmTsk.debug("Scheduled task \"" + name + "\" was correcly executed");
				setNextScheduling(false);
			} catch (Exception e) {
				if (unschedule_if_error) {
					Loggers.ClkPrgmTsk.error("Schedule task \"" + name + "\" cause exception. Next executions will be canceled", e);
				} else {
					if (retry_after > 0) {
						Loggers.ClkPrgmTsk.warn("Schedule task \"" + name + "\" cause exception. Retry in " + retry_after / 1000 + " sec", e);
						setNextScheduling(true);
					} else {
						Loggers.ClkPrgmTsk.warn("Schedule task \"" + name + "\" cause exception, retry tomorrow", e);
					}
				}
			}
			last_execute_duration = System.currentTimeMillis() - last_execute_date;
		}
		
		private void setNextScheduling(boolean add_retry_after) {
			if (executor.isRunning() == false) {
				return;
			}
			
			long time_to_wait = getNextSendTime() - System.currentTimeMillis();
			
			if (add_retry_after) {
				time_to_wait = retry_after;
			}
			
			Loggers.ClkPrgmTsk.debug("Create the next scheduling for \"" + name + "\" in " + (time_to_wait / 1000l) + " seconds");
			
			next_scheduled = scheduled_ex_service.schedule(() -> {
				if (executor.isRunning() == false) {
					return;
				}
				Loggers.ClkPrgmTsk.debug("The time to wait is done for \"" + name + "\", it's start to queue the task in executor.");
				
				executor.execute(() -> {
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
			Loggers.ClkPrgmTsk.info("Remove scheduled task \"" + name + "\"");
			next_scheduled.cancel(false);
		}
		
		public void schedule() {
			setNextScheduling(false);
			all_tasks.add(this);
			Loggers.ClkPrgmTsk.info("Schedule task \"" + name + "\" every days, the next execution will be the " + Loggers.dateLog(getNextSendTime()));
		}
		
	}
	
}
