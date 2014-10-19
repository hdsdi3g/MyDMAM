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

import hd3gtv.log2.Log2;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;

import org.apache.commons.codec.net.QuotedPrintableCodec;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.hppc.cursors.ObjectObjectCursor;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.search.SearchHit;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

class BackupDbElasticsearch implements ElastisearchCrawlerHit {
	
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
			Log2.log.error("Can't write to XML document", e);
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
	
	// TODO import
	// cf. http://stackoverflow.com/questions/22071198/adding-mapping-to-a-type-from-java-how-do-i-do-it
	
}
