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
package ext;

import hd3gtv.mydmam.MyDMAM;

import java.util.Map;
import java.util.Properties;

import play.i18n.Messages;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

@OnApplicationStart
public class Bootstrap extends Job {
	
	/**
	 * Inject configuration Messages to Play Messages
	 */
	public void doJob() {
		for (Map.Entry<String, Properties> entry : Messages.locales.entrySet()) {
			entry.getValue().putAll(MyDMAM.getconfiguredMessages());
		}
	}
}
