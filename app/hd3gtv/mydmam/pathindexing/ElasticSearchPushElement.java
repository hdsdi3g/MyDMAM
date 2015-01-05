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
 * Copyright (C) hdsdi3g for hd3g.tv 2015
 * 
*/
package hd3gtv.mydmam.pathindexing;

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.db.Elasticsearch;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;

class ElasticSearchPushElement implements IndexingEvent {
	
	private static final int window_update_size = 5000;
	
	public void onRemoveFile(String storagename, String path) throws Exception {
	}
	
	private Importer importer;
	BulkRequestBuilder bulkrequest_index;
	long ttl;
	private Client client;
	
	// ArrayList<SourcePathIndexerElement> l_elements_problems;
	
	public ElasticSearchPushElement(Importer importer) {
		client = Elasticsearch.getClient();
		bulkrequest_index = client.prepareBulk();
		ttl = importer.getTTL();
		this.importer = importer;
		// l_elements_problems = new ArrayList<SourcePathIndexerElement>();
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
		
		if (element.parentpath != null) {
			String filename = element.currentpath.substring(element.currentpath.lastIndexOf("/"), element.currentpath.length());
			if (searchForbiddenChars(filename)) {
				// l_elements_problems.add(element);
				/**
				 * Disabled this : there is too many bad file name
				 */
				// Log2.log.info("Bad filename", element);
			}
		}
		
		String index_type = null;
		if (element.directory) {
			index_type = Importer.ES_TYPE_DIRECTORY;
		} else {
			index_type = Importer.ES_TYPE_FILE;
		}
		
		/**
		 * Push it
		 */
		if (ttl > 0) {
			bulkrequest_index.add(client.prepareIndex(Importer.ES_INDEX, index_type, element.prepare_key()).setSource(element.toGson().toString()).setTTL(ttl));
		} else {
			bulkrequest_index.add(client.prepareIndex(Importer.ES_INDEX, index_type, element.prepare_key()).setSource(element.toGson().toString()));
		}
		
		return true;
	}
	
	public void execute_Bulks() {
		client = Elasticsearch.getClient();
		
		if (bulkrequest_index.numberOfActions() > 0) {
			Log2Dump dump = new Log2Dump();
			dump = new Log2Dump();
			dump.add("name", importer.getName());
			dump.add("indexed", bulkrequest_index.numberOfActions());
			Log2.log.debug("Prepare to update Elasticsearch database", dump);
			
			BulkRequest bu_r = bulkrequest_index.request();
			BulkResponse bulkresponse = client.bulk(bu_r).actionGet();
			
			if (bulkresponse.hasFailures()) {
				dump = new Log2Dump();
				dump.add("name", importer.getName());
				dump.add("type", "index");
				dump.add("failure message", bulkresponse.buildFailureMessage());
				Log2.log.error("Errors during indexing", null, dump);
			}
			bulkrequest_index.request().requests().clear();
		}
		
	}
	
	public void end() {
		execute_Bulks();
	}
	
}