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
package hd3gtv.mydmam.metadata;

import hd3gtv.configuration.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MasterAsPreviewProvider {
	
	private Map<String, Analyser> mime_list;
	
	MasterAsPreviewProvider() {
		if (Configuration.global.isElementExists("master_as_preview") == false) {
			return;
		}
		if (Configuration.global.getValueBoolean("master_as_preview", "enable") == false) {
			return;
		}
		mime_list = new HashMap<String, Analyser>();
		
	}
	
	void addAnalyser(Analyser analyser) {
		if (analyser == null) {
			return;
		}
		if (mime_list == null) {
			return;
		}
		List<String> list = analyser.getMimeFileListCanUsedInMasterAsPreview();
		if (list != null) {
			for (int pos = 0; pos < list.size(); pos++) {
				mime_list.put(list.get(pos).toLowerCase(), analyser);
			}
		}
	}
	
	boolean isFileIsValidForMasterAsPreview(MetadataIndexerResult metadatas_result) {
		if (mime_list == null) {
			return false;
		}
		String mime = metadatas_result.mimetype.toLowerCase();
		if (mime_list.containsKey(mime) == false) {
			return false;
		}
		return mime_list.get(mime).isCanUsedInMasterAsPreview(metadatas_result);
	}
}
