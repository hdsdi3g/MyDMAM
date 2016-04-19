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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.mydmam.metadata;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.FileUtils;

import com.eaio.uuid.UUID;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.manager.InstanceStatus;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.ContainerEntry;
import hd3gtv.mydmam.metadata.container.ContainerOperations;
import hd3gtv.mydmam.metadata.container.EntryRenderer;
import hd3gtv.mydmam.metadata.container.EntrySummary;
import hd3gtv.mydmam.metadata.container.RenderedContent;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.storage.Storage;

public class RenderedFile {
	
	private static File temp_directory;
	private static File local_directory;
	private static String digest_algorithm;
	private static volatile Random random;
	private static volatile ArrayList<File> commit_log_files;
	
	// TODO change sub dirs by AB/CD/EFGHI instead of AB/CDEFGHI
	
	static {
		try {
			commit_log_files = new ArrayList<File>();
			
			digest_algorithm = "MD5";
			MessageDigest.getInstance(digest_algorithm);
			
			temp_directory = MetadataCenter.rendering_temp_directory;
			if (temp_directory == null) {
				throw new NullPointerException("temp_directory");
			}
			
			local_directory = MetadataCenter.rendering_local_directory;
			if (local_directory == null) {
				throw new NullPointerException("temp_directory");
			}
			
			if (temp_directory.exists() == false) {
				throw new FileNotFoundException(temp_directory.getPath());
			}
			if (temp_directory.isDirectory() == false) {
				throw new FileNotFoundException("Not a directory: " + temp_directory.getPath());
			}
			if (temp_directory.canRead() == false) {
				throw new IOException("Can't read directory: " + temp_directory.getPath());
			}
			if (temp_directory.canWrite() == false) {
				throw new IOException("Can't write directory: " + temp_directory.getPath());
			}
			StringBuffer sb = new StringBuffer();
			sb.append(temp_directory.getCanonicalPath());
			sb.append(File.separator);
			sb.append(InstanceStatus.getStatic().summary.getInstanceName());
			sb.append("-");
			sb.append(InstanceStatus.getStatic().summary.getPID());
			temp_directory = new File(sb.toString());
			temp_directory.mkdirs();
			
			if (local_directory.exists() == false) {
				throw new FileNotFoundException(local_directory.getPath());
			}
			if (local_directory.isDirectory() == false) {
				throw new FileNotFoundException("Not a directory: " + local_directory.getPath());
			}
			if (local_directory.canRead() == false) {
				throw new IOException("Can't read directory: " + local_directory.getPath());
			}
			if (local_directory.canWrite() == false) {
				throw new IOException("Can't write directory: " + local_directory.getPath());
			}
			
			random = new Random();
		} catch (Exception e) {
			Loggers.Metadata.error("Can't init, check configuration on metadata_analysing.temp/local_directory", e);
		}
		
	}
	
	public static String getDigest_algorithm() {
		return digest_algorithm;
	}
	
	private File commit_log_file;
	
	private String extension;
	private File rendered_file;
	private File temp_file;
	private String metadata_reference_id;
	private String rendered_base_file_name;
	private MetadataExtractor metadata_extractor;
	private boolean consolidated;
	private String rendered_mime;
	private String rendered_digest;
	
	/**
	 * Create a file like temp_directory/serviceinstancename/
	 * @param extension with or without a point
	 */
	public RenderedFile(String rendered_base_file_name, String extension) throws IOException {
		this.rendered_base_file_name = rendered_base_file_name;
		if (rendered_base_file_name == null) {
			throw new NullPointerException("\"rendered_base_file_name\" can't to be null");
		}
		
		consolidated = false;
		
		temp_directory.mkdirs();
		if (temp_directory.exists() == false) {
			throw new IOException("Can't access to local temp dir directory: " + temp_directory.getPath());
		}
		
		StringBuffer sb = new StringBuffer();
		sb.append(temp_directory.getAbsolutePath());
		sb.append(File.separator);
		
		UUID uuid = new UUID();
		sb.append(uuid.toString());
		
		this.extension = "";
		if (extension != null) {
			if (extension.startsWith(".")) {
				sb.append(extension);
				this.extension = extension;
			} else {
				sb.append(".");
				sb.append(extension);
				this.extension = "." + extension;
			}
		}
		
		commit_log_file = new File(temp_directory.getAbsolutePath() + File.separator + uuid.toString() + "-commit.log");
		commit_log_files.add(commit_log_file);
		
		FileUtils.touch(commit_log_file);
		
		temp_file = new File(sb.toString());
		if (temp_file.createNewFile()) {
			temp_file.delete();
		}
		
		writeToCommitLog("Prepare temporary file, rendered_base_file_name: " + rendered_base_file_name + ",  extension: " + extension);
	}
	
