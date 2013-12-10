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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

@SuppressWarnings("nls")
public class SendMail {
	
	private MailConfigurator mailconfigurator;
	
	private String from;
	private ArrayList<String> to;
	private ArrayList<String> cc;
	private ArrayList<String> bcc;
	private Priority priority;
	
	public enum Priority {
		Highest, High, Normal, Low, Lowest;
	}
	
	/**
	 * @param mailconfigurator La configuration generique
	 */
	public SendMail(MailConfigurator mailconfigurator) {
		this.mailconfigurator = mailconfigurator;
		priority = Priority.Normal;
	}
	
	public ArrayList<String> getBcc() {
		return bcc;
	}
	
	public ArrayList<String> getCc() {
		return cc;
	}
	
	public String getFrom() {
		return from;
	}
	
	public ArrayList<String> getTo() {
		return to;
	}
	
	/**
	 * @param from Expediteur tel que "me@domain.tdl" ou "Me <me@domain.tdl>"
	 */
	public void setFrom(String from) {
		this.from = from;
	}
	
	/**
	 * @param to Destinataire tel que "you@domain.tdl" ou "You <you@domain.tdl>"
	 */
	public void addTo(String to) {
		if (this.to == null) {
			this.to = new ArrayList<String>();
		}
		this.to.add(to);
	}
	
	/**
	 * @param cc Destinataire tel que "you@domain.tdl" ou "You <you@domain.tdl>"
	 */
	public void addCc(String cc) {
		if (this.cc == null) {
			this.cc = new ArrayList<String>();
		}
		this.cc.add(cc);
	}
	
	/**
	 * @param bcc Destinataire tel que "you@domain.tdl" ou "You <you@domain.tdl>"
	 */
	public void addBcc(String bcc) {
		if (this.bcc == null) {
			this.bcc = new ArrayList<String>();
		}
		this.bcc.add(bcc);
	}
	
	/**
	 * @param to Destinataire tel que "you@domain.tdl" ou "You <you@domain.tdl>"
	 */
	public void setTo(String to) {
		this.to = new ArrayList<String>();
		this.to.add(to);
	}
	
	/**
	 * @param cc Destinataire tel que "you@domain.tdl" ou "You <you@domain.tdl>"
	 */
	public void setCC(String cc) {
		this.cc = new ArrayList<String>();
		this.cc.add(cc);
	}
	
	/**
	 * @param bcc Destinataire tel que "you@domain.tdl" ou "You <you@domain.tdl>"
	 */
	public void setBCC(String bcc) {
		this.bcc = new ArrayList<String>();
		this.bcc.add(bcc);
	}
	
	/**
	 * @param to Destinataires tel que "you@domain.tdl" ou "You <you@domain.tdl>"
	 */
	public void setTo(ArrayList<String> to) {
		this.to = to;
	}
	
	/**
	 * @param cc Destinataires tel que "you@domain.tdl" ou "You <you@domain.tdl>"
	 */
	public void setCc(ArrayList<String> cc) {
		this.cc = cc;
	}
	
	/**
	 * @param bcc Destinataires tel que "you@domain.tdl" ou "You <you@domain.tdl>"
	 */
	public void setBcc(ArrayList<String> bcc) {
		this.bcc = bcc;
	}
	
	public void setPriority(Priority priority) {
		this.priority = priority;
	}
	
