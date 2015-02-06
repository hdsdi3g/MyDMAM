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

import hd3gtv.log2.Log2;
import hd3gtv.mydmam.metadata.RenderedFile;
import hd3gtv.tools.GsonIgnore;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public abstract class EntryRenderer extends Entry {
	
	private @GsonIgnore List<RenderedContent> content;
	private @GsonIgnore JsonObject options;
	
	public EntryRenderer() {
		options = new JsonObject();
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
	
	protected final List<Class<? extends SelfSerializing>> getSerializationDependencies() {
		return null;
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
	
	private Type type_l_RenderedContent_OfT = new TypeToken<List<RenderedContent>>() {
	}.getType();
	
	public JsonObject getOptions() {
		return options;
	}
	
	protected final Entry internalDeserialize(JsonObject source, Gson gson) {
		EntryRenderer entry;
		try {
			entry = getClass().newInstance();
		} catch (Exception e) {
			Log2.log.error("Can't instanciate this Entry", e);
			return null;
		}
		entry.content = gson.fromJson(source.get("content").getAsJsonArray(), type_l_RenderedContent_OfT);
		entry.options = source.get("options").getAsJsonObject();
		return entry;
	}
	
	protected final JsonObject internalSerialize(Entry _item, Gson gson) {
		EntryRenderer src = (EntryRenderer) _item;
		JsonObject jo = new JsonObject();
		jo.add("options", src.options);
		jo.add("content", gson.toJsonTree(src.content));
		return jo;
	}
}
