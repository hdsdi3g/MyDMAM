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
 * Copyright (C) hdsdi3g for hd3g.tv 2012-2014
 * 
*/
package hd3gtv.mydmam.db;

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.MyDMAM;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.QuotedPrintableCodec;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.ddl.ColumnDefinition;
import com.netflix.astyanax.ddl.ColumnFamilyDefinition;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.serializers.StringSerializer;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

public class BackupDb {
	
	public static boolean mode_debug = false;
	
	private QuotedPrintableCodec qpc;
	
	public BackupDb() {
		qpc = new QuotedPrintableCodec("UTF-8");
	}
	
	public void backup(String basepath) throws Exception {
		Keyspace keyspace = CassandraDb.getkeyspace();
		List<ColumnFamilyDefinition> l_cdf = keyspace.describeKeyspace().getColumnFamilyList();
		for (int pos = 0; pos < l_cdf.size(); pos++) {
			backupCf(keyspace, l_cdf.get(pos), basepath);
		}
	}
	
	private void backupCf(Keyspace keyspace, ColumnFamilyDefinition cfd, String basepath) throws Exception {
		String cfname = cfd.getName();
		ColumnFamily<String, String> cf = new ColumnFamily<String, String>(cfname, StringSerializer.get(), StringSerializer.get());
		File outfile = new File(basepath + "_" + cfname + ".xml");
		
		Log2Dump dump = new Log2Dump();
		dump.add("name", cfname);
		dump.add("outfile", outfile);
		Log2.log.info("Start backup Column Family", dump);
		
		ExportCassandra exportcassandra = new ExportCassandra(keyspace, cfd, outfile);
		
		boolean result = CassandraDb.allRowsReader(cf, exportcassandra);
		
		if (result) {
			exportcassandra.closeDocument();
			Log2.log.info("End backup", new Log2Dump("key", exportcassandra.count));
		} else {
			Log2.log.error("End backup...", null);
		}
		
	}
	
	private class ExportCassandra implements AllRowsFoundRow {
		
		FileOutputStream fileoutputstream;
		ContentHandler content;
		ColumnList<String> columnlist;
		Column<String> column;
		int count = 0;
		
		public ExportCassandra(Keyspace keyspace, ColumnFamilyDefinition cfd, File outfile) throws Exception {
			String cfname = cfd.getName();
			
			/**
			 * Preparation
			 */
			fileoutputstream = new FileOutputStream(outfile);
			
			OutputFormat of = new OutputFormat();
			of.setMethod("xml");
			of.setEncoding("UTF-8");
			of.setVersion("1.0");
			of.setIndenting(mode_debug);
			if (mode_debug) {
				of.setIndent(2);
			}
			
			XMLSerializer serializer = new XMLSerializer(fileoutputstream, of);
			content = serializer.asContentHandler();
			content.startDocument();
			
			/**
			 * Headers
			 */
			AttributesImpl atts = new AttributesImpl();
			atts.addAttribute("", "", "keyspace", "CDATA", CassandraDb.default_keyspacename);
			atts.addAttribute("", "", "name", "CDATA", cfname);
			atts.addAttribute("", "", "created", "CDATA", String.valueOf(System.currentTimeMillis()));
			if (mode_debug) {
				atts.addAttribute("", "", "created_date", "CDATA", (new Date()).toString());
			}
			content.startElement("", "", "columnfamily", atts);
			
			/**
			 * Import description
			 */
			List<ColumnDefinition> l_cd = cfd.getColumnDefinitionList();
			for (int pos = 0; pos < l_cd.size(); pos++) {
				atts.clear();
				atts.addAttribute("", "", "name", "CDATA", l_cd.get(pos).getName());
				atts.addAttribute("", "", "indexname", "CDATA", l_cd.get(pos).getIndexName());
				atts.addAttribute("", "", "validationclass", "CDATA", l_cd.get(pos).getValidationClass());
				content.startElement("", "", "coldef", atts);
				content.endElement("", "", "coldef");
			}
		}
		
		public void closeDocument() throws Exception {
			content.endElement("", "", "columnfamily");
			content.endDocument();
			
			fileoutputstream.close();
		}
		
		public void onFoundRow(Row<String, String> row) throws Exception {
			AttributesImpl atts = new AttributesImpl();
			
			atts.addAttribute("", "", "name", "CDATA", row.getKey());
			content.startElement("", "", "key", atts);
			
			columnlist = row.getColumns();
			String columnvalue = null;
			for (int poscol = 0; poscol < columnlist.size(); poscol++) {
				column = columnlist.getColumnByIndex(poscol);
				atts.clear();
				atts.addAttribute("", "", "name", "CDATA", column.getName());
				atts.addAttribute("", "", "at", "CDATA", String.valueOf(column.getTimestamp() / 1000));
				atts.addAttribute("", "", "ttl", "CDATA", String.valueOf(column.getTtl()));
				
				columnvalue = new String(qpc.encode(column.getByteArrayValue()));
				
				if (mode_debug) {
					atts.addAttribute("", "", "at_date", "CDATA", (new Date(column.getTimestamp() / 1000)).toString());
					if (column.getStringValue().equals(columnvalue) == false) {
						atts.addAttribute("", "", "hex_value", "CDATA", MyDMAM.byteToString(column.getByteArrayValue()));
					}
				}
				
				content.startElement("", "", "col", atts);
				content.characters(columnvalue.toCharArray(), 0, columnvalue.length());
				content.endElement("", "", column.getName());
			}
			
			content.endElement("", "", "key");
			count++;
		}
		
	}
	
