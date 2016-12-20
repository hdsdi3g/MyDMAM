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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.indices.IndexMissingException;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.db.ElasticsearchBulkOperation;
import hd3gtv.mydmam.manager.JobNG;
import hd3gtv.mydmam.manager.JobProgression;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.ContainerOperations;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.Importer;
import hd3gtv.mydmam.pathindexing.IndexingEvent;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.pathindexing.WebCacheInvalidation;
import hd3gtv.mydmam.storage.Storage;
import hd3gtv.tools.StoppableProcessing;

public class MetadataStorageIndexer implements StoppableProcessing {
	
	private Explorer explorer;
	private boolean force_refresh;
	private boolean stop_analysis;
	private ElasticsearchBulkOperation es_bulk;
	private List<FutureCreateJobs> current_create_job_list;
	private MetadataIndexingLimit limit_processing;
	private ArrayList<SourcePathIndexerElement> process_list;
	
	public MetadataStorageIndexer(boolean force_refresh) throws Exception {
		explorer = new Explorer();
		this.force_refresh = force_refresh;
		current_create_job_list = new ArrayList<FutureCreateJobs>();
		process_list = new ArrayList<SourcePathIndexerElement>();
	}
	
	public void setLimitProcessing(MetadataIndexingLimit limit_processing) {
		this.limit_processing = limit_processing;
	}
	
	/**
	 * @return new created jobs, never null
	 */
	public List<JobNG> process(SourcePathIndexerElement item, long min_index_date, JobProgression progression) throws Exception {
		if (item == null) {
			return new ArrayList<JobNG>(1);
		}
		
		stop_analysis = false;
		
		Loggers.Metadata.debug("Prepare, item: " + item + ", min_index_date: " + Loggers.dateLog(min_index_date));
		
		if (item.directory) {
			explorer.getAllSubElementsFromElementKey(item.prepare_key(), min_index_date, new IndexingEvent() {
				
				public void onRemoveFile(String storagename, String path) throws Exception {
				}
				
				public boolean onFoundElement(SourcePathIndexerElement element) throws Exception {
					if (isWantToStopCurrentProcessing()) {
						return false;
					}
					if (element.directory == false) {
						process_list.add(element);
					}
					return true;
				}
			});
		} else {
			if (processFoundedElement(item) == false) {
				return new ArrayList<JobNG>(1);
			}
		}
		
		if (process_list.isEmpty()) {
			return new ArrayList<JobNG>(1);
		}
		
		es_bulk = Elasticsearch.prepareBulk();
		if (limit_processing == MetadataIndexingLimit.MIMETYPE | limit_processing == MetadataIndexingLimit.FAST) {
			es_bulk.setWindowUpdateSize(50);
		} else {
			es_bulk.setWindowUpdateSize(1);
		}
		
		for (int pos = 0; pos < process_list.size(); pos++) {
			if (progression != null) {
				progression.updateProgress(pos, process_list.size());
			}
			if (processFoundedElement(process_list.get(pos)) == false | isWantToStopCurrentProcessing()) {
				break;
			}
		}
		
		es_bulk.terminateBulk();
		
		WebCacheInvalidation.addInvalidation(item.storagename);
		
		if (current_create_job_list.isEmpty()) {
			return new ArrayList<JobNG>(1);
		}
		ArrayList<JobNG> new_jobs = new ArrayList<JobNG>(current_create_job_list.size());
		JobNG new_job;
		for (int pos = 0; pos < current_create_job_list.size(); pos++) {
			new_job = current_create_job_list.get(pos).createJob();
			if (new_job == null) {
				continue;
			}
			new_jobs.add(new_job);
			Loggers.Metadata.debug("Create job for deep metadata extracting: " + new_job.toStringLight());
		}
		return new_jobs;
	}
	
	private boolean processFoundedElement(SourcePathIndexerElement element) throws Exception {
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
						
						Loggers.Metadata.debug("Obsolete analysis, " + container + "; Element " + element);
						
						must_analyst = true;
						container = null;
					}
				}
			} catch (IndexMissingException ime) {
				must_analyst = true;
			} catch (SearchPhaseExecutionException e) {
				must_analyst = true;
			} catch (Exception e) {
				Loggers.Metadata.warn("Invalid Container status for [" + element.toString(" ") + "]. Ignore it and restart the analyst.", e);
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
		
		Loggers.Metadata.debug("Analyst this, " + element + ", physical_source: " + physical_source + ", force_refresh: " + force_refresh);
		
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
				Loggers.Metadata.debug("Delete obsolete analysis : original file isn't exists, physical_source: " + physical_source + ", " + container);
			}
			
			es_bulk.add(es_bulk.getClient().prepareDelete(Importer.ES_INDEX, Importer.ES_TYPE_FILE, element_key));
			Loggers.Metadata.debug("Delete path element: original file isn't exists, key: " + element_key + ", physical_source: " + physical_source);
			
			if (physical_source.getParentFile().exists() == false) {
				if (element.parentpath == null) {
					return true;
				}
				if (element.parentpath.equals("")) {
					return true;
				}
				es_bulk.add(es_bulk.getClient().prepareDelete(Importer.ES_INDEX, Importer.ES_TYPE_DIRECTORY, element.parentpath));
				Loggers.Metadata.debug("Delete parent path element: original directory isn't exists, key: " + element.parentpath + ", physical_source parent: " + physical_source.getParentFile());
			}
			
			return true;
		}
		if (physical_source.isFile() == false) {
			throw new IOException(physical_source.getPath() + " is not a file");
		}
		if (physical_source.canRead() == false) {
			throw new IOException("Can't read " + physical_source.getPath());
		}
		
		if (isWantToStopCurrentProcessing()) {
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
		
		/**
		 * Read the file first byte for check if this file can be read.
		 */
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(physical_source);
			fis.read();
		} catch (Exception e) {
			Loggers.Metadata.debug("Can't start index: " + element_key + ", physical_source: " + physical_source, e);
			IOUtils.closeQuietly(fis);
			return true;
		} finally {
			IOUtils.closeQuietly(fis);
		}
		
		MetadataIndexingOperation indexing = new MetadataIndexingOperation(physical_source);
		indexing.setReference(element);
		indexing.setCreateJobList(current_create_job_list);
		indexing.importConfiguration();
		indexing.setStoppable(this);
		if (limit_processing != null) {
			indexing.setLimit(limit_processing);
		}
		Loggers.Metadata.debug("Start indexing for: " + element_key + ", physical_source: " + physical_source);
		
		container = indexing.doIndexing();
		if (stop_analysis == false) {
			ContainerOperations.save(container, false, es_bulk);
		}
		return true;
	}
	
	public synchronized void stop() {
		stop_analysis = true;
	}
	
	public synchronized boolean isWantToStopCurrentProcessing() {
		return stop_analysis;
	}
	
}
