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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.mydmam.mail;

import groovy.lang.Writable;
import groovy.text.SimpleTemplateEngine;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;
import hd3gtv.mydmam.module.MessagesOutsidePlay;
import hd3gtv.mydmam.module.MyDMAMModulesManager;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.mail.internet.InternetAddress;

import org.codehaus.groovy.control.CompilationFailedException;

/**
 * With Groovy engine front-end (outside Play scope). Ignore Jar files content, but manage Play modules.
 */
public class EndUserBaseMail implements Log2Dumpable {
	
	private static MailCenter mailcenter;
	private static LinkedHashMap<String, File> templates;
	
	private static final String TEMPLATE_BASE_PATH = "mail-templates";
	private static final String TEMPLATE_HTML_FILE = "template.html";
	private static final String TEMPLATE_SUBJECT_FILE = "subject.txt";
	
	public static boolean GROOVY_VERBOSE = false;
	private static SimpleTemplateEngine template_engine;
	
	static {
		mailcenter = MailCenter.getGlobal();
		templates = new LinkedHashMap<String, File>();
		template_engine = new SimpleTemplateEngine();
		template_engine.setVerbose(GROOVY_VERBOSE);
		
		LinkedHashMap<String, File> conf_dirs = MyDMAMModulesManager.getAllConfDirectories();
		File templates_directories;
		File[] templates_directories_content;
		File template_directory;
		String module_name;
		String template_name;
		String tpl_path;
		Log2Dump new_templates = new Log2Dump();
		for (Map.Entry<String, File> conf_dir_entry : conf_dirs.entrySet()) {
			module_name = conf_dir_entry.getKey();
			templates_directories = new File(conf_dir_entry.getValue() + File.separator + TEMPLATE_BASE_PATH);
			if (templates_directories.exists() == false) {
				continue;
			}
			if (templates_directories.isDirectory() == false) {
				continue;
			}
			
			/**
			 * Test validity
			 */
			templates_directories_content = templates_directories.listFiles();
			for (int pos_tpl = 0; pos_tpl < templates_directories_content.length; pos_tpl++) {
				template_directory = templates_directories_content[pos_tpl];
				try {
					if (template_directory.isDirectory() == false) {
						continue;
					}
					tpl_path = template_directory.getCanonicalPath();
					if (new File(tpl_path + File.separator + TEMPLATE_HTML_FILE).exists() == false) {
						throw new FileNotFoundException(tpl_path + File.separator + TEMPLATE_HTML_FILE);
					}
					tpl_path = template_directory.getCanonicalPath();
					if (new File(tpl_path + File.separator + TEMPLATE_SUBJECT_FILE).exists() == false) {
						throw new FileNotFoundException(tpl_path + File.separator + TEMPLATE_SUBJECT_FILE);
					}
					
					template_name = template_directory.getName();
					if (templates.containsKey(template_name) == false) {
						templates.put(template_name, template_directory);
						new_templates.add("template", "\"" + template_name + "\" from \"" + module_name + "\" module in \"" + template_directory.getAbsolutePath() + "\"");
					} else {
						throw new Exception("Template directory with the name \"" + template_name + "\" is already added.");
					}
				} catch (Exception e) {
					Log2Dump dump = new Log2Dump();
					dump.add("path", template_directory);
					dump.add("module", module_name);
					Log2.log.error("Can't use/import template directory: ", e, dump);
				}
			}
			
		}
		
		Log2.log.debug("Import mail templates", new_templates);
	}
	
	private Locale locale;
	private InternetAddress to;
	private MailPriority priority;
	
	private File template_directory;
	
	public EndUserBaseMail(Locale locale, InternetAddress to, String mail_template_name) throws FileNotFoundException {
		this.locale = locale;
		if (locale == null) {
			throw new NullPointerException("\"locale\" can't to be null");
		}
		
		this.to = to;
		if (to == null) {
			throw new NullPointerException("\"to\" can't to be null");
		}
		
		if (mail_template_name == null) {
			throw new NullPointerException("\"mail_template\" can't to be null");
		}
		if (templates.containsKey(mail_template_name.toLowerCase()) == false) {
			throw new FileNotFoundException("Not template for this name: " + mail_template_name.toLowerCase());
		}
		this.template_directory = templates.get(mail_template_name.toLowerCase());
	}
	
	public EndUserBaseMail setMailPriority(MailPriority priority) {
		this.priority = priority;
		return this;
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("to", to);
		dump.add("locale", locale);
		dump.add("template_directory", template_directory);
		return dump;
	}
	
	public void send() {
		send(new HashMap<String, Object>());
	}
	
	/**
	 * @param mail_vars Beware ! "message" key is reserved, and it used with messages.* files.
	 */
	public void send(HashMap<String, Object> mail_vars) {
		if (mail_vars == null) {
			throw new NullPointerException("\"mail_vars\" can't to be null");
		}
		try {
			/**
			 * Prepare template variables and messages
			 */
			HashMap<String, Object> all_mail_vars = new HashMap<String, Object>();
			all_mail_vars.putAll(mail_vars);
			all_mail_vars.put("messages", new MessagesOutsidePlay(locale));
			
			/**
			 * Process templates: subject
			 */
			ArrayList<String> subject_text_list = process(new File(template_directory.getPath() + File.separator + TEMPLATE_SUBJECT_FILE), all_mail_vars);
			StringBuffer sb = new StringBuffer();
			for (int pos = 0; pos < subject_text_list.size(); pos++) {
				sb.append(subject_text_list.get(pos));
				sb.append(" ");
			}
			MailContent mail = mailcenter.prepareMessage(sb.toString().trim(), to);
			
			/**
			 * Process templates: html text
			 */
			mail.setHtmltext(process(new File(template_directory.getPath() + File.separator + TEMPLATE_HTML_FILE), all_mail_vars));
			
			mail.setMailPriority(priority);
			mail.send();
			
			Log2Dump dump = getLog2Dump();
			for (Entry<String, Object> entry : mail_vars.entrySet()) {
				dump.add("mail_vars: " + (String) entry.getKey(), entry.getValue());
			}
			Log2.log.info("Send an user mail", dump);
		} catch (Exception e) {
			Log2.log.error("Fail to send an user mail", e, this);
		}
	}
	
	private static class TemplateWriter extends Writer {
		StringBuffer raw;
		
		public TemplateWriter() {
			raw = new StringBuffer();
		}
		
		public void write(char[] cbuf, int off, int len) throws IOException {
			raw.append(cbuf, off, len);
		}
		
		public void flush() throws IOException {
		}
		
		public void close() throws IOException {
		}
		
		public ArrayList<String> getContent() {
			ArrayList<String> content = new ArrayList<String>();
			ByteArrayInputStream baos = new ByteArrayInputStream(raw.toString().getBytes());
			BufferedReader reader = new BufferedReader(new InputStreamReader(baos), 0xFFF);
			String line;
			try {
				while (((line = reader.readLine()) != null)) {
					content.add(line.trim());
				}
			} catch (IOException e) {
				Log2.log.error("Can't convert text stream", e);
				return null;
			}
			if (content.size() > 0) {
				if (content.get(0).trim().equals("")) {
					content.remove(0);
				}
			}
			return content;
		}
		
	};
	
	private static ArrayList<String> process(File template_file, HashMap<String, Object> all_mail_vars) throws IOException, CompilationFailedException, ClassNotFoundException {
		Writable writable = template_engine.createTemplate(template_file).make(all_mail_vars);
		TemplateWriter tw = new TemplateWriter();
		writable.writeTo(tw);
		return tw.getContent();
	}
}
