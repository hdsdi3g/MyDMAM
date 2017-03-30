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
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import hd3gtv.configuration.Configuration;
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
import hd3gtv.tools.ExecprocessBadExecutionException;
import hd3gtv.tools.ExecprocessGettext;
import hd3gtv.tools.ExecprocessPipedCascade;
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
		private ArrayList<File> files_to_clean;
		
		public Instance(File temp_directory) throws NullPointerException, IOException {
			super(temp_directory);
			files_to_clean = new ArrayList<>();
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
			
			File file_atom_0 = new File(FilenameUtils.removeExtension(physical_source.getAbsolutePath()) + ".mxf").getCanonicalFile();
			File file_atom_1 = new File("");
			File file_atom_2 = new File("");
			File file_atom_3 = new File("");
			File file_atom_4 = new File("");
			URL dest_archive = null;
			String outputfile_basename = "(error).mxf";
			
			NodeList document_items = order_xml.getDocumentElement().getChildNodes();
			for (int pos = 0; pos < document_items.getLength(); pos++) {
				Element node = (Element) document_items.item(pos);
				if (node.getTagName().equals("atom1")) {
					file_atom_1 = new File(FilenameUtils.getFullPath(physical_source.getAbsolutePath()) + node.getTextContent()).getCanonicalFile();
				} else if (node.getTagName().equals("atom2")) {
					file_atom_2 = new File(FilenameUtils.getFullPath(physical_source.getAbsolutePath()) + node.getTextContent()).getCanonicalFile();
				} else if (node.getTagName().equals("atom3")) {
					file_atom_3 = new File(FilenameUtils.getFullPath(physical_source.getAbsolutePath()) + node.getTextContent()).getCanonicalFile();
				} else if (node.getTagName().equals("atom4")) {
					file_atom_4 = new File(FilenameUtils.getFullPath(physical_source.getAbsolutePath()) + node.getTextContent()).getCanonicalFile();
				} else if (node.getTagName().equals("archive")) {
					dest_archive = new URL(node.getTextContent());
				} else if (node.getTagName().equals("mediaid")) {
					outputfile_basename = node.getTextContent().trim() + ".mxf";
				} else {
					throw new IOException("Invalid element in XML Document. Element is <" + node.getTagName() + ">");
				}
			}
			
			ArrayList<Atom> mxf_files = new ArrayList<>(5);
			if (file_atom_0.exists() && file_atom_0.isFile()) {
				mxf_files.add(new Atom(file_atom_0));
			} else {
				Loggers.Transcode.warn("Can't found main MXF file " + file_atom_0 + ", archive xml file to xml-old");
				FileUtils.copyFile(physical_source, new File(FilenameUtils.removeExtension(physical_source.getAbsolutePath()) + ".xml-old"));
				return null;
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
				
				if (atom.metadatas.getByType(FFprobe.ES_TYPE, FFprobe.class) == null) {
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
			}
			
			/**
			 * Get source timecode and name
			 */
			BBCBmx bmx = mxf_files.get(0).metadatas.getByType(BBCBmx.ES_TYPE, BBCBmx.class);
			String source_tc_in = bmx.getMXFStartTimecode();
			if (source_tc_in == null) {
				source_tc_in = "00:00:00:00";
				Loggers.Transcode.warn("Can't extract TC from source " + mxf_files.get(0).path);
			}
			
			String source_name = bmx.getMXFName();
			if (source_name == null) {
				source_name = "";
				Loggers.Transcode.warn("Can't extract name from source " + mxf_files.get(0).path);
			}
			
			/**
			 * Prepare output file
			 */
			File result_op1a = null;
			String chroot_ftp = Configuration.global.getValue("PKitOpAtomTo1A_XMLBased", "chroot_ftp", "/tmp");
			if (chroot_ftp.equals("")) {
				if (this.dest_base_directory != null) {
					result_op1a = new File(this.dest_base_directory.getAbsolutePath() + File.separator + outputfile_basename);
				} else if (this.transcode_context.getLocalDestDirectory() != null) {
					result_op1a = new File(this.transcode_context.getLocalDestDirectory().getAbsolutePath() + File.separator + outputfile_basename);
				} else {
					result_op1a = new File(mxf_files.get(0).path.getParent() + File.separator + FilenameUtils.getBaseName(outputfile_basename) + "-OP1A.mxf");
				}
			} else {
				result_op1a = new File(new File(chroot_ftp).getAbsolutePath() + File.separator + dest_archive.getPath() + File.separator + outputfile_basename);
			}
			
			FileUtils.forceMkdir(result_op1a.getParentFile());
			if (result_op1a.exists()) {
				FileUtils.forceDelete(result_op1a);
			}
			
			/**
			 * Prepare process exec
			 */
			ArrayList<String> bmxparams = new ArrayList<>();
			bmxparams.add("-t");
			bmxparams.add("op1a");
			bmxparams.add("-y");
			bmxparams.add(source_tc_in);
			bmxparams.add("--clip");
			bmxparams.add(source_name);
			bmxparams.add("-o");
			bmxparams.add(result_op1a.getAbsolutePath());
			
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
				
				ExecprocessPipedCascade pipe = new ExecprocessPipedCascade();
				pipe.setWorkingDirectory(result_op1a.getParentFile());
				
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
				pipe.add(ExecBinaryPath.get("ffmpeg"), params_ffmpeg);
				
				bmxparams.add("-");
				pipe.add(ExecBinaryPath.get("bmxtranswrap"), bmxparams);
				
				if (progression != null) {
					progression.update("Wrap all essences/Atom with ffmpeg and bmxtranswrap");
				}
				
				pipe.startAll();
				boolean is_ok = false;
				if (stoppable != null) {
					is_ok = pipe.waitExec(stoppable);
					if (stoppable.isWantToStopCurrentProcessing() == true) {
						return null;
					}
				} else {
					is_ok = pipe.waitExec();
				}
				
				if (is_ok == false) {
					throw new IOException("Can't process wrap with bmx and ffmpeg");
				}
				
			} else {
				/**
				 * Use bmxtranswrap for rewap atoms and set metadatas
				 */
				for (int pos = 0; pos < mxf_files.size(); pos++) {
					Atom item = mxf_files.get(pos);
					bmxparams.add(item.path.getAbsolutePath());
				}
				
				ExecprocessGettext bmx_process = new ExecprocessGettext(ExecBinaryPath.get("bmxtranswrap"), bmxparams);
				bmx_process.setWorkingDirectory(result_op1a.getParentFile());
				
				if (progression != null) {
					progression.update("Wrap all essences/Atom with bmxtranswrap");
				}
				
				bmx_process.start(stoppable);
			}
			
			if (stoppable != null) {
				if (stoppable.isWantToStopCurrentProcessing() == true) {
					return null;
				}
			}
			if (progression != null) {
				progression.update("Remove source files");
			}
			
			/**
			 * Remove source files.
			 */
			mxf_files.forEach(atom -> {
				atom.path.delete();
			});
			
			return null;
		}
		
		public void cleanTempFiles() {
			/**
			 * Delete raw temp files, if exists
			 */
			files_to_clean.forEach(v -> {
				try {
					FileUtils.forceDelete(v);
				} catch (Exception e) {
					Loggers.Transcode.warn("Can't clean temp file", e);
				}
			});
		}
		
	}
}
