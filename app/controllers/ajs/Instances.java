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
package controllers.ajs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import controllers.Check;
import controllers.Secure;
import hd3gtv.configuration.GitInfo;
import hd3gtv.mydmam.db.status.ElasticsearchStatus;
import hd3gtv.mydmam.manager.AsyncJSInstanceActionRequest;
import hd3gtv.mydmam.manager.BrokerNG;
import hd3gtv.mydmam.manager.InstanceAction;
import hd3gtv.mydmam.manager.InstanceStatus;
import hd3gtv.mydmam.web.AJSController;
import hd3gtv.mydmam.web.PlayServerReport;
import hd3gtv.mydmam.web.PlayServerUpdateConfiguration;

public class Instances extends AJSController {
	
	/**
	 * @return instance ref -> raw JS
	 */
	@Check("showInstances")
	public static JsonObject allSummaries() {
		return InstanceStatus.getAll(InstanceStatus.CF_COLS.COL_SUMMARY);
	}
	
	/**
	 * @return instance ref -> raw JS
	 */
	@Check("showInstances")
	public static JsonObject allThreads() {
		return InstanceStatus.getAll(InstanceStatus.CF_COLS.COL_THREADS);
	}
	
	/**
	 * @return instance ref -> raw JS
	 */
	@Check("showInstances")
	public static JsonObject allClasspaths() {
		return InstanceStatus.getAll(InstanceStatus.CF_COLS.COL_CLASSPATH);
	}
	
	/**
	 * @return instance ref -> raw JS
	 */
	@Check("showInstances")
	public static JsonObject allPerfStats() {
		return InstanceStatus.getAll(InstanceStatus.CF_COLS.COL_PERFSTATS);
	}
	
	/**
	 * @return instance ref -> raw JS
	 */
	@Check("showInstances")
	public static JsonObject allItems() {
		return InstanceStatus.getAll(InstanceStatus.CF_COLS.COL_ITEMS);
	}
	
	/**
	 * @return [job]
	 */
	@Check("showInstances")
	public static JsonArray allDoneJobs() throws Exception {
		return BrokerNG.getAllDoneJobs();
	}
	
	/**
	 * @return [action]
	 */
	@Check("showInstances")
	public static JsonArray allPendingActions() throws Exception {
		return InstanceAction.getAllPending();
	}
	
	@Check("showInstances")
	public static void truncate() throws Exception {
		InstanceStatus.truncate();
		InstanceAction.truncate();
		Thread.sleep(300);
	}
	
	@Check("showInstances")
	public static String appversion() throws Exception {
		try {
			return GitInfo.getFromRoot().getActualRepositoryInformation();
		} catch (NullPointerException e) {
			return "";
		}
	}
	
	@Check("doInstanceAction")
	public static void instanceAction(AsyncJSInstanceActionRequest action) throws Exception {
		action.doAction(getUserProfile().getKey() + " " + Secure.getRequestAddress());
	}
	
	@Check("showInstances")
	public static ElasticsearchStatus esClusterStatus() throws Exception {
		ElasticsearchStatus es_status = new ElasticsearchStatus();
		es_status.refreshStatus();
		return es_status;
	}
	
	@Check("showInstances")
	public static PlayServerReport playserver() throws Exception {
		return new PlayServerReport();
	}
	
	@Check("showInstances")
	public static PlayServerReport playserverUpdate(PlayServerUpdateConfiguration upd_conf) throws Exception {
		upd_conf.doAction();
		return new PlayServerReport();
	}
	
}