	private void writeToCommitLog(String message) {
		try {
			String caller = new Throwable().getStackTrace()[1].toString();
			Loggers.Metadata_Commitlog.debug("Write to commit log (" + commit_log_file.getName() + ") " + message);
			FileUtils.writeStringToFile(commit_log_file, Loggers.dateLog(System.currentTimeMillis()) + " at " + caller + "\t" + message + MyDMAM.LINESEPARATOR, true);
		} catch (IOException e) {
			Loggers.Metadata_Commitlog.error("Can't write to commit log (" + commit_log_file.getName() + ") this message:\t" + message);
		}
	}
	
	private RenderedFile() {
	}
	
	public File getTempFile() {
		return temp_file;
	}
	
	public void deleteTempFile() {
		if (temp_file == null) {
			return;
		}
		if (temp_file.exists() == false) {
			return;
		}
		temp_file.delete();
	}
	
	private void checkConsolidate(Container container, MetadataExtractor metadata_extractor) throws IOException {
		if (container == null) {
			throw new NullPointerException("\"source_element\" can't to be null");
		}
		metadata_reference_id = container.getMtd_key();
		
		this.metadata_extractor = metadata_extractor;
		if (metadata_extractor == null) {
			throw new NullPointerException("\"renderer\" can't to be null");
		}
		
		if (temp_file.exists() == false) {
			throw new NullPointerException(temp_file.getPath() + " don't exists");
		}
		if (temp_file.isFile() == false) {
			throw new NullPointerException(temp_file.getPath() + " is not a file");
		}
		if (temp_file.canRead() == false) {
			throw new NullPointerException("Can't read: " + temp_file.getPath());
		}
		if (temp_file.canWrite() == false) {
			throw new NullPointerException("Can't write: " + temp_file.getPath());
		}
		if (temp_file.length() == 0) {
			throw new IOException(temp_file.getPath() + " is empty");
		}
	}
	
	/**
	 * metadata_reference_id[0-2]/metadata_reference_id[2-4]/metadata_reference_id[4-]/renderedbasefilename_RandomValue.extension
	 */
	public EntryRenderer consolidateAndExportToEntry(EntryRenderer entry_renderer, Container container, MetadataExtractor metadata_extractor) throws IOException {
		if (consolidated) {
			export_to_entry(entry_renderer, container);
			return entry_renderer;
		}
		checkConsolidate(container, metadata_extractor);
		
		StringBuffer sb_base_directory_dest = new StringBuffer();
		sb_base_directory_dest.append(local_directory.getCanonicalPath());
		sb_base_directory_dest.append(File.separator);
		sb_base_directory_dest.append(metadata_reference_id.substring(0, 6));
		sb_base_directory_dest.append(File.separator);
		sb_base_directory_dest.append(metadata_reference_id.substring(6, 8));
		sb_base_directory_dest.append(File.separator);
		sb_base_directory_dest.append(metadata_reference_id.substring(8));
		String base_directory_dest = sb_base_directory_dest.toString();
		
		File f_base_directory_dest = new File(base_directory_dest);
		f_base_directory_dest.mkdirs();
		if (f_base_directory_dest.exists() == false) {
			throw new IOException("Can't create dest directory: " + sb_base_directory_dest.toString());
		}
		
		rendered_file = nextRandomFile(f_base_directory_dest);
		while (rendered_file.exists()) {
			/**
			 * Search an available file name
			 */
			writeToCommitLog("Searching new temporary file name... rendered_file: " + rendered_file);
			
			rendered_file = nextRandomFile(f_base_directory_dest);
		}
		
		MessageDigest mdigest = null;
		try {
			mdigest = MessageDigest.getInstance(digest_algorithm);
		} catch (NoSuchAlgorithmException e) {
		}
		
		writeToCommitLog("Prepare consolidate, temp_file: " + temp_file + ", rendered_file: " + rendered_file);
		
		BufferedInputStream source_stream = new BufferedInputStream(new FileInputStream(temp_file), 0xFFF);
		OutputStream dest_stream = new FileOutputStream(rendered_file);
		int len;
		byte[] buffer = new byte[0xFFF];
		
		writeToCommitLog("Start copy");
		while ((len = source_stream.read(buffer)) > 0) {
			dest_stream.write(buffer, 0, len);
			mdigest.update(buffer, 0, len);
		}
		writeToCommitLog("End copy");
		dest_stream.close();
		source_stream.close();
		
		writeToCommitLog("Delete temp file: " + temp_file + ", ok: " + temp_file.delete());
		
		rendered_digest = MyDMAM.byteToString(mdigest.digest());
		
		writeToCommitLog("Digest, rendered_file: " + rendered_file + ", rendered_digest: " + rendered_digest);
		
		FileWriter fw = new FileWriter(new File(rendered_file + "." + digest_algorithm.toLowerCase()));
		fw.write(rendered_digest + "\r\n");
		fw.close();
		writeToCommitLog("Write digest side-car");
		
		rendered_mime = MimeExtract.getMime(rendered_file);
		
		writeToCommitLog("Mime, rendered_file: " + rendered_file + ", rendered_mime: " + rendered_mime);
		
		LinkedHashMap<String, Object> log = new LinkedHashMap<String, Object>();
		log.put("metadata_reference_id", metadata_reference_id);
		log.put("source_element", container.getOrigin());
		log.put("renderer name", metadata_extractor.getLongName());
		log.put("rendered_file", rendered_file);
		log.put("rendered_mime", rendered_mime);
		log.put("rendered_digest", rendered_digest);
		Loggers.Metadata.debug("Consolidate rendered file: " + log);
		
		consolidated = true;
		
		writeToCommitLog("End consolidate");
		
		export_to_entry(entry_renderer, container);
		return entry_renderer;
	}
	
