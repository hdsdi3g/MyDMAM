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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

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
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.ddl.ColumnFamilyDefinition;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.serializers.StringSerializer;

public class BackupDb {
	
	public static boolean mode_debug = false;
	
	private boolean cassandra;
	private boolean elasticsearch;
	
	public BackupDb(boolean cassandra, boolean elasticsearch) {
		this.cassandra = cassandra;
		this.elasticsearch = elasticsearch;
	}
	
	public void backup(String basepath) throws Exception {
		if (cassandra) {
			Keyspace keyspace = CassandraDb.getkeyspace();
			List<ColumnFamilyDefinition> l_cdf = keyspace.describeKeyspace().getColumnFamilyList();
			
			ColumnFamilyDefinition cfd;
			String cfname;
			ColumnFamily<String, String> cf;
			File outfile;
			Log2Dump dump;
			BackupDbCassandra exportcassandra;
			for (int pos = 0; pos < l_cdf.size(); pos++) {
				cfd = l_cdf.get(pos);
				cfname = cfd.getName();
				cf = new ColumnFamily<String, String>(cfname, StringSerializer.get(), StringSerializer.get());
				outfile = new File(basepath + "_cs_" + cfname + ".xml");
				
				dump = new Log2Dump();
				dump.add("name", cfname);
				dump.add("outfile", outfile);
				Log2.log.info("Start backup Cassandra Column Family", dump);
				
				exportcassandra = new BackupDbCassandra(keyspace, cfd, outfile);
				
				boolean result = CassandraDb.allRowsReader(cf, exportcassandra);
				
				if (result) {
					exportcassandra.closeDocument();
					Log2.log.info("End backup", new Log2Dump("keys", exportcassandra.getCount()));
				} else {
					Log2.log.error("End backup...", null);
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
			Log2Dump dump;
			BackupDbElasticsearch exportes;
			String index_name;
			for (int pos = 0; pos < index_names.size(); pos++) {
				index_name = index_names.get(pos);
				outfile = new File(basepath + "_es_" + index_name + ".xml");
				
				dump = new Log2Dump();
				dump.add("index", index_names.get(pos));
				dump.add("outfile", outfile);
				Log2.log.info("Start backup ElasticSearch Index", dump);
				
				GetSettingsResponse settings = admin_client.getSettings(new GetSettingsRequest().indices(index_name)).actionGet();
				GetMappingsResponse mapping = admin_client.getMappings(new GetMappingsRequest().indices(index_name)).actionGet();
				
				exportes = new BackupDbElasticsearch(outfile, index_name, mapping.getMappings(), settings.getIndexToSettings());
				
				crawler_reader = Elasticsearch.createCrawlerReader();
				crawler_reader.setIndices(index_name);
				crawler_reader.allReader(exportes);
				
				exportes.closeDocument();
				Log2.log.info("End backup", new Log2Dump("keys", exportes.getCount()));
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
	
	public void restore(File xmlfile, String keyspacename, boolean purgebefore) throws Exception {
		// TODO autodetect xml type
		if (cassandra) {
			parse(new BackupDbCassandra(keyspacename, purgebefore), xmlfile);
		}
		if (elasticsearch) {
			// TODO
			// BackupDbElasticsearch
		}
	}
	
}
