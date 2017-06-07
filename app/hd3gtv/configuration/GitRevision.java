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

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.revwalk.RevCommit;

public class GitRevision {
	
	private RevCommit commit;
	
	GitRevision(RevCommit commit) {
		this.commit = commit;
		if (commit == null) {
			throw new NullPointerException("\"commit\" can't to be null");
		}
	}
	
	public String getFullMessage() {
		return commit.getFullMessage().trim();
	}
	
	public String toString() {
		return commit.getShortMessage();
	}
	
	public static final Pattern ONLY_SPACES = Pattern.compile(" ", Pattern.LITERAL);
	
	/**
	 * Search Strings chunks like "#13454"...
	 */
	public List<Integer> extractIssueIdFromCommitMessage() {
		String full_message = getFullMessage().replaceAll("\r", "").replaceAll("\n", " ");
		
		return ONLY_SPACES.splitAsStream(full_message).filter(chunk -> {
			return chunk.startsWith("#") & chunk.length() > 1;
		}).map(chunk -> {
			String corrected_ck = chunk.substring(1);
			if (corrected_ck.length() > 1) {
				if (StringUtils.isNumeric(corrected_ck.substring(corrected_ck.length() - 1)) == false) {
					/**
					 * If the last char is not a number, remove it.
					 * Example = "Task #123: correct some thing", "#123:", remome the first "#", and the last ":".
					 */
					corrected_ck = corrected_ck.substring(0, corrected_ck.length() - 1);
				}
			}
			return corrected_ck;
		}).filter(chunk -> {
			return StringUtils.isNumeric(chunk);
		}).map(chunk -> {
			return Integer.parseInt(chunk);
		}).collect(Collectors.toList());
	}
	
}
