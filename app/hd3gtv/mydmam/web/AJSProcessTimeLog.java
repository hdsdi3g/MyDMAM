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
package hd3gtv.mydmam.web;

import java.util.LinkedList;
import java.util.Locale;
import java.util.LongSummaryStatistics;

import hd3gtv.tools.TableList;

public class AJSProcessTimeLog {
	
	private final LinkedList<Long> process_durations;
	private final int max_entry_count;
	
	private String slow_ressource_name;
	private Long slow_duration = 0l;
	
	AJSProcessTimeLog() {
		process_durations = new LinkedList<>();
		max_entry_count = 1000;
	}
	
	public void addEntry(long duration_time_ms, String ressource_name) {
		synchronized (process_durations) {
			if (process_durations.size() > max_entry_count) {
				process_durations.removeFirst();
			}
			process_durations.add(duration_time_ms);
		}
		
		synchronized (slow_duration) {
			if (duration_time_ms > slow_duration) {
				slow_duration = duration_time_ms;
				slow_ressource_name = ressource_name;
			}
		}
	}
	
	public synchronized void truncate() {
		process_durations.clear();
		slow_duration = 0l;
		slow_ressource_name = null;
	}
	
	public LongSummaryStatistics getStats() {
		return process_durations.stream().mapToLong(val -> {
			return val;
		}).summaryStatistics();
	}
	
	public String toString() {
		if (process_durations.isEmpty()) {
			return null;
		}
		
		TableList list = new TableList();
		
		LongSummaryStatistics stats = getStats();
		list.addRow("entries", stats.getCount(), 1, 0, "measured (" + max_entry_count + " max)", Locale.US);
		list.addRow("total time", stats.getSum(), 1000, 3, "sec", Locale.US);
		list.addRow("min", stats.getMin(), 1000, 3, "sec", Locale.US);
		list.addRow("average", stats.getAverage(), 1000, 3, "sec", Locale.US);
		list.addRow("max", stats.getMax(), 1000, 3, "sec", Locale.US);
		
		if (stats.getMax() != slow_duration) {
			System.err.println(slow_duration);
		}
		
		if (slow_ressource_name != null) {
			list.addRow("slower", slow_ressource_name);
		}
		
		return list.toString();
	}
	
}
