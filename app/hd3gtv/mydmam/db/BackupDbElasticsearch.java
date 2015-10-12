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
package hd3gtv.mydmam.db;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.QuotedPrintableCodec;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.hppc.cursors.ObjectObjectCursor;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.search.SearchHit;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

import hd3gtv.log2.Log2Event;
import hd3gtv.mydmam.Loggers;

class BackupDbElasticsearch extends DefaultHandler implements ErrorHandler, ElastisearchCrawlerHit {
	
	private QuotedPrintableCodec quotedprintablecodec;
	private int count = 0;
	private FileOutputStream fileoutputstream;
	private ContentHandler content;
	private XMLSerializer serializer;
	private Gson gson;
	
	BackupDbElasticsearch(File outfile, String index_name, ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mapping, ImmutableOpenMap<String, Settings> settings) throws Exception {
		quotedprintablecodec = new QuotedPrintableCodec("UTF-8");
		GsonBuilder g_builder = new GsonBuilder();
		g_builder.disableHtmlEscaping();
		if (BackupDb.mode_debug) {
			g_builder.setPrettyPrinting();
		}
		gson = g_builder.create();
		
		/**
		 * Preparation
		 */
		fileoutputstream = new FileOutputStream(outfile);
		
		OutputFormat of = new OutputFormat();
		of.setMethod("xml");
		of.setEncoding("UTF-8");
		of.setVersion("1.0");
		of.setIndenting(BackupDb.mode_debug);
		if (BackupDb.mode_debug) {
			of.setIndent(2);
		}
		
		serializer = new XMLSerializer(fileoutputstream, of);
		content = serializer.asContentHandler();
		content.startDocument();
		
		/**
		 * Headers
		 */
		AttributesImpl atts = new AttributesImpl();
		atts.addAttribute("", "", "name", "CDATA", index_name);
		atts.addAttribute("", "", "created", "CDATA", String.valueOf(System.currentTimeMillis()));
		if (BackupDb.mode_debug) {
			atts.addAttribute("", "", "created_date", "CDATA", (new Date()).toString());
		}
		content.startElement("", "", "index", atts);
		
		/**
		 * Import configuration
		 * ES Mapping
		 */
		String jo_mapping;
		ImmutableOpenMap<String, MappingMetaData> mapping_value;
		for (ObjectObjectCursor<String, ImmutableOpenMap<String, MappingMetaData>> mapping_cursor : mapping) {
			mapping_value = mapping_cursor.value;
			for (ObjectObjectCursor<String, MappingMetaData> mapping_value_cursor : mapping_value) {
				atts.clear();
				atts.addAttribute("", "", "name", "CDATA", mapping_value_cursor.key);
				
				content.startElement("", "", "mapping", atts);
				
				jo_mapping = gson.toJson(mapping_value_cursor.value.getSourceAsMap());
				
				if (BackupDb.mode_debug) {
					serializer.comment(jo_mapping);
				}
				
				jo_mapping = new String(quotedprintablecodec.encode(jo_mapping));
				content.characters(jo_mapping.toCharArray(), 0, jo_mapping.length());
				
				content.endElement("", "", "mapping");
			}
		}
		
		/**
		 * ES settings
		 */
		for (ObjectObjectCursor<String, Settings> settings_cursor : settings) {
			atts.clear();
			content.startElement("", "", "settings", atts);
			
			jo_mapping = gson.toJson(settings_cursor.value.getAsMap());
			
			if (BackupDb.mode_debug) {
				serializer.comment(jo_mapping);
			}
			
			jo_mapping = new String(quotedprintablecodec.encode(jo_mapping));
			content.characters(jo_mapping.toCharArray(), 0, jo_mapping.length());
			
			content.endElement("", "", "settings");
		}
	}
	
