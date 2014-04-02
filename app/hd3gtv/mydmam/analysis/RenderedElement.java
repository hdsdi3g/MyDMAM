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
import hd3gtv.log2.Log2Dumpable;
import hd3gtv.log2.LogHandlerToLogfile;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;

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
import java.util.Random;

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.eaio.uuid.UUID;

public class RenderedElement implements Log2Dumpable {
	
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
			
			if (Configuration.global.isElementExists("analysing_renderer") == false) {
				throw new NullPointerException("Can't found analysing_renderer element in configuration");
			}
			
			temp_directory = new File(Configuration.global.getValue("analysing_renderer", "temp_directory", (new File("/tmp")).getAbsolutePath()));
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
			
			local_directory = new File(Configuration.global.getValue("analysing_renderer", "local_directory", null));
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
	
	public static String getDigest_algorithm() {
		return digest_algorithm;
	}
	
	private Log2 commit_log;
	
	private String extension;
	private File rendered_file;
	private File temp_file;
	private String metadata_reference_id;
	private String rendered_base_file_name;
	private Renderer renderer;
	private boolean consolidated;
	private String rendered_mime;
	private String rendered_digest;
	
	/**
	 * Create a file like temp_directory/serviceinstancename/
	 * @param extension with or without a point
	 */
	public RenderedElement(String rendered_base_file_name, String extension) throws IOException {
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
		
		File commit_log_file = new File(temp_directory.getAbsolutePath() + File.separator + uuid.toString() + "-commit.log");
		commit_log_files.add(commit_log_file);
		commit_log = new Log2(new LogHandlerToLogfile(commit_log_file, 0xFFFFF, 100));
		
		temp_file = new File(sb.toString());
		if (temp_file.createNewFile()) {
			temp_file.delete();
		}
		
		Log2Dump dump = new Log2Dump();
		dump.add("rendered_base_file_name", rendered_base_file_name);
		dump.add("extension", extension);
		commit_log.info("Prepare temporary file", dump);
	}
	
