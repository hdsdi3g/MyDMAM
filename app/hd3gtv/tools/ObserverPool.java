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
 * Copyright (C) hdsdi3g for hd3g.tv 10 sept. 2017
 * 
*/
package hd3gtv.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import hd3gtv.tools.ThreadPoolExecutorFactory.SourcedCompletableFuture;

/**
 * @param Distribute <T> produced by SourceProducer<T> to Observable<T>
 */
public class ObserverPool<T, S extends SourceProducer<T>> {
	
	private static Logger log = Logger.getLogger(ObserverPool.class);
	
	private ThreadPoolExecutorFactory executor;
	private ArrayList<Observable<T, S>> observers;
	
	public ObserverPool(ThreadPoolExecutorFactory executor) {
		this.executor = executor;
		if (executor == null) {
			throw new NullPointerException("\"executor\" can't to be null");
		}
		observers = new ArrayList<>();
	}
	
	public void addObserver(Observable<T, S> observer) {
		synchronized (observers) {
			observers.add(observer);
		}
	}
	
	public void removeObserver(Observable<T, S> observer) {
		synchronized (observers) {
			observers.remove(observer);
		}
	}
	
	/**
	 * Blocking, but use parallel callback.
	 * @return this
	 */
	public ObserverPool<T, S> distributeSync(S source, T item) {
		long count = executor.multipleProcessing(observers.stream(), observer -> {
			observer.onChange(source, item);
			return null;
		}).count();
		
		source.onAfterDistribute(item);
		
		if (log.isTraceEnabled()) {
			log.trace("Update " + count + "/" + observers.size() + " observer(s) with " + item.toString() + " from " + source.toString());
		}
		
		return this;
	}
	
	public ObserverPool<T, S> distributeAsync(S source, Stream<T> items) {
		Supplier<Stream<T>> items_to_process = () -> {
			return items;
		};
		
		FunctionWithException<T, Void> processor = item -> {
			distributeSync(source, item);
			return null;
		};
		
		Consumer<Stream<SourcedCompletableFuture<T, Void>>> allProcess = result_stream -> {
			List<SourcedCompletableFuture<T, Void>> pending_list = result_stream.peek(sc_future -> {
				if (log.isDebugEnabled()) {
					log.debug("Queued distribution for " + sc_future.processed_item);
				}
			}).collect(Collectors.toList());
			
			if (log.isTraceEnabled()) {
				log.debug("All " + pending_list.size() + " pending item(s) are in the process queue");
			}
			
			while (pending_list.stream().allMatch(sc_future -> {
				return sc_future.c_future.isDone();
			}) == false) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					return;
				}
			}
			
			if (log.isDebugEnabled()) {
				List<SourcedCompletableFuture<T, Void>> done_list = pending_list.stream().filter(sc_future -> {
					return sc_future.c_future.isCancelled() == false & sc_future.c_future.isCompletedExceptionally() == false;
				}).collect(Collectors.toList());
				
				if (done_list.isEmpty() == false) {
					if (log.isTraceEnabled()) {
						String s_list = done_list.stream().map(sc_future -> {
							return sc_future.processed_item.toString();
						}).collect(Collectors.joining(", "));
						
						log.trace("Distribution is done for " + done_list.size() + " items: " + s_list);
					} else {
						log.debug("Distribution is done for " + done_list.size() + " items");
					}
				}
			}
			
			List<SourcedCompletableFuture<T, Void>> error_list = pending_list.stream().filter(sc_future -> {
				return sc_future.hasProcessingError();
			}).collect(Collectors.toList());
			
			if (error_list.isEmpty() == false) {
				if (log.isTraceEnabled()) {
					error_list.forEach(sc_future -> {
						log.error("TRACE Distribution error for \"" + sc_future.processed_item.toString() + "\"", sc_future.getProcessingError());
					});
				} else {
					log.error("Distribution is in error for " + error_list.size() + " item(s). First error: ", error_list.get(0).getProcessingError());
				}
			}
			
			pending_list.stream().filter(sc_future -> {
				if (sc_future.hasProcessingError()) {
					return true;
				}
				try {
					sc_future.c_future.get();
					return false;
				} catch (InterruptedException | ExecutionException e1) {
					return true;
				}
			}).forEach(sc_future -> {
				source.ifCantDistribute(sc_future.processed_item);
			});
		};
		
		executor.asyncProcessing(items_to_process, processor, allProcess).exceptionally(e -> {
			log.error("Can't distribute", e);
			return null;
		});
		return this;
	}
	
}
