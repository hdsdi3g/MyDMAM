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
package controllers.ajs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import controllers.Check;
import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.dareport.AJS_DAR_AccountList_Rs;
import hd3gtv.mydmam.dareport.AJS_DAR_AccountNew;
import hd3gtv.mydmam.dareport.AJS_DAR_EventList_Rs;
import hd3gtv.mydmam.dareport.AJS_DAR_EventName;
import hd3gtv.mydmam.dareport.AJS_DAR_EventNew;
import hd3gtv.mydmam.dareport.AJS_DAR_ReportNew;
import hd3gtv.mydmam.dareport.DARAccount;
import hd3gtv.mydmam.dareport.DARDB;
import hd3gtv.mydmam.dareport.DAREvent;
import hd3gtv.mydmam.web.AJSController;

public class DAReport extends AJSController {
	
	public static boolean isEnabled() {
		return Configuration.global.isElementExists("dareport_setup");
	}
	
	@Check("adminDAReport")
	public static AJS_DAR_AccountList_Rs accountdelete(String user_key) throws Exception {
		Loggers.DAReport.info("Remove account: " + user_key);
		DARAccount.delete(user_key);
		
		AJS_DAR_AccountList_Rs list = new AJS_DAR_AccountList_Rs();
		list.populate(MyDMAM.getPlayBootstrapper().getAuth());
		return list;
	}
	
	@Check("adminDAReport")
	public static AJS_DAR_AccountList_Rs accountnewupdate(AJS_DAR_AccountNew order) throws Exception {
		order.create();
		
		AJS_DAR_AccountList_Rs list = new AJS_DAR_AccountList_Rs();
		list.populate(MyDMAM.getPlayBootstrapper().getAuth());
		return list;
	}
	
	@Check("adminDAReport")
	public static AJS_DAR_AccountList_Rs accountlist() throws Exception {
		AJS_DAR_AccountList_Rs list = new AJS_DAR_AccountList_Rs();
		list.populate(MyDMAM.getPlayBootstrapper().getAuth());
		return list;
	}
	
	@Check("adminDAReport")
	public static AJS_DAR_EventList_Rs eventnew(AJS_DAR_EventNew order) throws Exception {
		order.create();
		return eventlist();
	}
	
	@Check("adminDAReport")
	public static void eventsendmail(AJS_DAR_EventName order) throws Exception {
		order.sendMain();
	}
	
	@Check("adminDAReport")
	public static void eventdelete(AJS_DAR_EventName order) throws Exception {
		order.delete();
	}
	
	@Check("adminDAReport")
	public static AJS_DAR_EventList_Rs eventlist() throws Exception {
		AJS_DAR_EventList_Rs result = new AJS_DAR_EventList_Rs();
		result.populate(MyDMAM.getPlayBootstrapper().getAuth());
		return result;
	}
	
	@Check("userDAReport")
	public static void reportnew(AJS_DAR_ReportNew order) throws Exception {
		order.create();
	}
	
	@Check("userDAReport")
	public static JsonObject getpanelsformyjob() throws Exception {
		JsonObject jo = new JsonObject();
		
		DARAccount account = DARAccount.get(AJSController.getUserProfile().getKey());
		
		if (account == null) {
			jo.addProperty("error", "account is not declared");
		}
		
		jo.add("panels", MyDMAM.gson_kit.getGsonSimple().toJsonTree(DARDB.get().getPanelsForJob(account.getJobKey())));
		jo.add("jobname", MyDMAM.gson_kit.getGsonSimple().toJsonTree(DARDB.get().getJobLongName(account.getJobKey())));
		
		return jo;
	}
	
	@Check("adminDAReport")
	public static JsonObject alldeclaredjobslist() throws Exception {
		return DARDB.get().allDeclaredJobs();
	}
	
	@Check("userDAReport")
	public static JsonArray eventlisttoday() throws Exception {
		return MyDMAM.gson_kit.getGsonSimple().toJsonTree(DAREvent.todayList()).getAsJsonArray();
	}
	
}
