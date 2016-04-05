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
import hd3gtv.mydmam.metadata.container.ContainerOrigin;
import hd3gtv.mydmam.metadata.container.EntrySummary;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;

public class MetadataIndexingOperation {
	private File physical_source;
	private SourcePathIndexerElement reference;
	private List<FutureCreateJobs> create_job_list;
	private MetadataIndexingLimit limit;
	
	private Map<String, MetadataExtractor> master_as_preview_mime_list_providers;
	private ArrayList<MetadataExtractor> current_metadata_extractors;
	
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
	
	public MetadataIndexingOperation blacklistGenerator(Class<? extends MetadataExtractor> generator_to_ignore) {
		if (generator_to_ignore == null) {
			return this;
		}
		for (int pos = current_metadata_extractors.size() - 1; pos > -1; pos--) {
			if (generator_to_ignore.isAssignableFrom(current_metadata_extractors.get(pos).getClass())) {
				current_metadata_extractors.remove(pos);
			}
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
					blacklistGenerator(item.blacklist.get(pos_bl));
				}
			}
			setLimit(item.limit);
			break;
		}
		
		return this;
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
		
		ContainerEntryResult generator_result = null;
		for (int pos = 0; pos < current_metadata_extractors.size(); pos++) {
			MetadataExtractor metadata_extractor = current_metadata_extractors.get(pos);
			try {
				if (metadata_extractor.canProcessThisMimeType(entry_summary.getMimetype()) == false) {
					continue;
				}
				
				if (limit == MetadataIndexingLimit.ANALYST) {
					Loggers.Metadata.debug("Indexing item " + reference + " with extractor " + metadata_extractor.getClass() + " in processFast()");
					generator_result = metadata_extractor.processFast(container);
				} else {
					Loggers.Metadata.debug("Indexing item " + reference + " with extractor " + metadata_extractor.getClass() + " in processFull()");
					generator_result = metadata_extractor.processFull(container);
					
					if ((limit == MetadataIndexingLimit.NOLIMITS) & (metadata_extractor instanceof MetadataGeneratorRendererViaWorker) & (create_job_list != null)) {
						MetadataGeneratorRendererViaWorker renderer_via_worker = (MetadataGeneratorRendererViaWorker) metadata_extractor;
						int before = create_job_list.size();
						Loggers.Metadata.debug("Indexing item " + reference + " with extractor " + metadata_extractor.getClass() + " do a prepareJobs()");
						renderer_via_worker.prepareJobs(container, create_job_list);
						
						if (before < create_job_list.size()) {
							for (int pos_mgrvw = before; pos_mgrvw < create_job_list.size() - 1; pos_mgrvw++) {
								Loggers.Metadata.debug("Indexing item " + reference + " with extractor " + metadata_extractor.getClass() + " will create this job: " + create_job_list.get(pos_mgrvw));
							}
						}
					}
				}
				
				if (generator_result == null) {
					Loggers.Metadata.debug("Indexing item " + reference + " with extractor " + metadata_extractor.getClass() + " don't return result");
					continue;
				}
				
				container.getSummary().addPreviewsFromEntryRenderer(generator_result, container, metadata_extractor);
				generator_result.addEntriesToContainer(container);
			} catch (Exception e) {
				Loggers.Metadata.error(
						"Can't analyst/render file, " + "analyser class: " + metadata_extractor + ", analyser name: " + metadata_extractor.getLongName() + ", physical_source: " + physical_source, e);
			}
		}
		
		if (master_as_preview_mime_list_providers != null) {
			String mime = container.getSummary().getMimetype().toLowerCase();
			if (master_as_preview_mime_list_providers.containsKey(mime)) {
				entry_summary.master_as_preview = master_as_preview_mime_list_providers.get(mime).isCanUsedInMasterAsPreview(container);
			}
		}
		
		Loggers.Metadata.debug("Indexing item " + reference + " will have this container: " + container.toString());
		
		if (limit == MetadataIndexingLimit.NOLIMITS | limit == MetadataIndexingLimit.SIMPLERENDERS) {
			RenderedFile.cleanCurrentTempDirectory();
		}
		
		return container;
	}
	
}
