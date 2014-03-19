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
package hd3gtv.mydmam.analysis;

import java.util.LinkedHashMap;
import java.util.List;

import org.json.simple.JSONObject;

public interface Renderer extends MetadataProvider {
	
	public static final String METADATA_PROVIDER_RENDERER = "renderer";
	
	public static final String METADATA_PROVIDER_RENDERER_CONTENT = "content";
	
	/**
	 * You don't need to consolidate rendered elements
	 */
	List<RenderedElement> process(MetadataIndexerResult analysis_result) throws Exception;
	
	String getElasticSearchIndexType();
	
	/**
	 * @param rendered_elements never null, never empty.
	 * @return JS parser name for display this render, or null.
	 */
	PreviewType getPreviewTypeForRenderer(LinkedHashMap<String, JSONObject> all_metadatas_for_element, List<RenderedElement> rendered_elements);
	
	/**
	 * @return Data to send to JS parser for display this render, or null.
	 */
	JSONObject getPreviewConfigurationForRenderer(PreviewType preview_type, LinkedHashMap<String, JSONObject> all_metadatas_for_element, List<RenderedElement> rendered_elements);
}
