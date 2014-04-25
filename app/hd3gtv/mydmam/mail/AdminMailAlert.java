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

import hd3gtv.configuration.Configuration;
import hd3gtv.javasimpleservice.ServiceInformations;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;
import hd3gtv.log2.Log2Event;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javax.mail.internet.InternetAddress;

public class AdminMailAlert implements Log2Dumpable {
	
	private static MailCenter mailcenter;
	private static String hostname;
	private static String workername;
	private static InternetAddress admin_addr;
	
	static {
		try {
			mailcenter = MailCenter.getGlobal();
			workername = Configuration.global.getValue("service", "workername", "(no set)");
			admin_addr = new InternetAddress(Configuration.global.getValue("service", "administrator_mail", "root@localhost"));
		} catch (Exception e) {
			Log2.log.error("Can't init message alert", e);
		}
		
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			hostname = "localhost";
		}
		
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
	private ArrayList<Log2Dump> dumps;
	private ServiceInformations serviceinformations;
	private StringBuffer subject;
	private StackTraceElement caller;
	private boolean fatal_alert;
	private ArrayList<File> files;
	
	public AdminMailAlert setThrowable(Throwable throwable) {
		this.throwable = throwable;
		return this;
	}
	
	public AdminMailAlert addDump(Log2Dump dump) {
		if (dumps == null) {
			dumps = new ArrayList<Log2Dump>();
		}
		if (dump != null) {
			dumps.add(dump);
		}
		return this;
	}
	
	public AdminMailAlert addDump(Log2Dumpable dump) {
		return addDump(dump.getLog2Dump());
	}
	
	/**
	 * @param file Size must to be lower to 1MB, else the file will no be incorporated.
	 */
	public AdminMailAlert addNewAttachFileToMail(File file) {
		if (files == null) {
			files = new ArrayList<File>();
		}
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
		files.add(file);
		return this;
	}
	
	public AdminMailAlert setServiceinformations(ServiceInformations serviceinformations) {
		this.serviceinformations = serviceinformations;
		return this;
	}
	
	public void send() {
		try {
			
			/**
			 * Subject
			 */
			if (fatal_alert) {
				subject.append("Important ! ");
			}
			if (serviceinformations != null) {
				subject.append("[");
				subject.append(serviceinformations.getApplicationShortName());
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
				Log2Event.throwableToString(throwable, plaintext, "\r\n");
				plaintext.append("\r\n");
			}
			
			if (dumps != null) {
				for (int pos = 0; pos < dumps.size(); pos++) {
					dumps.get(pos).dumptoString(plaintext, "\r\n");
					plaintext.append("\r\n");
				}
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
			
			if (serviceinformations != null) {
				plaintext.append(serviceinformations.getApplicationName());
				plaintext.append(" - version ");
				plaintext.append(serviceinformations.getApplicationVersion());
				plaintext.append("\r\n");
				plaintext.append(serviceinformations.getApplicationCopyright());
				plaintext.append("\r\n");
			}
			
			mail.setPlaintext(plaintext.toString());
			
			if (fatal_alert) {
				mail.setMailPriority(MailPriority.HIGHEST);
			}
			
			mail.setFiles(files);
			
			mail.send();
			
			Log2.log.info("Send an alert mail", this);
		} catch (Exception e) {
			Log2.log.error("Fail to send an alert mail !", e, this);
		}
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump finaldump = new Log2Dump();
		finaldump.add("to", admin_addr);
		finaldump.add("subject", subject);
		return finaldump;
	}
	
}
