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

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import controllers.Check;
import controllers.Secure;
import hd3gtv.configuration.GitInfo;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.manager.AsyncJSBrokerRequestAction;
import hd3gtv.mydmam.manager.AsyncJSBrokerRequestList;
import hd3gtv.mydmam.manager.AsyncJSBrokerResponseAction;
import hd3gtv.mydmam.manager.AsyncJSBrokerResponseList;
import hd3gtv.mydmam.manager.JobNG;
import hd3gtv.mydmam.web.AJSController;

public class Broker extends AJSController {
	
	/*private static Type al_String_typeOfT = new TypeToken<ArrayList<String>>() {
	}.getType();*/
	
	/*private static Type hm_StringJob_typeOfT = new TypeToken<HashMap<String, JobNG>>() {
	}.getType();*/
	
	static {
		AJSController.registerTypeAdapter(AsyncJSBrokerResponseList.class, new JsonSerializer<AsyncJSBrokerResponseList>() {
			public JsonElement serialize(AsyncJSBrokerResponseList src, Type typeOfSrc, JsonSerializationContext context) {
				return src.list;
			}
		});
		
		AJSController.registerTypeAdapter(AsyncJSBrokerResponseAction.class, new JsonSerializer<AsyncJSBrokerResponseAction>() {
			public JsonElement serialize(AsyncJSBrokerResponseAction src, Type typeOfSrc, JsonSerializationContext context) {
				return src.modified_jobs;
			}
		});
	}
	
	@Check("showBroker")
	public static AsyncJSBrokerResponseList list(AsyncJSBrokerRequestList request) throws Exception {
		AsyncJSBrokerResponseList result = new AsyncJSBrokerResponseList();
		result.list = JobNG.Utility.getJobsFromUpdateDate(request.since);
		return result;
	}
	
	@Check("showBroker")
	public static String appversion() throws Exception {
		return GitInfo.getFromRoot().getActualRepositoryInformation();
	}
	
	@Check("actionBroker")
	public static AsyncJSBrokerResponseAction action(AsyncJSBrokerRequestAction request) throws Exception {
		AsyncJSBrokerResponseAction result = new AsyncJSBrokerResponseAction();
		
		Loggers.Job.info("Do action on job(s), caller: " + AJSController.getUserProfile().getKey() + " [" + Secure.getRequestAddress() + "], " + request);
		
		if (request.job_key != null) {
			result.modified_jobs = JobNG.Utility.alterJobByKey(request.job_key, request.order);
		} else if (request.all_status != null) {
			result.modified_jobs = JobNG.Utility.alterJobsByStatus(request.all_status, request.order);
		}
		
		return result;
	}
	
}
