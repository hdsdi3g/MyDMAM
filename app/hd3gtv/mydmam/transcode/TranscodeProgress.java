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
package hd3gtv.mydmam.transcode;

import java.io.File;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.manager.JobContext;
import hd3gtv.mydmam.manager.JobProgression;

public abstract class TranscodeProgress {
	
	private File progressfile;
	private JobProgression progression;
	private Internal internal;
	private JobContext context;
	
	public final TranscodeProgress init(File progressfile, JobProgression progression, JobContext context) {
		this.progressfile = progressfile;
		if (progressfile == null) {
			throw new NullPointerException("\"progressfile\" can't to be null");
		}
		this.progression = progression;
		if (progression == null) {
			throw new NullPointerException("\"progression\" can't to be null");
		}
		this.context = context;
		if (context == null) {
			throw new NullPointerException("\"progress_context\" can't to be null");
		}
		return this;
	}
	
	protected boolean stopthread;
	
	/**
	 * Blocking
	 */
	public synchronized void stopWatching() {
		stopthread = true;
		if (internal == null) {
			return;
		}
		if (internal.isAlive()) {
			Loggers.Transcode.debug("Stop watch transcode progress for: " + progressfile.getPath());
			while (internal.isAlive()) {
				try {
					Thread.sleep(10);
				} catch (Exception e) {
					Loggers.Transcode.warn("Can't stop thread", e);
				}
			}
			Loggers.Transcode.debug("Watch transcode progress for: " + progressfile.getPath() + " is stopped");
		}
		internal = null;
	}
	
	public synchronized void startWatching() {
		internal = new Internal();
		internal.setDaemon(true);
		internal.setName("TranscodeProgress for " + progression.getJobKey());
		stopthread = false;
		internal.start();
		Loggers.Transcode.debug("Start watch transcode progress for: " + progressfile.getPath());
	}
	
	/**
	 * Use stopthread to stop loop.
	 */
	protected abstract void processAnalystProgressFile(File progressfile, JobProgression progression, JobContext context) throws Exception;
	
	private class Internal extends Thread {
		public void run() {
			try {
				processAnalystProgressFile(progressfile, progression, context);
			} catch (Exception e) {
				Loggers.Transcode.error("Error during progress analyst", e);
			}
		}
	}
	
	File getProgressfile() {
		return progressfile;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("progressfile: ");
		sb.append(progressfile.getAbsolutePath());
		return sb.toString();
	}
	
}
