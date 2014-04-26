/*
 * This file is part of MyDMAM
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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.mydmam.mail;

import groovy.lang.Writable;
import groovy.text.SimpleTemplateEngine;
import groovy.text.Template;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.codehaus.groovy.control.CompilationFailedException;

import play.Play;
import play.utils.OrderSafeProperties;
import play.vfs.VirtualFile;

/**
 * Front-end to Groovy engine, outside Play scope.
 * Ignore Jar files content, but manage Play modules and classpath added conf directory modules.
 */
public class MailTemplateEngine {
	
	public static final String BASE_PATH = "mail-templates";
	public static final String TEMPLATE_HTML_FILE = "template.html";
	public static final String TEMPLATE_TXT_FILE = "template.txt";
	public static final String TEMPLATE_SUBJECT_FILE = "subject.txt";
	
	public static final String BASE_TEMPLATE_MESSAGES = "messages";
	
	public static boolean GROOVY_VERBOSE = false;
	private static LinkedHashMap<String, File> templates;
	
	static {
		templates = new LinkedHashMap<String, File>();
		ArrayList<File> templates_dir_to_test = new ArrayList<File>();
		
		File templates_directory;
		
		/**
		 * Play modules
		 */
		for (Map.Entry<String, VirtualFile> entry : Play.modules.entrySet()) {
			File module_dir = entry.getValue().getRealFile();
			templates_directory = new File(module_dir.getAbsolutePath() + File.separator + "conf" + File.separator + BASE_PATH);
			
			if (templates_directory.exists() & templates_directory.isDirectory()) {
				File[] local_template_content = templates_directory.listFiles();
				for (int pos = 0; pos < local_template_content.length; pos++) {
					if (local_template_content[pos].isDirectory()) {
						templates_dir_to_test.add(local_template_content[pos]);
					}
				}
			}
		}
		
		/**
		 * Classpath modules
		 */
		String[] classpathelements = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
		
		for (int i = 0; i < classpathelements.length; i++) {
			if (classpathelements[i].endsWith(".jar")) {
				continue;
			}
			File directoryclass = new File(classpathelements[i]);
			if (directoryclass.exists() && directoryclass.isDirectory()) {
				templates_directory = new File(directoryclass.getAbsolutePath() + File.separator + BASE_PATH);
				
				if (templates_directory.exists() & templates_directory.isDirectory()) {
					File[] local_template_content = templates_directory.listFiles();
					for (int pos = 0; pos < local_template_content.length; pos++) {
						if (local_template_content[pos].isDirectory()) {
							templates_dir_to_test.add(local_template_content[pos]);
						}
					}
				}
			}
		}
		
		/**
		 * Test candidates validity
		 */
		for (int pos_tpl = 0; pos_tpl < templates_dir_to_test.size(); pos_tpl++) {
			try {
				String tpl_path = templates_dir_to_test.get(pos_tpl).getCanonicalPath();
				if (new File(tpl_path + File.separator + TEMPLATE_HTML_FILE).exists() == false) {
					throw new FileNotFoundException(tpl_path + File.separator + TEMPLATE_HTML_FILE);
				}
				if (new File(tpl_path + File.separator + TEMPLATE_TXT_FILE).exists() == false) {
					throw new FileNotFoundException(tpl_path + File.separator + TEMPLATE_TXT_FILE);
				}
				String key = templates_dir_to_test.get(pos_tpl).getName().toLowerCase();
				if (templates.containsKey(key) == false) {
					templates.put(key, templates_dir_to_test.get(pos_tpl));
				} else {
					Log2.log.error("Template directory is already added", null, new Log2Dump("path", templates_dir_to_test.get(pos_tpl)));
				}
			} catch (IOException e) {
				Log2.log.error("Can't use template directory: ", e, new Log2Dump("path", templates_dir_to_test.get(pos_tpl)));
			}
		}
		
	}
	
	private class TemplateWriter extends Writer {
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
	
	private Locale locale;
	private File template_directory;
	
	public MailTemplateEngine(String template_name) throws FileNotFoundException {
		if (template_name == null) {
			throw new NullPointerException("\"template_name\" can't to be null");
		}
		template_directory = templates.get(template_name.toLowerCase());
		if (template_directory == null) {
			throw new FileNotFoundException("Not template for this name: " + template_name.toLowerCase());
		}
		locale = Locale.getDefault();
	}
	
	public void setLocale(Locale locale) {
		if (locale != null) {
			this.locale = locale;
		}
	}
	
	private ArrayList<String> html_text;
	private String plain_text;
	private String subject;
	
	public void process(HashMap<Object, Object> variables) throws CompilationFailedException, ClassNotFoundException, IOException {
		SimpleTemplateEngine template_engine = new SimpleTemplateEngine();
		template_engine.setVerbose(GROOVY_VERBOSE);
		
		/**
		 * Prepare template variables and messages
		 */
		HashMap<Object, Object> all_variables = new HashMap<Object, Object>();
		
		File message_file = new File(template_directory.getAbsolutePath() + File.separator + BASE_TEMPLATE_MESSAGES + "." + locale.getLanguage());
		if (message_file.exists() == false) {
			Log2.log.error("No valid message file for " + locale.getLanguage() + " lang", null);
			message_file = new File(template_directory.getAbsolutePath() + File.separator + BASE_TEMPLATE_MESSAGES + "." + Locale.ENGLISH.getLanguage());
			if (message_file.exists() == false) {
				File[] all_files = template_directory.listFiles(new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return name.startsWith(BASE_TEMPLATE_MESSAGES);
					}
				});
				if (all_files.length == 0) {
					throw new ClassNotFoundException("No valid message file for template");
				}
				message_file = all_files[0];
			}
		}
		
		FileInputStream message_fis = new FileInputStream(message_file);
		Properties message = new OrderSafeProperties();
		message.load(message_fis);
		message_fis.close();
		
		all_variables.putAll(message);
		if (variables != null) {
			all_variables.putAll(variables);
		}
		
		/**
		 * Process templates
		 */
		html_text = process(template_engine.createTemplate(new File(template_directory.getAbsolutePath() + File.separator + TEMPLATE_HTML_FILE)), all_variables);
		ArrayList<String> plain_text_list = process(template_engine.createTemplate(new File(template_directory.getAbsolutePath() + File.separator + TEMPLATE_TXT_FILE)), all_variables);
		
		StringBuffer sb = new StringBuffer();
		for (int pos = 0; pos < plain_text_list.size(); pos++) {
			sb.append(plain_text_list.get(pos));
			sb.append("\r\n");
		}
		plain_text = sb.toString();
		
		ArrayList<String> subject_text_list = process(template_engine.createTemplate(new File(template_directory.getAbsolutePath() + File.separator + TEMPLATE_SUBJECT_FILE)), all_variables);
		sb = new StringBuffer();
		for (int pos = 0; pos < subject_text_list.size(); pos++) {
			sb.append(subject_text_list.get(pos));
			sb.append(" ");
		}
		subject = sb.toString().trim();
	}
	
	private ArrayList<String> process(Template template, HashMap<Object, Object> real_variables) throws IOException {
		Writable writable = template.make(real_variables);
		TemplateWriter tw = new TemplateWriter();
		writable.writeTo(tw);
		return tw.getContent();
	}
	
	public ArrayList<String> getHtml_text() {
		return html_text;
	}
	
	public String getPlain_text() {
		return plain_text;
	}
	
	public String getSubject() {
		return subject;
	}
	
}
