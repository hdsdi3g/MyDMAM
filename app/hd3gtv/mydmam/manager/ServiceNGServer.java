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

import java.io.File;
import java.util.ArrayList;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.tools.Execprocess;

public class ServiceNGServer extends ServiceNG {
	
	public ServiceNGServer(String[] args) throws Exception {
		super(args, "Play Server service handler");
	}
	
	private Execprocess process_play;
	
	protected boolean startBroker() {
		return false;
	}
	
	protected void startService() throws Exception {
		File f_playdeploy = new File(Configuration.global.getValue("play", "deploy", "/opt/play"));
		
		ArrayList<String> p_play = new ArrayList<String>();
		p_play.add("run");
		p_play.add(MyDMAM.APP_ROOT_PLAY_DIRECTORY.getAbsolutePath());
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
		
		process_play = new Execprocess(new File(f_playdeploy.getPath() + File.separator + "play"), p_play, new ExecprocessEventServicelog("play"));
		process_play.start();
	}
	
	protected void stopService() throws Exception {
		process_play.kill();
	}
}
