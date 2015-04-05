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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.mydmam.web;

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.GitInfo;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import play.Play;
import play.vfs.VirtualFile;
import yuiforkorgmozillajavascript.ErrorReporter;

import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

/**
 * "Binary" is optimzed version of a javascript commented source file.
 */
public class JsCompile {
	
	public static final String PUBLIC_JAVASCRIPT_DIRECTORY = "/public/javascripts";
	public static final String SOURCE_DIRECTORY = "src";
	public static final String BINARY_DIRECTORY = "bin";
	
	private static final ConcurrentHashMap<String, Db> compiled_db = new ConcurrentHashMap<String, JsCompile.Db>();
	
	public static final boolean COMPILE_JS;
	
	static {
		COMPILE_JS = Configuration.global.getValueBoolean("play", "compile_javascript");
	}
	
	private static class Db {
		long source_size;
		long source_lastchange;
		
		Db(VirtualFile source_file) throws FileNotFoundException {
			if (source_file.exists() == false) {
				throw new FileNotFoundException("Can't found " + source_file.getRealFile().getPath());
			}
			if (source_file.isDirectory()) {
				throw new FileNotFoundException("This element " + source_file.getRealFile().getPath() + " is not a regular file.");
			}
			this.source_size = source_file.length();
			this.source_lastchange = source_file.lastModified();
		}
		
		boolean isValidFile(VirtualFile source_file) {
			if (source_file.exists() == false) {
				return false;
			}
			if (source_file.isDirectory()) {
				return false;
			}
			if (source_file.lastModified().longValue() != this.source_lastchange) {
				return false;
			}
			if (source_file.length() != this.source_size) {
				return false;
			}
			return true;
		}
	}
	
	/**
	 * Get all items named and on this path, from all modules, and not only the first.
	 */
	public static List<VirtualFile> getAllfromRelativePath(String path, boolean must_exists, boolean must_directory) {
		List<VirtualFile> file_list = new ArrayList<VirtualFile>();
		VirtualFile child;
		for (VirtualFile vfile : Play.roots) {
			child = vfile.child(path);
			if (must_exists & (child.exists() == false)) {
				continue;
			}
			if (must_directory & (child.isDirectory() == false)) {
				continue;
			}
			file_list.add(child);
		}
		return file_list;
	}
	
