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

import com.google.gson.JsonObject;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonIgnore;
import hd3gtv.mydmam.gson.GsonKit;
import hd3gtv.mydmam.metadata.RenderedFile;

public final class EntryRenderer extends ContainerEntry {
	
	@GsonIgnore
	private List<RenderedContent> content;
	@GsonIgnore
	private JsonObject options;
	
	private transient String es_type;
	
	private EntryRenderer() {
		options = new JsonObject();
	}
	
	/**
	 * Don't forget to declare type in ContainerOperations static block.
	 * @param type ES type
	 */
	public EntryRenderer(String type) {
		options = new JsonObject();
		if (type == null) {
			throw new NullPointerException("\"type\" can't to be null");
		}
		es_type = type;
	}
	
	public String getES_Type() {
		return es_type;
	}
	
	public EntryRenderer setESType(String type) {
		if (type == null) {
			throw new NullPointerException("\"type\" can't to be null");
		}
		es_type = type;
		return this;
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
	
	public static class Serializer extends ContainerEntryDeSerializer<EntryRenderer> {
		
		protected EntryRenderer internalDeserialize(JsonObject source) {// TODO method for patch ES Type value after deserialize...
			EntryRenderer entry = new EntryRenderer();
			entry.content = MyDMAM.gson_kit.getGson().fromJson(source.get("content").getAsJsonArray(), GsonKit.type_List_RenderedContent);
			entry.options = source.get("options").getAsJsonObject();
			return entry;
		}
		
		protected JsonObject internalSerialize(EntryRenderer item) {
			JsonObject jo = new JsonObject();
			jo.add("options", item.options);
			jo.add("content", MyDMAM.gson_kit.getGson().toJsonTree(item.content));
			return jo;
		}
		
	}
	
}