	private void export_to_entry(EntryRenderer entry_renderer, Container container) {
		if (consolidated == false) {
			throw new NullPointerException("Element is not consolidated !");
		}
		entry_renderer.setOrigin(container.getOrigin());
		
		RenderedContent rendered_content = new RenderedContent();
		rendered_content.name = rendered_file.getName();
		rendered_content.size = rendered_file.length();
		rendered_content.date = rendered_file.lastModified();
		rendered_content.hash = rendered_digest;
		rendered_content.producer = metadata_extractor.getLongName();
		rendered_content.mime = rendered_mime;
		entry_renderer.addContent(rendered_content);
	}
	
	private File nextRandomFile(File f_base_directory_dest) throws IOException {
		StringBuffer sb_rendered_file = new StringBuffer();
		sb_rendered_file.append(f_base_directory_dest.getAbsolutePath());
		sb_rendered_file.append(File.separator);
		sb_rendered_file.append(rendered_base_file_name);
		sb_rendered_file.append("_");
		byte[] b = new byte[2];
		random.nextBytes(b);
		sb_rendered_file.append(MyDMAM.byteToString(b).toUpperCase());
		sb_rendered_file.append(extension);
		return new File(sb_rendered_file.toString());
	}
	
	public boolean isConsolidated() {
		return consolidated;
	}
	
	/**
	 * @return null if not rendered.
	 */
	public File getRendered_file() {
		return rendered_file;
	}
	
	public String getRendered_digest() {
		return rendered_digest;
	}
	
	public String getRendered_mime() {
		return rendered_mime;
	}
	
	public static void copyMoveAllMetadataContent(String metadata_reference_id_from, String metadata_reference_id_dest, boolean copy) throws IOException {
		if (metadata_reference_id_from == null) {
			throw new NullPointerException("\"metadata_reference_id_from\" can't to be null");
		}
		if (metadata_reference_id_dest == null) {
			throw new NullPointerException("\"metadata_reference_id_dest\" can't to be null");
		}
		if (local_directory == null) {
			throw new IOException("No configuration is set !");
		}
		if (local_directory.exists() == false) {
			throw new IOException("Invalid configuration is set !");
		}
		
		StringBuilder sb_from_directory = new StringBuilder();
		sb_from_directory.append(local_directory.getCanonicalPath());
		sb_from_directory.append(File.separator);
		sb_from_directory.append(metadata_reference_id_from.substring(0, 6));
		sb_from_directory.append(File.separator);
		sb_from_directory.append(metadata_reference_id_from.substring(6, 8));
		sb_from_directory.append(File.separator);
		sb_from_directory.append(metadata_reference_id_from.substring(8));
		
		StringBuilder sb_dest_directory = new StringBuilder();
		sb_dest_directory.append(local_directory.getCanonicalPath());
		sb_dest_directory.append(File.separator);
		sb_dest_directory.append(metadata_reference_id_dest.substring(0, 6));
		sb_dest_directory.append(File.separator);
		sb_dest_directory.append(metadata_reference_id_dest.substring(6, 8));
		sb_dest_directory.append(File.separator);
		sb_dest_directory.append(metadata_reference_id_dest.substring(8));
		
		File from_dir = new File(sb_from_directory.toString()).getCanonicalFile();
		File dest_dir = new File(sb_dest_directory.toString()).getCanonicalFile();
		
		Loggers.Metadata.debug("Prepare operation, from: " + from_dir + ", to: " + dest_dir);
		
		/**
		 * Create sub directories.
		 */
		FileUtils.forceMkdir(dest_dir);
		FileUtils.deleteDirectory(dest_dir);
		
		if (copy == false) {
			FileUtils.moveDirectory(from_dir, dest_dir);
		} else {
			FileUtils.copyDirectory(from_dir, dest_dir, true);
		}
	}
	
