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

import hd3gtv.mydmam.metadata.PreviewType;
import hd3gtv.mydmam.transcode.TranscodeProfile;
import hd3gtv.mydmam.transcode.images.ImageAttributeGeometry.Compare;

public class ImageMagickThumbnailerIcon extends ImageMagickThumbnailer {
	
	public ImageMagickThumbnailerIcon() {
		preview_type = PreviewType.icon_thumbnail;
		
		if (TranscodeProfile.isConfigured()) {
			tprofile_opaque = TranscodeProfile.getTranscodeProfile(PROFILE_NAME);
			tprofile_alpha = TranscodeProfile.getTranscodeProfile(PROFILE_NAME + "_alpha");
		}
	}
	
	static final String PROFILE_NAME = "convert_icon";
	
	final protected String getFileReferenceName() {
		return "icon";
	}
	
	public static final String ES_TYPE = "imthumbnail_icon";
	
	final protected String getEntryRendererESType() {
		return ES_TYPE;
	}
	
	final protected TranscodeProfile getProfileIfItJudiciousToDoThumbnail(Compare compare, ImageAttributes image_attributes, TranscodeProfile primary_tprofile) {
		return primary_tprofile;
	}
	
	final protected boolean isPersonalizedSize(Compare compare, ImageAttributes image_attributes) {
		return false;
	}
}
