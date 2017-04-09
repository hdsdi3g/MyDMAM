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
package hd3gtv.mydmam.transcode.kit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.ffmpeg.ffprobe.FfprobeType;
import org.ffmpeg.ffprobe.StreamType;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.transcode.mtdcontainer.BBCBmx;
import hd3gtv.mydmam.transcode.mtdcontainer.FFprobeJAXB;
import hd3gtv.tools.ExecBinaryPath;
import hd3gtv.tools.ExecprocessGettext;
import hd3gtv.tools.StoppableProcessing;

class PKitOpAtomTo1A_XMLBasedAtom {
	
	private File original_atom;
	private FfprobeType ffprobe;
	private BBCBmx bmx;
	private String temp_base_file_name;
	private File extracted_atom;
	private StoppableProcessing stoppable;
	
	PKitOpAtomTo1A_XMLBasedAtom(StoppableProcessing stoppable, File original_atom, String temp_base_file_name) {
		this.stoppable = stoppable;
		this.original_atom = original_atom;
		this.temp_base_file_name = temp_base_file_name;
		bmx = new BBCBmx();
	}
	
	private String makeTempFilePath(String suffix) {
		return original_atom.getParentFile().getAbsolutePath() + File.separator + temp_base_file_name + "_" + suffix;
	}
	
	void analystNcorrect() throws Exception {
		FFprobeJAXB ffprobe_jaxb = new FFprobeJAXB();
		ffprobe = ffprobe_jaxb.analystFile(original_atom);
		
		/**
		 * bmx2raw maybe don't works
		 */
		try {
			bmx.analystFile(original_atom);
		} catch (Exception e) {
			Loggers.Transcode.info("Can't analysing atom file with bmx " + original_atom.getName() + ", do a special treatment for it");
			/**
			 * Special treatment for "corrupted" files: 4 rewrap operations
			 */
			File corrected_file = execWriteavidmxf(original_atom, makeTempFilePath("writeavidmxf"));
			
			File corrected_file2 = new File(FilenameUtils.removeExtension(corrected_file.getPath()) + ".mxf");
			if (corrected_file.renameTo(corrected_file2) == false) {
				throw new FileNotFoundException("Can't rename file " + corrected_file + " to " + corrected_file2.getName());
			}
			
			corrected_file = execMxf2raw(corrected_file2, makeTempFilePath("mxf2raw1"));
			FileUtils.forceDelete(corrected_file2);
			
			corrected_file2 = new File(makeTempFilePath("bmxtranswrap") + ".mxf");
			execBmxtranswrap(corrected_file, corrected_file2);
			FileUtils.forceDelete(corrected_file);
			
			extracted_atom = execMxf2raw(corrected_file2, makeTempFilePath("mxf2raw2"));
			FileUtils.forceDelete(corrected_file2);
			return;
		}
		
		if (isVideoAtom()) {
			File corrected_file = execMxf2raw(original_atom, makeTempFilePath("mxf2raw"));
			
			extracted_atom = new File(FilenameUtils.removeExtension(corrected_file.getPath()) + ".mxf");
			if (corrected_file.renameTo(extracted_atom) == false) {
				throw new FileNotFoundException("Can't rename file " + corrected_file + " to " + extracted_atom.getName());
			}
		} else if (isAudioAtom()) {
			extracted_atom = new File(makeTempFilePath("ffmpeg") + ".wav");
			execFFmpegExtrAudioWav(original_atom, extracted_atom);
		} else {
			throw new Exception("WTF is this file ? " + original_atom.getName());
		}
		
		// bmx.analystFile(extracted_atom);
		// ffprobe = ffprobe_jaxb.analystFile(extracted_atom);
	}
	
	private void execBmxtranswrap(File source_file, File dest_file) throws IOException {
		ArrayList<String> param = new ArrayList<String>();
		param.add("-t");
		param.add("op1a");
		// param.add("-y");
		// param.add("00:00:00:00");
		// param.add("--clip");
		// param.add("AAA");
		param.add("-o");
		param.add(dest_file.getAbsolutePath());
		param.add(source_file.getAbsolutePath());
		
		ExecprocessGettext process = new ExecprocessGettext(ExecBinaryPath.get("bmxtranswrap"), param);
		process.setEndlinewidthnewline(true);
		
		Loggers.Transcode.info("Rewrap MXF essence: " + process.getRunprocess().getCommandline());
		process.start(stoppable);
	}
	
	private File execWriteavidmxf(File source_file, String output_file_base_name) throws IOException {
		ArrayList<String> param = new ArrayList<String>();
		param.add("--prefix");
		param.add(output_file_base_name);
		param.add("--unc1080i");
		param.add(source_file.getAbsolutePath());
		
		ExecprocessGettext process = new ExecprocessGettext(ExecBinaryPath.get("writeavidmxf"), param);
		process.setEndlinewidthnewline(true);
		
		Loggers.Transcode.info("Extract MXF essence: " + process.getRunprocess().getCommandline());
		process.start(stoppable);
		return foundFile(output_file_base_name);
	}
	
