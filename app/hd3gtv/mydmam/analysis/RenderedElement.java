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
package hd3gtv.mydmam.analysis;

import hd3gtv.configuration.Configuration;
import hd3gtv.javasimpleservice.ServiceManager;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.LogHandlerToLogfile;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Random;

import org.json.simple.JSONObject;

import com.eaio.uuid.UUID;

public class RenderedElement {
	
	private static File temp_directory;
	private static File local_directory;
	private static String digest_algorithm;
	private static volatile Random random;
	private static volatile ArrayList<File> commit_log_files;
	
	static {
		try {
			commit_log_files = new ArrayList<File>();
			
			digest_algorithm = "MD5";
			MessageDigest.getInstance(digest_algorithm);
			
			temp_directory = new File(Configuration.global.getValue("analysing_renderer", "temp_directory", (new File("")).getAbsolutePath()));
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
			sb.append(ServiceManager.getInstancename(false));
			sb.append("-");
			sb.append(ServiceManager.getInstancePID());
			temp_directory = new File(sb.toString());
			temp_directory.mkdirs();
			
			local_directory = new File(Configuration.global.getValue("analysing_renderer", "local_directory", (new File("")).getAbsolutePath()));
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
			Log2.log.error("Can't init, check configuration analysing_renderer element", e);
		}
		
	}
	
	private Log2 commit_log;
	
	private String extention;
	private File rendered_file;
	private File temp_file;
	private String storageindexname;
	private String metadata_reference_id;
	private String rendered_base_file_name;
	private Renderer renderer;
	private boolean consolidated;
	private String rendered_mime;
	private String rendered_digest;
	
	/**
	 * Create a file like temp_directory/serviceinstancename/
	 * @param extention with point. Not mandatory. Just appended to temp file name.
	 */
	public RenderedElement(String rendered_base_file_name, String extention) throws IOException {
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
		
		this.extention = "";
		if (extention != null) {
			sb.append(extention);
			this.extention = extention;
		}
		
		File commit_log_file = new File(temp_directory.getAbsolutePath() + File.separator + uuid.toString() + "-commit.log");
		commit_log_files.add(commit_log_file);
		commit_log = new Log2(new LogHandlerToLogfile(commit_log_file, 0xFFFFF, 100));
		
		temp_file = new File(sb.toString());
		if (temp_file.createNewFile()) {
			temp_file.delete();
		}
		
		Log2Dump dump = new Log2Dump();
		dump.add("rendered_base_file_name", rendered_base_file_name);
		dump.add("extention", extention);
		commit_log.info("Prepare temporary file", dump);
	}
	
	public File getTempFile() {
		return temp_file;
	}
	
