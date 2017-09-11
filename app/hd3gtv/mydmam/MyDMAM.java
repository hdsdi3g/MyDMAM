/*
 * This file is part of MyDMAM
 * 
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.embddb.EmbDDB;
import hd3gtv.mydmam.factory.Factory;
import hd3gtv.mydmam.gson.GsonKit;
import hd3gtv.mydmam.web.PlayBootstrap;

public class MyDMAM {
	
	public static final String LINESEPARATOR = System.getProperty("line.separator");
	
	/**
	 * Transform accents to non accented (ascii) version.
	 */
	public static final Pattern PATTERN_Combining_Diacritical_Marks = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
	
	/**
	 * Transform accents to non accented (ascii) version, and remove all spaces chars.
	 */
	public final static Pattern PATTERN_Combining_Diacritical_Marks_Spaced = Pattern.compile("[\\p{InCombiningDiacriticalMarks}\\s]+");
	
	/**
	 * Remove all non char/number like #@-"ı\r\n\t\t,\\;.?&'(§°*$%+=... BUT keep "_"
	 */
	public final static Pattern PATTERN_Special_Chars = Pattern.compile("[^\\w]");
	
	public static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static String APP_COPYRIGHT = "Copyright (C) hdsdi3g for hd3g.tv 2012-2017";
	
	public static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
	public static final Charset UTF8 = StandardCharsets.UTF_8;
	public static final Charset ISO_8859_1 = StandardCharsets.ISO_8859_1;
	
	public static final Factory factory = new Factory();
	
	/**
	 * Search application.conf in classpath, and return the /mydmam main directory.
	 */
	public static final File APP_ROOT_PLAY_DIRECTORY;
	
	/**
	 * @see APP_ROOT_PLAY_DIRECTORY
	 * @return APP_ROOT_PLAY_DIRECTORY/conf
	 */
	public static final File APP_ROOT_PLAY_CONF_DIRECTORY;
	
	static {
		APP_ROOT_PLAY_DIRECTORY = getMyDMAMRootPlayDirectory();
		APP_ROOT_PLAY_CONF_DIRECTORY = new File(MyDMAM.APP_ROOT_PLAY_DIRECTORY.getPath() + File.separator + "conf");
	}
	
	public static final GsonKit gson_kit = new GsonKit();
	
	private static PlayBootstrap play_bootstrapper;
	
	public static PlayBootstrap getPlayBootstrapper() {
		if (play_bootstrapper == null) {
			play_bootstrapper = new PlayBootstrap();
		}
		return play_bootstrapper;
	}
	
	public static final String byteToString(byte[] b) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < b.length; i++) {
			int v = b[i] & 0xFF;
			if (v < 16) {
				sb.append(0);
			}
			sb.append(Integer.toString(v, 16).toLowerCase());
		}
		return sb.toString();
	}
	
	private volatile static Properties configured_messages;
	
	/**
	 * @return Properties key/values by yaml configuration via "message" entry.
	 *         Restart to see changes.
	 */
	public static Properties getconfiguredMessages() {
		if (configured_messages == null) {
			configured_messages = new Properties();
			if (Configuration.global.isElementExists("messages")) {
				LinkedHashMap<String, String> conf = Configuration.global.getValues("messages");
				for (Map.Entry<String, String> entry : conf.entrySet()) {
					configured_messages.put(entry.getKey(), entry.getValue());
				}
			}
		}
		return configured_messages;
	}
	
	private static File getMyDMAMRootPlayDirectory() {
		/**
		 * Search application.conf
		 */
		Optional<File> o_applicationconf_file = factory.getClasspathOnlyDirectories().map(cp -> {
			return new File(cp.getPath() + File.separator + "application.conf");
		}).filter(appfile -> {
			return appfile.exists();
		}).findFirst();
		
		if (o_applicationconf_file.isPresent()) {
			/**
			 * /mydmam/conf/application.conf => /mydmam
			 */
			return o_applicationconf_file.get().getParentFile().getParentFile();
		}
		
		Loggers.Manager.error("Can't found MyDMAM Play application", new FileNotFoundException(new File("").getAbsolutePath()));
		return new File("");
	}
	
	/**
	 * Compares two version strings.
	 * Use this instead of String.compareTo() for a non-lexicographical
	 * comparison that works for version strings. e.g. "1.10".compareTo("1.6").
	 * It remove the "v" char in front.
	 * @note It does not work if "1.10" is supposed to be equal to "1.10.0".
	 * @param str1 a string of ordinal numbers separated by decimal points.
	 * @param str2 a string of ordinal numbers separated by decimal points.
	 * @return The result is a negative integer if str1 is _numerically_ less than str2.
	 *         The result is a positive integer if str1 is _numerically_ greater than str2.
	 *         The result is zero if the strings are _numerically_ equal.
	 * @see from http://stackoverflow.com/questions/6701948/efficient-way-to-compare-version-strings-in-java
	 */
	public static int versionCompare(String version1, String version2) {
		String str1 = version1;
		if (str1.startsWith("v")) {
			str1 = str1.substring(1);
		}
		str1 = str1.trim();
		
		String str2 = version2;
		if (str2.startsWith("v")) {
			str2 = str2.substring(1);
		}
		str2 = str2.trim();
		
		String[] vals1 = str1.split("\\.");
		String[] vals2 = str2.split("\\.");
		int i = 0;
		/**
		 * Set index to first non-equal ordinal or length of shortest version string
		 */
		while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i])) {
			i++;
		}
		/**
		 * Compare first non-equal ordinal number
		 */
		if (i < vals1.length && i < vals2.length) {
			int diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
			return Integer.signum(diff);
		}
		/**
		 * The strings are equal or one string is a substring of the other
		 * e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
		 */
		return Integer.signum(vals1.length - vals2.length);
	}
	
	private static EmbDDB embddb;
	private static boolean embddb_loaded = false;
	
	public static EmbDDB getEmbDDB() {
		if (embddb_loaded == false) {
			try {
				embddb = EmbDDB.createFromConfiguration();
			} catch (GeneralSecurityException | IOException | InterruptedException e) {
				Loggers.Manager.error("Can't load EmbDDB", e);
			}
			embddb_loaded = true;
		}
		return embddb;
	}
	
}
