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
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
*/
package hd3gtv.mydmam.server;

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.GitInfo;
import hd3gtv.javasimpleservice.ServiceInformations;
import hd3gtv.javasimpleservice.ServiceManager;
import hd3gtv.log2.Log2Dump;
import hd3gtv.tools.Execprocess;

import java.io.File;
import java.util.ArrayList;

public class MyDMAMServerService extends ServiceManager implements ServiceInformations {
	
	public MyDMAMServerService(String[] args, ServiceInformations serviceinformations) {
		super(args, serviceinformations);
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("applicationname", getApplicationName());
		dump.add("version", getApplicationVersion());
		return dump;
	}
	
	public String getApplicationName() {
		return "MyDMAM - Server service";
	}
	
	public String getApplicationVersion() {
		GitInfo git = GitInfo.getFromRoot();
		if (git != null) {
			return git.getBranch() + " " + git.getCommit();
		} else {
			return "noset";
		}
	}
	
	public String getApplicationCopyright() {
		return "Copyright (C) hdsdi3g for hd3g.tv 2012-2014";
	}
	
	public String getApplicationShortName() {
		return "MyDMAM-Server";
	}
	
	private Execprocess process_play;
	
	protected void startApplicationService() throws Exception {
		File f_playdeploy = new File(Configuration.global.getValue("play", "deploy", "/opt/play"));
		
		ArrayList<String> p_play = new ArrayList<String>();
		p_play.add("run");
		p_play.add((new File("")).getAbsolutePath());
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
	
	protected void stopApplicationService() throws Exception {
		process_play.kill();
	}
	
	protected void postClassInit() throws Exception {
	}
	
}
