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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;

import com.avid.interplay.ws.assets.AssetsFault;
import com.avid.interplay.ws.assets.AttributeType;
import com.avid.interplay.ws.assets.FileLocationType;
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
import hd3gtv.mydmam.assetsxcross.InterplayAPI.AssetType;
import hd3gtv.mydmam.assetsxcross.InterplayAPI.AttributeGroup;
import hd3gtv.mydmam.assetsxcross.InterplayAPI.Condition;
import hd3gtv.mydmam.assetsxcross.RestoreInterplayVantageAC.ManageableAsset.ArchivedAsset;
import hd3gtv.mydmam.assetsxcross.VantageAPI.VantageJob;
import hd3gtv.mydmam.assetsxcross.VantageAPI.VariableDefinition;
import hd3gtv.mydmam.pathindexing.AJSFileLocationStatus;
import hd3gtv.mydmam.pathindexing.BridgePathindexArchivelocation;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.Importer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import net.telestream.vantage.ws.IDomainGetWorkflowsUnlicensedSdkExceptionFaultFaultMessage;
import net.telestream.vantage.ws.IWorkflowSubmitSubmitFileUnlicensedSdkExceptionFaultFaultMessage;
import net.telestream.vantage.ws.IWorkflowSubmitSubmitFileWorkflowDoesNotExistExceptionFaultFaultMessage;
import net.telestream.vantage.ws.IWorkflowSubmitSubmitFileWorkflowInvalidStateExceptionFaultFaultMessage;

public class RestoreInterplayVantageAC {
	
	private String vantage_variable_name_interplay_mastermob;
	private String vantage_variable_name_interplay_sourcemob;
	private String vantage_variable_name_interplay_path;
	private String vantage_variable_name_interplay_file;
	private String vantage_variable_name_audio_ch;
	private String vantage_variable_name_tcin;
	private String vantage_variable_name_filepathaudio;
	private String vantage_variable_name_mediaid;
	private String vantage_variable_name_archivepath;
	private String archive_storagename;
	private float fps;
	private String vantage_workflow_name;
	private String ac_locations_in_interplay;
	private String ac_path_in_interplay;
	private String last_check_shred_in_interplay;
	private String todoarchives_interplay_folder;
	private String lostandfound_base_interplay_folder;
	private String seq_check_rel_orphan_in_interplay;
	private ArrayList<String> do_not_touch_interplay_paths;
	private ArrayList<String> interplay_paths_ignore_during_orphan_projects_dir_search;
	private ArrayList<String> interplay_paths_tag_to_purge_during_orphan_projects_dir_search;
	private int mydmam_id_size;
	private String vantage_archive_workflow_name;
	private String vantage_archive_destbasepath;
	
	public static RestoreInterplayVantageAC createFromConfiguration() {
		Object raw_conf = Configuration.global.getRawValue("assetsxcross", "interplay_restore");
		String s_conf = MyDMAM.gson_kit.getGsonSimple().toJsonTree(raw_conf).toString();
		return MyDMAM.gson_kit.getGsonSimple().fromJson(s_conf, RestoreInterplayVantageAC.class);
	}
	
