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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.mydmam.metadata;

import hd3gtv.mydmam.manager.AppManager;
import hd3gtv.mydmam.manager.JobContext;
import hd3gtv.mydmam.manager.JobProgression;
import hd3gtv.mydmam.manager.WorkerCapablities;
import hd3gtv.mydmam.manager.WorkerNG;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.ContainerOperations;
import hd3gtv.mydmam.metadata.container.EntryRenderer;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

public class WorkerRenderer extends WorkerNG {
	
	private WorkerIndexer metadataworkerindexer;
	
	public WorkerRenderer(WorkerIndexer metadataworkerindexer) {
		this.metadataworkerindexer = metadataworkerindexer;
		if (metadataworkerindexer == null) {
			throw new NullPointerException("\"metadataindexerworker\" can't to be null");
		}
		if (metadataworkerindexer.isActivated() == false) {
			return;
		}
	}
	
	public static void createJob(SourcePathIndexerElement source, String name, JobContextMetadataRenderer renderer_context, MetadataGeneratorRendererViaWorker renderer) throws ConnectionException {
		if (source == null) {
			throw new NullPointerException("\"origin_key\" can't to be null");
		}
		if (name == null) {
			throw new NullPointerException("\"name\" can't to be null");
		}
		if (renderer == null) {
			throw new NullPointerException("\"renderer\" can't to be null");
		}
		
		renderer_context.origin_pathindex_key = source.prepare_key();
		renderer_context.neededstorages = Arrays.asList(source.storagename);
		
		AppManager.createJob(renderer_context).setCreator(WorkerRenderer.class).setDeleteAfterCompleted().setName(name).publish();
	}
	
	private volatile MetadataGeneratorRendererViaWorker current_renderer;
	
	protected void workerProcessJob(JobProgression progression, JobContext context) throws Exception {
		current_renderer = null;
		
		JobContextMetadataRenderer renderer_context = (JobContextMetadataRenderer) context;
		
		List<MetadataGeneratorRenderer> metadataGeneratorRenderers = MetadataCenter.getRenderers();
		if (metadataGeneratorRenderers.isEmpty()) {
			throw new NullPointerException("No declared metadatas renderers");
		}
		
		MetadataGeneratorRendererViaWorker _renderer = null;
		for (int pos = 0; pos < metadataGeneratorRenderers.size(); pos++) {
			if (metadataGeneratorRenderers.get(pos) instanceof MetadataGeneratorRendererViaWorker) {
				if (((MetadataGeneratorRendererViaWorker) metadataGeneratorRenderers.get(pos)).getContextClass().equals(renderer_context.getClass())) {
					_renderer = (MetadataGeneratorRendererViaWorker) metadataGeneratorRenderers.get(pos);
					break;
				}
			}
		}
		
		if (_renderer == null) {
			throw new NullPointerException("Can't found declared rendrerer: \"" + renderer_context.getClass().getName() + "\"");
		}
		if ((_renderer instanceof MetadataGeneratorRendererViaWorker) == false) {
			throw new NullPointerException("Invalid rendrerer: \"" + renderer_context.getClass().getName() + "\"");
		}
		current_renderer = (MetadataGeneratorRendererViaWorker) _renderer;
		
		Explorer explorer = new Explorer();
		SourcePathIndexerElement source_element = explorer.getelementByIdkey(renderer_context.origin_pathindex_key);
		if (source_element == null) {
			throw new NullPointerException("Can't found origin element: " + renderer_context.origin_pathindex_key);
		}
		
		Container container = ContainerOperations.getByPathIndexId(renderer_context.origin_pathindex_key);
		if (container == null) {
			/**
			 * Too fast for ES ?
			 */
			Thread.sleep(500);
			container = ContainerOperations.getByPathIndexId(renderer_context.origin_pathindex_key);
			if (container == null) {
				throw new NullPointerException("No actual metadatas !");
			}
		}
		
		File physical_file = Explorer.getLocalBridgedElement(source_element);
		if (physical_file == null) {
			throw new NullPointerException("Can't bridge with real file origin element: " + renderer_context.origin_pathindex_key);
		}
		if (physical_file.exists() == false) {
			throw new FileNotFoundException(physical_file.getPath());
		}
		
		EntryRenderer rendered_entry = current_renderer.standaloneProcess(physical_file, progression, container, renderer_context);
		if (rendered_entry == null) {
			current_renderer = null;
			return;
		}
		
		container.getSummary().addPreviewsFromEntryRenderer(rendered_entry, container, current_renderer);
		container.addEntry(rendered_entry);
		container.save(false);
		
		current_renderer = null;
	}
	
	public void forceStopProcess() throws Exception {
		if (current_renderer == null) {
			return;
		}
		current_renderer.stopStandaloneProcess();
		current_renderer = null;
	}
	
	public WorkerCategory getWorkerCategory() {
		return WorkerCategory.METADATA;
	}
	
	public String getWorkerLongName() {
		return "Metadata Renderer";
	}
	
	public String getWorkerVendorName() {
		return "MyDMAM Internal";
	}
	
	public List<WorkerCapablities> getWorkerCapablities() {
		List<MetadataGeneratorRenderer> metadataGeneratorRenderers = MetadataCenter.getRenderers();
		if (metadataGeneratorRenderers == null) {
			return null;
		}
		if (metadataGeneratorRenderers.isEmpty()) {
			return null;
		}
		List<WorkerCapablities> result = new ArrayList<WorkerCapablities>();
		for (int pos = 0; pos < metadataGeneratorRenderers.size(); pos++) {
			if (metadataGeneratorRenderers.get(pos) instanceof MetadataGeneratorRendererViaWorker) {
				result.addAll(WorkerCapablities.createList(((MetadataGeneratorRendererViaWorker) metadataGeneratorRenderers.get(pos)).getContextClass(), Explorer.getBridgedStoragesName()));
			}
		}
		return result;
	}
	
	protected boolean isActivated() {
		return metadataworkerindexer.isActivated();
	}
	
}
