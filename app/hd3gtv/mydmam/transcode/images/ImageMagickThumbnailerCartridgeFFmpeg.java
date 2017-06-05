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
package hd3gtv.mydmam.transcode.images;

import java.io.File;
import java.util.List;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.metadata.ContainerEntryResult;
import hd3gtv.mydmam.metadata.MetadataIndexingLimit;
import hd3gtv.mydmam.metadata.MetadataIndexingOperation;
import hd3gtv.mydmam.metadata.RenderedFile;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.EntryRenderer;
import hd3gtv.mydmam.transcode.mtdgenerator.FFmpegAlbumartwork;
import hd3gtv.mydmam.transcode.mtdgenerator.FFmpegSnapshot;
import hd3gtv.mydmam.transcode.mtdgenerator.FFprobeAnalyser;
import hd3gtv.tools.StoppableProcessing;

/**
 * Create valid and usable thumbnails from raw ffmpeg snapshots and albums artworks.
 */
public class ImageMagickThumbnailerCartridgeFFmpeg extends ImageMagickThumbnailerCartridge {
	
	public ImageMagickThumbnailerCartridgeFFmpeg() {
		super();
	}
	
	public boolean canProcessThisMimeType(String mimetype) {
		if (FFprobeAnalyser.canProcessThisVideoOnly(mimetype)) return true;
		if (FFprobeAnalyser.canProcessThisAudioOnly(mimetype)) return true;
		return false;
	}
	
	public String getLongName() {
		return super.getLongName() + " from ffmpeg snapshots and artworks";
	}
	
	static ContainerEntryResult preProcessFull(Container media_source_container, StoppableProcessing stoppable, ImageMagickThumbnailer this_thumbnailer) throws Exception {
		EntryRenderer snapshot = media_source_container.getByType(FFmpegAlbumartwork.ES_TYPE, EntryRenderer.class);
		if (snapshot == null) {
			snapshot = media_source_container.getByType(FFmpegSnapshot.ES_TYPE, EntryRenderer.class);
		}
		if (snapshot == null) {
			Loggers.Transcode.debug("No snapshot or artwork found from this container: " + media_source_container);
			return null;
		}
		
		List<String> filenames = snapshot.getContentFileNames();
		if (filenames.isEmpty()) {
			Loggers.Transcode.debug("snapshot or artwork list from this container is empty: " + media_source_container);
			return null;
		}
		
		RenderedFile snapshot_rfile = snapshot.getRenderedFile(filenames.get(0), true);
		File physical_source = snapshot_rfile.getRendered_file();
		
		if (Loggers.Metadata.isDebugEnabled()) {
			Loggers.Metadata.debug("Snapshoted file: " + physical_source);
		}
		
		/**
		 * Used for analyst previous rendered file and get an ImageAttributes for it.
		 */
		Container snapshot_file_container = new MetadataIndexingOperation(physical_source).setStoppable(stoppable).setLimit(MetadataIndexingLimit.FAST).doIndexing();
		
		ImageAttributes image_attributes = snapshot_file_container.getByType(this_thumbnailer.getEntryRendererESType(), ImageAttributes.class);
		if (image_attributes == null) {
			Loggers.Transcode.debug("No image_attributes for the snapshot file container: " + snapshot_file_container);
			return null;
		}
		return this_thumbnailer.subProcess(media_source_container, stoppable, physical_source, image_attributes);
	}
	
	public ContainerEntryResult processFull(Container media_source_container, StoppableProcessing stoppable) throws Exception {
		return preProcessFull(media_source_container, stoppable, this);
	}
}
