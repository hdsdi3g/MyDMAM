package hd3gtv.mydmam.transcode.images;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.metadata.GeneratorRenderer;
import hd3gtv.mydmam.metadata.PreviewType;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.EntryRenderer;
import hd3gtv.mydmam.transcode.TranscodeProfile;

import java.io.File;

public class ImageMagickThumbnailer implements GeneratorRenderer {
	
	// -limit memory 100MB -limit map 100MB -limit area 100MB -limit disk 30MB -limit file 50 -limit time 50
	
	/*
	convert logo-mtd-CMJN.psd[0] -profile srgb.icc \
	null: \( -brightness-contrast 30x60 -size 1290x1588 pattern:CHECKERBOARD \) -compose Dst_Over -layers composite \
	-strip -thumbnail 800x800 -density 72x72 -units PixelsPerInch -interlace plane -sampling-factor 4:1:1 -quality 90 low-411.jpg

	convert IMG_0627.jpg[0] -profile srgb.icc -strip -thumbnail 800x800 -density 72x72 -units PixelsPerInch -interlace plane -sampling-factor 4:1:1 -quality 90 low.jpg
	 * 
	 */
	
	private String convert_bin;
	private TranscodeProfile tprofile_full_display;
	private TranscodeProfile tprofile_cartridge;
	private TranscodeProfile tprofile_icon;
	
	// TODO limits : -limit memory 100MB -limit map 100MB -limit area 100MB -limit disk 30MB -limit file 50 -limit time 50
	
	public ImageMagickThumbnailer() {
		convert_bin = Configuration.global.getValue("transcoding", "convert_bin", "convert");
		if (TranscodeProfile.isConfigured()) {
			tprofile_full_display = TranscodeProfile.getTranscodeProfile("imagemagick", "convert_full_display");
			tprofile_cartridge = TranscodeProfile.getTranscodeProfile("imagemagick", "convert_cartridge");
			tprofile_icon = TranscodeProfile.getTranscodeProfile("imagemagick", "convert_icon");
		}
		
	}
	
	public boolean canProcessThis(String mimetype) {
		return ImageMagickAnalyser.mimetype_list.contains(mimetype);
	}
	
	public boolean isEnabled() {
		return (new File(convert_bin)).exists() & ((tprofile_full_display != null) | (tprofile_cartridge != null) | (tprofile_icon != null));
	}
	
	public String getLongName() {
		return "ImageMagick low-res images thumbnailer";
	}
	
	@Override
	public EntryRenderer process(Container container) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public PreviewType getPreviewTypeForRenderer(Container container, EntryRenderer entry) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Class<? extends EntryRenderer> getRootEntryClass() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
