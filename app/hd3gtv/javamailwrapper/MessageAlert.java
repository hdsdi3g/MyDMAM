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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.javamailwrapper;

import hd3gtv.configuration.Configuration;
import hd3gtv.javamailwrapper.SendMail.Priority;
import hd3gtv.javasimpleservice.ServiceInformations;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;
import hd3gtv.log2.Log2Event;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;

public class MessageAlert implements Log2Dumpable {
	
	private static MailConfigurator mailconfigurator;
	private static String hostname;
	private static String workername;
	
	static {
		try {
			mailconfigurator = MailConfigurator.importConfiguration(Configuration.global);
			workername = Configuration.global.getValue("service", "workername", "(no set)");
			
			if (mailconfigurator == null) {
				throw new NullPointerException("Invalid Javamail configuration");
			}
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (Exception e) {
			Log2.log.error("Can't init message alert", e);
		}
	}
	
	private MessageAlert() {
	}
	
	public static MessageAlert create(String basemessage, boolean fatal_alert) {
		MessageAlert message = new MessageAlert();
		message.basemessage = basemessage;
		message.fatal_alert = fatal_alert;
		message.mail = new SendMail(mailconfigurator);
		message.to = message.mail.getTo();
		message.subject = new StringBuffer();
		message.caller = (new Throwable()).getStackTrace()[1];
		return message;
	}
	
	private Throwable throwable;
	private String basemessage;
	private ArrayList<Log2Dump> dumps;
	private SendMail mail;
	private ServiceInformations serviceinformations;
	private ArrayList<String> to;
	private StringBuffer subject;
	private StackTraceElement caller;
	private boolean fatal_alert;
	private ArrayList<File> files;
	
	public MessageAlert setThrowable(Throwable throwable) {
		this.throwable = throwable;
		return this;
	}
	
	public MessageAlert addDump(Log2Dump dump) {
		if (dumps == null) {
			dumps = new ArrayList<Log2Dump>();
		}
		if (dump != null) {
			dumps.add(dump);
		}
		return this;
	}
	
	public MessageAlert addDump(Log2Dumpable dump) {
		return addDump(dump.getLog2Dump());
	}
	
	/**
	 * @param file Size must to be lower to 1MB, else the file will no be incorporated.
	 */
	public MessageAlert addNewAttachFileToMail(File file) {
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
	
	public MessageAlert setServiceinformations(ServiceInformations serviceinformations) {
		this.serviceinformations = serviceinformations;
		return this;
	}
	
	public void send() {
		try {
			SendMailContent content = new SendMailContent();
			
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
			content.setSubject(subject.toString());
			
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
			
			content.setPlaintext(plaintext.toString());
			
			if (fatal_alert) {
				mail.setPriority(Priority.Highest);
			}
			
			content.setFiles(files);
			
			mail.send(content);
			
			Log2.log.info("Send an alert mail", this);
		} catch (Exception e) {
			Log2.log.error("Fail to send an alert mail !", e, this);
		}
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump finaldump = new Log2Dump();
		finaldump.add("to", to);
		finaldump.add("subject", subject);
		return finaldump;
	}
	
}
