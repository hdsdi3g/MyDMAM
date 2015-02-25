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
 * Copyright (C) hdsdi3g for hd3g.tv 2015
 * 
*/
package hd3gtv.mydmam.useraction.fileoperation;

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.manager.JobProgression;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.useraction.UACapability;
import hd3gtv.mydmam.useraction.UAConfigurator;
import hd3gtv.mydmam.useraction.UAJobProcess;
import hd3gtv.mydmam.useraction.fileoperation.UAFileOperationCopyMoveConfigurator.Action;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import models.UserProfile;

import org.apache.commons.io.FileUtils;

public class UAFileOperationCopyMove extends BaseFileOperation {
	
	protected String getSubLongName() {
		return "copy/move files and directories";
	}
	
	protected String getSubMessageBaseName() {
		return "copymove";
	}
	
	public UAJobProcess createProcess() {
		return new UAFileOperationCopyMove();
	}
	
	public String getDescription() {
		return "Copy and move files and directories content";
	}
	
	public boolean isPowerfulAndDangerous() {
		return true;
	}
	
	public Serializable prepareEmptyConfiguration() {
		return new UAFileOperationCopyMoveConfigurator();
	}
	
	public class Capability extends UACapability {
		
		public boolean enableFileProcessing() {
			return true;
		}
		
		public boolean enableDirectoryProcessing() {
			return true;
		}
		
		public boolean mustHaveLocalStorageindexBridge() {
			return true;
		}
		
	}
	
	public UACapability createCapability(LinkedHashMap<String, ?> internal_configuration) {
		return new Capability();
	}
	
	public void process(JobProgression progression, UserProfile userprofile, UAConfigurator user_configuration, HashMap<String, SourcePathIndexerElement> source_elements) throws Exception {
		UAFileOperationCopyMoveConfigurator conf = user_configuration.getObject(UAFileOperationCopyMoveConfigurator.class);
		
		if (conf.destination == null) {
			throw new NullPointerException("\"destination\" can't to be null");
		}
		
		Log2Dump dump = new Log2Dump();
		dump.add("user", userprofile.key);
		
		if (conf.destination.trim().equals("")) {
			Log2.log.debug("\"destination\" is empty", dump);
			return;
		}
		dump.add("path index dest", conf.destination);
		File f_destination = getDestinationFile(conf);
		dump.add("real dest", f_destination);
		
		progression.updateStep(1, source_elements.size());
		
		for (Map.Entry<String, SourcePathIndexerElement> entry : source_elements.entrySet()) {
			File source = Explorer.getLocalBridgedElement(entry.getValue());
			if (source == null) {
				throw new NullPointerException("\"" + entry.getValue().toString(" ") + "\" can't to be found");
			}
			if (source.exists() == false) {
				throw new FileNotFoundException("\"" + source.getPath() + "\" can't to be found");
			}
			if (source.canRead() == false) {
				throw new IOException("\"" + source.getPath() + "\" can't to be read");
			}
			if ((conf.action == Action.MOVE) & (source.canWrite() == false)) {
				throw new IOException("\"" + source.getPath() + "\" can't to be write (erased)");
			}
			
			Log2.log.debug("Prepare " + conf.action, dump);
			
			processSourceElement(progression, conf, source, f_destination);
			
			if (stop) {
				return;
			}
			progression.incrStep();
		}
	}
	
	private File getDestinationFile(UAFileOperationCopyMoveConfigurator conf) throws IOException, IndexOutOfBoundsException {
		// conf.destination
		String destination = conf.destination.replace("\\", File.separator);
		destination = destination.replace("/", File.separator);
		if (destination.indexOf(":") < 1) {
			throw new IndexOutOfBoundsException("Invalid destination pathindex: " + destination);
		}
		if (destination.endsWith(File.separator)) {
			destination = destination.substring(destination.length() - 1);
		}
		Explorer explorer = new Explorer();
		SourcePathIndexerElement spie_dest = explorer.getelementByIdkey(SourcePathIndexerElement.hashThis(destination));
		if (spie_dest == null) {
			throw new FileNotFoundException("\"" + destination + "\" in storage index");
		}
		File f_destination = Explorer.getLocalBridgedElement(spie_dest);
		if (f_destination == null) {
			throw new FileNotFoundException("\"" + spie_dest.toString(" ") + "\"  in storage index");
		}
		if (f_destination.exists() == false) {
			throw new FileNotFoundException("\"" + f_destination.getPath() + "\" in filesytem");
		}
		if (f_destination.isDirectory() == false) {
			throw new IOException("\"" + f_destination.getPath() + "\" is not a directory");
		}
		if ((f_destination.canRead() == false) | (f_destination.canWrite() == false)) {
			throw new IOException("Can't read or write in destinnation directory \"" + f_destination.getPath() + "\"");
		}
		return f_destination;
	}
	
