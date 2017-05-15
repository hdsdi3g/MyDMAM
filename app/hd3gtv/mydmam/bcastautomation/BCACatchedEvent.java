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

public final class BCACatchedEvent {
	
	private long date;
	
	private String name;
	
	private Timecode duration;
	
	private long update_date;
	
	private transient boolean checked;
	
	private String external_ref;
	
	static BCACatchedEvent create(BCAAutomationEvent event, String external_ref) {
		BCACatchedEvent result = new BCACatchedEvent();
		result.date = event.getStartDate();
		result.duration = event.getDuration();
		result.name = event.getName();
		result.update_date = System.currentTimeMillis();
		result.checked = true;
		result.external_ref = external_ref;
		return result;
	}
	
	boolean compare(BCAAutomationEvent event) {
		/*System.out.println(event.getStartDate());
		System.out.println(date);
		System.out.println(Timecode.delta(duration, event.getDuration()));*/
		return event.getStartDate() == date && Math.abs(Timecode.delta(duration, event.getDuration())) < 10;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(Loggers.dateLog(date));
		sb.append(" ");
		sb.append(name);
		sb.append(" (");
		sb.append(duration);
		sb.append(") upt ");
		sb.append(Loggers.dateLog(update_date));
		return sb.toString();
	}
	
	boolean isOld() {
		return date + 10000 < System.currentTimeMillis();
	}
	
	boolean isChecked() {
		return checked;
	}
	
	void setChecked() {
		this.checked = true;
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
}
