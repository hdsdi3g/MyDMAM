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
 * Copyright (C) hdsdi3g for hd3g.tv 26 févr. 2015
 * 
*/
package hd3gtv.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.CRC32;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import hd3gtv.log2.Log2;
import hd3gtv.mydmam.manager.JobProgression;

public class CopyMove {
	
	public enum FileExistsPolicy {
		OVERWRITE, IGNORE, RENAME
	}
	
	public static void checkExistsCanRead(File element) throws IOException, NullPointerException {
		if (element == null) {
			throw new NullPointerException("element is null");
		}
		if (element.exists() == false) {
			throw new FileNotFoundException("\"" + element.getPath() + "\" in filesytem");
		}
		if (element.canRead() == false) {
			throw new IOException("Can't read element \"" + element.getPath() + "\"");
		}
	}
	
	public static void checkIsDirectory(File element) throws FileNotFoundException {
		if (element.isDirectory() == false) {
			throw new FileNotFoundException("\"" + element.getPath() + "\" is not a directory");
		}
	}
	
	public static void checkIsWritable(File element) throws IOException {
		if (element.canWrite() == false) {
			throw new IOException("\"" + element.getPath() + "\" is not writable");
		}
	}
	
	private File source;
	private File destination;
	private boolean delete_after_copy;
	private JobProgression progression;
	
	private long total_size = 0;
	private long progress_copy = 0;
	private int actual_progress_value = 0;
	private int last_progress_value = 0;
	private int progress_size = 0;
	private FileExistsPolicy fileexistspolicy;
	
	public CopyMove(File source, File destination) throws NullPointerException, IOException {
		this.source = source;
		this.destination = destination;
		
		checkExistsCanRead(source);
		
		checkExistsCanRead(destination);
		checkIsDirectory(destination);
		checkIsWritable(destination);
		fileexistspolicy = FileExistsPolicy.OVERWRITE;
	}
	
	public CopyMove setFileExistsPolicy(FileExistsPolicy fileexistspolicy) {
		if (fileexistspolicy == null) {
			return this;
		}
		this.fileexistspolicy = fileexistspolicy;
		return this;
	}
	
	public CopyMove setDelete_after_copy(boolean delete_after_copy) throws IOException {
		this.delete_after_copy = delete_after_copy;
		if (delete_after_copy) {
			checkIsWritable(source);
		}
		return this;
	}
	
	private boolean force_move_with_copy_delete;
	
	private CopyMove forceMoveWithCopyDelete() throws IOException {
		setDelete_after_copy(true);
		force_move_with_copy_delete = true;
		return this;
	}
	
	public CopyMove setProgression(JobProgression progression) {
		this.progression = progression;
		return this;
	}
	
