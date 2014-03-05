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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam.pathindexing;

import hd3gtv.javasimpleservice.ServiceMessageError;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.db.Elasticsearch;

import java.io.IOException;
import java.util.ArrayList;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.json.simple.parser.ParseException;

public abstract class Importer {
	
	public static final String ES_INDEX = "pathindex";
	public static final String ES_TYPE_FILE = "file";
	public static final String ES_TYPE_DIRECTORY = "directory";
	
	private Client client;
	private int window_update_size;
	
	/**
	 * Test if GC need to be activated on ES
	 */
	public Importer(Client client) throws IOException, ParseException {
		this.client = client;
		if (client == null) {
			throw new NullPointerException("\"client\" can't to be null");
		}
		window_update_size = 10000;
		
		Elasticsearch.enableTTL(client, ES_INDEX, ES_TYPE_FILE);
		Elasticsearch.enableTTL(client, ES_INDEX, ES_TYPE_DIRECTORY);
	}
	
	/**
	 * Don"t forget to add root path (Storage).
	 * @return number of imported elements
	 */
	protected abstract long doIndex(IndexingEvent elementpush) throws Exception;
	
	protected abstract String getName();
	
	/**
	 * @return in msec, set 0 to disable.
	 */
	protected abstract long getTTL();
	
	private class ElasticSearchPushElement implements IndexingEvent {
		
		BulkRequestBuilder bulkrequest_index;
		long ttl;
		ArrayList<SourcePathIndexerElement> l_elements_problems;
		
		public ElasticSearchPushElement() {
			bulkrequest_index = client.prepareBulk();
			ttl = getTTL();
			l_elements_problems = new ArrayList<SourcePathIndexerElement>();
		}
		
		private boolean searchForbiddenChars(String filename) {
			if (filename.indexOf("/") > -1) {
				return true;
			}
			if (filename.indexOf("\\") > -1) {
				return true;
			}
			if (filename.indexOf(":") > -1) {
				return true;
			}
			if (filename.indexOf("*") > -1) {
				return true;
			}
			if (filename.indexOf("?") > -1) {
				return true;
			}
			if (filename.indexOf("\"") > -1) {
				return true;
			}
			if (filename.indexOf("<") > -1) {
				return true;
			}
			if (filename.indexOf(">") > -1) {
				return true;
			}
			if (filename.indexOf("|") > -1) {
				return true;
			}
			return false;
		}
		
		public boolean onFoundElement(SourcePathIndexerElement element) {
			if (bulkrequest_index.numberOfActions() > (window_update_size - 1)) {
				execute_Bulks();
			}
			
			String filename = null;
			if (element.parentpath == null) {
				filename = element.currentpath.substring(1);
			} else {
				filename = element.currentpath.substring(element.parentpath.length() + 1);
			}
			
			if (searchForbiddenChars(filename)) {
				l_elements_problems.add(element);
				/**
				 * Disabled this : there is too many bad file name
				 */
				// Log2.log.info("Bad filename", element);
			}
			
			/**
			 * Push it
			 */
			IndexRequestBuilder irb = null;
			String index_type = null;
			if (element.directory) {
				index_type = ES_TYPE_DIRECTORY;
			} else {
				index_type = ES_TYPE_FILE;
			}
			
			if (ttl > 0) {
				irb = client.prepareIndex(ES_INDEX, index_type, element.prepare_key()).setSource(element.toJson().toJSONString()).setTTL(ttl);
			} else {
				irb = client.prepareIndex(ES_INDEX, index_type, element.prepare_key()).setSource(element.toJson().toJSONString());
			}
			bulkrequest_index.add(irb);
			return true;
		}
		
		public void execute_Bulks() {
			Log2Dump dump = new Log2Dump();
			dump.add("name", getName());
			dump.add("indexed", bulkrequest_index.numberOfActions());
			Log2.log.debug("Prepare to update Elasticsearch database", dump);
			
			BulkResponse bulkresponse;
			
			if (bulkrequest_index.numberOfActions() > 0) {
				bulkresponse = bulkrequest_index.execute().actionGet();
				if (bulkresponse.hasFailures()) {
					dump = new Log2Dump();
					dump.add("name", getName());
					dump.add("type", "index");
					dump.add("failure message", bulkresponse.buildFailureMessage());
					Log2.log.error("Errors during indexing", null, dump);
				}
				bulkrequest_index = client.prepareBulk();
			}
			
		}
		
		public void end() {
			execute_Bulks();
		}
	}
	
	public final long index() throws Exception {
		ElasticSearchPushElement push = new ElasticSearchPushElement();
		long result = doIndex(push);
		push.end();
		
		if (push.l_elements_problems.size() > 0) {
			ServiceMessageError messageerror = new ServiceMessageError("Indexation des stockage : certains fichiers ont des noms invalides", null);
			ArrayList<Log2Dump> tablecontent = new ArrayList<Log2Dump>(push.l_elements_problems.size());
			for (int pos = 0; pos < push.l_elements_problems.size(); pos++) {
				tablecontent.add(push.l_elements_problems.get(pos).getLog2Dump());
			}
			messageerror.setTablecontent(tablecontent);
			/**
			 * Disabled this : there is too many bad file name
			 */
			// messagemanager.sendMessage(messageerror);
		}
		
		return result;
	}
}
