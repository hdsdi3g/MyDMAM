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

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;

public class PlayServerUpdateConfiguration {
	
	boolean purgeplaycache = false;
	boolean refreshlogconf = false;
	boolean switchjsdevmode = false;
	boolean purgejs = false;
	boolean reset_process_time_log = false;
	boolean toogle_process_time_log = false;
	
	public void doAction() throws Exception {
		if (purgeplaycache) {
			Loggers.Play.info("Purge Play cache");
			MyDMAM.getPlayBootstrapper().clearPlayCache();
		}
		if (refreshlogconf) {
			Loggers.Play.info("Manual refresh log configuration");
			Loggers.refreshLogConfiguration();
		}
		if (switchjsdevmode) {
			JSSourceManager.switchSetJsDevMode();
			if (JSSourceManager.isJsDevMode()) {
				Loggers.Play_JSSource.info("Switch to JS dev mode");
			} else {
				Loggers.Play_JSSource.info("Switch to JS prod mode");
			}
			JSSourceManager.refreshAllSources();
		}
		if (purgejs) {
			Loggers.Play_JSSource.info("Purge and remake all JS computed");
			JSSourceManager.purgeAll();
		}
		if (reset_process_time_log) {
			PlayBootstrap playboot = MyDMAM.getPlayBootstrapper();
			
			if (playboot.getAJSProcessTimeLog() != null || playboot.getJSRessourceProcessTimeLog() != null) {
				Loggers.Play_JSSource.info("Reset process time logs");
				if (playboot.getAJSProcessTimeLog() != null) {
					playboot.getAJSProcessTimeLog().truncate();
				}
				if (playboot.getJSRessourceProcessTimeLog() != null) {
					playboot.getJSRessourceProcessTimeLog().truncate();
				}
			}
		}
		if (toogle_process_time_log) {
			if (MyDMAM.getPlayBootstrapper().getAJSProcessTimeLog() == null) {
				Loggers.Play_JSSource.info("Enable process time logs");
				MyDMAM.getPlayBootstrapper().enableProcessTimeLogs();
			} else {
				Loggers.Play_JSSource.info("Disable process time logs");
				MyDMAM.getPlayBootstrapper().disableProcessTimeLogs();
			}
		}
	}
	
}
