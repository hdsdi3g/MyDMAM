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

import controllers.Check;
import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.dareport.AJS_DAR_AccountDelete;
import hd3gtv.mydmam.dareport.AJS_DAR_AccountList_Rs;
import hd3gtv.mydmam.dareport.AJS_DAR_AccountNew;
import hd3gtv.mydmam.dareport.AJS_DAR_EventDelete;
import hd3gtv.mydmam.dareport.AJS_DAR_EventList_Rs;
import hd3gtv.mydmam.dareport.AJS_DAR_EventNew;
import hd3gtv.mydmam.dareport.AJS_DAR_EventSendmail;
import hd3gtv.mydmam.web.AJSController;

public class DAReport extends AJSController {
	
	public static boolean isEnabled() {
		return Configuration.global.isElementExists("dareport_setup");
	}
	
	// TODO event
	// TODO report new
	// TODO search user
	// TODO get panels for user job
	// TODO get job list for admin
	
	@Check("adminDAReport")
	public static void accountdelete(AJS_DAR_AccountDelete order) throws Exception {
		// TODO
	}
	
	@Check("adminDAReport")
	public static AJS_DAR_AccountList_Rs accountlist() throws Exception {
		// TODO
		return null;
	}
	
	@Check("adminDAReport")
	public static void accountnew(AJS_DAR_AccountNew order) throws Exception {
		// TODO
	}
	
	@Check("adminDAReport")
	public static void accountupdate(AJS_DAR_AccountNew order) throws Exception {
		// TODO delete + add
	}
	
	@Check("adminDAReport")
	public static void eventnew(AJS_DAR_EventNew order) throws Exception {
		// TODO
	}
	
	@Check("adminDAReport")
	public static void sendmailforevent(AJS_DAR_EventSendmail order) throws Exception {
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
	
}
