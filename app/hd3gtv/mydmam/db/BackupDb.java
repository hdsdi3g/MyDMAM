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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.admin.indices.stats.CommonStats;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.common.collect.ImmutableMap;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.ddl.ColumnFamilyDefinition;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.serializers.StringSerializer;

import hd3gtv.mydmam.Loggers;
import hd3gtv.tools.CopyMove;

public class BackupDb {
	
	public static boolean mode_debug = false;
	
	public BackupDb() {
	}
	
	public void backup(String basepath, boolean cassandra, boolean elasticsearch) throws Exception {
		if ((cassandra == false) & (elasticsearch == false)) {
			cassandra = true;
			elasticsearch = true;
		}
		
		if (cassandra) {
			Keyspace keyspace = CassandraDb.getkeyspace();
			List<ColumnFamilyDefinition> l_cdf = keyspace.describeKeyspace().getColumnFamilyList();
			
			ColumnFamilyDefinition cfd;
			String cfname;
			ColumnFamily<String, String> cf;
			File outfile;
			BackupDbCassandra exportcassandra;
			for (int pos = 0; pos < l_cdf.size(); pos++) {
				cfd = l_cdf.get(pos);
				cfname = cfd.getName();
				cf = new ColumnFamily<String, String>(cfname, StringSerializer.get(), StringSerializer.get());
				outfile = new File(basepath + "_cs_" + cfname + ".xml");
				
				Loggers.Cassandra.info("Start backup Cassandra Column Family, name: " + cfname + ", outfile: " + outfile);
				
				exportcassandra = new BackupDbCassandra(keyspace, cfd, outfile);
				
				boolean result = CassandraDb.allRowsReader(cf, exportcassandra);
				
				if (result) {
					exportcassandra.closeDocument();
					Loggers.Cassandra.info("End backup, keys" + exportcassandra.getCount());
				} else {
					Loggers.Cassandra.error("End backup...");
				}
			}
		}
		if (elasticsearch) {
			Client client = Elasticsearch.getClient();
			IndicesAdminClient admin_client = client.admin().indices();
			ImmutableMap<ShardRouting, CommonStats> stats = admin_client.stats(new IndicesStatsRequest()).actionGet().asMap();
			ShardRouting shard;
			ArrayList<String> index_names = new ArrayList<String>();
			for (Map.Entry<ShardRouting, CommonStats> entry_stats : stats.entrySet()) {
				shard = entry_stats.getKey();
				if (shard.active() & shard.started() & shard.primary() & (shard.getId() == 0)) {
					index_names.add(shard.getIndex());
				}
			}
			
			ElastisearchCrawlerReader crawler_reader;
			File outfile;
			BackupDbElasticsearch exportes;
			String index_name;
			for (int pos = 0; pos < index_names.size(); pos++) {
				index_name = index_names.get(pos);
				outfile = new File(basepath + "_es_" + index_name + ".xml");
				
				Loggers.ElasticSearch.info("Start backup ElasticSearch Index with index: " + index_names.get(pos) + " and outfile: " + outfile);
				
				GetSettingsResponse settings = admin_client.getSettings(new GetSettingsRequest().indices(index_name)).actionGet();
				GetMappingsResponse mapping = admin_client.getMappings(new GetMappingsRequest().indices(index_name)).actionGet();
				
				exportes = new BackupDbElasticsearch(outfile, index_name, mapping.getMappings(), settings.getIndexToSettings());
				
				crawler_reader = Elasticsearch.createCrawlerReader();
				crawler_reader.setIndices(index_name);
				crawler_reader.allReader(exportes);
				
				exportes.closeDocument();
				Loggers.ElasticSearch.info("End backup with " + exportes.getCount() + " keys");
			}
		}
	}
	
	private void parse(DefaultHandler handler, File xmlfile) throws Exception {
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
		parseur.parse(is, handler);
		fis.close();
	}
	
	private class XmlTypeDetection extends DefaultHandler {
		String first_element = null;
		
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			first_element = qName;
			throw new SAXException("STOP");
		}
		
	}
	
	public void restore(File xmlfile, String keyspacename, boolean purgebefore, long esttl) throws Exception {
		XmlTypeDetection type_detect = new XmlTypeDetection();
		try {
			parse(type_detect, xmlfile);
		} catch (SAXException e) {
			if (e.getMessage().equalsIgnoreCase("STOP") == false) {
				throw e;
			}
		}
		
		if (type_detect.first_element == null) {
			throw new SAXException("Can't found XML root element");
		}
		
		if (type_detect.first_element.equalsIgnoreCase("columnfamily")) {
			parse(new BackupDbCassandra(keyspacename, purgebefore), xmlfile);
		} else if (type_detect.first_element.equalsIgnoreCase("index")) {
			parse(new BackupDbElasticsearch(purgebefore, esttl), xmlfile);
		} else {
			throw new SAXException("Can't detect XML root element parser type");
		}
	}
	
	public void backupCF(String basepath, String cf_name) {
		try {
			File file_basepath = new File(basepath);
			if (file_basepath.exists()) {
				CopyMove.checkExistsCanRead(file_basepath);
				CopyMove.checkIsDirectory(file_basepath);
				CopyMove.checkIsWritable(file_basepath);
			} else {
				FileUtils.forceMkdir(file_basepath);
			}
			
			Keyspace keyspace = CassandraDb.getkeyspace();
			List<ColumnFamilyDefinition> l_cdf = keyspace.describeKeyspace().getColumnFamilyList();
			
			ColumnFamilyDefinition cfd = null;
			for (int pos = 0; pos < l_cdf.size(); pos++) {
				cfd = l_cdf.get(pos);
				if (cf_name.equals(cfd.getName())) {
					break;
				}
			}
			if (cfd == null) {
				Loggers.Cassandra.debug("Backup CF " + cf_name + " canceled: CF don't exists");
				return;
			}
			
			ColumnFamily<String, String> cf = new ColumnFamily<String, String>(cf_name, StringSerializer.get(), StringSerializer.get());
			File outfile = new File(file_basepath.getAbsolutePath() + File.separator + "backup_cs_" + cf_name + ".xml");
			Loggers.Cassandra.info("Start backup Cassandra Column Family, name: " + cf_name + ", outfile: " + outfile);
			BackupDbCassandra exportcassandra = new BackupDbCassandra(keyspace, cfd, outfile);
			
			boolean result = CassandraDb.allRowsReader(cf, exportcassandra);
			exportcassandra.closeDocument();
			
			if (result == false) {
				Loggers.Cassandra.error("Backup failed...");
				return;
			}
			
			Loggers.Cassandra.debug("Start compress backup CF file: " + outfile.getPath());
			GZIPOutputStream gzos = null;
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(outfile);
				gzos = new GZIPOutputStream(
						new FileOutputStream(new File(file_basepath.getAbsolutePath() + File.separator + "backup_cs_" + cf_name + "_" + Loggers.dateFilename(System.currentTimeMillis()) + ".xml.gz")));
				IOUtils.copy(fis, gzos);
				fis.close();
				gzos.close();
				FileUtils.forceDelete(outfile);
			} catch (IOException e) {
				Loggers.Cassandra.error("Can't compress backup file during backup", e);
			} finally {
				IOUtils.closeQuietly(fis);
				IOUtils.closeQuietly(gzos);
			}
			
			Loggers.Cassandra.debug("End backup, keys " + exportcassandra.getCount());
		} catch (Exception ce) {
			Loggers.Cassandra.error("Can't backup Cassandra CF " + cf_name + " in " + basepath);
		}
	}
	
}