	public boolean onFoundHit(SearchHit hit) {
		try {
			AttributesImpl atts = new AttributesImpl();
			
			atts.addAttribute("", "", "name", "CDATA", hit.getId());
			atts.addAttribute("", "", "type", "CDATA", hit.getType());
			atts.addAttribute("", "", "version", "CDATA", Long.toString(hit.getVersion()));
			
			content.startElement("", "", "key", atts);
			if (BackupDb.mode_debug) {
				Object source = gson.fromJson(hit.getSourceAsString(), Object.class);
				serializer.comment(gson.toJson(source));
			}
			
			String value = new String(quotedprintablecodec.encode(hit.getSourceAsString()));
			content.characters(value.toCharArray(), 0, value.length());
			
			content.endElement("", "", "key");
			
			count++;
			return true;
		} catch (Exception e) {
			Loggers.ElasticSearch.error("Can't write to XML document", e);
			return false;
		}
	}
	
	public int getCount() {
		return count;
	}
	
	public void closeDocument() throws Exception {
		content.endElement("", "", "index");
		content.endDocument();
		
		fileoutputstream.close();
	}
	
	private ElasticsearchBulkOperation bulk_op;
	private boolean purgebefore;
	private String index_name;
	private String mapping_name;
	private StringBuffer rawtext;
	private String key_name;
	private String type_name;
	private long ttl;
	
	BackupDbElasticsearch(boolean purgebefore, long ttl) throws Exception {
		this.purgebefore = purgebefore;
		this.ttl = ttl;
		quotedprintablecodec = new QuotedPrintableCodec("UTF-8");
	}
	
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		
		if (qName.equalsIgnoreCase("index")) {
			index_name = attributes.getValue("name");
			
			Loggers.ElasticSearch.info("Start import XML for restore ElasticSearch Index with name " + index_name + ", created " + Log2Event.dateLog(Long.parseLong(attributes.getValue("created"))));
			
			if (purgebefore) {
				Elasticsearch.deleteIndexRequest(index_name);
			}
			return;
		}
		
		if (qName.equalsIgnoreCase("mapping")) {
			mapping_name = attributes.getValue("name");
			rawtext = new StringBuffer();
			return;
		}
		
		if (qName.equalsIgnoreCase("settings")) {
			return;
		}
		
		if (qName.equalsIgnoreCase("key")) {
			rawtext = new StringBuffer();
			key_name = attributes.getValue("name");
			type_name = attributes.getValue("type");
			return;
		}
		
		Loggers.ElasticSearch.error("Unknow start qName " + qName);
	}
	
	public void endElement(String uri, String localName, String qName) throws SAXException {
		try {
			if (qName.equalsIgnoreCase("index")) {
				if (bulk_op == null) {
					return;
				}
				bulk_op.terminateBulk();
				return;
			}
			if (qName.equalsIgnoreCase("mapping")) {
				if (Elasticsearch.isIndexExists(index_name) == false) {
					Elasticsearch.createIndex(index_name);
				}
				Elasticsearch.addMappingToIndex(index_name, mapping_name, getContent());
				return;
			}
			if (qName.equalsIgnoreCase("settings")) {
				return;
			}
			
			// throw new SAXException("Can't declare mapping", e);
			
			if (qName.equalsIgnoreCase("key")) {
				if (bulk_op == null) {
					bulk_op = Elasticsearch.prepareBulk();
				}
				
				IndexRequestBuilder index = bulk_op.getClient().prepareIndex();
				index.setId(key_name);
				index.setIndex(index_name);
				index.setType(type_name);
				if (ttl > 0) {
					index.setTTL(ttl);
				}
				index.setSource(getContent());
				bulk_op.add(index);
				return;
			}
			
		} catch (DecoderException e) {
			throw new SAXException("Bad XML content decoding", e);
		}
		Loggers.ElasticSearch.error("Unknow end qName " + qName);
	}
	
	private String getContent() throws DecoderException {
		return new String(quotedprintablecodec.decode(rawtext.toString().getBytes()));
	}
	
	public void error(SAXParseException e) throws SAXException {
		Loggers.ElasticSearch.error("XML Parsing error", e);
	}
	
	public void fatalError(SAXParseException e) throws SAXException {
		Loggers.ElasticSearch.error("XML Parsing error", e);
	}
	
	public void warning(SAXParseException e) throws SAXException {
		Loggers.ElasticSearch.error("XML Parsing warning", e);
	}
	
	public void characters(char[] ch, int start, int length) throws SAXException {
		String read = new String(ch, start, length);
		if (read.trim().length() > 0) {
			rawtext.append(read.trim());
		}
	}
	
}
