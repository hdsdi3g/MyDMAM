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
package hd3gtv.mydmam;

import java.util.Enumeration;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.gson.JsonObject;

public final class Loggers {
	
	public final static Logger Manager = Logger.getLogger("mydmam.manager");
	public final static Logger Broker = Logger.getLogger("mydmam.broker");
	public final static Logger Worker = Logger.getLogger("mydmam.worker");
	public final static Logger Job = Logger.getLogger("mydmam.job");
	public final static Logger WatchFolder = Logger.getLogger("mydmam.watchFolder");
	public final static Logger Transcoder = Logger.getLogger("mydmam.transcoder");
	public final static Logger Loggers = Logger.getLogger("mydmam.loggers");
	
	public static JsonObject getAllLevels() {
		JsonObject result = new JsonObject();
		
		@SuppressWarnings("unchecked")
		Enumeration<Logger> all_loggers = LogManager.getCurrentLoggers();
		
		Logger item;
		while (all_loggers.hasMoreElements()) {
			item = all_loggers.nextElement();
			result.addProperty(item.getName(), item.getEffectiveLevel().toString());
		}
		
		return result;
	}
	
	/**
	 * For all declared Loggers, even outside this code.
	 */
	public static void changeLevel(String logger_name, Level new_level) {
		Loggers.info("Change level for " + logger_name + ": " + new_level);
		Logger l = Logger.getLogger(logger_name);
		l.setLevel(new_level);
	}
	
	public static void changeRootLevel(Level new_level) {
		Loggers.info("Change root level: " + new_level);
		Logger.getRootLogger().setLevel(new_level);
	}
	
	public static void displayCurrentConfiguration() {
		if (Loggers.isDebugEnabled()) {
			Loggers.debug("Show log level configuration:");
			
			Loggers.debug("root: " + LogManager.getRootLogger().getEffectiveLevel());
			
			@SuppressWarnings("unchecked")
			Enumeration<Logger> all_loggers = LogManager.getCurrentLoggers();
			
			Logger item;
			while (all_loggers.hasMoreElements()) {
				item = all_loggers.nextElement();
				Loggers.debug(item.getName() + ": " + item.getEffectiveLevel());
			}
			
		}
	}
}
