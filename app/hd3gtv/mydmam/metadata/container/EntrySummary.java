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

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import hd3gtv.mydmam.metadata.ContainerEntryResult;
import hd3gtv.mydmam.metadata.MetadataExtractor;
import hd3gtv.mydmam.metadata.PreviewType;

public final class EntrySummary extends ContainerEntry {
	
	private HashMap<String, ContainerPreview> containerPreviews;
	Map<String, String> summaries;
	public boolean master_as_preview;
	private String mimetype;
	private transient HashMap<PreviewType, ContainerPreview> cache_previews;
	
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
	
	public static final String MASTER_AS_PREVIEW = "master_as_preview";
	
	// public static
	
	protected ContainerEntry internalDeserialize(JsonObject source, Gson gson) {// TODO move de/serializer
		EntrySummary entry = new EntrySummary();
		for (Map.Entry<String, JsonElement> item : source.entrySet()) {
			if (item.getKey().equals("previews")) {
				Type typeOfT = new TypeToken<HashMap<String, ContainerPreview>>() {
				}.getType();
				entry.containerPreviews = gson.fromJson(item.getValue().getAsJsonObject(), typeOfT);
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
		return entry;
	}
	
	protected JsonObject internalSerialize(ContainerEntry _item, Gson gson) {// TODO move de/serializer
		EntrySummary src = (EntrySummary) _item;
		JsonObject jo = new JsonObject();
		
		if (src.containerPreviews != null) {
			jo.add("previews", gson.toJsonTree(src.containerPreviews));
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
		if (containerPreviews == null) {
			containerPreviews = new HashMap<String, ContainerPreview>(1);
		}
		if (cache_previews == null) {
			cache_previews = new HashMap<PreviewType, ContainerPreview>();
			for (Map.Entry<String, ContainerPreview> entry : containerPreviews.entrySet()) {
				cache_previews.put(PreviewType.valueOf(entry.getKey()), entry.getValue());
			}
		}
	}
	
	public void addPreviewsFromEntryRenderer(ContainerEntryResult result_entries, Container container, MetadataExtractor generator) {
		if (result_entries == null) {
			return;
		}
		EntryRenderer entry = result_entries.getRenderingResult();
		if (entry == null) {
			return;
		}
		if (generator == null) {
			throw new NullPointerException("\"generator\" can't to be null");
		}
		if (container == null) {
			throw new NullPointerException("\"container\" can't to be null");
		}
		
		populate_previews();
		
		List<String> files = entry.getContentFileNames();
		
		for (int pos = 0; pos < files.size(); pos++) {
			ContainerPreview containerPreview = new ContainerPreview();
			containerPreview.type = entry.getES_Type();
			containerPreview.file = files.get(pos);
			containerPreview.options = entry.getOptions();
			
			PreviewType previewtype = generator.getPreviewTypeForRenderer(container, entry);
			if (previewtype != null) {
				cache_previews.put(previewtype, containerPreview);
				containerPreviews.put(previewtype.toString(), containerPreview);
			}
		}
	}
	
	public ContainerPreview getPreview(PreviewType previewtype) {
		populate_previews();
		if (cache_previews.containsKey(previewtype)) {
			return cache_previews.get(previewtype);
		}
		return null;
	}
	
	/**
	 * Don't add or delete items from here.
	 */
	public HashMap<PreviewType, ContainerPreview> getPreviews() {
		populate_previews();
		return cache_previews;
	}
	
}
