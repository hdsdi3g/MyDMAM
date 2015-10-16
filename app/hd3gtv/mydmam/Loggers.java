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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.gson.JsonObject;

public final class Loggers {
	
	public final static Logger Manager = Logger.getLogger("mydmam.manager");
	public final static Logger ClusterStatus = Logger.getLogger("mydmam.clusterstatus");
	public final static Logger Broker = Logger.getLogger("mydmam.broker");
	public final static Logger Worker = Logger.getLogger("mydmam.worker");
	public final static Logger Job = Logger.getLogger("mydmam.job");
	public final static Logger Transcode = Logger.getLogger("mydmam.transcode");
	public final static Logger Transcode_Metadata = Logger.getLogger("mydmam.transcoder.mtd");
	public final static Logger Transcode_WatchFolder = Logger.getLogger("mydmam.transcode.watchfolder");
	public final static Logger Storage = Logger.getLogger("mydmam.storage");
	public final static Logger Storage_FTP = Logger.getLogger("mydmam.storage.ftp");
	public final static Logger Storage_FTPBCFT = Logger.getLogger("mydmam.storage.ftpbcst");
	public final static Logger Storage_Local = Logger.getLogger("mydmam.storage.local");
	public final static Logger Storage_SMB = Logger.getLogger("mydmam.storage.smb");
	public final static Logger Loggers = Logger.getLogger("mydmam.loggers");
	public final static Logger Cassandra = Logger.getLogger("mydmam.cassandra");
	public final static Logger Metadata = Logger.getLogger("mydmam.metadata");
	public final static Logger ORM = Logger.getLogger("mydmam.orm");
	public final static Logger ElasticSearch = Logger.getLogger("mydmam.elasticsearch");
	public final static Logger Play = Logger.getLogger("mydmam.play");
	public final static Logger Configuration = Logger.getLogger("mydmam.configuration");
	public final static Logger Auth = Logger.getLogger("mydmam.auth");
	public final static Logger CLI = Logger.getLogger("mydmam.cli");
	public final static Logger Mail = Logger.getLogger("mydmam.mail");
	public final static Logger Module = Logger.getLogger("mydmam.module");
	public final static Logger Ssh = Logger.getLogger("mydmam.ssh");
	public final static Logger Pathindex = Logger.getLogger("mydmam.pathindex");
	
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
	
	public static void throwableToString(Throwable error, StringBuffer append, String newline) {
		if (append == null) {
			return;
		}
		
		if (error == null) {
			return;
		}
		
		for (int i = 0; i < error.getStackTrace().length; i++) {
			append.append("    at ");
			append.append(error.getStackTrace()[i].toString());
			if (i + 1 < error.getStackTrace().length) {
				append.append(newline);
			}
		}
		
		if (error.getCause() != null) {
			append.append("  ");
			append.append(error.getCause().getMessage());
			append.append(newline);
			throwableToString(error.getCause(), append, newline);
		}
	}
	
	public static final String dateLog(long date) {
		return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss,SSS").format(new Date(date));
	}
	
}