	public void operate() throws IOException {
		if (delete_after_copy & (force_move_with_copy_delete == false)) {
			/**
			 * Move operation
			 * Can we simply move ?
			 */
			File moveto = new File(destination.getAbsoluteFile() + File.separator + source.getName());
			if (source.renameTo(moveto)) {
				return;
			} else {
				/*Log2Dump dump = new Log2Dump();
				dump.add("source", source);
				dump.add("moveto", moveto);
				Log2.log.debug("Can't simply move, do a copy + move operation", dump);*/
			}
		}
		
		ArrayList<File> list_to_copy = new ArrayList<File>();
		/**
		 * Dirlist source
		 */
		if (source.isDirectory()) {
			dirListing(source, list_to_copy);
		} else {
			list_to_copy.add(source);
		}
		
		File item_to_copy;
		/**
		 * Compute total_size to display progress.
		 */
		total_size = 0;
		for (int pos_ltc = 0; pos_ltc < list_to_copy.size(); pos_ltc++) {
			item_to_copy = list_to_copy.get(pos_ltc);
			total_size += item_to_copy.length();
		}
		
		/**
		 * check UsableSpace
		 */
		if (destination.getUsableSpace() > 0) {
			if (total_size > destination.getUsableSpace()) {
				throw new IOException("Not enough space in destination directory");
			}
		}
		
		/**
		 * Copy source
		 */
		File destination_element_to_copy;
		progress_size = ((int) total_size / (1024 * 1024));
		actual_progress_value = 0;
		last_progress_value = 0;
		progress_copy = 0;
		
		int chars_to_ignore = source.getParentFile().getAbsolutePath().length() + 1;
		String base_destination_path = destination.getAbsolutePath() + File.separator;
		
		for (int pos_ltc = 0; pos_ltc < list_to_copy.size(); pos_ltc++) {
			item_to_copy = list_to_copy.get(pos_ltc);
			destination_element_to_copy = new File(base_destination_path + item_to_copy.getAbsolutePath().substring(chars_to_ignore));
			
			if (item_to_copy.isDirectory()) {
				if (destination_element_to_copy.exists()) {
					if (destination_element_to_copy.isDirectory() == false) {
						throw new IOException("Destination directory exists, and it not a directory: \"" + destination_element_to_copy.getPath() + "\"");
					}
				} else {
					if (destination_element_to_copy.mkdirs() == false) {
						throw new IOException("Can't create destination directory: \"" + destination_element_to_copy.getPath() + "\"");
					}
				}
			} else {
				copyFile(item_to_copy, destination_element_to_copy);
			}
			
			if (progression != null) {
				progress_copy += destination_element_to_copy.length();
				actual_progress_value = (int) (progress_copy / (1024 * 1024));
				if (actual_progress_value > last_progress_value) {
					last_progress_value = actual_progress_value;
					progression.updateProgress(actual_progress_value, progress_size);
				}
			}
		}
		
		if (delete_after_copy) {
			/**
			 * To complete a move operation
			 */
			for (int pos_ltc = list_to_copy.size() - 1; pos_ltc > -1; pos_ltc--) {
				item_to_copy = list_to_copy.get(pos_ltc);
				if (item_to_copy.delete() == false) {
					throw new IOException("Can't delete moved file: \"" + item_to_copy.getPath() + "\"");
				}
			}
		}
	}
	
	/**
	 * 50 Mbytes in bytes
	 */
	private static final long FIFTY_MB = 52428800;
	
	private void copyFile(File source_file, File destination_file) throws IOException {
		if (destination_file.exists()) {
			// dump.add("source_file", source_file);
			// dump.add("destination_file", destination_file);
			// dump.add("delete_after_copy", delete_after_copy);
			
			if (fileexistspolicy == FileExistsPolicy.IGNORE) {
				// Log2.log.debug("Destination file exists, ignore copy/move", dump);
				return;
			} else if (fileexistspolicy == FileExistsPolicy.OVERWRITE) {
				// Log2.log.debug("Destination file exists, overwrite it", dump);
				FileUtils.forceDelete(destination_file);
			} else if (fileexistspolicy == FileExistsPolicy.RENAME) {
				// destination_file
				int cursor = 1;
				int dot_pos;
				StringBuilder sb;
				while (destination_file.exists()) {
					sb = new StringBuilder();
					sb.append(destination_file.getParent());
					sb.append(File.separator);
					dot_pos = destination_file.getName().lastIndexOf(".");
					if (dot_pos > 0) {
						sb.append(destination_file.getName().substring(0, dot_pos));
						sb.append(" (");
						sb.append(cursor);
						sb.append(")");
						sb.append(destination_file.getName().substring(dot_pos, destination_file.getName().length()));
					} else {
						sb.append(destination_file.getName());
						sb.append(" (");
						sb.append(cursor);
						sb.append(")");
					}
					destination_file = new File(sb.toString());
					cursor++;
				}
				// dump.add("new destination file name", destination_file);
				// Log2.log.debug("Destination file exists, change destionation name", dump);
			}
		}
		/**
		 * Imported from org.apache.commons.io.FileUtils
		 * Licensed to the Apache Software Foundation,
		 * http://www.apache.org/licenses/LICENSE-2.0
		 */
		FileInputStream fis = null;
		FileOutputStream fos = null;
		FileChannel input = null;
		FileChannel output = null;
		try {
			fis = new FileInputStream(source_file);
			fos = new FileOutputStream(destination_file);
			input = fis.getChannel();
			output = fos.getChannel();
			long size = input.size();
			long pos = 0;
			long count = 0;
			while (pos < size) {
				count = (size - pos) > FIFTY_MB ? FIFTY_MB : (size - pos);
				pos += output.transferFrom(input, pos, count);
				
				if (progression != null) {
					actual_progress_value = (int) ((pos + progress_copy) / (1024 * 1024));
					if (actual_progress_value > last_progress_value) {
						last_progress_value = actual_progress_value;
						progression.updateProgress(actual_progress_value, progress_size);
					}
				}
			}
		} finally {
			IOUtils.closeQuietly(output);
			IOUtils.closeQuietly(fos);
			IOUtils.closeQuietly(input);
			IOUtils.closeQuietly(fis);
		}
		
		if (source_file.length() != destination_file.length()) {
			throw new IOException("Failed to copy full contents from '" + source_file + "' to '" + destination_file + "'");
		}
		if (destination_file.setExecutable(source_file.canExecute()) == false) {
			Log2.log.error("Can't set Executable status to dest file", new IOException(destination_file.getPath()));
		}
		if (destination_file.setLastModified(source_file.lastModified()) == false) {
			Log2.log.error("Can't set LastModified status to dest file", new IOException(destination_file.getPath()));
		}
	}
	