	public static List<VirtualFile> prepareFiles() {
		List<VirtualFile> file_list = new ArrayList<VirtualFile>();
		
		List<VirtualFile> sources_file_list = new ArrayList<VirtualFile>();
		List<VirtualFile> main_dirs = getAllfromRelativePath(PUBLIC_JAVASCRIPT_DIRECTORY + "/" + SOURCE_DIRECTORY, true, true);
		List<VirtualFile> child_content;
		
		for (int pos_md = 0; pos_md < main_dirs.size(); pos_md++) {
			child_content = main_dirs.get(pos_md).list();
			for (int pos = 0; pos < child_content.size(); pos++) {
				if (child_content.get(pos).isDirectory()) {
					continue;
				}
				if (child_content.get(pos).getRealFile().isHidden()) {
					continue;
				}
				sources_file_list.add(child_content.get(pos));
			}
		}
		
		if (sources_file_list.isEmpty()) {
			return null;
		}
		
		if (COMPILE_JS == false) {
			return sources_file_list;
		}
		
		VirtualFile binary_dir = VirtualFile.search(Play.roots, PUBLIC_JAVASCRIPT_DIRECTORY + "/" + BINARY_DIRECTORY);
		VirtualFile sourcefile;
		VirtualFile binaryfile;
		Db element_compare;
		boolean must_concat = false;
		for (int pos = 0; pos < sources_file_list.size(); pos++) {
			sourcefile = sources_file_list.get(pos);
			binaryfile = binary_dir.child(sourcefile.getName());
			
			if (binaryfile.exists() && (compiled_db.get(binaryfile.getName()) != null)) {
				element_compare = compiled_db.get(binaryfile.getName());
				if (element_compare.isValidFile(sourcefile)) {
					file_list.add(binaryfile);
				} else {
					try {
						compile(sourcefile, binaryfile);
						file_list.add(binaryfile);
						must_concat = true;
					} catch (IOException e) {
						Log2.log.error("Can't create binary JS file", e);
						file_list.add(sourcefile);
					}
				}
			} else {
				try {
					compile(sourcefile, binaryfile);
					file_list.add(binaryfile);
					must_concat = true;
				} catch (IOException e) {
					Log2.log.error("Can't create binary JS file", e);
					file_list.add(sourcefile);
				}
			}
		}
		
		StringBuffer sb = new StringBuffer();
		sb.append(PUBLIC_JAVASCRIPT_DIRECTORY);
		sb.append("/");
		sb.append(BINARY_DIRECTORY);
		sb.append("/mydmam");
		try {
			GitInfo git = new GitInfo(new File(Play.applicationPath.getPath() + File.separator + ".git"));
			if (git != null) {
				sb.append("-");
				sb.append(git.getBranch());
				sb.append("-");
				sb.append(git.getCommit());
			}
		} catch (IOException e) {
			Log2.log.error("Can't found git repository", e, new Log2Dump("Play application path", Play.applicationPath));
		}
		sb.append(".min.js");
		
		VirtualFile concat_file = VirtualFile.search(Play.roots, sb.toString());
		if (must_concat == false) {
			if (concat_file == null) {
				must_concat = true;
			} else if (concat_file.isDirectory()) {
				must_concat = true;
			}
		}
		concat_file = VirtualFile.fromRelativePath(sb.toString());
		
		if (must_concat == false) {
			long total_expected_size = 0;
			for (int pos = 0; pos < file_list.size(); pos++) {
				total_expected_size += file_list.get(pos).length();
			}
			if (concat_file.length() != total_expected_size) {
				must_concat = true;
			}
		}
		
		if (must_concat) {
			Collections.sort(file_list, new Comparator<VirtualFile>() {
				public int compare(VirtualFile o1, VirtualFile o2) {
					return o1.getName().compareToIgnoreCase(o2.getName());
				}
			});
			
			try {
				FileOutputStream fso = new FileOutputStream(concat_file.getRealFile(), false);
				for (int pos = 0; pos < file_list.size(); pos++) {
					fso.write(file_list.get(pos).content());
					fso.flush();
				}
				fso.close();
			} catch (IOException e) {
				Log2.log.error("Can't write compiled file", e);
				return file_list;
			}
		}
		file_list.clear();
		file_list.add(concat_file);
		return file_list;
	}
	
	private static class CompilerErrorReporter implements ErrorReporter {
		
		File warning_file;
		
		public CompilerErrorReporter(File warning_file) {
			this.warning_file = warning_file;
		}
		
		public void error(String arg0, String arg1, int arg2, String arg3, int arg4) {
			Log2Dump dump = new Log2Dump();
			dump.add("arg0", arg0);
			dump.add("arg1", arg1);
			dump.add("arg2", arg2);
			dump.add("arg3", arg3);
			dump.add("arg4", arg4);
			Log2.log.error("Rhino error during javascript parsing", null, dump);
		}
		
		public yuiforkorgmozillajavascript.EvaluatorException runtimeError(String arg0, String arg1, int arg2, String arg3, int arg4) {
			Log2Dump dump = new Log2Dump();
			dump.add("arg0", arg0);
			dump.add("arg1", arg1);
			dump.add("arg2", arg2);
			dump.add("arg3", arg3);
			dump.add("arg4", arg4);
			Log2.log.error("Rhino error during javascript parsing", null, dump);
			return null;
		}
		
