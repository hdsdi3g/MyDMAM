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

import hd3gtv.log2.Log2;
import hd3gtv.mydmam.metadata.MetadataCenter;
import hd3gtv.mydmam.metadata.PreviewType;
import hd3gtv.mydmam.metadata.RenderedFile;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.EntryRenderer;
import hd3gtv.mydmam.transcode.mtdgenerator.FFmpegAlbumartwork.Albumartwork;
import hd3gtv.mydmam.transcode.mtdgenerator.FFmpegSnapshoot.Snapshoot;
import hd3gtv.mydmam.transcode.mtdgenerator.FFprobeAnalyser;

import java.io.File;
import java.util.List;

/**
 * Create valid and usable thumbnails from raw ffmpeg snapshoots and albums artworks.
 */
public class ImageMagickFFmpegThumbnailer extends ImageMagickThumbnailer {
	
	// TODO limits : -limit memory 100MB -limit map 100MB -limit area 100MB -limit disk 30MB -limit file 50 -limit time 50
	
	public ImageMagickFFmpegThumbnailer(Class<? extends EntryRenderer> root_entry_class, PreviewType preview_type, String profile_name) {
		super(root_entry_class, preview_type, profile_name);
	}
	
	public boolean canProcessThis(String mimetype) {
		if (FFprobeAnalyser.canProcessThisVideoOnly(mimetype)) return true;
		if (FFprobeAnalyser.canProcessThisAudioOnly(mimetype)) return true;
		return false;
	}
	
	public boolean isEnabled() {
		return super.isEnabled();
	}
	
	public String getLongName() {
		return "ImageMagick low-res thumbnailer from ffmpeg snapshoots and artworks";
	}
	
	public PreviewType getPreviewTypeForRenderer(Container container, EntryRenderer entry) {
		return super.getPreviewTypeForRenderer(container, entry);
	}
	
	public Class<? extends EntryRenderer> getRootEntryClass() {
		return super.getRootEntryClass();
	}
	
	public EntryRenderer process(Container media_source_container) throws Exception {
		EntryRenderer snapshoot = media_source_container.getByClass(Albumartwork.class);
		if (snapshoot == null) {
			snapshoot = media_source_container.getByClass(Snapshoot.class);
		}
		if (snapshoot == null) {
			Log2.log.debug("No snapshoot or artwork found from this container", media_source_container);
			return null;
		}
		
		List<String> filenames = snapshoot.getContentFileNames();
		if (filenames.isEmpty()) {
			Log2.log.debug("Snapshoot or artwork list from this container is empty", media_source_container);
			return null;
		}
		RenderedFile snapshoot_rfile = snapshoot.getRenderedFile(filenames.get(0), true);
		File physical_source = snapshoot_rfile.getRendered_file();
		
		/**
		 * Used for analyst previous rendered file and get an ImageAttributes for it.
		 */
		Container snapshoot_file_container = MetadataCenter.standaloneAnalysis(physical_source);
		
		ImageAttributes image_attributes = snapshoot_file_container.getByClass(ImageAttributes.class);
		if (image_attributes == null) {
			Log2.log.debug("No image_attributes for the snapshoot file container", snapshoot_file_container);
			return null;
		}
		// TODO get aspect ratio from media_source_container, and set to IM
		return subProcess(media_source_container, physical_source, image_attributes);
	}
}
