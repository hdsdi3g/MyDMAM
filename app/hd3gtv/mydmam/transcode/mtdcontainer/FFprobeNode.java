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
package hd3gtv.mydmam.transcode.mtdcontainer;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

abstract class FFprobeNode {
	
	protected JsonObject node_content;
	
	public final boolean hasMultipleParams(String... list_params) {
		for (int pos = 0; pos < list_params.length; pos++) {
			if (node_content.has(list_params[pos]) == false) {
				return false;
			}
		}
		return true;
	}
	
	public final JsonPrimitive getParam(String param_name) {
		if (node_content.has(param_name) == false) {
			return null;
		}
		return node_content.getAsJsonPrimitive(param_name);
	}
	
	public final JsonObject getTags() {
		if (node_content.has("tags") == false) {
			return new JsonObject();
		}
		return node_content.getAsJsonObject("tags");
	}
}
