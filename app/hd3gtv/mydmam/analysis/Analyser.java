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

import java.util.List;

import org.json.simple.JSONObject;

public interface Analyser extends MetadataProvider {
	
	public static final String METADATA_PROVIDER_ANALYSER = "analyser";
	
	String getSummary(JSONObject processresult);
	
	JSONObject process(MetadataIndexerResult analysis_result) throws Exception;
	
	List<String> getMimeFileListCanUsedInMasterAsPreview();
	
	boolean isCanUsedInMasterAsPreview(MetadataIndexerResult analysis_result);
	
}
