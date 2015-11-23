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

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.elasticsearch.search.SearchHit;

import com.google.common.io.Files;
import com.google.gson.Gson;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.cli.CliModule;
import hd3gtv.mydmam.manager.AppManager;
import hd3gtv.mydmam.manager.CyclicJobCreator;
import hd3gtv.mydmam.manager.InstanceActionReceiver;
import hd3gtv.mydmam.manager.InstanceStatusItem;
import hd3gtv.mydmam.manager.TriggerJobCreator;
import hd3gtv.mydmam.manager.WorkerNG;
import hd3gtv.mydmam.pathindexing.Importer;
import hd3gtv.mydmam.pathindexing.Importer.SearchPreProcessor;
import hd3gtv.mydmam.web.MenuEntry;
import hd3gtv.mydmam.web.search.SearchResult;
import hd3gtv.mydmam.web.search.SearchResultPreProcessor;
import hd3gtv.tools.CopyMove;
import play.Play;
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
						Loggers.Module.error("Can't load/open jar file " + classpathelements[i], e);
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
					Loggers.Module.error("Class not found " + classes_to_test.get(pos_classes), e);
				} catch (InstantiationException e) {
					Loggers.Module.error("Unvalid class " + classes_to_test.get(pos_classes), e);
				} catch (IllegalAccessException e) {
					Loggers.Module.error("Can't access to class " + classes_to_test.get(pos_classes), e);
				}
			}
			
		} catch (Exception e) {
			Loggers.Module.error("Can't load modules", e);
		}
	}
	
	public static List<MyDMAMModule> getAllModules() {
		return MODULES;
	}
	
	private static volatile LinkedHashMap<String, File> all_conf_directories;
	
	/**
	 * Don't use MODULES system, but Play /module directory.
	 * @return ModuleName-Version -> conf path directory
	 */
	public static LinkedHashMap<String, File> getAllConfDirectories() {
		if (all_conf_directories == null) {
			all_conf_directories = new LinkedHashMap<String, File>();
			
			File app_module_path = new File(MyDMAM.APP_ROOT_PLAY_DIRECTORY.getPath() + File.separator + "modules");
			
			all_conf_directories.put("mydmam", new File(MyDMAM.APP_ROOT_PLAY_DIRECTORY.getPath() + File.separator + "conf"));
			
			try {
				CopyMove.checkExistsCanRead(app_module_path);
				CopyMove.checkIsDirectory(app_module_path);
			} catch (Exception e) {
				Loggers.Module.error("Can't found MyDMAM /modules directory", e);
				return all_conf_directories;
			}
			
			File[] modules_link_files = app_module_path.listFiles(new FileFilter() {
				
				public boolean accept(File pathname) {
					if (pathname.isDirectory()) {
						return false;
					}
					if (pathname.isHidden()) {
						return false;
					}
					if (pathname.canRead() == false) {
						return false;
					}
					return true;
				}
			});
			
			File module_path;
			File conf_dir;
			for (int pos = 0; pos < modules_link_files.length; pos++) {
				try {
					module_path = new File(Files.readFirstLine(modules_link_files[pos], Charset.defaultCharset()));
					CopyMove.checkExistsCanRead(module_path);
					CopyMove.checkIsDirectory(module_path);
					conf_dir = new File(module_path.getPath() + File.separator + "conf");
					CopyMove.checkExistsCanRead(conf_dir);
					CopyMove.checkIsDirectory(conf_dir);
					all_conf_directories.put(modules_link_files[pos].getName(), conf_dir);
				} catch (IOException e) {
					Loggers.Module.error("Can't read module desc file and found module conf directory, file: " + modules_link_files[pos], e);
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
	
	public static void declareAllModuleWorkerElement(AppManager manager) {
		if (manager == null) {
			return;
		}
		
		List<WorkerNG> elements_worker;
		List<CyclicJobCreator> elements_cyclic;
		List<TriggerJobCreator> elements_trigger;
		List<InstanceActionReceiver> elements_instance_action;
		List<InstanceStatusItem> elements_instance_status_item;
		
		for (int pos = 0; pos < MODULES.size(); pos++) {
			elements_worker = MODULES.get(pos).getWorkers();
			if (elements_worker != null) {
				for (int pos_worker = 0; pos_worker < elements_worker.size(); pos_worker++) {
					manager.register(elements_worker.get(pos_worker));
				}
			}
			
			elements_cyclic = MODULES.get(pos).getCyclicsCreateJobs(manager);
			if (elements_cyclic != null) {
				for (int pos_cyclic = 0; pos_cyclic < elements_cyclic.size(); pos_cyclic++) {
					manager.register(elements_cyclic.get(pos_cyclic));
				}
			}
			
			elements_trigger = MODULES.get(pos).getTriggersWorker(manager);
			if (elements_trigger != null) {
				for (int pos_trigger = 0; pos_trigger < elements_trigger.size(); pos_trigger++) {
					manager.register(elements_trigger.get(pos_trigger));
				}
			}
			
			elements_instance_action = MODULES.get(pos).getSpecificInstanceActionReceiver();
			if (elements_instance_action != null) {
				for (int pos_is = 0; pos_is < elements_instance_action.size(); pos_is++) {
					manager.registerInstanceActionReceiver(elements_instance_action.get(pos_is));
				}
			}
			
			elements_instance_status_item = MODULES.get(pos).getSpecificInstanceStatusItem();
			if (elements_instance_status_item != null) {
				for (int pos_is = 0; pos_is < elements_instance_status_item.size(); pos_is++) {
					manager.getInstanceStatus().registerInstanceStatusItem(elements_instance_status_item.get(pos_is));
				}
			}
		}
	}
	
	public static List<String> getESTypesForUserSearch() {
		ArrayList<String> all_elements = new ArrayList<String>();
		List<String> elements;
		
		/**
		 * Add Pathindex
		 */
		all_elements.addAll(new Importer.SearchPreProcessor().getESTypeForUserSearch());
		
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
	private volatile static Map<String, SearchResultPreProcessor> search_engines_pre_processing;
	
	/**
	 * Reboot Play to see changes.
	 * @return never null.
	 */
	public static SearchResult renderSearchResult(SearchHit hit) throws Exception {
		SearchResult result = new SearchResult(hit.getIndex(), hit.getType(), hit.getId(), hit.getSource(), hit.getScore());
		
		if (search_engines_pre_processing == null) {
			search_engines_pre_processing = new HashMap<String, SearchResultPreProcessor>();
			
			/**
			 * Add Pathindex
			 */
			SearchPreProcessor pathindex_preproc = new Importer.SearchPreProcessor();
			List<String> es_type_handled = pathindex_preproc.getESTypeForUserSearch();
			for (int pos_estype = 0; pos_estype < es_type_handled.size(); pos_estype++) {
				search_engines_pre_processing.put(es_type_handled.get(pos_estype), pathindex_preproc);
			}
			
			for (int pos_module = 0; pos_module < MODULES.size(); pos_module++) {
				es_type_handled = MODULES.get(pos_module).getESTypeForUserSearch();
				if (es_type_handled == null) {
					continue;
				}
				for (int pos_estype = 0; pos_estype < es_type_handled.size(); pos_estype++) {
					if (search_engines_pre_processing.containsKey(es_type_handled.get(pos_estype))) {
						Loggers.Module.error("Twice modules declares the same ES Type for user search, es_type_handled: " + es_type_handled.get(pos_estype) + ", module: "
								+ MODULES.get(pos_module).getClass().getName());
						continue;
					}
					search_engines_pre_processing.put(es_type_handled.get(pos_estype), MODULES.get(pos_module));
				}
			}
		}
		
		SearchResultPreProcessor pre_processor = search_engines_pre_processing.get(hit.getType());
		if (pre_processor == null) {
			return result;
		}
		pre_processor.prepareSearchResult(hit, result);
		
		if (result.getContent() == null) {
			result.setContent(new HashMap<String, Object>());
		}
		return result;
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
	public static Map<String, ArchivingTapeInformation> getPositionInformationsByTapeName(String... tapenames) {
		populate_tape_localisators();
		
		Map<String, ArchivingTapeInformation> result_module;
		HashMap<String, ArchivingTapeInformation> result = new HashMap<String, ArchivingTapeInformation>();
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
	
	public static String getStorageIndexNameJsonListForHostedInArchiving() {
		if (fullliststorageindexnamejsonlistforhostedinarchiving == null) {
			populate_tape_localisators();
			List<String> ja = new ArrayList<String>();
			
			List<String> ja_module;
			for (int pos = 0; pos < tape_localisators.size(); pos++) {
				ja_module = tape_localisators.get(pos).getStorageIndexNameListForHostedInArchiving();
				if (ja_module.isEmpty()) {
					continue;
				}
				ja.addAll(ja_module);
			}
			fullliststorageindexnamejsonlistforhostedinarchiving = new Gson().toJson(ja);
		}
		
		return fullliststorageindexnamejsonlistforhostedinarchiving;
	}
	
}
