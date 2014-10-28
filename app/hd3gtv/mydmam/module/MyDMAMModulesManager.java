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
package hd3gtv.mydmam.module;

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.cli.CliModule;
import hd3gtv.mydmam.metadata.Generator;
import hd3gtv.mydmam.taskqueue.CyclicCreateTasks;
import hd3gtv.mydmam.taskqueue.TriggerWorker;
import hd3gtv.mydmam.taskqueue.Worker;
import hd3gtv.mydmam.taskqueue.WorkerGroup;
import hd3gtv.mydmam.web.MenuEntry;
import hd3gtv.mydmam.web.SearchResultItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.yaml.snakeyaml.Yaml;

import play.Play;
import play.templates.TemplateLoader;
import play.utils.OrderSafeProperties;
import play.vfs.VirtualFile;

public class MyDMAMModulesManager {
	
	private static final List<MyDMAMModule> MODULES = new ArrayList<MyDMAMModule>();
	
	static {
		try {
			List<String> classes_to_test = new ArrayList<String>();
			
			/**
			 * Play modules
			 */
			for (Map.Entry<String, VirtualFile> entry : Play.modules.entrySet()) {
				File module_dir = entry.getValue().getRealFile();
				File[] module_app_content = (new File(module_dir.getAbsolutePath() + File.separator + "app")).listFiles(new FilenameFilter() {
					public boolean accept(File arg0, String arg1) {
						return arg1.endsWith(".java");
					}
				});
				for (int pos = 0; pos < module_app_content.length; pos++) {
					classes_to_test.add(module_app_content[pos].getName());
				}
			}
			
			/**
			 * Classpath modules
			 */
			String[] classpathelements = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
			
			for (int i = 0; i < classpathelements.length; i++) {
				if (classpathelements[i].endsWith(".jar")) {
					try {
						JarFile jfile = new JarFile(classpathelements[i]);
						for (Enumeration<JarEntry> entries = jfile.entries(); entries.hasMoreElements();) {
							JarEntry element = entries.nextElement();
							if (element.getName().endsWith(".class") & (element.getName().indexOf("/") == -1)) {
								classes_to_test.add(element.getName());
							}
						}
						jfile.close();
					} catch (IOException e) {
						Log2.log.error("Can't load/open jar file " + classpathelements[i], e);
					}
				} else {
					File directoryclass = new File(classpathelements[i]);
					if (directoryclass.exists() && directoryclass.isDirectory()) {
						File[] list = directoryclass.listFiles(new FilenameFilter() {
							public boolean accept(File arg0, String arg1) {
								return arg1.endsWith(".class");
							}
						});
						for (int j = 0; j < list.length; j++) {
							classes_to_test.add(list[j].getName());
						}
					}
				}
			}
			
			for (int pos_classes = 0; pos_classes < classes_to_test.size(); pos_classes++) {
				try {
					String classname = classes_to_test.get(pos_classes);
					if (classname.endsWith(".java")) {
						classname = classname.substring(0, classname.length() - (".java".length()));
					} else if (classname.endsWith(".class")) {
						classname = classname.substring(0, classname.length() - (".class".length()));
					}
					Object module_loader = Class.forName(classname).newInstance();
					if (module_loader instanceof MyDMAMModule) {
						MODULES.add((MyDMAMModule) module_loader);
					}
				} catch (ClassNotFoundException e) {
					Log2.log.error("Class not found " + classes_to_test.get(pos_classes), e);
				} catch (InstantiationException e) {
					Log2.log.error("Unvalid class " + classes_to_test.get(pos_classes), e);
				} catch (IllegalAccessException e) {
					Log2.log.error("Can't access to class " + classes_to_test.get(pos_classes), e);
				}
			}
			
		} catch (Exception e) {
			Log2.log.error("Can't load modules", e);
		}
	}
	
	public static List<MyDMAMModule> getAllModules() {
		return MODULES;
	}
	
	private static volatile LinkedHashMap<String, File> all_conf_directories;
	
