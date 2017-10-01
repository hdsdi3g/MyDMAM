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
 * Copyright (C) hdsdi3g for hd3g.tv 1 oct. 2017
 * 
*/
package hd3gtv.mydmam.embddb.store;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import junit.framework.TestCase;

public class FileIndexPathsTest extends TestCase {
	
	private final static File USER_HOME = new File(System.getProperty("user.home"));
	private final FileIndexPaths file_index;
	private File llist_file;
	
	public FileIndexPathsTest() throws IOException {
		File index_file = File.createTempFile("mydmam-test-idx", ".bin", USER_HOME);
		if (index_file.getFreeSpace() < 10_000_000l) {
			throw new IOException("No more space for " + index_file);
		}
		FileUtils.forceDelete(index_file);
		index_file.deleteOnExit();
		
		llist_file = File.createTempFile("mydmam-test-llist", ".bin", USER_HOME);
		FileUtils.forceDelete(llist_file);
		llist_file.deleteOnExit();
		file_index = new FileIndexPaths(index_file, llist_file, 16);
	}
	
	protected void tearDown() throws Exception {
		file_index.clear();
	}
	
	public void testSimple() throws IOException {
		Map<ItemKey, File> all_user_files = Files.walk(USER_HOME.toPath()).map(path -> {
			return path.toFile();
		}).filter(file -> {
			return file.getName().startsWith(".") == false;
		}).filter(file -> {
			return file.isFile();
		}).filter(file -> {
			return file.getParent().contains("/.") == false;
		}).limit(100).collect(Collectors.toMap(f -> {
			return new ItemKey(f.getPath());
		}, f -> {
			return f;
		}));
		
		List<File> all_parents = all_user_files.values().stream().map(f -> {
			try {
				file_index.add(new ItemKey(f.getPath()), f.getParent());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return f.getParentFile();
		}).distinct().collect(Collectors.toList());
		
		all_parents.stream().forEach(parent -> {
			try {
				System.out.print(parent + " -> ");
				System.out.println(file_index.getAllKeysInPath(parent.getPath(), false).map(sub_file -> {// FIXME recursive don't works...
					return all_user_files.get(sub_file).getName();
				}).collect(Collectors.toList()));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		
		// file_index.getAllKeysInPath(path)
	}
}
