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
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
*/
package hd3gtv.tools;

import org.apache.log4j.Logger;

public abstract class StoppableThread extends Thread {
	
	private volatile boolean want_to_stop;
	private Logger logger;
	
	public StoppableThread(String name) {
		super(name);
		this.setDaemon(true);
		want_to_stop = false;
	}
	
	public StoppableThread(String name, Logger logger) {
		this(name);
		this.logger = logger;
	}
	
	public boolean isWantToStop() {
		return want_to_stop;
	}
	
	public boolean isWantToRun() {
		return want_to_stop == false;
	}
	
	/**
	 * Non blocking
	 */
	public void wantToStop() {
		if (logger != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Manual stop");
			}
		}
		this.want_to_stop = true;
	}
	
	private static final Logger log = Logger.getLogger(StoppableThread.class);
	
	/**
	 * Blocking
	 */
	public void waitToStop() {
		wantToStop();
		try {
			while (isAlive()) {
				Thread.sleep(10);
			}
		} catch (InterruptedException e) {
			log.warn("Can't wait Thread stop", e);
		}
	}
	
	public void setLogger(Logger logger) {
		this.logger = logger;
	}
	
	protected void stoppableSleep(long milis) {
		if (logger != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Wait some times (" + milis + " msec)");
			}
		}
		
		try {
			long end_date = System.currentTimeMillis() + milis;
			while (end_date > System.currentTimeMillis() & want_to_stop == false) {
				Thread.sleep(50);
			}
		} catch (InterruptedException e) {
			log.warn("Can't wait Thread stop", e);
		}
	}
	
}
