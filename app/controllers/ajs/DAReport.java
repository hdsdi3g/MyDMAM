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

import java.util.ArrayList;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import controllers.Check;
import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.auth.AuthTurret;
import hd3gtv.mydmam.auth.UserNG;
import hd3gtv.mydmam.auth.asyncjs.UserAdminUpdate;
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
	
	public static final String PRIVILEGE_USER = "userDAReport";
	
	@Check("adminDAReport")
	public static AJS_DAR_AccountList_Rs accountdelete(String user_key) throws Exception {
		Loggers.DAReport.info("Remove account: " + user_key);
		DARAccount.delete(user_key);
		
		AuthTurret auth = MyDMAM.getPlayBootstrapper().getAuth();
		try {
			UserNG user = auth.getByUserKey(user_key);
			if (user != null) {
				if (user.getUser_groups_roles_privileges().stream().anyMatch(privilege -> {
					return privilege.equalsIgnoreCase(PRIVILEGE_USER);
				})) {
					if (user.getUserGroups().stream().anyMatch(group -> {
						return group.getName().equalsIgnoreCase(AJS_DAR_AccountNew.GROUP_NAME);
					})) {
						/**
						 * Remove user from special group
						 */
						UserAdminUpdate uau = new UserAdminUpdate();
						uau.user_key = user.getKey();
						uau.user_groups = new ArrayList<>(user.getUserGroups().stream().filter(group -> {
							return group.getName().equalsIgnoreCase(AJS_DAR_AccountNew.GROUP_NAME) == false;
						}).map(group -> {
							return group.getKey();
						}).collect(Collectors.toList()));
						
						auth.changeAdminUserPasswordGroups(uau);
					} else {
						Loggers.DAReport.info("Please remove role(s)/group(s) with privilege \"" + PRIVILEGE_USER + "\" for user " + user.getFullname());
					}
				}
			}
		} catch (Exception e) {
			Loggers.DAReport.error("Can't update user rights", e);
		}
		
		AJS_DAR_AccountList_Rs list = new AJS_DAR_AccountList_Rs();
		list.populate(auth);
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
		
		AJS_DAR_EventList_Rs result = new AJS_DAR_EventList_Rs();
		result.populate(MyDMAM.getPlayBootstrapper().getAuth());
		return result;
	}
	
	@Check("adminDAReport")
	public static void eventsendmail(AJS_DAR_EventName order) throws Exception {
		order.sendMail();
	}
	
	@Check("adminDAReport")
	public static AJS_DAR_EventList_Rs eventdelete(AJS_DAR_EventName order) throws Exception {
		order.delete();
		
		AJS_DAR_EventList_Rs result = new AJS_DAR_EventList_Rs();
		result.populate(MyDMAM.getPlayBootstrapper().getAuth());
		return result;
	}
	
	@Check("adminDAReport")
	public static AJS_DAR_EventList_Rs eventlist() throws Exception {
		AJS_DAR_EventList_Rs result = new AJS_DAR_EventList_Rs();
		result.populate(MyDMAM.getPlayBootstrapper().getAuth());
		return result;
	}
	
	@Check(PRIVILEGE_USER)
	public static JsonObject reportnew(AJS_DAR_ReportNew order) throws Exception {
		order.create();
		JsonObject jo = new JsonObject();
		jo.addProperty("done", true);
		return jo;
	}
	
	@Check(PRIVILEGE_USER)
	public static JsonObject getpanelsformyjob() throws Exception {
		JsonObject jo = new JsonObject();
		
		DARAccount account = DARAccount.get(AJSController.getUserProfile().getKey());
		
		if (account == null) {
			jo.addProperty("error", "account is not declared");
		}
		
		try {
			jo.add("panels", MyDMAM.gson_kit.getGsonSimple().toJsonTree(DARDB.get().getPanelsForJob(account.getJobKey())));
			jo.add("jobname", MyDMAM.gson_kit.getGsonSimple().toJsonTree(DARDB.get().getJobLongName(account.getJobKey())));
		} catch (NullPointerException e) {
			jo.add("panels", new JsonArray());
			jo.addProperty("jobname", "");
		}
		
		return jo;
	}
	
	@Check("adminDAReport")
	public static JsonObject alldeclaredjobslist() throws Exception {
		return DARDB.get().allDeclaredJobs();
	}
	
	@Check(PRIVILEGE_USER)
	public static JsonArray eventlisttoday() throws Exception {
		return MyDMAM.gson_kit.getGsonSimple().toJsonTree(DAREvent.todayList(AJSController.getUserProfile())).getAsJsonArray();
	}
	
}
