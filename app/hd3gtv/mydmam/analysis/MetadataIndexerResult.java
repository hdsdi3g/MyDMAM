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
package hd3gtv.mydmam.analysis;

import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class MetadataIndexerResult implements Log2Dumpable {
	
	File origin;
	String mimetype;
	LinkedHashMap<Analyser, JSONObject> analysis_results;
	LinkedHashMap<Renderer, List<RenderedElement>> rendering_results;
	boolean master_as_preview;
	SourcePathIndexerElement reference;
	
	MetadataIndexerResult(SourcePathIndexerElement reference) {
		this.reference = reference;
		if (reference == null) {
			throw new NullPointerException("\"reference\" can't to be null");
		}
		analysis_results = new LinkedHashMap<Analyser, JSONObject>();
		rendering_results = new LinkedHashMap<Renderer, List<RenderedElement>>();
	}
	
	public String getMimetype() {
		return mimetype;
	}
	
	public SourcePathIndexerElement getReference() {
		return reference;
	}
	
	public void changeMimetype(String mimetype) {
		if (mimetype == null) {
			return;
		}
		if (mimetype.equals("")) {
			return;
		}
		this.mimetype = mimetype;
	}
	
	public boolean equalsMimetype(String... mime) {
		if (mime == null) {
			return false;
		}
		for (int pos = 0; pos < mime.length; pos++) {
			if (mime[pos].equalsIgnoreCase(mimetype)) {
				return true;
			}
		}
		return false;
	}
	
	public File getOrigin() {
		return origin;
	}
	
	public LinkedHashMap<Analyser, JSONObject> getAnalysis_results() {
		return analysis_results;
	}
	
	public LinkedHashMap<Renderer, List<RenderedElement>> getRendering_results() {
		return rendering_results;
	}
	
	static LinkedHashMap<Renderer, JSONArray> makeJSONRendering_results(LinkedHashMap<Renderer, List<RenderedElement>> rendering_results) {
		if (rendering_results == null) {
			return null;
		}
		if (rendering_results.isEmpty()) {
			return null;
		}
		List<RenderedElement> rendered;
		LinkedHashMap<Renderer, JSONArray> result = new LinkedHashMap<Renderer, JSONArray>();
		
		for (Map.Entry<Renderer, List<RenderedElement>> entry : rendering_results.entrySet()) {
			rendered = entry.getValue();
			
			JSONArray ja_files = new JSONArray();
			for (int pos = 0; pos < rendered.size(); pos++) {
				if (rendered.get(pos).isConsolidated() == false) {
					continue;
				}
				ja_files.add(rendered.get(pos).toDatabase());
			}
			if (ja_files.isEmpty()) {
				continue;
			}
			result.put(entry.getKey(), ja_files);
		}
		return result;
	}
	
	LinkedHashMap<Renderer, JSONArray> makeJSONRendering_results() {
		return makeJSONRendering_results(rendering_results);
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("origin", origin);
		dump.add("mimetype", mimetype);
		if (rendering_results != null) {
			for (Map.Entry<Analyser, JSONObject> entry : analysis_results.entrySet()) {
				dump.add(entry.getKey().getName() + "/summary", entry.getKey().getSummary(entry.getValue()));
				dump.add(entry.getKey().getName() + "/full", MetadataCenter.json_prettify(entry.getValue()));
			}
		}
		if (rendering_results != null) {
			for (Map.Entry<Renderer, List<RenderedElement>> entry : rendering_results.entrySet()) {
				List<RenderedElement> elements = entry.getValue();
				for (int pos = 0; pos < elements.size(); pos++) {
					dump.add(entry.getKey().getName(), elements.get(pos));
				}
			}
		}
		return dump;
	}
}