	private void checkConsolidate(SourcePathIndexerElement source_element, Renderer renderer) throws IOException {
		if (source_element == null) {
			throw new NullPointerException("\"source_element\" can't to be null");
		}
		storageindexname = source_element.storagename;
		metadata_reference_id = MetadataCenterIndexer.getUniqueElementKey(source_element);
		
		this.renderer = renderer;
		if (renderer == null) {
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
	 * storageindexname/metadata_reference_id[0-2]/metadata_reference_id[2-]/renderedbasefilename_RandomValue.extention
	 */
	void consolidate(SourcePathIndexerElement source_element, Renderer renderer) throws IOException {
		if (consolidated) {
			return;
		}
		checkConsolidate(source_element, renderer);
		
		File f_base_directory_dest = createBase_Directory_Dest();
		
		rendered_file = nextRandomFile(f_base_directory_dest);
		while (rendered_file.exists()) {
			/**
			 * Search an available file name
			 */
			Log2Dump dump = new Log2Dump();
			dump.add("rendered_file", rendered_file);
			commit_log.info("Searching new temporary file name...", dump);
			
			rendered_file = nextRandomFile(f_base_directory_dest);
		}
		
		MessageDigest mdigest = null;
		try {
			mdigest = MessageDigest.getInstance(digest_algorithm);
		} catch (NoSuchAlgorithmException e) {
		}
		
		Log2Dump dump = new Log2Dump();
		dump.add("temp_file", temp_file);
		dump.add("rendered_file", rendered_file);
		commit_log.info("Prepare consolidate", dump);
		
		BufferedInputStream source_stream = new BufferedInputStream(new FileInputStream(temp_file), 0xFFF);
		OutputStream dest_stream = new FileOutputStream(rendered_file);
		int len;
		byte[] buffer = new byte[0xFFF];
		
		commit_log.info("Start copy");
		while ((len = source_stream.read(buffer)) > 0) {
			dest_stream.write(buffer, 0, len);
			mdigest.update(buffer, 0, len);
		}
		commit_log.info("End copy");
		dest_stream.close();
		source_stream.close();
		
		dump = new Log2Dump();
		dump.add("temp_file", temp_file);
		dump.add("ok", temp_file.delete());
		commit_log.info("Delete temp file", dump);
		
		rendered_digest = MyDMAM.byteToString(mdigest.digest());
		
		dump = new Log2Dump();
		dump.add("rendered_file", rendered_file);
		dump.add("rendered_digest", rendered_digest);
		commit_log.info("Digest", dump);
		
		FileWriter fw = new FileWriter(new File(rendered_file + "." + digest_algorithm.toLowerCase()));
		fw.write(rendered_digest + "\r\n");
		fw.close();
		commit_log.info("Write digest side-car");
		
		rendered_mime = MimeExtract.getMime(rendered_file);
		
		dump = new Log2Dump();
		dump.add("rendered_file", rendered_file);
		dump.add("rendered_mime", rendered_mime);
		commit_log.info("Mime", dump);
		
		dump = new Log2Dump();
		dump.add("storageindexname", storageindexname);
		dump.add("metadata_reference_id", metadata_reference_id);
		dump.add("source_element", source_element);
		dump.add("renderer name", renderer.getName());
		dump.add("rendered_file", rendered_file);
		dump.add("rendered_mime", rendered_mime);
		dump.add("rendered_digest", rendered_digest);
		Log2.log.info("Consolidate rendered file", dump);
		
		consolidated = true;
		
		dump = new Log2Dump();
		commit_log.info("End consolidate", dump);
	}
	
	private File createBase_Directory_Dest() throws IOException {
		StringBuffer sb_base_directory_dest = new StringBuffer();
		sb_base_directory_dest.append(local_directory.getCanonicalPath());
		sb_base_directory_dest.append(File.separator);
		sb_base_directory_dest.append(storageindexname);
		sb_base_directory_dest.append(File.separator);
		sb_base_directory_dest.append(metadata_reference_id.substring(0, 6));
		sb_base_directory_dest.append(File.separator);
		sb_base_directory_dest.append(metadata_reference_id.substring(6));
		String base_directory_dest = sb_base_directory_dest.toString();
		
		File f_base_directory_dest = new File(base_directory_dest);
		f_base_directory_dest.mkdirs();
		if (f_base_directory_dest.exists() == false) {
			throw new IOException("Can't create dest directory: " + sb_base_directory_dest.toString());
		}
		return f_base_directory_dest;
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
		sb_rendered_file.append(extention);
		return new File(sb_rendered_file.toString());
	}
	
	boolean isConsolidated() {
		return consolidated;
	}
	
	/**
	 * @return null if not rendered.
	 */
	public File getRendered_file() {
		return rendered_file;
	}
	
	/**
	 * Don't forget to consolidate before.
	 */
	JSONObject toDatabase() {
		if (consolidated == false) {
			throw new NullPointerException("Element is not consolidated !");
		}
		JSONObject jo = new JSONObject();
		jo.put("name", rendered_file.getName());
		jo.put("size", rendered_file.length());
		jo.put("date", rendered_file.lastModified());
		jo.put("hash", rendered_digest);
		jo.put("producer", renderer.getName());
		jo.put("mime", rendered_mime);
		return jo;
	}
	
	/**
	 * Test presence and validity for file.
	 */
	public static File fromDatabase(JSONObject renderfromdatabase, String storageindexname, String metadata_reference_id, boolean check_hash) throws IOException {
		if (renderfromdatabase == null) {
			throw new NullPointerException("\"renderfromdatabase\" can't to be null");
		}
		if (storageindexname == null) {
			throw new NullPointerException("\"storageindexname\" can't to be null");
		}
		if (metadata_reference_id == null) {
			throw new NullPointerException("\"metadata_reference_id\" can't to be null");
		}
		
		StringBuffer sb_rendered_file = new StringBuffer();
		sb_rendered_file.append(local_directory.getCanonicalPath());
		sb_rendered_file.append(File.separator);
		sb_rendered_file.append(storageindexname);
		sb_rendered_file.append(File.separator);
		sb_rendered_file.append(metadata_reference_id.substring(0, 6));
		sb_rendered_file.append(File.separator);
		sb_rendered_file.append(metadata_reference_id.substring(6));
		sb_rendered_file.append(File.separator);
		sb_rendered_file.append((String) renderfromdatabase.get("name"));
		File rendered_file = new File(sb_rendered_file.toString());
		
		if (rendered_file.exists() == false) {
			throw new FileNotFoundException("Can't found rendered file " + sb_rendered_file.toString());
		}
		
		if (rendered_file.length() != (Integer) renderfromdatabase.get("size")) {
			throw new FileNotFoundException("Rendered file has not the expected size " + sb_rendered_file.toString());
		}
		
		if (check_hash) {
			String db_digest = (String) renderfromdatabase.get("hash");
			
			MessageDigest mdigest = null;
			try {
				mdigest = MessageDigest.getInstance(digest_algorithm);
			} catch (NoSuchAlgorithmException e) {
			}
			BufferedInputStream source_stream = new BufferedInputStream(new FileInputStream(rendered_file), 0xFFF);
			int len;
			byte[] buffer = new byte[0xFFF];
			while ((len = source_stream.read(buffer)) > 0) {
				mdigest.update(buffer, 0, len);
			}
			source_stream.close();
			String file_digest = MyDMAM.byteToString(mdigest.digest());
			
			if (file_digest.equalsIgnoreCase(db_digest) == false) {
				Log2Dump dump = new Log2Dump();
				dump.add("source", sb_rendered_file);
				dump.add("source", file_digest);
				dump.add("expected", renderfromdatabase.toJSONString());
				dump.add("expected", db_digest);
				Log2.log.error("Invalid " + digest_algorithm + " check", null, dump);
				throw new FileNotFoundException("Rendered file has not the expected content " + sb_rendered_file.toString());
			}
		}
		
		return rendered_file;
	}
	
	static synchronized void cleanCurrentTempDirectory() {
		for (int pos = 0; pos < commit_log_files.size(); pos++) {
			commit_log_files.get(pos).delete();
		}
		commit_log_files.clear();
		temp_directory.delete();
	}
}
