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
 * Copyright (C) hdsdi3g for hd3g.tv 2015
 * 
*/
package hd3gtv.mydmam.transcode.images;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.metadata.ContainerEntryResult;
import hd3gtv.mydmam.metadata.MetadataExtractor;
import hd3gtv.mydmam.metadata.MetadataIndexingLimit;
import hd3gtv.mydmam.metadata.MetadataIndexingOperation;
import hd3gtv.mydmam.metadata.PreviewType;
import hd3gtv.mydmam.metadata.RenderedFile;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.ContainerEntry;
import hd3gtv.mydmam.metadata.container.EntryRenderer;
import hd3gtv.mydmam.transcode.TranscodeProfile;
import hd3gtv.mydmam.transcode.TranscodeProfile.ProcessConfiguration;
import hd3gtv.mydmam.transcode.images.ImageAttributeGeometry.Compare;
import hd3gtv.tools.CopyMove;
import hd3gtv.tools.ExecprocessGettext;
import hd3gtv.tools.StoppableProcessing;

public class ImageMagickThumbnailer implements MetadataExtractor {
	
	public static class FullDisplay extends EntryRenderer {
		public String getES_Type() {
			return "imthumbnail_full";
		}
		
		public static final String profile_name = "convert_full_display";
		public static final String profile_personalizedsize_name = "convert_personalizedsize";
	}
	
	public static class Cartridge extends EntryRenderer {
		public String getES_Type() {
			return "imthumbnail_cartridge";
		}
		
		public static final String profile_name = "convert_cartridge";
	}
	
	public static class Icon extends EntryRenderer {
		public String getES_Type() {
			return "imthumbnail_icon";
		}
		
		public static final String profile_name = "convert_icon";
	}
	
	private TranscodeProfile tprofile_opaque;
	private TranscodeProfile tprofile_alpha;
	private Class<? extends EntryRenderer> root_entry_class;
	private PreviewType preview_type;
	protected static File icc_profile;
	
	static {
		icc_profile = new File(MyDMAM.APP_ROOT_PLAY_CONF_DIRECTORY + File.separator + "srgb.icc");
		try {
			CopyMove.checkExistsCanRead(icc_profile);
		} catch (Exception e) {
			Loggers.Transcode.error("Can't found icc profile file in conf directory.", e);
		}
	}
	
	public static File getICCProfile() {
		return icc_profile;
	}
	
	public ImageMagickThumbnailer(Class<? extends EntryRenderer> root_entry_class, PreviewType preview_type, String profile_name) {
		this.root_entry_class = root_entry_class;
		if (root_entry_class == null) {
			throw new NullPointerException("\"root_entry_class\" can't to be null");
		}
		this.preview_type = preview_type;
		if (preview_type == null) {
			throw new NullPointerException("\"preview_type\" can't to be null");
		}
		
		if (TranscodeProfile.isConfigured()) {
			tprofile_opaque = TranscodeProfile.getTranscodeProfile(profile_name);
			tprofile_alpha = TranscodeProfile.getTranscodeProfile(profile_name + "_alpha");
		}
	}
	
	public boolean canProcessThisMimeType(String mimetype) {
		return ImageMagickAnalyser.mimetype_list.contains(mimetype);
	}
	
	public boolean isEnabled() {
		return tprofile_opaque != null;
	}
	
	public String getLongName() {
		return "ImageMagick low-res image thumbnailer (" + preview_type + ")";
	}
	
	public PreviewType getPreviewTypeForRenderer(Container container, EntryRenderer entry) {
		return preview_type;
	}
	
	public ContainerEntryResult processFast(Container container) throws Exception {
		return null;
	}
	
	public ContainerEntryResult processFull(Container container, StoppableProcessing stoppable) throws Exception {
		ImageAttributes image_attributes = container.getByClass(ImageAttributes.class);
		if (image_attributes == null) {
			Loggers.Transcode.debug("No image_attributes for this container: " + container);
			return null;
		}
		
		return subProcess(container, stoppable, container.getPhysicalSource(), image_attributes);
	}
	
