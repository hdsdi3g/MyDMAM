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

import org.apache.commons.io.FilenameUtils;
import org.ffmpeg.ffprobe.FfprobeType;
import org.ffmpeg.ffprobe.StreamType;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.transcode.mtdcontainer.BBCBmx;
import hd3gtv.mydmam.transcode.mtdcontainer.FFprobeJAXB;
import hd3gtv.tools.ExecBinaryPath;
import hd3gtv.tools.ExecprocessGettext;

class PKitOpAtomTo1A_XMLBasedAtom {
	
	private File original_atom;
	private FfprobeType ffprobe;
	private BBCBmx bmx;
	private String temp_base_file_name;
	private File extracted_atom;
	
	PKitOpAtomTo1A_XMLBasedAtom(File original_atom, String temp_base_file_name) {
		this.original_atom = original_atom;
		this.temp_base_file_name = temp_base_file_name;
		bmx = new BBCBmx();
	}
	
	private String makeTempFilePath(String suffix) {
		return original_atom.getParentFile().getAbsolutePath() + File.separator + temp_base_file_name + "_" + suffix;
	}
	
	void analystNcorrect() throws Exception {
		FFprobeJAXB ffprobe_jaxb = new FFprobeJAXB();
		
		/**
		 * ffprobe and bmx maybe don't works
		 */
		try {
			bmx.analystFile(original_atom);
		} catch (Exception e) {
			Loggers.Transcode.debug("Can't analysing atom file with bmx " + original_atom.getPath(), e);
		}
		
		try {
			ffprobe = ffprobe_jaxb.analystFile(original_atom);
		} catch (Exception e) {
			Loggers.Transcode.debug("Can't analysing atom file with ffprobe " + original_atom.getPath(), e);
		}
		
		// TODO refactor: use ffmpeg for audio, mxf2raw for video
		
		/**
		 * This file is a perfect bug less MXF file
		 */
		if (bmx.isLoaded() && ffprobe != null) {
			return;
		}
		
		if (bmx.isLoaded() == false ^ ffprobe == null) {
			/**
			 * Simple correction
			 */
			/*String temp_file_name_mxf2raw = makeTempFilePath("mxf2raw");
			execMxf2raw(original_atom, temp_file_name_mxf2raw);
			File corrected_file = foundFile(temp_file_name_mxf2raw);
			
			extracted_atom = new File(FilenameUtils.removeExtension(corrected_file.getPath()) + ".mxf");
			if (corrected_file.renameTo(extracted_atom) == false) {
				throw new FileNotFoundException("Can't rename file " + corrected_file + " to " + extracted_atom.getName());
			}*/
		} else {
			/**
			 * Big corrections for "corrupted" files
			 */
			/*String temp_file_name_writeavidmxf = makeTempFilePath("writeavidmxf");
			execWriteavidmxf(original_atom, temp_file_name_writeavidmxf);
			File corrected_file = foundFile(temp_file_name_writeavidmxf);
			
			extracted_atom = new File(FilenameUtils.removeExtension(corrected_file.getPath()) + ".mxf");
			if (corrected_file.renameTo(extracted_atom) == false) {
				throw new FileNotFoundException("Can't rename file " + corrected_file + " to " + extracted_atom.getName());
			}
			
			String temp_file_name_mxf2raw = makeTempFilePath("mxf2raw");
			execMxf2raw(extracted_atom, temp_file_name_mxf2raw);
			corrected_file = foundFile(temp_file_name_mxf2raw);
			FileUtils.forceDelete(extracted_atom);
			
			extracted_atom = new File(makeTempFilePath("bmxtranswrap") + ".mxf");
			execBmxtranswrap(corrected_file, extracted_atom);
			FileUtils.forceDelete(corrected_file);*/
		}
		
		/**
		 * No error are allowed from here
		 */
		// TODO if actual is incorrect, do it
		bmx.analystFile(extracted_atom);
		ffprobe = ffprobe_jaxb.analystFile(extracted_atom);
	}
	
	/*private void execBmxtranswrap(File source_file, File dest_file) throws IOException {
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
		process.start();
	}*/
	
	/*private void execWriteavidmxf(File source_file, String output_file_base_name) throws IOException {
		ArrayList<String> param = new ArrayList<String>();
		param.add("--prefix");
		param.add(output_file_base_name);
		param.add("--unc1080i");
		param.add(source_file.getAbsolutePath());
		
		ExecprocessGettext process = new ExecprocessGettext(ExecBinaryPath.get("writeavidmxf"), param);
		process.setEndlinewidthnewline(true);
		
		Loggers.Transcode.info("Extract MXF essence: " + process.getRunprocess().getCommandline());
		process.start();
	}*/
	
	private void execMxf2raw(File source_file, String output_file_base_name) throws IOException {
		ArrayList<String> param = new ArrayList<String>();
		param.add("--ess-out");
		param.add(output_file_base_name);
		param.add(source_file.getAbsolutePath());
		
		ExecprocessGettext process = new ExecprocessGettext(ExecBinaryPath.get("mxf2raw"), param);
		process.setEndlinewidthnewline(true);
		
		Loggers.Transcode.info("Extract MXF essence: " + process.getRunprocess().getCommandline());
		process.start();
	}
	
	// TODO ffmpeg audio
	/*ArrayList<String> params_ffmpeg = new ArrayList<>();
	params_ffmpeg.add("-y");
	all_atoms.stream().forEach(atom -> {
		params_ffmpeg.add("-i");
		params_ffmpeg.add(atom.getValidAtomFile().getAbsolutePath());
	});
	
	if (all_atoms.stream().anyMatch(atom -> {
		return atom.isVideoAtom();
	})) {
		params_ffmpeg.add("-codec:v");
		params_ffmpeg.add("copy");
	}
	
	if (all_atoms.stream().anyMatch(atom -> {
		return atom.isAudioAtom();
	})) {
		params_ffmpeg.add("-codec:a");
		params_ffmpeg.add("copy");
	}
	
	for (int pos = 0; pos < all_atoms.size(); pos++) {
		params_ffmpeg.add("-map");
		params_ffmpeg.add(String.valueOf(pos) + ":" + String.valueOf(pos));
	}
	params_ffmpeg.add("-f");
	params_ffmpeg.add("mxf");
	params_ffmpeg.add("-");
	pipe.add(ExecBinaryPath.get("ffmpeg"), params_ffmpeg);
	*/
	
	private File foundFile(String basename) throws FileNotFoundException {
		String real_base_name = FilenameUtils.getBaseName(basename);
		File[] founded = original_atom.getParentFile().listFiles((dir, name) -> {
			return name.startsWith(real_base_name);
		});
		
		if (founded.length != 1) {
			throw new FileNotFoundException(original_atom.getParent() + File.separator + basename + "*");
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
			if (stream_type.getCodecType().equalsIgnoreCase("data")) {
				return false;
			}
			if (stream_type.getCodecName() == null) {
				return false;
			}
			if (stream_type.getCodecName().equals("")) {
				return false;
			}
			if (stream_type.getBitRate() == null) {
				return false;
			}
			if (stream_type.getBitRate() == 0) {
				return false;
			}
			return true;
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
		return getValidStream().getCodecType().equalsIgnoreCase("video");
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
