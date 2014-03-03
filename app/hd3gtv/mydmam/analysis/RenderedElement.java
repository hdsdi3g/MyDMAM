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
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;

import org.json.simple.JSONObject;

import com.eaio.uuid.UUID;

public class RenderedElement {
	
	private static File temp_directory;
	private static File local_directory;
	private static volatile Random random;
	
	static {
		try {
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
		} catch (IOException e) {
			Log2.log.error("Can't init, check configuration analysing_renderer element", e);
		}
	}
	
	private String extention;
	private File rendered_file;
	private File temp_file;
	private String storageindexname;
	private String metadata_reference_id;
	private String rendered_base_file_name;
	private Renderer renderer;
	private boolean consolidated;
	private String rendered_mime;
	
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
		
		temp_file = new File(sb.toString());
		if (temp_file.createNewFile()) {
			temp_file.delete();
		}
	}
	
	public File getTempFile() {
		return temp_file;
	}
	
	private void checkConsolidate(String storageindexname, String metadata_reference_id, SourcePathIndexerElement source_element, Renderer renderer) throws IOException {
		this.storageindexname = storageindexname;
		if (storageindexname == null) {
			throw new NullPointerException("\"storageindexname\" can't to be null");
		}
		this.metadata_reference_id = metadata_reference_id;
		if (metadata_reference_id == null) {
			throw new NullPointerException("\"referenceid\" can't to be null");
		}
		if (source_element == null) {
			throw new NullPointerException("\"source_element\" can't to be null");
		}
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
	void consolidate(String storageindexname, String metadata_reference_id, SourcePathIndexerElement source_element, Renderer renderer) throws IOException {
		if (consolidated) {
			return;
		}
		checkConsolidate(storageindexname, metadata_reference_id, source_element, renderer);
		
		File f_base_directory_dest = createBase_Directory_Dest();
		
		rendered_file = nextRandomFile(f_base_directory_dest);
		while (rendered_file.exists()) {
			/**
			 * Search an available file name
			 */
			rendered_file = nextRandomFile(f_base_directory_dest);
		}
		
		if (temp_file.renameTo(rendered_file) == false) {
			// TODO move temp_file to rendered_file (copy + md5)
			throw new IOException("Can't move rendered_file: \"" + temp_file.getPath() + "\" to \"" + rendered_file.getPath() + "\"");
		}
		
		rendered_mime = MimeExtract.getMime(rendered_file);
		
		/*
		String storageindexname, String metadata_reference_id, SourcePathIndexerElement source_element, Renderer renderer
		*/
		
		Log2Dump dump = new Log2Dump();
		dump.add("storageindexname", storageindexname);
		dump.add("metadata_reference_id", metadata_reference_id);
		dump.add("source_element", source_element);
		dump.add("renderer name", renderer.getName());
		dump.add("rendered_file", rendered_file);
		dump.add("rendered_mime", rendered_mime);
		Log2.log.info("Consolidate rendered file", dump);
		
		// TODO commit log
		consolidated = true;
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
		sb_rendered_file.append(random.nextInt(1000));
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
		jo.put("hash", "000"); // TODO hash
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
			// TODO jo.get("hash", "000");
		}
		
		return rendered_file;
	}
	
	static void cleanCurrentTempDirectory() {
		temp_directory.delete();
	}
}