	private static void dirListing(File source, ArrayList<File> list_to_copy) throws IOException {
		ArrayList<File> bucket = new ArrayList<File>();
		ArrayList<File> next_bucket = new ArrayList<File>();
		bucket.add(source);
		
		File bucket_item;
		File[] list_content;
		
		while (true) {
			for (int pos_b = 0; pos_b < bucket.size(); pos_b++) {
				bucket_item = bucket.get(pos_b).getCanonicalFile();
				
				if (FileUtils.isSymlink(bucket_item)) {
					continue;
				}
				
				if (list_to_copy.contains(bucket_item)) {
					continue;
				}
				if (bucket_item.isDirectory()) {
					list_content = bucket_item.listFiles();
					for (int pos_lc = 0; pos_lc < list_content.length; pos_lc++) {
						next_bucket.add(list_content[pos_lc]);
					}
				}
				list_to_copy.add(bucket_item);
			}
			if (next_bucket.isEmpty()) {
				return;
			}
			bucket.clear();
			bucket.addAll(next_bucket);
			next_bucket.clear();
		}
	}
	
	private CopyMove() {
	}
	
	/**
	 * Do a complete and recursive test for this class.
	 * Test copy, move (rename) and move (copy + delete) for a file, and directories with a thousand of files inside.
	 */
	private class InternalTests {
		Random random = new Random(System.nanoTime());
		CRC32 crc32 = new CRC32();
		
		private class TestFile {
			String relative_path;
			long size = 0;
			long cksum = 0;
			boolean directory = false;
			
			public TestFile(File root, File source) throws IOException {
				relative_path = source.getPath().substring(root.getPath().length());
				if (source.isDirectory()) {
					directory = true;
				} else {
					size = source.length();
					
					crc32.reset();
					cksum = FileUtils.checksum(source, crc32).getValue();
				}
			}
			
			void checkFile(File parent) throws IOException {
				File compare = new File(parent.getPath() + relative_path);
				
				checkExistsCanRead(compare);
				
				if (directory) {
					checkIsDirectory(compare);
					return;
				}
				
				if (size != compare.length()) {
					throw new IOException("Invalid size with " + relative_path);
				}
				
				crc32.reset();
				long cksum_new = FileUtils.checksum(compare, crc32).getValue();
				if (cksum != cksum_new) {
					throw new IOException("Invalid cksum with " + relative_path + " (" + cksum + "/" + cksum_new + ")");
				}
			}
			
			public String toString() {
				StringBuilder sb = new StringBuilder();
				sb.append(relative_path);
				sb.append("\t");
				sb.append(size);
				sb.append("\t");
				sb.append(cksum);
				sb.append("\t");
				sb.append(directory);
				return sb.toString();
			}
		}
		
