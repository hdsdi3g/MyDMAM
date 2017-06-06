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

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import hd3gtv.mydmam.MyDMAM;

/**
 * @see https://developer.github.com/v3/issues/#get-a-single-issue
 */
public class GithubIssue {
	
	private static final Logger log = Logger.getLogger(GithubIssue.class);
	
	public static final String GITHUB_API_HOST = "api.github.com";
	
	/**
	 * https://github.com/octocat/Hello-World/issues/1347
	 */
	String html_url;
	
	/**
	 * 1347
	 */
	int number;
	
	/**
	 * open
	 */
	String state;
	
	/**
	 * Found a bug
	 */
	String title;
	
	/**
	 * 2011-04-22T13:33:48Z
	 */
	String closed_at;
	
	/**
	 * 2011-04-22T13:33:48Z
	 */
	String created_at;
	
	/**
	 * 2011-04-22T13:33:48Z
	 */
	String updated_at;
	
	User user;
	
	class User {
		/**
		 * octocat
		 */
		String login;
		
		/**
		 * 1347
		 */
		int number;
		
		/**
		 * https://github.com/images/error/octocat_happy.gif
		 */
		String avatar_url;
		
		/**
		 * https://github.com/octocat
		 */
		String html_url;
		
		/**
		 * User
		 */
		String type;
	}
	
	ArrayList<Label> labels;
	
	class Label {
		/**
		 * 208045946
		 */
		int id;
		
		/**
		 * bug
		 */
		String name;
		
		/**
		 * f29513
		 */
		String color;
	}
	
	/**
	 * For Gson
	 */
	private GithubIssue() {
	}
	
	static GithubIssue getIssue(Integer id) {
		GithubIssue result = null;
		
		String owner = Configuration.global.getValue("mydmam_github_repository", "owner", "hdsdi3g");
		String repo = Configuration.global.getValue("mydmam_github_repository", "repo", "MyDMAM");
		
		HttpURLConnection connection = null;
		try {
			String utf8 = MyDMAM.UTF8.name();
			URL url = new URL("https", GITHUB_API_HOST, 443, "/repos/" + URLEncoder.encode(owner, utf8) + "/" + URLEncoder.encode(repo, utf8) + "/issues/" + id);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "application/vnd.github.v3.raw+json");
			int status = connection.getResponseCode();
			
			if (status != 200) {
				throw new IOException("GitHub responds " + status + " for " + url.toString());
			}
			
			InputStreamReader isr = new InputStreamReader(connection.getInputStream());
			result = MyDMAM.gson_kit.getGson().fromJson(isr, GithubIssue.class);
			IOUtils.closeQuietly(isr);
		} catch (IOException e) {
			log.error("Can't get valid response from GitHub with issue " + id, e);
		} finally {
			if (connection != null) {
				try {
					connection.disconnect();
				} catch (Exception e2) {
					log.warn("Can't disconnect correctly from GitHub", e2);
				}
			}
		}
		
		return result;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(title);
		sb.append(" (#");
		sb.append(number);
		
		sb.append(") is ");
		sb.append(labels());
		
		sb.append(" by ");
		sb.append(user.login);
		return sb.toString();
	}
	
	public String labels() {
		return labels.stream().map(label -> {
			return label.name;
		}).collect(Collectors.joining(", "));
	}
	
	public boolean isBug() {
		return labels.stream().anyMatch(label -> {
			return label.name.equalsIgnoreCase("bug");
		});
	}
	
}