	private RenderedElement() {
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
	
	private void checkConsolidate(SourcePathIndexerElement source_element, Renderer renderer) throws IOException {
		if (source_element == null) {
			throw new NullPointerException("\"source_element\" can't to be null");
		}
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
	 * metadata_reference_id[0-2]/metadata_reference_id[2-]/renderedbasefilename_RandomValue.extension
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
		dump.add("metadata_reference_id", metadata_reference_id);
		dump.add("source_element", source_element);
		dump.add("renderer name", renderer.getLongName());
		dump.add("rendered_file", rendered_file);
		dump.add("rendered_mime", rendered_mime);
		dump.add("rendered_digest", rendered_digest);
		Log2.log.debug("Consolidate rendered file", dump);
		
		consolidated = true;
		
		dump = new Log2Dump();
		commit_log.info("End consolidate", dump);
	}
	
	private File createBase_Directory_Dest() throws IOException {
		StringBuffer sb_base_directory_dest = new StringBuffer();
		sb_base_directory_dest.append(local_directory.getCanonicalPath());
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
		sb_rendered_file.append(extension);
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
		jo.put("producer", renderer.getLongName());
		jo.put("mime", rendered_mime);
		return jo;
	}
	
	public String getRendered_digest() {
		return rendered_digest;
	}
	
	public String getRendered_mime() {
		return rendered_mime;
	}
	
	/**
	 * Test presence and validity for file.
	 */
	static RenderedElement fromDatabase(JSONObject renderfromdatabase, String metadata_reference_id, boolean check_hash) throws IOException {
		if (renderfromdatabase == null) {
			throw new NullPointerException("\"renderfromdatabase\" can't to be null");
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
		
		RenderedElement result = new RenderedElement();
		
		StringBuffer sb_rendered_file = new StringBuffer();
		sb_rendered_file.append(local_directory.getCanonicalPath());
		sb_rendered_file.append(File.separator);
		sb_rendered_file.append(metadata_reference_id.substring(0, 6));
		sb_rendered_file.append(File.separator);
		sb_rendered_file.append(metadata_reference_id.substring(6));
		sb_rendered_file.append(File.separator);
		sb_rendered_file.append((String) renderfromdatabase.get("name"));
		result.rendered_file = new File(sb_rendered_file.toString());
		
		if (result.rendered_file.exists() == false) {
			throw new FileNotFoundException("Can't found rendered file " + sb_rendered_file.toString());
		}
		
		if (result.rendered_file.length() != (Long) renderfromdatabase.get("size")) {
			throw new FileNotFoundException("Rendered file has not the expected size " + sb_rendered_file.toString());
		}
		
		if (check_hash) {
			result.rendered_digest = (String) renderfromdatabase.get("hash");
			
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
				Log2Dump dump = new Log2Dump();
				dump.add("source", sb_rendered_file);
				dump.add("source", file_digest);
				dump.add("expected", renderfromdatabase.toJSONString());
				dump.add("expected", result.rendered_digest);
				Log2.log.error("Invalid " + digest_algorithm + " check", null, dump);
				throw new FileNotFoundException("Rendered file has not the expected content " + sb_rendered_file.toString());
			}
		}
		result.rendered_mime = (String) renderfromdatabase.get("mime");
		result.consolidated = true;
		return result;
	}
	
	/**
	 * Test presence and validity for file.
	 */
	static RenderedElement fromDatabaseMasterAsPreview(SourcePathIndexerElement sourcepathindexerelement, String mime_file) throws IOException {
		if (sourcepathindexerelement == null) {
			throw new NullPointerException("\"renderfromdatabase\" can't to be null");
		}
		
		RenderedElement result = new RenderedElement();
		result.rendered_file = Explorer.getLocalBridgedElement(sourcepathindexerelement);
		
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
	
	static synchronized void cleanCurrentTempDirectory() {
		for (int pos = 0; pos < commit_log_files.size(); pos++) {
			commit_log_files.get(pos).delete();
		}
		commit_log_files.clear();
		temp_directory.delete();
	}
	
	static void purge(String metadata_reference_id) {
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
		sb_base_directory_dest.append(metadata_reference_id.substring(6));
		
		File base_dir_lvl2 = new File(sb_base_directory_dest.toString());
		if (base_dir_lvl2.exists() == false) {
			return;
		}
		
		File[] deleteall = base_dir_lvl2.listFiles();
		for (int pos = 0; pos < deleteall.length; pos++) {
			deleteall[pos].delete();
		}
		base_dir_lvl2.delete();
		
		if (base_dir_lvl2.exists()) {
			Log2Dump dump = new Log2Dump();
			dump.add("directory", base_dir_lvl2);
			Log2.log.error("Can't delete", null, dump);
		}
		
		if (base_dir_lvl1.list().length == 0) {
			base_dir_lvl1.delete();
			if (base_dir_lvl1.exists()) {
				Log2Dump dump = new Log2Dump();
				dump.add("directory", base_dir_lvl1);
				Log2.log.error("Can't delete", null, dump);
			}
		}
	}
	
	static void gc(Client client) {
		if (client == null) {
			throw new NullPointerException("\"client\" can't to be null");
		}
		
		if (local_directory == null) {
			return;
		}
		
		GetResponse getresponse;
		File[] mtddir;
		File[] allrootelements = local_directory.listFiles();
		File[] mtdfiles;
		String element_source_key;
		
		for (int pos = 0; pos < allrootelements.length; pos++) {
			if (allrootelements[pos].exists() == false) {
				continue;
			}
			if (allrootelements[pos].isDirectory() == false) {
				Log2Dump dump = new Log2Dump();
				dump.add("rootelements", allrootelements[pos]);
				Log2.log.error("Element is not a directory, delete it", null, dump);
				allrootelements[pos].delete();
				continue;
			}
			mtddir = allrootelements[pos].listFiles();
			for (int pos_mtd = 0; pos_mtd < mtddir.length; pos_mtd++) {
				if (allrootelements[pos].exists() == false) {
					break;
				}
				if (mtddir[pos_mtd].exists() == false) {
					continue;
				}
				if (mtddir[pos_mtd].isDirectory() == false) {
					Log2Dump dump = new Log2Dump();
					dump.add("mtddir", mtddir[pos_mtd]);
					Log2.log.error("Element is not a directory, delete it", null, dump);
					allrootelements[pos].delete();
					continue;
				}
				element_source_key = allrootelements[pos].getName() + mtddir[pos_mtd].getName();
				getresponse = client.get(new GetRequest(MetadataCenter.ES_INDEX, MetadataCenter.ES_TYPE_SUMMARY, element_source_key)).actionGet();
				if (getresponse.isExists() == false) {
					RenderedElement.purge(element_source_key);
					continue;
				}
				
				try {
					mtdfiles = mtddir[pos_mtd].listFiles(new FilenameFilter() {
						public boolean accept(File dir, String name) {
							if (name.toLowerCase().endsWith("." + digest_algorithm.toLowerCase())) return false;
							return true;
						}
					});
					
					if (mtdfiles == null) {
						continue;
					}
					
					/**
					 * Search all rendered files for this mtd element_source_key
					 */
					SearchRequestBuilder request = client.prepareSearch();
					request.setIndices(MetadataCenter.ES_INDEX);
					
					BoolQueryBuilder query = QueryBuilders.boolQuery();
					query.must(QueryBuilders.termQuery("_id", element_source_key));
					query.must(QueryBuilders.termQuery(MetadataCenter.METADATA_PROVIDER_TYPE, Renderer.METADATA_PROVIDER_RENDERER));
					request.setQuery(query);// field
					
					SearchHit[] hits = request.execute().actionGet().getHits().hits();
					
					JSONParser parser = new JSONParser();
					JSONObject current_mtd;
					JSONArray currect_content;
					JSONObject content_rendered_element;
					
					ArrayList<String> elements_name = new ArrayList<String>();
					
					/**
					 * Get all rendered files references from db
					 */
					for (int pos_hits = 0; pos_hits < hits.length; pos_hits++) {
						parser.reset();
						current_mtd = (JSONObject) parser.parse(hits[pos_hits].getSourceAsString());
						if (current_mtd.containsKey(Renderer.METADATA_PROVIDER_RENDERER_CONTENT) == false) {
							continue;
						}
						currect_content = ((JSONArray) current_mtd.get(Renderer.METADATA_PROVIDER_RENDERER_CONTENT));
						
						for (int pos_content = 0; pos_content < currect_content.size(); pos_content++) {
							content_rendered_element = (JSONObject) currect_content.get(pos_content);
							elements_name.add((String) content_rendered_element.get("name"));
						}
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
							Log2.log.info("Delete old metadata file", new Log2Dump("file", mtdfiles[pos_mtdf]));
							mtdfiles[pos_mtdf].delete();
							/**
							 * Delete MD5 file
							 */
							(new File(mtdfiles[pos_mtdf].getPath() + "." + digest_algorithm.toLowerCase())).delete();
						}
					}
				} catch (IndexMissingException e) {
					continue;
				} catch (ParseException e) {
					Log2.log.error("Invalid ES response", e);
					continue;
				}
			}
		}
		
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("consolidated", consolidated);
		dump.add("renderer", renderer);
		dump.add("rendered_file", rendered_file);
		dump.add("rendered_mime", rendered_mime);
		dump.add("rendered_digest", rendered_digest);
		dump.add("rendered_base_file_name", rendered_base_file_name);
		dump.add("extension", extension);
		dump.add("metadata_reference_id", metadata_reference_id);
		dump.add("temp_file", temp_file);
		return dump;
	}
}
