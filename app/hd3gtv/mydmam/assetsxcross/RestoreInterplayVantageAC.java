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
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;

import com.avid.interplay.ws.assets.AttributeType;
import com.avid.interplay.ws.assets.SearchGroupType;
import com.avid.interplay.ws.assets.SearchResponseType;
import com.avid.interplay.ws.assets.SearchType;

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
import hd3gtv.mydmam.pathindexing.BridgePathindexArchivelocation;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.tools.TableList;
import hd3gtv.tools.TableList.Row;
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
	private String vantage_workflow_name;
	private String ac_locations_in_interplay;
	private String ac_path_in_interplay;
	
	public static RestoreInterplayVantageAC createFromConfiguration() {
		Object raw_conf = Configuration.global.getRawValue("assetsxcross", "interplay_restore");
		String s_conf = MyDMAM.gson_kit.getGsonSimple().toJsonTree(raw_conf).toString();
		return MyDMAM.gson_kit.getGsonSimple().fromJson(s_conf, RestoreInterplayVantageAC.class);
	}
	
	private transient Explorer explorer;
	private transient InterplayAPI interplay;
	private transient ACAPI acapi;
	private transient VantageAPI vantage;
	private transient BridgePathindexArchivelocation bridge_pathindex_archivelocation;
	private transient DestageManager destage_manager;
	
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
		
		bridge_pathindex_archivelocation = new BridgePathindexArchivelocation(acapi, Configuration.global.getListMapValues("acapi", "bridge"));
		
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
	 * @return null if mydmam_id don't match with an Sequence/Masterclip, of if this is already online.
	 */
	public RestoreJob restore(String mydmam_id, String search_root_path, String base_job_name) throws Exception {
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
			return null;
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
		
		if (asset.isOnline()) {
			Loggers.AssetsXCross.info("Asset is already online: " + asset.getDisplayName());
			return null;
		}
		
		if (asset.isMasterclip()) {
			/**
			 * Restore this Masterclip
			 */
			ManageableAsset ma = new ManageableAsset(asset);
			ma.localizeArchivedVersion();
			if (ma.localized_archived_version == null) {
				return null;
			}
			
			Loggers.AssetsXCross.info("Start to restore master clip \"" + asset.getDisplayName() + "\" " + mydmam_id);
			
			ma.localized_archived_version.destageAndCreateVantageRestoreJob(base_job_name, vantage_job -> {
				Loggers.AssetsXCross.info("Submit Vantage job " + vantage_job + " for restore master clip \"" + asset.getDisplayName() + "\" " + mydmam_id);
			}, e -> {
				Loggers.AssetsXCross.info("Error during master clip restauration \"" + asset.getDisplayName() + "\" " + mydmam_id, e);
			});
			
			return new RestoreJob(asset, Arrays.asList(ma));
		} else if (asset.isSequence()) {
			/**
			 * Restore all Masterclips linked in this sequence
			 */
			Loggers.AssetsXCross.info("Prepare to restore sequence \"" + asset.getDisplayName() + "\" " + mydmam_id);
			
			RelativeMasterclips rmc = new RelativeMasterclips(asset.getRelatives(true, ac_locations_in_interplay, ac_path_in_interplay).stream().map(r_asset -> {
				return new ManageableAsset(r_asset);
			}).collect(Collectors.toList()));
			
			Consumer<ManageableAsset> onStartDestage = relative_asset -> {
				Loggers.AssetsXCross.info("Start to restore master clip \"" + relative_asset.asset.getDisplayName() + "\" " + relative_asset.asset.getMyDMAMID() + " (asked by \"" + asset.getDisplayName() + "\" " + mydmam_id + ")");
			};
			BiConsumer<ManageableAsset, VantageJob> onCreateVantageJob = (relative_asset, vantage_job) -> {
				Loggers.AssetsXCross.info("Submit Vantage job " + vantage_job + " for restore master clip \"" + relative_asset.asset.getDisplayName() + "\" " + relative_asset.asset.getMyDMAMID() + " (asked by \"" + asset.getDisplayName() + "\" " + mydmam_id + ")");
			};
			BiConsumer<ManageableAsset, Exception> onError = (relative_asset, e) -> {
				Loggers.AssetsXCross.info("Error during master clip restauration \"" + relative_asset.asset.getDisplayName() + "\" " + relative_asset.asset.getMyDMAMID() + " (asked by \"" + asset.getDisplayName() + "\" " + mydmam_id + ")", e);
			};
			
			rmc.destageAllAndCreateVantageRestoreJobForEachDestaged(base_job_name, onStartDestage, onCreateVantageJob, onError);
			
			return new RestoreJob(asset, rmc);
		} else {
			throw new IOException("Asset " + asset.getDisplayName() + " is neither a sequence or a master clip");
		}
	}
	
	public class RestoreJob {
		private InterplayAsset base_asset;
		private Collection<ManageableAsset> manageable_assets;
		
		private RestoreJob(InterplayAsset base_asset, Collection<ManageableAsset> manageable_assets) {
			this.base_asset = base_asset;
			this.manageable_assets = manageable_assets;
		}
		
		public void globalStatus(TableList table) {
			final Row row_base = table.createRow();
			row_base.addCell(base_asset.getType().getShortName());
			if (base_asset.getMyDMAMID() != null) {
				row_base.addCell(base_asset.getMyDMAMID());
			} else {
				row_base.addEmptyCell();
			}
			row_base.addCell(base_asset.getDisplayName().substring(0, Math.min(base_asset.getDisplayName().length(), 15)));
			if (base_asset.isOnline()) {
				row_base.addCell("online");
				return;
			}
			
			manageable_assets.stream().forEach(m_asset -> {
				Row row_line = row_base;
				if (base_asset.interplay_uri != m_asset.asset.interplay_uri) {
					row_line = table.createRow();
					
					if (m_asset.asset.isOnline()) {
						row_base.addCell("  o");
					} else if (m_asset.localized_archived_version != null) {
						if (m_asset.localized_archived_version.vantage_job != null) {
							row_base.addCell("  V");
						} else if (m_asset.localized_archived_version.destage_job != null) {
							row_base.addCell("  D");
						} else {
							row_base.addCell("  !");
						}
					} else {
						row_base.addCell("  X");
					}
					
					if (m_asset.asset.getMyDMAMID() != null) {
						row_base.addCell(m_asset.asset.getMyDMAMID());
					} else {
						row_base.addEmptyCell();
					}
					row_base.addCell(m_asset.asset.getDisplayName().substring(0, Math.min(m_asset.asset.getDisplayName().length(), 15)));
				}
				
				if (m_asset.asset.isOnline()) {
					row_line.addCell("Online");
				} else if (m_asset.localized_archived_version == null) {
					row_line.addCell("No localized version: can't restore this clip");
				} else if (m_asset.localized_archived_version.vantage_job != null) {
					row_line.addCell("Vantage: " + m_asset.localized_archived_version.vantage_job.toString());
				} else if (m_asset.localized_archived_version.destage_job != null) {
					row_line.addCell(m_asset.localized_archived_version.destage_job.toString());
				} else {
					row_line.addCell("No action pending for localized version !");
				}
			});
		}
		
		public boolean isDone() {
			if (base_asset.isOnline()) {
				return true;
			}
			return manageable_assets.stream().allMatch(m_asset -> {
				if (m_asset.asset.isOnline()) {
					return true;
				}
				if (m_asset.localized_archived_version != null) {
					ArchivedAsset arch = m_asset.localized_archived_version;
					if (arch.vantage_job != null) {
						return true;
					}
				}
				return false;
			});
		}
	}
	
	public class ManageableAsset {
		
		private InterplayAsset asset;
		private ArchivedAsset localized_archived_version;
		
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
			return asset.isOnline() & canBeRestoredinFuture() & asset.isMasterclip();
		}
		
		/**
		 * @return true if video & audio presence.
		 */
		public boolean canBeRestoredinFuture() {
			return asset.getMyDMAMID() != null & asset.isMasterclip() & asset.hasVideoTrack() & asset.getAudioTracksCount() > 0;
		}
		
		/**
		 * @return null if destage is impossible (file not archived/not found in AC) or stupid (file is not offine in Interplay) or not a master clip
		 */
		public void localizeArchivedVersion() throws Exception {
			if (localized_archived_version != null) {
				return;
			}
			
			if (asset.isOnline()) {
				Loggers.AssetsXCross.debug("Ignore the restoration of " + asset.getDisplayName() + " beacause it's already on line");
				return;
			}
			if (asset.isMasterclip() == false) {
				Loggers.AssetsXCross.debug("Ignore the restoration of " + asset.getDisplayName() + " beacause it's not a masterclip");
				return;
			}
			if (canBeRestoredinFuture() == false) {
				Loggers.AssetsXCross.info("Ignore the restoration of " + asset.getDisplayName() + " beacause it's not possible to restore it");
				return;
			}
			
			/**
			 * Resolve path from AC and get tape locations.
			 */
			ACFile ac_file = null;
			String full_ac_path = asset.getAttribute(ac_path_in_interplay);
			
			if (full_ac_path == null) {
				Loggers.AssetsXCross.debug("Search archived version for " + asset.getMyDMAMID() + " in " + archive_storagename);
				
				ArrayList<SourcePathIndexerElement> founded = explorer.getAllIdFromStorage(asset.getMyDMAMID(), archive_storagename);
				if (founded.isEmpty()) {
					throw new FileNotFoundException("Can't found archived file with ID " + asset.getMyDMAMID() + " in " + archive_storagename);
				}
				if (founded.size() > 1) {
					throw new IOException("More than 1 file archived as " + asset.getMyDMAMID() + ": " + founded);
				}
				
				ac_file = bridge_pathindex_archivelocation.getExternalLocation(founded.get(0));
				if (ac_file == null) {
					throw new FileNotFoundException("Can't found archived file in ACAPI: " + founded.get(0).currentpath);
				}
				
				Loggers.AssetsXCross.debug("Found archived version for " + asset.getMyDMAMID() + ": " + ac_file.toString() + " and update Interplay database");
				
				ArrayList<AttributeType> attributes = new ArrayList<>();
				attributes.add(InterplayAPI.createAttribute(AttributeGroup.USER, ac_locations_in_interplay, ac_file.getTapeBarcodeLocations().stream().collect(Collectors.joining(" "))));
				attributes.add(InterplayAPI.createAttribute(AttributeGroup.USER, ac_path_in_interplay, "/" + ac_file.share + "/" + ac_file.path));
				interplay.setAttributes(attributes, asset.interplay_uri);
			} else {
				String[] path = full_ac_path.split("/", 2);
				ac_file = acapi.getFile(path[0], "/" + path[1], false);
				
				if (ac_file == null) {
					throw new FileNotFoundException("Can't found archived file in ACAPI: " + full_ac_path);
				}
			}
			
			localized_archived_version = new ArchivedAsset(ac_file);
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
			
			private FileDestageJob destage_job;
			private VantageJob vantage_job;
			
			/**
			 * Async operation
			 */
			public void destageAndCreateVantageRestoreJob(String base_job_name, Consumer<VantageJob> onCreateVantageJob, Consumer<Exception> onError) {
				if (asset.getMyDMAMID() == null) {
					throw new NullPointerException("No MyDMAM ID for asset " + asset.getDisplayName());
				}
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
							vantage_job = vantage.createJob(source_file_unc, vantage_workflow_name, vars, base_job_name + " " + id);
							onCreateVantageJob.accept(vantage_job);
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
			
			/**
			 * @return maybe null if no planned Job
			 */
			public VantageJob getVantageJob() {
				return vantage_job;
			}
		}
		
		// TODO add copy into a shred directory function
	}
	
	public class RelativeMasterclips extends ArrayList<ManageableAsset> {
		private RelativeMasterclips(Collection<? extends ManageableAsset> source) {
			super(source);
		}
		
		public void localizeAllRelativeArchivedVersion(BiConsumer<ManageableAsset, Exception> onError) {
			stream().forEach(asset -> {
				try {
					asset.localizeArchivedVersion();
				} catch (Exception e) {
					onError.accept(asset, e);
				}
			});
		}
		
		public void destageAllAndCreateVantageRestoreJobForEachDestaged(String base_job_name, Consumer<ManageableAsset> onStartDestage, BiConsumer<ManageableAsset, VantageJob> onCreateVantageJob, BiConsumer<ManageableAsset, Exception> onError) {
			localizeAllRelativeArchivedVersion(onError);
			
			stream().forEach(asset -> {
				ArchivedAsset arch = asset.localized_archived_version;
				if (arch == null) {
					return;
				}
				onStartDestage.accept(asset);
				arch.destageAndCreateVantageRestoreJob(base_job_name, v -> {
					onCreateVantageJob.accept(asset, v);
				}, e -> {
					onError.accept(asset, e);
				});
			});
		}
		
		// TODO shred proposal
	}
	
	// TODO (postpone) search&restore scan with categories
	// TODO search&copy for shred proposal & mergue with ACAPI Interplay Tag
	// TODO Decision path lists in conf for shred proposal
}
