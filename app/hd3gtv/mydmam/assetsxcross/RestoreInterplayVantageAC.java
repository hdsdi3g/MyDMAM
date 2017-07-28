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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;

import com.avid.interplay.ws.assets.AssetsFault;
import com.avid.interplay.ws.assets.SearchGroupType;
import com.avid.interplay.ws.assets.SearchResponseType;
import com.avid.interplay.ws.assets.SearchType;
import com.google.gson.JsonElement;

import hd3gtv.archivecircleapi.ACAPI;
import hd3gtv.archivecircleapi.ACFile;
import hd3gtv.archivecircleapi.ACFileLocationCache;
import hd3gtv.archivecircleapi.DestageManager;
import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.assetsxcross.InterplayAPI.AssetType;
import hd3gtv.mydmam.assetsxcross.InterplayAPI.AttributeGroup;
import hd3gtv.mydmam.assetsxcross.InterplayAPI.Condition;
import hd3gtv.mydmam.assetsxcross.InterplayAPI.MediaStatus;
import hd3gtv.mydmam.assetsxcross.InterplayAPI.SimpleInterplayAsset;
import hd3gtv.mydmam.assetsxcross.VantageAPI.VariableDefinition;
import hd3gtv.mydmam.pathindexing.AJSFileLocationStatus;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.tools.Timecode;

public class RestoreInterplayVantageAC {
	
	private String mydmam_id_in_interplay;
	private String vantage_variable_name_interplay_mastermob;
	private String vantage_variable_name_interplay_sourcemob;
	private String vantage_variable_name_interplay_path;
	private String vantage_variable_name_interplay_file;
	private String vantage_variable_name_audio_ch;
	private String vantage_variable_name_tcin;
	private String archive_storagename;
	private int fps;
	private String ac_share;
	private String vantage_workflow_name;
	
	private DestageManager destage_manager;
	
	public static RestoreInterplayVantageAC createFromConfiguration() {
		Object raw_conf = Configuration.global.getRawValue("assetsxcross", "interplay_restore");
		JsonElement j_conf = MyDMAM.gson_kit.getGsonSimple().toJsonTree(raw_conf);
		return MyDMAM.gson_kit.getGsonSimple().fromJson(j_conf, RestoreInterplayVantageAC.class);
	}
	
	private transient Explorer explorer;
	private transient InterplayAPI interplay;
	private transient ACAPI acapi;
	private transient VantageAPI vantage;
	
	private RestoreInterplayVantageAC() throws IOException {
		interplay = InterplayAPI.initFromConfiguration();
		explorer = new Explorer();
		
		/**
		 * Init ACAPI
		 */
		acapi = ACAPI.loadFromConfiguration();
		if (acapi == null) {
			throw new IOException("ACAPI is not configured");
		}
		/*ACNode node = ac.getNode();
		if (node == null) {
			throw new IOException("Can't init ACAPI");
		}*/
		
		destage_manager = new DestageManager(acapi, tapes -> { // TODO by mail...
			if (tapes.size() > 1) {
				System.out.println("Please insert in a tape library one of " + tapes.stream().map(tape -> {
					return tape.barcode;
				}).collect(Collectors.joining(", ")));
			} else {
				System.out.println("Please insert in a tape library " + tapes.get(0).barcode);
			}
			return true;
		}, (ac_file, e) -> {
			// TODO by mail...
			System.err.println("Can't get AC file from tape: " + ac_file);
			e.printStackTrace();
		});
		
		vantage = VantageAPI.createFromConfiguration();
	}
	
	/**
	 * @return null if not found
	 */
	public InterplayMasterClip getMasterClipByMyDMAMID(String id, String search_root_path) throws AssetsFault, IOException {
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
		
		List<SimpleInterplayAsset> asset_list = interplay.convertSearchResponseToAssetList(response);
		
		if (asset_list.isEmpty()) {
			return null;
		}
		
		InterplayMasterClip result = new InterplayMasterClip();
		asset_list.forEach(asset -> {
			result.setAttributes(asset);
		});
		return result;
	}
	
	// TODO Restore a list of masterclip (only offline)
	
	public interface InterplayAsset {
		public boolean isOnline();
		
		public boolean isMasterclip();
	}
	
	public class InterplaySequence implements InterplayAsset {// TODO create after a search
		public boolean isMasterclip() {
			return false;
		}
		
		public boolean isOnline() {
			// TODO Auto-generated method stub
			return false;
		}
		
		// TODO Restore a sequence (all masterclips of this sequence)
		
		/*List<InterplayMasterClip> allRelatives() */
	}
	
	public class InterplayMasterClip implements InterplayAsset {
		
		private InterplayMasterClip() {
			display_names = new ArrayList<String>();
			paths = new ArrayList<String>();
		}
		
		// private String interplay_uri;
		private String mob_id;
		private String source_id;
		private MediaStatus media_status;
		private String tc_in;
		private AssetType type;
		private String mydmam_id;
		private ArrayList<String> display_names;
		private ArrayList<String> paths;
		private String tracks;
		private boolean has_video_track;
		private int audio_tracks_count;
		