		InternalTests(File temp_dir) throws Exception {
			checkExistsCanRead(temp_dir);
			checkIsDirectory(temp_dir);
			checkIsWritable(temp_dir);
			
			ArrayList<TestFile> test_files;
			CopyMove cm;
			File parent_source;
			File parent_destination;
			TestFile test_file;
			
			/**
			 * Test copy file
			 */
			parent_source = createTempFile(temp_dir, false);
			parent_destination = prepareWorkingDirectory(temp_dir, "dest");
			test_file = new TestFile(temp_dir, parent_source);
			test_file.checkFile(temp_dir);
			cm = new CopyMove(parent_source, parent_destination);
			cm.operate();
			test_file.checkFile(parent_destination);
			FileUtils.deleteQuietly(parent_source);
			FileUtils.deleteQuietly(parent_destination);
			
			/**
			 * Test move (rename) file
			 */
			parent_source = createTempFile(temp_dir, false);
			parent_destination = prepareWorkingDirectory(temp_dir, "dest");
			test_file = new TestFile(temp_dir, parent_source);
			test_file.checkFile(temp_dir);
			cm = new CopyMove(parent_source, parent_destination);
			cm.setDelete_after_copy(true);
			cm.operate();
			test_file.checkFile(parent_destination);
			if (parent_source.exists()) {
				throw new IOException("parent_source exists");
			}
			FileUtils.deleteQuietly(parent_source);
			FileUtils.deleteQuietly(parent_destination);
			
			/**
			 * Test move (copy + delete) file
			 */
			parent_source = createTempFile(temp_dir, false);
			parent_destination = prepareWorkingDirectory(temp_dir, "dest");
			test_file = new TestFile(temp_dir, parent_source);
			test_file.checkFile(temp_dir);
			cm = new CopyMove(parent_source, parent_destination);
			cm.forceMoveWithCopyDelete();
			cm.operate();
			test_file.checkFile(parent_destination);
			if (parent_source.exists()) {
				throw new IOException("parent_source exists");
			}
			FileUtils.deleteQuietly(parent_source);
			FileUtils.deleteQuietly(parent_destination);
			
			/**
			 * Test copy directory
			 */
			parent_source = prepareWorkingDirectory(temp_dir, "source");
			test_files = prepareTestFiles(parent_source);
			checkFiles(parent_source, test_files);
			
			parent_destination = prepareWorkingDirectory(temp_dir, "dest");
			cm = new CopyMove(parent_source, parent_destination);
			cm.operate();
			checkFiles(new File(parent_destination.getPath() + File.separator + parent_source.getName()), test_files);
			FileUtils.deleteQuietly(parent_destination);
			
			/**
			 * Test move (rename) directory
			 */
			parent_destination = prepareWorkingDirectory(temp_dir, "dest");
			cm = new CopyMove(parent_source, parent_destination);
			cm.setDelete_after_copy(true);
			cm.operate();
			checkFiles(new File(parent_destination.getPath() + File.separator + parent_source.getName()), test_files);
			if (parent_source.exists()) {
				throw new IOException("can't move parent source " + parent_source.getPath());
			}
			FileUtils.deleteQuietly(parent_destination);
			
			/**
			 * Test move (copy + delete) directory
			 */
			parent_source = prepareWorkingDirectory(temp_dir, "source");
			test_files = prepareTestFiles(parent_source);
			checkFiles(parent_source, test_files);
			parent_destination = prepareWorkingDirectory(temp_dir, "dest");
			cm = new CopyMove(parent_source, parent_destination);
			cm.forceMoveWithCopyDelete();
			cm.operate();
			checkFiles(new File(parent_destination.getPath() + File.separator + parent_source.getName()), test_files);
			if (parent_source.exists()) {
				throw new IOException("can't move parent source " + parent_source.getPath());
			}
			FileUtils.deleteQuietly(parent_destination);
			
			FileUtils.deleteQuietly(parent_source);
		}
		
		File prepareWorkingDirectory(File root, String name) throws IOException {
			File item_dir = new File(root.getPath() + File.separator + name);
			
			if (item_dir.exists()) {
				FileUtils.deleteQuietly(item_dir);
			}
			if (item_dir.mkdir() == false) {
				throw new IOException("Can't create source file \"" + item_dir + "\"");
			}
			return item_dir;
		}
		
