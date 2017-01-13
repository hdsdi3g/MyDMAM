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
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
*/
package hd3gtv.mydmam.transcode.mtdcontainer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.metadata.container.ContainerEntry;
import hd3gtv.mydmam.metadata.container.ContainerOperations;
import hd3gtv.mydmam.metadata.container.EntryAnalyser;
import hd3gtv.mydmam.metadata.container.SelfSerializing;
import hd3gtv.tools.VideoConst;
import hd3gtv.tools.VideoConst.Framerate;
import uk.co.bbc.rd.bmx.Bmx;
import uk.co.bbc.rd.bmx.ClipType;
import uk.co.bbc.rd.bmx.FileType;
import uk.co.bbc.rd.bmx.PictureDescriptorType;
import uk.co.bbc.rd.bmx.SoundDescriptorType;
import uk.co.bbc.rd.bmx.TrackType;

public class BBCBmx extends EntryAnalyser {
	
	protected FileType file;
	protected ClipType clip;
	
	protected void extendedInternalSerializer(JsonObject current_element, EntryAnalyser _item, Gson gson) {
	}
	
	public String getES_Type() {
		return "bbc_bmx";
	}
	
	protected List<Class<? extends SelfSerializing>> getSerializationDependencies() {
		return null;
	}
	
	protected ContainerEntry internalDeserialize(JsonObject source, Gson gson) {
		JsonElement j_file = source.get("file");
		JsonElement j_clip = source.get("clip");
		
		BBCBmx ea = new BBCBmx();
		ea.file = ContainerOperations.getGsonSimple().fromJson(j_file, FileType.class);
		ea.clip = ContainerOperations.getGsonSimple().fromJson(j_clip, ClipType.class);
		return ea;
	}
	
	public void setBmx(Bmx bmx) {
		this.clip = bmx.getClip();
		this.file = bmx.getFiles().getFile().get(0);
	}
	
	public String processSummary() {
		if (clip == null | file == null) {
			throw new NullPointerException("No clip/file object");
		}
		StringBuilder sb = new StringBuilder();
		
		sb.append("MXF ");
		sb.append(getMXFOPLabel());
		sb.append(" ");
		
		String name = getMXFName();
		if (name != null) {
			sb.append("\"");
			sb.append(name);
			sb.append("\" ");
		}
		
		String start_tc = getMXFStartTimecode();
		if (start_tc != null) {
			sb.append("Start: ");
			sb.append(start_tc);
			sb.append(" ");
		}
		
		String duration = getMXFDuration();
		if (duration != null) {
			sb.append("Duration: ");
			sb.append(duration);
			sb.append(" ");
		}
		
		String editrate = getMXFEditRate();
		if (editrate != null) {
			sb.append(editrate);
			sb.append(" ");
		}
		
		List<TrackType> tracks = getTracks();
		TrackType track;
		for (int pos = 0; pos < tracks.size(); pos++) {
			track = tracks.get(pos);
			try {
				sb.append("[CH-");
				sb.append(pos + 1);
				sb.append(" ");
				sb.append(toString(track).trim());
				sb.append("] ");
				// track.getDataDescriptor().getAncDescriptor().getManifest().getElement().get(0).
				// track.getDataDescriptor().getVbiDescriptor().getManifest().getElement().get(0).getWrappingType().getValue()
			} catch (NullPointerException e) {
				Loggers.Transcode_Metadata.warn("Can't extract datas from BMX (track #" + pos + ")", e);
			}
		}
		
		sb.append("/ Date: ");
		sb.append(MyDMAM.DATE_TIME_FORMAT.format(new Date(getMXFModifiedDate())));
		sb.append(" ");
		
		String company = getMXFCompany();
		String product = getMXFProduct();
		if (company != null || product != null) {
			sb.append("/ ");
		}
		
		if (company != null) {
			sb.append(company);
			sb.append(" ");
		}
		
		if (product != null) {
			sb.append(product);
		}
		
		return sb.toString();
	}
	
