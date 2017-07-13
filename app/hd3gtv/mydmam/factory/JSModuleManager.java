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
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
*/
package hd3gtv.mydmam.factory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;

import javax.script.ScriptException;

import org.apache.commons.io.FilenameUtils;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

public final class JSModuleManager {
	
	private ArrayList<File> js_conf_sources;
	private NashornEngine engine;
	private PublishedModuleJSAPI published_api;
	private ArrayList<JSModule> declared_modules;
	
	public JSModuleManager() {
		File configuration_dir = Configuration.getGlobalConfigurationDirectory();
		
		js_conf_sources = new ArrayList<>(Arrays.asList(configuration_dir.listFiles((file) -> {
			if (file.isDirectory()) {
				return false;
			}
			if (file.isHidden()) {
				return false;
			}
			return FilenameUtils.getExtension(file.getName()).equalsIgnoreCase("js");
		})));
		
		js_conf_sources.sort((l, r) -> {
			return l.compareTo(r);
		});
		
		published_api = new PublishedModuleJSAPI();
		
		engine = new NashornEngine();
		engine.getBindings().put("module", published_api);
		
		declared_modules = new ArrayList<>();
	}
	
	public class PublishedModuleJSAPI {
		private PublishedModuleJSAPI() {
		}
		
		public void register(Object _module) throws ScriptException {
			if (_module instanceof ScriptObjectMirror == false) {
				throw new ScriptException("module parameter must be a JS object");
			}
			ScriptObjectMirror module = (ScriptObjectMirror) _module;
			
			if (module.isArray()) {
				throw new ScriptException("module parameter can't be a JS Array");
			}
			if (module.getClassName().equalsIgnoreCase("Object") == false) {
				throw new ScriptException("module parameter must be a JS object");
			}
			
			JSModule new_module = new JSModule();
			Arrays.asList(module.getOwnKeys(false)).stream().forEach(key -> {
				try {
					JSModule.class.getDeclaredField(key).set(new_module, module.get(key));
				} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
					throw new RuntimeException("Can't acces to field \"" + key + "\" in JSModule class", e);
				}
			});
			
			if (new_module.content == null) {
				throw new NullPointerException("Content field is not set... This module (" + new_module.toString() + ") can't to be fonctionnal...");
			}
			
			declared_modules.add(new_module);
		}
	}
	
	/**
	 * @return null if it can't found a module by name.
	 */
	<T> T moduleBindTo(String module_name, Class<? extends T> interface_reference) {
		JSModule module = declared_modules.stream().filter(m -> {
			return m.name.equalsIgnoreCase(module_name);
		}).findFirst().orElseGet(() -> {
			return null;
		});
		
		return module.bindTo(interface_reference);
	}
	
	final class JSModule {
		// Do not set to final..
		String name;
		String vendor;
		String version;
		ScriptObjectMirror content;
		
		public String toString() {
			return name + " (" + vendor + ") v" + version;
		}
		
		private JSModule() {
		}
		
		private <T> T bindTo(Class<? extends T> interface_reference) {
			return Factory.instanceDynamicProxy(interface_reference, (method_desc, arguments) -> {
				String method = method_desc.getName();
				if (content.containsKey(method) == false) {
					Loggers.Factory.warn("Interface " + interface_reference.getName() + " want to call a missing JS method, " + method + " for module " + toString() + " !");
					// TODO manage if interface has default declaration, and use it...
					return null;
				}
				Object o = content.get(method);
				if ((o instanceof ScriptObjectMirror) == false) {
					return o;
				}
				ScriptObjectMirror js_attribute = (ScriptObjectMirror) o;
				
				if (js_attribute.isArray()) {
					if (js_attribute.size() == 0) {
						return Collections.emptyList();
					} else {
						ArrayList<Object> items = new ArrayList<>(js_attribute.size());
						js_attribute.forEach((pos, value) -> {
							items.add(value);
						});
						return items;
					}
				} else if (js_attribute.isFunction()) {
					return js_attribute.call(null, arguments);
				} else {
					if (js_attribute.size() == 0) {
						return Collections.emptyMap();
					} else {
						LinkedHashMap<String, Object> items = new LinkedHashMap<>(js_attribute.size());
						js_attribute.forEach((key, value) -> {
							items.put(key, value);
						});
						return items;
					}
				}
			});
		}
		
	}
	
	public void load() {
		js_conf_sources.forEach(js_file -> {
			try {
				engine.eval(js_file);
			} catch (NullPointerException | IOException | ScriptException e) {
				Loggers.Factory.error("Can't load JS file: " + js_file, e);
			}
		});
	}
	
	// TODO CLI !
}
