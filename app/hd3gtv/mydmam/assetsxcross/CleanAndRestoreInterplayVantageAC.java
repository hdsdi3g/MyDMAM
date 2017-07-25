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
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

import com.avid.interplay.ws.assets.AssetDescriptionType;
import com.avid.interplay.ws.assets.AssetsFault;
import com.avid.interplay.ws.assets.AttributeListType;
import com.avid.interplay.ws.assets.SearchGroupType;
import com.avid.interplay.ws.assets.SearchResponseType;
import com.avid.interplay.ws.assets.SearchType;
import com.google.gson.JsonElement;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.assetsxcross.InterplayAPI.AssetType;
import hd3gtv.mydmam.assetsxcross.InterplayAPI.AttributeGroup;
import hd3gtv.mydmam.assetsxcross.InterplayAPI.Condition;
import hd3gtv.mydmam.assetsxcross.InterplayAPI.MediaStatus;
import hd3gtv.mydmam.assetsxcross.VantageAPI.VariableDefinition;
import hd3gtv.tools.Timecode;

public class CleanAndRestoreInterplayVantageAC {
	
	public static CleanAndRestoreInterplayVantageAC createFromConfiguration() {
		Object raw_conf = Configuration.global.getRawValue("assetsxcross", "clean_restore");
		JsonElement j_conf = MyDMAM.gson_kit.getGsonSimple().toJsonTree(raw_conf);
		return MyDMAM.gson_kit.getGsonSimple().fromJson(j_conf, CleanAndRestoreInterplayVantageAC.class);
	}
	
	private transient InterplayAPI interplay;
	
	private CleanAndRestoreInterplayVantageAC() throws IOException {
		interplay = InterplayAPI.initFromConfiguration();
	}
	
	private String mydmam_id_in_interplay;
	
	/**
	 * @return null if not found
	 */
	public InterplayAsset getAssetByMyDMAMID(String id, String search_root_path) throws AssetsFault, IOException {
		SearchType search_type = new SearchType();
		search_type.setMaxResults(100);
		
		SearchGroupType search_group_type = new SearchGroupType();
		search_group_type.setOperator("AND");
		search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.EQUALS, AttributeGroup.SYSTEM, "Type", "masterclip"));
		search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.EQUALS, AttributeGroup.USER, mydmam_id_in_interplay, id));
		search_type.setSearchGroup(search_group_type);
		
		search_type.setInterplayPathURI(interplay.createURLInterplayPath(search_root_path));
		
		SearchResponseType response = interplay.search(search_type);
		InterplayAPI.checkError(response.getErrors());
		
		List<AssetDescriptionType> asset_description_list = response.getResults().getAssetDescription();
		
		if (asset_description_list.isEmpty()) {
			return null;
		}
		
		InterplayAsset result = new InterplayAsset();
		asset_description_list.forEach(asset -> {
			result.setAttributes(asset.getInterplayURI(), asset.getAttributes());
		});
		return result;
	}
	
	public class InterplayAsset {
		private InterplayAsset() {
			display_names = new ArrayList<String>();
			paths = new ArrayList<String>();
		}
		
		private String interplay_uri;
		private String mob_id;
		private String source_id;
		private MediaStatus media_status;
		private String tc_in;
		private AssetType type;
		private String mydmam_id;
		private ArrayList<String> display_names;
		private ArrayList<String> paths;
		private String tracks;
		
		private void setAttributes(String interplay_uri, AttributeListType attr) {
			this.interplay_uri = interplay_uri;
			
			Map<String, String> attributes = InterplayAPI.getSimpleAttributeMap(attr.getAttribute());
			
			if (mob_id != null) {
				if (mob_id.equalsIgnoreCase(attributes.get("MOB ID")) == false) {
					throw new IndexOutOfBoundsException("MobID conflicts with this version");
				}
			}
			mob_id = attributes.get("MOB ID");
			
			if (source_id != null) {
				if (source_id.equalsIgnoreCase(attributes.get("Source ID")) == false) {
					throw new IndexOutOfBoundsException("Source ID conflicts with this version");
				}
			}
			source_id = attributes.get("Source ID");
			
			media_status = MediaStatus.valueOf(attributes.get("Media Status"));
			tc_in = attributes.get("Start"); // TC IN
			type = AssetType.valueOf(attributes.get("Type"));
			mydmam_id = attributes.get(mydmam_id_in_interplay);
			tracks = attributes.get("Tracks"); // V1 A1A2
			
			if (display_names.contains(attributes.get("Display Name")) == false) {
				display_names.add(attributes.get("Display Name"));
			}
			if (paths.contains(attributes.get("Path")) == false) {
				paths.add(attributes.get("Path"));
			}
		}
		
		public boolean isOnline() {
			return media_status == MediaStatus.online;
		}
		
		public boolean isMasterclip() {
			return type == AssetType.masterclip;
		}
		
		// TODO Restore a list of masterclip (only offline)
		// TODO Destage items: get archived OP1A file from AC (or local)
		
		public ArrayList<VariableDefinition> createVantageVariables(VantageAPI vantage) {
			ArrayList<VariableDefinition> vars = new ArrayList<>();
			vars.add(vantage.createVariableDef(vantage_variable_name_interplay_mastermob, mob_id));
			vars.add(vantage.createVariableDef(vantage_variable_name_interplay_sourcemob, source_id));
			vars.add(vantage.createVariableDef(vantage_variable_name_interplay_path, FilenameUtils.getFullPathNoEndSeparator(paths.get(0))));
			vars.add(vantage.createVariableDef(vantage_variable_name_interplay_file, display_names.get(0)));
			
			/**
			 * V1 A1-2
			 */
			int audio_tracks_count = Arrays.asList(tracks.split(" ")).stream().filter(t -> {
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
			
			vars.add(vantage.createVariableDef(vantage_variable_name_audio_ch, audio_tracks_count));
			vars.add(vantage.createVariableDef(vantage_variable_name_tcin, new Timecode("00:00:00:00", 25)));
			return vars;
		}
		
	}
	
	// TODO from conf
	private String vantage_variable_name_interplay_mastermob = "Interplay Master_MobID";
	private String vantage_variable_name_interplay_sourcemob = "Interplay Source_MobID";
	private String vantage_variable_name_interplay_path = "Interplay Path";
	private String vantage_variable_name_interplay_file = "File Name";
	private String vantage_variable_name_audio_ch = "Pistes Audio";
	private String vantage_variable_name_tcin = "TC-IN";
	
	// TODO purge a list of masterclip (only online)
	// TODO purge a sequence (all masterclips of this sequence)
	// TODO set white Path lists: never purge, only archive
	// TODO DoArchive Vantage job
	
	// TODO Restore a sequence (all masterclips of this sequence)
	
	// TODO DoRestore Vantage job + watch
	
	// TODO search&purge scan
	// TODO search&restore scan
	
}