		void checkFiles(File parent, ArrayList<TestFile> test_files) throws IOException {
			for (int pos = 0; pos < test_files.size(); pos++) {
				test_files.get(pos).checkFile(parent);
			}
		}
		
		ArrayList<TestFile> prepareTestFiles(File parent) throws IOException {
			
			List<File> dirs = createTreeDir(parent);
			List<File> files = new ArrayList<File>(dirs.size());
			
			/**
			 * Add a 50MB file
			 */
			files.add(createTempFile(dirs.get(random.nextInt(dirs.size())), true));
			
			/**
			 * Add a lot of small files.
			 */
			for (int pos_d = 0; pos_d < dirs.size(); pos_d++) {
				for (int pos_f = 0; pos_f < random.nextInt(10) - 1; pos_f++) {
					files.add(createTempFile(dirs.get(pos_d), false));
				}
			}
			
			ArrayList<TestFile> test_files = new ArrayList<CopyMove.InternalTests.TestFile>(dirs.size() + files.size());
			
			for (int pos = 0; pos < dirs.size(); pos++) {
				test_files.add(new TestFile(parent, dirs.get(pos)));
			}
			for (int pos = 0; pos < files.size(); pos++) {
				test_files.add(new TestFile(parent, files.get(pos)));
			}
			return test_files;
		}
		
		String createTempName(boolean isfile) {
			StringBuilder sb = new StringBuilder();
			
			if (random.nextInt(10) == 1) {
				sb.append(".");
			}
			
			int val;
			for (int pos = 0; pos < (random.nextInt(20) + 1); pos++) {
				val = random.nextInt(91 - 65);
				sb.append(Character.toChars(val + 65));
			}
			sb.append(" ");
			sb.append(random.nextInt(10000));
			
			if (isfile) {
				sb.append(".");
				for (int pos = 0; pos < 3; pos++) {
					val = random.nextInt(122 - 97);
					sb.append(Character.toChars(val + 97));
				}
			}
			
			return sb.toString();
		}
		
		List<File> createTreeDir(File parent) throws IOException {
			ArrayList<File> result = new ArrayList<File>();
			
			for (int i = 0; i < 5; i++) {
				result.add(createTempDir(parent));
			}
			
			ArrayList<File> temp = new ArrayList<File>();
			for (int d = 0; d < 5; d++) {
				temp.clear();
				temp.addAll(result);
				for (int pos = 0; pos < temp.size(); pos++) {
					for (int i = 0; i < random.nextInt(5); i++) {
						result.add(createTempDir(temp.get(pos)));
					}
				}
			}
			return result;
		}
		
		File createTempDir(File parent) throws IOException {
			File new_dir = new File(parent.getPath() + File.separator + createTempName(false));
			if (new_dir.mkdir() == false) {
				throw new IOException("Can't create temp dir");
			}
			return new_dir;
		}
		
		File createTempFile(File parent, boolean big_file) throws IOException {
			File new_file = new File(parent.getPath() + File.separator + createTempName(true));
			
			FileOutputStream fos = new FileOutputStream(new_file);
			int size = random.nextInt(500);
			byte[] buffer = new byte[random.nextInt(500)];
			for (int pos = 0; pos < size; pos++) {
				random.nextBytes(buffer);
				fos.write(buffer, 0, buffer.length);
			}
			if (big_file) {
				size = size * 10;
				buffer = new byte[random.nextInt(10000)];
				while (new_file.length() < (FIFTY_MB * 1.5)) {
					fos.flush();
					for (int pos = 0; pos < size; pos++) {
						random.nextBytes(buffer);
						fos.write(buffer, 0, buffer.length);
					}
				}
			}
			fos.close();
			
			return new_file;
		}
	}
	
	/**
	 * @param args with a empty dir
	 */
	public static void main(String[] args) throws Exception {
		new CopyMove().new InternalTests(new File(args[0]).getCanonicalFile());
	}
	
}
