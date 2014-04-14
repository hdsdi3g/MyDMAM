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
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
*/
package hd3gtv.configuration;

import hd3gtv.log2.Log2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

public class GitInfo {
	
	private String branch = "unknown";
	private String commit = "";
	
	public GitInfo(File repository_file) throws IOException {
		if (repository_file == null) {
			throw new NullPointerException("\"repository_file\" can't to be null");
		}
		if (repository_file.exists() == false) {
			throw new IOException("Can't found \"" + repository_file + "\"");
		}
		if (repository_file.isDirectory() == false) {
			throw new IOException("\"" + repository_file + "\" is not a directory");
		}
		
		try {
			FileRepositoryBuilder builder = new FileRepositoryBuilder();
			Repository repository = builder.setGitDir(repository_file).readEnvironment().findGitDir().build();
			if (repository.getBranch() == null) {
				throw new FileNotFoundException("Can't found branch in \"" + repository_file + "\"");
			}
			branch = repository.getBranch();
			commit = repository.getRef(Constants.HEAD).getObjectId().abbreviate(8).name();
		} catch (Exception e) {
			throw new IOException("Can't load git repository \"" + repository_file + "\"");
		}
	}
	
	public String getBranch() {
		return branch;
	}
	
	public String getCommit() {
		return commit;
	}
	
	public String getActualRepositoryInformation() {
		return branch + " " + commit;
	}
	
	public static GitInfo getFromRoot() {
		try {
			return new GitInfo(new File(".git"));
		} catch (IOException e) {
			Log2.log.error("Can't access to local code git repository", e);
			return null;
		}
	}
	
}
