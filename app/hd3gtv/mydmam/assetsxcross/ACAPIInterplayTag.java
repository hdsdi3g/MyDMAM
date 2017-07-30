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
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;

import com.avid.interplay.ws.assets.AssetDescriptionType;
import com.avid.interplay.ws.assets.AssetsFault;
import com.avid.interplay.ws.assets.AttributeConditionType;
import com.avid.interplay.ws.assets.AttributeType;
import com.avid.interplay.ws.assets.SearchResponseType;
import com.google.gson.JsonElement;

import hd3gtv.archivecircleapi.ACAPI;
import hd3gtv.archivecircleapi.ACFile;
import hd3gtv.archivecircleapi.ACNode;
import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.assetsxcross.InterplayAPI.AttributeGroup;
import hd3gtv.mydmam.assetsxcross.InterplayAPI.Condition;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;

public class ACAPIInterplayTag {
	
	public static boolean isConfigured() {
		return Configuration.global.isElementKeyExists("assetsxcross", "interplay_tag");
	}
	
	public static ACAPIInterplayTag createFromConfiguration() {
		Object raw_conf = Configuration.global.getRawValue("assetsxcross", "interplay_tag");
		JsonElement j_conf = MyDMAM.gson_kit.getGsonSimple().toJsonTree(raw_conf);
		return MyDMAM.gson_kit.getGsonSimple().fromJson(j_conf, ACAPIInterplayTag.class);
	}
	
	private class ConfigurableCondition {
		private Condition condition;
		private AttributeGroup group;
		private String name;
		private String value;
		
		public String toString() {
			return condition.name() + " " + name + ": " + value;
		}
		
		private AttributeConditionType getAttributeCondition() {
			return InterplayAPI.createAttributeCondition(condition, group, name, value);
		}
	}
	
	private transient final Explorer explorer;
	
	private ArrayList<String> interplay_paths;
	private ArrayList<ConfigurableCondition> search_conditions;
	private String ac_locations_in_interplay;
	private String ac_path_in_interplay;
	private String ac_share;
	private String storage_name;
	private int bulk_size;
	
	private transient ACAPI ac;
	private transient InterplayAPI interplay;
	
