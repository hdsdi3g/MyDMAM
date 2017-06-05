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
package hd3gtv.mydmam.metadata;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.EntryAnalyser;
import hd3gtv.mydmam.metadata.container.EntryRenderer;

public final class ContainerEntryResult {
	
	private EntryRenderer rendering_result;
	private EntryAnalyser analyst_result;
	
	public ContainerEntryResult(EntryRenderer rendering_result, EntryAnalyser analyst_result) {
		this.rendering_result = rendering_result;
		this.analyst_result = analyst_result;
	}
	
	public ContainerEntryResult(EntryAnalyser analyst_result) {
		this.analyst_result = analyst_result;
	}
	
	/**
	 * You NEED to consolidate rendered elements.
	 * Call RenderedFile.export_to_entry() for populate in EntryRenderer
	 */
	public ContainerEntryResult(EntryRenderer rendering_result) {
		this.rendering_result = rendering_result;
	}
	
	public EntryAnalyser getAnalystResult() {
		return analyst_result;
	}
	
	public EntryRenderer getRenderingResult() {
		return rendering_result;
	}
	
	void addEntriesToContainer(Container container) {
		if (analyst_result != null) {
			container.addEntry(analyst_result);
		}
		if (rendering_result != null) {
			container.addEntry(rendering_result);
		}
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ContainerEntryResult ");
		
		if (analyst_result != null) {
			sb.append("analyst [");
			sb.append(analyst_result.getES_Type());
			sb.append("] ");
			sb.append(MyDMAM.gson_kit.getGson().toJson(analyst_result));
		}
		
		if (analyst_result != null && rendering_result != null) {
			sb.append("; ");
		}
		
		if (rendering_result != null) {
			sb.append("rendering [");
			sb.append(rendering_result.getES_Type());
			sb.append("] ");
			sb.append(MyDMAM.gson_kit.getGson().toJson(rendering_result));
		}
		
		return sb.toString();
	}
	
}
