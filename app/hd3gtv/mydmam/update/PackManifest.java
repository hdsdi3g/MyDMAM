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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import hd3gtv.configuration.GitInfo;

class PackManifest {
	
	long creation_date;
	String version;
	String maintainer;
	long total_size;
	String digest_type;
	ArrayList<PackFile> files;
	
	PackManifest(String digest_type) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance(digest_type);
		
		this.creation_date = System.currentTimeMillis();
		this.files = new ArrayList<>();
		this.version = GitInfo.getFromRoot().getActualRepositoryInformation(); // TODO bad form: "aa bbb"
		this.maintainer = null; // TODO get last git user commit
		this.total_size = 0l;
		this.digest_type = digest_type;
	}
	
	/**
	 * Only for Gson
	 */
	PackManifest() {
	}
	
	File getWorkingDir(Packager packager) throws IOException {
		if (packager == null) {
			throw new NullPointerException("\"packager\" can't to be null");
		}
		File wd = new File(packager.getLocalPackageDir().getAbsolutePath() + File.separator + version);
		FileUtils.forceMkdir(wd);
		return wd;
	}
	
	void addToManifest(List<PackFile> pfile) {
		total_size += pfile.stream().collect(Collectors.summingLong(f -> {
			return f.size;
		}));
		files.addAll(pfile);
	}
	
}
