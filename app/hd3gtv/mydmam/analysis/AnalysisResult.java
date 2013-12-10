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

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.json.simple.JSONObject;

public class AnalysisResult implements Log2Dumpable {
	
	File origin;
	String mimetype;
	LinkedHashMap<Analyser, JSONObject> processing_results;
	
	AnalysisResult() {
	}
	
	public String getMimetype() {
		return mimetype;
	}
	
	public File getOrigin() {
		return origin;
	}
	
	public LinkedHashMap<Analyser, JSONObject> getProcessing_results() {
		return processing_results;
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("origin", origin);
		dump.add("mimetype", mimetype);
		if (processing_results != null) {
			for (Map.Entry<Analyser, JSONObject> entry : processing_results.entrySet()) {
				dump.add(entry.getKey().getName() + "/summary", entry.getKey().getSummary(entry.getValue()));
				dump.add(entry.getKey().getName() + "/full", json_prettify(entry.getValue()));
			}
		}
		return dump;
	}
	
	public static String json_prettify(JSONObject json) {
		ObjectMapper mapper = new ObjectMapper();
		// MyClass myObject = mapper.readValue( new FileReader("input.json"), MyClass.class);
		ObjectWriter writer = mapper.writer().withDefaultPrettyPrinter();
		try {
			return writer.writeValueAsString(json);
		} catch (Exception e) {
			e.printStackTrace();
			return json.toJSONString();
		}
	}
}
