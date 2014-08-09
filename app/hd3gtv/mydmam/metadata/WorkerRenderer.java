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

import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.EntryRenderer;
import hd3gtv.mydmam.metadata.container.Operations;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.taskqueue.Broker;
import hd3gtv.mydmam.taskqueue.Job;
import hd3gtv.mydmam.taskqueue.Profile;
import hd3gtv.mydmam.taskqueue.Worker;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

public class WorkerRenderer extends Worker {
	
	public static final String PROFILE_CATEGORY = "metadata-rendering";
	
	private WorkerIndexer metadataworkerindexer;
	
	public WorkerRenderer(WorkerIndexer metadataworkerindexer) {
		this.metadataworkerindexer = metadataworkerindexer;
		if (metadataworkerindexer == null) {
			throw new NullPointerException("\"metadataindexerworker\" can't to be null");
		}
		if (metadataworkerindexer.isConfigurationAllowToEnabled() == false) {
			return;
		}
	}
	
	public List<Profile> getManagedProfiles() {
		List<GeneratorRenderer> generatorRenderers = MetadataCenter.getRenderers();
		if (generatorRenderers == null) {
			return null;
		}
		if (generatorRenderers.isEmpty()) {
			return null;
		}
		List<Profile> profiles = new ArrayList<Profile>();
		for (int pos = 0; pos < generatorRenderers.size(); pos++) {
			if (generatorRenderers.get(pos) instanceof GeneratorRendererViaWorker) {
				profiles.add(generatorRenderers.get(pos).getManagedProfile());
			}
		}
		return profiles;
	}
	
	public String getShortWorkerName() {
		return "metadata-renderer";
	}
	
	public String getLongWorkerName() {
		return "Metadata Renderer";
	}
	
	public boolean isConfigurationAllowToEnabled() {
		return metadataworkerindexer.isConfigurationAllowToEnabled();
	}
	
	@SuppressWarnings("unchecked")
	public static String createTask(String origin_key, String name, JSONObject renderer_context, GeneratorRendererViaWorker renderer, String require_task) throws ConnectionException {
		if (origin_key == null) {
			throw new NullPointerException("\"origin_key\" can't to be null");
		}
		if (name == null) {
			throw new NullPointerException("\"name\" can't to be null");
		}
		if (renderer == null) {
			throw new NullPointerException("\"renderer\" can't to be null");
		}
		
		JSONObject context = new JSONObject();
		context.put("origin", origin_key);
		if (renderer_context != null) {
			context.put("renderer", renderer_context);
		}
		return Broker.publishTask(name, renderer.getManagedProfile(), context, WorkerRenderer.class, false, 0, require_task, false);
	}
	
	public static String createTask(String origin_key, String name, JSONObject renderer_context, GeneratorRendererViaWorker renderer) throws ConnectionException {
		return createTask(origin_key, name, renderer_context, renderer, null);
	}
	
	private volatile GeneratorRendererViaWorker current_renderer;
	
	public void process(Job job) throws Exception {
		current_renderer = null;
		
		JSONObject context = job.getContext();
		if (context == null) {
			throw new NullPointerException("No context");
		}
		if (context.isEmpty()) {
			throw new NullPointerException("No context");
		}
		List<GeneratorRenderer> generatorRenderers = MetadataCenter.getRenderers();
		if (generatorRenderers.isEmpty()) {
			throw new NullPointerException("No declared metadatas renderers");
		}
		
		GeneratorRenderer _renderer = null;
		for (int pos = 0; pos < generatorRenderers.size(); pos++) {
			if (generatorRenderers.get(pos).getManagedProfile().equals(job.getProfile())) {
				_renderer = generatorRenderers.get(pos);
				break;
			}
		}
		
		if (_renderer == null) {
			throw new NullPointerException("Can't found declared rendrerer: \"" + job.getProfile().getName() + "\"");
		}
		if ((_renderer instanceof GeneratorRendererViaWorker) == false) {
			throw new NullPointerException("Invalid rendrerer: \"" + job.getProfile().getName() + "\"");
		}
		current_renderer = (GeneratorRendererViaWorker) _renderer;
		
		if (context.containsKey("origin") == false) {
			throw new NullPointerException("No origin file !");
		}
		String origin_pathindex_key = (String) context.get("origin");
		
		Explorer explorer = new Explorer();
		SourcePathIndexerElement source_element = explorer.getelementByIdkey(origin_pathindex_key);
		if (source_element == null) {
			throw new NullPointerException("Can't found origin element: " + origin_pathindex_key);
		}
		
		Container container = Operations.getByPathIndexId(origin_pathindex_key);
		if (container == null) {
			throw new NullPointerException("No actual metadatas !");
		}
		
		File physical_file = Explorer.getLocalBridgedElement(source_element);
		if (physical_file == null) {
			throw new NullPointerException("Can't bridge with real file origin element: " + origin_pathindex_key);
		}
		if (physical_file.exists() == false) {
			throw new FileNotFoundException(physical_file.getPath());
		}
		
		EntryRenderer rendered_entry = null;
		if (context.containsKey("renderer")) {
			rendered_entry = current_renderer.standaloneProcess(physical_file, job, container, (JSONObject) context.get("renderer"));
		} else {
			rendered_entry = current_renderer.standaloneProcess(physical_file, job, container, null);
		}
		if (rendered_entry == null) {
			current_renderer = null;
			return;
		}
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
	
}
