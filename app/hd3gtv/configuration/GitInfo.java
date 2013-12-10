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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.configuration;

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;

import java.io.File;
import java.io.FileNotFoundException;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

public class GitInfo {
	
	public static String cur_branch = "Unknown";
	public static String cur_commit = "repository";
	
	static {
		File file_repository = new File(".git");
		try {
			FileRepositoryBuilder builder = new FileRepositoryBuilder();
			Repository repository = builder.setGitDir(file_repository).readEnvironment().findGitDir().build();
			if (repository.getBranch() == null) {
				throw new FileNotFoundException();
			}
			cur_branch = repository.getBranch();
			cur_commit = repository.getRef(Constants.HEAD).getObjectId().abbreviate(8).name();
		} catch (Exception e) {
			Log2.log.error("Can't access to local code git repository", e, new Log2Dump("file-expected", file_repository.getAbsoluteFile()));
		}
	}
	
	public static String getActualRepositoryInformation() {
		return cur_branch + " " + cur_commit;
	}
}
