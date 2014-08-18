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
package hd3gtv.mydmam.useraction;

import hd3gtv.mydmam.taskqueue.Job;

public class UAJobProgress {
	
	private Job job;
	
	UAJobProgress(Job job) {
		this.job = job;
	}
	
	public UAJobProgress updateLastMessage(String message) {
		job.last_message = message;
		return this;
	}
	
	public UAJobProgress updateProgress(int progress) {
		job.progress = progress;
		return this;
	}
	
	public UAJobProgress updateProgress_size(int progress_size) {
		job.progress_size = progress_size;
		return this;
	}
	
	public UAJobProgress updateStep(int step) {
		job.step = step;
		return this;
	}
	
	public UAJobProgress updateStep_count(int step_count) {
		job.step_count = step_count;
		return this;
	}
	
}
