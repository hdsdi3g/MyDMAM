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
import hd3gtv.mydmam.transcode.mtdcontainer.BBCBmx;
import hd3gtv.mydmam.transcode.mtdcontainer.FFprobe;
import hd3gtv.tools.ExecBinaryPath;
import hd3gtv.tools.Execprocess;
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
			if (progression != null) {
				progression.update("Open order XML");
			}
			
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
			boolean need_to_wrap_with_ffmpeg = false;
			
			for (int pos = 0; pos < mxf_files.size(); pos++) {
				Atom item = mxf_files.get(pos);
				if (item.must_be_extracted == false) {
					continue;
				}
				need_to_wrap_with_ffmpeg = true;
				
				if (progression != null) {
					progression.update("Extract raw MXF essence (" + (pos + 1) + "/" + mxf_files.size() + ")");
				}
				if (stoppable != null) {
					if (stoppable.isWantToStopCurrentProcessing() == true) {
						return null;
					}
				}
				
				File temp_dir = item.path.getParentFile();
				if (temp_directory != null) {
					temp_dir = temp_directory;
				}
				
				String temp_file_name = temp_dir.getAbsolutePath() + item.metadatas.getMtd_key() + "_" + (pos + 1);
				
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
				
				File raw_file = new File(raw_files_names[0]);
				if (raw_file.exists() == false) {
					throw new FileNotFoundException(raw_file.getAbsolutePath());
				}
				item.extracted_path = new File(temp_file_name + ".mxf");
				raw_file.renameTo(item.extracted_path);
			}
			
			/**
			 * Get source timecode and name
			 */
			BBCBmx bmx = mxf_files.get(0).metadatas.getByClass(BBCBmx.class);
			String source_tc_in = bmx.getBmx().getClip().getStartTimecodes().getPhysicalSource().getValue();
			String source_name = bmx.getBmx().getFiles().getFile().get(0).getMaterialPackages().getMaterialPackage().get(0).getName();
			
			File result_op1a = null;
			if (this.dest_base_directory != null) {
				result_op1a = new File(this.dest_base_directory.getAbsolutePath() + File.separator + mxf_files.get(0).path.getName());
			} else if (this.transcode_context.getLocalDestDirectory() != null) {
				result_op1a = new File(this.transcode_context.getLocalDestDirectory().getAbsolutePath() + File.separator + mxf_files.get(0).path.getName());
			} else {
				result_op1a = new File(mxf_files.get(0).path.getParent() + File.separator + FilenameUtils.getBaseName(mxf_files.get(0).path.getName()) + "-OP1A.mxf");
			}
			
			if (need_to_wrap_with_ffmpeg) {
				/**
				 * Start ffmpeg for rewrap raw streams, and pipe to bmxtranswrap to set metadatas.
				 */
				ArrayList<String> params_streams = new ArrayList<>();
				ArrayList<String> params_maps = new ArrayList<>();
				for (int pos = 0; pos < mxf_files.size(); pos++) {
					Atom item = mxf_files.get(pos);
					params_streams.add("-i");
					if (item.extracted_path != null) {
						params_streams.add(item.extracted_path.getAbsolutePath());
					} else {
						params_streams.add(item.path.getAbsolutePath());
					}
					params_maps.add("-map");
					params_maps.add(pos + ":0");
				}
				
				ArrayList<String> params_ffmpeg = new ArrayList<>();
				params_ffmpeg.add("-y");
				params_ffmpeg.addAll(params_streams);
				params_ffmpeg.add("-codec:v");
				params_ffmpeg.add("copy");
				params_ffmpeg.add("-codec:a");
				params_ffmpeg.add("copy");
				params_ffmpeg.addAll(params_maps);
				params_ffmpeg.add("-f");
				params_ffmpeg.add("mxf");
				params_ffmpeg.add("-");
				
				if (progression != null) {
					progression.update("Wrap all essences/Atom with ffmpeg");
				}
				if (stoppable != null) {
					if (stoppable.isWantToStopCurrentProcessing() == true) {
						return null;
					}
				}
				
				Execprocess ffmpeg_process = new Execprocess(ExecBinaryPath.get("ffmpeg"), params_ffmpeg);
				ffmpeg_process.setDaemon(true);
				ffmpeg_process.setName("ffmpeg-transc-" + source_name);
				// ffmpeg_process.setOutputstreamhandler(outputstreamhandler);
				
				/*try {
					 process.start();
				} catch (IOException e) {
					if (e instanceof ExecprocessBadExecutionException) {
						 Loggers.Transcode_Metadata.error("Problem with ffmpeg wrapping, " + process);
					}
					throw e;
				}*/
			} else {
				/**
				 * Use bmxtranswrap for rewap atoms and set metadatas
				 */
				
			}
			
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