	/**
	 * Test presence and validity for file.
	 */
	public static RenderedFile import_from_entry(RenderedContent content, String metadata_reference_id, boolean check_hash) throws IOException {
		if (content == null) {
			throw new NullPointerException("\"content\" can't to be null");
		}
		
		if (metadata_reference_id == null) {
			throw new NullPointerException("\"metadata_reference_id\" can't to be null");
		}
		
		if (local_directory == null) {
			throw new IOException("No configuration is set !");
		}
		if (local_directory.exists() == false) {
			throw new IOException("Invalid configuration is set !");
		}
		
		RenderedFile result = new RenderedFile();
		
		StringBuffer sb_rendered_file = new StringBuffer();
		sb_rendered_file.append(local_directory.getCanonicalPath());
		sb_rendered_file.append(File.separator);
		sb_rendered_file.append(metadata_reference_id.substring(0, 6));
		sb_rendered_file.append(File.separator);
		sb_rendered_file.append(metadata_reference_id.substring(6, 8));
		sb_rendered_file.append(File.separator);
		sb_rendered_file.append(metadata_reference_id.substring(8));
		sb_rendered_file.append(File.separator);
		sb_rendered_file.append(content.name);
		result.rendered_file = new File(sb_rendered_file.toString());
		
		if (result.rendered_file.exists() == false) {
			throw new FileNotFoundException("Can't found rendered file " + sb_rendered_file.toString());
		}
		
		if (result.rendered_file.length() != content.size) {
			throw new FileNotFoundException("Rendered file has not the expected size " + sb_rendered_file.toString());
		}
		
		if (check_hash) {
			result.rendered_digest = (String) content.hash;
			
			MessageDigest mdigest = null;
			try {
				mdigest = MessageDigest.getInstance(digest_algorithm);
			} catch (NoSuchAlgorithmException e) {
			}
			BufferedInputStream source_stream = new BufferedInputStream(new FileInputStream(result.rendered_file), 0xFFF);
			int len;
			byte[] buffer = new byte[0xFFF];
			while ((len = source_stream.read(buffer)) > 0) {
				mdigest.update(buffer, 0, len);
			}
			source_stream.close();
			String file_digest = MyDMAM.byteToString(mdigest.digest());
			
			if (file_digest.equalsIgnoreCase(result.rendered_digest) == false) {
				LinkedHashMap<String, Object> log = new LinkedHashMap<String, Object>();
				log.put("source", sb_rendered_file);
				log.put("source", file_digest);
				log.put("expected", content);
				log.put("expected", result.rendered_digest);
				Loggers.Metadata.error("Invalid " + digest_algorithm + " check, " + log);
				throw new FileNotFoundException("Rendered file has not the expected content " + sb_rendered_file.toString());
			}
		}
		result.rendered_mime = content.mime;
		result.consolidated = true;
		return result;
	}
	
	/**
	 * Test presence and validity for file.
	 */
	public static RenderedFile fromDatabaseMasterAsPreview(SourcePathIndexerElement sourcepathindexerelement, String mime_file) throws IOException {
		if (sourcepathindexerelement == null) {
			throw new NullPointerException("\"renderfromdatabase\" can't to be null");
		}
		
		RenderedFile result = new RenderedFile();
		result.rendered_file = Storage.getLocalFile(sourcepathindexerelement);
		
		if (result.rendered_file == null) {
			throw new NullPointerException("Find storage bridge in configuration");
		}
		if (result.rendered_file.exists() == false) {
			throw new FileNotFoundException("Can't found rendered file " + result.rendered_file.getPath());
		}
		if (result.rendered_file.length() != sourcepathindexerelement.size) {
			throw new FileNotFoundException("Original file has not the expected size " + result.rendered_file.getPath());
		}
		
		result.rendered_mime = mime_file;
		result.consolidated = true;
		return result;
	}
	
