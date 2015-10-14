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
import hd3gtv.mydmam.metadata.container.EntryAnalyser;
import hd3gtv.mydmam.metadata.container.EntryRenderer;
import hd3gtv.mydmam.metadata.container.EntrySummary;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;

public class MetadataIndexingOperation {
	private File physical_source;
	private SourcePathIndexerElement reference;
	private List<FutureCreateJobs> create_job_list;
	private MetadataIndexingLimit limit;
	
	private Map<String, MetadataGeneratorAnalyser> master_as_preview_mime_list_providers;
	private ArrayList<MetadataGeneratorAnalyser> metadataGeneratorAnalysers;
	private ArrayList<MetadataGeneratorRenderer> metadataGeneratorRenderers;
	
	@SuppressWarnings("unchecked")
	public MetadataIndexingOperation(File physical_source) {
		this.physical_source = physical_source;
		if (physical_source == null) {
			throw new NullPointerException("\"physical_source\" can't to be null");
		}
		limit = MetadataIndexingLimit.NOLIMITS;
		master_as_preview_mime_list_providers = MetadataCenter.getMasterAsPreviewMimeListProviders();
		metadataGeneratorAnalysers = (ArrayList<MetadataGeneratorAnalyser>) MetadataCenter.getAnalysers().clone();
		metadataGeneratorRenderers = (ArrayList<MetadataGeneratorRenderer>) MetadataCenter.getRenderers().clone();
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
	
	public enum MetadataIndexingLimit {
		/**
		 * Just get Mime type
		 */
		MIMETYPE, /**
					 * MimeType + all analysers
					 */
		ANALYST, /**
					 * MimeType + all analysers + all simple renderers
					 */
		SIMPLERENDERS, /**
						 * Full, but you need to set a CreateJobList
						 */
		NOLIMITS
	}
	
	public MetadataIndexingOperation setLimit(MetadataIndexingLimit limit) {
		if (limit == null) {
			this.limit = MetadataIndexingLimit.NOLIMITS;
		} else {
			this.limit = limit;
		}
		return this;
	}
	
	public MetadataIndexingOperation blacklistGenerator(Class<? extends MetadataGenerator> generator_to_ignore) {
		if (generator_to_ignore == null) {
			return this;
		}
		for (int pos = metadataGeneratorAnalysers.size() - 1; pos > -1; pos--) {
			if (generator_to_ignore.isAssignableFrom(metadataGeneratorAnalysers.get(pos).getClass())) {
				metadataGeneratorAnalysers.remove(pos);
			}
		}
		for (int pos = metadataGeneratorRenderers.size() - 1; pos > -1; pos--) {
			if (generator_to_ignore.isAssignableFrom(metadataGeneratorRenderers.get(pos).getClass())) {
				metadataGeneratorRenderers.remove(pos);
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
			reference.storagename = "standalone-" + InstanceStatus.getThisInstanceNamePid();
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
		
		if (limit == MetadataIndexingLimit.MIMETYPE) {
			return container;
		}
		
		for (int pos = 0; pos < metadataGeneratorAnalysers.size(); pos++) {
			MetadataGeneratorAnalyser metadataGeneratorAnalyser = metadataGeneratorAnalysers.get(pos);
			if (metadataGeneratorAnalyser.canProcessThis(entry_summary.getMimetype())) {
				try {
					EntryAnalyser entry_analyser = metadataGeneratorAnalyser.process(container);
					if (entry_analyser == null) {
						continue;
					}
					container.addEntry(entry_analyser);
				} catch (Exception e) {
					Loggers.Metadata.error("Can't analyst/render file, " + "analyser class: " + metadataGeneratorAnalyser + ", analyser name: " + metadataGeneratorAnalyser.getLongName()
							+ ", physical_source: " + physical_source, e);
				}
			}
		}
		
		if (master_as_preview_mime_list_providers != null) {
			String mime = container.getSummary().getMimetype().toLowerCase();
			if (master_as_preview_mime_list_providers.containsKey(mime)) {
				entry_summary.master_as_preview = master_as_preview_mime_list_providers.get(mime).isCanUsedInMasterAsPreview(container);
			}
		}
		
		if (limit == MetadataIndexingLimit.ANALYST) {
			return container;
		}
		
		for (int pos = 0; pos < metadataGeneratorRenderers.size(); pos++) {
			MetadataGeneratorRenderer metadataGeneratorRenderer = metadataGeneratorRenderers.get(pos);
			if (metadataGeneratorRenderer.canProcessThis(entry_summary.getMimetype())) {
				try {
					EntryRenderer entry_renderer = metadataGeneratorRenderer.process(container);
					if ((metadataGeneratorRenderer instanceof MetadataGeneratorRendererViaWorker) & (create_job_list != null) & (limit == MetadataIndexingLimit.NOLIMITS)) {
						MetadataGeneratorRendererViaWorker renderer_via_worker = (MetadataGeneratorRendererViaWorker) metadataGeneratorRenderer;
						renderer_via_worker.prepareJobs(container, create_job_list);
					}
					if (entry_renderer == null) {
						continue;
					}
					
					container.getSummary().addPreviewsFromEntryRenderer(entry_renderer, container, metadataGeneratorRenderer);
					container.addEntry(entry_renderer);
				} catch (Exception e) {
					Loggers.Metadata.error("Can't analyst/render file, " + "provider class: " + metadataGeneratorRenderer + ", provider name: " + metadataGeneratorRenderer.getLongName()
							+ ", physical_source: " + physical_source, e);
				}
			}
		}
		
		RenderedFile.cleanCurrentTempDirectory();
		
		return container;
	}
	
}
