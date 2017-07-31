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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;

import com.avid.interplay.ws.assets.SearchGroupType;
import com.avid.interplay.ws.assets.SearchResponseType;
import com.avid.interplay.ws.assets.SearchType;
import com.google.gson.JsonElement;

import hd3gtv.archivecircleapi.ACAPI;
import hd3gtv.archivecircleapi.ACFile;
import hd3gtv.archivecircleapi.ACFileLocationCache;
import hd3gtv.archivecircleapi.DestageManager;
import hd3gtv.archivecircleapi.DestageManager.FileDestageJob;
import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.assetsxcross.InterplayAPI.AttributeGroup;
import hd3gtv.mydmam.assetsxcross.InterplayAPI.Condition;
import hd3gtv.mydmam.assetsxcross.RestoreInterplayVantageAC.ManageableAsset.ArchivedAsset;
import hd3gtv.mydmam.assetsxcross.VantageAPI.VantageJob;
import hd3gtv.mydmam.assetsxcross.VantageAPI.VariableDefinition;
import hd3gtv.mydmam.pathindexing.AJSFileLocationStatus;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.tools.Timecode;

public class RestoreInterplayVantageAC {
	
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
	private String ac_locations_in_interplay;
	private String ac_path_in_interplay;
	
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
		
		destage_manager = new DestageManager(acapi, tapes -> { // TODO (postponed) by mail...
			if (tapes.size() > 1) {
				System.out.println("Please insert in a tape library one of " + tapes.stream().map(tape -> {
					return tape.barcode;
				}).collect(Collectors.joining(", ")));
			} else {
				System.out.println("Please insert in a tape library " + tapes.get(0).barcode);
			}
			return true;
		}, (ac_file, e) -> {
			// TODO (postponed) by mail...
			System.err.println("Can't get AC file from tape: " + ac_file);
			e.printStackTrace();
		});
		