	public static synchronized void cleanCurrentTempDirectory() {
		if (Loggers.Metadata.isDebugEnabled() & commit_log_files.isEmpty() == false) {
			Loggers.Metadata.debug("Do a clean current temp directory for " + commit_log_files);
		}
		
		for (int pos = 0; pos < commit_log_files.size(); pos++) {
			commit_log_files.get(pos).delete();
		}
		commit_log_files.clear();
		
		if (Loggers.Metadata.isDebugEnabled()) {
			Loggers.Metadata.debug("Do a clean current temp directory for " + temp_directory);
		}
		
		temp_directory.delete();
	}
	
	public static void purge(String metadata_reference_id) {// TODO
		if (metadata_reference_id == null) {
			throw new NullPointerException("\"mtd_id\" can't to be null");
		}
		if (metadata_reference_id.equals("")) {
			return;
		}
		
		StringBuffer sb_base_directory_dest = new StringBuffer();
		sb_base_directory_dest.append(local_directory.getAbsolutePath());
		sb_base_directory_dest.append(File.separator);
		sb_base_directory_dest.append(metadata_reference_id.substring(0, 6));
		
		File base_dir_lvl1 = new File(sb_base_directory_dest.toString());
		if (base_dir_lvl1.exists() == false) {
			return;
		}
		
		sb_base_directory_dest.append(File.separator);
		sb_base_directory_dest.append(metadata_reference_id.substring(6, 8));
		
		File base_dir_lvl2 = new File(sb_base_directory_dest.toString());
		if (base_dir_lvl2.exists() == false) {
			return;
		}
		
		sb_base_directory_dest.append(File.separator);
		sb_base_directory_dest.append(metadata_reference_id.substring(8));
		
		File base_dir_lvl3 = new File(sb_base_directory_dest.toString());
		if (base_dir_lvl3.exists() == false) {
			return;
		}
		
		File[] deleteall = base_dir_lvl3.listFiles();
		for (int pos = 0; pos < deleteall.length; pos++) {
			deleteall[pos].delete();
		}
		base_dir_lvl3.delete();
		
		if (base_dir_lvl3.exists()) {
			Loggers.Metadata.warn("Can't delete directory: " + base_dir_lvl3);
		}
		
		if (base_dir_lvl2.list().length == 0) {
			base_dir_lvl2.delete();
			if (base_dir_lvl2.exists()) {
				Loggers.Metadata.warn("Can't delete directory: " + base_dir_lvl2);
			}
		}
		
		if (base_dir_lvl1.list().length == 0) {
			base_dir_lvl1.delete();
			if (base_dir_lvl1.exists()) {
				Loggers.Metadata.warn("Can't delete directory: " + base_dir_lvl1);
			}
		}
	}
	
