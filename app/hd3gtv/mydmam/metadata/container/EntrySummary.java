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

import hd3gtv.mydmam.metadata.GeneratorRenderer;
import hd3gtv.mydmam.metadata.PreviewType;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

public final class EntrySummary extends Entry {
	
	private LinkedHashMap<String, Preview> previews;
	Map<String, String> summaries;
	public boolean master_as_preview;
	private String mimetype;
	private transient LinkedHashMap<PreviewType, Preview> cache_previews;
	
	public final static String type = "summary";
	
	public String getES_Type() {
		return type;
	}
	
	public String getMimetype() {
		return mimetype;
	}
	
	public void setMimetype(String mimetype) {
		if (mimetype == null) {
			return;
		}
		if (mimetype.equals("")) {
			return;
		}
		this.mimetype = mimetype;
	}
	
	public boolean equalsMimetype(String... mime) {
		if (mime == null) {
			return false;
		}
		for (int pos = 0; pos < mime.length; pos++) {
			if (mime[pos].equalsIgnoreCase(mimetype)) {
				return true;
			}
		}
		return false;
	}
	
	/*
	 * Start serializers functions.
	 * */
	
	protected Entry create() {
		return new EntrySummary();
	}
	
	public static final String MASTER_AS_PREVIEW = "master_as_preview";
	
	protected void internalDeserialize(Entry _entry, JsonObject source, Gson gson) {
		EntrySummary entry = (EntrySummary) _entry;
		for (Map.Entry<String, JsonElement> item : source.entrySet()) {
			if (item.getKey().equals("previews")) {
				Type typeOfT = new TypeToken<HashMap<String, Preview>>() {
				}.getType();
				entry.previews = gson.fromJson(item.getValue().getAsJsonObject(), typeOfT);
			} else if (item.getKey().equals("mimetype")) {
				entry.mimetype = item.getValue().getAsString();
			} else if (item.getKey().equals(MASTER_AS_PREVIEW)) {
				entry.master_as_preview = item.getValue().getAsBoolean();
			} else {
				if (entry.summaries == null) {
					entry.summaries = new HashMap<String, String>();
				}
				entry.summaries.put(item.getKey(), item.getValue().getAsString());
			}
		}
	}
	
	protected JsonObject internalSerialize(Entry _item, Gson gson) {
		EntrySummary src = (EntrySummary) _item;
		JsonObject jo = new JsonObject();
		
		if (src.previews != null) {
			jo.add("previews", gson.toJsonTree(src.previews));
		}
		
		jo.addProperty("mimetype", src.mimetype);
		jo.addProperty("master_as_preview", src.master_as_preview);
		if (src.summaries != null) {
			for (Map.Entry<String, String> entry : src.summaries.entrySet()) {
				jo.addProperty(entry.getKey(), entry.getValue());
			}
		}
		return jo;
	}
	
	protected List<Class<? extends SelfSerializing>> getSerializationDependencies() {
		return null;
	}
	
	public void putSummaryContent(EntryAnalyser entry, String value) {
		if (summaries == null) {
			summaries = new HashMap<String, String>();
		}
		summaries.put(entry.getES_Type(), value);
	}
	
	public Map<String, String> getSummaries() {
		if (summaries == null) {
			summaries = new HashMap<String, String>();
		}
		return summaries;
	}
	
	private void populate_previews() {
		if (previews == null) {
			previews = new LinkedHashMap<String, Preview>(1);
		}
		if (cache_previews == null) {
			cache_previews = new LinkedHashMap<PreviewType, Preview>();
			for (Map.Entry<String, Preview> entry : previews.entrySet()) {
				cache_previews.put(PreviewType.valueOf(entry.getKey()), entry.getValue());
			}
		}
	}
	
	public void addPreviewsFromEntryRenderer(EntryRenderer entry, Container container, GeneratorRenderer generator) {
		populate_previews();
		
		List<String> files = entry.getContentFileNames();
		
		for (int pos = 0; pos < files.size(); pos++) {
			Preview preview = new Preview();
			preview.type = entry.getES_Type();
			preview.file = files.get(pos);
			
			PreviewType previewtype = generator.getPreviewTypeForRenderer(container, entry);
			if (previewtype != null) {
				cache_previews.put(previewtype, preview);
				previews.put(previewtype.toString(), preview);
			}
		}
	}
	
	public Preview getPreview(PreviewType previewtype) {
		populate_previews();
		if (cache_previews.containsKey(previewtype)) {
			return cache_previews.get(previewtype);
		}
		return null;
	}
	
	/**
	 * Don't add or delete items from here.
	 */
	public LinkedHashMap<PreviewType, Preview> getPreviews() {
		populate_previews();
		return cache_previews;
	}
	
}
