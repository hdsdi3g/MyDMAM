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

import java.io.File;

import org.json.simple.JSONObject;

public interface Analyser {
	
	boolean canProcessThis(String mimetype);
	
	JSONObject process(File file) throws Exception;
	
	boolean isEnabled();
	
	String getName();
	
	String getElasticSearchIndexType();
	
	String getSummary(JSONObject processresult);
	
}
