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

import hd3gtv.mydmam.metadata.ContainerEntryResult;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.transcode.mtdgenerator.FFprobeAnalyser;
import hd3gtv.tools.StoppableProcessing;

/**
 * Create valid and usable thumbnails from raw ffmpeg snapshots and albums artworks.
 */
public class ImageMagickThumbnailerIconFFmpeg extends ImageMagickThumbnailerIcon {
	
	public ImageMagickThumbnailerIconFFmpeg() {
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
	
	public ContainerEntryResult processFull(Container media_source_container, StoppableProcessing stoppable) throws Exception {
		return ImageMagickThumbnailerCartridgeFFmpeg.preProcessFull(media_source_container, stoppable, this);
	}
	
}