		private void setAttributes(SimpleInterplayAsset asset) {
			
			if (mob_id != null) {
				if (mob_id.equalsIgnoreCase(asset.getAttribute("MOB ID")) == false) {
					throw new IndexOutOfBoundsException("MobID conflicts with this version");
				}
			}
			mob_id = asset.getAttribute("MOB ID");
			
			if (source_id != null) {
				if (source_id.equalsIgnoreCase(asset.getAttribute("Source ID")) == false) {
					throw new IndexOutOfBoundsException("Source ID conflicts with this version");
				}
			}
			source_id = asset.getAttribute("Source ID");
			
			media_status = MediaStatus.valueOf(asset.getAttribute("Media Status"));
			tc_in = asset.getAttribute("Start"); // TC IN
			type = AssetType.valueOf(asset.getAttribute("Type"));
			mydmam_id = asset.getAttribute(mydmam_id_in_interplay);
			tracks = asset.getAttribute("Tracks"); // V1 A1A2
			
			if (display_names.contains(asset.getAttribute("Display Name")) == false) {
				display_names.add(asset.getAttribute("Display Name"));
			}
			if (paths.contains(asset.getAttribute("Path")) == false) {
				paths.add(asset.getAttribute("Path"));
			}
			
			/**
			 * V1 A1-2
			 */
			audio_tracks_count = Arrays.asList(tracks.split(" ")).stream().filter(t -> {
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
			
			has_video_track = Arrays.asList(tracks.split(" ")).stream().anyMatch(t -> {
				return t.startsWith("V");
			});
			
		}
		
		public boolean isOnline() {
			return media_status == MediaStatus.online;
		}
		
		public boolean isMasterclip() {
			return type == AssetType.masterclip;
		}
		
		/**
		 * @return true if online & can be restored in the future
		 */
		public boolean canBePurged() {
			return isOnline() && canBeRestoredinFuture();
		}
		
		/**
		 * @return true if video & audio presence.
		 */
		public boolean canBeRestoredinFuture() {
			return has_video_track && audio_tracks_count > 0;
		}
		
		// TODO add copy into a shred directory function
		
		/**
		 * @return null if destage is impossible (file not archived/not found in AC) or stupid (file is not offine in Interplay) or not a master clip
		 */
		public ArchivedAsset localizeArchivedVersion() throws Exception {
			if (canBeRestoredinFuture() == false | isOnline() | isMasterclip() == false) {
				return null;
			}
			
			ArrayList<SourcePathIndexerElement> founded = explorer.getAllIdFromStorage(mydmam_id, archive_storagename);
			if (founded.isEmpty()) {
				return null;
			}
			if (founded.size() > 1) {
				throw new IOException("More than 1 file archived as " + mydmam_id + ": " + founded);
			}
			
			SourcePathIndexerElement archived_pathelement = founded.get(0);
			
			/**
			 * Resolve path from AC and get tape locations.
			 */
			ACFile ac_file = acapi.getFile(ac_share, archived_pathelement.currentpath, false);
			
			if (ac_file == null) {
				throw new FileNotFoundException("Can't found archived file in ACAPI: " + ac_share + "/" + archived_pathelement.currentpath);
			}
			return new ArchivedAsset(ac_file);
		}
		
		public class ArchivedAsset {
			private ACFile acfile;
			private AJSFileLocationStatus status;
			
			private ArchivedAsset(ACFile acfile) {
				this.acfile = acfile;
				if (acfile == null) {
					throw new NullPointerException("\"acfile\" can't to be null");
				}
				status = new AJSFileLocationStatus();
				status.getFromACAPI(acfile);
			}
			
			public AJSFileLocationStatus getStatus() {
				return status;
			}
			
			/**
			 * Async operation
			 */
			public void destageAndCreateVantageRestoreJob(String base_job_name) {
				destage_manager.addFileToDestage(acfile, mydmam_id, (acf, id) -> {
					try {
						/**
						 * Get the better node to get file
						 */
						ArrayList<String> stored_media_nodes = acf.this_locations.stream().filter(location -> {
							return location instanceof ACFileLocationCache;
						}).map(location -> {
							return ((ACFileLocationCache) location).nodes;
						}).findFirst().orElse(new ArrayList<>(Arrays.asList(acapi.getDefaultDestageNode())));
						
						if (stored_media_nodes.isEmpty()) {
							stored_media_nodes = new ArrayList<>(Arrays.asList(acapi.getDefaultDestageNode()));
						}
						
						String ac_unc_host = null;
						if (stored_media_nodes.contains(acapi.getDefaultDestageNode())) {
							ac_unc_host = acapi.getHostnameByNodename(acapi.getDefaultDestageNode());
						} else {
							ac_unc_host = acapi.getHostnameByNodename(stored_media_nodes.get(0));
						}
						
						/**
						 * Prepare new Vantage Job vars
						 */
						String source_file_unc = "//" + ac_unc_host + "/" + acfile.share + "/" + acfile.path;
						try {
							ArrayList<VariableDefinition> vars = new ArrayList<>();
							vars.add(vantage.createVariableDef(vantage_variable_name_interplay_mastermob, mob_id));
							vars.add(vantage.createVariableDef(vantage_variable_name_interplay_sourcemob, source_id));
							vars.add(vantage.createVariableDef(vantage_variable_name_interplay_path, FilenameUtils.getFullPathNoEndSeparator(paths.get(0))));
							vars.add(vantage.createVariableDef(vantage_variable_name_interplay_file, display_names.get(0)));
							vars.add(vantage.createVariableDef(vantage_variable_name_audio_ch, audio_tracks_count));
							vars.add(vantage.createVariableDef(vantage_variable_name_tcin, new Timecode(tc_in, fps)));
							
							/**
							 * Start Vantage Job
							 */
							String key = vantage.createJob(source_file_unc, vantage_workflow_name, vars, base_job_name + " " + id);
							// TODO Watch Vantage job during restore
						} catch (Exception e) {
							throw new IOException("Can't send to Vantage restore Job", e);
						}
					} catch (IOException e) {
						// TODO error
						e.printStackTrace();
					}
				}, (acf, id) -> {
					// TODO error
					System.err.println("Can't destage " + acf + " (" + id + ")");
				}, 4, TimeUnit.HOURS);
			}
			
		}
		
	}
	
	// TODO search&restore scan
	// TODO search&copy for shred proposal
	// TODO Decision lists conf for shred proposal
}
