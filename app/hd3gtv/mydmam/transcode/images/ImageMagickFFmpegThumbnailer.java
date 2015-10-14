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

import hd3gtv.log2.Log2;
import hd3gtv.mydmam.metadata.MetadataIndexingOperation;
import hd3gtv.mydmam.metadata.MetadataIndexingOperation.MetadataIndexingLimit;
import hd3gtv.mydmam.metadata.PreviewType;
import hd3gtv.mydmam.metadata.RenderedFile;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.EntryRenderer;
import hd3gtv.mydmam.transcode.mtdgenerator.FFmpegAlbumartwork.Albumartwork;
import hd3gtv.mydmam.transcode.mtdgenerator.FFmpegSnapshot.Snapshot;
import hd3gtv.mydmam.transcode.mtdgenerator.FFprobeAnalyser;

/**
 * Create valid and usable thumbnails from raw ffmpeg snapshots and albums artworks.
 */
public class ImageMagickFFmpegThumbnailer extends ImageMagickThumbnailer {
	
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
		return "ImageMagick low-res thumbnailer from ffmpeg snapshots and artworks";
	}
	
	public PreviewType getPreviewTypeForRenderer(Container container, EntryRenderer entry) {
		return super.getPreviewTypeForRenderer(container, entry);
	}
	
	public Class<? extends EntryRenderer> getRootEntryClass() {
		return super.getRootEntryClass();
	}
	
	public EntryRenderer process(Container media_source_container) throws Exception {
		EntryRenderer snapshot = media_source_container.getByClass(Albumartwork.class);
		if (snapshot == null) {
			snapshot = media_source_container.getByClass(Snapshot.class);
		}
		if (snapshot == null) {
			Log2.log.debug("No snapshot or artwork found from this container: " + media_source_container);
			return null;
		}
		
		List<String> filenames = snapshot.getContentFileNames();
		if (filenames.isEmpty()) {
			Log2.log.debug("snapshot or artwork list from this container is empty: " + media_source_container);
			return null;
		}
		RenderedFile snapshot_rfile = snapshot.getRenderedFile(filenames.get(0), true);
		File physical_source = snapshot_rfile.getRendered_file();
		
		/**
		 * Used for analyst previous rendered file and get an ImageAttributes for it.
		 */
		Container snapshot_file_container = new MetadataIndexingOperation(physical_source).setLimit(MetadataIndexingLimit.ANALYST).doIndexing();
		
		ImageAttributes image_attributes = snapshot_file_container.getByClass(ImageAttributes.class);
		if (image_attributes == null) {
			Log2.log.debug("No image_attributes for the snapshot file container: " + snapshot_file_container);
			return null;
		}
		return subProcess(media_source_container, physical_source, image_attributes);
	}
}
