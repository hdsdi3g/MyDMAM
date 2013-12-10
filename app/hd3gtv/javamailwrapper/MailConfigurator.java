/*
 * This file is part of Javamail Wrapper
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

package hd3gtv.javamailwrapper;

import hd3gtv.configuration.Configuration;

import java.util.ArrayList;
import java.util.Properties;

import javax.mail.Session;

@SuppressWarnings("nls")
public class MailConfigurator {
	
	String from;
	ArrayList<String> to;
	ArrayList<String> cc;
	ArrayList<String> bcc;
	String smtp_server;
	boolean debug;
	Session session;
	String mail_header_listid;
	
	MailConfigurator() {
		session = null;
		setDebug(false);
	}
	
	/**
	 * @param smtp_server Serveur a contacter pour envoyer le mail.
	 * @see importToXMLElement()
	 */
	public MailConfigurator(String smtp_server) {
		// T O D O fonctions de dig et de resolv MX, et A/AAAA
		Properties prop = System.getProperties();
		if (smtp_server == null) {
			smtp_server = "127.0.0.1";
		}
		if (smtp_server.equals("")) {
			smtp_server = "127.0.0.1";
		}
		prop.put("mail.smtp.host", smtp_server);
		session = Session.getDefaultInstance(prop);
		from = "root@localhost";
	}
	
	/**
	 * Expediteur par defaut.
	 */
	public void setFrom(String from) {
		this.from = from;
	}
	
	/**
	 * Destinataires par defaut.
	 */
	public void setTo(ArrayList<String> to) {
		this.to = to;
	}
	
	/**
	 * Destinataires par defaut.
	 */
	public void setCc(ArrayList<String> cc) {
		this.cc = cc;
	}
	
	/**
	 * Destinataires par defaut.
	 */
	public void setBcc(ArrayList<String> bcc) {
		this.bcc = bcc;
	}
	
	/**
	 * Mode de debug de JavaMail
	 */
	public void setDebug(boolean debug) {
		this.debug = debug;
		if (session != null) {
			session.setDebug(debug);
		}
	}
	
	public void setSession(Session session) {
		this.session = session;
	}
	
	/**
	 * Signature dans l'entete des mails pour une interception de filtres mail.
	 */
	public void setMail_header_listid(String mailHeaderListid) {
		mail_header_listid = mailHeaderListid;
	}
	
	/**
	 * Importe une configuration XML.
	 * @param mailconf element XML qui contient directement la conf
	 * @return La configuration.
	 */
	public static MailConfigurator importConfiguration(Configuration configuration) {
		if (configuration.isElementExists("javamail") == false) {
			throw new NullPointerException("No Javamail configuration in xmlconfig");
		}
		
		MailConfigurator mailconfigurator = new MailConfigurator();
		
		mailconfigurator.smtp_server = configuration.getValue("javamail", "server", "localhost");
		mailconfigurator.debug = configuration.getValueBoolean("javamail", "debug");
		
		mailconfigurator.from = configuration.getValue("javamail", "from", "localhost@localdomain");
		mailconfigurator.to = configuration.getValues("javamail", "to", "localhost@localdomain");
		mailconfigurator.cc = configuration.getValues("javamail", "cc", null);
		mailconfigurator.bcc = configuration.getValues("javamail", "bcc", null);
		
		if (mailconfigurator.smtp_server != null) {
			Properties prop = System.getProperties();
			prop.put("mail.smtp.host", mailconfigurator.smtp_server);
			mailconfigurator.session = Session.getDefaultInstance(prop);
		}
		
		mailconfigurator.setDebug(mailconfigurator.debug);
		return mailconfigurator;
		
	}
	
}
