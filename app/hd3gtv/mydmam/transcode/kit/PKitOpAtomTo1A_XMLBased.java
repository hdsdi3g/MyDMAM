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
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.apache.commons.io.FilenameUtils;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.metadata.MetadataIndexingLimit;
import hd3gtv.mydmam.metadata.MetadataIndexingOperation;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.transcode.ProcessingKit;
import hd3gtv.mydmam.transcode.ProcessingKitInstance;
import hd3gtv.mydmam.transcode.mtdcontainer.FFprobe;
import hd3gtv.tools.ExecBinaryPath;
import hd3gtv.tools.ExecprocessBadExecutionException;
import hd3gtv.tools.ExecprocessGettext;
import hd3gtv.tools.XmlData;

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
	
	private class Atom {
		File path;
		Container metadatas;
		boolean must_be_extracted = false;
		File extracted_path;
		
		private Atom(File path) {
			this.path = path;
		}
	}
	
	public class Instance extends ProcessingKitInstance {
		
		public Instance(File temp_directory) throws NullPointerException, IOException {
			super(temp_directory);
		}
		
		public List<File> process(File physical_source, Container source_indexing_result) throws Exception {
			/**
			 * Open XML.
			 **/
			XmlData order_xml = XmlData.loadFromFile(physical_source);
			if (order_xml.getDocumentElement().getTagName().equals("wrap") == false) {
				throw new IOException("Invalid format for XML document. Document element is <" + order_xml.getDocumentElement().getTagName() + ">");
			}
			
			File file_atom_0 = new File(FilenameUtils.removeExtension(physical_source.getAbsolutePath()) + ".mxf");
			File file_atom_1 = new File("");
			File file_atom_2 = new File("");
			File file_atom_3 = new File("");
			File file_atom_4 = new File("");
			URL dest_archive = null;
			
			NodeList document_items = order_xml.getDocumentElement().getChildNodes();
			for (int pos = 0; pos < document_items.getLength(); pos++) {
				Element node = (Element) document_items.item(pos);
				if (node.getTagName().equals("atom1")) {
					file_atom_1 = new File(FilenameUtils.getFullPath(physical_source.getAbsolutePath()) + node.getTextContent());
				} else if (node.getTagName().equals("atom2")) {
					file_atom_2 = new File(FilenameUtils.getFullPath(physical_source.getAbsolutePath()) + node.getTextContent());
				} else if (node.getTagName().equals("atom3")) {
					file_atom_3 = new File(FilenameUtils.getFullPath(physical_source.getAbsolutePath()) + node.getTextContent());
				} else if (node.getTagName().equals("atom4")) {
					file_atom_4 = new File(FilenameUtils.getFullPath(physical_source.getAbsolutePath()) + node.getTextContent());
				} else if (node.getTagName().equals("dest_archive")) {
					dest_archive = new URL(node.getTextContent());
				} else {
					throw new IOException("Invalid element in XML Document. Element is <" + node.getTagName() + ">");
				}
			}
			
			ArrayList<Atom> mxf_files = new ArrayList<>(5);
			if (file_atom_0.exists() && file_atom_0.isFile()) {
				mxf_files.add(new Atom(file_atom_0));
			} else {
				throw new FileNotFoundException("Can't found main MXF file " + file_atom_0);
			}
			if (file_atom_1.exists() && file_atom_1.isFile()) {
				mxf_files.add(new Atom(file_atom_1));
			}
			if (file_atom_2.exists() && file_atom_2.isFile()) {
				mxf_files.add(new Atom(file_atom_2));
			}
			if (file_atom_3.exists() && file_atom_3.isFile()) {
				mxf_files.add(new Atom(file_atom_3));
			}
			if (file_atom_4.exists() && file_atom_4.isFile()) {
				mxf_files.add(new Atom(file_atom_4));
			}
			
			/**
			 * Get all files, do MXF analyst on it.
			 */
			mxf_files.forEach(atom -> {
				SourcePathIndexerElement spie = new SourcePathIndexerElement();
				spie.currentpath = "/cli-request/" + System.currentTimeMillis();
				spie.date = atom.path.lastModified();
				spie.dateindex = System.currentTimeMillis();
				spie.directory = false;
				spie.parentpath = "/cli-request";
				spie.size = atom.path.length();
				spie.storagename = "MyDMAM-Request";
				
				try {
					atom.metadatas = new MetadataIndexingOperation(atom.path).setLimit(MetadataIndexingLimit.FAST).doIndexing();
				} catch (Exception e) {
					Loggers.Metadata.warn("Can't extract medatatas", e);
					return;
				}
				if (atom.metadatas.getByClass(FFprobe.class) == null) {
					atom.must_be_extracted = true;
				}
			});
			
			/**
			 * Extract raw content.
			 */
			for (int pos = 0; pos < mxf_files.size(); pos++) {
				Atom item = mxf_files.get(pos);
				if (item.must_be_extracted == false) {
					continue;
				}
				
				String temp_file_name = FilenameUtils.getFullPath(item.path.getAbsolutePath()) + item.metadatas.getMtd_key();
				
				ArrayList<String> param = new ArrayList<String>();
				param.add("--ess-out");
				param.add(temp_file_name);
				param.add(item.path.getAbsolutePath());
				
				ExecprocessGettext process = new ExecprocessGettext(ExecBinaryPath.get("mxf2raw"), param);
				process.setEndlinewidthnewline(true);
				try {
					process.start();
				} catch (IOException e) {
					if (e instanceof ExecprocessBadExecutionException) {
						Loggers.Transcode_Metadata.error("Problem with mxf2raw extraction (BBC BMX), " + process + ", " + item.metadatas);
					}
					throw e;
				}
				
				String[] raw_files_names = (new File(FilenameUtils.getFullPath(item.path.getAbsolutePath()))).list(new FilenameFilter() {
					
					@Override
					public boolean accept(File dir, String name) {
						return name.startsWith(item.metadatas.getMtd_key());
					}
				});
				if (raw_files_names.length != 1) {
					throw new IndexOutOfBoundsException("Cant found BMX temp file !" + raw_files_names);
				}
				
				item.extracted_path = new File(raw_files_names[0]);
			}
			
			/**
			 * Start ffmpeg for rewrap
			 */
			// TODO add source timecode
			/*ffmpeg -y $ALL_STREAMS -codec:v copy -codec:a copy $ALL_MAPS -f mxf $FILE_DEST.mxf
			ALL_STREAMS=$ALL_STREAMS" -i "$(extractStreamPath $2);
			ALL_MAPS=$ALL_MAPS" -map 0:0";*/
			
			/**
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
