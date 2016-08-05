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
package hd3gtv.mydmam.transcode.mtdcontainer;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import hd3gtv.mydmam.metadata.container.ContainerEntry;
import hd3gtv.mydmam.metadata.container.EntryAnalyser;
import hd3gtv.mydmam.metadata.container.SelfSerializing;
import uk.co.bbc.rd.bmx.Bmx;

public class BBCBmx extends EntryAnalyser {
	
	private Bmx bmx;
	
	protected void extendedInternalSerializer(JsonObject current_element, EntryAnalyser _item, Gson gson) {
	}
	
	public String getES_Type() {
		return "bbc_bmx";
	}
	
	protected List<Class<? extends SelfSerializing>> getSerializationDependencies() {
		return null;
	}
	
	protected ContainerEntry internalDeserialize(JsonObject source, Gson gson) {
		return null;
	}
	
	public Bmx getBmx() {
		return bmx;
	}
	
	public void setBmx(Bmx bmx) {
		this.bmx = bmx;
	}
	
}
