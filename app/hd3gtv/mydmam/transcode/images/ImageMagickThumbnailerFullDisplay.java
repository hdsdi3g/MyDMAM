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
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.transcode.TranscodeProfile;
import hd3gtv.mydmam.transcode.images.ImageAttributeGeometry.Compare;

public class ImageMagickThumbnailerFullDisplay extends ImageMagickThumbnailer {
	
	public ImageMagickThumbnailerFullDisplay() {
		preview_type = PreviewType.full_size_thumbnail;
		
		if (TranscodeProfile.isConfigured()) {
			tprofile_opaque = TranscodeProfile.getTranscodeProfile("convert_full_display");
			tprofile_alpha = TranscodeProfile.getTranscodeProfile("convert_full_display_alpha");
		}
	}
	
	final protected String getFileReferenceName() {
		return "fulldisplay";
	}
	
	public static final String ES_TYPE = "imthumbnail_full";
	
	final protected String getEntryRendererESType() {
		return ES_TYPE;
	}
	
	public boolean isTheExtractionWasActuallyDoes(Container container) {
		return container.containAnyMatchContainerEntryType(ES_TYPE);
	}
	
	/*
	 * Choose list :
				img < icon		img < cartridge	img < full		img > full
	full		nope			personalized	personalized	full
	cartridge	nope			nope			cartridge		cartridge
	icon		icon			icon			icon			icon 
	 * */
	
	final protected TranscodeProfile getProfileIfItJudiciousToDoThumbnail(Compare compare, ImageAttributes image_attributes, TranscodeProfile primary_tprofile) {
		if (compare != Compare.IS_SMALLER_THAN_PROFILE) {
			return primary_tprofile;
		}
		
		if (image_attributes.geometry.compare(TranscodeProfile.getTranscodeProfile(ImageMagickThumbnailerIcon.PROFILE_NAME)) == Compare.IS_SMALLER_THAN_PROFILE) {
			/**
			 * full, but img < icon => nope
			 */
			return null;
		}
		if (image_attributes.alpha == null) {
			return TranscodeProfile.getTranscodeProfile("convert_personalizedsize");
		}
		
		return TranscodeProfile.getTranscodeProfile("convert_personalizedsize_alpha");
	}
	
	final protected boolean isPersonalizedSize(Compare compare, ImageAttributes image_attributes) {
		return compare == Compare.IS_SMALLER_THAN_PROFILE;
	}
	
}
