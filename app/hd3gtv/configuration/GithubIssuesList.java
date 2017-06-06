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
package hd3gtv.configuration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;

import org.apache.commons.io.FileUtils;

import com.google.gson.JsonSyntaxException;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonKit;

public class GithubIssuesList {
	
	private LinkedHashMap<Integer, GithubIssue> issues;
	private final File issues_cache_file;
	private int initial_size;
	
	public GithubIssuesList() throws JsonSyntaxException, IOException {
		issues_cache_file = new File(MyDMAM.APP_ROOT_PLAY_CONF_DIRECTORY.getAbsolutePath() + File.separator + "github_issues.json");
		
		if (issues_cache_file.exists()) {
			issues = MyDMAM.gson_kit.getGsonSimple().fromJson(FileUtils.readFileToString(issues_cache_file, MyDMAM.UTF8), GsonKit.type_LinkedHashMap_Integer_GithubIssue);
		} else {
			issues = new LinkedHashMap<>();
		}
		
		initial_size = issues.size();
	}
	
	public void save() throws IOException {
		if (initial_size == issues.size()) {
			return;
		}
		FileUtils.writeStringToFile(issues_cache_file, MyDMAM.gson_kit.getGsonSimple().toJson(issues), MyDMAM.UTF8);
		initial_size = issues.size();
	}
	
	/**
	 * @return null if can't found id from cache or from Github web site
	 */
	public GithubIssue getById(Integer id) {
		return issues.computeIfAbsent(id, g_id -> {
			return GithubIssue.getIssue(g_id);
		});
	}
	
}
