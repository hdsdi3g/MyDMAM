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
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
*/
package hd3gtv.mydmam.metadata;

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.Operations;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.Importer;
import hd3gtv.mydmam.pathindexing.IndexingEvent;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.taskqueue.FutureCreateTasks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.indices.IndexMissingException;

public class MetadataIndexer implements IndexingEvent {
	
	private static Client client;
	private Explorer explorer;
	private boolean force_refresh;
	private boolean stop_analysis;
	private BulkRequestBuilder bulkrequest;
	private List<FutureCreateTasks> current_create_task_list;
	
	static {
		client = Elasticsearch.getClient();
	}
	
	public MetadataIndexer(boolean force_refresh) throws Exception {
		this.force_refresh = force_refresh;
		current_create_task_list = new ArrayList<FutureCreateTasks>();
	}
	
	public void process(String storagename, String currentpath, long min_index_date) throws Exception {
		stop_analysis = false;
		bulkrequest = client.prepareBulk();
		explorer = new Explorer();
		explorer.getAllSubElementsFromElementKey(Explorer.getElementKey(storagename, currentpath), min_index_date, this);
		bulkExecute();
		
		for (int pos = 0; pos < current_create_task_list.size(); pos++) {
			current_create_task_list.get(pos).createTask();
		}
	}
	
	private void bulkExecute() {
		if (bulkrequest.numberOfActions() == 0) {
			return;
		}
		BulkResponse bulkresponse = bulkrequest.execute().actionGet();
		if (bulkresponse.hasFailures()) {
			Log2Dump dump = new Log2Dump();
			dump.add("failure message", bulkresponse.buildFailureMessage());
			Log2.log.error("ES errors during add/delete documents", null, dump);
		}
	}
	
	public synchronized void stop() {
		stop_analysis = true;
	}
	
	public boolean onFoundElement(SourcePathIndexerElement element) throws Exception {
		if (stop_analysis) {
			return false;
		}
		if (element.directory) {
			return true;
		}
		
		if (bulkrequest.numberOfActions() > 1000) {
			bulkExecute();
			bulkrequest = client.prepareBulk();
		}
		
		String element_key = element.prepare_key();
		
		boolean must_analyst = false;
		Container container = null;
		
		if (force_refresh) {
			must_analyst = true;
		} else {
			try {
				/**
				 * Search old metadatas element
				 */
				container = Operations.getByPathIndexId(element_key);
				if (container == null) {
					must_analyst = true;
				} else {
					/**
					 * For all metadata elements for this source path indexed element
					 */
					if ((element.date != container.getOrigin().getDate()) | (element.size != container.getOrigin().getSize())) {
						RenderedFile.purge(container.getMtd_key());
						Operations.requestDelete(container, bulkrequest);
						
						Log2Dump dump = new Log2Dump();
						dump.addAll(element);
						dump.addAll(container);
						Log2.log.debug("Obsolete analysis", dump);
						
						must_analyst = true;
						container = null;
					}
				}
			} catch (IndexMissingException ime) {
				must_analyst = true;
			}
		}
		
		if (must_analyst == false) {
			return true;
		}
		
		File physical_source = Explorer.getLocalBridgedElement(element);
		
		if (stop_analysis) {
			return false;
		}
		
		Log2Dump dump = new Log2Dump();
		dump.addAll(element);
		dump.add("physical_source", physical_source);
		dump.add("force_refresh", force_refresh);
		Log2.log.debug("Analyst this", dump);
		
		/**
		 * Test if real file exists and if it's valid
		 */
		if (physical_source == null) {
			throw new IOException("Can't analyst element : there is no Configuration bridge for the \"" + element.storagename + "\" storage index name.");
		}
		if (physical_source.exists() == false) {
			if (container != null) {
				Operations.requestDelete(container, bulkrequest);
				RenderedFile.purge(container.getMtd_key());
				
				dump = new Log2Dump();
				dump.add("physical_source", physical_source);
				dump.addAll(container);
				Log2.log.debug("Delete obsolete analysis : original file isn't exists", dump);
			}
			
			bulkrequest.add(explorer.deleteRequestFileElement(element_key, Importer.ES_TYPE_FILE));
			dump = new Log2Dump();
			dump.add("key", element_key);
			dump.add("physical_source", physical_source);
			Log2.log.debug("Delete path element : original file isn't exists", dump);
			
			return true;
		}
		if (physical_source.isFile() == false) {
			throw new IOException(physical_source.getPath() + " is not a file");
		}
		if (physical_source.canRead() == false) {
			throw new IOException("Can't read " + physical_source.getPath());
		}
		
		if (stop_analysis) {
			return false;
		}
		
		/**
		 * Tests file size : must be constant
		 */
		long current_length = physical_source.length();
		
		if (element.size != current_length) {
			/**
			 * Ignore this file, the size isn't constant... May be this file is in copy ?
			 */
			return true;
		}
		
		if (physical_source.exists() == false) {
			/**
			 * Ignore this file, it's deleted !
			 */
			return true;
		}
		
		Operations.save(MetadataCenter.standaloneIndexing(physical_source, element, current_create_task_list), false, bulkrequest);
		return true;
	}
	
	public void onRemoveFile(String storagename, String path) throws Exception {
	}
	
}
