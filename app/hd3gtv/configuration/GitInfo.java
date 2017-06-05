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
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;

public class GitInfo implements IGitInfo {
	
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
	
	public String getActualRepositoryInformation() {
		return branch + " " + commit;
	}
	
	private static class NoGit implements IGitInfo {
		
		public String getActualRepositoryInformation() {
			return "unknown";
		}
	}
	
	private static class Release implements IGitInfo {
		
		private String content;
		
		/**
		 * @param version => v0.18-78-g316c9be (from git describe --always)
		 */
		public Release(File version) throws IOException {
			content = FileUtils.readFileToString(version, MyDMAM.UTF8).trim();
		}
		
		public String getActualRepositoryInformation() {
			return content;
		}
	}
	
	private static IGitInfo git;
	
	public static IGitInfo getFromRoot() {
		if (git == null) {
			File _git_dir = new File(".git");
			if (_git_dir.exists()) {
				try {
					git = new GitInfo(_git_dir);
				} catch (IOException e) {
					git = new NoGit();
					Loggers.Manager.error("Can't load git repository in " + _git_dir.getAbsolutePath(), e);
				}
			} else {
				Optional<GitInfo> o_git = MyDMAM.factory.getClasspathOnlyDirectories().map(cp -> {
					try {
						File git_dir = new File(cp.getPath() + File.separator + ".git");
						if (git_dir.exists()) {
							return new GitInfo(git_dir);
						} else if (cp.getPath().endsWith(File.separator + "conf")) {
							git_dir = new File(cp.getParent() + File.separator + ".git");
							if (git_dir.exists()) {
								return new GitInfo(git_dir);
							}
						}
					} catch (IOException e) {
						Loggers.Manager.error("Can't access to classpath dir " + cp.getPath(), e);
					}
					return null;
				}).filter(cp -> {
					return cp != null;
				}).findFirst();
				
				if (o_git.isPresent()) {
					git = o_git.get();
				} else {
					File version = new File(MyDMAM.APP_ROOT_PLAY_DIRECTORY + File.separator + "version");
					if (version.exists()) {
						try {
							git = new Release(version);
						} catch (IOException e) {
							Loggers.Manager.warn("Can't open " + version + " file", e);
							git = new NoGit();
						}
					} else {
						Loggers.Manager.debug("Can't found git repository");
						git = new NoGit();
					}
				}
			}
		}
		return git;
	}
}
