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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam.analysis;

import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.simple.JSONObject;

public class AnalysisResult implements Log2Dumpable {
	
	File origin;
	String mimetype;
	LinkedHashMap<MetadataProvider, JSONObject> processing_results;
	
	AnalysisResult() {
	}
	
	public String getMimetype() {
		return mimetype;
	}
	
	public File getOrigin() {
		return origin;
	}
	
	public LinkedHashMap<MetadataProvider, JSONObject> getProcessing_results() {
		return processing_results;
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("origin", origin);
		dump.add("mimetype", mimetype);
		if (processing_results != null) {
			for (Map.Entry<MetadataProvider, JSONObject> entry : processing_results.entrySet()) {
				if (entry.getKey() instanceof Analyser) {
					dump.add(entry.getKey().getName() + "/summary", ((Analyser) entry.getKey()).getSummary(entry.getValue()));
				}
				dump.add(entry.getKey().getName() + "/full", MetadataCenter.json_prettify(entry.getValue()));
			}
		}
		return dump;
	}
	
}