	protected ContainerEntryResult subProcess(Container container, StoppableProcessing stoppable, File physical_source, ImageAttributes image_attributes) throws Exception {
		TranscodeProfile tprofile = tprofile_opaque;
		if (image_attributes.alpha != null) {
			tprofile = tprofile_alpha;
		}
		
		/*
		 * Choose list :
					img < icon		img < cartridge	img < full		img > full
		full		nope			personalized	personalized	full
		cartridge	nope			nope			cartridge		cartridge
		icon		icon			icon			icon			icon 
		 * */
		
		Compare compare = image_attributes.geometry.compare(tprofile);
		boolean is_personalizedsize = false;
		if (root_entry_class == FullDisplay.class) {
			if (compare == Compare.IS_SMALLER_THAN_PROFILE) {
				if (image_attributes.geometry.compare(TranscodeProfile.getTranscodeProfile(Icon.profile_name)) == Compare.IS_SMALLER_THAN_PROFILE) {
					/**
					 * full, but img < icon => nope
					 */
					Loggers.Transcode.debug(
							"Image size (full) is too litte to fit in this profile, source: " + container + ", physical_source: " + physical_source + ", output format: " + tprofile.getOutputformat());
					return null;
				}
				if (image_attributes.alpha == null) {
					tprofile = TranscodeProfile.getTranscodeProfile(FullDisplay.profile_personalizedsize_name);
				} else {
					tprofile = TranscodeProfile.getTranscodeProfile(FullDisplay.profile_personalizedsize_name + "_alpha");
				}
				is_personalizedsize = true;
			}
		} else if (root_entry_class == Cartridge.class) {
			if (compare == Compare.IS_SMALLER_THAN_PROFILE) {
				/**
				 * cartridge, but img < cartridge => nope
				 */
				Loggers.Transcode.debug("Image size (cartridge) is too litte to fit in this profile, source: " + container + ", physical_source: " + physical_source + ", output format: "
						+ tprofile.getOutputformat());
				return null;
			}
		} else if (root_entry_class == null) {
			throw new NullPointerException("\"root_entry_class\" can't to be null");
		}
		if (tprofile == null) {
			throw new NullPointerException("\"tprofile\" can't to be null");
		}
		
		RenderedFile element = new RenderedFile(root_entry_class.getSimpleName().toLowerCase(), tprofile.getExtension("jpg"));
		ProcessConfiguration process_conf = tprofile.createProcessConfiguration(physical_source, element.getTempFile());
		process_conf.getInitialParams().addAll(ImageMagickAnalyser.convert_limits_params);
		process_conf.getParamTags().put("ICCPROFILE", icc_profile.getAbsolutePath());
		if (is_personalizedsize) {
			process_conf.getParamTags().put("THUMBNAILSIZE", image_attributes.geometry.width + "x" + image_attributes.geometry.height);
			process_conf.getParamTags().put("CHECKERBOARDSIZE", (image_attributes.geometry.width * 2) + "x" + (image_attributes.geometry.height * 2));
		}
		ExecprocessGettext process = process_conf.prepareExecprocess();
		Loggers.Transcode.debug("Start conversion, process_conf: " + process_conf.toString());
		process.start();
		
		EntryRenderer thumbnail = root_entry_class.newInstance();
		
		Container thumbnail_file_container = new MetadataIndexingOperation(element.getTempFile()).setLimit(MetadataIndexingLimit.FAST).doIndexing();
		ImageAttributes thumbnail_image_attributes = thumbnail_file_container.getByClass(ImageAttributes.class);
		if (thumbnail_image_attributes == null) {
			Loggers.Transcode.debug("No image_attributes for the snapshot file container: " + thumbnail_image_attributes);
			return null;
		}
		if (thumbnail_image_attributes.geometry == null) {
			Loggers.Transcode.debug("No image_attributes.geometry for the snapshot file container: " + thumbnail_image_attributes);
			return null;
		}
		thumbnail.getOptions().addProperty("height", thumbnail_image_attributes.geometry.height);
		thumbnail.getOptions().addProperty("width", thumbnail_image_attributes.geometry.width);
		
		return new ContainerEntryResult(element.consolidateAndExportToEntry(thumbnail, container, this));
	}
	
	public List<String> getMimeFileListCanUsedInMasterAsPreview() {
		return null;
	}
	
	public List<Class<? extends ContainerEntry>> getAllRootEntryClasses() {
		return Arrays.asList(root_entry_class);
	}
	
	public boolean isCanUsedInMasterAsPreview(Container container) {
		return false;
	}
}
