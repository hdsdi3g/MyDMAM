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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.ffmpeg.ffprobe.FfprobeType;

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
	private File extracted_path; // TODO rename
	
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
			String temp_file_name_mxf2raw = makeTempFilePath("mxf2raw");
			execMxf2raw(original_atom, temp_file_name_mxf2raw);
			File corrected_file = foundFile(temp_file_name_mxf2raw);
			
			extracted_path = new File(FilenameUtils.removeExtension(corrected_file.getPath()) + ".mxf");
			if (corrected_file.renameTo(extracted_path) == false) {
				throw new FileNotFoundException("Can't rename file " + corrected_file + " to " + extracted_path.getName());
			}
		} else {
			/**
			 * Big corrections for "corrupted" files
			 */
			String temp_file_name_writeavidmxf = makeTempFilePath("writeavidmxf");
			execWriteavidmxf(original_atom, temp_file_name_writeavidmxf);
			File corrected_file = foundFile(temp_file_name_writeavidmxf);
			
			extracted_path = new File(FilenameUtils.removeExtension(corrected_file.getPath()) + ".mxf");
			if (corrected_file.renameTo(extracted_path) == false) {
				throw new FileNotFoundException("Can't rename file " + corrected_file + " to " + extracted_path.getName());
			}
			
			String temp_file_name_mxf2raw = makeTempFilePath("mxf2raw");
			execMxf2raw(extracted_path, temp_file_name_mxf2raw);
			corrected_file = foundFile(temp_file_name_mxf2raw);
			FileUtils.forceDelete(extracted_path);
			
			extracted_path = new File(makeTempFilePath("bmxtranswrap") + ".mxf");
			execBmxtranswrap(corrected_file, extracted_path);
			FileUtils.forceDelete(corrected_file);
		}
		
		// TODO do an analyse of output file (converted) with ffprobe and bmx
		
		/**
		 * No error are allowed
		 */
		bmx.analystFile(extracted_path);
		ffprobe = ffprobe_jaxb.analystFile(extracted_path);
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
		process.start();
	}
	
	private void execWriteavidmxf(File source_file, String output_file_base_name) throws IOException {
		ArrayList<String> param = new ArrayList<String>();
		param.add("--prefix");
		param.add(output_file_base_name);
		param.add("--unc1080i");
		param.add(source_file.getAbsolutePath());
		
		ExecprocessGettext process = new ExecprocessGettext(ExecBinaryPath.get("writeavidmxf"), param);
		process.setEndlinewidthnewline(true);
		
		Loggers.Transcode.info("Extract MXF essence: " + process.getRunprocess().getCommandline());
		process.start();
	}
	
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
		if (extracted_path != null) {
			return extracted_path;
		}
		return original_atom;
	}
	
	/**
	 * @return true if the file returned by getValidAtomFile is not manageable directly by bmxtranswrap
	 */
	boolean needsToBeWrapWithFFmpeg() {
		return bmx.isLoaded() == false | ffprobe == null;
	}
	
	int getMXFStreamMap() {
		// TODO
		return 0;
	}
	
	String getStartTC() {
		// TODO
		/*BBCBmx bmx = mxf_files.get(0).metadatas.getByType(BBCBmx.ES_TYPE, BBCBmx.class);
		String source_tc_in = bmx.getMXFStartTimecode();
		if (source_tc_in == null) {
			source_tc_in = "00:00:00:00";
			Loggers.Transcode.warn("Can't extract TC from source " + mxf_files.get(0).path);
		}
		*/
		return null;
	}
	
	String getName() {
		// TODO
		/*
			String source_name = bmx.getMXFName();
			if (source_name == null) {
			source_name = "";
			Loggers.Transcode.warn("Can't extract name from source " + mxf_files.get(0).path);
			}*/
		return null;
	}
	
	void clean() {
		if (extracted_path != null) {
			if (extracted_path.exists()) {
				try {
					FileUtils.forceDelete(extracted_path);
				} catch (IOException e) {
					Loggers.Transcode.error("Can't delete temp file", e);
				}
			}
		}
	}
	
	public String toString() {
		return this.original_atom.getPath();
	}
}
