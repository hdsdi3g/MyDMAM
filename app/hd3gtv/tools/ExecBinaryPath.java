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
package hd3gtv.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;

import hd3gtv.configuration.Configuration;

public class ExecBinaryPath {
	
	public final static Logger Log = Logger.getLogger(ExecBinaryPath.class);
	
	private ExecBinaryPath() {
	}
	
	public static final String[] PATH;
	private static final HashMap<String, File> declared_in_configuration;
	private final static String[] WINDOWS_EXEC_EXTENTIONS = { "exe", "com", "bat", "cmd" };
	
	static {
		PATH = System.getenv("PATH").split(File.pathSeparator);
		declared_in_configuration = new HashMap<String, File>();
		
		Map<String, String> values = Configuration.global.getValues("executables");
		
		if (values != null) {
			File exec = null;
			for (Map.Entry<String, String> entry : values.entrySet()) {
				exec = new File(entry.getValue());
				if (validExec(exec)) {
					declared_in_configuration.put(entry.getKey(), exec);
					continue;
				}
				Log.error("Invalid declared_in_configuration executable: Key[" + entry.getKey() + "] " + exec, new FileNotFoundException(exec.getPath()));
			}
		}
	}
	
	private static boolean validExec(File exec) {
		if (exec.exists() == false) {
			return false;
		}
		if (exec.isFile() == false) {
			return false;
		}
		if (exec.canRead() == false) {
			return false;
		}
		if (SystemUtils.IS_OS_WINDOWS) {
			for (int pos_w_exe = 0; pos_w_exe < WINDOWS_EXEC_EXTENTIONS.length; pos_w_exe++) {
				if (exec.getName().toLowerCase().endsWith("." + WINDOWS_EXEC_EXTENTIONS[pos_w_exe])) {
					return true;
				}
			}
			return false;
		} else {
			return exec.canExecute();
		}
	}
	
	/**
	 * Can add .exe to name if OS == Windows and if missing.
	 * @throws FileNotFoundException if exec don't exists or is not correctly declared_in_configuration.
	 */
	public static File get(String name) throws IOException {
		if (declared_in_configuration.containsKey(name)) {
			return declared_in_configuration.get(name);
		}
		
		File exec = new File(name);
		if (validExec(exec)) {
			return exec;
		}
		
		for (int pos_path = 0; pos_path < PATH.length; pos_path++) {
			exec = new File(PATH[pos_path] + File.separator + name);
			if (validExec(exec)) {
				return exec;
			}
			if (SystemUtils.IS_OS_WINDOWS) {
				for (int pos_w_exe = 0; pos_w_exe < WINDOWS_EXEC_EXTENTIONS.length; pos_w_exe++) {
					exec = new File(PATH[pos_path] + File.separator + name + "." + WINDOWS_EXEC_EXTENTIONS[pos_w_exe]);
					if (validExec(exec)) {
						return exec;
					}
				}
			}
		}
		
		throw new IOException("Can't found executable \"" + name + "\"");
	}
	
}