	private File execMxf2raw(File source_file, String output_file_base_name) throws IOException {
		ArrayList<String> param = new ArrayList<String>();
		param.add("--ess-out");
		param.add(output_file_base_name);
		param.add(source_file.getAbsolutePath());
		
		ExecprocessGettext process = new ExecprocessGettext(ExecBinaryPath.get("mxf2raw"), param);
		process.setEndlinewidthnewline(true);
		
		Loggers.Transcode.info("Extract MXF essence: " + process.getRunprocess().getCommandline());
		process.start(stoppable);
		return foundFile(output_file_base_name);
	}
	
	private void execFFmpegExtrAudioWav(File source_file, File dest_file) throws IOException {
		ArrayList<String> param = new ArrayList<String>();
		param.add("-y");
		param.add("-i");
		param.add(source_file.getAbsolutePath());
		
		param.add("-codec:a");
		param.add("pcm_s16le");
		
		param.add("-map");
		param.add("0:" + getMXFStreamMap());
		
		param.add("-f");
		param.add("wav");
		param.add(dest_file.getAbsolutePath());
		
		ExecprocessGettext process = new ExecprocessGettext(ExecBinaryPath.get("ffmpeg"), param);
		process.setEndlinewidthnewline(true);
		
		Loggers.Transcode.info("Extract audio with ffmpeg: " + process.getRunprocess().getCommandline());
		process.start(stoppable);
	}
	
	private File foundFile(String basename) throws FileNotFoundException {
		String real_base_name = FilenameUtils.getName(basename);
		File[] founded = original_atom.getParentFile().listFiles((dir, name) -> {
			return name.startsWith(real_base_name);
		});
		
		if (founded.length != 1) {
			throw new FileNotFoundException(original_atom.getParent() + File.separator + real_base_name + " *");
		}
		return founded[0];
	}
	
	File getValidAtomFile() {
		if (extracted_atom != null) {
			return extracted_atom;
		}
		return original_atom;
	}
	
	private StreamType getValidStream() {
		if (ffprobe == null) {
			throw new NullPointerException("ffprobe analysing is null for " + this.original_atom.getName());
		}
		
		Optional<StreamType> result = ffprobe.getStreams().getStream().stream().filter(stream_type -> {
			if (stream_type.getCodecType().equalsIgnoreCase("video") | stream_type.getCodecType().equalsIgnoreCase("audio")) {
				return true;
			}
			return false;
		}).findFirst();
		
		if (result.isPresent() == false) {
			throw new IndexOutOfBoundsException("No valid stream founded for " + this.original_atom.getName());
		}
		
		return result.get();
	}
	
	int getMXFStreamMap() {
		return getValidStream().getIndex();
	}
	
	String getStartTC() {
		if (bmx.isLoaded() == false) {
			return null;
		}
		
		try {
			return bmx.getMXFStartTimecode();
		} catch (Exception e) {
			Loggers.Transcode.debug("Can't get MXF TC from BMX " + original_atom.getName(), e);
		}
		
		try {
			return getValidStream().getTag().stream().filter(tag -> {
				return tag.getKey().equals("timecode");
			}).findFirst().get().getValue();
		} catch (Exception e) {
			Loggers.Transcode.debug("Can't get MXF TC from ffprobe " + original_atom.getName(), e);
		}
		
		return null;
	}
	
	String getName() {
		if (bmx.isLoaded() == false) {
			return null;
		}
		
		try {
			return bmx.getMXFName();
		} catch (Exception e) {
			Loggers.Transcode.debug("Can't get MXF Name from BMX " + original_atom.getName(), e);
		}
		
		try {
			return getValidStream().getTag().stream().filter(tag -> {
				return tag.getKey().equals("file_package_name");
			}).findFirst().orElseGet(() -> {
				return getValidStream().getTag().stream().filter(tag -> {
					return tag.getKey().equals("reel_name");
				}).findFirst().get();
			}).getValue();
		} catch (Exception e) {
			Loggers.Transcode.debug("Can't get MXF Name from ffprobe " + original_atom.getName(), e);
		}
		
		return null;
	}
	
	boolean isVideoAtom() {
		return getValidStream().getCodecType().equalsIgnoreCase("video");
	}
	
	boolean isAudioAtom() {
		return getValidStream().getCodecType().equalsIgnoreCase("audio");
	}
	
	List<File> getFilesToClean() {
		ArrayList<File> result = new ArrayList<>();
		if (extracted_atom != null) {
			if (extracted_atom.exists()) {
				result.add(extracted_atom);
			}
		}
		result.add(original_atom);
		return result;
	}
	
	public String toString() {
		return original_atom.getPath();
	}
}
