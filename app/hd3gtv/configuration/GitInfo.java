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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import hd3gtv.mydmam.Loggers;

public class GitInfo {
	
	private String branch = "unknown";
	private String commit = "00000000";
	
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
			File git_dir = new File(".git");
			if (git_dir.exists()) {
				return new GitInfo(git_dir);
			}
			
			String[] classpathelementsstr = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
			File cp_element;
			for (int i = 0; i < classpathelementsstr.length; i++) {
				if (classpathelementsstr[i].endsWith(".jar")) {
					continue;
				}
				cp_element = new File(classpathelementsstr[i]);
				if (cp_element.isDirectory() == false) {
					continue;
				}
				git_dir = new File(cp_element.getCanonicalPath() + File.separator + ".git");
				if (git_dir.exists()) {
					return new GitInfo(git_dir);
				} else if (cp_element.getCanonicalPath().endsWith(File.separator + "conf")) {
					git_dir = new File(cp_element.getCanonicalFile().getParentFile().getPath() + File.separator + ".git");
					if (git_dir.exists()) {
						return new GitInfo(git_dir);
					}
				}
			}
			throw new FileNotFoundException();
		} catch (IOException e) {
			Loggers.Manager.error("Can't access to local code git repository", e);
			return null;
		}
	}
}
