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
import java.lang.reflect.Method;
import java.util.Arrays;

import org.apache.commons.io.FilenameUtils;

import com.google.gson.JsonParser;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.cli.CLIDefinition;
import hd3gtv.tools.ApplicationArgs;

public class CLIJS implements CLIDefinition {
	
	public String getCliModuleName() {
		return "jsinjava";
	}
	
	public String getCliModuleShortDescr() {
		return "Tools for manipulate JS files executed in Java (out of browsers)";
	}
	
	public void execCliModule(ApplicationArgs args) throws Exception {
		if (args.getParamExist("-run")) {
			NashornEngine ne = new NashornEngine();
			String f_name = args.getSimpleParamValue("-run");
			if (f_name == null) {
				System.err.println("No js file set for -run.");
				showFullCliModuleHelp();
				return;
			}
			ne.eval(new File(f_name));
		} else if (args.getParamExist("-template")) {
			String module_name = args.getSimpleParamValue("-template");
			if (module_name == null) {
				System.err.println("No module name is set for -template.");
				showFullCliModuleHelp();
				return;
			}
			String interface_name = args.getSimpleParamValue("-class");
			if (interface_name == null) {
				System.err.println("No -class set.");
				showFullCliModuleHelp();
				return;
			}
			Class<?> interface_class = MyDMAM.factory.getClassByName(interface_name);
			if (interface_class == null) {
				System.err.println("Can't found class " + interface_name);
				MyDMAM.factory.getAllClassesFromPackage(FilenameUtils.getBaseName(interface_name)).forEach(cl -> {
					System.err.println(" - " + cl.getName());
				});
				return;
			}
			
			File js_file = JSModuleManager.createEmptyJSDefinition(interface_class, module_name, args.getParamExist("no_hints"));
			System.out.println("Create " + js_file.getAbsolutePath() + " file");
		} else if (args.getParamExist("-module")) {
			String module_name = args.getSimpleParamValue("-module");
			if (module_name == null) {
				System.out.println("Load JS module manager...");
				new JSModuleManager().load();
				System.out.println("Done.");
				return;
			}
			String interface_name = args.getSimpleParamValue("-class");
			if (interface_name == null) {
				System.err.println("No -class set.");
				showFullCliModuleHelp();
				return;
			}
			Class<?> interface_class = MyDMAM.factory.getClassByName(interface_name);
			if (interface_class == null) {
				System.err.println("Can't found class " + interface_name);
				MyDMAM.factory.getAllClassesFromPackage(FilenameUtils.getBaseName(interface_name)).forEach(cl -> {
					System.err.println(" - " + cl.getName());
				});
				return;
			}
			
			String method = args.getSimpleParamValue("-method");
			if (method == null) {
				System.out.println("Try to instantiate " + interface_class.getSimpleName() + " via " + module_name + "...");
			}
			
			Object module = MyDMAM.factory.getInterfaceDeclaredByJSModule(interface_class, module_name, () -> {
				return null;
			});
			
			if (module == null) {
				System.err.println("Can't create " + interface_class.getName() + " via " + module_name);
				return;
			} else if (method == null) {
				System.out.println("Done: " + module);
				return;
			}
			
			Method method_to_run = Arrays.asList(interface_class.getMethods()).stream().filter(m -> {
				return m.getName().equalsIgnoreCase(method);
			}).findFirst().orElseThrow(() -> {
				return new IOException("Can't found method " + method + "() in " + interface_class.getName());
			});
			
			String method_args = args.getSimpleParamValue("-args");
			if (method_args == null | method_to_run.getParameterTypes().length == 0) {
				System.out.println("Result:");
				System.out.println(MyDMAM.gson_kit.getGsonPretty().toJson(method_to_run.invoke(module)));
			} else {
				Object o_method_args = method_args;
				
				JsonParser json_parser = new JsonParser();
				Class<?> ptype = method_to_run.getParameterTypes()[0];
				if (ptype != String.class) {
					o_method_args = MyDMAM.gson_kit.getGsonSimple().fromJson(json_parser.parse(method_args), ptype);
				}
				System.out.println("Result: " + MyDMAM.gson_kit.getGsonPretty().toJson(method_to_run.invoke(module, o_method_args)));
			}
		} else {
			showFullCliModuleHelp();
		}
	}
	
	public void showFullCliModuleHelp() {
		System.out.println("Usage for simply run a JS file: ");
		System.out.println(" " + getCliModuleName() + " -run <file.js>");
		System.out.println("Usage JSModule engine: ");
		System.out.println(" * Create a js module template");
		System.out.println("    " + getCliModuleName() + " -template <module_name> -class <class_name> [-no_hints]");
		System.out.println("   With -no_hints for hide tips in JS source");
		System.out.println(" * Just load modules");
		System.out.println("    " + getCliModuleName() + " -module");
		System.out.println(" * Instantiate an interface via a module ");
		System.out.println("    " + getCliModuleName() + " -module <module_name> -class <class_name>");
		System.out.println(" * Instantiate an interface and call a method");
		System.out.println("    " + getCliModuleName() + " -module <module_name> -class <class_name> -method <methodname> [-args json_format_args]");
	}
	
}
