/*
 * This file is part of VideoConst : Java constants for professional audios and videos values
 * 
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2008-2013
 * 
*/

package hd3gtv.tools;

import java.awt.Point;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public final class VideoConst {
	
	public final static Logger Log = Logger.getLogger(VideoConst.class);
	
	public enum Systemvideo {
		PAL, NTSC, CINEMA, OTHER, CINEMA_SCAN, HFR_PAL, HFR_NTSC, HFR_CINEMA_SCAN;
		
		public static Systemvideo getSystem(String systemname) {
			if (systemname.equalsIgnoreCase("PAL")) {
				return PAL;
			}
			if (systemname.equalsIgnoreCase("NTSC")) {
				return NTSC;
			}
			if (systemname.equalsIgnoreCase("CINEMA")) {
				return CINEMA;
			}
			if (systemname.equalsIgnoreCase("CINEMA_SCAN")) {
				return CINEMA_SCAN;
			}
			if (systemname.equalsIgnoreCase("HFR_PAL")) {
				return HFR_PAL;
			}
			if (systemname.equalsIgnoreCase("HFR_NTSC")) {
				return HFR_NTSC;
			}
			if (systemname.equalsIgnoreCase("HFR_CINEMA_SCAN")) {
				return HFR_CINEMA_SCAN;
			}
			return OTHER;
		}
		
		public String getSummary() {
			switch (this) {
			case PAL:
				return "PAL";
			case NTSC:
				return "NTSC";
			case CINEMA:
				return "Cinema 23.976 fps";
			case CINEMA_SCAN:
				return "Cinema 24 fps";
			case HFR_PAL:
				return "HFR 50 fps";
			case HFR_NTSC:
				return "HFR 60 fps";
			case HFR_CINEMA_SCAN:
				return "HFR 48 fps";
			default:
				break;
			}
			return "";
		}
	}
	
	public enum Framerate {
		FPS_25, FPS_30, FPS_2997, FPS_50, FPS_60, FPS_23976, FPS_5994, FPS_24, FPS_48, OTHER;
		
		public static Systemvideo getSystem(Framerate framerate) {
			if (framerate == OTHER) {
				return Systemvideo.OTHER;
			}
			if (framerate == FPS_25) {
				return Systemvideo.PAL;
			}
			if (framerate == FPS_30) {
				return Systemvideo.NTSC;
			}
			if (framerate == FPS_2997) {
				return Systemvideo.NTSC;
			}
			if (framerate == FPS_50) {
				return Systemvideo.HFR_PAL;
			}
			if (framerate == FPS_60) {
				return Systemvideo.HFR_NTSC;
			}
			if (framerate == FPS_48) {
				return Systemvideo.HFR_CINEMA_SCAN;
			}
			if (framerate == FPS_24) {
				return Systemvideo.CINEMA_SCAN;
			}
			if (framerate == FPS_23976) {
				return Systemvideo.CINEMA;
			}
			if (framerate == FPS_5994) {
				return Systemvideo.HFR_NTSC;
			}
			return Systemvideo.OTHER;
		}
		
		public static Framerate getFramerate(float fps) {
			if (fps == 25f) {
				return FPS_25;
			}
			if (fps == 30f) {
				return FPS_30;
			}
			if ((fps > 29.8f) && (fps < 30f)) {
				return FPS_2997;
			}
			if (fps == 50f) {
				return FPS_50;
			}
			if (fps == 60f) {
				return FPS_60;
			}
			if (fps == 24f) {
				return FPS_24;
			}
			if (fps == 48f) {
				return FPS_48;
			}
			if ((fps > 23.8f) && (fps < 24f)) {
				return FPS_23976;
			}
			if ((fps > 59.8f) && (fps < 60f)) {
				return FPS_5994;
			}
			return OTHER;
		}
		
		public float getNumericValue() {
			if (this == FPS_25) {
				return 25f;
			}
			if (this == FPS_30) {
				return 30f;
			}
			if (this == FPS_2997) {
				return 29.97f;
			}
			if (this == FPS_50) {
				return 50f;
			}
			if (this == FPS_60) {
				return 60f;
			}
			if (this == FPS_24) {
				return 24f;
			}
			if (this == FPS_48) {
				return 48f;
			}
			if (this == FPS_23976) {
				return 23.976f;
			}
			if (this == FPS_5994) {
				return 59.94f;
			}
			return 25;
		}
		
		/**
		 * @param ratio like "30000/1001"
		 */
		public static Framerate getFramerate(String ratio) {
			String[] part = ratio.trim().split("/");
			if (part.length != 2) {
				return null;
			}
			try {
				int left = Integer.valueOf(part[0]);
				int right = Integer.valueOf(part[1]);
				if (left == 0) {
					return null;
				}
				if (right == 0) {
					return null;
				}
				float real_fps = (float) left / (float) right;
				return getFramerate((float) Math.round(real_fps * 10f) / 10f);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		
		public String toString() {
			return String.valueOf(getNumericValue());
		}
	}
	
	public enum Aspectratio {
		AR_43, AR_169, AR_VGA;
		
		/**
		 * @return -1 if VGA
		 */
		public float toFloat() {
			if (this == Aspectratio.AR_43) {
				return 4f / 3f;
			}
			if (this == Aspectratio.AR_169) {
				return 16f / 9f;
			}
			return -1;
		}
		
		public String toString() {
			if (this == Aspectratio.AR_43) {
				return "4/3";
			}
			if (this == Aspectratio.AR_169) {
				return "16/9";
			}
			if (this == Aspectratio.AR_VGA) {
				return "1/1";
			}
			return "1/1";
		}
		
		/**
		 * @param value une valeur telle que "16/9", "4:3", "4-3", "16_9", "1.333", "1.77".
		 */
		public static Aspectratio parseAR(String value) {
			String splitchar = null;
			if (value.indexOf(":") > 0) {
				splitchar = ":";
			}
			if (value.indexOf("_") > 0) {
				splitchar = "_";
			}
			if (value.indexOf("-") > 0) {
				splitchar = "-";
			}
			if (value.indexOf("/") > 0) {
				splitchar = "/";
			}
			if (splitchar != null) {
				String[] raw = value.split(splitchar);
				if (raw.length != 2) {
					throw new NumberFormatException("Invalid aspect ratio : " + value);
				}
				raw[0] = raw[0].trim();
				raw[1] = raw[1].trim();
				if (raw[0].equals("4") & raw[1].equals("3")) {
					return AR_43;
				}
				if (raw[0].equals("16") & raw[1].equals("9")) {
					return AR_169;
				}
			}
			
			if (value.startsWith("1.33") | value.startsWith("1,33")) {
				return AR_43;
			}
			if (value.startsWith("1.77") | value.startsWith("1,77") | value.startsWith("1.78") | value.startsWith("1,78")) {
				return AR_169;
			}
			
			return AR_VGA;
		}
	}
	
	public enum ChromaFormat {
		CHROMA_420, CHROMA_422, CHROMA_411, CHROMA_RVB, CHROMA_4444, OTHER;
		
		public String toString() {
			if (this == CHROMA_420) {
				return "4:2:0";
			}
			if (this == CHROMA_422) {
				return "4:2:2";
			}
			if (this == CHROMA_411) {
				return "4:1:1";
			}
			if (this == CHROMA_RVB) {
				return "RVB";
			}
			if (this == CHROMA_4444) {
				return "4:4:4:4";
			}
			return "4:2:0";
		}
		
		public static ChromaFormat parseCF(String value) {
			if (value.equals("4:2:0")) {
				return CHROMA_420;
			}
			if (value.equals("4:2:2")) {
				return CHROMA_422;
			}
			if (value.equals("4:1:1")) {
				return CHROMA_411;
			}
			if (value.equals("RVB")) {
				return CHROMA_RVB;
			}
			if (value.equals("4:4:4:4")) {
				return CHROMA_4444;
			}
			return null;
		}
	}
	
	public enum Interlacing {
		Progressive,
		/**
		 * odd
		 */
		TopFieldFirst,
		/**
		 * even
		 */
		BottomFieldFirst, Unknow;
		
		public static Interlacing getFromString(String val) {
			if (val == null) {
				return Interlacing.Unknow;
			}
			if (val.equals("")) {
				return Interlacing.Unknow;
			}
			switch (val.toLowerCase()) {
			case "1":
				return Interlacing.TopFieldFirst;
			case "-1":
				return Interlacing.BottomFieldFirst;
			case "0":
				return Interlacing.Progressive;
			case "top":
				return Interlacing.TopFieldFirst;
			case "bottom":
				return Interlacing.BottomFieldFirst;
			case "progressive":
				return Interlacing.Progressive;
			case "tff":
				return Interlacing.TopFieldFirst;
			case "bff":
				return Interlacing.BottomFieldFirst;
			case "p":
				return Interlacing.Progressive;
			case "odd":
				return Interlacing.TopFieldFirst;
			case "even":
				return Interlacing.BottomFieldFirst;
			case "topfieldfirst":
				return Interlacing.TopFieldFirst;
			case "bottomfieldfirst":
				return Interlacing.BottomFieldFirst;
			default:
				return Interlacing.Unknow;
			}
		}
		
	}
	
	public static String audioChannelCounttoString(int channelcount) {
		if (channelcount == 1) {
			return "mono";
		}
		if (channelcount == 2) {
			return "stereo";
		}
		if (channelcount == 6) {
			return "5.1";
		}
		return String.valueOf(channelcount) + "ch";
	}
	
	public static int stringToAudioChannelCount(String channelcount) throws NumberFormatException {
		if (channelcount.equals("mono")) {
			return 1;
		}
		if (channelcount.equals("stereo")) {
			return 2;
		}
		if (channelcount.equals("5.1")) {
			return 6;
		}
		if (channelcount.startsWith("CH_")) {
			return Integer.parseInt(channelcount.substring(3));
		}
		return Integer.parseInt(channelcount);
	}
	
	public enum Resolution {
		SQCIF, QCIF, CIF, CIF4, CIF9, CIF16, VGA, SD_480, SD_480_VBI, SD_576, SD_576_VBI, HD_720, HD_HALF_1080, HD_1080, DCI_2K_FLAT, DCI_2K_SCOPE, DCI_2K_FULL, DCI_4K_NATIVE, DCI_4K_FLAT, DCI_4K_SCOPE, DCI_4K_FULL, DCI_4K_ACADEMY, UHD_4K, UHD_8K, OTHER;
		
		/**
		 * @param value like 123x456 or ":" or "/" or "-" or "_"
		 */
		public static Resolution parseResolution(String value) throws NumberFormatException {
			if (value == null) {
				return Resolution.OTHER;
			}
			if (value.equals("")) {
				return Resolution.OTHER;
			}
			
			int x = 0;
			int y = 0;
			if (value.indexOf("x") > -1) {
				x = Integer.parseInt(value.split("x")[0]);
				y = Integer.parseInt(value.split("x")[1]);
			} else if (value.indexOf(":") > -1) {
				x = Integer.parseInt(value.split(":")[0]);
				y = Integer.parseInt(value.split(":")[1]);
			} else if (value.indexOf("/") > -1) {
				x = Integer.parseInt(value.split("/")[0]);
				y = Integer.parseInt(value.split("/")[1]);
			} else if (value.indexOf("-") > -1) {
				x = Integer.parseInt(value.split("-")[0]);
				y = Integer.parseInt(value.split("-")[1]);
			} else if (value.indexOf("_") > -1) {
				x = Integer.parseInt(value.split("_")[0]);
				y = Integer.parseInt(value.split("_")[1]);
			}
			return getResolution(x, y);
		}
		
		public static Resolution getResolution(Point res) {
			return getResolution(res.x, res.y);
		}
		
		public static Resolution getResolution(int width, int height) {
			if (width == 128 && height == 96) {
				return SQCIF;
			}
			if (width == 176 && height == 144) {
				return QCIF;
			}
			if (width == 352 && height == 288) {
				return CIF;
			}
			if (width == 704 && height == 576) {
				return CIF4;
			}
			if (width == 1056 && height == 864) {
				return CIF9;
			}
			if (width == 1408 && height == 1152) {
				return CIF16;
			}
			if (width == 640 && height == 480) {
				return VGA;
			}
			if (width == 720 && height == 480) {
				return SD_480;
			}
			if (width == 720 && height == 512) {
				return SD_480_VBI;
			}
			if (width == 720 && height == 576) {
				return SD_576;
			}
			if (width == 720 && height == 608) {
				return SD_576_VBI;
			}
			if (width == 1280 && height == 720) {
				return HD_720;
			}
			if (width == 1440 && height == 1080) {
				return HD_HALF_1080;
			}
			if (width == 1920 && height == 1080) {
				return HD_1080;
			}
			if (width == 1998 && height == 1080) {
				return DCI_2K_FLAT;
			}
			if (width == 2048 && height == 858) {
				return DCI_2K_SCOPE;
			}
			if (width == 2048 && height == 1080) {
				return DCI_2K_FULL;
			}
			if (width == 4096 && height == 2160) {
				return DCI_4K_NATIVE;
			}
			if (width == 3996 && height == 2160) {
				return DCI_4K_FLAT;
			}
			if (width == 4096 && height == 1714) {
				return DCI_4K_SCOPE;
			}
			if (width == 4096 && height == 3112) {
				return DCI_4K_FULL;
			}
			if (width == 3656 && height == 2664) {
				return DCI_4K_ACADEMY;
			}
			if (width == 3840 && height == 2160) {
				return UHD_4K;
			}
			if (width == 7680 && height == 4320) {
				return UHD_8K;
			}
			return OTHER;
		}
		
		public Point getResolution() {
			switch (this) {
			case SQCIF:
				return new Point(128, 96);
			case QCIF:
				return new Point(176, 144);
			case CIF:
				return new Point(352, 288);
			case CIF4:
				return new Point(704, 576);
			case CIF9:
				return new Point(1056, 864);
			case CIF16:
				return new Point(1408, 1152);
			case VGA:
				return new Point(640, 480);
			case SD_480:
				return new Point(720, 480);
			case SD_480_VBI:
				return new Point(720, 512);
			case SD_576:
				return new Point(720, 576);
			case SD_576_VBI:
				return new Point(720, 608);
			case HD_720:
				return new Point(1280, 720);
			case HD_HALF_1080:
				return new Point(1440, 1080);
			case HD_1080:
				return new Point(1920, 1080);
			case DCI_2K_FLAT:
				return new Point(1998, 1080);
			case DCI_2K_SCOPE:
				return new Point(2048, 858);
			case DCI_2K_FULL:
				return new Point(2048, 1080);
			case DCI_4K_NATIVE:
				return new Point(4096, 2160);
			case DCI_4K_FLAT:
				return new Point(3996, 2160);
			case DCI_4K_SCOPE:
				return new Point(4096, 1714);
			case DCI_4K_FULL:
				return new Point(4096, 3112);
			case DCI_4K_ACADEMY:
				return new Point(3656, 2664);
			case UHD_4K:
				return new Point(3840, 2160);
			case UHD_8K:
				return new Point(7680, 4320);
			default:
				break;
			}
			return null;
		}
		
		public int getX_width() {
			return getResolution().x;
		}
		
		public int getY_height() {
			return getResolution().y;
		}
		
		/**
		 * @return like "SD", "HD", "2K Scope"...
		 */
		public String getBaseSystemResolution() {
			switch (this) {
			case SQCIF:
			case QCIF:
			case CIF:
			case CIF4:
			case CIF9:
			case CIF16:
				return "Legacy";
			case VGA:
			case SD_480:
			case SD_576:
				return "SD";
			case SD_480_VBI:
			case SD_576_VBI:
				return "SD VBI";
			case HD_720:
				return "HD 720";
			case HD_1080:
			case HD_HALF_1080:
				return "HD";
			case DCI_2K_FLAT:
				return "2K Flat";
			case DCI_2K_SCOPE:
				return "2K Scope";
			case DCI_2K_FULL:
				return "2K Full";
			case DCI_4K_NATIVE:
				return "4K Native";
			case DCI_4K_FLAT:
				return "4K Flat";
			case DCI_4K_SCOPE:
				return "4K Scope";
			case DCI_4K_FULL:
				return "4K Full";
			case DCI_4K_ACADEMY:
				return "4K Academy";
			case UHD_4K:
				return "4K UHDTV";
			case UHD_8K:
				return "8K UHDTV";
			default:
				break;
			}
			return "";
		}
		
		public boolean isVerticalBlankIntervalResolution() {
			if (this == SD_480_VBI) {
				return true;
			} else if (this == SD_576_VBI) {
				return true;
			} else {
				return false;
			}
		}
	}
	
	/**
	 * @return like "HD PAL" or "4K HFR 48"
	 */
	public static String getSystemSummary(int width, int height, Framerate framerate) {
		Resolution resolution = Resolution.getResolution(width, height);
		Systemvideo systemvideo = Framerate.getSystem(framerate);
		
		if ((resolution == Resolution.OTHER) || (systemvideo == Systemvideo.OTHER)) {
			StringBuffer sb = new StringBuffer();
			sb.append(width);
			sb.append("x");
			sb.append(height);
			sb.append(" ");
			sb.append(framerate);
			sb.append(" fps");
			return sb.toString();
		}
		StringBuffer sb = new StringBuffer();
		sb.append(resolution.getBaseSystemResolution());
		sb.append(" ");
		sb.append(systemvideo.getSummary());
		return sb.toString();
	}
	
	/**
	 * @return like "HD PAL" or "4K HFR 48"
	 */
	public static String getSystemSummary(int width, int height, float framerate) {
		return getSystemSummary(width, height, Framerate.getFramerate(framerate));
	}
	
	public enum AudioSampling {
		FQ_48000, FQ_44100, FQ_32000, FQ_88200, FQ_96000, FQ_192000, FQ_22050, FQ_176400, FQ_64000, FQ_16000, FQ_11025, FQ_8000, FQ_6000, OTHER;
		
		public int toInt() {
			if (this == FQ_48000) {
				return 48000;
			}
			if (this == FQ_44100) {
				return 44100;
			}
			if (this == FQ_32000) {
				return 32000;
			}
			if (this == FQ_88200) {
				return 88200;
			}
			if (this == FQ_96000) {
				return 96000;
			}
			if (this == FQ_192000) {
				return 192000;
			}
			if (this == FQ_22050) {
				return 22050;
			}
			if (this == FQ_176400) {
				return 176400;
			}
			if (this == FQ_64000) {
				return 64000;
			}
			if (this == FQ_16000) {
				return 16000;
			}
			if (this == FQ_11025) {
				return 11025;
			}
			if (this == FQ_8000) {
				return 8000;
			}
			if (this == FQ_6000) {
				return 6000;
			}
			return -1;
		}
		
		public static AudioSampling parseAS(String value) {
			String intvalue = value;
			if (value.toUpperCase().startsWith("FQ_")) {
				intvalue = value.substring(3);
			}
			int ivalue = -1;
			try {
				ivalue = Integer.parseInt(intvalue);
			} catch (NumberFormatException e) {
				Log.error("Can't parse audio frequency value, rawvalue: " + value, e);
				return null;
			}
			if (ivalue == 48000) {
				return FQ_48000;
			}
			if (ivalue == 44100) {
				return FQ_44100;
			}
			if (ivalue == 32000) {
				return FQ_32000;
			}
			if (ivalue == 88200) {
				return FQ_88200;
			}
			if (ivalue == 96000) {
				return FQ_96000;
			}
			if (ivalue == 192000) {
				return FQ_192000;
			}
			if (ivalue == 22050) {
				return FQ_22050;
			}
			if (ivalue == 176400) {
				return FQ_176400;
			}
			if (ivalue == 64000) {
				return FQ_64000;
			}
			if (ivalue == 16000) {
				return FQ_16000;
			}
			if (ivalue == 11025) {
				return FQ_11025;
			}
			if (ivalue == 8000) {
				return FQ_8000;
			}
			if (ivalue == 6000) {
				return FQ_6000;
			}
			return OTHER;
		}
	}
	
	public static class AudioRoutingStream {
		/**
		 * Absolute position from source.
		 */
		public ArrayList<Integer> source_channel;
		
		private AudioRoutingStream(String audio_map) {
			String[] map = audio_map.split("-");
			if (map.length == 0) {
				throw new IndexOutOfBoundsException("No configured map");
			}
			source_channel = new ArrayList<Integer>(map.length);
			for (int pos_map = 0; pos_map < map.length; pos_map++) {
				source_channel.add(Integer.valueOf(map[pos_map]));
			}
		}
		
		public String toString() {
			return source_channel.toString();
		}
	}
	
	/**
	 * @param value like "1-2 3-4 5 6 7 8-9-10-11"
	 * @return null, or had valid values.
	 */
	public static ArrayList<AudioRoutingStream> parseAudioRoutingStream(String value) throws NumberFormatException, IndexOutOfBoundsException {
		String[] audio_maps = value.trim().split(" ");
		if (audio_maps.length == 0) {
			return null;
		}
		ArrayList<AudioRoutingStream> result = new ArrayList<AudioRoutingStream>(audio_maps.length);
		
		for (int pos = 0; pos < audio_maps.length; pos++) {
			result.add(new AudioRoutingStream(audio_maps[pos]));
		}
		
		return result;
	}
	
}
