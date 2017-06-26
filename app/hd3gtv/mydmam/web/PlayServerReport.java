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
package hd3gtv.mydmam.web;

import hd3gtv.mydmam.MyDMAM;
import play.Play;
import play.PlayPlugin;
import play.jobs.JobsPlugin;

public class PlayServerReport {
	
	boolean is_js_dev_mode;
	String pluginstatus;
	String ajs_process_time_log;
	String jsressource_process_time_log;
	
	public PlayServerReport() {
	}
	
	public PlayServerReport populate() {
		PlayPlugin plugin = Play.pluginCollection.getPluginInstance(JobsPlugin.class);
		if (plugin != null) {
			pluginstatus = plugin.getStatus();
		} else {
			pluginstatus = "(pluginCollection is disabled)";
		}
		is_js_dev_mode = JSSourceManager.isJsDevMode();
		
		if (MyDMAM.getPlayBootstrapper().getAJSProcessTimeLog() != null) {
			ajs_process_time_log = MyDMAM.getPlayBootstrapper().getAJSProcessTimeLog().toString();
		} else {
			ajs_process_time_log = null;
		}
		if (MyDMAM.getPlayBootstrapper().getJSRessourceProcessTimeLog() != null) {
			jsressource_process_time_log = MyDMAM.getPlayBootstrapper().getJSRessourceProcessTimeLog().toString();
		} else {
			jsressource_process_time_log = null;
		}
		return this;
	}
	
}
