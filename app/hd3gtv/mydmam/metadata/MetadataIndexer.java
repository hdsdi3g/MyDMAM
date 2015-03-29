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
import hd3gtv.mydmam.db.ElasticsearchBulkOperation;
import hd3gtv.mydmam.manager.JobNG;
import hd3gtv.mydmam.metadata.MetadataIndexingOperation.MetadataIndexingLimit;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.ContainerOperations;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.Importer;
import hd3gtv.mydmam.pathindexing.IndexingEvent;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.pathindexing.WebCacheInvalidation;
import hd3gtv.mydmam.storage.Storage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.indices.IndexMissingException;

public class MetadataIndexer implements IndexingEvent {
	
	private Explorer explorer;
	private boolean force_refresh;
	private boolean stop_analysis;
	private ElasticsearchBulkOperation es_bulk;
	private List<FutureCreateJobs> current_create_job_list;
	private MetadataIndexingLimit limit_processing;
	
	public MetadataIndexer(boolean force_refresh) throws Exception {
		explorer = new Explorer();
		this.force_refresh = force_refresh;
		current_create_job_list = new ArrayList<FutureCreateJobs>();
	}
	
	public void setLimitProcessing(MetadataIndexingLimit limit_processing) {
		this.limit_processing = limit_processing;
	}
	
	/**
	 * @return new created jobs, never null
	 */
	public List<JobNG> process(SourcePathIndexerElement item, long min_index_date) throws Exception {
		if (item == null) {
			return new ArrayList<JobNG>(1);
		}
		
		stop_analysis = false;
		es_bulk = Elasticsearch.prepareBulk();
		
		Log2Dump dump = new Log2Dump();
		dump.add("item", item);
		dump.addDate("min_index_date", min_index_date);
		Log2.log.debug("Prepare", dump);
		
		if (item.directory) {
			explorer.getAllSubElementsFromElementKey(item.prepare_key(), min_index_date, this);
		} else {
			if (onFoundElement(item) == false) {
				return new ArrayList<JobNG>(1);
			}
		}
		
		es_bulk.terminateBulk();
		
		WebCacheInvalidation.addInvalidation(item.storagename);
		
		if (current_create_job_list.isEmpty()) {
			return new ArrayList<JobNG>(1);
		}
		ArrayList<JobNG> new_jobs = new ArrayList<JobNG>(current_create_job_list.size());
		for (int pos = 0; pos < current_create_job_list.size(); pos++) {
			new_jobs.add(current_create_job_list.get(pos).createJob());
		}
		return new_jobs;
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
				container = ContainerOperations.getByPathIndexId(element_key);
				if (container == null) {
					must_analyst = true;
				} else {
					/**
					 * For all metadata elements for this source path indexed element
					 */
					if ((element.date != container.getOrigin().getDate()) | (element.size != container.getOrigin().getSize())) {
						RenderedFile.purge(container.getMtd_key());
						ContainerOperations.requestDelete(container, es_bulk);
						
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
		
		File physical_source = Storage.getLocalFile(element);
		
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
				ContainerOperations.requestDelete(container, es_bulk);
				RenderedFile.purge(container.getMtd_key());
				
				dump = new Log2Dump();
				dump.add("physical_source", physical_source);
				dump.addAll(container);
				Log2.log.debug("Delete obsolete analysis : original file isn't exists", dump);
			}
			
			es_bulk.add(es_bulk.getClient().prepareDelete(Importer.ES_INDEX, Importer.ES_TYPE_FILE, element_key));
			dump = new Log2Dump();
			dump.add("key", element_key);
			dump.add("physical_source", physical_source);
			Log2.log.debug("Delete path element: original file isn't exists", dump);
			
			if (physical_source.getParentFile().exists() == false) {
				if (element.parentpath == null) {
					return true;
				}
				if (element.parentpath.equals("")) {
					return true;
				}
				es_bulk.add(es_bulk.getClient().prepareDelete(Importer.ES_INDEX, Importer.ES_TYPE_DIRECTORY, element.parentpath));
				dump = new Log2Dump();
				dump.add("key", element.parentpath);
				dump.add("physical_source parent", physical_source.getParentFile());
				Log2.log.debug("Delete parent path element: original directory isn't exists", dump);
			}
			
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
		
		MetadataIndexingOperation indexing = new MetadataIndexingOperation(physical_source);
		indexing.setReference(element);
		indexing.setCreateJobList(current_create_job_list);
		indexing.importConfiguration();
		if (limit_processing != null) {
			indexing.setLimit(limit_processing);
		}
		ContainerOperations.save(indexing.doIndexing(), false, es_bulk);
		return true;
	}
	
	public void onRemoveFile(String storagename, String path) throws Exception {
	}
	
}
