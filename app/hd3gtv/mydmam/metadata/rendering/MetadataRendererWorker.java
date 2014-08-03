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
package hd3gtv.mydmam.metadata.rendering;

import hd3gtv.mydmam.metadata.MetadataCenter;
import hd3gtv.mydmam.metadata.indexing.MetadataIndexer;
import hd3gtv.mydmam.metadata.indexing.MetadataIndexerWorker;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.taskqueue.Broker;
import hd3gtv.mydmam.taskqueue.Job;
import hd3gtv.mydmam.taskqueue.Profile;
import hd3gtv.mydmam.taskqueue.Worker;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

@SuppressWarnings("unchecked")
public class MetadataRendererWorker extends Worker {
	
	static final String PROFILE_CATEGORY = "metadata-rendering";
	
	private MetadataCenter metadata_center;
	private MetadataIndexerWorker metadataindexerworker;
	
	public MetadataRendererWorker(MetadataIndexerWorker metadataindexerworker) {
		this.metadataindexerworker = metadataindexerworker;
		if (metadataindexerworker == null) {
			throw new NullPointerException("\"metadataindexerworker\" can't to be null");
		}
		if (metadataindexerworker.isConfigurationAllowToEnabled() == false) {
			return;
		}
		metadata_center = metadataindexerworker.getMetadata_center();
		if (metadata_center == null) {
			return;
		}
	}
	
	public List<Profile> getManagedProfiles() {
		LinkedHashMap<String, Renderer> renderers = metadata_center.getRenderers();
		if (renderers == null) {
			return null;
		}
		if (renderers.isEmpty()) {
			return null;
		}
		List<Profile> profiles = new ArrayList<Profile>();
		for (Map.Entry<String, Renderer> entry : renderers.entrySet()) {
			if (entry.getValue() instanceof RendererViaWorker) {
				profiles.add(createProfile((RendererViaWorker) entry.getValue()));
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
		return metadataindexerworker.isConfigurationAllowToEnabled();
	}
	
	private static Profile createProfile(RendererViaWorker renderer) {
		if (renderer == null) {
			throw new NullPointerException("\"renderer\" can't to be null");
		}
		return new Profile(PROFILE_CATEGORY, renderer.getElasticSearchIndexType());
	}
	
	public static String createTask(String origin_key, String name, JSONObject renderer_context, RendererViaWorker renderer, String require_task) throws ConnectionException {
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
		return Broker.publishTask(name, createProfile(renderer), context, MetadataRendererWorker.class, false, 0, require_task, false);
	}
	
	public static String createTask(String origin_key, String name, JSONObject renderer_context, RendererViaWorker renderer) throws ConnectionException {
		return createTask(origin_key, name, renderer_context, renderer, null);
	}
	
	private volatile RendererViaWorker current_renderer;
	
	public void process(Job job) throws Exception {
		current_renderer = null;
		
		JSONObject context = job.getContext();
		if (context == null) {
			throw new NullPointerException("No context");
		}
		if (context.isEmpty()) {
			throw new NullPointerException("No context");
		}
		LinkedHashMap<String, Renderer> renderers = metadata_center.getRenderers();
		if (renderers == null) {
			throw new NullPointerException("No declared metadatas renderers");
		}
		Renderer _renderer = renderers.get(job.getProfile().getName());
		if (_renderer == null) {
			throw new NullPointerException("Can't found declared rendrerer: \"" + job.getProfile().getName() + "\"");
		}
		if ((_renderer instanceof RendererViaWorker) == false) {
			throw new NullPointerException("Invalid rendrerer: \"" + job.getProfile().getName() + "\"");
		}
		current_renderer = (RendererViaWorker) _renderer;
		
		if (context.containsKey("origin") == false) {
			throw new NullPointerException("No origin file !");
		}
		String origin_key = (String) context.get("origin");
		
		Explorer explorer = new Explorer();
		SourcePathIndexerElement element = explorer.getelementByIdkey(origin_key);
		if (element == null) {
			throw new NullPointerException("Can't found origin element: " + origin_key);
		}
		
		File origin = explorer.getLocalBridgedElement(element);
		if (origin == null) {
			throw new NullPointerException("Can't bridge with real file origin element: " + origin_key);
		}
		if (origin.exists() == false) {
			throw new FileNotFoundException(origin.getPath());
		}
		
		List<RenderedElement> rendered_elements = null;
		if (context.containsKey("renderer")) {
			rendered_elements = current_renderer.standaloneProcess(origin, job, (JSONObject) context.get("renderer"));
		} else {
			rendered_elements = current_renderer.standaloneProcess(origin, job, null);
		}
		
		if (rendered_elements == null) {
			current_renderer = null;
			return;
		}
		if (rendered_elements.isEmpty()) {
			current_renderer = null;
			return;
		}
		
		MetadataIndexer.merge(current_renderer, rendered_elements, element, current_renderer.getElasticSearchIndexType());
		
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
