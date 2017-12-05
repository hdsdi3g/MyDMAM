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
 * Copyright (C) hdsdi3g for hd3g.tv 3 déc. 2017
 * 
*/
package hd3gtv.mydmam;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

public class CheckJVM {
	private static Logger log = Logger.getLogger(CheckJVM.class);
	
	public static final String BASE_FILENAME = "jvm_checked_versions.json";
	
	public static final String JAVA_VENDOR = System.getProperty("java.vendor", "");
	public static final String JAVA_VERSION = System.getProperty("java.version", "");
	public static final String OS_ARCH = System.getProperty("os.arch", "");
	public static final String OS_NAME = System.getProperty("os.name", "");
	public static final String OS_VERSION = System.getProperty("os.version", "");
	
	public enum Level {
		/**
		 * Don't do checks
		 */
		IGNORE,
		/**
		 * Do checks and warns if it's not compilant.
		 */
		WARN,
		/**
		 * Will throw an RuntimeException if it's not compilant.
		 */
		STRICT;
	}
	
	public CheckJVM(Level check_level) {
		if (check_level == Level.IGNORE) {
			return;
		}
		
		File json_file = new File(MyDMAM.APP_ROOT_PLAY_CONF_DIRECTORY.getAbsolutePath() + File.separator + BASE_FILENAME);
		
		String content = null;
		try {
			content = FileUtils.readFileToString(json_file, MyDMAM.UTF8);
		} catch (IOException e) {
			if (check_level == Level.STRICT) {
				throw new RuntimeException("Can't open json_file: " + json_file, e);
			} else {
				log.warn("Can't open json_file: " + json_file, e);
				return;
			}
		}
		
		ValidatedSetups v_setups = MyDMAM.gson_kit.getGsonPretty().fromJson(content, ValidatedSetups.class);
		
		log.debug("Start JVM and setup conformance");
		
		try {
			v_setups.checks();
			log.debug("Conformance is OK");
		} catch (Exception e) {
			if (check_level == Level.STRICT) {
				throw new RuntimeException("Can't valid this setup: " + e.getMessage() + ". See " + json_file + " file.");
			} else {
				log.warn("Can't valid this JVM and this setup. You should not run MyDMAM with this JVM.", e);
			}
		}
	}
	
	interface CheckConformance {
		void checks() throws Exception;
	}
	
	public class ValidatedSetups implements CheckConformance {
		
		HashMap<String, ValidatedSetupsVendors> jvm_vendors;
		
		private ValidatedSetups() {
		}
		
		public void checks() throws Exception {
			if (jvm_vendors.containsKey(JAVA_VENDOR) == false) {
				throw new Exception("Not tested JVM vendor: " + JAVA_VENDOR);
			}
			jvm_vendors.get(JAVA_VENDOR).checks();
		}
	}
	
	public class ValidatedSetupsVendors implements CheckConformance {
		MinMaxVersions jvm_version;
		HashMap<String, ValidatedSetupsArchs> arch;
		
		private ValidatedSetupsVendors() {
		}
		
		public void checks() throws Exception {
			jvm_version.checks(JAVA_VERSION, "JVM version");
			
			if (arch.containsKey(OS_ARCH) == false) {
				throw new Exception("Not tested architecture: " + OS_ARCH);
			}
			arch.get(OS_ARCH).checks();
		}
		
	}
	
	public class ValidatedSetupsArchs implements CheckConformance {
		HashMap<String, ValidatedSetupsOS> os;
		
		private ValidatedSetupsArchs() {
		}
		
		public void checks() throws Exception {
			if (os.containsKey(OS_NAME) == false) {
				throw new Exception("Not tested OS: " + OS_NAME);
			}
			os.get(OS_NAME).checks();
		}
	}
	
	public class ValidatedSetupsOS implements CheckConformance {
		MinMaxVersions version;
		
		private ValidatedSetupsOS() {
		}
		
		public void checks() throws Exception {
			version.checks(OS_VERSION, "OS version");
		}
	}
	
	public class MinMaxVersions {
		String min;
		String max;
		
		private MinMaxVersions() {
		}
		
		public void checks(String current, String what) throws Exception {
			if (MyDMAM.versionCompare(current, min) < 0) {
				throw new Exception("Not tested " + what + ": " + current + " (only tested/functionnal since v" + min + ")");
			}
			if (MyDMAM.versionCompare(max, current) < 0) {
				throw new Exception("Not tested " + what + ": " + current + " (only tested/functionnal up to v" + max + ")");
			}
		}
	}
	
}
