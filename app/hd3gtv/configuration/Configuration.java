/*
 * This file is part of YAML Configuration for MyDMAM
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
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
*/
package hd3gtv.configuration;

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2FilterType;
import hd3gtv.log2.Log2Level;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class Configuration {
	
	public static Configuration global;
	
	static {
		refreshGlobalConfiguration();
	}
	
	public static void refreshGlobalConfiguration() {
		File file = new File(System.getProperty("service.config.path", "conf/app.d"));
		
		try {
			global = new Configuration(file);
		} catch (IOException e) {
			Log2Dump dump = new Log2Dump();
			dump.add("user-set", System.getProperty("service.config.path", "null"));
			dump.add("default", (new File("conf/app.d")).getAbsolutePath());
			Log2.log.error("Problem while load configuration documents", e, dump);
			System.exit(1);
		}
	}
	
	private HashMap<String, ConfigurationItem> configuration;
	
	public Configuration(File file) throws IOException {
		if (file.exists() == false) {
			throw new FileNotFoundException();
		}
		
		File[] files = file.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return (name.endsWith(".yml") | name.endsWith(".yaml") | name.endsWith(".conf"));
			}
		});
		
		configuration = new HashMap<String, ConfigurationItem>();
		Yaml yaml = new Yaml();
		
		FileInputStream fis;
		for (int pos = 0; pos < files.length; pos++) {
			fis = new FileInputStream(files[pos]);
			
			if (System.getProperty("service.config.verboseload", "").equalsIgnoreCase("true")) {
				Log2.log.info("Load configuration file", new Log2Dump("file", files[pos]));
			}
			
			for (Object data : yaml.loadAll(fis)) {
				if ((data instanceof LinkedHashMap<?, ?>) == false) {
					fis.close();
					throw new SyntaxConfigurationException("No Map at root document.");
				}
				
				try {
					LinkedHashMap<String, ?> document = (LinkedHashMap<String, ?>) data;
					int posdocument = 0;
					for (Map.Entry<String, ?> entry : document.entrySet()) {
						posdocument++;
						if ((entry.getValue() instanceof LinkedHashMap<?, ?>) == false) {
							fis.close();
							throw new SyntaxConfigurationException("No Map at root document.");
						}
						populateConfigurationItems(configuration, entry.getKey(), (LinkedHashMap<String, ?>) entry.getValue(), files[pos].getName() + ":" + posdocument);
					}
				} catch (Exception e) {
					fis.close();
					throw new SyntaxConfigurationException("Bad Map struct at root document.", e);
				}
			}
			fis.close();
		}
	}
	
	private static void populateConfigurationItems(HashMap<String, ConfigurationItem> baseelement, String itemname, LinkedHashMap<String, ?> content, String referer) {
		String select_apply = System.getProperty("service.config.apply", "");
		
		/**
		 * If admin select a version string to "apply", and configuration declare a version string/array to "apply", verify if equals.
		 * Else break.
		 */
		if ((select_apply.equals("") == false) & (content.containsKey("apply"))) {
			Object applyto = content.get("apply");
			if (applyto instanceof String) {
				if (select_apply.equalsIgnoreCase((String) applyto) == false) {
					return;
				}
			} else if (applyto instanceof ArrayList<?>) {
				ArrayList<String> list = (ArrayList<String>) applyto;
				boolean exists = false;
				for (int pos = 0; pos < list.size(); pos++) {
					if (select_apply.equalsIgnoreCase(list.get(pos))) {
						exists = true;
						break;
					}
				}
				if (exists == false) {
					return;
				}
			}
			content.remove("apply");
		}
		
		baseelement.put(itemname, new ConfigurationItem(content, referer));
	}
	
	public boolean isElementExists(String elementname) {
		return isElementExists(configuration, elementname);
	}
	
	public static boolean isElementExists(HashMap<String, ConfigurationItem> baseelement, String elementname) {
		if (baseelement == null) {
			return false;
		}
		return baseelement.containsKey(elementname);
	}
	
	public boolean isElementKeyExists(String elementname, String key) {
		return isElementKeyExists(configuration, elementname, key);
	}
	
	public static boolean isElementKeyExists(HashMap<String, ConfigurationItem> baseelement, String elementname, String key) {
		if (baseelement == null) {
			return false;
		}
		if (baseelement.containsKey(elementname) == false) {
			return false;
		}
		return baseelement.get(elementname).content.containsKey(key);
	}
	
	private static Log2Dump getLog2Dump(HashMap<String, ConfigurationItem> baseelement, String elementname, String key, Object defaultvalue) {
		if (isElementExists(baseelement, elementname) == false) {
			return null;
		}
		
		Log2Dump dump = new Log2Dump();
		dump.add("element", baseelement.get(elementname).referer + "/" + elementname + "/" + key);
		if (isElementKeyExists(baseelement, elementname, key)) {
			dump.add("value", baseelement.get(elementname).content.get(key));
		} else {
			dump.add("value", "(empty)");
		}
		dump.add("expected", defaultvalue);
		
		return dump;
	}
	
	public String getValue(String elementname, String key, String defaultvalue) {
		return getValue(configuration, elementname, key, defaultvalue);
	}
	
	public static String getValue(HashMap<String, ConfigurationItem> baseelement, String elementname, String key, String defaultvalue) {
		if (isElementKeyExists(baseelement, elementname, key) == false) {
			return defaultvalue;
		}
		Object value = baseelement.get(elementname).content.get(key);
		if (value instanceof String) {
			return (String) value;
		} else if (value instanceof Number) {
			return String.valueOf((Number) value);
		} else if (value instanceof Boolean) {
			return String.valueOf((Boolean) value);
		} else {
			Log2.log.error("Bad configuration value : not a string", null, getLog2Dump(baseelement, elementname, key, defaultvalue));
			return defaultvalue;
		}
	}
	
	public static List<ConfigurationClusterItem> getClusterConfiguration(HashMap<String, ConfigurationItem> baseelement, String elementname, String key, String defaultaddress, int defaultport) {
		List<ConfigurationClusterItem> items = new ArrayList<ConfigurationClusterItem>();
		
		if (isElementKeyExists(baseelement, elementname, key)) {
			Object value = baseelement.get(elementname).content.get(key);
			if (value instanceof String) {
				String[] clusterdef = ((String) value).split(",");
				for (int pos_cluster = 0; pos_cluster < clusterdef.length; pos_cluster++) {
					clusterdef[pos_cluster] = clusterdef[pos_cluster].trim();
					int colonpos = clusterdef[pos_cluster].indexOf(":");
					String hostname;
					int port;
					if (colonpos > 0) {
						hostname = clusterdef[pos_cluster].substring(0, colonpos);
						try {
							port = Integer.valueOf(clusterdef[pos_cluster].substring(colonpos + 1));
						} catch (NumberFormatException e) {
							Log2.log.error("Bad port definition : " + clusterdef[pos_cluster].substring(colonpos + 1) + " is not an integer", e);
							port = defaultport;
						}
					} else {
						hostname = clusterdef[pos_cluster];
						port = defaultport;
					}
					items.add(new ConfigurationClusterItem(hostname, port));
				}
			}
		}
		if ((items.size() == 0) & (defaultaddress != null)) {
			items.add(new ConfigurationClusterItem(defaultaddress, defaultport));
		}
		return items;
	}
	
	public List<ConfigurationClusterItem> getClusterConfiguration(String elementname, String key, String defaultaddress, int defaultport) {
		return getClusterConfiguration(configuration, elementname, key, defaultaddress, defaultport);
	}
	
	public int getValue(String elementname, String key, int defaultvalue) {
		return getValue(configuration, elementname, key, defaultvalue);
	}
	
	public static int getValue(HashMap<String, ConfigurationItem> baseelement, String elementname, String key, int defaultvalue) {
		if (isElementKeyExists(baseelement, elementname, key) == false) {
			return defaultvalue;
		}
		
		Object value = baseelement.get(elementname).content.get(key);
		if (value instanceof Integer) {
			return (Integer) value;
		} else {
			Log2.log.error("Bad configuration value : not an integer", null, getLog2Dump(baseelement, elementname, key, defaultvalue));
			return defaultvalue;
		}
	}
	
	public long getValue(String elementname, String key, long defaultvalue) {
		return getValue(configuration, elementname, key, defaultvalue);
	}
	
	public static long getValue(HashMap<String, ConfigurationItem> baseelement, String elementname, String key, long defaultvalue) {
		if (isElementKeyExists(baseelement, elementname, key) == false) {
			return defaultvalue;
		}
		
		Object value = baseelement.get(elementname).content.get(key);
		if (value instanceof Long) {
			return (Long) value;
		} else if (value instanceof Integer) {
			return (Integer) value;
		} else {
			Log2.log.error("Bad configuration value : not a long", null, getLog2Dump(baseelement, elementname, key, defaultvalue));
			return defaultvalue;
		}
	}
	
	public double getValue(String elementname, String key, double defaultvalue) {
		return getValue(configuration, elementname, key, defaultvalue);
	}
	
	public static double getValue(HashMap<String, ConfigurationItem> baseelement, String elementname, String key, double defaultvalue) {
		if (isElementKeyExists(baseelement, elementname, key) == false) {
			return defaultvalue;
		}
		
		Object value = baseelement.get(elementname).content.get(key);
		if (value instanceof Double) {
			return (Double) value;
		} else if (value instanceof Long) {
			return (Long) value;
		} else if (value instanceof Integer) {
			return (Integer) value;
		} else {
			Log2.log.error("Bad configuration value : not a double", null, getLog2Dump(baseelement, elementname, key, defaultvalue));
			return defaultvalue;
		}
	}
	
	/**
	 * @return true if value == true or != 0 or == "yes"
	 */
	public boolean getValueBoolean(String elementname, String key) {
		return getValueBoolean(configuration, elementname, key);
	}
	
	/**
	 * @return true if value == true or != 0 or == "yes"
	 */
	public static boolean getValueBoolean(HashMap<String, ConfigurationItem> baseelement, String elementname, String key) {
		if (isElementKeyExists(baseelement, elementname, key) == false) {
			return false;
		}
		
		Object value = baseelement.get(elementname).content.get(key);
		if (value instanceof Boolean) {
			return (Boolean) value;
		} else if (value instanceof Long) {
			return ((Long) value) != 0l;
		} else if (value instanceof Integer) {
			return ((Integer) value) != 0;
		} else if (value instanceof Double) {
			return ((Double) value) != 0d;
		} else if (value instanceof String) {
			return ((String) value).equalsIgnoreCase("yes");
		} else {
			Log2.log.error("Bad configuration value : not a boolean", null, getLog2Dump(baseelement, elementname, key, null));
			return false;
		}
	}
	
	public Object getRawValue(String elementname, String key) {
		return getRawValue(configuration, elementname, key);
	}
	
	public static Object getRawValue(HashMap<String, ConfigurationItem> baseelement, String elementname, String key) {
		if (isElementKeyExists(baseelement, elementname, key) == false) {
			return null;
		}
		return baseelement.get(elementname).content.get(key);
	}
	
	/**
	 * If key value is a String and not an List, create simple list and add value.
	 */
	public ArrayList<String> getValues(String elementname, String key, String defaultfirstelement) {
		return getValues(configuration, elementname, key, defaultfirstelement);
	}
	
	public LinkedHashMap<String, String> getValues(String elementname) {
		return getValues(configuration, elementname);
	}
	
	public static LinkedHashMap<String, String> getValues(HashMap<String, ConfigurationItem> baseelement, String elementname) {
		if (isElementExists(baseelement, elementname) == false) {
			return null;
		}
		ConfigurationItem ci = baseelement.get(elementname);
		return (LinkedHashMap<String, String>) ci.content;
	}
	
	public static ArrayList<String> getValues(HashMap<String, ConfigurationItem> baseelement, String elementname, String key, String defaultfirstelement) {
		ArrayList<String> result = new ArrayList<String>();
		if (isElementKeyExists(baseelement, elementname, key) == false) {
			if (defaultfirstelement != null) {
				result.add(defaultfirstelement);
				return result;
			} else {
				return null;
			}
		}
		
		Object value = baseelement.get(elementname).content.get(key);
		if (value instanceof ArrayList<?>) {
			ArrayList<?> list = (ArrayList<?>) value;
			Object element;
			for (int pos = 0; pos < list.size(); pos++) {
				element = list.get(pos);
				if (element instanceof String) {
					result.add((String) element);
				} else if (element instanceof Number) {
					result.add(String.valueOf((Number) element));
				} else if (element instanceof Boolean) {
					result.add(String.valueOf((Boolean) element));
				}
			}
			if (result.size() == 0) {
				if (defaultfirstelement != null) {
					result.add(defaultfirstelement);
				} else {
					return null;
				}
			}
			return result;
		} else {
			result.add(getValue(baseelement, elementname, key, defaultfirstelement));
			return result;
		}
	}
	
	/**
	 * @return Only the keys/value Maps for elementname in baseelement
	 */
	public HashMap<String, ConfigurationItem> getElement(String elementname) {
		return getElement(configuration, elementname);
	}
	
	/**
	 * @return Only the keys/value Maps for elementname in baseelement
	 */
	public static HashMap<String, ConfigurationItem> getElement(HashMap<String, ConfigurationItem> baseelement, String elementname) {
		ConfigurationItem ci = baseelement.get(elementname);
		
		if (ci == null) {
			return null;
		}
		
		HashMap<String, ConfigurationItem> result = new HashMap<String, ConfigurationItem>();
		
		for (Map.Entry<String, ?> entry : ci.content.entrySet()) {
			if (entry.getValue() instanceof LinkedHashMap<?, ?>) {
				populateConfigurationItems(result, entry.getKey(), (LinkedHashMap<String, ?>) entry.getValue(), ci.referer + "/" + elementname);
			}
		}
		
		if (result.size() == 0) {
			return null;
		}
		
		return result;
	}
	
	public static void importLog2Configuration(Configuration configuration, boolean logrotate_enabled) {
		try {
			/**
			 * Redirect STDOUT/ERR to log file.
			 */
			String outfilename = System.getProperty("service.redirectouterr", "");
			if (outfilename.equals("") == false) {
				File outfile = new File(outfilename);
				System.out.println("Redirect standard out and error out to " + outfile.getAbsolutePath());
				FileOutputStream fos = new FileOutputStream(outfile, true);
				PrintStream ps = new PrintStream(fos);
				System.setErr(ps);
				System.setOut(ps);
				System.out.println("Redirect standard out and error out to " + outfile.getAbsolutePath());
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		if (configuration.isElementExists("log2") == false) {
			return;
		}
		
		if (logrotate_enabled) {
			new Log2Rotate();
		}
		
		if (isElementKeyExists(configuration.configuration, "log2", "filter") == false) {
			return;
		}
		
		try {
			Object o_filters = configuration.configuration.get("log2").content.get("filter");
			ArrayList<?> filter_list = (ArrayList<?>) o_filters;
			
			LinkedHashMap<String, ?> filter_def;
			String filter_level;
			Log2Level level;
			String filter_for;
			Log2FilterType filtertype;
			String filter_type;
			for (int pos = 0; pos < filter_list.size(); pos++) {
				filter_def = (LinkedHashMap<String, ?>) filter_list.get(pos);
				filter_for = (String) filter_def.get("for");
				
				filter_level = (String) filter_def.get("level");
				if (filter_level.equalsIgnoreCase("DEBUG")) {
					level = Log2Level.DEBUG;
				} else if (filter_level.equalsIgnoreCase("INFO")) {
					level = Log2Level.INFO;
				} else if (filter_level.equalsIgnoreCase("ERROR")) {
					level = Log2Level.ERROR;
				} else if (filter_level.equalsIgnoreCase("SECURITY")) {
					level = Log2Level.SECURITY;
				} else if (filter_level.equalsIgnoreCase("NONE")) {
					level = Log2Level.NONE;
				} else {
					throw new Exception("Unknown " + filter_level + " level");
				}
				
				filter_type = (String) filter_def.get("type");
				if (filter_type.equalsIgnoreCase("HIDE")) {
					filtertype = Log2FilterType.HIDE;
				} else if (filter_type.equalsIgnoreCase("ONE_LINE")) {
					filtertype = Log2FilterType.ONE_LINE;
				} else if (filter_type.equalsIgnoreCase("NO_DUMP")) {
					filtertype = Log2FilterType.NO_DUMP;
				} else if (filter_type.equalsIgnoreCase("DEFAULT")) {
					filtertype = Log2FilterType.DEFAULT;
				} else if (filter_type.equalsIgnoreCase("VERBOSE_CALLER")) {
					filtertype = Log2FilterType.VERBOSE_CALLER;
				} else {
					throw new Exception("Unknown " + filter_type + " filter type");
				}
				
				Log2.log.createFilter(filter_for, level, filtertype);
			}
		} catch (Exception e) {
			Log2.log.error("Bad log filter configuration : check syntax", e);
		}
		
	}
}
