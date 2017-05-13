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
package hd3gtv.mydmam.update;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;

import com.google.gson.JsonSyntaxException;

import hd3gtv.configuration.GitInfo;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;

class PackManifest {
	
	long creation_date;
	String version;
	String maintainer;
	long total_size;
	String digest_type;
	String reference;
	long archive_size;
	String archive_digest;
	ArrayList<PackFile> files;
	
	private transient File manifest_file;
	private transient File archive_file;
	private transient File working_dir;
	private transient Packager packager;
	private transient ZipOutputStream zip_package;
	
	static final String MANIFEST_FILE_NAME = "manifest.json";
	static final String ZIP_FILE_NAME = "archive.zip";
	
	PackManifest(Packager packager) throws IOException {
		this.packager = packager;
		if (packager == null) {
			throw new NullPointerException("\"packager\" can't to be null");
		}
		
		creation_date = System.currentTimeMillis();
		files = new ArrayList<>();
		
		version = FileUtils.readFileToString(new File(packager.build_dir.getAbsolutePath() + File.separator + GitInfo.VERSION_FILE), MyDMAM.UTF8).trim();
		maintainer = GitInfo.getFromRoot().getCurrentGitUser();
		total_size = 0l;
		digest_type = packager.getDigestType();
		reference = UUID.randomUUID().toString();
		
		working_dir = new File(packager.getLocalPackageDir().getAbsolutePath() + File.separator + version);
		FileUtils.forceMkdir(working_dir);
		
		manifest_file = new File(working_dir + File.separator + MANIFEST_FILE_NAME);
		manifest_file = new File(working_dir + File.separator + MANIFEST_FILE_NAME);
		archive_file = new File(working_dir + File.separator + ZIP_FILE_NAME);
		
		zip_package = new ZipOutputStream(new FileOutputStream(archive_file));
		zip_package.setLevel(3);
		zip_package.setComment("MyDMAM-Upgrade:" + reference);
	}
	
	/**
	 * @return null or not empty list
	 */
	List<PackFile> checkManifestWithActualVersion(Packager packager) throws IOException {
		if (packager.getDigestType().equalsIgnoreCase(digest_type) == false) {
			throw new IOException("Invalid digest type in manifest: " + digest_type);
		}
		
		File f_version = new File(MyDMAM.APP_ROOT_PLAY_DIRECTORY + File.separator + GitInfo.VERSION_FILE);
		if (f_version.exists()) {
			String current_version = FileUtils.readFileToString(f_version, MyDMAM.UTF8).trim();
			if (current_version.equalsIgnoreCase(version)) {
				Loggers.Update.info("Same version between actual an update package (" + current_version + "). Nothing to do.");
				return null;
			}
		} else {
			Loggers.Update.info("MyDMAM is actually run from a dev version. Don't use update tools for upgrading, but use git.");
			return null;
		}
		
		Predicate<PackFile> chechAndValidFile = m_file -> {
			File real_file = m_file.getLocalFile(MyDMAM.APP_ROOT_PLAY_DIRECTORY);
			if (real_file.exists()) {
				if (m_file.size != real_file.length()) {
					return true;
				}
				String real_digest;
				try {
					real_digest = packager.computeDigest(real_file);
				} catch (IOException e) {
					throw new RuntimeException("Can't open file " + real_file.getAbsolutePath(), e);
				}
				return real_digest.equalsIgnoreCase(m_file.digest) == false;
			}
			return true;
		};
		
		List<PackFile> to_change = files.stream().filter(chechAndValidFile).collect(Collectors.toList());
		if (to_change.isEmpty()) {
			Loggers.Update.warn("Can't found some file to change with this version !");
			return null;
		}
		
		return to_change;
	}
	
	/**
	 * Only for Gson
	 */
	protected PackManifest() {
	}
	
	File getWorkingDir() throws IOException {
		return working_dir;
	}
	
	ZipOutputStream getOutputZipPackage() {
		return zip_package;
	}
	
	void addToManifest(List<PackFile> pfile) {
		total_size += pfile.stream().collect(Collectors.summingLong(f -> {
			return f.size;
		}));
		files.addAll(pfile);
	}
	
	void close() throws IOException {
		zip_package.flush();
		zip_package.finish();
		zip_package.close();
		
		archive_size = archive_file.length();
		archive_digest = packager.computeDigest(archive_file);
		
		FileUtils.write(manifest_file, MyDMAM.gson_kit.getGsonPretty().toJson(this), "UTF-8", false);
		
		FileUtils.write(new File(manifest_file.getAbsolutePath() + "." + packager.getDigestType()), packager.computeDigest(manifest_file) + "\r\n", MyDMAM.UTF8, false);
	}
	
	/**
	 * With json digest check
	 */
	static PackManifest openFromJson(Packager packager, File manifest_file) throws JsonSyntaxException, IOException {
		String expected_json_digest = FileUtils.readFileToString(new File(manifest_file.getAbsolutePath() + "." + packager.getDigestType()), MyDMAM.UTF8).trim();
		String real_json_digest = packager.computeDigest(manifest_file);
		
		if (real_json_digest.equalsIgnoreCase(expected_json_digest) == false) {
			throw new IOException("Invalid digest for " + manifest_file.getPath() + " (" + real_json_digest + ")");
		}
		
		return MyDMAM.gson_kit.getGsonSimple().fromJson(FileUtils.readFileToString(manifest_file, MyDMAM.UTF8), PackManifest.class);
	}
	
	String validStoredName(String file_name) {
		if (file_name == null) {
			throw new NullPointerException("\"file_name\" can't to be null");
		}
		if (file_name.isEmpty()) {
			throw new NullPointerException("\"file_name\" can't to be empty");
		}
		
		if (file_name.startsWith(MANIFEST_FILE_NAME)) {
			return validStoredName("_" + file_name);
		}
		
		if (files.stream().map(file -> {
			return file.stored_name;
		}).anyMatch(stored_name -> {
			return stored_name.equals(file_name);
		})) {
			return validStoredName(file_name + "_");
		}
		
		return file_name;
	}
	
	/*
	 * 	read zip	ZipInputStream request_zip = new ZipInputStream(dis);
		
		ZipEntry entry;
		while ((entry = request_zip.getNextEntry()) != null) {
			entries.add(new RequestEntry(entry, request_zip));
		}
		request_zip.close();
	*/
	
}
