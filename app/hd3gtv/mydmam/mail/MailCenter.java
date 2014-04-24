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
 * Copyright (C) hdsdi3g for hd3g.tv 2008-2014
 * 
*/

package hd3gtv.mydmam.mail;

import hd3gtv.configuration.Configuration;
import hd3gtv.log2.Log2;

import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MailCenter {
	
	private static MailCenter global;
	
	static {
		global = new MailCenter();
		try {
			global.from = new InternetAddress(Configuration.global.getValue("javamail", "from", "root@localhost"));
		} catch (AddressException e) {
			Log2.log.error("Can't set form address", e);
			throw new NullPointerException("Invalid Javamail configuration");
		}
		global.port = Configuration.global.getValue("javamail", "port", 587);
		global.smtp_server = Configuration.global.getValue("javamail", "server", "localhost");
		global.debug = Configuration.global.getValueBoolean("javamail", "debug");
		global.auth = Configuration.global.getValueBoolean("javamail", "auth");
		global.starttls = Configuration.global.getValueBoolean("javamail", "starttls");
		global.username = Configuration.global.getValue("javamail", "username", "");
		global.password = Configuration.global.getValue("javamail", "password", "");
		global.prepareSession();
	}
	
	public static MailCenter getGlobal() {
		return global;
	}
	
	private InternetAddress from;
	private int port;
	private String smtp_server;
	private boolean debug;
	private boolean auth;
	private boolean starttls;
	private String username;
	private String password;
	
	private Session session;
	
	void prepareSession() {
		Properties props = new Properties();
		props.put("mail.smtp.host", smtp_server);
		props.put("mail.smtp.port", port);
		if (auth) {
			props.put("mail.smtp.auth", "true");
		}
		if (starttls) {
			props.put("mail.smtp.starttls.enable", "true");
		}
		// props.put("mail.smtp.socketFactory.port", "465");
		// props.put("mail.smtp.socketFactory.class", YesSSLSocketFactory.class.getName());
		// props.put("mail.smtp.socketFactory.fallback", "false");
		
		session = Session.getDefaultInstance(props);
		session.setDebug(debug);
	}
	
	public MailContent prepareMessage(String subject, InternetAddress... to_addr) throws MessagingException {
		if (subject == null) {
			throw new NullPointerException("\"subject\" can't to be null");
		}
		if (to_addr == null) {
			throw new NullPointerException("\"to_addr\" can't to be null");
		}
		try {
			Message message = new MimeMessage(session);
			message.setFrom(from);
			message.setRecipients(Message.RecipientType.TO, to_addr);
			message.setSubject(subject);
			MailContent result = new MailContent(this, message);
			// message.setHeader("List-ID", );
			return result;
		} catch (MessagingException e) {
			Log2.log.error("Can't prepare message", e);
		}
		return null;
	}
	
	void sendMessage(Message message) throws MessagingException {
		message.setHeader("X-Mailer", "MyDMAM");
		message.setSentDate(new Date());
		message.saveChanges();
		
		Transport transport = session.getTransport("smtp");
		if ((username.equals("") == false) & (password.equals("") == false)) {
			transport.connect(username, password);
		} else {
			transport.connect();
		}
		transport.sendMessage(message, message.getAllRecipients());
		transport.close();
	}
}
