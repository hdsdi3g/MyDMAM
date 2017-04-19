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
import hd3gtv.mydmam.dareport.AJS_DAR_AccountList_Rs;
import hd3gtv.mydmam.dareport.AJS_DAR_AccountNew;
import hd3gtv.mydmam.dareport.AJS_DAR_EventDelete;
import hd3gtv.mydmam.dareport.AJS_DAR_EventList_Rs;
import hd3gtv.mydmam.dareport.AJS_DAR_EventNew;
import hd3gtv.mydmam.dareport.AJS_DAR_EventSendmail;
import hd3gtv.mydmam.dareport.AJS_DAR_ReportNew;
import hd3gtv.mydmam.dareport.DARAccount;
import hd3gtv.mydmam.web.AJSController;

public class DAReport extends AJSController {
	
	public static boolean isEnabled() {
		return Configuration.global.isElementExists("dareport_setup");
	}
	
	@Check("adminDAReport")
	public static void accountdelete(String name) throws Exception {
		DARAccount.delete(name);
	}
	
	@Check("adminDAReport")
	public static void accountnewupdate(AJS_DAR_AccountNew order) throws Exception {
		order.create();
	}
	
	@Check("adminDAReport")
	public static AJS_DAR_AccountList_Rs accountlist() throws Exception {
		AJS_DAR_AccountList_Rs list = new AJS_DAR_AccountList_Rs();
		list.list = DARAccount.list();
		return list;
	}
	
	@Check("adminDAReport")
	public static void eventnew(AJS_DAR_EventNew order) throws Exception {
		// TODO
	}
	
	@Check("adminDAReport")
	public static void eventsendmail(AJS_DAR_EventSendmail order) throws Exception {
		// TODO send mail action
	}
	
	@Check("adminDAReport")
	public static void eventdelete(AJS_DAR_EventDelete order) throws Exception {
		// TODO
	}
	
	@Check("adminDAReport")
	public static AJS_DAR_EventList_Rs eventlist() throws Exception {
		// TODO
		return null;
	}
	
	@Check("userDAReport")
	public static void reportnew(AJS_DAR_ReportNew order) throws Exception {
		// TODO
	}
	
	@Check("userDAReport")
	public static JsonObject getpanelsformyjob() throws Exception {
		JsonObject jo = new JsonObject();
		// TODO put panels and job name
		return jo;
	}
	
	@Check("adminDAReport")
	public static JsonArray alldeclaredjobslist() throws Exception {
		// TODO
		JsonArray ja = new JsonArray();
		return ja;
	}
	
	// TODO get job list for admin
	
}
