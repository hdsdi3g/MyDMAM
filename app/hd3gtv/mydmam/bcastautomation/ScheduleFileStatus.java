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
package hd3gtv.mydmam.bcastautomation;

import java.io.File;

public final class ScheduleFileStatus {
	
	private int event_count;
	private long last_event_start_date;
	private File schedule;
	
	public ScheduleFileStatus(File schedule, int event_count, long last_event_start_date) {
		this.schedule = schedule;
		this.event_count = event_count;
		this.last_event_start_date = last_event_start_date;
	}
	
	public int getEventCount() {
		return event_count;
	}
	
	public long getLastEventStartDate() {
		return last_event_start_date;
	}
	
	public File getScheduleFile() {
		return schedule;
	}
}
