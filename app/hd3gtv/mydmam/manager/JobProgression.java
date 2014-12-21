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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.mydmam.manager;

public final class JobProgression {
	
	private transient JobNG job;
	private int progress = 0;
	private int progress_size = 0;
	private int step = 0;
	private int step_count = 0;
	private String last_message;
	private String last_caller;
	
	JobProgression(JobNG job) {
		this.job = job;
	}
	
	private void updateLastCaller() {
		StackTraceElement[] stack = new Throwable().getStackTrace();
		if (stack.length > 2) {
			last_caller = stack[2].toString();
		} else {
			last_caller = "<null>";
		}
	}
	
	/**
	 * Async update.
	 * @param last_message can be null.
	 */
	public synchronized void update(String last_message) {
		job.update_date = System.currentTimeMillis();
		this.last_message = last_message;
		updateLastCaller();
	}
	
	/**
	 * Async update.
	 */
	public synchronized void updateStep(int step, int step_count) {
		job.update_date = System.currentTimeMillis();
		this.step = step;
		this.step_count = step_count;
		updateLastCaller();
	}
	
	/**
	 * Async update.
	 */
	public synchronized void updateProgress(int progress, int progress_size) {
		job.update_date = System.currentTimeMillis();
		this.progress = progress;
		this.progress_size = progress_size;
		updateLastCaller();
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("step:");
		sb.append(step);
		sb.append("/");
		sb.append(step_count);
		sb.append(" progress:");
		sb.append(progress);
		sb.append("/");
		sb.append(progress_size);
		sb.append(" \"");
		sb.append(last_message);
		sb.append("\" by ");
		sb.append(last_caller);
		return sb.toString();
	}
}
