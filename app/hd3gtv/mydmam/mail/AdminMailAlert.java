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
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
*/
package hd3gtv.mydmam.mail;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javax.mail.internet.InternetAddress;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.manager.AppManager;

public class AdminMailAlert {
	
	private static MailCenter mailcenter;
	private static String hostname;
	private static String workername;
	private static InternetAddress admin_addr;
	
	private static final String NO_ADMIN_ADDR = "noset@noset.noset";
	
	static {
		try {
			mailcenter = MailCenter.getGlobal();
			workername = Configuration.global.getValue("service", "workername", "(no set)");
			admin_addr = new InternetAddress(Configuration.global.getValue("service", "administrator_mail", NO_ADMIN_ADDR));
		} catch (Exception e) {
			Loggers.Mail.error("Can't init message alert", e);
			System.exit(1);
		}
		
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			hostname = "localhost";
		}
		
	}
	
	public static String getAdminAddr(String default_addr) {
		if (admin_addr.getAddress().equalsIgnoreCase(NO_ADMIN_ADDR)) {
			return default_addr;
		}
		return admin_addr.getAddress();
	}
	
	private AdminMailAlert() {
	}
	
	public static AdminMailAlert create(String basemessage, boolean fatal_alert) {
		AdminMailAlert mail = new AdminMailAlert();
		mail.basemessage = basemessage;
		mail.fatal_alert = fatal_alert;
		mail.subject = new StringBuffer();
		mail.caller = (new Throwable()).getStackTrace()[1];
		return mail;
	}
	
	private Throwable throwable;
	private String basemessage;
	private AppManager manager;
	private StringBuffer subject;
	private StackTraceElement caller;
	private boolean fatal_alert;
	private ArrayList<File> files;
	private ArrayList<String> messagecontent;
	
	public AdminMailAlert setThrowable(Throwable throwable) {
		this.throwable = throwable;
		return this;
	}
	
	/**
	 * @param file Size must to be lower to 1MB, else the file will no be incorporated.
	 */
	public AdminMailAlert addNewAttachFileToMail(File file) {
		if (file == null) {
			return this;
		}
		if (file.exists() == false) {
			return this;
		}
		if (file.isFile() == false) {
			return this;
		}
		if (file.length() > (1024 * 1024)) {
			return this;
		}
		if (files == null) {
			files = new ArrayList<File>();
		}
		files.add(file);
		return this;
	}
	
	public AdminMailAlert addToMessagecontent(ArrayList<String> lines) {
		if (lines == null) {
			return this;
		}
		if (lines.isEmpty()) {
			return this;
		}
		if (messagecontent == null) {
			messagecontent = new ArrayList<String>(lines.size());
		}
		messagecontent.addAll(lines);
		return this;
	}
	
	public AdminMailAlert addToMessagecontent(String line) {
		if (line == null) {
			return this;
		}
		if (line.equals("")) {
			return this;
		}
		if (messagecontent == null) {
			messagecontent = new ArrayList<String>(1);
		}
		messagecontent.add(line);
		return this;
	}
	
	public AdminMailAlert setManager(AppManager manager) {
		this.manager = manager;
		return this;
	}
	
	public void send() {
		if (admin_addr.getAddress().equals(NO_ADMIN_ADDR)) {
			Loggers.Mail.info("No admin mail is declared: no mails will be send.");
			return;
		}
		
		try {
			
			/**
			 * Subject
			 */
			if (fatal_alert) {
				subject.append("Important ! ");
			}
			if (manager != null) {
				subject.append("[");
				subject.append(manager.getInstanceStatus().summary.getAppName());
				subject.append("] ");
			}
			subject.append("General error: ");
			subject.append(basemessage);
			
			MailContent mail = mailcenter.prepareMessage(subject.toString(), admin_addr);
			
			/**
			 * Message
			 */
			StringBuffer plaintext = new StringBuffer();
			plaintext.append("An error occurred when running the application.");
			plaintext.append("\r\n");
			plaintext.append("\r\n");
			
			plaintext.append(basemessage);
			plaintext.append("\r\n");
			plaintext.append("\r\n");
			
			if (messagecontent != null) {
				for (int pos = 0; pos < messagecontent.size(); pos++) {
					plaintext.append(messagecontent.get(pos).trim());
					plaintext.append("\r\n");
				}
				plaintext.append("\r\n");
			}
			
			plaintext.append("Send by: ");
			plaintext.append(Thread.currentThread().getName());
			plaintext.append(" (");
			plaintext.append(Thread.currentThread().getId());
			plaintext.append(") from ");
			plaintext.append(caller.getClassName());
			plaintext.append(".");
			plaintext.append(caller.getMethodName());
			plaintext.append("(");
			plaintext.append(caller.getFileName());
			plaintext.append(":");
			plaintext.append(caller.getLineNumber());
			plaintext.append(")\r\n");
			
			if (throwable != null) {
				plaintext.append("\r\nBy ");
				plaintext.append(throwable.getClass().getName());
				plaintext.append(": ");
				plaintext.append(throwable.getMessage());
				plaintext.append("\r\n");
				Loggers.throwableToString(throwable, plaintext, "\r\n");
				plaintext.append("\r\n");
			}
			
			plaintext.append("\r\n");
			
			if (fatal_alert) {
				plaintext.append("You must correct the problem and RESTART service. During this time, there will be no activity on it.");
				plaintext.append("\r\n");
				plaintext.append("Send from: ");
				plaintext.append(hostname);
				plaintext.append(" :: ");
				plaintext.append(workername);
				plaintext.append(".");
			} else {
				plaintext.append("Thank you for checking as soon as the state of the application (logs, service, mount ...) on ");
				plaintext.append(hostname);
				plaintext.append(" :: ");
				plaintext.append(workername);
				plaintext.append(".");
			}
			plaintext.append("\r\n");
			plaintext.append("\r\n");
			
			if (manager != null) {
				plaintext.append(manager.getInstanceStatus().summary.getInstanceName());
				plaintext.append(" - version ");
				plaintext.append(manager.getInstanceStatus().summary.getAppVersion());
				plaintext.append("\r\n");
				plaintext.append(MyDMAM.APP_COPYRIGHT);
				plaintext.append("\r\n");
			}
			
			mail.setPlaintext(plaintext.toString());
			
			if (fatal_alert) {
				mail.setMailPriority(MailPriority.HIGHEST);
			}
			
			mail.setFiles(files);
			
			mail.send();
			
			Loggers.Mail.info("Send an alert mail: " + this);
		} catch (Exception e) {
			Loggers.Mail.error("Fail to send an alert mail ! " + this, e);
		}
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("to: ");
		sb.append(admin_addr);
		sb.append(", subject: ");
		sb.append(subject);
		
		if (caller != null) {
			sb.append(", caller: ");
			sb.append(caller);
		}
		if (throwable != null) {
			sb.append(", throwable: ");
			sb.append(throwable);
		}
		
		return sb.toString();
	}
	
}