		public void warning(String arg0, String arg1, int arg2, String arg3, int arg4) {
			if (warning_file == null) {
				return;
			}
			try {
				FileWriter fw = new FileWriter(warning_file, true);
				PrintWriter warning_log = new PrintWriter(fw);
				warning_log.println(arg0);
				if (arg1 != null) {
					warning_log.println(arg1);
				}
				if (arg2 > -1) {
					warning_log.println(arg2);
				}
				if (arg3 != null) {
					warning_log.println(arg3);
				}
				if (arg4 > -1) {
					warning_log.println(arg4);
				}
				warning_log.println();
				warning_log.close();
			} catch (IOException e) {
				Log2.log.error("Can't write warning log", e);
			}
		}
	}
	
	private static void compile(VirtualFile sourcefile, VirtualFile binaryfile) throws IOException {
		File warning_file = new File(binaryfile.getRealFile().getAbsolutePath() + "-warning.txt");
		warning_file.delete();
		
		CompilerErrorReporter compiler_error_reporter = new CompilerErrorReporter(warning_file);
		
		InputStreamReader in = new InputStreamReader(new FileInputStream(sourcefile.getRealFile()));
		JavaScriptCompressor compressor = new JavaScriptCompressor(in, compiler_error_reporter);
		in.close();
		
		OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(binaryfile.getRealFile()));
		compressor.compress(out, 200, true, Play.mode.isDev(), true, false);
		out.close();
		
		compiled_db.put(binaryfile.getName(), new Db(sourcefile));
		if (COMPILE_JS) {
			Log2.log.debug("Compile JS file", new Log2Dump("source", sourcefile.getRealFile()));
		}
	}
	
	public static void purgeBinDirectory() {
		VirtualFile binary_dir = VirtualFile.search(Play.roots, PUBLIC_JAVASCRIPT_DIRECTORY + "/" + BINARY_DIRECTORY);
		if (binary_dir == null) {
			return;
		}
		List<VirtualFile> list = binary_dir.list();
		File realfile;
		Log2Dump dump = new Log2Dump();
		boolean has_purge = false;
		for (int pos = 0; pos < list.size(); pos++) {
			realfile = list.get(pos).getRealFile();
			if (realfile.isDirectory()) {
				continue;
			}
			if (realfile.isHidden()) {
				continue;
			}
			dump.add("file", realfile);
			dump.add("delete", realfile.delete());
			has_purge = true;
		}
		if (has_purge) {
			Log2.log.debug("Purge compiled js files temp", dump);
		}
	}
	
	public static List<String> getURLlist() {
		ArrayList<String> list = new ArrayList<String>();
		
		List<VirtualFile> file_list = prepareFiles();
		if (file_list == null) {
			return new ArrayList<String>();
		}
		
		File real_file;
		for (int pos = 0; pos < file_list.size(); pos++) {
			real_file = file_list.get(pos).getRealFile();
			if (real_file.getParentFile().getName().equals(PUBLIC_JAVASCRIPT_DIRECTORY.substring(PUBLIC_JAVASCRIPT_DIRECTORY.lastIndexOf("/") + 1))) {
				list.add("/" + real_file.getName());
			} else {
				list.add(real_file.getParentFile().getName() + "/" + real_file.getName());
			}
		}
		
		Collections.sort(list);
		return list;
	}
	
	/**
	 * Beware, this operation TAKE TIME and MEMORY (because it must instance a Javascript interpreter).
	 */
	public static String compileJSOnTheFly(String content) {
		try {
			StringReader sr = new StringReader(content);
			JavaScriptCompressor compressor;
			compressor = new JavaScriptCompressor(sr, new CompilerErrorReporter(null));
			StringWriter sw = new StringWriter(content.length());
			compressor.compress(sw, 80, true, false, true, false);
			return sw.toString();
		} catch (Exception e) {
			Log2.log.error("Can't compile on the fly", e, new Log2Dump("raw content", content));
			return content;
		}
	}
	
}
