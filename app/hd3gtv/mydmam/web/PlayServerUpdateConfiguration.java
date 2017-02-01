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
import play.cache.Cache;

public class PlayServerUpdateConfiguration {
	
	boolean purgeplaycache = false;
	boolean refreshlogconf = false;
	boolean switchjsdevmode = false;
	boolean purgejs = false;
	
	public void doAction() throws Exception {
		if (purgeplaycache) {
			Loggers.Play.info("Purge Play cache");
			Cache.clear();
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
	}
	
}
