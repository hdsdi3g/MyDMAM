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
import java.io.PrintStream;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import groovy.text.SimpleTemplateEngine;
import groovy.text.Template;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.cli.CLIDefinition;
import hd3gtv.tools.ApplicationArgs;

public class CLIProject implements CLIDefinition {
	
	public String getCliModuleName() {
		return "project";
	}
	
	public String getCliModuleShortDescr() {
		return "Tools for MyDMAM Project";
	}
	
	public void execCliModule(ApplicationArgs args) throws Exception {
		if (args.getParamExist("-githistory")) {
			IGitInfo git = GitInfo.getFromRoot();
			
			String from = args.getSimpleParamValue("-githistory");
			if (from == null) {
				from = git.getLastTag();
				System.out.println(" == Get history from tag " + from + " ==");
			}
			
			GithubIssuesList issues_list = new GithubIssuesList();
			
			List<GithubIssue> current_issues = git.getRevisionsFrom(from).stream().map(r -> {
				return r.extractIssueIdFromCommitMessage();
			}).flatMap(a -> {
				return a.stream();
			}).distinct().map(issueid -> {
				return issues_list.getById(issueid);
			}).collect(Collectors.toList());
			issues_list.save();
			
			if (current_issues.isEmpty()) {
				return;
			}
			
			SimpleTemplateEngine template_engine = new SimpleTemplateEngine();
			template_engine.setVerbose(false);
			Template template = template_engine.createTemplate(new File(MyDMAM.APP_ROOT_PLAY_CONF_DIRECTORY.getAbsolutePath() + File.separator + "tools" + File.separator + "git-history-export.groovy"));
			HashMap<String, Object> vars = new HashMap<>();
			TemplateWriter tw = new TemplateWriter(System.out);
			
			Consumer<GithubIssue> process_template = issue -> {
				vars.put("issue", issue);
				try {
					template.make(vars).writeTo(tw);
				} catch (IOException e) {
					throw new RuntimeException("Can't make template", e);
				}
			};
			
			Predicate<GithubIssue> all_not_bug = issue -> {
				return issue.isBug() == false;
			};
			
			current_issues.stream().filter(all_not_bug).forEach(process_template);
			
			Predicate<GithubIssue> all_bugs = issue -> {
				return issue.isBug();
			};
			
			if (current_issues.stream().anyMatch(all_bugs)) {
				System.out.println("");
				System.out.println("");
				System.out.println("Bugs");
				current_issues.stream().filter(all_bugs).forEach(process_template);
				System.out.println("");
			}
			
			return;
		}
		
		showFullCliModuleHelp();
	}
	
	public void showFullCliModuleHelp() {
		System.out.println("Usage " + getCliModuleName() + " -githistory [from tag]");
	}
	
	public boolean isFunctionnal() {
		return GitInfo.getFromRoot().isEmulatedGit() == false;
	}
	
	private class TemplateWriter extends Writer {
		
		private PrintStream out;
		
		public TemplateWriter(PrintStream out) {
			this.out = out;
			if (out == null) {
				throw new NullPointerException("\"out\" can't to be null");
			}
		}
		
		public void write(char[] cbuf, int off, int len) throws IOException {
			out.print(String.valueOf(cbuf, off, len));
		}
		
		public void flush() throws IOException {
			out.flush();
		}
		
		public void close() throws IOException {
		}
		
	};
	
}