	/**
	 * Don't use MODULES system, but application.conf & dependencies.yml.
	 * @return Module name -> conf path
	 */
	public static LinkedHashMap<String, File> getAllConfDirectories() {
		if (all_conf_directories == null) {
			all_conf_directories = new LinkedHashMap<String, File>();
			String[] classpathelements = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
			
			/**
			 * Search & parse application.conf & dependencies.yml
			 */
			Properties applicationconf = new OrderSafeProperties();
			List<String> modules_names = null;
			try {
				for (int i = 0; i < classpathelements.length; i++) {
					if (classpathelements[i].endsWith(".jar") == false) {
						File applicationconf_file = new File(classpathelements[i] + File.separator + "application.conf");
						File dependenciesyml_file = new File(classpathelements[i] + File.separator + "dependencies.yml");
						if (applicationconf_file.exists() && dependenciesyml_file.exists()) {
							if (applicationconf_file.isFile() && dependenciesyml_file.isFile()) {
								FileInputStream fis = new FileInputStream(applicationconf_file);
								applicationconf.load(fis);
								fis.close();
								
								fis = new FileInputStream(dependenciesyml_file);
								Yaml yaml = new Yaml();
								for (Object data : yaml.loadAll(fis)) {
									LinkedHashMap<?, ?> root_item = (LinkedHashMap<?, ?>) data;
									modules_names = (List<String>) root_item.get("require");
								}
								fis.close();
								
								all_conf_directories.put("play", new File(classpathelements[i]));
								break;
							}
						}
					}
				}
			} catch (Exception e) {
				Log2.log.error("Can't import modules configuration files", e);
			}
			
			// Add messages modules files in classpath jar files ?
			
			/**
			 * Import for each modules all conf/messages.*
			 */
			if (modules_names != null) {
				modules_names.remove("play");
				for (int pos_mn = 0; pos_mn < modules_names.size(); pos_mn++) {
					File conf_dir = new File(applicationconf.getProperty("module." + modules_names.get(pos_mn), "") + File.separator + "conf");
					if ((conf_dir.exists() == false) | (conf_dir.isDirectory() == false)) {
						Log2.log.error("Can't found module conf directory", new FileNotFoundException(conf_dir.getPath()), new Log2Dump("module name", modules_names.get(pos_mn)));
						continue;
					}
					all_conf_directories.put(modules_names.get(pos_mn), conf_dir);
				}
			}
		}
		
		return all_conf_directories;
	}
	
	public static List<CliModule> getAllCliModules() {
		List<CliModule> result = new ArrayList<CliModule>();
		List<CliModule> elements;
		for (int pos = 0; pos < MODULES.size(); pos++) {
			elements = MODULES.get(pos).getCliModules();
			if (elements != null) {
				result.addAll(elements);
			}
		}
		return result;
	}
	
	public static void declareAllModuleWorkerElement(WorkerGroup workergroup) {
		if (workergroup == null) {
			return;
		}
		
		List<Worker> elements_worker;
		List<CyclicCreateTasks> elements_cyclic;
		List<TriggerWorker> elements_trigger;
		
		for (int pos = 0; pos < MODULES.size(); pos++) {
			elements_worker = MODULES.get(pos).getWorkers();
			if (elements_worker != null) {
				for (int pos_worker = 0; pos_worker < elements_worker.size(); pos_worker++) {
					workergroup.addWorker(elements_worker.get(pos_worker));
				}
			}
			
			elements_cyclic = MODULES.get(pos).getCyclicsCreateTasks();
			if (elements_cyclic != null) {
				for (int pos_cyclic = 0; pos_cyclic < elements_cyclic.size(); pos_cyclic++) {
					workergroup.addCyclicWorker(elements_cyclic.get(pos_cyclic));
				}
			}
			
			elements_trigger = MODULES.get(pos).getTriggersWorker();
			if (elements_trigger != null) {
				for (int pos_trigger = 0; pos_trigger < elements_trigger.size(); pos_trigger++) {
					workergroup.addTriggerWorker(elements_trigger.get(pos_trigger));
				}
			}
		}
	}
	
	public static List<String> getESTypesForUserSearch() {
		ArrayList<String> all_elements = new ArrayList<String>();
		List<String> elements;
		for (int pos = 0; pos < MODULES.size(); pos++) {
			elements = MODULES.get(pos).getESTypeForUserSearch();
			if (elements == null) {
				continue;
			}
			all_elements.addAll(elements);
		}
		
		return all_elements;
	}
	
	private volatile static List<MenuEntry> user_menus_entries;
	
	private static class MenuEntryComparator implements Comparator<MenuEntry> {
		public int compare(MenuEntry o1, MenuEntry o2) {
			if (o1.order > o2.order) {
				return -1;
			} else if (o1.order < o2.order) {
				return +1;
			} else {
				return 0;
			}
		}
	}
	
	/**
	 * Reboot Play to see changes.
	 */
	public static List<MenuEntry> getAllUserMenusEntries() {
		if (user_menus_entries == null) {
			user_menus_entries = new ArrayList<MenuEntry>();
			
			List<MenuEntry> elements;
			for (int pos_module = 0; pos_module < MODULES.size(); pos_module++) {
				elements = MODULES.get(pos_module).getPublishedItemsToWebsiteUserMenu();
				if (elements == null) {
					continue;
				}
				user_menus_entries.addAll(elements);
			}
			Collections.sort(user_menus_entries, new MenuEntryComparator());
		}
		
		return user_menus_entries;
	}
	
	private volatile static List<MenuEntry> admin_menus_entries;
	
	/**
	 * Reboot Play to see changes.
	 */
	public static List<MenuEntry> getAllAdminMenusEntries() {
		if (admin_menus_entries == null) {
			admin_menus_entries = new ArrayList<MenuEntry>();
			
			List<MenuEntry> elements;
			for (int pos = 0; pos < MODULES.size(); pos++) {
				elements = MODULES.get(pos).getPublishedItemsToWebsiteAdminMenu();
				if (elements != null) {
					admin_menus_entries.addAll(elements);
				}
			}
			Collections.sort(admin_menus_entries, new MenuEntryComparator());
		}
		
		return admin_menus_entries;
	}
	
