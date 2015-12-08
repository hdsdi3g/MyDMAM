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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.Callable;

import javax.mail.internet.InternetAddress;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import controllers.Check;
import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.ftpserver.AJSRequestAdminOperationUser;
import hd3gtv.mydmam.ftpserver.AJSRequestRecent;
import hd3gtv.mydmam.ftpserver.AJSResponseActivities;
import hd3gtv.mydmam.ftpserver.AJSResponseAdminOperationUser;
import hd3gtv.mydmam.ftpserver.AJSResponseGroupsDomainsLists;
import hd3gtv.mydmam.ftpserver.AJSResponseUserList;
import hd3gtv.mydmam.ftpserver.AJSUser;
import hd3gtv.mydmam.ftpserver.FTPActivity;
import hd3gtv.mydmam.ftpserver.FTPOperations;
import hd3gtv.mydmam.ftpserver.FTPUser;
import hd3gtv.mydmam.mail.EndUserBaseMail;
import hd3gtv.mydmam.web.AJSController;
import models.UserProfile;
import play.i18n.Lang;
import play.jobs.JobsPlugin;

public class FTPServer extends AJSController {
	
	static Type type_List_FTPActivity = new TypeToken<ArrayList<FTPActivity>>() {
	}.getType();
	static Type type_List_User = new TypeToken<ArrayList<AJSUser>>() {
	}.getType();
	
	static {
		AJSController.registerTypeAdapter(AJSResponseActivities.class, new JsonSerializer<AJSResponseActivities>() {
			public JsonElement serialize(AJSResponseActivities src, Type typeOfSrc, JsonSerializationContext context) {
				JsonObject result = new JsonObject();
				result.add("activities", FTPOperations.getGson().toJsonTree(src.activities, type_List_FTPActivity));
				return result;
			}
		});
		
		AJSController.registerTypeAdapter(AJSResponseUserList.class, new JsonSerializer<AJSResponseUserList>() {
			public JsonElement serialize(AJSResponseUserList src, Type typeOfSrc, JsonSerializationContext context) {
				JsonObject result = new JsonObject();
				result.add("users", FTPOperations.getGson().toJsonTree(src.users, type_List_User));
				return result;
			}
		});
		
	}
	
	@Check({ "ftpServer", "adminFtpServer" })
	public static AJSResponseActivities recentactivities(AJSRequestRecent request) throws Exception {
		AJSResponseActivities response = new AJSResponseActivities();
		response.activities = FTPActivity.getRecentActivities(request.user_session_ref, request.max_items);
		return response;
	}
	
	@Check("adminFtpServer")
	public static AJSResponseAdminOperationUser adminOperationUser(AJSRequestAdminOperationUser request) throws Exception {
		AJSResponseAdminOperationUser response = new AJSResponseAdminOperationUser();
		
		switch (request.operation) {
		case CREATE:
			FTPUser ftp_user = request.createFTPUser();
			response.user_name = ftp_user.getName();
			response.done = true;
			JobsPlugin.executor.submit(new SendMailAfterSave(ftp_user, request.clear_password, AJSController.getUserProfile()));
			break;
		case DELETE:
			request.delete();
			response.done = true;
			break;
		case CH_PASSWORD:
			request.chPassword();
			response.done = true;
			break;
		case TOGGLE_ENABLE:
			request.toggleEnable();
			response.done = true;
			break;
		default:
			break;
		}
		
		if (response.done) {
			JobsPlugin.executor.submit(new BackupAfterSave());
		}
		
		return response;
	}
	
	private static class SendMailAfterSave implements Callable<Void> {
		
		FTPUser ftp_user;
		String clear_password;
		UserProfile user;
		
		public SendMailAfterSave(FTPUser ftp_user, String clear_password, UserProfile user) {
			this.ftp_user = ftp_user;
			this.clear_password = clear_password;
			this.user = user;
		}
		
		public Void call() throws Exception {
			if (user.email == null) {
				return null;
			}
			if (user.email.equals("")) {
				return null;
			}
			InternetAddress email_addr = new InternetAddress(user.email);
			
			EndUserBaseMail mail;
			if (user.language == null) {
				mail = new EndUserBaseMail(Locale.getDefault(), email_addr, "adduserftpserver");
			} else {
				mail = new EndUserBaseMail(Lang.getLocale(user.language), email_addr, "adduserftpserver");
			}
			HashMap<String, Object> mail_vars = new HashMap<String, Object>();
			mail_vars.put("login", ftp_user.getName());
			mail_vars.put("password", clear_password);
			
			if (Configuration.global.isElementExists("ftpserveradmin_hostsbydomain")) {
				String domain = ftp_user.getDomain();
				if (domain.equals("")) {
					domain = "default";
				}
				if (Configuration.global.isElementKeyExists("ftpserveradmin_hostsbydomain", domain)) {
					if (Configuration.isElementKeyExists(Configuration.global.getElement("ftpserveradmin_hostsbydomain"), domain, "host")) {
						String host = (String) Configuration.getRawValue(Configuration.global.getElement("ftpserveradmin_hostsbydomain"), domain, "host");
						mail_vars.put("host", host);
						mail_vars.put("uri", "ftp://" + ftp_user.getName() + ":" + clear_password + "@" + host + "/");
					}
				}
			}
			
			mail.send(mail_vars);
			return null;
		}
		
	}
	
	private static class BackupAfterSave implements Callable<Void> {
		
		public Void call() throws Exception {
			FTPUser.backupCF();
			return null;
		}
		
	}
	
	@Check({ "ftpServer", "adminFtpServer" })
	public static AJSResponseUserList allUsers() throws Exception {
		AJSResponseUserList ul = new AJSResponseUserList();
		ul.users = FTPUser.getAllAJSUsers();
		return ul;
	}
	
	@Check("adminFtpServer")
	public static AJSResponseGroupsDomainsLists groupDomainLists() throws Exception {
		return new AJSResponseGroupsDomainsLists();
	}
	
}
