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
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.google.gson.JsonSyntaxException;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.storage.IgnoreFiles;
import hd3gtv.tools.CopyMove;

public class Packager {
	
	final File local_package_dir = new File(MyDMAM.APP_ROOT_PLAY_DIRECTORY.getAbsolutePath() + File.separator + "pack" + File.separator + "upgrade");
	final File build_dir = new File(MyDMAM.APP_ROOT_PLAY_DIRECTORY.getAbsolutePath() + File.separator + "build");
	final Collection<File> ignore_filesdirs = Collections.unmodifiableCollection(Arrays.asList("jre", "conf/app.d", "conf/application.conf", "conf/dependencies.yml", "conf/log4j.xml").stream().map(item -> {
		return (new File(build_dir + File.separator + item.replace("/", File.separator))).getAbsoluteFile();
	}).collect(Collectors.toList()));
	
	private String digest_type = "SHA-256";
	transient final byte[] buffer = new byte[1024 * 1024];
	
	/**
	 * From build directory
	 */
	public Packager() throws IOException, NoSuchAlgorithmException {
		FileUtils.forceMkdir(local_package_dir);
		FileUtils.forceMkdir(build_dir);
		MessageDigest.getInstance(digest_type);
	}
	
	/**
	 * @return like /opt/mydmam/pack/upgrade
	 */
	File getLocalPackageDir() {
		return local_package_dir;
	}
	
	public String getDigestType() {
		return digest_type;
	}
	
	public void importCurrentDirectory() throws Exception {
		PackManifest manifest = new PackManifest(this);
		// TODO manage actual manifest in conf
		recursiveSearch(manifest, build_dir);
		manifest.close();
		// TODO overwrite the new manifest in conf
		// TODO overwrite the new manifest in build/conf
		
		// TODO update build.xml >> "build" target should: make, move, call java/CLI/importCurrentDirectory. And only after pack.
	}
	
	private void recursiveSearch(PackManifest manifest, File start_from) {
		try {
			CopyMove.checkExistsCanRead(start_from);
			CopyMove.checkIsDirectory(start_from);
		} catch (Exception e) {
			throw new RuntimeException("Can't walk into directory " + start_from, e);
		}
		
		List<File> founded = Arrays.asList(start_from.listFiles((file) -> {
			try {
				if (file.canRead() == false) {
					return false;
				} else if (ignore_filesdirs.contains(file)) {
					return false;
				} else if (IgnoreFiles.directory_config_list.isFileNameIsAllowed(file.getName()) == false) {
					return false;
				} else if (FileUtils.isSymlink(file)) {
					return false;
				}
				return true;
			} catch (Exception e) {
				throw new RuntimeException("Can't walk into directory " + file.getAbsolutePath(), e);
			}
		}));
		
		manifest.addToManifest(founded.stream().filter(item -> {
			return item.isFile();
		}).map(file -> {
			try {
				return new PackFile(manifest, file, this);
			} catch (Exception e) {
				throw new RuntimeException("Can't create PackFile with " + file.getAbsolutePath(), e);
			}
		}).collect(Collectors.toList()));
		
		founded.stream().filter(item -> {
			return item.isDirectory();
		}).forEach(dir -> {
			recursiveSearch(manifest, dir);
		});
	}
	
	public void importPackage(File json_manifest) throws JsonSyntaxException, IOException {
		PackManifest manifest = PackManifest.openFromJson(this, json_manifest);
		List<PackFile> to_change = manifest.checkManifestWithActualVersion(this);
		
		System.out.println(MyDMAM.gson_kit.getGsonPretty().toJson(to_change));
		// TODO for debug, work only in a /build dir
		// TODO extract, or not, some files
		// TODO create upgrade list
	}
	
	String computeDigest(File file_to_do) throws IOException {
		FileInputStream fis = null;
		try {
			MessageDigest md = MessageDigest.getInstance(digest_type);
			fis = new FileInputStream(file_to_do);
			
			int len;
			while ((len = fis.read(buffer, 0, buffer.length)) != -1) {
				md.update(buffer, 0, len);
			}
			fis.close();
			
			return MyDMAM.byteToString(md.digest());
		} catch (NoSuchAlgorithmException e) {
			return null;
		} catch (IOException e) {
			if (fis != null) {
				IOUtils.closeQuietly(fis);
			}
			throw e;
		}
	}
	
}
