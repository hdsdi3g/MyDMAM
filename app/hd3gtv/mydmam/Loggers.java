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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

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
	public final static Logger Logger_log = Logger.getLogger("mydmam.loggers");
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
	
	/*public static JsonObject getAllLevels() {
		JsonObject result = new JsonObject();
		@SuppressWarnings("unchecked")
		Enumeration<Logger> all_loggers = LogManager.getCurrentLoggers();
		
		Logger item;
		while (all_loggers.hasMoreElements()) {
			item = all_loggers.nextElement();
			result.addProperty(item.getName(), item.getEffectiveLevel().toString());
		}
		
		return result;
	}*/
	
	/**
	 * For all declared Loggers, even outside this code.
	 */
	/*public static void changeLevel(String logger_name, Level new_level) {
		Logger_log.info("Change level for " + logger_name + ": " + new_level);
		Logger l = Logger.getLogger(logger_name);
		l.setLevel(new_level);
	}
	
	public static void changeRootLevel(Level new_level) {
		Logger_log.info("Change root level: " + new_level);
		Logger.getRootLogger().setLevel(new_level);
	}*/
	
	private static class LogLevelItem {
		String name;
		Level level;
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(name);
			sb.append(": ");
			sb.append(level);
			return sb.toString();
		}
		
		LogLevelItem(Logger source) {
			name = source.getName();
			level = source.getEffectiveLevel();
		}
	}
	
	public static void displayCurrentConfiguration() {
		if (Logger_log.isDebugEnabled() == false) {
			return;
		}
		
		@SuppressWarnings("unchecked")
		Enumeration<Logger> all_loggers = LogManager.getCurrentLoggers();
		
		ArrayList<LogLevelItem> items = new ArrayList<Loggers.LogLevelItem>();
		while (all_loggers.hasMoreElements()) {
			items.add(new LogLevelItem(all_loggers.nextElement()));
		}
		
		items.sort(new Comparator<LogLevelItem>() {
			public int compare(LogLevelItem o1, LogLevelItem o2) {
				return o1.name.compareTo(o2.name);
			}
		});
		
		Logger_log.debug("Show log level configuration:");
		Logger_log.debug("root: " + LogManager.getRootLogger().getEffectiveLevel());
		for (int pos = 0; pos < items.size(); pos++) {
			Logger_log.debug(items.get(pos));
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
	
	public static final File log4j_xml_configuration_file = new File(MyDMAM.APP_ROOT_PLAY_DIRECTORY.getAbsolutePath() + File.separator + "conf" + File.separator + "log4j.xml");
	
	public static void refreshLogConfiguration() {
		if (log4j_xml_configuration_file.exists() == false) {
			return;
		}
		if (log4j_xml_configuration_file.canRead() == false) {
			return;
		}
		try {
			Logger_log.info("Refresh log configuration with this file: " + log4j_xml_configuration_file);
			/**
			 * Test if new XML file is working
			 */
			DOMConfigurator.configure(log4j_xml_configuration_file.getAbsolutePath());
			
			/**
			 * Erase actual log configuration
			 */
			LogManager.resetConfiguration();
			
			/**
			 * Push new configuration from XML
			 */
			DOMConfigurator.configure(log4j_xml_configuration_file.getAbsolutePath());
			
			displayCurrentConfiguration();
		} catch (Exception e) {
			Logger_log.error("Can't refresh log configuration with this file: " + log4j_xml_configuration_file, e);
		}
	}
}
