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
 * Copyright (C) hdsdi3g for hd3g.tv 2016-2017
 * 
*/
package hd3gtv.mydmam.transcode.kit;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.transcode.ProcessingKit;
import hd3gtv.mydmam.transcode.ProcessingKitInstance;
import hd3gtv.mydmam.transcode.mtdcontainer.FFprobeJAXB;
import hd3gtv.tools.ExecBinaryPath;
import hd3gtv.tools.ExecprocessGettext;
import hd3gtv.tools.ExecprocessPipedCascade;
import hd3gtv.tools.XmlData;

public class PKitOpAtomTo1A_XMLBased extends ProcessingKit {
	
	public boolean isFunctionnal() {
		try {
			ExecBinaryPath.get("ffprobe");
			ExecBinaryPath.get("ffmpeg");
			ExecBinaryPath.get("mxf2raw");
			ExecBinaryPath.get("bmxtranswrap");
			ExecBinaryPath.get("writeavidmxf");
			new FFprobeJAXB();
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
		return "2.0";
	}
	
	public ProcessingKitInstance createInstance(File temp_directory) throws Exception {
		return new Instance(temp_directory);
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
			
			ArrayList<File> all_mxf_files = new ArrayList<>(5);
			
			all_mxf_files.add(new File(FilenameUtils.removeExtension(physical_source.getAbsolutePath()) + ".mxf").getCanonicalFile());
			URL dest_archive = null;
			String outputfile_basename = "(error).mxf";
			
			NodeList document_items = order_xml.getDocumentElement().getChildNodes();
			for (int pos = 0; pos < document_items.getLength(); pos++) {
				Element node = (Element) document_items.item(pos);
				if (node.getTagName().equals("atom1")) {
					all_mxf_files.add(new File(FilenameUtils.getFullPath(physical_source.getAbsolutePath()) + node.getTextContent()).getCanonicalFile());
				} else if (node.getTagName().equals("atom2")) {
					all_mxf_files.add(new File(FilenameUtils.getFullPath(physical_source.getAbsolutePath()) + node.getTextContent()).getCanonicalFile());
				} else if (node.getTagName().equals("atom3")) {
					all_mxf_files.add(new File(FilenameUtils.getFullPath(physical_source.getAbsolutePath()) + node.getTextContent()).getCanonicalFile());
				} else if (node.getTagName().equals("atom4")) {
					all_mxf_files.add(new File(FilenameUtils.getFullPath(physical_source.getAbsolutePath()) + node.getTextContent()).getCanonicalFile());
				} else if (node.getTagName().equals("archive")) {
					dest_archive = new URL(node.getTextContent());
				} else if (node.getTagName().equals("mediaid")) {
					outputfile_basename = node.getTextContent().trim() + ".mxf";
				} else {
					throw new IOException("Invalid element in XML Document. Element is <" + node.getTagName() + ">");
				}
			}
			
			/**
			 * Prepare output file
			 */
			File result_op1a = null;
			String chroot_ftp = Configuration.global.getValue("PKitOpAtomTo1A_XMLBased", "chroot_ftp", "/tmp");
			if (chroot_ftp.equals("")) {
				if (this.dest_base_directory != null) {
					result_op1a = new File(dest_base_directory.getAbsolutePath() + File.separator + outputfile_basename);
				} else if (this.transcode_context.getLocalDestDirectory() != null) {
					result_op1a = new File(transcode_context.getLocalDestDirectory().getAbsolutePath() + File.separator + outputfile_basename);
				} else {
					result_op1a = new File(all_mxf_files.get(0).getParent() + File.separator + FilenameUtils.getBaseName(outputfile_basename) + "-OP1A.mxf");
				}
			} else {
				result_op1a = new File(new File(chroot_ftp).getAbsolutePath() + File.separator + dest_archive.getPath() + File.separator + outputfile_basename);
			}
			
			FileUtils.forceMkdir(result_op1a.getParentFile());
			if (result_op1a.exists()) {
				FileUtils.forceDelete(result_op1a);
			}
			
			/**
			 * Search some missing MXF files
			 */
			List<File> error_files = all_mxf_files.stream().filter(mxf_file -> {
				return mxf_file.exists() == false;
			}).filter(mxf_file -> {
				return mxf_file.isFile() == false;
			}).filter(mxf_file -> {
				return mxf_file.canRead() == false;
			}).collect(Collectors.toList());
			
			if (error_files.isEmpty() == false) {
				Loggers.Transcode.error("Can't found some MXF files: " + error_files + ", archive xml file to xml-old");
				FileUtils.copyFile(physical_source, new File(FilenameUtils.removeExtension(physical_source.getAbsolutePath()) + ".xml-old"));
				return null;
			}
			
			/**
			 * Create atom list
			 */
			AtomicInteger inc = new AtomicInteger(0);
			List<PKitOpAtomTo1A_XMLBasedAtom> all_atoms = all_mxf_files.stream().distinct().map(mxf_file -> {
				return new PKitOpAtomTo1A_XMLBasedAtom(mxf_file, FilenameUtils.removeExtension(physical_source.getName()) + "_" + String.valueOf(inc.getAndIncrement()));
			}).collect(Collectors.toList());
			
			/**
			 * Analyst and correct each atom, stop at the first error
			 */
			Optional<Exception> o_exception = all_atoms.stream().map(atom -> {
				try {
					atom.analystNcorrect();
					return null;
				} catch (Exception e) {
					return e;
				}
			}).filter(error -> {
				return error != null;
			}).findFirst();
			
			if (o_exception.isPresent()) {
				Loggers.Transcode.error("Can't analyst some MXF files, archive xml file to xml-old", o_exception.get());
				FileUtils.copyFile(physical_source, new File(FilenameUtils.removeExtension(physical_source.getAbsolutePath()) + ".xml-old"));
				return null;
			}
			
			/**
			 * Get source name
			 */
			String source_name = all_atoms.stream().map(atom -> {
				return atom.getName();
			}).filter(name -> {
				return name != null;
			}).filter(name -> {
				return name.isEmpty() == false;
			}).findFirst().orElseGet(() -> {
				Loggers.Transcode.warn("Can't extract MXF Name from files: " + all_atoms);
				return "";
			});
			
			/**
			 * Get source timecode
			 */
			String source_tc_in = all_atoms.stream().map(atom -> {
				return atom.getStartTC();
			}).filter(name -> {
				return name != null;
			}).filter(name -> {
				return name.isEmpty() == false;
			}).findFirst().orElseGet(() -> {
				Loggers.Transcode.warn("Can't extract MXF Start TC from files: " + all_atoms);
				return "";
			});
			
			/**
			 * Prepare bmxtranswrap process exec
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
			
			/**
			 * Muxing operation
			 */
			if (all_atoms.stream().filter(atom -> {
				return atom.needsToBeWrapWithFFmpeg();
			}).findFirst().isPresent()) {
				/**
				 * Start ffmpeg for rewrap raw streams, and pipe to bmxtranswrap to set metadatas.
				 */
				ExecprocessPipedCascade pipe = new ExecprocessPipedCascade();
				pipe.setWorkingDirectory(result_op1a.getParentFile());
				
				ArrayList<String> params_ffmpeg = new ArrayList<>();
				params_ffmpeg.add("-y");
				all_atoms.stream().forEach(atom -> {
					params_ffmpeg.add("-i");
					params_ffmpeg.add(atom.getValidAtomFile().getAbsolutePath());
				});
				
				params_ffmpeg.add("-codec:v");
				params_ffmpeg.add("copy");
				params_ffmpeg.add("-codec:a");
				params_ffmpeg.add("copy");
				all_atoms.stream().forEach(atom -> {
					params_ffmpeg.add("-map");
					params_ffmpeg.add(String.valueOf(atom.getMXFStreamMap()));
				});
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
				all_atoms.stream().forEach(atom -> {
					bmxparams.add(atom.getValidAtomFile().getAbsolutePath());
				});
				
				ExecprocessGettext bmx_process = new ExecprocessGettext(ExecBinaryPath.get("bmxtranswrap"), bmxparams);
				bmx_process.setWorkingDirectory(result_op1a.getParentFile());
				
				if (progression != null) {
					progression.update("Wrap all essences/Atom with bmxtranswrap");
				}
				
				bmx_process.start(stoppable);
			}
			
			/*if (stoppable != null) {
			if (stoppable.isWantToStopCurrentProcessing() == true) {
				return null;
			}
			}
			
			if (progression != null) {
			progression.update("Remove source files");
			}*/
			
			/**
			 * Remove source files.
			 */
			all_atoms.forEach(atom -> {
				atom.clean();
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