	private void processSourceElement(JobProgression progression, UAFileOperationCopyMoveConfigurator conf, File source, File destination) throws Exception {
		boolean source_is_like_file = (source.isFile() | FileUtils.isSymlink(source));
		boolean user_want_copy = (conf.action == Action.COPY);
		String destination_base_path = destination.getCanonicalPath() + File.separator;
		
		if (source_is_like_file) {
			if (user_want_copy == false) {
				if (source.renameTo(new File(destination_base_path + source.getName()))) {
					/**
					 * Simple rename: ok
					 */
					return;
				}
			}
			// TODO copy file
			
			if (user_want_copy == false) {
				if (source.delete() == false) {
					// TODO ERR
				}
			}
		}
		
		// TODO copy/move dir
		
		/**
		 * copy or move
		 * if move > simple mv is ok ? else "copy"
		 * if copy & file > simple copy
		 * if copy & dir > recursive copy
		 * if move & file > delete source
		 * if move & dir > recursive delete source
		 * DON'T FORGET DATES FOR NEW FILES !
		 */
		
		/*
					String current_dir_path = current_dir.getCanonicalPath();
					File dest_dir = new File(current_dir_path + File.separator + conf.newpathname);
					
					if (dest_dir.exists()) {
						continue;
					}
					
					if (dest_dir.getCanonicalPath().startsWith(current_dir_path) == false) {
						dump.add("current_dir_path", current_dir_path);
						dump.add("dest_dir", dest_dir.getCanonicalPath());
						Log2.log.security("User try to create a sub directory outside the choosed directory", dump);
						throw new IOException("Invalid newpathname: " + conf.newpathname);
					}
					
					if (dest_dir.mkdirs() == false) {
						dump.add("dest_dir", dest_dir);
						Log2.log.debug("Can't create correctly directories", dump);
						throw new IOException("Can't create correctly directories: " + dest_dir.getPath());
					}
				 * */
		
		/*Log2Dump dump = new Log2Dump();
		dump.add("user", userprofile.key);
		
		ArrayList<File> items_to_delete = new ArrayList<File>();
		for (Map.Entry<String, SourcePathIndexerElement> entry : source_elements.entrySet()) {
			File current_element = Explorer.getLocalBridgedElement(entry.getValue());
			if (current_element == null) {
				throw new NullPointerException("Can't found current_element: " + entry.getValue().storagename + ":" + entry.getValue().currentpath);
			}
			if (current_element.exists() == false) {
				continue;
			}
			if (current_element.getParentFile().canWrite() == false) {
				throw new IOException("Can't write to current_element parent directory: " + entry.getValue().storagename + ":" + entry.getValue().currentpath);
			}
			
			if (current_element.isFile() | FileUtils.isSymlink(current_element)) {
				if (current_element.delete() == false) {
					Log2.log.debug("Can't delete correctly file", dump);
					throw new IOException("Can't delete correctly file: " + current_element.getPath());
				}
				if (stop) {
					return;
				}
				continue;
			}
			
			items_to_delete.clear();
			items_to_delete.add(current_element);
			
			recursivePath(current_element, items_to_delete);
			
			if (stop) {
				return;
			}
			
			boolean can_delete_all = true;
			
			for (int pos_idel = items_to_delete.size() - 1; pos_idel > -1; pos_idel--) {
				if (items_to_delete.get(pos_idel).delete() == false) {
					dump.add("item", items_to_delete.get(pos_idel));
					can_delete_all = false;
				}
				if (stop) {
					return;
				}
			}
			
			if (can_delete_all == false) {
				Log2.log.debug("Can't delete correctly multiple files", dump);
				throw new IOException("Can't delete multiple files from: " + current_element.getPath());
			}
			
			if (stop) {
				return;
			}
		}*/
	}
}
