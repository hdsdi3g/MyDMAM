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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.mydmam.metadata.container;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.gson.GsonIgnore;
import hd3gtv.mydmam.gson.GsonKit;
import hd3gtv.mydmam.metadata.RenderedFile;

public abstract class EntryRenderer extends ContainerEntry {
	
	private @GsonIgnore List<RenderedContent> content;
	private @GsonIgnore JsonObject options;
	
	public EntryRenderer() {
		options = new JsonObject();
	}
	
	public boolean isEmpty() {
		if (content == null) {
			return true;
		}
		return content.isEmpty();
	}
	
	public RenderedContent getByFile(String name) {
		if (content == null) {
			return null;
		}
		for (int pos = 0; pos < content.size(); pos++) {
			if (name.equals(content.get(pos).name)) {
				return content.get(pos);
			}
		}
		return null;
	}
	
	public void addContent(RenderedContent rendered_content) {
		if (content == null) {
			content = new ArrayList<RenderedContent>(1);
		}
		content.add(rendered_content);
	}
	
	public List<String> getContentFileNames() {
		if (content == null) {
			content = new ArrayList<RenderedContent>(1);
			return new ArrayList<String>(1);
		}
		List<String> result = new ArrayList<String>();
		for (int pos = 0; pos < content.size(); pos++) {
			result.add(content.get(pos).name);
		}
		return result;
	}
	
	public RenderedFile getRenderedFile(String name, boolean check_hash) throws IOException {
		if (content == null) {
			return null;
		}
		for (int pos = 0; pos < content.size(); pos++) {
			if (name.equals(content.get(pos).name)) {
				return RenderedFile.import_from_entry(content.get(pos), container.getMtd_key(), check_hash);
			}
		}
		return null;
	}
	
	public JsonObject getOptions() {
		return options;
	}
	
	protected final ContainerEntry internalDeserialize(JsonObject source, Gson gson) {// TODO move de/serializer
		EntryRenderer entry;
		try {
			entry = getClass().newInstance();
		} catch (Exception e) {
			Loggers.Metadata.error("Can't instanciate this Entry", e);
			return null;
		}
		entry.content = gson.fromJson(source.get("content").getAsJsonArray(), GsonKit.type_List_RenderedContent);
		entry.options = source.get("options").getAsJsonObject();
		return entry;
	}
	
	protected final JsonObject internalSerialize(ContainerEntry _item, Gson gson) {// TODO move de/serializer
		EntryRenderer src = (EntryRenderer) _item;
		JsonObject jo = new JsonObject();
		jo.add("options", src.options);
		jo.add("content", gson.toJsonTree(src.content));
		return jo;
	}
	
}
