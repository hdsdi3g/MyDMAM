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
package hd3gtv.mydmam.manager;

import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

public class StatisticsTime {
	
	private static final int MAX_ITEMS = 100;
	
	private class MeterEntry {
		long start_date;
		
		/**
		 * In ms
		 */
		long duration;
	}
	
	private volatile ArrayList<MeterEntry> entries;
	private MeterEntry current;
	
	public StatisticsTime() {
		entries = new ArrayList<StatisticsTime.MeterEntry>();
	}
	
	public void startMeasure() {
		current = new MeterEntry();
		current.start_date = System.currentTimeMillis();
	}
	
	public void endMeasure() {
		if (current == null) {
			return;
		}
		current.duration = System.currentTimeMillis() - current.start_date;
		entries.add(current);
		pack();
		current = null;
	}
	
	public void addMeasure(long start_date, long duration, TimeUnit unit) {
		MeterEntry item = new MeterEntry();
		item.start_date = start_date;
		item.duration = unit.toMillis(duration);
		entries.add(item);
		pack();
	}
	
	private void pack() {
		if (entries.size() < MAX_ITEMS) {
			return;
		}
		entries.remove(0);
	}
	
	/**
	 * @return null if not entries.
	 */
	public StatisticTimeResult getStatisticTimeResult() {
		if (entries.isEmpty()) {
			return null;
		}
		ArrayList<Long> al_values = new ArrayList<Long>(entries.size());
		for (int pos = 0; pos < entries.size(); pos++) {
			al_values.add(entries.get(pos).duration);
		}
		return new StatisticTimeResult(al_values);
	}
	
	public class StatisticTimeResult implements Log2Dumpable {
		private long max = Long.MIN_VALUE;
		private long min = Long.MAX_VALUE;
		private long median = 0;
		private long mean = 0;
		private long stddev = 0;
		
		private StatisticTimeResult(ArrayList<Long> al_values) {
			if (al_values.size() < 2) {
				return;
			}
			Collections.sort(al_values, new Comparator<Long>() {
				public int compare(Long o1, Long o2) {
					if (o1 == null) {
						return -1;
					}
					if (o2 == null) {
						return 1;
					}
					if (o1 == o2) {
						return 0;
					} else if (o1 < o2) {
						return -1;
					} else {
						return 1;
					}
				}
			});
			
			min = al_values.get(0);
			max = al_values.get(al_values.size() - 1);
			median = al_values.get(al_values.size() / 2);
			
			for (int pos = 0; pos < al_values.size(); pos++) {
				mean += al_values.get(pos);
			}
			mean = Math.round(mean / al_values.size());
			
			for (int pos = 0; pos < al_values.size(); pos++) {
				stddev += Math.pow(al_values.get(pos) - mean, 2);
			}
			if (al_values.size() > 1) {
				stddev = Math.round(Math.sqrt(stddev / (al_values.size() - 1)));
			}
		}
		
		public Log2Dump getLog2Dump() {
			Log2Dump dump = new Log2Dump();
			dump.add("stat min", (double) min / 1000d);
			dump.add("stat mean", (double) mean / 1000d);
			dump.add("stat stddev", (double) stddev / 1000d);
			dump.add("stat median", (double) median / 1000d);
			dump.add("stat max", (double) max / 1000d);
			return dump;
		}
	}
	
}
