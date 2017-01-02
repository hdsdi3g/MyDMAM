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
package hd3gtv.mydmam.metadata;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.manager.InstanceStatus;
import hd3gtv.mydmam.metadata.MetadataCenter.MetadataConfigurationItem;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.ContainerOperations;
import hd3gtv.mydmam.metadata.container.ContainerOrigin;
import hd3gtv.mydmam.metadata.container.EntrySummary;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.tools.StoppableProcessing;

public class MetadataIndexingOperation {
	private File physical_source;
	private SourcePathIndexerElement reference;
	private List<FutureCreateJobs> create_job_list;
	private MetadataIndexingLimit limit;
	
	private Map<String, MetadataExtractor> master_as_preview_mime_list_providers;
	private ArrayList<MetadataExtractor> current_metadata_extractors;
	
	private StoppableProcessing stoppable = new StoppableProcessing() {
		public boolean isWantToStopCurrentProcessing() {
			return false;
		}
	};
	
	@SuppressWarnings("unchecked")
	public MetadataIndexingOperation(File physical_source) {
		this.physical_source = physical_source;
		if (physical_source == null) {
			throw new NullPointerException("\"physical_source\" can't to be null");
		}
		limit = MetadataIndexingLimit.NOLIMITS;
		master_as_preview_mime_list_providers = MetadataCenter.getMasterAsPreviewMimeListProviders();
		current_metadata_extractors = (ArrayList<MetadataExtractor>) MetadataCenter.getExtractors().clone();
	}
	
	public MetadataIndexingOperation setStoppable(StoppableProcessing stoppable) {
		if (stoppable != null) {
			this.stoppable = stoppable;
		}
		return this;
	}
	
	public MetadataIndexingOperation setReference(SourcePathIndexerElement reference) {
		this.reference = reference;
		return this;
	}
	
	/**
	 * Need to set MetadataIndexingLimit to NoLimit, for to be functionnal.
	 */
	public MetadataIndexingOperation setCreateJobList(List<FutureCreateJobs> create_job_list) {
		this.create_job_list = create_job_list;
		return this;
	}
	
	public MetadataIndexingOperation setLimit(MetadataIndexingLimit limit) {
		if (limit == null) {
			this.limit = MetadataIndexingLimit.NOLIMITS;
		} else {
			this.limit = limit;
		}
		return this;
	}
	
	/**
	 * Functionnal only if reference is set.
	 */
	public MetadataIndexingOperation importConfiguration() {
		if (reference == null) {
			return this;
		}
		for (int pos = 0; pos < MetadataCenter.conf_items.size(); pos++) {
			MetadataConfigurationItem item = MetadataCenter.conf_items.get(pos);
			if (reference.storagename.equals(item.storage_label_name) == false) {
				continue;
			}
			if (reference.currentpath.startsWith(item.currentpath) == false) {
				continue;
			}
			if (item.blacklist != null) {
				for (int pos_bl = 0; pos_bl < item.blacklist.size(); pos_bl++) {
					for (int pos_cme = current_metadata_extractors.size() - 1; pos_cme > -1; pos_cme--) {
						if (item.blacklist.get(pos_bl).isAssignableFrom(current_metadata_extractors.get(pos_cme).getClass())) {
							current_metadata_extractors.remove(pos_cme);
						}
					}
				}
			}
			setLimit(item.limit);
			break;
		}
		
		return this;
	}
	
	/**
	 * If setReference() is not called before, or if actual db info don't exists... it will call doIndexing().
	 * It follow limit. It ignore blacklist.
	 * @return the updated container or null if the actual container is not modified. YOU MUST SAVE IT IN YOUR SIDE.
	 */
	Container reprocess(MetadataExtractor metadata_extractor, boolean cancel_if_not_found) throws Exception {
		/**
		 * If current blackling configuration was remove this metadata_extractor, it will re-add on current extractors list.
		 * Needed for all doIndexing() calls: if this funct has need to call doIndexing(), it will stupid to forget to call the original extractor that user want to process.
		 */
		if (current_metadata_extractors.stream().anyMatch(p -> {
			return metadata_extractor.getClass().isAssignableFrom(p.getClass());
		}) == false) {
			current_metadata_extractors.add(metadata_extractor);
		}
		
		if (reference == null) {
			Loggers.Metadata.debug("Can't reprocess a null item");
			return doIndexing();
		}
		
		Container container = null;
		try {
			container = ContainerOperations.getByMtdKey(ContainerOrigin.getUniqueElementKey(reference));
		} catch (Exception e) {
			Loggers.Metadata.warn("Can't reprocess " + reference + " because the actual container can't be get correctly from database.", e);
			if (cancel_if_not_found == false) {
				return doIndexing();
			}
		}
		
		if (container == null) {
			Loggers.Metadata.info("Can't reprocess " + reference + " because it's a new element.");
			if (cancel_if_not_found == false) {
				return doIndexing();
			}
		}
		
		String mime = container.getSummary().getMimetype();
		ContainerEntryResult generator_result = null;
		try {
			if (metadata_extractor.canProcessThisMimeType(mime) == false) {
				Loggers.Metadata.debug("Can't reprocess " + reference + " with " + metadata_extractor.getClass().getSimpleName() + " because it's not support the item mime (" + mime + ").");
				return null;
			}
			
			generator_result = internalProcess(metadata_extractor, mime, container);
		} catch (Exception e) {
			Loggers.Metadata.error(
					"Can't reanalyst/render file, " + "analyser class: " + metadata_extractor + ", analyser name: " + metadata_extractor.getLongName() + ", physical_source: " + physical_source, e);
			return null;
		}
		
		if (generator_result == null) {
			Loggers.Metadata.debug("Reprocess " + reference + " with " + metadata_extractor.getClass().getSimpleName() + " return nothing.");
			return null;
		}
		
		if (master_as_preview_mime_list_providers != null) {
			mime = mime.toLowerCase();
			if (master_as_preview_mime_list_providers.containsKey(mime)) {
				container.getSummary().master_as_preview = master_as_preview_mime_list_providers.get(mime).isCanUsedInMasterAsPreview(container);
			}
		}
		
		if (Loggers.Metadata.isInfoEnabled()) {
			Loggers.Metadata.info("Indexing item " + reference + " with " + metadata_extractor.getClass().getSimpleName() + " will have now this entry: " + generator_result.toString());
		}
		
		if (limit == MetadataIndexingLimit.NOLIMITS | limit == MetadataIndexingLimit.FULL) {
			RenderedFile.cleanCurrentTempDirectory();
		}
		
		return container;
	}
	
