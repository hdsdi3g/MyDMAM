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

import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.Callable;

import javax.mail.internet.InternetAddress;

import controllers.Check;
import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.auth.UserNG;
import hd3gtv.mydmam.ftpserver.AJSRequestAdminOperationUser;
import hd3gtv.mydmam.ftpserver.AJSRequestRecent;
import hd3gtv.mydmam.ftpserver.AJSResponseActivities;
import hd3gtv.mydmam.ftpserver.AJSResponseAdminOperationUser;
import hd3gtv.mydmam.ftpserver.AJSResponseGroupsDomainsLists;
import hd3gtv.mydmam.ftpserver.AJSResponseUserList;
import hd3gtv.mydmam.ftpserver.FTPActivity;
import hd3gtv.mydmam.ftpserver.FTPGroup;
import hd3gtv.mydmam.ftpserver.FTPUser;
import hd3gtv.mydmam.mail.EndUserBaseMail;
import hd3gtv.mydmam.web.AJSController;
import play.i18n.Lang;
import play.jobs.JobsPlugin;

public class FTPServer extends AJSController {
	
	public static boolean isEnabled() {
		return Configuration.global.isElementExists("ftpserveradmin");
	}
	
	@Check({ "ftpServer", "adminFtpServer" })
	public static AJSResponseActivities recentactivities(AJSRequestRecent request) throws Exception {
		AJSResponseActivities response = new AJSResponseActivities();
		response.activities = FTPActivity.getRecentActivities(request.user_session_ref, request.max_items, request.searched_text, request.searched_action_type);
		return response;
	}
	
	@Check("adminFtpServer")
	public static AJSResponseAdminOperationUser adminOperationUser(AJSRequestAdminOperationUser request) throws Exception {
		AJSResponseAdminOperationUser response = new AJSResponseAdminOperationUser();
		
		try {
			switch (request.operation) {
			case CREATE:
				FTPUser ftp_user = request.createFTPUser();
				response.user_name = ftp_user.getName();
				response.done = true;
				JobsPlugin.executor.submit(new SendMailAfterSave(ftp_user, request.clear_password, AJSController.getUserProfile()));
				JobsPlugin.executor.submit(new CreateUserDir(ftp_user));
				break;
			case DELETE:
				request.delete();
				response.done = true;
				JobsPlugin.executor.submit(new DeleteUserActivities(request.user_id));
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
		} catch (SecurityException e) {
			Loggers.Play.error("User " + AJSController.getUserProfileLongName() + " can't do this: " + e.getMessage());
			response.done = false;
			return response;
		}
		
		if (response.done) {
			JobsPlugin.executor.submit(new BackupAfterSave());
		}
		
		return response;
	}
	
	private static class CreateUserDir implements Callable<Void> {
		
		FTPUser ftp_user;
		
		public CreateUserDir(FTPUser ftp_user) {
			this.ftp_user = ftp_user;
		}
		
		public Void call() throws Exception {
			if (FTPGroup.isConfigured()) {
				String dir = ftp_user.getHomeDirectory();
				Loggers.Play.info("Create local user " + ftp_user.getName() + " FTP directory: " + dir);
			} else {
				Loggers.Play.info("Can't create local user FTP directory: groups are not configured");
			}
			return null;
		}
	}
	
	private static class SendMailAfterSave implements Callable<Void> {
		
		FTPUser ftp_user;
		String clear_password;
		UserNG user;
		
		public SendMailAfterSave(FTPUser ftp_user, String clear_password, UserNG user) {
			this.ftp_user = ftp_user;
			this.clear_password = clear_password;
			this.user = user;
		}
		
		public Void call() throws Exception {
			if (user.getEmailAddr() == null) {
				return null;
			}
			if (user.getEmailAddr().equals("")) {
				return null;
			}
			InternetAddress email_addr = new InternetAddress(user.getEmailAddr());
			
			EndUserBaseMail mail;
			if (user.getLanguage() == null) {
				mail = new EndUserBaseMail(Locale.getDefault(), "adduserftpserver", email_addr);
			} else {
				mail = new EndUserBaseMail(Lang.getLocale(user.getLanguage()), "adduserftpserver", email_addr);
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
			if (mail_vars.containsKey("host") == false) {
				mail_vars.put("host", "(no set in configuration for this domain)");
			}
			if (mail_vars.containsKey("uri") == false) {
				mail_vars.put("uri", "(no set in configuration for this domain)");
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
	
	private static class DeleteUserActivities implements Callable<Void> {
		
		String user_id;
		
		public DeleteUserActivities(String user_id) {
			this.user_id = user_id;
			if (user_id == null) {
				throw new NullPointerException("\"user_id\" can't to be null");
			}
		}
		
		public Void call() throws Exception {
			FTPActivity.purgeUserActivity(user_id);
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
