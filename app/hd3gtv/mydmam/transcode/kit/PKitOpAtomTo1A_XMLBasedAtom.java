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

class PKitOpAtomTo1A_XMLBasedAtom {
	
	private File original_atom;
	// File extracted_path;
	
	PKitOpAtomTo1A_XMLBasedAtom(File original_atom) {
		this.original_atom = original_atom;
	}
	
	void analystNcorrect() throws Exception {
		/* 
		ffprobe and bmx maybe don't works
		*/
		// BBCBmx bmx = new BBCBmx();
		// bmx.analystFile(source_file);
		// ffprobe_jaxb.analystFile(local_file)
		
		// TODO get is audio / video
		// TODO get duration
		// TODO get name
		// TODO get mob
		
		// TODO correct if needed
		/* SIMPLE CORRECT
		 * 				File temp_dir = item.path.getParentFile();
				if (temp_directory != null) {
					temp_dir = temp_directory;
				}
				temp_dir = temp_dir.getCanonicalFile();
				
				String temp_file_name = temp_dir.getPath() + File.separator + item.metadatas.getMtd_key() + "_" + (pos + 1);
				
				ArrayList<String> param = new ArrayList<String>();
				param.add("--ess-out");
				param.add(temp_file_name);
				param.add(item.path.getAbsolutePath());
				
				ExecprocessGettext process = new ExecprocessGettext(ExecBinaryPath.get("mxf2raw"), param);
				process.setEndlinewidthnewline(true);
				
				Loggers.Transcode.info("Extract MXF essences with mxf2raw: " + process.getRunprocess().getCommandline());
				try {
					process.start();
				} catch (IOException e) {
					if (e instanceof ExecprocessBadExecutionException) {
						Loggers.Transcode_Metadata.error("Problem with mxf2raw extraction (BBC BMX), " + process + ", " + item.metadatas);
					}
					throw e;
				}
				
				String[] raw_files_names = temp_dir.list(new FilenameFilter() {
					
					@Override
					public boolean accept(File dir, String name) {
						return name.startsWith(item.metadatas.getMtd_key());
					}
				});
				if (raw_files_names.length != 1) {
					throw new IndexOutOfBoundsException("Cant found BMX temp file !" + Arrays.asList(raw_files_names));
				}
				
				File raw_file = new File(temp_dir.getAbsolutePath() + File.separator + raw_files_names[0]);
				if (raw_file.exists() == false) {
					throw new FileNotFoundException(raw_file.getAbsolutePath());
				}
				item.extracted_path = new File(temp_file_name + ".mxf");
				raw_file.renameTo(item.extracted_path);
				files_to_clean.add(item.extracted_path);
		
		 * */
		
	}
	
	File getValidAtomFile() {
		// TODO
		
		return original_atom;
	}
	
	/**
	 * @return true if the file returned by getValidAtomFile is not manageable directly by bmxtranswrap
	 */
	boolean needsToBeWrapWithFFmpeg() {
		// TODO
		return false;
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
		// TODO remove corrected files
	}
	
	public String toString() {
		return this.original_atom.getPath();
	}
}
