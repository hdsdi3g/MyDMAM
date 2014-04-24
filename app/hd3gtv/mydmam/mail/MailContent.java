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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

public class MailContent {
	
	private Message message;
	private MailCenter center;
	
	private Multipart multipart_content;
	private ArrayList<String> htmltext;
	private String plaintext;
	private String plaintext_content;
	private ArrayList<File> files;
	private ArrayList<File> pictures;
	private MailPriority priority;
	private InternetAddress[] cc_addr;
	private InternetAddress[] bcc_addr;
	
	MailContent(MailCenter center, Message message) {
		this.center = center;
		if (center == null) {
			throw new NullPointerException("\"center\" can't to be null");
		}
		
		this.message = message;
		if (message == null) {
			throw new NullPointerException("\"message\" can't to be null");
		}
	}
	
	public MailContent setHtmltext(ArrayList<String> htmltext) {
		this.htmltext = htmltext;
		return this;
	}
	
	public MailContent setHtmltext(String htmltext_inline) {
		htmltext = new ArrayList<String>(1);
		htmltext.add(htmltext_inline);
		return this;
	}
	
	public MailContent setFiles(ArrayList<File> files) {
		this.files = files;
		return this;
	}
	
	public MailContent setFiles(File... files) {
		this.files = new ArrayList<File>(files.length);
		for (int pos = 0; pos < files.length; pos++) {
			this.files.add(files[pos]);
		}
		return this;
	}
	
	public MailContent setPictures(ArrayList<File> pictures) {
		this.pictures = pictures;
		return this;
	}
	
	public MailContent setPictures(File... pictures) {
		this.pictures = new ArrayList<File>(pictures.length);
		for (int pos = 0; pos < pictures.length; pos++) {
			this.pictures.set(pos, pictures[pos]);
		}
		return this;
	}
	
	public MailContent setPlaintext(String plaintext) {
		this.plaintext = plaintext;
		return this;
	}
	
	public MailContent setCCAddr(InternetAddress... cc_addr) {
		if (cc_addr != null) {
			if (cc_addr.length > 0) {
				this.cc_addr = cc_addr;
			}
		}
		return this;
	}
	
	public MailContent setBCCAddr(InternetAddress... bcc_addr) {
		if (bcc_addr != null) {
			if (bcc_addr.length > 0) {
				this.bcc_addr = bcc_addr;
			}
		}
		return this;
	}
	
	public MailContent setMailPriority(MailPriority priority) {
		this.priority = priority;
		return this;
	}
	
	/*
	Case	TEXTE	HTML	IMAGE	PJ	Value
	0)	0		0		0		0	Exception
	1)	1		0		0		0	Texte
	2)	0		1		0		0	HTML
	3)	1		1		0		0	Alternative (Texte, HTML)
	4)	0		0		1		0	Exception
	5)	1		0		1		0	Related (Texte, image)
	6)	0		1		1		0	Related (HTML, image)
	7)	1		1		1		0	Related (Alternative (Texte, HTML), image)
	8)	0		0		0		1	Exception
	9)	1		0		0		1	Mixed (text, pj)
	10)	0		1		0		1	Mixed (HTML, pj)
	11)	1		1		0		1	Mixed (Alternative (Texte, HTML), pj)
	12)	0		0		1		1	Exception
	13)	1		0		1		1	Mixed (Related (Texte, image), pj)
	14)	0		1		1		1	Mixed (Related (HTML, image), pj)
	15)	1		1		1		1	Mixed (Related (Alternative (Texte, HTML), image), pj)
	 */
	