	/**
	 * Envoi le mail
	 * @param mailcontent Contenu du mail
	 * @throws AddressException Mauvais format d'adresse mail
	 * @throws MessagingException Erreur d'envoi ou de fabrication du mail
	 * @throws NullPointerException Champ vide
	 * @throws IOException Acces de fichier
	 */
	public void send(SendMailContent mailcontent) throws MailSendException {
		
		try {
			
			/**
			 * Message
			 */
			Message message = new MimeMessage(mailconfigurator.session);
			
			if (mailconfigurator.mail_header_listid != null) {
				message.setHeader("List-ID", mailconfigurator.mail_header_listid);
			}
			
			if (priority == Priority.Highest) {
				message.setHeader("X-PRIORITY", "1 (Highest)");
				message.setHeader("Priority", "urgent");
			}
			if (priority == Priority.High) {
				message.setHeader("X-PRIORITY", "2 (High)");
				message.setHeader("Priority", "urgent");
			}
			if (priority == Priority.Low) {
				message.setHeader("X-PRIORITY", "4 (Low)");
				message.setHeader("Priority", "non-urgent");
			}
			if (priority == Priority.Lowest) {
				message.setHeader("X-PRIORITY", "5 (Lowest)");
				message.setHeader("Priority", "non-urgent");
			}
			
			/**
			 * FROM
			 */
			if (from != null) {
				message.setFrom(new InternetAddress(from));
			} else {
				if (mailconfigurator.from != null) {
					message.setFrom(new InternetAddress(mailconfigurator.from));
				} else {
					throw new NullPointerException("No from address mail");
				}
			}
			
			/**
			 * TO
			 */
			InternetAddress[] internetaddressesTO = null;
			if (to != null) {
				internetaddressesTO = new InternetAddress[to.size()];
				for (int i = 0; i < to.size(); i++) {
					internetaddressesTO[i] = new InternetAddress(to.get(i));
				}
				message.setRecipients(Message.RecipientType.TO, internetaddressesTO);
			} else {
				if (mailconfigurator.to != null) {
					internetaddressesTO = new InternetAddress[mailconfigurator.to.size()];
					for (int i = 0; i < mailconfigurator.to.size(); i++) {
						internetaddressesTO[i] = new InternetAddress(mailconfigurator.to.get(i));
					}
					message.setRecipients(Message.RecipientType.TO, internetaddressesTO);
					to = mailconfigurator.to;
				} else {
					throw new NullPointerException("No to address mail");
				}
			}
			
			/**
			 * CC
			 */
			InternetAddress[] internetaddressesCC = null;
			if (cc != null) {
				internetaddressesCC = new InternetAddress[cc.size()];
				for (int i = 0; i < cc.size(); i++) {
					internetaddressesCC[i] = new InternetAddress(cc.get(i));
				}
				message.setRecipients(Message.RecipientType.CC, internetaddressesCC);
			} else {
				if (mailconfigurator.cc != null) {
					internetaddressesCC = new InternetAddress[mailconfigurator.cc.size()];
					for (int i = 0; i < mailconfigurator.cc.size(); i++) {
						internetaddressesCC[i] = new InternetAddress(mailconfigurator.cc.get(i));
					}
					message.setRecipients(Message.RecipientType.CC, internetaddressesCC);
					cc = mailconfigurator.cc;
				}
			}
			
			/**
			 * BCC
			 */
			InternetAddress[] internetaddressesBCC = null;
			if (bcc != null) {
				internetaddressesBCC = new InternetAddress[bcc.size()];
				for (int i = 0; i < bcc.size(); i++) {
					internetaddressesBCC[i] = new InternetAddress(bcc.get(i));
				}
				message.setRecipients(Message.RecipientType.BCC, internetaddressesBCC);
			} else {
				if (mailconfigurator.bcc != null) {
					internetaddressesBCC = new InternetAddress[mailconfigurator.bcc.size()];
					for (int i = 0; i < mailconfigurator.bcc.size(); i++) {
						internetaddressesBCC[i] = new InternetAddress(mailconfigurator.bcc.get(i));
					}
					message.setRecipients(Message.RecipientType.BCC, internetaddressesBCC);
					bcc = mailconfigurator.bcc;
				}
			}
			
			message.setHeader("X-Mailer", "JavaMail");
			message.setSentDate(new Date());
			
			mailcontent.process();
			
			if (mailcontent.getSubject() != null) {
				message.setSubject(mailcontent.getSubject());
			} else {
				throw new NullPointerException("No mail subject");
			}
			
			if (mailcontent.getTextContent() != null) {
				message.setText(mailcontent.getTextContent());
			} else {
				if (mailcontent.getMPContent() != null) {
					message.setContent(mailcontent.getMPContent());
				} else {
					throw new NullPointerException("No mail content");
				}
			}
			
			Transport.send(message);
			
		} catch (AddressException e) {
			throw new MailSendException("Mail adresses is not valid", e);
		} catch (NullPointerException e) {
			throw new MailSendException("Empty value required.", e);
		} catch (MessagingException e) {
			throw new MailSendException("Can't send this mail.", e);
		} catch (IOException e) {
			throw new MailSendException("Network error", e);
		} catch (Exception e) {
			throw new MailSendException(e);
		}
		
	}
	
}