	public static String toString(TrackType track) {
		StringBuilder sb = new StringBuilder();
		
		/**
		 * Picture / Sound / Data
		 */
		sb.append(track.getEssenceKind().value());
		sb.append(" ");
		
		/**
		 * MPEG_2_Long_GOP_422P_HL_1080i
		 * WAVE_PCM
		 * ANC_Data
		 */
		sb.append(track.getEssenceType().value());
		sb.append(" ");
		
		/**
		 * Video
		 */
		PictureDescriptorType p_type = track.getPictureDescriptor();
		if (p_type != null) {
			sb.append(p_type.getAspectRatio());
			sb.append(" ");
			sb.append(VideoConst.getSystemSummary((int) p_type.getDisplayWidth(), (int) p_type.getDisplayHeight(), Framerate.getFramerate(track.getEditRate())));
			sb.append(" ");
			
			sb.append("(");
			try {
				String cdci = p_type.getCdciDescriptor().getColorSiting().getValue();
				if (cdci.equalsIgnoreCase("none") == false) {
					sb.append(cdci);
					sb.append("/");
				}
			} catch (NullPointerException e) {
			}
			try {
				String si_st = p_type.getSignalStandard().getValue();
				if (si_st.equalsIgnoreCase("none") == false) {
					sb.append(si_st);
					sb.append("/");
				}
			} catch (NullPointerException e) {
			}
			try {
				sb.append(p_type.getFrameLayout().getValue());
			} catch (NullPointerException e) {
			}
			sb.append(")");
		}
		
		/**
		 * Audio
		 */
		SoundDescriptorType s_type = track.getSoundDescriptor();
		if (s_type != null) {
			sb.append(s_type.getSamplingRate().split("/")[0]);
			sb.append(" Hz ");
			sb.append(s_type.getBitsPerSample());
			sb.append("b ");
			if (s_type.getChannelCount() > 1) {
				sb.append(s_type.getChannelCount());
				sb.append(" channels.");
			}
		}
		
		return sb.toString();
	}
	
	/**
	 * @return never null.
	 */
	public List<TrackType> getTracks() {
		try {
			/**
			 * Test if the first trask exists and its valid.
			 */
			clip.getTracks().getTrack().get(0).getIndex();
			return clip.getTracks().getTrack();
		} catch (NullPointerException e) {
			return new ArrayList<>(1);
		}
	}
	
	/**
	 * @return like 00:56:50:00 or 00:00:00:3840 or null
	 */
	public String getMXFStartTimecode() {
		try {
			return clip.getStartTimecodes().getMaterial().getValue();
		} catch (NullPointerException e) {
			try {
				return clip.getStartTimecodes().getPhysicalSource().getValue() + " (from physical source)";
			} catch (NullPointerException e1) {
				return null;
			}
		}
	}
	
	/**
	 * @return like 00:56:50:00 or 00:00:00:3840 or null
	 */
	public String getMXFDuration() {
		try {
			return clip.getDuration().getValue();
		} catch (NullPointerException e) {
			return null;
		}
	}
	
	/**
	 * @return like 48000/1 or 25/1 or null
	 */
	public String getMXFEditRate() {
		try {
			return clip.getEditRate();
		} catch (NullPointerException e) {
			return null;
		}
	}
	
	/**
	 * @return like "Edit,Audio_Dissolve,2"
	 */
	public String getMXFName() {
		try {
			return file.getMaterialPackages().getMaterialPackage().get(0).getName();
		} catch (NullPointerException e) {
			return null;
		}
	}
	
	public long getMXFModifiedDate() {
		try {
			return file.getIdentifications().getIdentification().get(0).getModifiedDate().toGregorianCalendar().getTimeInMillis();
		} catch (NullPointerException e) {
			return -1;
		}
	}
	
	/**
	 * @return like "Avid Media Composer 8.5.3"
	 */
	public String getMXFProduct() {
		try {
			return file.getIdentifications().getIdentification().get(0).getProductName();
		} catch (NullPointerException e) {
			return null;
		}
	}
	
	/**
	 * @return like "Avid Technology, Inc."
	 */
	public String getMXFCompany() {
		try {
			return file.getIdentifications().getIdentification().get(0).getCompanyName();
		} catch (NullPointerException e) {
			return null;
		}
	}
	
	/**
	 * @return like OP1A
	 */
	public String getMXFOPLabel() {
		try {
			return file.getOpLabel().getValue();
		} catch (NullPointerException e) {
			return null;
		}
	}
	
	public ClipType getClip() {
		return clip;
	}
	
	public FileType getFile() {
		return file;
	}
	
}