	/**
	 * Index -> Module
	 */
	private volatile static Map<String, MyDMAMModule> render_engines;
	
	/**
	 * Reboot Play to see changes.
	 */
	public static String renderSearchResult(SearchResultItem item) throws Exception {
		if (render_engines == null) {
			render_engines = new HashMap<String, MyDMAMModule>();
			List<String> es_type_handled;
			for (int pos_module = 0; pos_module < MODULES.size(); pos_module++) {
				es_type_handled = MODULES.get(pos_module).getESTypeForUserSearch();
				if (es_type_handled == null) {
					continue;
				}
				for (int pos_estype = 0; pos_estype < es_type_handled.size(); pos_estype++) {
					if (render_engines.containsKey(es_type_handled.get(pos_estype))) {
						Log2Dump dump = new Log2Dump();
						dump.add("es_type_handled", es_type_handled.get(pos_estype));
						dump.add("module", MODULES.get(pos_module).getClass().getName());
						Log2.log.error("Twice modules declares the same ES Type for user search", null, dump);
						continue;
					}
					render_engines.put(es_type_handled.get(pos_estype), MODULES.get(pos_module));
				}
			}
		}
		
		MyDMAMModule module_handle = render_engines.get(item.type);
		if (module_handle == null) {
			return null;
		}
		
		String templatename = module_handle.getTemplateNameForSearchResultItem(item);
		if (templatename == null) {
			Log2Dump dump = new Log2Dump();
			dump.add("module", module_handle.getClass().getName());
			dump.addAll(item);
			Log2.log.error("No template name for search result item", null);
			return null;
		}
		
		HashMap<String, Object> args = item.getArgs();
		args.put("_body", true);
		StringWriter writer = new StringWriter();
		args.put("out", new PrintWriter(writer));
		try {
			TemplateLoader.load(templatename).render(args);
		} catch (Exception e) {
			Log2Dump dump = new Log2Dump();
			dump.add("module", module_handle.getClass().getName());
			dump.add("template name", templatename);
			dump.addAll(item);
			Log2.log.error("Can't render search template name for search result item", e);
			throw e;
		}
		return writer.toString();
	}
	
	private volatile static List<ArchivingTapeLocalisator> tape_localisators;
	
	private static void populate_tape_localisators() {
		if (tape_localisators == null) {
			tape_localisators = new ArrayList<ArchivingTapeLocalisator>();
			ArchivingTapeLocalisator atl;
			for (int pos_module = 0; pos_module < MODULES.size(); pos_module++) {
				atl = MODULES.get(pos_module).getTapeLocalisator();
				if (atl == null) {
					continue;
				}
				tape_localisators.add(atl);
			}
		}
	}
	
	/**
	 * Reboot Play to see changes.
	 */
	@SuppressWarnings("unchecked")
	public static JSONObject getPositionInformationsByTapeName(String... tapenames) {
		populate_tape_localisators();
		JSONObject result = new JSONObject();
		
		JSONObject result_module;
		for (int pos = 0; pos < tape_localisators.size(); pos++) {
			result_module = tape_localisators.get(pos).getPositionInformationsByTapeName(tapenames);
			if (result_module.isEmpty()) {
				continue;
			}
			result.putAll(result_module);
		}
		
		return result;
	}
	
	/**
	 * Reboot Play to see changes.
	 */
	public static Map<String, List<String>> getPositions(String[] key) {
		populate_tape_localisators();
		HashMap<String, List<String>> positions = new HashMap<String, List<String>>();
		
		Map<String, List<String>> positions_module;
		for (int pos = 0; pos < tape_localisators.size(); pos++) {
			positions_module = tape_localisators.get(pos).getPositions(key);
			if (positions_module.isEmpty()) {
				continue;
			}
			positions.putAll(positions_module);
		}
		
		return positions;
	}
	
	private static volatile String fullliststorageindexnamejsonlistforhostedinarchiving;
	
	@SuppressWarnings("unchecked")
	public static String getStorageIndexNameJsonListForHostedInArchiving() {
		if (fullliststorageindexnamejsonlistforhostedinarchiving == null) {
			populate_tape_localisators();
			JSONArray ja = new JSONArray();
			
			JSONArray ja_module;
			for (int pos = 0; pos < tape_localisators.size(); pos++) {
				ja_module = tape_localisators.get(pos).getStorageIndexNameJsonListForHostedInArchiving();
				if (ja_module.isEmpty()) {
					continue;
				}
				ja.addAll(ja_module);
			}
			fullliststorageindexnamejsonlistforhostedinarchiving = ja.toJSONString();
		}
		
		return fullliststorageindexnamejsonlistforhostedinarchiving;
	}
	
	public static List<Generator> getAllExternalMetadataGenerator() {
		List<Generator> result = new ArrayList<Generator>();
		List<Generator> providers;
		for (int pos = 0; pos < MODULES.size(); pos++) {
			providers = MODULES.get(pos).getMetadataGenerator();
			if (providers == null) {
				continue;
			}
			if (providers.isEmpty()) {
				continue;
			}
			result.addAll(providers);
		}
		return result;
	}
	
}