	private void process() throws MessagingException, IOException, NullPointerException {
		/**
		 * Case 0 4 8 12
		 */
		if ((plaintext == null) && (htmltext == null)) {
			throw new NullPointerException("No text content");
		}
		if ((pictures == null) && (files == null)) {
			/**
			 * Case 1 2 3
			 */
			if ((plaintext != null) && (htmltext == null)) {
				/**
				 * Case 1
				 */
				plaintext_content = plaintext;
				return;
			}
			if ((plaintext == null) && (htmltext != null)) {
				/**
				 * Case 2
				 */
				multipart_content = new MimeMultipart("related");
				multipart_content.addBodyPart(getHTMLBodyPart());
				return;
			}
			if ((plaintext != null) && (htmltext != null)) {
				/**
				 * Case 3
				 */
				multipart_content = getAlternativeContent();
				return;
			}
		}
		
		MimeMultipart relatedmessage = null;
		if (pictures != null) {
			/**
			 * Case 5 6 7 and 13 14 15
			 */
			relatedmessage = new MimeMultipart("related");
			
			if ((plaintext != null) && (htmltext == null)) {
				/**
				 * Case 5 and 13
				 */
				relatedmessage.addBodyPart(getTextBodyPart());
			}
			
			if ((plaintext == null) && (htmltext != null)) {
				/**
				 * Case 6 and 14
				 */
				relatedmessage.addBodyPart(getHTMLBodyPart());
			}
			
			if ((plaintext != null) && (htmltext != null)) {
				/**
				 * Case 7 and 15
				 */
				MimeBodyPart partalternated = new MimeBodyPart();
				partalternated.setContent(getAlternativeContent());
				relatedmessage.addBodyPart(partalternated);
			}
			
			for (int i = 0; i < pictures.size(); i++) {
				MimeBodyPart filepart = new MimeBodyPart();
				FileDataSource filedatasource = new FileDataSource(pictures.get(i));
				
				filepart.setFileName(filedatasource.getName());
				filepart.setText(filedatasource.getName());
				filepart.setDataHandler(new DataHandler(filedatasource));
				filepart.setHeader("Content-ID", "<" + pictures.get(i).getName() + ">");
				// filepart.setDisposition("attachement");
				filepart.setDisposition("inline");
				relatedmessage.addBodyPart(filepart);
			}
		}
		
		if (files != null) {
			/**
			 * Case 9 10 11 12 13 14 15
			 */
			
			MimeMultipart mixedmessage = new MimeMultipart("mixed");
			
			if (relatedmessage != null) {
				/**
				 * Case 13 14 15
				 */
				BodyPart relatedmessagepart = new MimeBodyPart();
				relatedmessagepart.setContent(relatedmessage);
				mixedmessage.addBodyPart(relatedmessagepart);
			} else {
				/**
				 * Case 9 10 11
				 */
				if ((plaintext != null) && (htmltext == null)) {
					/**
					 * Case 9
					 */
					mixedmessage.addBodyPart(getTextBodyPart());
				}
				
				if ((plaintext == null) && (htmltext != null)) {
					/**
					 * Case 10
					 */
					mixedmessage.addBodyPart(getHTMLBodyPart());
				}
				
				if ((plaintext != null) && (htmltext != null)) {
					/**
					 * Case 11
					 */
					MimeBodyPart partalternated = new MimeBodyPart();
					partalternated.setContent(getAlternativeContent());
					mixedmessage.addBodyPart(partalternated);
				}
			}
			
			for (int i = 0; i < files.size(); i++) {
				MimeBodyPart filepart = new MimeBodyPart();
				FileDataSource filedatasource = new FileDataSource(files.get(i));
				
				filepart.setFileName(filedatasource.getName());
				filepart.setText(filedatasource.getName());
				filepart.setDataHandler(new DataHandler(filedatasource));
				filepart.setDisposition("attachement");
				mixedmessage.addBodyPart(filepart);
			}
			
			multipart_content = mixedmessage;
		} else {
			multipart_content = relatedmessage;
		}
	}
	
	private BodyPart getHTMLBodyPart() throws MessagingException, IOException, NullPointerException {
		MimeBodyPart parthtmltext = new MimeBodyPart();
		
		if (htmltext == null) {
			return null;
		}
		
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < htmltext.size(); i++) {
			sb.append(htmltext.get(i));
			sb.append("\r\n");
		}
		parthtmltext.setDataHandler(new DataHandler(new ByteArrayDataSource(sb.toString(), "text/html")));
		return parthtmltext;
	}
	
	private BodyPart getTextBodyPart() throws MessagingException, NullPointerException {
		MimeBodyPart partplaintext = new MimeBodyPart();
		if (plaintext == null) {
			throw new NullPointerException("No plain text");
		}
		partplaintext.setText(plaintext);
		return partplaintext;
	}
	
	private MimeMultipart getAlternativeContent() throws NullPointerException, MessagingException, IOException {
		MimeMultipart alternative = new MimeMultipart("alternative");
		alternative.addBodyPart(getTextBodyPart());
		alternative.addBodyPart(getHTMLBodyPart());
		return alternative;
	}
	
	public void send() throws MessagingException, IOException, NullPointerException {
		process();
		
		if (plaintext_content != null) {
			message.setText(plaintext_content);
		} else if (multipart_content != null) {
			message.setContent(multipart_content);
		} else {
			throw new NullPointerException("No mail content");
		}
		
		if (priority != null) {
			priority.updateMessage(message);
		}
		if (cc_addr != null) {
			message.setRecipients(Message.RecipientType.CC, cc_addr);
		}
		if (bcc_addr != null) {
			message.setRecipients(Message.RecipientType.BCC, bcc_addr);
		}
		center.sendMessage(message);
	}
	
}
