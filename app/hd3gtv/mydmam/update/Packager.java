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
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.tools.CopyMove;

public class Packager {
	
	final File local_package_dir = new File(MyDMAM.APP_ROOT_PLAY_DIRECTORY.getAbsolutePath() + File.separator + "pack" + File.separator + "upgrade");
	final File build_dir = new File(MyDMAM.APP_ROOT_PLAY_DIRECTORY.getAbsolutePath() + File.separator + "build");
	
	/**
	 * From build directory
	 */
	private final Collection<String> ignore_filesdirs = Collections.unmodifiableCollection(Arrays.asList("jre", "conf/app.d", "conf/application.conf", "conf/dependencies.yml", "conf/log4j.xml"));
	// TODO ignore DS_Store, desktop.ini ...
	
	public Packager() throws IOException {
		FileUtils.forceMkdir(local_package_dir);
		FileUtils.forceMkdir(build_dir);
	}
	
	/**
	 * @return like /opt/mydmam/pack/upgrade
	 */
	File getLocalPackageDir() {
		return local_package_dir;
	}
	
	private List<File> ignoreFilesDirs() {
		return ignore_filesdirs.stream().map(item -> {
			return (new File(build_dir + File.separator + item.replace("/", File.separator))).getAbsoluteFile();
		}).collect(Collectors.toList());
	}
	
	public void importCurrentDirectory() throws Exception {
		List<File> ignore_filesdirs = ignoreFilesDirs();
		
		PackManifest manifest = new PackManifest("SHA-256");
		
		ByteBuffer byte_buffer = ByteBuffer.allocate(1024 * 1024);
		
		recursiveSearch(manifest, build_dir, byte_buffer, ignore_filesdirs);
		
		File manifest_file = new File(manifest.getWorkingDir(this) + File.separator + "manifest.json");
		FileUtils.write(manifest_file, MyDMAM.gson_kit.getGsonPretty().toJson(manifest), "UTF-8", false);
	}
	
	private void recursiveSearch(PackManifest manifest, File start_from, ByteBuffer byte_buffer, List<File> ignore_filesdirs) {
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
				return new PackFile(manifest, file, this, byte_buffer);
			} catch (Exception e) {
				throw new RuntimeException("Can't create PackFile with " + file.getAbsolutePath(), e);
			}
		}).collect(Collectors.toList()));
		
		founded.stream().filter(item -> {
			return item.isDirectory();
		}).forEach(dir -> {
			recursiveSearch(manifest, dir, byte_buffer, ignore_filesdirs);
		});
	}
	
}
