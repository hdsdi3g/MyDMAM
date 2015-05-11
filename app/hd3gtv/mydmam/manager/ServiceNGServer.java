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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.mydmam.manager;

import hd3gtv.configuration.Configuration;
import hd3gtv.tools.Execprocess;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

public class ServiceNGServer extends ServiceNG {
	
	public ServiceNGServer(String[] args) throws Exception {
		super(args, "Play Server service handler");
	}
	
	private Execprocess process_play;
	
	protected boolean startBroker() {
		return false;
	}
	
	/**
	 * Search application.conf in classpath, and return the /mydmam main directory.
	 */
	public static File getMyDMAMRootPlayDirectory() throws FileNotFoundException {
		String[] classpathelements = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
		/**
		 * Search application.conf
		 */
		for (int i = 0; i < classpathelements.length; i++) {
			if (classpathelements[i].endsWith(".jar")) {
				continue;
			}
			File applicationconf_file = new File(classpathelements[i] + File.separator + "application.conf");
			if (applicationconf_file.exists()) {
				return (new File(classpathelements[i]).getParentFile());
			}
		}
		throw new FileNotFoundException("Can't found MyDMAM Play application");
	}
	
	protected void startService() throws Exception {
		File f_playdeploy = new File(Configuration.global.getValue("play", "deploy", "/opt/play"));
		
		ArrayList<String> p_play = new ArrayList<String>();
		p_play.add("run");
		p_play.add(getMyDMAMRootPlayDirectory().getAbsolutePath());
		p_play.add("--silent");
		
		String config_path = System.getProperty("service.config.path", "");
		if (config_path.equals("") == false) {
			p_play.add("-Dservice.config.path=" + config_path);
		}
		String config_select_apply = System.getProperty("service.config.apply", "");
		if (config_select_apply.equals("") == false) {
			p_play.add("-Dservice.config.apply=" + config_select_apply);
		}
		String config_verboseload = System.getProperty("service.config.verboseload", "");
		if (config_verboseload.equals("") == false) {
			p_play.add("-Dservice.config.verboseload=" + config_verboseload);
		}
		
		process_play = new Execprocess(f_playdeploy.getPath() + File.separator + "play", p_play, new ExecprocessEventServicelog("play"));
		process_play.start();
	}
	
	protected void stopService() throws Exception {
		process_play.kill();
	}
}
