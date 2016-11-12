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
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

public class ACNodesEntry {
	ACNodesEntry() {
	}
	
	public String name;
	public String description;
	public long lastStartingTime;
	public String nodeVersion;
	public boolean hasTapeDrive;
	public String path;
	public ArrayList<InetAddress> ipAddresses;
	public int httpPort;
	public int ftpPort;
	public URL url;
	
	/**
	 * @return name
	 */
	public String toString() {
		return name;
	}
	
	static class Deseralizer implements JsonDeserializer<ACNodesEntry> {
		Type type_AL_InetAddr = new TypeToken<ArrayList<InetAddress>>() {
		}.getType();
		
		ACAPI acapi;
		
		public Deseralizer(ACAPI acapi) {
			this.acapi = acapi;
		}
		
		public ACNodesEntry deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			ACNodesEntry nodes = acapi.gson_simple.fromJson(json, ACNodesEntry.class);
			nodes.ipAddresses = acapi.gson_simple.fromJson(json.getAsJsonObject().get("ipAddresses"), type_AL_InetAddr);
			return nodes;
		}
	}
	
}
