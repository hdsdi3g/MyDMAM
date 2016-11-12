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
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import hd3gtv.tools.GsonIgnore;

public class ACFile implements ACAPIResult {
	
	public String self;
	public int max = 0;
	public int offset = 0;
	public String sort;
	public ACOrder order;
	public String share;
	public String path;
	public long creationDate = 0;
	public long modificationDate = 0;
	public ACFileType type;
	public int count = 0;
	public int size = 0;
	
	public long date_min = 0;
	public long date_max = 0;
	
	public ACQuotas quota;
	
	@GsonIgnore
	public ACItemLocations sub_locations;
	
	@GsonIgnore
	public ArrayList<ACFileLocations> this_locations;
	
	/**
	 * Only for file
	 */
	public ACAccessibility accessibility;
	
	public ACLocationType bestLocation;
	
	@GsonIgnore
	public ArrayList<String> files;
	
	ACFile() {
	}
	
	static class Deseralizer implements JsonDeserializer<ACFile> {
		Type type_AL_String = new TypeToken<ArrayList<String>>() {
		}.getType();
		
		Type type_AL_ACFileLocations = new TypeToken<ArrayList<ACFileLocations>>() {
		}.getType();
		
		ACAPI acapi;
		
		public Deseralizer(ACAPI acapi) {
			this.acapi = acapi;
		}
		
		public ACFile deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			ACFile node = acapi.gson_simple.fromJson(json, ACFile.class);
			JsonObject jo = json.getAsJsonObject();
			if (node.type == ACFileType.directory) {
				node.files = acapi.gson_simple.fromJson(jo.get("files"), type_AL_String);
				node.sub_locations = acapi.gson.fromJson(jo.get("locations"), ACItemLocations.class);
			} else if (node.type == ACFileType.file) {
				node.this_locations = acapi.gson.fromJson(jo.get("locations"), type_AL_ACFileLocations);
			} else {
				throw new NullPointerException("node");
			}
			return node;
		}
	}
	
}