	public static void purge_orphan_metadatas_files() throws Exception {
		File[] mtddir;
		File[] allrootelements;
		
		/**
		 * GC Temp directory
		 */
		if (temp_directory != null) {
			allrootelements = temp_directory.getCanonicalFile().getParentFile().listFiles();
			for (int pos = 0; pos < allrootelements.length; pos++) {
				if (allrootelements[pos].isDirectory() == false) {
					Loggers.Metadata.info("Element is not a directory, delete it, directory: " + allrootelements[pos]);
					allrootelements[pos].delete();
					continue;
				}
				mtddir = allrootelements[pos].listFiles();
				for (int pos_mtd = 0; pos_mtd < mtddir.length; pos_mtd++) {
					/**
					 * Purge hidden files.
					 */
					if (mtddir[pos_mtd].isHidden()) {
						Loggers.Metadata.info("Element is not a directory, delete it, directory: " + mtddir[pos_mtd]);
						mtddir[pos_mtd].delete();
					}
				}
				mtddir = allrootelements[pos].listFiles();
				if (mtddir.length == 0) {
					Loggers.Metadata.info("Directory is empty, delete it, directory: " + allrootelements[pos]);
					allrootelements[pos].delete();
				}
			}
		}
		
		/**
		 * GC Local directory
		 */
		if (local_directory == null) {
			return;
		}
		
		allrootelements = local_directory.getCanonicalFile().listFiles();
		File[] mtdfiles;
		File[] subrootfiles;
		String element_source_key;
		
		for (int pos = 0; pos < allrootelements.length; pos++) {
			/**
			 * Level 1
			 */
			if (allrootelements[pos].exists() == false) {
				continue;
			}
			if (allrootelements[pos].isDirectory() == false) {
				Loggers.Metadata.info("Element is not a directory, delete it, directory: " + allrootelements[pos]);
				allrootelements[pos].delete();
				continue;
			}
			
			subrootfiles = allrootelements[pos].listFiles();
			if (subrootfiles.length == 0) {
				/**
				 * Remove empty dir.
				 */
				Loggers.Metadata.info("Directory is empty delete it, rootelements: " + allrootelements[pos]);
				allrootelements[pos].delete();
				continue;
			}
			for (int pos_sroot = 0; pos_sroot < subrootfiles.length; pos_sroot++) {
				/**
				 * Level 2
				 */
				if (subrootfiles[pos_sroot].isDirectory() == false) {
					Loggers.Metadata.info("Element is not a directory, delete it, directory: " + subrootfiles[pos_sroot]);
					subrootfiles[pos_sroot].delete();
					continue;
				}
				// System.out.println(subrootfiles[pos_sroot]);// XXX
				mtddir = subrootfiles[pos_sroot].listFiles();
				if (mtddir.length == 0) {
					/**
					 * Remove empty dir.
					 */
					Loggers.Metadata.info("Directory is empty delete it, subrootfiles: " + subrootfiles[pos_sroot]);
					subrootfiles[pos_sroot].delete();
					continue;
				}
				for (int pos_mtd = 0; pos_mtd < mtddir.length; pos_mtd++) {
					/**
					 * Level 3
					 */
					if (subrootfiles[pos_sroot].exists() == false) {
						break;
					}
					if (mtddir[pos_mtd].exists() == false) {
						continue;
					}
					if (mtddir[pos_mtd].isDirectory() == false) {
						Loggers.Metadata.info("Element is not a directory, delete it, mtddir: " + mtddir[pos_mtd]);
						mtddir[pos_mtd].delete();
						continue;
					}
					element_source_key = allrootelements[pos].getName() + subrootfiles[pos_sroot].getName() + mtddir[pos_mtd].getName();
					
					Container container = ContainerOperations.getByMtdKeyForOnlyOneType(element_source_key, EntrySummary.type);
					if (container == null) {
						Loggers.Metadata.info("Delete all metadata references for directory, mtd key: " + element_source_key);
						purge(element_source_key);
						continue;
					}
					
					mtdfiles = mtddir[pos_mtd].listFiles(new FilenameFilter() {
						public boolean accept(File dir, String name) {
							if (name.toLowerCase().endsWith("." + digest_algorithm.toLowerCase())) return false;
							return true;
						}
					});
					
					if (mtdfiles == null) {
						continue;
					}
					
					ArrayList<String> elements_name = new ArrayList<String>();
					
					container = ContainerOperations.getByMtdKey(element_source_key);
					List<ContainerEntry> containerEntries = container.getEntries();
					EntryRenderer current_entry;
					/**
					 * Get all rendered files references from db
					 */
					for (int pos_entry = 0; pos_entry < containerEntries.size(); pos_entry++) {
						if ((containerEntries.get(pos_entry) instanceof EntryRenderer) == false) {
							continue;
						}
						current_entry = (EntryRenderer) containerEntries.get(pos_entry);
						elements_name.addAll(current_entry.getContentFileNames());
					}
					
					/**
					 * Search for each real rendered file, a presence in database.
					 */
					for (int pos_mtdf = 0; pos_mtdf < mtdfiles.length; pos_mtdf++) {
						boolean founded = false;
						for (int pos_dbf = 0; pos_dbf < elements_name.size(); pos_dbf++) {
							if (mtdfiles[pos_mtdf].getName().equals(elements_name.get(pos_dbf))) {
								founded = true;
								break;
							}
						}
						if (founded == false) {
							/**
							 * Delete old rendered file
							 */
							Loggers.Metadata.info("Delete old metadata file: " + mtdfiles[pos_mtdf]);
							mtdfiles[pos_mtdf].delete();
							/**
							 * Delete MD5 file
							 */
							(new File(mtdfiles[pos_mtdf].getPath() + "." + digest_algorithm.toLowerCase())).delete();
						}
					}
				}
			}
		}
		
	}
	
}