	public Container doIndexing() throws Exception {
		if (reference == null) {
			if (physical_source.isFile() == false) {
				throw new IOException(physical_source.getPath() + " is not a valid file");
			}
			
			reference = new SourcePathIndexerElement();
			reference.size = physical_source.length();
			reference.currentpath = physical_source.getCanonicalPath();
			reference.date = physical_source.lastModified();
			reference.directory = false;
			reference.storagename = "standalone-" + InstanceStatus.getStatic().summary.getInstanceNamePid();
			reference.parentpath = physical_source.getParentFile().getAbsolutePath();
		}
		
		ContainerOrigin origin = ContainerOrigin.fromSource(reference, physical_source);
		Container container = new Container(origin.getUniqueElementKey(), origin);
		EntrySummary entry_summary = new EntrySummary();
		container.addEntry(entry_summary);
		
		if (physical_source.length() == 0) {
			entry_summary.setMimetype("application/null");
		} else {
			entry_summary.setMimetype(MimeExtract.getMime(physical_source));
		}
		
		Loggers.Metadata.debug("Indexing item " + reference + " is a " + entry_summary.getMimetype());
		
		if (limit == MetadataIndexingLimit.MIMETYPE) {
			return container;
		}
		
		String mime = entry_summary.getMimetype();
		
		for (int pos = 0; pos < current_metadata_extractors.size(); pos++) {
			MetadataExtractor metadata_extractor = current_metadata_extractors.get(pos);
			try {
				if (metadata_extractor.canProcessThisMimeType(mime) == false) {
					continue;
				}
				internalProcess(metadata_extractor, mime, container);
			} catch (Exception e) {
				Loggers.Metadata.error(
						"Can't analyst/render file, " + "analyser class: " + metadata_extractor + ", analyser name: " + metadata_extractor.getLongName() + ", physical_source: " + physical_source, e);
			}
		}
		
		if (master_as_preview_mime_list_providers != null) {
			mime = container.getSummary().getMimetype().toLowerCase();
			if (master_as_preview_mime_list_providers.containsKey(mime)) {
				entry_summary.master_as_preview = master_as_preview_mime_list_providers.get(mime).isCanUsedInMasterAsPreview(container);
			}
		}
		
		if (Loggers.Metadata.isDebugEnabled()) {
			Loggers.Metadata.debug("Indexing item " + reference + " will have this container: " + container.toString());
		}
		
		if (limit == MetadataIndexingLimit.NOLIMITS | limit == MetadataIndexingLimit.FULL) {
			RenderedFile.cleanCurrentTempDirectory();
		}
		
		return container;
	}
	
	private ContainerEntryResult internalProcess(MetadataExtractor metadata_extractor, String mime, Container container) throws Exception {
		ContainerEntryResult generator_result = null;
		
		if (limit == MetadataIndexingLimit.FAST) {
			Loggers.Metadata.info("Indexing item " + reference + " with extractor " + metadata_extractor.getLongName() + " in processFast()");
			generator_result = metadata_extractor.processFast(container);
		} else {
			Loggers.Metadata.info("Indexing item " + reference + " with extractor " + metadata_extractor.getLongName() + " in processFull()");
			generator_result = metadata_extractor.processFull(container, stoppable);
			
			if ((limit == MetadataIndexingLimit.NOLIMITS) & (metadata_extractor instanceof MetadataGeneratorRendererViaWorker) & (create_job_list != null)) {
				MetadataGeneratorRendererViaWorker renderer_via_worker = (MetadataGeneratorRendererViaWorker) metadata_extractor;
				int before = create_job_list.size();
				Loggers.Metadata.debug("Indexing item " + reference + " with extractor " + metadata_extractor.getLongName() + " do a prepareJobs()");
				renderer_via_worker.prepareJobs(container, create_job_list);
				
				if (before < create_job_list.size()) {
					for (int pos_mgrvw = before; pos_mgrvw < create_job_list.size() - 1; pos_mgrvw++) {
						Loggers.Metadata.debug("Indexing item " + reference + " with extractor " + metadata_extractor.getLongName() + " will create this job: " + create_job_list.get(pos_mgrvw));
					}
				}
			}
		}
		
		if (generator_result == null) {
			Loggers.Metadata.debug("Indexing item " + reference + " with extractor " + metadata_extractor.getLongName() + " don't return result");
			return null;
		}
		
		container.getSummary().addPreviewsFromEntryRenderer(generator_result, container, metadata_extractor);
		generator_result.addEntriesToContainer(container);
		
		return generator_result;
	}
	
}
