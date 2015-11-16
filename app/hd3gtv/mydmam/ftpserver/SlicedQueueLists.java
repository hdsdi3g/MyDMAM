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
 * Copyright (C) hdsdi3g for hd3g.tv 2015
 * 
*/
package hd3gtv.mydmam.ftpserver;

import java.util.ArrayList;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;

public class SlicedQueueLists<T extends Delayed> {
	
	private volatile ArrayList<T> list;
	
	public SlicedQueueLists() {
		list = new ArrayList<T>();
	}
	
	/**
	 * High speed additing.
	 */
	public void push(T item) {
		list.add(item);
	}
	
	/**
	 * Move all actual internal list to a DelayQueue, for waiting <T> definited delay.
	 * Extract expired waiting items to valid_list for process it.
	 */
	public void pullNextSlice(DelayQueue<T> dest, ArrayList<T> valid_list) {
		int actual_size = list.size();
		
		if (actual_size == 0) {
			return;
		}
		
		for (int pos = actual_size - 1; pos > -1; pos--) {
			dest.add(list.get(pos));
			list.remove(pos);
		}
		
		valid_list.clear();
		T item = dest.poll();
		while (item != null) {
			valid_list.add(item);
			item = dest.poll();
		}
	}
	
}
