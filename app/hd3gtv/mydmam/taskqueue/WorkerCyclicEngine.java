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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam.taskqueue;

import java.util.ArrayList;
import java.util.List;

class WorkerCyclicEngine extends Worker {
	
	CyclicCreateTasks cyclic;
	
	private boolean operate;
	
	/**
	 * In msec
	 */
	private long time_to_sleep;
	
	/**
	 * In msec
	 */
	private long countdown_to_process;
	
	WorkerCyclicEngine(CyclicCreateTasks cyclic) {
		this.cyclic = cyclic;
	}
	
	/**
	 * Ignore job, start doCyclicProcess regulary.
	 */
	final public void process(Job job) throws Exception {
		operate = true;
		time_to_sleep = cyclic.getInitialCyclicPeriodTasks();
		if (time_to_sleep < 1000) {
			time_to_sleep = 1000;
		}
		countdown_to_process = time_to_sleep;
		
		while (operate) {
			cyclic.createTasks();
			
			for (int pos = 0; pos < (time_to_sleep / 1000); pos++) {
				/**
				 * For all seconds
				 */
				if (operate == false) {
					countdown_to_process = 0;
					return;
				}
				countdown_to_process = time_to_sleep - (pos * 1000);
				Thread.sleep(1000);
			}
		}
	}
	
	public boolean canChangeTimeToSleep() {
		return cyclic.isPeriodDurationForCreateTasksCanChange();
	}
	
	public final void forceStopProcess() throws Exception {
		operate = false;
	}
	
	/**
	 * @return in sec
	 */
	final synchronized int getCountdown_to_process() {
		return (int) countdown_to_process / 1000;
	}
	
	public long getTime_to_sleep() {
		return time_to_sleep;
	}
	
	public void setTime_to_sleep(long time_to_sleep) {
		if (canChangeTimeToSleep()) {
			this.time_to_sleep = time_to_sleep;
		} else {
			throw new NullPointerException("Can't change TTS, it's disabled");
		}
	}
	
	private ArrayList<Profile> profiles = new ArrayList<Profile>(1);
	
	public List<Profile> getManagedProfiles() {
		return profiles;
	}
	
	public String getShortWorkerName() {
		return cyclic.getShortCyclicName();
	}
	
	public String getLongWorkerName() {
		return cyclic.getLongCyclicName();
	}
	
	public boolean isConfigurationAllowToEnabled() {
		return cyclic.isCyclicConfigurationAllowToEnabled();
	}
	
}
