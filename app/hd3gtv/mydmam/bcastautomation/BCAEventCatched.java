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

import hd3gtv.mydmam.Loggers;
import hd3gtv.tools.Timecode;

public final class BCAEventCatched {
	
	private long date;
	
	private String name;
	
	private Timecode duration;
	
	private String external_ref;
	
	private String original_event_key;
	
	/**
	 * @return checked event
	 */
	static BCAEventCatched create(BCAAutomationEvent event, String external_ref) {
		BCAEventCatched result = new BCAEventCatched();
		result.date = event.getStartDate();
		result.duration = event.getDuration();
		result.name = event.getName() + ": " + Loggers.dateLog(result.date) + " > " + Loggers.dateLog(event.getEndDate()) + " (" + Math.round(result.duration.getValue()) + " sec)";
		result.external_ref = external_ref;
		result.original_event_key = event.getPreviouslyComputedKey();
		return result;
	}
	
	public String toString() {
		return name + " [" + external_ref + "] event key: " + original_event_key;
	}
	
	boolean isOldAired() {
		return date + 10000l < System.currentTimeMillis();
	}
	
	public String getExternalRef() {
		return external_ref;
	}
	
	public long getDate() {
		return date;
	}
	
	public Timecode getDuration() {
		return duration;
	}
	
	String getOriginalEventKey() {
		return original_event_key;
	}
}