	private ACAPIInterplayTag() {
		explorer = new Explorer();
		
		/**
		 * Init ACAPI
		 */
		ACAPI ac = ACAPI.loadFromConfiguration();
		if (ac == null) {
			throw new RuntimeException(new IOException("ACAPI is not configured"));
		}
		ACNode node = ac.getNode();
		if (node == null) {
			throw new RuntimeException(new IOException("Can't init ACAPI"));
		}
		
		/**
		 * Init Interplay WS
		 */
		try {
			interplay = InterplayAPI.initFromConfiguration();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * @param verbose_found_no_id display a message if an Interplay asset is ready for tag, but it don't have valid ID.
	 */
	public void process(boolean verbose_found_noid) throws IOException, AssetsFault {
		/**
		 * Set Interplay search result display
		 */
		ArrayList<AttributeType> attrs = new ArrayList<>();
		attrs.add(InterplayAPI.createAttribute(AttributeGroup.USER, interplay.getMydmamIDinInterplay(), ""));
		attrs.add(InterplayAPI.createAttribute(AttributeGroup.USER, "Display Name", ""));
		attrs.add(InterplayAPI.createAttribute(AttributeGroup.SYSTEM, "Path", ""));
		
		/**
		 * Set Interplay search params
		 */
		List<AttributeConditionType> configured_conditions = search_conditions.stream().map(cond -> {
			return cond.getAttributeCondition();
		}).collect(Collectors.toList());
		
		ArrayList<AttributeConditionType> conds = new ArrayList<>();
		conds.addAll(configured_conditions);
		conds.add(InterplayAPI.createAttributeCondition(Condition.NOT_CONTAINS, AttributeGroup.USER, ac_locations_in_interplay, " "));
		
		AtomicInteger proceeded = new AtomicInteger(-1);
		
		/**
		 * id -> Asset
		 */
		HashMap<String, InterplayAssetLocalisator> founded_not_archived = new HashMap<>();
		
		while (proceeded.get() != 0) {
			proceeded.set(0);
			
			interplay_paths.forEach(interplay_path -> {
				try {
					SearchResponseType resp = interplay.search(interplay_path, bulk_size, attrs, conds);
					
					List<AssetDescriptionType> asset_description_list = resp.getResults().getAssetDescription();
					if (asset_description_list.isEmpty()) {
						return;
					}
					
					asset_description_list.stream().filter(asset_description -> {
						String id = InterplayAPI.getAttributeValueFromList(asset_description.getAttributes(), interplay.getMydmamIDinInterplay());
						if (id == null) {
							if (verbose_found_noid) {
								System.out.println("No valid ID for " + getFullInterplayPath(asset_description));
							}
							return false;
						}
						return founded_not_archived.containsKey(id) == false;
					}).map(asset_description -> {
						return new InterplayAssetLocalisator(asset_description);
					}).filter(ial -> {
						if (ial.isArchived(ac) == false) {
							founded_not_archived.put(ial.id, ial);
							return false;
						}
						return true;
					}).forEach(ial -> {
						ial.pushACtoInterplay(interplay);
						proceeded.incrementAndGet();
					});
					
					System.out.println("Done: " + proceeded.get());
					System.out.println();
				} catch (Exception e) {
					throw new RuntimeException("For Interplay path: " + interplay_path, e);
				}
			});
		}
		
		founded_not_archived.values().stream().sorted((l, r) -> {
			return l.asset_pathname.compareTo(r.asset_pathname);
		}).forEach(ial -> {
			System.out.println("Not archived: " + ial.toString());
		});
	}
	
	private static String getFullInterplayPath(AssetDescriptionType asset_description) {
		String asset_name = InterplayAPI.getAttributeValueFromList(asset_description.getAttributes(), "Display Name");
		String asset_path = InterplayAPI.getAttributeValueFromList(asset_description.getAttributes(), "Path");
		return FilenameUtils.getFullPath(asset_path) + asset_name;
	}
	
	private class InterplayAssetLocalisator {
		String id;
		String asset_pathname;
		// String mob_id;
		String asset_uri;
		String locations;
		String ac_path;
		
		public InterplayAssetLocalisator(AssetDescriptionType asset_description) {
			id = InterplayAPI.getAttributeValueFromList(asset_description.getAttributes(), interplay.getMydmamIDinInterplay());
			if (id == null) {
				throw new NullPointerException("Interplay attribute \"" + interplay.getMydmamIDinInterplay() + "\" can't to be empty");
			}
			asset_pathname = getFullInterplayPath(asset_description);
			// mob_id = InterplayAPI.getAttributeValueFromList(asset_description.getAttributes(), "MOB ID");
			asset_uri = asset_description.getInterplayURI();
		}
		
		public String toString() {
			return id + " " + asset_pathname;
		}
		
		public boolean isArchived(ACAPI ac) {
			try {
				ArrayList<SourcePathIndexerElement> items = explorer.getAllIdFromStorage(id, storage_name);
				if (items.size() > 1) {
					System.err.println("Too many files founded for id " + id + ": " + items);
					return false;
				}
				if (items.isEmpty()) {
					return false;
				}
				
				SourcePathIndexerElement archived_pathelement = items.get(0);
				
				/**
				 * Resolve path from AC and get tape locations.
				 */
				ACFile ac_file = ac.getFile(ac_share, archived_pathelement.currentpath, false);
				
				if (ac_file == null) {
					return false;
				}
				if (ac_file.isOnTape() == false) {
					return false;
				}
				
				locations = ac_file.getTapeBarcodeLocations().stream().collect(Collectors.joining(" "));
				
				ac_path = "/" + ac_file.share + "/" + ac_file.path;
				
				return true;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		public void pushACtoInterplay(InterplayAPI interplay) {
			try {
				System.out.println(toString());
				System.out.println(" => " + ac_locations_in_interplay + ": " + locations + ", " + ac_path_in_interplay + ": " + ac_path);
				
				ArrayList<AttributeType> attributes = new ArrayList<>();
				attributes.add(InterplayAPI.createAttribute(AttributeGroup.USER, ac_locations_in_interplay, locations));
				attributes.add(InterplayAPI.createAttribute(AttributeGroup.USER, ac_path_in_interplay, ac_path));
				interplay.setAttributes(attributes, asset_uri);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			
		}
	}
	
}
