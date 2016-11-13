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
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
*/
package hd3gtv.archivecircleapi;

import java.lang.reflect.Type;
import java.util.ArrayList;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import hd3gtv.tools.GsonIgnore;

public class ACNode implements ACAPIResult {
	
	public String self;
	public int max = 0;
	public int offset = 0;
	public String sort;
	public ACOrder order;
	public long date_min = 0;
	public long date_max = 0;
	public String search_path;
	public int count = 0;
	public int size = 0;
	
	@GsonIgnore
	public ArrayList<ACNodesEntry> nodes;
	
	ACNode() {
	}
	
	static class Deseralizer implements JsonDeserializer<ACNode> {
		Type type_AL_ACNodesEntry = new TypeToken<ArrayList<ACNodesEntry>>() {
		}.getType();
		
		ACAPI acapi;
		
		public Deseralizer(ACAPI acapi) {
			this.acapi = acapi;
		}
		
		public ACNode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			ACNode node = acapi.gson_simple.fromJson(json, ACNode.class);
			node.nodes = acapi.gson.fromJson(json.getAsJsonObject().get("nodes"), type_AL_ACNodesEntry);
			return node;
		}
	}
	
}