	/**
	 * @param id can be null: return null
	 */
	public String parseMyDMAMID(String id) {
		if (id == null) {
			return null;
		}
		if (id.length() < mydmam_id_size) {
			return null;
		}
		if (Importer.getIdExtractorFileName().isValidId(id) == false) {
			return null;
		}
		return id.substring(0, mydmam_id_size);
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
		
		destage_manager = new DestageManager(acapi, tapes -> { // T O D O (postponed) by mail...
			if (tapes.size() > 1) {
				System.out.println("Please insert in a tape library one of " + tapes.stream().map(tape -> {
					return tape.barcode;
				}).collect(Collectors.joining(", ")));
			} else {
				System.out.println("Please insert in a tape library " + tapes.get(0).barcode);
			}
			return true;
		}, (ac_file, e) -> {
			// T O D O (postponed) by mail...
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
				return new ManageableAsset(r_asset, asset.getPath());
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
	
	public static boolean isTechnicallyPossibleToRestore(InterplayAsset asset) {
		return asset.isMasterclip() & asset.hasVideoTrack() & asset.getAudioTracksCount() > 0;
	}
	
	public class ManageableAsset {
		
		private InterplayAsset asset;
		private final String interplay_destination_path;
		private ArchivedAsset localized_archived_version;
		
		private ManageableAsset(InterplayAsset asset) {
			this(asset, asset.getPath());
		}
		
		private ManageableAsset(InterplayAsset asset, String interplay_destination_path) {
			this.asset = asset;
			if (asset == null) {
				throw new NullPointerException("\"asset\" can't to be null");
			}
			this.interplay_destination_path = interplay_destination_path;
		}
		
		public void refreshAsset() throws AssetsFault, IOException {
			asset = asset.refresh();
		}
		
		public String toString() {
			return asset.toString();
		}
		
		public InterplayAsset getAsset() {
			return asset;
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
			return asset.getMyDMAMID() != null & isTechnicallyPossibleToRestore(asset);
		}
		
		/**
		 * Resolve path from AC and get tape locations.
		 * @return never null (or maybe if just_check_dont_return)
		 */
		private ACFile resolveArchivedVersion(boolean just_check_dont_return) throws Exception {
			String asset_mydmam_id = getMyDMAMId();
			
			ACFile ac_file;
			
			String full_ac_path = asset.getAttribute(ac_path_in_interplay);
			if (full_ac_path != null) {
				if (just_check_dont_return) {
					return null;
				}
				String[] path = full_ac_path.split("/", 3);
				ac_file = acapi.getFile(path[1], "/" + path[2], false);
				
				if (ac_file == null) {
					throw new FileNotFoundInArchive("Can't found archived file in ACAPI: " + full_ac_path);
				}
				return ac_file;
			}
			
			// Loggers.AssetsXCross.debug("Search archived version for " + asset_mydmam_id + " in " + archive_storagename);
			
			ArrayList<SourcePathIndexerElement> founded = explorer.getAllIdFromStorage(asset_mydmam_id, archive_storagename);
			if (founded.isEmpty()) {
				throw new FileNotArchived("Can't found archived file with ID " + asset_mydmam_id + " in " + archive_storagename);
			}
			if (founded.size() > 1) {
				throw new InvalidArchivedFile("More than 1 file archived as " + asset_mydmam_id + ": " + founded);
			}
			
			ac_file = bridge_pathindex_archivelocation.getExternalLocation(founded.get(0));
			if (ac_file == null) {
				throw new FileNotFoundInArchive("Can't found archived file in ACAPI: " + founded.get(0).currentpath);
			}
			
			if (ac_file.isOnTape()) {
				ArrayList<AttributeType> attributes = new ArrayList<>();
				attributes.add(InterplayAPI.createAttribute(AttributeGroup.USER, ac_locations_in_interplay, ac_file.getTapeBarcodeLocations().stream().collect(Collectors.joining(" "))));
				attributes.add(InterplayAPI.createAttribute(AttributeGroup.USER, ac_path_in_interplay, "/" + ac_file.share + "/" + ac_file.path));
				interplay.setAttributes(attributes, asset.interplay_uri);
			}
			
			return ac_file;
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
			
			ACFile ac_file = resolveArchivedVersion(false);
			Loggers.AssetsXCross.debug("Found archived version for " + getMyDMAMId() + ": " + ac_file.toString() + " and update Interplay database");
			
			localized_archived_version = new ArchivedAsset(ac_file);// TODO if file is not in tape ?
		}
		
		public String getMyDMAMId() {
			return parseMyDMAMID(asset.getMyDMAMID());
		}
		
		ArchivedAsset getLocalizedArchivedVersion() {
			return localized_archived_version;
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
				destage_job = destage_manager.addFileToDestage(acfile, asset.getMyDMAMID().substring(0, mydmam_id_size), (acf, id) -> {
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
						String source_file_unc = "\\\\" + ac_unc_host + "\\" + acfile.share + "\\" + acfile.path.replace("/", "\\");
						try {
							ArrayList<VariableDefinition> vars = new ArrayList<>();
							vars.add(vantage.createVariableDef(vantage_variable_name_interplay_mastermob, asset.getMobID()));
							vars.add(vantage.createVariableDef(vantage_variable_name_interplay_sourcemob, asset.getSourceID()));
							vars.add(vantage.createVariableDef(vantage_variable_name_interplay_path, FilenameUtils.getFullPathNoEndSeparator(interplay_destination_path)));
							vars.add(vantage.createVariableDef(vantage_variable_name_interplay_file, asset.getDisplayName()));
							vars.add(vantage.createVariableDef(vantage_variable_name_audio_ch, asset.getAudioTracksCount()));
							String tc_in = asset.getStart();
							if (tc_in == null) {
								throw new IOException("Not TC IN for Clip " + asset.getDisplayName());
							} else if (tc_in.equals("")) {
								throw new IOException("TC IN empty for Clip " + asset.getDisplayName());
							}
							vars.add(vantage.createTimeCodeVariableDef(vantage_variable_name_tcin, tc_in, fps));
							
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
				
				try {
					updateDateLastCheckShred(asset, "Restored from archives");
				} catch (AssetsFault | IOException e) {
					onError.accept(e);
				}
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
		
		public void tagToShred(String dest_interplay_dir, long min_date_relative) throws Exception {
			if (canBePurged() == false) {
				if (asset.getMyDMAMID() != null) {
					try {
						resolveArchivedVersion(true);
					} catch (Exception e) {
					}
				}
				updateDateLastCheckShred(asset, null);
				return;
			}
			
			String asset_mydmam_id = getMyDMAMId();
			
			ACFile ac_file = resolveArchivedVersion(false);
			
			if (ac_file.isOnTape() == false) {
				Loggers.AssetsXCross.warn("File is dropped in archive system but not actually archived, ID " + asset_mydmam_id + " in " + archive_storagename);
			} else {
				Loggers.AssetsXCross.debug("Found archived version for " + asset_mydmam_id + ": " + ac_file.toString() + " and update Interplay database");
			}
			
			/**
			 * Search if is linked in a (recent) sequence...
			 */
			if (asset.getRelatives(true).stream().anyMatch(relative -> {
				return (relative.isSequence() | relative.isGroup()) && relative.getLastModificationDate() > min_date_relative;
			})) {
				Loggers.AssetsXCross.info("Clip " + asset.getDisplayName() + " (" + asset_mydmam_id + ") still relative to a recent used sequence");
				return;
			}
			
			/**
			 * Search if is present in a protected directory.
			 */
			if (asset.getPathLinks(false).stream().anyMatch(path -> {
				return do_not_touch_interplay_paths.stream().anyMatch(protected_path -> {
					return path.startsWith(protected_path);
				});
			})) {
				Loggers.AssetsXCross.info("Clip " + asset.getDisplayName() + " (" + asset_mydmam_id + ") is placed in a protected path: tag it, but don't purge it.");
				return;
			}
			
			Loggers.AssetsXCross.info("Clip " + asset.getDisplayName() + " (" + asset_mydmam_id + ") will be put in the Interplay shred directory.");
			
			/**
			 * Create a link in the shred directory
			 */
			interplay.link(asset.interplay_uri, dest_interplay_dir);
			
			/**
			 * Remove from future searchs
			 */
			updateDateLastCheckShred(asset, null);
		}
		
		private void moveToLostAndFoundInterplayPath() throws AssetsFault, IOException {
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(asset.getLastModificationDate());
			String path = createAndGetLostAndFoundInterplayFolder(String.valueOf(c.get(Calendar.YEAR)));
			interplay.move(asset.getPath(), path, false);
		}
		
		private void copyToLostAndFoundInterplayPath() throws AssetsFault, IOException {
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(asset.getLastModificationDate());
			String path = createAndGetLostAndFoundInterplayFolder(String.valueOf(c.get(Calendar.YEAR)));
			interplay.link(asset.interplay_uri, path);
		}
		
		private final Function<InterplayAsset, Stream<InterplayAsset>> search_Sub_Restorable_Masterclips_WO_MyDMAM_Ids = sub_dir_asset -> {
			SearchType search_type = new SearchType();
			SearchGroupType search_group_type = new SearchGroupType();
			search_group_type.setOperator("AND");
			search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.EQUALS, AttributeGroup.SYSTEM, "Type", InterplayAPI.AssetType.masterclip.name()));
			search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.EQUALS, AttributeGroup.SYSTEM, "Media Status", InterplayAPI.MediaStatus.online.name()));
			search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.NOT_EQUALS, AttributeGroup.USER, last_check_shred_in_interplay, "1"));
			search_type.setSearchGroup(search_group_type);
			search_type.setInterplayPathURI(interplay.createURLInterplayPath(FilenameUtils.getFullPath(sub_dir_asset.getPath())));
			
			try {
				SearchResponseType response = interplay.search(search_type);
				InterplayAPI.checkError(response.getErrors());
				
				return interplay.convertSearchResponseToAssetList(response).stream().filter(sub_asset -> {
					return sub_asset.getMyDMAMID() == null;
				}).filter(sub_asset -> {
					return isTechnicallyPossibleToRestore(sub_asset);
				});
			} catch (AssetsFault | IOException e) {
				throw new RuntimeException(e);
			}
		};
		
		private void setMyDMAMIdInInterplay(InterplayAsset asset, String new_mydmam_id) throws AssetsFault, IOException {
			ArrayList<AttributeType> attributes = new ArrayList<>();
			attributes.add(InterplayAPI.createAttribute(AttributeGroup.USER, interplay.getMydmamIDinInterplay(), new_mydmam_id));
			asset.setAttributes(attributes);
		}
		
		private void searchOrphansInProjectDirectories(String purge_interplay_folder) throws AssetsFault, IOException {
			SearchType search_type = new SearchType();
			
			String asset_real_path = FilenameUtils.getFullPath(asset.getPath());
			
			SearchGroupType search_group_type = new SearchGroupType();
			search_group_type.setOperator("AND");
			search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.EQUALS, AttributeGroup.SYSTEM, "Type", InterplayAPI.AssetType.folder.name()));
			search_type.setSearchGroup(search_group_type);
			search_type.setInterplayPathURI(interplay.createURLInterplayPath(asset_real_path));
			
			SearchResponseType response = interplay.search(search_type);
			InterplayAPI.checkError(response.getErrors());
			
			List<InterplayAsset> to_check = interplay.convertSearchResponseToAssetList(response).stream().filter(f_asset -> {
				String name = f_asset.getDisplayName();
				return name.equalsIgnoreCase("RESSOURCES") | name.equalsIgnoreCase("SUJETS") | name.equalsIgnoreCase("RUSHS");
			}).map(search_Sub_Restorable_Masterclips_WO_MyDMAM_Ids).flatMap(s_i_asset -> {
				return s_i_asset;
			}).collect(Collectors.toList());
			
			if (to_check.isEmpty()) {
				updateSeqCheckRelOrphan();
				return;
			}
			
			if (interplay_paths_tag_to_purge_during_orphan_projects_dir_search.stream().anyMatch(remove_path -> {
				return asset_real_path.startsWith(remove_path);
			})) {
				to_check.forEach(s_asset -> {
					/**
					 * Create a link in the shred directory + tag Shred
					 */
					Loggers.AssetsXCross.info("Tag to purge " + s_asset.getDisplayName() + " " + s_asset.getFullPath());
					try {
						interplay.link(s_asset.interplay_uri, purge_interplay_folder);
						updateDateLastCheckShred(s_asset, "Non-archived in an ignored path: " + asset_real_path + ", for an archived show");
					} catch (AssetsFault | IOException e) {
						throw new RuntimeException(e);
					}
				});
				
				updateSeqCheckRelOrphan();
				return;
			}
			
			to_check.forEach(orphan -> {
				InternalId m_id = InternalId.create(orphan, asset.getFullPath(), asset.interplay_uri);
				Loggers.AssetsXCross.info("Create an Id [" + m_id.getId() + "] for " + orphan.getDisplayName() + " " + FilenameUtils.getFullPath(orphan.getPath()));
				try {
					setMyDMAMIdInInterplay(orphan, m_id.getId());
					interplay.link(orphan.interplay_uri, todoarchives_interplay_folder);
				} catch (AssetsFault | IOException e) {
					throw new RuntimeException("Can't operate for " + asset.getDisplayName(), e);
				}
			});
			
			updateSeqCheckRelOrphan();
		}
		
