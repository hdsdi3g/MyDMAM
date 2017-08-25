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
 * Copyright (C) hdsdi3g for hd3g.tv 22 janv. 2017
 * 
*/
package hd3gtv.tools;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import hd3gtv.tools.TableList;

public class PressureMeasurement {
	private long start_time;
	private AtomicLong current_transfered_datas;
	private AtomicLong current_transfered_blocks;
	private AtomicLong current_transfered_time;
	private AtomicLong current_transfered_time_max;
	private AtomicLong current_transfered_time_min;
	
	public PressureMeasurement() {
		start_time = System.currentTimeMillis();
		current_transfered_datas = new AtomicLong();
		current_transfered_blocks = new AtomicLong();
		current_transfered_time = new AtomicLong();
		current_transfered_time_max = new AtomicLong();
		current_transfered_time_min = new AtomicLong(Long.MAX_VALUE);
	}
	
	public void onDatas(long size, long duration) {
		current_transfered_blocks.incrementAndGet();
		current_transfered_datas.addAndGet(size);
		current_transfered_time.addAndGet(duration);
		current_transfered_time_max.getAndAccumulate(duration, (before, after) -> {
			if (after > before) {
				return after;
			} else {
				return before;
			}
		});
		current_transfered_time_min.getAndAccumulate(duration, (before, after) -> {
			if (after < before) {
				return after;
			} else {
				return before;
			}
		});
	}
	
	public void onProcess(long duration) {
		onDatas(0, duration);
	}
	
	public CollectedData getActualStats(boolean reset_after) {
		CollectedData r = new CollectedData();
		if (reset_after) {
			reset();
		}
		return r;
	}
	
	private void reset() {
		current_transfered_datas.set(0);
		current_transfered_blocks.set(0);
		current_transfered_time.set(0);
		current_transfered_time_max.set(0);
		current_transfered_time_min.set(0);
		start_time = System.currentTimeMillis();
	}
	
	/**
	 * @param table size 10
	 */
	public static void toTableHeader(TableList table) {
		table.addRow("Name", "Last", "Transf", "Hits", "During", "Max", "Min", "Mean", "Speed", "Speed");
		
	}
	
	public class CollectedData {
		
		/**
		 * In sec
		 */
		private double duration;
		
		/**
		 * Bytes
		 */
		private double last_transfered_datas;
		
		/**
		 * Incr
		 */
		private double last_transfered_blocks;
		
		/**
		 * Sec
		 */
		private double last_transfered_time;
		
		/**
		 * Sec
		 */
		private double last_transfered_time_max;
		
		/**
		 * Sec
		 */
		private double last_transfered_time_min;
		
		private String time;
		private String transfered_datas;
		private String transfered_blocks;
		private String transfered_time;
		private String transfered_time_max;
		private String transfered_time_min;
		private String transfered_time_mean;
		private String transfered_datas_speed;
		private String transfered_datas_blocks_speed;
		
		private CollectedData() {
			duration = (System.currentTimeMillis() - start_time) / 1000d;
			last_transfered_datas = current_transfered_datas.get();
			last_transfered_blocks = current_transfered_blocks.get();
			last_transfered_time = current_transfered_time.get() / 1000d;
			last_transfered_time_max = current_transfered_time_max.get() / 1000d;
			last_transfered_time_min = current_transfered_time_min.get() / 1000d;
			
			if (last_transfered_blocks == 0) {
				return;
			}
			
			DecimalFormat simple = (DecimalFormat) NumberFormat.getNumberInstance(Locale.getDefault());
			simple.applyPattern("###,###,###.#");
			DecimalFormat high = (DecimalFormat) NumberFormat.getNumberInstance(Locale.getDefault());
			high.applyPattern("###,###,###.###");
			
			time = simple.format(duration) + " sec";
			
			transfered_datas = simple.format(last_transfered_datas / 1024d) + " kb";
			if (last_transfered_datas == 0) {
				transfered_datas = "";
			}
			
			transfered_blocks = simple.format(last_transfered_blocks) + " hits";
			transfered_time = high.format(last_transfered_time) + " sec";
			transfered_time_max = high.format(last_transfered_time_max) + " sec";
			transfered_time_min = high.format(last_transfered_time_min) + " sec";
			
			transfered_time_mean = high.format(last_transfered_time / last_transfered_blocks) + " sec";
			
			transfered_datas_speed = high.format((last_transfered_datas / duration) / 1024d) + " kb/sec";
			if (last_transfered_datas == 0) {
				transfered_datas_speed = "";
			}
			
			transfered_datas_blocks_speed = high.format((last_transfered_blocks / duration)) + " hit/sec";
		}
		
		/**
		 * @param table size 10
		 */
		public void toTable(TableList table, String prefix) {
			if (last_transfered_blocks == 0) {
				return;
			}
			table.addRow(prefix, time, transfered_datas, transfered_blocks, transfered_time, transfered_time_max, transfered_time_min, transfered_time_mean, transfered_datas_speed,
					transfered_datas_blocks_speed);
		}
		
		public String toString() {
			if (last_transfered_blocks == 0) {
				return "(nodatas)";
			}
			
			StringBuilder sb = new StringBuilder();
			sb.append("Last ");
			sb.append(time);
			if (last_transfered_datas > 0) {
				sb.append(": ");
				sb.append(transfered_datas);
			}
			sb.append(" in ");
			sb.append(transfered_blocks);
			sb.append(" during ");
			sb.append(transfered_time);
			sb.append(" (max: ");
			sb.append(transfered_time_max);
			sb.append(", min: ");
			sb.append(transfered_time_min);
			sb.append(", mean: ");
			sb.append(transfered_time_mean);
			sb.append("), speed: ");
			if (last_transfered_datas > 0) {
				sb.append(transfered_datas_speed);
				sb.append(" / ");
			}
			sb.append(transfered_datas_blocks_speed);
			return sb.toString();
		}
		
	}
	
}
