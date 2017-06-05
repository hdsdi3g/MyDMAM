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
import hd3gtv.mydmam.metadata.container.EntryRenderer;
import hd3gtv.mydmam.transcode.TranscodeProfile;
import hd3gtv.mydmam.transcode.TranscodeProfile.ProcessConfiguration;
import hd3gtv.mydmam.transcode.images.ImageAttributeGeometry.Compare;
import hd3gtv.tools.CopyMove;
import hd3gtv.tools.ExecprocessGettext;
import hd3gtv.tools.StoppableProcessing;

public abstract class ImageMagickThumbnailer implements MetadataExtractor {
	
	protected TranscodeProfile tprofile_opaque;
	protected TranscodeProfile tprofile_alpha;
	protected PreviewType preview_type;
	protected static File icc_profile;
	
	static {
		icc_profile = new File(MyDMAM.APP_ROOT_PLAY_CONF_DIRECTORY + File.separator + "srgb.icc");
		try {
			CopyMove.checkExistsCanRead(icc_profile);
		} catch (Exception e) {
			Loggers.Transcode.error("Can't found icc profile file in conf directory.", e);
			System.exit(1);
		}
	}
	
	public static File getICCProfile() {
		return icc_profile;
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
		ImageAttributes image_attributes = container.getByType(getEntryRendererESType(), ImageAttributes.class);
		if (image_attributes == null) {
			Loggers.Transcode.debug("No image_attributes for this container: " + container);
			return null;
		}
		
		return subProcess(container, stoppable, container.getPhysicalSource(), image_attributes);
	}
	
	protected abstract String getFileReferenceName();
	
	protected abstract String getEntryRendererESType();
	
	/**
	 * @return if null, cancel this image format
	 */
	protected abstract TranscodeProfile getProfileIfItJudiciousToDoThumbnail(Compare compare, ImageAttributes image_attributes, TranscodeProfile primary_tprofile);
	
	protected abstract boolean isPersonalizedSize(Compare compare, ImageAttributes image_attributes);
	
	protected ContainerEntryResult subProcess(Container container, StoppableProcessing stoppable, File physical_source, ImageAttributes image_attributes) throws Exception {
		TranscodeProfile tprofile = tprofile_opaque;
		if (image_attributes.alpha != null) {
			tprofile = tprofile_alpha;
		}
		
		Compare compare = image_attributes.geometry.compare(tprofile);
		
		tprofile = getProfileIfItJudiciousToDoThumbnail(compare, image_attributes, tprofile);
		
		if (tprofile == null) {
			Loggers.Transcode.debug("Image is too litte to fit in this extractor (" + getClass().getName() + "), source: " + container + ", physical_source: " + physical_source);
			return null;
		}
		
		boolean is_personalizedsize = isPersonalizedSize(compare, image_attributes);
		
		RenderedFile element = new RenderedFile(getFileReferenceName(), tprofile.getExtension("jpg"));
		ProcessConfiguration process_conf = tprofile.createProcessConfiguration(physical_source, element.getTempFile(), container);
		process_conf.getInitialParams().addAll(ImageMagickAnalyser.convert_limits_params);
		process_conf.getParamTags().put("ICCPROFILE", icc_profile.getAbsolutePath());
		if (is_personalizedsize) {
			process_conf.getParamTags().put("THUMBNAILSIZE", image_attributes.geometry.width + "x" + image_attributes.geometry.height);
			process_conf.getParamTags().put("CHECKERBOARDSIZE", (image_attributes.geometry.width * 2) + "x" + (image_attributes.geometry.height * 2));
		}
		ExecprocessGettext process = process_conf.prepareExecprocess();
		Loggers.Transcode.debug("Start conversion, process_conf: " + process_conf.toString());
		process.start();
		
		EntryRenderer thumbnail = new EntryRenderer(getEntryRendererESType());
		
		Container thumbnail_file_container = new MetadataIndexingOperation(element.getTempFile()).setLimit(MetadataIndexingLimit.FAST).doIndexing();
		ImageAttributes thumbnail_image_attributes = thumbnail_file_container.getByType(getEntryRendererESType(), ImageAttributes.class);
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
	
	public boolean isCanUsedInMasterAsPreview(Container container) {
		return false;
	}
}