		vantage = VantageAPI.createFromConfiguration();
	}
	
	/**
	 * Search in interplay and AC is sync. Destage is async. Transcoding is async (and not managed).
	 */
	public void restore(String mydmam_id, String search_root_path, String base_job_name) throws Exception {
		SearchType search_type = new SearchType();
		search_type.setMaxResults(100);
		
		SearchGroupType search_group_type = new SearchGroupType();
		search_group_type.setOperator("AND");
		// search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.EQUALS, AttributeGroup.SYSTEM, "Type", type));
		search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.EQUALS, AttributeGroup.USER, interplay.getMydmamIDinInterplay(), mydmam_id));
		search_type.setSearchGroup(search_group_type);
		search_type.setInterplayPathURI(interplay.createURLInterplayPath(search_root_path));
		
		SearchResponseType response = interplay.search(search_type);
		InterplayAPI.checkError(response.getErrors());
		
		ArrayList<InterplayAsset> asset_list = new ArrayList<>(interplay.convertSearchResponseToAssetList(response));
		asset_list.removeIf(asset -> {
			return asset.isMasterclip() == false & asset.isSequence() == false;
		});
		if (asset_list.isEmpty()) {
			return;
		}
		
		if (asset_list.size() > 1) {
			String first_mobid = asset_list.get(0).getMobID();
			if (asset_list.stream().allMatch(asset -> {
				return first_mobid.equals(asset.getMobID());
			}) == false) {
				throw new IndexOutOfBoundsException("Many differents Asset for the same ID " + mydmam_id);
			}
		}
		
		InterplayAsset asset = asset_list.stream().filter(a -> {
			return a.getPath().startsWith(search_root_path);
		}).findFirst().orElseThrow(() -> {
			return new FileNotFoundException("Can't found asset with ID " + mydmam_id + " in \"" + search_root_path + "\" Interplay directory");
		});
		
		if (asset.isMasterclip()) {
			/**
			 * Restore this Masterclip
			 */
			ManageableAsset ma = new ManageableAsset(asset);
			ArchivedAsset archive = ma.localizeArchivedVersion();
			if (archive == null) {
				return;
			}
			
			Loggers.AssetsXCross.info("Start to restore master clip \"" + asset.getDisplayName() + "\" " + mydmam_id);
			
			archive.destageAndCreateVantageRestoreJob(base_job_name, vantage_job -> {
				// TODO log
			}, error -> {
				// TODO log
			});
		} else if (asset.isSequence()) {
			/**
			 * Restore all Masterclips linked in this sequence
			 */
			Loggers.AssetsXCross.info("Prepare to restore sequence \"" + asset.getDisplayName() + "\" " + mydmam_id);
			
			RelativeMasterclips rmc = new RelativeMasterclips(asset.getRelatives(true, ac_locations_in_interplay, ac_path_in_interplay).stream().map(r_asset -> {
				return new ManageableAsset(r_asset);
			}).collect(Collectors.toList()));
			
			List<ArchivedAsset> archived_list = rmc.destageAllAndCreateVantageRestoreJobForEachDestaged(base_job_name, (archive, vantage_job) -> {
				// TODO log
				// ((ArchivedAsset) archive).referer.asset.getDisplayName();
			}, (mng_asset, error) -> {
				// TODO log
			}, (archive, error) -> {
				// TODO log
			});
			
			if (archived_list.isEmpty() == false) {
				archived_list.stream().forEach(arch_asset -> {
					Loggers.AssetsXCross.info("Start to restore master clip \"" + arch_asset.referer.asset.getDisplayName() + "\" " + arch_asset.referer.asset.getMyDMAMID() + " (asked by \"" + asset.getDisplayName() + "\" " + mydmam_id + ")");
				});
			}
		}
		
		// TODO how to know if destage is finish ?! >> must watch all restore pending (aka list & refresh)... >> create Class for store all this
		// see archived_list.get(0).getDestageJob() for destage progress
	}
	
	public class ManageableAsset {
		
		private InterplayAsset asset;
		
		private ManageableAsset(InterplayAsset asset) {
			this.asset = asset;
			if (asset == null) {
				throw new NullPointerException("\"asset\" can't to be null");
			}
		}
		
		/**
		 * @return true if online & can be restored in the future
		 */
		public boolean canBePurged() {
			return asset.isOnline() && canBeRestoredinFuture() && asset.isMasterclip();
		}
		
		/**
		 * @return true if video & audio presence.
		 */
		public boolean canBeRestoredinFuture() {
			return asset.hasVideoTrack() && asset.getAudioTracksCount() > 0 && asset.isMasterclip();
		}
		
		/**
		 * @return null if destage is impossible (file not archived/not found in AC) or stupid (file is not offine in Interplay) or not a master clip
		 */
		public ArchivedAsset localizeArchivedVersion() throws Exception {
			if (canBeRestoredinFuture() == false | asset.isOnline() | asset.isMasterclip() == false) {
				return null;
			}
			// TODO use ac_path if exists for resolve archive path
			
			ArrayList<SourcePathIndexerElement> founded = explorer.getAllIdFromStorage(asset.getMyDMAMID(), archive_storagename);
			if (founded.isEmpty()) {
				return null;
			}
			if (founded.size() > 1) {
				throw new IOException("More than 1 file archived as " + asset.getMyDMAMID() + ": " + founded);
			}
			
			SourcePathIndexerElement archived_pathelement = founded.get(0);
			// TODO set ac_path && ac_tapes
			
			/**
			 * Resolve path from AC and get tape locations.
			 */
			ACFile ac_file = acapi.getFile(ac_share, archived_pathelement.currentpath, false);
			
			if (ac_file == null) {
				throw new FileNotFoundException("Can't found archived file in ACAPI: " + ac_share + "/" + archived_pathelement.currentpath);
			}
			return new ArchivedAsset(this, ac_file);
		}
		
		public class ArchivedAsset {
			private ACFile acfile;
			private AJSFileLocationStatus status;
			private ManageableAsset referer;
			
			private ArchivedAsset(ManageableAsset referer, ACFile acfile) {
				this.referer = referer;
				if (referer == null) {
					throw new NullPointerException("\"referer\" can't to be null");
				}
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
			
			private FileDestageJob destage_job;
			
			/**
			 * Async operation
			 */
			public void destageAndCreateVantageRestoreJob(String base_job_name, Consumer<VantageJob> onCreateVantageJob, Consumer<Exception> onError) {
				destage_job = destage_manager.addFileToDestage(acfile, asset.getMyDMAMID(), (acf, id) -> {
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
							vars.add(vantage.createVariableDef(vantage_variable_name_interplay_mastermob, asset.getMobID()));
							vars.add(vantage.createVariableDef(vantage_variable_name_interplay_sourcemob, asset.getSourceID()));
							vars.add(vantage.createVariableDef(vantage_variable_name_interplay_path, FilenameUtils.getFullPathNoEndSeparator(asset.getPath())));
							vars.add(vantage.createVariableDef(vantage_variable_name_interplay_file, asset.getDisplayName()));
							vars.add(vantage.createVariableDef(vantage_variable_name_audio_ch, asset.getAudioTracksCount()));
							vars.add(vantage.createVariableDef(vantage_variable_name_tcin, new Timecode(asset.getStart(), fps)));
							
							/**
							 * Start Vantage Job
							 */
							onCreateVantageJob.accept(vantage.createJob(source_file_unc, vantage_workflow_name, vars, base_job_name + " " + id));
						} catch (Exception e) {
							throw new IOException("Can't send to Vantage restore Job (" + vantage_workflow_name + "] \"" + base_job_name + " " + id + "\" from " + source_file_unc, e);
						}
					} catch (IOException e) {
						onError.accept(e);
					}
				}, (acf, id) -> {
					onError.accept(new IOException("Can't destage " + acf + " (" + id + ")"));
				}, 4, TimeUnit.HOURS);
			}
			
			/**
			 * @return maybe null if no planned destage
			 */
			public FileDestageJob getDestageJob() {
				return destage_job;
			}
			
		}
		
		// TODO add copy into a shred directory function
	}
	
	public class RelativeMasterclips extends ArrayList<ManageableAsset> {
		private RelativeMasterclips(Collection<? extends ManageableAsset> source) {
			super(source);
		}
		
		public List<ArchivedAsset> localizeAllRelativeArchivedVersion(BiConsumer<ManageableAsset, Exception> onError) {
			return stream().map(asset -> {
				try {
					return asset.localizeArchivedVersion();
				} catch (Exception e) {
					onError.accept(asset, e);
				}
				return null;
			}).filter(arch -> {
				return arch != null;
			}).collect(Collectors.toList());
		}
		
		public List<ArchivedAsset> destageAllAndCreateVantageRestoreJobForEachDestaged(String base_job_name, BiConsumer<ArchivedAsset, VantageJob> onCreateVantageJob, BiConsumer<ManageableAsset, Exception> onErrorLocalizeArchived, BiConsumer<ArchivedAsset, Exception> onErrorDestageAndVantage) {
			List<ArchivedAsset> archived_list = localizeAllRelativeArchivedVersion(onErrorLocalizeArchived);
			
			// BiConsumer<ArchivedAsset, VantageJob> onCreateVantageJob, BiConsumer<ArchivedAsset, Exception> onError
			archived_list.forEach(arch -> {
				arch.destageAndCreateVantageRestoreJob(base_job_name, v -> {
					onCreateVantageJob.accept(arch, v);
				}, e -> {
					onErrorDestageAndVantage.accept(arch, e);
				});
			});
			
			return archived_list;
		}
		
		// TODO shred proposal
	}
	
	// TODO search&restore scan with catergories
	// TODO search&copy for shred proposal & mergue with ACAPI Interplay Tag
	// TODO Decision path lists in conf for shred proposal
}
