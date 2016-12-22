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
 * Copyright (C) hdsdi3g for hd3g.tv 13 nov. 2016
 * 
*/
package hd3gtv.internaltaskqueue;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import hd3gtv.tools.StoppableProcessing;
import hd3gtv.tools.StoppableThread;

public class ITQueue {
	
	private LinkedList<ParameterizedTask<?, ?>> pending_tasks;
	private ArrayList<Executor> executors;
	private int executor_count;
	private StoppableProcessing external_stoppable;
	
	public ITQueue(int executor_count) {
		pending_tasks = new LinkedList<>();
		executors = new ArrayList<>();
		this.executor_count = executor_count;
		startAll();
	}
	
	public void setExternalStoppable(StoppableProcessing external_stoppable) {
		this.external_stoppable = external_stoppable;
	}
	
	private boolean isExternalWantStop() {
		if (external_stoppable == null) {
			return false;
		}
		return external_stoppable.isWantToStopCurrentProcessing();
	}
	
	public <T, O> void addToQueue(T item, Class<O> result, ParametedWithResultProcedure<T, O> process, BiConsumer<T, O> onSuccess, BiConsumer<T, Exception> onError) {
		if (process == null) {
			return;
		}
		synchronized (pending_tasks) {
			pending_tasks.add(new ParameterizedTask<T, O>(item, process, onSuccess, onError));
		}
	}
	
	/**
	 * @param result_collector Catch all results and add to it. It will be synchronized during the add. Only add if not null.
	 */
	public <T, O> void addToQueue(T item, ParametedWithResultProcedure<T, O> process, ArrayList<O> result_collector, BiConsumer<T, Exception> onError) {
		if (process == null) {
			return;
		}
		if (result_collector == null) {
			throw new NullPointerException("\"result_collector\" can't to be null");
		}
		synchronized (pending_tasks) {
			pending_tasks.add(new ParameterizedTask<T, O>(item, process, (t, o) -> {
				synchronized (result_collector) {
					if (o != null) {
						result_collector.add(o);
					}
				}
			}, onError));
		}
	}
	
	public <T, O> void addToQueue(T item, Class<O> result, ParametedWithResultProcedure<T, O> process, BiConsumer<T, Exception> onError) {
		addToQueue(item, result, process, null, onError);
	}
	
	public <T> void addToQueue(T item, ParametedProcedure<T> process, Consumer<T> onSuccess, BiConsumer<T, Exception> onError) {
		addToQueue(item, Void.class, t -> {
			process.process(t);
			return null;
		}, (t, o) -> {
			if (onSuccess != null) {
				onSuccess.accept(t);
			}
		}, onError);
	}
	
	public <T> void addToQueue(T item, ParametedProcedure<T> process, BiConsumer<T, Exception> onError) {
		addToQueue(item, process, null, onError);
	}
	
	public void addToQueue(Procedure process, Procedure onSuccess, Consumer<Exception> onError) {
		addToQueue(null, Void.class, t -> {
			process.process();
			return null;
		}, (t, o) -> {
			if (onSuccess != null) {
				try {
					onSuccess.process();
				} catch (Exception e1) {
					if (onError != null) {
						onError.accept(e1);
					}
				}
			}
		}, (t, e) -> {
			if (onError != null) {
				onError.accept(e);
			}
		});
	}
	
	public void addToQueue(Procedure process, Consumer<Exception> onError) {
		addToQueue(process, null, onError);
	}
	
	private class ParameterizedTask<T, O> {
		T item;
		O result;
		ParametedWithResultProcedure<T, O> process;
		BiConsumer<T, O> onSuccess;
		BiConsumer<T, Exception> onError;
		
		private ParameterizedTask(T item, ParametedWithResultProcedure<T, O> process, BiConsumer<T, O> onSuccess, BiConsumer<T, Exception> onError) {
			this.item = item;
			this.process = process;
			this.onSuccess = onSuccess;
			this.onError = onError;
		}
		
		private void process() throws Exception {
			result = process.process(item);
		}
		
		private void onError(Exception e) {
			if (onError != null) {
				onError.accept(item, e);
			}
		}
		
		private void onDone() {
			if (onSuccess != null) {
				onSuccess.accept(item, result);
			}
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			
			if (item != null) {
				sb.append("Item " + item.getClass() + " [" + item.toString() + "] ");
			}
			if (process != null) {
				sb.append("Process " + process.getClass() + " ");
			}
			if (result != null) {
				sb.append("Result " + result.getClass() + " [" + result.toString() + "]");
			}
			
			return sb.toString();
		}
	}
	
	public void waitToStopAll() {
		executors.forEach(ex -> {
			ex.waitToStop();
		});
	}
	
	public void waitToEndCurrentList() {
		while (pending_tasks.isEmpty() == false) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
			}
		}
		executors.forEach(ex -> {
			if (ex.isAlive()) {
				try {
					Thread.sleep(1);
				} catch (Exception e) {
				}
			}
		});
	}
	
	public void emptyTheCurrentWaitingList() {
		synchronized (pending_tasks) {
			pending_tasks.clear();
		}
	}
	
	public void startAll() {
		executors.forEach(ex -> {
			if (ex.isAlive()) {
				ex.waitToStop();
			}
		});
		
		executors.clear();
		
		for (int pos = 0; pos < executor_count; pos++) {
			executors.add(new Executor("QueueExecutor-" + pos));
		}
		
		executors.forEach(ex -> {
			ex.start();
		});
	}
	
	private class Executor extends StoppableThread {
		
		public Executor(String name) {
			super(name);
		}
		
		public boolean isWantToRun() {
			return super.isWantToRun() && (isExternalWantStop() == false);
		}
		
		public boolean isWantToStop() {
			return super.isWantToStop() | isExternalWantStop();
		}
		
		private volatile ParameterizedTask<?, ?> selected_task;
		
		public void run() {
			while (isWantToRun()) {
				if (pending_tasks.isEmpty() == false) {
					synchronized (pending_tasks) {
						try {
							selected_task = pending_tasks.removeFirst();
						} catch (NoSuchElementException nsee) {
							stoppableSleep(10);
							continue;
						}
					}
					if (selected_task == null) {
						stoppableSleep(10);
						continue;
					}
					
					try {
						selected_task.process();
					} catch (Exception e) {
						selected_task.onError(e);
					}
					if (isWantToRun()) {
						selected_task.onDone();
					}
					selected_task = null;
				}
				stoppableSleep(10);
			}
		}
		
	}
	
}