	public void restore(File xmlfile, String keyspacename, boolean purgebefore) throws Exception {
		
		ImportCassandra importcassandra = new ImportCassandra(keyspacename, purgebefore);
		
		SAXParserFactory fabrique = SAXParserFactory.newInstance();
		SAXParser parseur = fabrique.newSAXParser();
		
		try {
			fabrique.setFeature("http://xml.org/sax/features/external-general-entities", false);
		} catch (ParserConfigurationException pce) {
		}
		try {
			fabrique.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		} catch (ParserConfigurationException pce) {
		}
		
		InputStream fis = new BufferedInputStream(new FileInputStream(xmlfile), 8192);
		InputSource is = new InputSource(fis);
		is.setEncoding("UTF-8");
		parseur.parse(is, importcassandra);
		fis.close();
	}
	
	private class ImportCassandra extends DefaultHandler implements ErrorHandler {
		private boolean purgebefore;
		private Keyspace keyspace;
		
		public ImportCassandra(String keyspacename, boolean purgebefore) throws Exception {
			this.purgebefore = purgebefore;
			
			if (CassandraDb.isKeyspaceExists(keyspacename) == false) {
				CassandraDb.createKeyspace(keyspacename);
			}
			
			keyspace = CassandraDb.getkeyspace(keyspacename);
		}
		
		private int mutator_key_count;
		private int max_mutator_key_count = 1000;
		
		private ColumnFamily<String, String> columnfamily;;
		private String key_name;
		private StringBuffer rawtext;
		private MutationBatch mutator;
		private String col_name;
		private int col_ttl;
		
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			
			if (qName.equalsIgnoreCase("columnfamily")) {
				String cfname = attributes.getValue("name");
				
				Log2Dump dump = new Log2Dump();
				dump.add("keyspace", attributes.getValue("keyspace"));
				dump.add("name", cfname);
				dump.addDate("created", Long.parseLong(attributes.getValue("created")));
				Log2.log.info("Start import XML for restore Cassandra Column Family", dump);
				
				try {
					boolean iscfexists = CassandraDb.isColumnFamilyExists(keyspace, cfname);
					
					if (purgebefore & iscfexists) {
						keyspace.truncateColumnFamily(cfname);
					} else if (iscfexists == false) {
						CassandraDb.createColumnFamilyString(CassandraDb.getDefaultKeyspacename(), cfname, true);
					}
				} catch (ConnectionException e) {
					Log2.log.error("Prepare column family", e, dump);
					return;
				}
				mutator_key_count = 0;
				columnfamily = new ColumnFamily<String, String>(cfname, StringSerializer.get(), StringSerializer.get());
				
				return;
			}
			
			if (qName.equalsIgnoreCase("coldef")) {
				try {
					CassandraDb.declareIndexedColumn(keyspace, columnfamily, attributes.getValue("name"), attributes.getValue("indexname"), attributes.getValue("validationclass"));
				} catch (ConnectionException e) {
					throw new SAXException("Can't declare column", e);
				}
				return;
			}
			
			if (qName.equalsIgnoreCase("key")) {
				key_name = attributes.getValue("name");
				if (mutator == null) {
					try {
						mutator = CassandraDb.prepareMutationBatch();
					} catch (ConnectionException e) {
						throw new SAXException("Can't open access to CassandraDb", e);
					}
				}
				mutator_key_count++;
				return;
			}
			
			if (qName.equalsIgnoreCase("col")) {
				rawtext = new StringBuffer();
				col_name = attributes.getValue("name");
				col_ttl = Integer.parseInt(attributes.getValue("ttl"));
				return;
			}
			
			Log2Dump dump = new Log2Dump();
			dump.add("qName", qName);
			Log2.log.error("Unknow start qName", null, dump);
		}
		
		public void endElement(String uri, String localName, String qName) throws SAXException {
			try {
				if (qName.equalsIgnoreCase("columnfamily")) {
					if (mutator != null) {
						mutator.execute().getResult();
					}
					return;
				}
				if (qName.equalsIgnoreCase("coldef")) {
					return;
				}
				if (qName.equalsIgnoreCase("key")) {
					if (mutator_key_count > max_mutator_key_count) {
						mutator.execute().getResult();
						mutator = CassandraDb.prepareMutationBatch();
						mutator_key_count = 0;
					}
					return;
				}
				
				if (qName.equalsIgnoreCase("col")) {
					try {
						if (col_ttl > 0) {
							mutator.withRow(columnfamily, key_name).putColumn(col_name, qpc.decode(rawtext.toString().getBytes()), col_ttl);
						} else {
							mutator.withRow(columnfamily, key_name).putColumn(col_name, qpc.decode(rawtext.toString().getBytes()));
						}
					} catch (DecoderException e) {
						Log2.log.error("Bad column content decoding", e, new Log2Dump("raw content", rawtext.toString()));
						e.printStackTrace();
					}
					return;
				}
				
			} catch (ConnectionException e) {
				throw new SAXException("Can't access to CassandraDb", e);
			}
			Log2Dump dump = new Log2Dump();
			dump.add("qName", qName);
			Log2.log.error("Unknow end qName", null, dump);
		}
		
		public void error(SAXParseException e) throws SAXException {
			Log2.log.error("XML Parsing error", e);
		}
		
		public void fatalError(SAXParseException e) throws SAXException {
			Log2.log.error("XML Parsing error", e);
		}
		
		public void warning(SAXParseException e) throws SAXException {
			Log2.log.error("XML Parsing warning", e);
		}
		
		public void characters(char[] ch, int start, int length) throws SAXException {
			String read = new String(ch, start, length);
			if (read.trim().length() > 0) {
				rawtext.append(read.trim());
			}
		}
	}
	
}