		private void updateSeqCheckRelOrphan() throws AssetsFault, IOException {
			ArrayList<AttributeType> attributes = new ArrayList<>();
			attributes.add(InterplayAPI.createAttribute(AttributeGroup.USER, seq_check_rel_orphan_in_interplay, "1"));
			asset.setAttributes(attributes);
		}
		
	}
	
	private void updateDateLastCheckShred(InterplayAsset asset, String optional_comment) throws AssetsFault, IOException {
		ArrayList<AttributeType> attributes = new ArrayList<>();
		attributes.add(InterplayAPI.createAttribute(AttributeGroup.USER, last_check_shred_in_interplay, "1"));
		if (optional_comment != null) {
			attributes.add(InterplayAPI.createAttribute(AttributeGroup.USER, "Comments", optional_comment));
		}
		asset.setAttributes(attributes);
	}
	
	private static final SimpleDateFormat date_link_dir = new SimpleDateFormat("yyyy-MM-dd");
	private String purge_interplay_folder;
	
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
		
	}
	
	private String createPurgeInterplayFolder() throws AssetsFault, IOException {
		String dest_dir = purge_interplay_folder + "/" + date_link_dir.format(System.currentTimeMillis());
		interplay.createFolder(dest_dir);
		return dest_dir;
	}
	
	/**
	 * year > Interplay path
	 */
	private transient HashMap<String, String> lost_and_found_interplay_paths_by_years;
	
	private String createAndGetLostAndFoundInterplayFolder(String year) {
		if (lost_and_found_interplay_paths_by_years == null) {
			lost_and_found_interplay_paths_by_years = new HashMap<>(1);
		}
		
		return lost_and_found_interplay_paths_by_years.computeIfAbsent(year, _year -> {
			String dest_dir = lostandfound_base_interplay_folder + "/" + _year;
			try {
				interplay.createFolder(dest_dir);
			} catch (AssetsFault | IOException e) {
				throw new RuntimeException(e);
			}
			return dest_dir;
		});
	}
	
	public void tagForShred(String search_root_path, int since_update_month, int since_used_month) throws Exception {
		Calendar calendar_update = Calendar.getInstance();
		calendar_update.add(Calendar.MONTH, -Math.abs(since_update_month));
		
		SearchType search_type = new SearchType();
		
		SearchGroupType search_group_type = new SearchGroupType();
		search_group_type.setOperator("AND");
		search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.EQUALS, AttributeGroup.SYSTEM, "Type", InterplayAPI.AssetType.masterclip.name()));
		search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.EQUALS, AttributeGroup.SYSTEM, "Media Status", InterplayAPI.MediaStatus.online.name()));
		search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.LESS_THAN, AttributeGroup.SYSTEM, "Modified Date", InterplayAPI.formatInterplayDate(calendar_update.getTimeInMillis())));
		search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.NOT_EQUALS, AttributeGroup.USER, last_check_shred_in_interplay, "1"));
		// search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.GREATER_THAN, AttributeGroup.USER, interplay.getMydmamIDinInterplay(), String.valueOf(0)));
		
		search_type.setSearchGroup(search_group_type);
		search_type.setInterplayPathURI(interplay.createURLInterplayPath(search_root_path));
		
		SearchResponseType response = interplay.search(search_type);
		InterplayAPI.checkError(response.getErrors());
		
		List<ManageableAsset> m_assets = interplay.convertSearchResponseToAssetList(response).stream().map(asset -> {
			return new ManageableAsset(asset);
		}).collect(Collectors.toList());
		
		Calendar calendar_used = Calendar.getInstance();
		calendar_used.add(Calendar.MONTH, -Math.abs(since_used_month));
		
		String dest_dir = createPurgeInterplayFolder();
		
		/**
		 * Checks if is restorable
		 */
		m_assets.stream().forEach(asset -> {
			try {
				asset.tagToShred(dest_dir, calendar_used.getTimeInMillis());
			} catch (FileNotFoundException e) {
				Loggers.AssetsXCross.warn("Can't found asset " + asset + " > " + e.getMessage());
			} catch (Exception e) {
				throw new RuntimeException("Error with asset " + asset, e);
			}
		});
	}
	
	/**
	 * Don't remove files, only assets.
	 */
	public void removeOldAssets(String search_root_path, int since_update_month) throws AssetsFault, IOException, InterruptedException {
		Calendar calendar_update = Calendar.getInstance();
		calendar_update.add(Calendar.MONTH, -Math.abs(since_update_month));
		
		SearchType search_type = new SearchType();
		search_type.setMaxResults(500);
		
		SearchGroupType search_group_type = new SearchGroupType();
		search_group_type.setOperator("AND");
		search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.NOT_EQUALS, AttributeGroup.SYSTEM, "Type", InterplayAPI.AssetType.folder.name()));
		search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.LESS_THAN, AttributeGroup.SYSTEM, "Created Date", InterplayAPI.formatInterplayDate(calendar_update.getTimeInMillis())));
		
		search_type.setSearchGroup(search_group_type);
		search_type.setInterplayPathURI(interplay.createURLInterplayPath(search_root_path));
		
		SearchResponseType response = interplay.search(search_type);
		InterplayAPI.checkError(response.getErrors());
		Loggers.AssetsXCross.info("Search assets to delete in " + search_root_path + " older from " + Loggers.dateLog(calendar_update.getTimeInMillis()) + "...");
		List<InterplayAsset> assets = interplay.convertSearchResponseToAssetList(response);
		
		while (assets.isEmpty() == false) {
			List<String> assets_uri_list = assets.stream().map(asset -> asset.getPath()).collect(Collectors.toList());
			
			Loggers.AssetsXCross.info("Start to delete " + assets_uri_list.size() + " assets");
			
			final ArrayList<String> deleted_uri_path_list = new ArrayList<>();
			interplay.delete(assets_uri_list, true, false, d_uri_asset_path -> {
				deleted_uri_path_list.addAll(d_uri_asset_path);
			}, null, false);
			
			assets.stream().map(asset_to_delete -> {
				return interplay.createURLInterplayPath(asset_to_delete.getPath());
			}).filter(asset_uri_path_to_delete -> {
				return deleted_uri_path_list.contains(asset_uri_path_to_delete) == false;
			}).forEach(asset -> {
				Loggers.AssetsXCross.warn("Can't delete " + asset);
			});
			
			assets.stream().map(asset_to_delete -> {
				return interplay.createURLInterplayPath(asset_to_delete.getPath());
			}).filter(asset_uri_path_to_delete -> {
				return deleted_uri_path_list.contains(asset_uri_path_to_delete);
			}).forEach(asset -> {
				Loggers.AssetsXCross.trace("Delete " + asset);
			});
			
			Thread.sleep(100);
			
			Loggers.AssetsXCross.info("Search assets to delete in " + search_root_path + " older from " + Loggers.dateLog(calendar_update.getTimeInMillis()) + "...");
			response = interplay.search(search_type);
			InterplayAPI.checkError(response.getErrors());
			assets = interplay.convertSearchResponseToAssetList(response);
		}
	}
	
	public void searchAndTagOrphansInProjectDirectories(String search_root_path, int since_created_month, int grace_time_sec_non_archived_since_month /*, boolean bypass_archive_check*/) throws Exception {
		SearchType search_type = new SearchType();
		
		Calendar calendar_update = Calendar.getInstance();
		calendar_update.add(Calendar.MONTH, -Math.abs(since_created_month));
		
		SearchGroupType search_group_type = new SearchGroupType();
		search_group_type.setOperator("AND");
		search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.EQUALS, AttributeGroup.SYSTEM, "Type", InterplayAPI.AssetType.sequence.name()));
		search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.LESS_THAN, AttributeGroup.SYSTEM, "Created Date", InterplayAPI.formatInterplayDate(calendar_update.getTimeInMillis())));
		search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.NOT_EQUALS, AttributeGroup.USER, seq_check_rel_orphan_in_interplay, "1"));
		
		interplay_paths_ignore_during_orphan_projects_dir_search.forEach(path -> {
			search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.NOT_CONTAINS, AttributeGroup.SYSTEM, "Path", path));
		});
		
		/*if (bypass_archive_check) {
			search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.CONTAINS, AttributeGroup.USER, ac_locations_in_interplay, " "));
		}*/
		Calendar calendar_grace_sec_non_archived = Calendar.getInstance();
		calendar_grace_sec_non_archived.add(Calendar.MONTH, -Math.abs(grace_time_sec_non_archived_since_month));
		
		search_type.setSearchGroup(search_group_type);
		search_type.setInterplayPathURI(interplay.createURLInterplayPath(search_root_path));
		
		SearchResponseType response = interplay.search(search_type);
		InterplayAPI.checkError(response.getErrors());
		
		List<ManageableAsset> m_assets = interplay.convertSearchResponseToAssetList(response).stream().filter(asset -> {
			return asset.getMyDMAMID() != null;
		}).map(asset -> {
			return new ManageableAsset(asset);
		}).collect(Collectors.toList());
		
		String purge_interplay_folder = createPurgeInterplayFolder();
		
		m_assets.stream().filter(asset -> {
			/*if (bypass_archive_check) {
				return true;
			}*/
			try {
				asset.resolveArchivedVersion(true);
				Loggers.AssetsXCross.info("Archived sequence: " + asset.asset.getDisplayName());
				return true;
			} catch (Exception e) {
				if (asset.asset.getLastModificationDate() < calendar_grace_sec_non_archived.getTimeInMillis()) {
					Loggers.AssetsXCross.info("Non archived sequence, but too old and not needs to wait to be archived " + asset.asset.getDisplayName());
					return true;
				}
				Loggers.AssetsXCross.info("Non archived sequence, but still recent " + FilenameUtils.getFullPath(asset.asset.getPath()) + asset.asset.getDisplayName() + " (" + asset.asset.getMyDMAMID() + ")");
				return false;
			}
		}).forEach(asset -> {
			try {
				asset.searchOrphansInProjectDirectories(purge_interplay_folder);
			} catch (AssetsFault | IOException e) {
				throw new RuntimeException("Can't process " + asset.toString(), e);
			}
		});
	}
	
	public VantageJob archive(InterplayAsset asset, boolean delete_asset_after_start_job) throws AssetsFault, IOException, IWorkflowSubmitSubmitFileUnlicensedSdkExceptionFaultFaultMessage, IWorkflowSubmitSubmitFileWorkflowDoesNotExistExceptionFaultFaultMessage, IWorkflowSubmitSubmitFileWorkflowInvalidStateExceptionFaultFaultMessage, IDomainGetWorkflowsUnlicensedSdkExceptionFaultFaultMessage {
		if (asset == null) {
			throw new NullPointerException("\"asset\" can't to be null");
		}
		if (asset.isMasterclip() == false) {
			throw new NullPointerException("Can't archive other think like masterclips");
		}
		if (asset.isOnline() == false) {
			throw new NullPointerException("Can't archive offline masterclip");
		}
		if (asset.getMyDMAMID() == null) {
			throw new NullPointerException("Can't archive masterclip without MyDMAM Id");
		}
		
		List<FileLocationType> all_atoms = interplay.getFileDetailsByURI(asset.interplay_uri).get(asset.interplay_uri);
		
		List<String> atom_unc_paths_for_asset = all_atoms.stream().filter(flt -> {
			/**
			 * Get only online video and audio atoms.
			 */
			return flt.getStatus().equalsIgnoreCase("Online") && (flt.getTrack().startsWith("A") | flt.getTrack().startsWith("V"));
		}).map(flt -> {
			return flt.getInterplayURI();
		}).distinct().map(atom_uri -> {
			/**
			 * Remove multiple identical Atoms by each distinct InterplayURIs
			 */
			return all_atoms.stream().filter(flt -> {
				return flt.getInterplayURI().equals(atom_uri);
			}).findFirst().get();
		}).sorted((l, r) -> {
			/**
			 * Vx first, Ax after
			 */
			String l_track = l.getTrack();
			String r_track = r.getTrack();
			
			if (l_track.substring(0, 1).equals(r_track.substring(0, 1))) {
				/**
				 * Both video/audio
				 */
				return Integer.valueOf(l_track.substring(1)) - Integer.valueOf(r_track.substring(1));
			} else if (l_track.substring(0, 1).equals("A") & r_track.substring(0, 1).equals("V")) {
				/**
				 * Audio < Video
				 */
				return 1;
			}
			return -1;
		}).map(flt -> {
			return flt.getFilePath();
		}).collect(Collectors.toList());
		
		if (atom_unc_paths_for_asset.isEmpty()) {
			throw new NullPointerException("Not found valid Atoms for " + asset.getMyDMAMID() + " " + asset.getDisplayName());
		}
		
		String mydmam_id = asset.getMyDMAMID();
		
		String source_file_unc = atom_unc_paths_for_asset.get(0);
		ArrayList<VariableDefinition> vars = new ArrayList<>();
		if (atom_unc_paths_for_asset.size() > 1) {
			AtomicInteger count = new AtomicInteger(1);
			atom_unc_paths_for_asset.stream().skip(1).limit(4).forEach(unc_atom -> {
				vars.add(vantage.createVariableDefURI(vantage_variable_name_filepathaudio + " " + count.getAndIncrement(), unc_atom));
			});
		}
		vars.add(vantage.createVariableDef(vantage_variable_name_mediaid, mydmam_id));
		vars.add(vantage.createVariableDef(vantage_variable_name_archivepath, vantage_archive_destbasepath + "/" + mydmam_id.substring(0, 4)));
		
		VantageJob job = vantage.createJob(source_file_unc, vantage_archive_workflow_name, vars, "Archiving Avid Atoms for " + mydmam_id);
		
		if (delete_asset_after_start_job) {
			asset.delete(true, false, null, null, false);
		}
		
		return job;
	}
	
	public List<VantageJob> searchAssetsToArchive(int max_results) throws AssetsFault, IOException {
		SearchType search_type = new SearchType();
		search_type.setMaxResults(max_results);
		
		SearchGroupType search_group_type = new SearchGroupType();
		search_group_type.setOperator("AND");
		search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.EQUALS, AttributeGroup.SYSTEM, "Type", AssetType.masterclip.name()));
		search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.EQUALS, AttributeGroup.SYSTEM, "Media Status", InterplayAPI.MediaStatus.online.name()));
		search_type.setSearchGroup(search_group_type);
		search_type.setInterplayPathURI(interplay.createURLInterplayPath(todoarchives_interplay_folder));
		
		SearchResponseType response = interplay.search(search_type);
		InterplayAPI.checkError(response.getErrors());
		
		return interplay.convertSearchResponseToAssetList(response).stream().map(asset -> {
			try {
				VantageJob job = archive(asset, true);
				Loggers.AssetsXCross.info("Start archive job for " + asset.getMyDMAMID() + " " + asset.getDisplayName() + ". Vantage #" + job.getJobId());
				return job;
			} catch (Exception e) {
				Loggers.AssetsXCross.error("Can't start archive job for " + asset.getMyDMAMID() + " " + asset.getDisplayName(), e);
				return null;
			}
		}).filter(job -> {
			return job != null;
		}).collect(Collectors.toList());
	}
	
	/**
	 * @param search_sequences > false == masterclip
	 */
	public void tagArchiveStatusForRecent(String search_root_path, int since_after_update_month, boolean search_sequences) throws Exception {
		Calendar calendar_update = Calendar.getInstance();
		calendar_update.add(Calendar.MONTH, -Math.abs(since_after_update_month));
		
		SearchType search_type = new SearchType();
		
		SearchGroupType search_group_type = new SearchGroupType();
		search_group_type.setOperator("AND");
		if (search_sequences) {
			search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.EQUALS, AttributeGroup.SYSTEM, "Type", InterplayAPI.AssetType.sequence.name()));
		} else {
			/**
			 * masterclip
			 */
			search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.EQUALS, AttributeGroup.SYSTEM, "Type", InterplayAPI.AssetType.masterclip.name()));
			search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.EQUALS, AttributeGroup.SYSTEM, "Media Status", InterplayAPI.MediaStatus.online.name()));
		}
		
		long start_date = calendar_update.getTimeInMillis();
		long end_date = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2);
		search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.GREATER_THAN_OR_EQUAL_TO, AttributeGroup.SYSTEM, "Modified Date", InterplayAPI.formatInterplayDate(start_date)));
		search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.LESS_THAN, AttributeGroup.SYSTEM, "Modified Date", InterplayAPI.formatInterplayDate(end_date)));
		search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.NOT_CONTAINS, AttributeGroup.USER, ac_locations_in_interplay, " "));
		search_type.setSearchGroup(search_group_type);
		search_type.setInterplayPathURI(interplay.createURLInterplayPath(search_root_path));
		
		SearchResponseType response = interplay.search(search_type);
		InterplayAPI.checkError(response.getErrors());
		
		if (response.getResults().getAssetDescription().isEmpty()) {
			return;
		}
		Loggers.AssetsXCross.info("First Interplay search get " + response.getResults().getAssetDescription().size() + " asset(s)");
		
		List<ManageableAsset> m_assets = interplay.convertSearchResponseToAssetList(response).stream().map(asset -> {
			return new ManageableAsset(asset);
		}).filter(m_asset -> {
			return m_asset.getMyDMAMId() != null;
		}).collect(Collectors.toList());
		
		Loggers.AssetsXCross.info("Only " + m_assets.size() + " asset(s) match for this search (have an usable Id)");
		
		/**
		 * Checks if is restorable
		 */
		m_assets.stream().forEach(asset -> {
			try {
				ACFile f = asset.resolveArchivedVersion(false);
				Loggers.AssetsXCross.info("Founded archived asset " + asset + " on " + f.getTapeBarcodeLocations());
			} catch (FileNotFoundException e) {
				Loggers.AssetsXCross.info("Not (yet) archived asset " + asset + " > " + e.getMessage());
			} catch (Exception e) {
				throw new RuntimeException("Error with asset " + asset, e);
			}
		});
	}
	
	public void tagToPurgeOrArchiveIsolatesMClipsInPath(String path, int since_after_update_month) throws Exception {
		Calendar calendar_update = Calendar.getInstance();
		calendar_update.add(Calendar.MONTH, -Math.abs(since_after_update_month));
		
		SearchType search_type = new SearchType();
		
		SearchGroupType search_group_type = new SearchGroupType();
		search_group_type.setOperator("AND");
		search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.EQUALS, AttributeGroup.SYSTEM, "Type", InterplayAPI.AssetType.masterclip.name()));
		search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.EQUALS, AttributeGroup.SYSTEM, "Media Status", InterplayAPI.MediaStatus.online.name()));
		search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.LESS_THAN, AttributeGroup.SYSTEM, "Modified Date", InterplayAPI.formatInterplayDate(calendar_update.getTimeInMillis())));
		search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.NOT_CONTAINS, AttributeGroup.USER, ac_locations_in_interplay, " "));
		search_type.setSearchGroup(search_group_type);
		search_type.setInterplayPathURI(interplay.createURLInterplayPath(path));
		
		search_type.setMaxResults(500);// XXX
		
		SearchResponseType response = interplay.search(search_type);
		InterplayAPI.checkError(response.getErrors());
		
		List<ManageableAsset> m_assets = interplay.convertSearchResponseToAssetList(response).stream().filter(asset -> {
			return path.equals(asset.getFullPath());
		}).map(asset -> {
			return new ManageableAsset(asset);
		}).collect(Collectors.toList());
		
		/**
		 * Search all mclips with a real Id (Or if the name is a an ID)
		 */
		m_assets.forEach(m_asset -> {
			try {
				String id = m_asset.getMyDMAMId();
				if (id == null) {
					id = parseMyDMAMID(m_asset.asset.getDisplayName());
					if (id != null) {
						Loggers.AssetsXCross.info("Set clip ID " + id + " for " + m_asset.asset.getDisplayName());
						m_asset.setMyDMAMIdInInterplay(m_asset.asset, id);
						m_asset.refreshAsset();
						if (id.equals(m_asset.getMyDMAMId()) == false) {
							throw new IOException("Can't set new MyDMAM Id [" + id + "] for " + m_asset + "(" + m_asset.getMyDMAMId() + ")");
						}
						try {
							ACFile f = m_asset.resolveArchivedVersion(false);
							Loggers.AssetsXCross.info("Founded archived asset " + m_asset + " on " + f.getTapeBarcodeLocations());
							if (m_asset.asset.getPathLinks(false).isEmpty() == false) {
								/**
								 * If item is linked outside
								 */
								m_asset.asset.delete(true, false, uri -> {
									Loggers.AssetsXCross.info("Remove asset " + m_asset);
								}, null, true);// TODO set false
							} else {
								Loggers.AssetsXCross.info("Move to Lost+Found orphan archived asset " + m_asset);
								m_asset.moveToLostAndFoundInterplayPath();
							}
							return;
						} catch (FileNotArchived e) {
							/**
							 * Not archived ? Continue...
							 */
						}
					} else {
						/**
						 * No id ?
						 */
						Loggers.AssetsXCross.info("Move to Lost+Found orphan non archived w/o Id asset " + m_asset);
						m_asset.moveToLostAndFoundInterplayPath();
						return;
					}
				}
				
				/**
				 * MClip has now an Id, but is not yet archived
				 */
				if (m_asset.asset.getPathLinks(false).isEmpty()) {
					/**
					 * if item is not linked outside
					 */
					Loggers.AssetsXCross.info("Copy to Lost+Found orphan non archived (" + id + ") asset " + m_asset);
					m_asset.copyToLostAndFoundInterplayPath();
				}
				
				Loggers.AssetsXCross.info("Move to " + todoarchives_interplay_folder + " non archived (" + id + ") asset " + m_asset);
				interplay.move(m_asset.asset.getPath(), todoarchives_interplay_folder, false);
			} catch (InvalidArchivedFile | FileNotFoundInArchive e) {
				Loggers.AssetsXCross.error("Found invalid archived file for " + m_asset, e);
			} catch (Exception e) {
				throw new RuntimeException("Error (with " + m_asset + ")", e);
			}
		});
	}
	
	public void restartArchive(String search_root_path, int since_update_month) throws Exception {
		Calendar calendar_update = Calendar.getInstance();
		calendar_update.add(Calendar.MONTH, -Math.abs(since_update_month));
		
		SearchType search_type = new SearchType();
		
		SearchGroupType search_group_type = new SearchGroupType();
		search_group_type.setOperator("AND");
		search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.EQUALS, AttributeGroup.SYSTEM, "Type", InterplayAPI.AssetType.masterclip.name()));
		search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.EQUALS, AttributeGroup.SYSTEM, "Media Status", InterplayAPI.MediaStatus.online.name()));
		search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.LESS_THAN, AttributeGroup.SYSTEM, "Modified Date", InterplayAPI.formatInterplayDate(calendar_update.getTimeInMillis())));
		search_group_type.getAttributeCondition().add(InterplayAPI.createAttributeCondition(Condition.NOT_CONTAINS, AttributeGroup.USER, ac_locations_in_interplay, " "));
		
		search_type.setSearchGroup(search_group_type);
		search_type.setInterplayPathURI(interplay.createURLInterplayPath(search_root_path));
		
		SearchResponseType response = interplay.search(search_type);
		InterplayAPI.checkError(response.getErrors());
		
		List<ManageableAsset> m_assets = interplay.convertSearchResponseToAssetList(response).stream().map(asset -> {
			return new ManageableAsset(asset);
		}).filter(m_asset -> {
			return m_asset.getMyDMAMId() != null;
		}).collect(Collectors.toList());
		
		m_assets.stream().forEach(asset -> {
			try {
				asset.resolveArchivedVersion(false);
			} catch (FileNotArchived e) {
				try {
					if (asset.asset.getPathLinks(true).stream().noneMatch(path -> {
						return todoarchives_interplay_folder.equals(path);
					})) {
						Loggers.AssetsXCross.info("Propose to archive asset " + asset.getMyDMAMId() + " " + asset);
						interplay.link(asset.asset.interplay_uri, todoarchives_interplay_folder);
					} else {
						Loggers.AssetsXCross.info("Asset already waits to be archived " + asset.getMyDMAMId() + " " + asset);
					}
				} catch (AssetsFault | IOException e1) {
					throw new RuntimeException("Error with asset during link " + asset, e);
				}
			} catch (Exception e) {
				throw new RuntimeException("Error with asset " + asset, e);
			}
		});
	}
	
}
