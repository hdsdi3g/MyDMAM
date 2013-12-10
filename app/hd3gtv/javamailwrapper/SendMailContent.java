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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

@SuppressWarnings("nls")
public class SendMailContent {
	
	protected String subject;
	protected Multipart messagecontent;
	protected String textcontent;
	protected ArrayList<String> htmltext;
	protected String plaintext;
	protected ArrayList<File> files;
	protected ArrayList<File> pictures;
	
	public SendMailContent() {
		textcontent = null;
		messagecontent = null;
		htmltext = null;
		files = null;
		pictures = null;
		plaintext = null;
	}
	
	public void setSubject(String subject) {
		this.subject = subject;
	}
	
	String getSubject() {
		return subject;
	}
	
	Multipart getMPContent() {
		return messagecontent;
	}
	
	String getTextContent() {
		return textcontent;
	}
	
	/**
	 * @param htmltext Contenu HTML, ligne a ligne
	 */
	public void setHtmltext(ArrayList<String> htmltext) {
		this.htmltext = htmltext;
	}
	
	/**
	 * @param files Fichiers a envoyer en piece jointe, pas de verification de taille
	 */
	public void setFiles(ArrayList<File> files) {
		this.files = files;
	}
	
	/**
	 * @param files Images a associer au message texte, pas de verification de taille
	 */
	public void setPictures(ArrayList<File> pictures) {
		this.pictures = pictures;
	}
	
	/**
	 * @param htmltext Contenu texte pure
	 */
	public void setPlaintext(String plaintext) {
		this.plaintext = plaintext;
	}
	
	/*
	CAS	TEXTE	HTML	IMAGE	PJ	Value
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
	
	void process() throws MessagingException, IOException, NullPointerException {
		
		/**
		 * CAS 0 4 8 12
		 */
		if ((plaintext == null) && (htmltext == null)) {
			throw new NullPointerException("No text content");
		}
		/* ******************** */
		if ((pictures == null) && (files == null)) {
			/**
			 * CAS 1 2 3
			 */
			if ((plaintext != null) && (htmltext == null)) {
				/**
				 * CAS 1
				 */
				textcontent = plaintext;
				return;
			}
			if ((plaintext == null) && (htmltext != null)) {
				/**
				 * CAS 2
				 */
				messagecontent = new MimeMultipart("related");
				messagecontent.addBodyPart(getHTMLBodyPart());
				return;
			}
			if ((plaintext != null) && (htmltext != null)) {
				/**
				 * CAS 3
				 */
				messagecontent = getAlternativeContent();
				return;
			}
		}
		
		/* ******************** */
		MimeMultipart relatedmessage = null;
		if (pictures != null) {
			/**
			 * CAS 5 6 7 et 13 14 15
			 */
			relatedmessage = new MimeMultipart("related");
			
			if ((plaintext != null) && (htmltext == null)) {
				/**
				 * CAS 5 et 13
				 */
				relatedmessage.addBodyPart(getTextBodyPart());
			}
			
			if ((plaintext == null) && (htmltext != null)) {
				/**
				 * CAS 6 et 14
				 */
				relatedmessage.addBodyPart(getHTMLBodyPart());
			}
			
			if ((plaintext != null) && (htmltext != null)) {
				/**
				 * CAS 7 et 15
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
			 * CAS 9 10 11 12 13 14 15
			 */
			
			MimeMultipart mixedmessage = new MimeMultipart("mixed");
			
			if (relatedmessage != null) {
				/**
				 * CAS 13 14 15
				 */
				BodyPart relatedmessagepart = new MimeBodyPart();
				relatedmessagepart.setContent(relatedmessage);
				mixedmessage.addBodyPart(relatedmessagepart);
			} else {
				/**
				 * CAS 9 10 11
				 */
				if ((plaintext != null) && (htmltext == null)) {
					/**
					 * CAS 9
					 */
					mixedmessage.addBodyPart(getTextBodyPart());
				}
				
				if ((plaintext == null) && (htmltext != null)) {
					/**
					 * CAS 10
					 */
					mixedmessage.addBodyPart(getHTMLBodyPart());
				}
				
				if ((plaintext != null) && (htmltext != null)) {
					/**
					 * CAS 11
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
			
			messagecontent = mixedmessage;
			return;
		} else {
			messagecontent = relatedmessage;
			return;
		}
		
	}
	
	protected BodyPart getHTMLBodyPart() throws MessagingException, IOException, NullPointerException {
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
	
	protected BodyPart getTextBodyPart() throws MessagingException, NullPointerException {
		MimeBodyPart partplaintext = new MimeBodyPart();
		if (plaintext == null) {
			throw new NullPointerException("No plain text");
		}
		partplaintext.setText(plaintext);
		return partplaintext;
	}
	
	protected MimeMultipart getAlternativeContent() throws NullPointerException, MessagingException, IOException {
		MimeMultipart alternative = new MimeMultipart("alternative");
		alternative.addBodyPart(getTextBodyPart());
		alternative.addBodyPart(getHTMLBodyPart());
		return alternative;
	}
	
}
