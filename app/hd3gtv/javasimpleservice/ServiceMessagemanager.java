/*
 * This file is part of Java Simple ServiceManager
 * 
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2008-2013
 * 
*/

package hd3gtv.javasimpleservice;

import hd3gtv.configuration.Configuration;
import hd3gtv.javamailwrapper.MailConfigurator;
import hd3gtv.javamailwrapper.SendMail;
import hd3gtv.javamailwrapper.SendMailContent;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;

import java.util.ArrayList;

@SuppressWarnings("nls")
public class ServiceMessagemanager {
	
	private MailConfigurator mailconfigurator;
	private ServiceManager serviceManager;
	
	public ServiceMessagemanager(ServiceManager serviceManager) {
		if (serviceManager == null) {
			throw new NullPointerException("\"serviceManager\" can't to be null");
		}
		this.serviceManager = serviceManager;
		
		mailconfigurator = MailConfigurator.importConfiguration(Configuration.global);
		
		if (mailconfigurator == null) {
			throw new NullPointerException("Invalid Javamail configuration in xmlconfig");
		}
	}
	
	public void sendMessage(ServiceMessage message) {
		try {
			if (message == null) {
				throw new NullPointerException("\"message\" can't to be null");
			}
			
			SendMail mail = new SendMail(mailconfigurator);
			SendMailContent content = new SendMailContent();
			
			StringBuffer subject = new StringBuffer();
			StringBuffer plaintext = new StringBuffer();
			
			/**
			 * Subject
			 */
			subject.append("[");
			subject.append(serviceManager.getServiceinformations().getApplicationShortName());
			subject.append("] ");
			subject.append(message.getSubjectContent());
			
			/**
			 * Message
			 */
			plaintext.append(message.getMessageHeader());
			plaintext.append("\r\n");
			plaintext.append("\r\n");
			
			plaintext.append(message.getMessageContent(false));
			plaintext.append("\r\n");
			plaintext.append("\r\n");
			
			ArrayList<Log2Dump> tablecontent = message.getTablecontent();
			if (tablecontent != null) {
				if (tablecontent.size() > 0) {
					ArrayList<String> colsname = tablecontent.get(0).getAllKeyName();
					for (int poscol = 0; poscol < colsname.size(); poscol++) {
						plaintext.append(colsname.get(poscol));
						plaintext.append("\t");
					}
					plaintext.append("\r\n");
					
					ArrayList<String[]> values;
					for (int postbl = 0; postbl < tablecontent.size(); postbl++) {
						values = tablecontent.get(postbl).getAllValueMeta();
						for (int posval = 0; posval < values.size(); posval++) {
							plaintext.append(values.get(posval)[0]);
							if (values.get(posval)[1] != null) {
								plaintext.append(" [");
								plaintext.append(values.get(posval)[1]);
								plaintext.append("]");
							}
							plaintext.append("\t");
						}
						plaintext.append("\r\n");
					}
					plaintext.append("\r\n");
				}
			}
			
			plaintext.append(message.getMessageFooter());
			plaintext.append("\r\n");
			plaintext.append("\r\n");
			
			plaintext.append(serviceManager.getServiceinformations().getApplicationName());
			plaintext.append(" - version ");
			plaintext.append(serviceManager.getServiceinformations().getApplicationVersion());
			plaintext.append("\r\n");
			plaintext.append(serviceManager.getServiceinformations().getApplicationCopyright());
			plaintext.append("\r\n");
			
			content.setSubject(subject.toString());
			content.setPlaintext(plaintext.toString());
			mail.send(content);
			
			Log2Dump dump = new Log2Dump();
			dump.add("to", mail.getTo());
			dump.add("subject", subject.toString());
			Log2.log.info("Send a mail", dump);
			
		} catch (Exception e) {
			Log2.log.error("Fail to send mail", e, message);
		}
	}
}
