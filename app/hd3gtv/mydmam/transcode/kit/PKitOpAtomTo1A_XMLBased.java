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
package hd3gtv.mydmam.transcode.kit;

import java.io.File;
import java.io.IOException;
import java.util.List;

import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.transcode.ProcessingKit;
import hd3gtv.mydmam.transcode.ProcessingKitInstance;
import hd3gtv.tools.ExecBinaryPath;

public class PKitOpAtomTo1A_XMLBased extends ProcessingKit {
	
	public boolean isFunctionnal() {
		try {
			ExecBinaryPath.get("ffprobe");
			ExecBinaryPath.get("ffmpeg");
			ExecBinaryPath.get("mxf2raw");
			return true;
		} catch (Exception e) {
		}
		return false;
	}
	
	public boolean validateItem(Container indexing_result) {
		return indexing_result.getSummary().equalsMimetype("text/xml", "application/xml");
	}
	
	public String getDescription() {
		return "Wrap some MXF OpAtom files to an OP1A file, and move it to a specific dest, all is based to a XML file.";
	}
	
	public String getVendor() {
		return "Internal MyDMAM";
	}
	
	public String getVersion() {
		return "1.0";
	}
	
	public ProcessingKitInstance createInstance(File temp_directory) throws Exception {
		return new Instance(temp_directory);
	}
	
	public class Instance extends ProcessingKitInstance {
		
		public Instance(File temp_directory) throws NullPointerException, IOException {
			super(temp_directory);
		}
		
		public List<File> process(File physical_source, Container source_indexing_result) throws Exception {
			// TODO Auto-generated method stub
			/**
			 * Open XML.
			 * Get all files, do MXF analyst on it.
			 * Try to process video file (if exists).
			 * If failed, extract raw content.
			 * Start ffmpeg for rewrap
			 * Move to dest (ftp)
			 */
			return null;
		}
		
		@Override
		public void cleanTempFiles() {
			/**
			 * Delete raw temp file, if exists
			 */
		}
		
	}
	
}
