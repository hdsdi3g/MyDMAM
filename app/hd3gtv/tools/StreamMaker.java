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
 * Copyright (C) hdsdi3g for hd3g.tv 20 sept. 2017
 * 
*/
package hd3gtv.tools;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class StreamMaker<T> {
	
	private Internal iterator;
	private Consumer<Throwable> onError;
	private Supplier<T> getNext;
	
	private StreamMaker() {
		iterator = new Internal();
	}
	
	/**
	 * @param getNext return null for end stream
	 */
	public StreamMaker(Supplier<T> getNext, Consumer<Throwable> onError) {
		this();
		this.getNext = getNext;
		if (getNext == null) {
			throw new NullPointerException("\"getNext\" can't to be null");
		}
		this.onError = onError;
	}
	
	/**
	 * @param getNext return null for end stream
	 */
	public StreamMaker(Supplier<T> getNext) {
		this();
		this.getNext = getNext;
		if (getNext == null) {
			throw new NullPointerException("\"getNext\" can't to be null");
		}
		this.onError = null;
	}
	
	private class Internal implements Iterator<T> {
		
		T current_item;
		
		public boolean hasNext() {
			try {
				current_item = getNext.get();
			} catch (Throwable e) {
				if (onError != null) {
					current_item = null;
					onError.accept(e);
					return false;
				} else {
					throw new RuntimeException(e);
				}
			}
			
			return current_item != null;
		}
		
		public T next() {
			return current_item;
		}
		
	}
	
	public Stream<T> stream() {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.IMMUTABLE + Spliterator.DISTINCT + Spliterator.NONNULL), false);
	}
	
	public static <T> StreamMaker<T> create(Supplier<T> getNext, Consumer<Throwable> onError) {
		return new StreamMaker<>(getNext, onError);
	}
	
	public static <T> StreamMaker<T> create(Supplier<T> getNext) {
		return new StreamMaker<>(getNext);
	}
	
	/*public static <T> CompletableFuture<Stream<T>> createCompletableFuture(Supplier<T> getNext) {
		StreamMaker<T> sm = new StreamMaker<>(getNext);
		CompletableFuture<Stream<T>> c_future = new CompletableFuture<>();
		sm.onError = e -> {
			c_future.completeExceptionally(e);
		};
		c_future.complete(sm.stream());
		return c_future;
	}*/
	
	/**
	 * Let pass items until stop_trigger == true, then add the current element, and stop.
	 * @see https://stackoverflow.com/questions/20746429/limit-a-stream-by-a-predicate
	 */
	public static <T> Stream<T> takeUntilTrigger(Predicate<? super T> stop_trigger, Stream<T> stream) {
		Spliterator<T> splitr = stream.spliterator();
		return StreamSupport.stream(new Spliterators.AbstractSpliterator<T>(splitr.estimateSize(), 0) {
			boolean still_going = true;
			
			public boolean tryAdvance(Consumer<? super T> consumer) {
				if (still_going) {
					boolean had_next = splitr.tryAdvance(elem -> {
						if (stop_trigger.test(elem) == false) {
							consumer.accept(elem);
						} else {
							consumer.accept(elem);
							still_going = false;
						}
					});
					return had_next && still_going;
				}
				return false;
			}
		}, false);
	}
}
