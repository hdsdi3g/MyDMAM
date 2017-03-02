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

public class ImageMagickThumbnailerCartridge extends ImageMagickThumbnailer {
	
	public ImageMagickThumbnailerCartridge() {
		preview_type = PreviewType.cartridge_thumbnail;
		
		if (TranscodeProfile.isConfigured()) {
			tprofile_opaque = TranscodeProfile.getTranscodeProfile("convert_cartridge");
			tprofile_alpha = TranscodeProfile.getTranscodeProfile("convert_cartridge_alpha");
		}
	}
	
	final protected String getFileReferenceName() {
		return "cartridge";
	}
	
	public static final String ES_TYPE = "imthumbnail_cartridge";
	
	final protected String getEntryRendererESType() {
		return ES_TYPE;
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
		/**
		 * cartridge, but img < cartridge => nope
		 */
		return null;
	}
	
	final protected boolean isPersonalizedSize(Compare compare, ImageAttributes image_attributes) {
		return false;
	}
}
