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
	// File extracted_path;
	
	PKitOpAtomTo1A_XMLBasedAtom(File original_atom) {
		this.original_atom = original_atom;
		bmx = new BBCBmx();
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
			/* TODO SIMPLE CORRECT
				File temp_dir = item.path.getParentFile();
			if (temp_directory != null) {
			temp_dir = temp_directory;
			}
			temp_dir = temp_dir.getCanonicalFile();
			
			String temp_file_name = temp_dir.getPath() + File.separator + item.metadatas.getMtd_key() + "_" + (pos + 1);
			
			mxf2raw(item.path, temp_file_name)
						
			File raw_file = foundFile(temp_dir, item.metadatas.getMtd_key())
			if (raw_file.exists() == false) {
			throw new FileNotFoundException(raw_file.getAbsolutePath());
			}
			item.extracted_path = new File(temp_file_name + ".mxf");
			raw_file.renameTo(item.extracted_path);
			files_to_clean.add(item.extracted_path);
			
			* */
		} else {
			/*
			 * TODO big correction
			 * 
			 * writeavidmxf --prefix out --unc1080i remont_e_v01f1e13bb4.mxf
			> out_v1.mxf
			execWriteavidmxf(source_file, output_file_base_name)
			
			mxf2raw --ess-out outraw out_v1.mxf
			>  outraw_v0.raw 
			mxf2raw(item.path, temp_file_name)
			
			bmxtranswrap -t op1a -y 00:00:00:00 --clip AAA -o remont_e_v01f1e13bb4-corrige.mxf outraw_v0.raw
			> remont_e_v01f1e13bb4-corrige.mxf
			
			execBmxtranswrap(File source_file, File dest_file)
			 * 
			 */
		}
		
		// TODO do an analyse of output file (converted) with ffprobe and bmx
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
	
	private File foundFile(File directory, String basename) throws FileNotFoundException {
		File[] founded = directory.listFiles((dir, name) -> {
			return name.startsWith(basename);
		});
		
		if (founded.length != 1) {
			throw new FileNotFoundException(directory.getPath() + File.separator + basename + "*");
		}
		return founded[0];
	}
	
	File getValidAtomFile() {
		// TODO
		
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
		if (needsToBeWrapWithFFmpeg() == false) {
			return;
		}
		
		// TODO remove corrected files
	}
	
	public String toString() {
		return this.original_atom.getPath();
	}
}
