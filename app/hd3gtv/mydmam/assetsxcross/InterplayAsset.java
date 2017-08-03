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
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
*/
package hd3gtv.mydmam.assetsxcross;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import com.avid.interplay.ws.assets.AssetsFault;
import com.avid.interplay.ws.assets.AttributeType;

import hd3gtv.mydmam.assetsxcross.InterplayAPI.AssetType;
import hd3gtv.mydmam.assetsxcross.InterplayAPI.MediaStatus;

public class InterplayAsset {
	
	private transient InterplayAPI interplay_api;
	public final String interplay_uri;
	/**
	 * unmodifiableList
	 */
	private final List<AttributeType> attributes;
	private LinkedHashMap<String, String> attributes_map;
	
	InterplayAsset(InterplayAPI interplay_api, String interplay_uri, List<AttributeType> attributes) {
		this.interplay_api = interplay_api;
		if (interplay_api == null) {
			throw new NullPointerException("\"interplay_api\" can't to be null");
		}
		this.interplay_uri = interplay_uri;
		if (interplay_uri == null) {
			throw new NullPointerException("\"interplay_uri\" can't to be null");
		}
		if (attributes != null) {
			this.attributes = Collections.unmodifiableList(attributes);
		} else {
			this.attributes = Collections.unmodifiableList(Collections.emptyList());
		}
	}
	
	public String getAttribute(String name, String default_value) {
		if (attributes_map == null) {
			attributes_map = new LinkedHashMap<>(attributes.size() + 1);
			
			attributes.stream().filter(attr -> {
				return attr.getGroup().equalsIgnoreCase(InterplayAPI.AttributeGroup.SYSTEM.name());
			}).forEach(attr -> {
				attributes_map.put(attr.getName(), attr.getValue());
			});
			
			attributes.stream().filter(attr -> {
				return attr.getGroup().equalsIgnoreCase(InterplayAPI.AttributeGroup.SYSTEM.name()) == false;
			}).forEach(attr -> {
				attributes_map.putIfAbsent(attr.getName(), attr.getValue());
			});
		}
		return attributes_map.getOrDefault(name, default_value);
	}
	
	public String getAttribute(String name) {
		return getAttribute(name, null);
	}
	
	public String getMobID() {
		return getAttribute("MOB ID", null);
	}
	
	public String getSourceID() {
		return getAttribute("Source ID", null);
	}
	
	public MediaStatus getMediaStatus() {
		return MediaStatus.valueOf(getAttribute("Media Status", null));
	}
	
	/**
	 * @return TC IN
	 */
	public String getStart() {
		return getAttribute("Start", null);
	}
	
	public AssetType getType() {
		return AssetType.valueOf(getAttribute("Type", null));
	}
	
	public String getMyDMAMID() {
		return getAttribute(interplay_api.getMydmamIDinInterplay(), null);
	}
	
	public String getTracks() {
		return getAttribute("Tracks", "");
	}
	
	public String getDisplayName() {
		return getAttribute("Display Name", null);
	}
	
	public String getPath() {
		return getAttribute("Path", null);
	}
	
	private int audio_tracks_count = -1;
	
	public int getAudioTracksCount() {
		if (audio_tracks_count == -1) {
			audio_tracks_count = Arrays.asList(getTracks().split(" ")).stream().filter(t -> {
				return t.startsWith("A");
			}).findFirst().map(t -> {
				/**
				 * A1-2
				 */
				String val = t.substring(1);
				String[] vals = val.split("-");
				return Arrays.asList(vals).get(vals.length - 1);
			}).map(t -> {
				return Integer.valueOf(t);
			}).orElse(0);
		}
		return audio_tracks_count;
	}
	
	public boolean hasVideoTrack() {
		return Arrays.asList(getTracks().split(" ")).stream().anyMatch(t -> {
			return t.startsWith("V");
		});
	}
	
	/**
	 * Beware: sequences can return partial (it's not online but maybe totally usable w/o renders)...
	 */
	public boolean isOnline() {
		return MediaStatus.online.equals(getMediaStatus());
	}
	
	public boolean isMasterclip() {
		return AssetType.masterclip.equals(getType());
	}
	
	public boolean isSequence() {
		return AssetType.sequence.equals(getType());
	}
	
	public List<InterplayAsset> getRelatives(boolean keep_only_masterclips_and_sequences, String... attributes) throws AssetsFault, IOException {
		List<AttributeType> attr = new ArrayList<>();
		attr.add(InterplayAPI.createAttribute(InterplayAPI.AttributeGroup.SYSTEM, "Type", ""));
		attr.add(InterplayAPI.createAttribute(InterplayAPI.AttributeGroup.SYSTEM, "Media Status", ""));
		attr.add(InterplayAPI.createAttribute(InterplayAPI.AttributeGroup.SYSTEM, "Tracks", ""));
		attr.add(InterplayAPI.createAttribute(InterplayAPI.AttributeGroup.SYSTEM, "MOB ID", ""));
		attr.add(InterplayAPI.createAttribute(InterplayAPI.AttributeGroup.SYSTEM, "Source ID", ""));
		attr.add(InterplayAPI.createAttribute(InterplayAPI.AttributeGroup.SYSTEM, "Start", ""));
		attr.add(InterplayAPI.createAttribute(InterplayAPI.AttributeGroup.SYSTEM, "Duration", ""));
		attr.add(InterplayAPI.createAttribute(InterplayAPI.AttributeGroup.SYSTEM, "Path", ""));
		attr.add(InterplayAPI.createAttribute(InterplayAPI.AttributeGroup.USER, "Display Name", ""));
		attr.add(InterplayAPI.createAttribute(InterplayAPI.AttributeGroup.USER, interplay_api.getMydmamIDinInterplay(), ""));
		
		Arrays.asList(attributes).forEach(external_attr -> {
			if (attr.stream().map(itp_a -> {
				return itp_a.getName();
			}).anyMatch(itp_a_n -> {
				return itp_a_n.equalsIgnoreCase(external_attr);
			}) == false) {
				attr.add(InterplayAPI.createAttribute(InterplayAPI.AttributeGroup.USER, external_attr, ""));
			}
		});
		
		List<InterplayAsset> relatives = interplay_api.getRelatives(interplay_uri, attr);
		
		if (keep_only_masterclips_and_sequences) {
			relatives.removeIf(asset -> {
				return asset.isMasterclip() == false & asset.isSequence() == false;
			});
		}
		
		return relatives;
	}
	
	/*	public String get() {
		return getAttribute("", null);
	}
	*/
	
}
