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

import hd3gtv.configuration.GitInfo;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import play.Play;
import play.vfs.VirtualFile;

/**
 * "Binary" is optimzed version of a javascript commented source file.
 */
public class JsCompile {
	
	public static final String PUBLIC_JAVASCRIPT_DIRECTORY = "/public/javascripts";
	public static final String SOURCE_DIRECTORY = "src";
	public static final String BINARY_DIRECTORY = "bin";
	
	private static final ConcurrentHashMap<String, Db> compiled_db = new ConcurrentHashMap<String, JsCompile.Db>();
	
	/**
	 * Set to true if you need to debug this in Play Dev mode.
	 */
	private static boolean FORCE_PROD_MODE = true;// TODO set to false
	
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
	
	public static List<VirtualFile> prepareFiles() {
		List<VirtualFile> file_list = new ArrayList<VirtualFile>();
		
		List<VirtualFile> sources_file_list = new ArrayList<VirtualFile>();
		List<VirtualFile> child_content;
		VirtualFile child;
		for (VirtualFile file : Play.roots) {
			child = file.child(PUBLIC_JAVASCRIPT_DIRECTORY + "/" + SOURCE_DIRECTORY);
			if (child.exists() == false) {
				continue;
			}
			if (child.isDirectory() == false) {
				continue;
			}
			child_content = child.list();
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
		
		if (Play.mode.isDev() & (FORCE_PROD_MODE == false)) {
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
	
	private static void compile(VirtualFile sourcefile, VirtualFile binaryfile) throws IOException {
		FileOutputStream fso = new FileOutputStream(binaryfile.getRealFile(), false);// TODO real compilation
		fso.write(sourcefile.content());
		fso.close();
		compiled_db.put(binaryfile.getName(), new Db(sourcefile));
		if (FORCE_PROD_MODE) {
			Log2.log.debug("Compile JS file", new Log2Dump("source", sourcefile.getRealFile()));
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
	
}
