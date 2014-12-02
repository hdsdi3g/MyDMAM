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

public class JobProgression {
	
	private JobNG job;
	
	@SuppressWarnings("unused")
	private int progress = 0;
	
	@SuppressWarnings("unused")
	private int progress_size = 0;
	
	@SuppressWarnings("unused")
	private int step = 0;
	
	@SuppressWarnings("unused")
	private int step_count = 0;
	
	@SuppressWarnings("unused")
	private String last_message;
	
	JobProgression(JobNG job) {
		this.job = job;
	}
	
	/**
	 * Async update.
	 * @param last_message can be null.
	 */
	public synchronized void update(String last_message) {
		job.update_date = System.currentTimeMillis();
		this.last_message = last_message;
	}
	
	/**
	 * Async update.
	 */
	public synchronized void updateStep(int step, int step_count) {
		job.update_date = System.currentTimeMillis();
		this.step = step;
		this.step_count = step_count;
	}
	
	/**
	 * Async update.
	 */
	public synchronized void updateProgress(int progress, int progress_size) {
		job.update_date = System.currentTimeMillis();
		this.progress = progress;
		this.progress_size = progress_size;
	}
	
}
