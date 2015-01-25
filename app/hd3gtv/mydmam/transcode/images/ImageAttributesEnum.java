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

/**
 * Enums imported from ImageMagick source file magick/option.c
 */
public final class ImageAttributesEnum {
	private ImageAttributesEnum() {
	}
	
	public enum Orientation {
		TopLeft, TopRight, BottomRight, BottomLeft, LeftTop, RightTop, RightBottom, LeftBottom
	}
	
	public enum Intent {
		Absolute, Perceptual, Relative, Saturation
	}
	
	public enum ImageClass {
		DirectClass, PseudoClass
	}
	
	public enum Compose {
		Atop, Blend, Blur, Bumpmap, ChangeMask, Clear, ColorBurn, ColorDodge, Colorize, CopyBlack, CopyBlue, CopyCyan, CopyGreen, Copy, CopyMagenta, CopyOpacity, CopyRed, CopyYellow, Darken, DarkenIntensity, DivideDst, DivideSrc, Dst, Difference, Displace, Dissolve, Distort, DstAtop, DstIn, DstOut, DstOver, Exclusion, HardLight, HardMix, Hue, In, Lighten, LightenIntensity, LinearBurn, LinearDodge, LinearLight, Luminize, Mathematics, MinusDst, MinusSrc, Modulate, ModulusAdd, ModulusSubtract, Multiply, None, Out, Overlay, Over, PegtopLight, PinLight, Plus, Replace, Saturate, Screen, SoftLight, Src, SrcAtop, SrcIn, SrcOut, SrcOver, VividLight, Xor, Add, Divide, Minus, Subtract, Threshold
	}
	
	public enum Colorspace {
		CIELab, CMY, CMYK, Gray, HCL, HCLp, HSB, HSI, HSL, HSV, HWB, Lab, LCH, LCHab, LCHuv, LMS, Log, Luv, OHTA, Rec601Luma, Rec601YCbCr, Rec709Luma, Rec709YCbCr, RGB, scRGB, sRGB, Transparent, XYZ, xyY, YCbCr, YDbDr, YCC, YIQ, YPbPr, YUV
	}
	
	public enum ImageType {
		Bilevel, ColorSeparation, ColorSeparationAlpha, ColorSeparationMatte, Grayscale, GrayscaleAlpha, GrayscaleMatte, Optimize, Palette, PaletteBilevelAlpha, PaletteBilevelMatte, PaletteAlpha, PaletteMatte, TrueColorAlpha, TrueColorMatte, TrueColor
	}
	
	public enum Dispose {
		Background, None, Previous
	}
	
	public enum Endian {
		LSB, MSB
	}
	
	public enum Interlace {
		Line, None, Plane, Partition, GIF, JPEG, PNG
	}
	
	public enum Intensity {
		Average, Brightness, Lightness, Mean, MS, Rec601Luma, Rec601Luminance, Rec709Luma, Rec709Luminance, RMS
	}
	
	public enum ResolutionUnits {
		PixelsPerInch, PixelsPerCentimeter
	}
	
	public enum Compress {
		B44, B44A, BZip, DXT1, DXT3, DXT5, Fax, Group4, JBIG1, JBIG2, JPEG, JPEG2000, Lossless, LosslessJPEG, LZMA, LZW, None, Piz, Pxr24, RLE, Zip, RunlengthEncoded, ZipS
	}
	
}
