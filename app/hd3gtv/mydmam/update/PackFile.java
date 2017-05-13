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
package hd3gtv.mydmam.update;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;

import org.apache.commons.io.IOUtils;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.tools.CopyMove;

class PackFile {
	
	String file_name;
	/**
	 * Only with "/"
	 */
	String file_path;
	long size;
	String digest;
	// TODO add var: stored_as
	// TODO and protect manifest.js* files
	
	/**
	 * Only for Gson
	 */
	PackFile() {
	}
	
	public PackFile(PackManifest manifest, File ref, Packager packager, ByteBuffer byte_buffer) throws NullPointerException, IOException {
		if (manifest == null) {
			throw new NullPointerException("\"manifest\" can't to be null");
		}
		if (packager == null) {
			throw new NullPointerException("\"packager\" can't to be null");
		}
		if (byte_buffer == null) {
			throw new NullPointerException("\"byte_buffer\" can't to be null");
		}
		
		if (ref == null) {
			throw new NullPointerException("\"ref\" can't to be null");
		}
		CopyMove.checkExistsCanRead(ref);
		CopyMove.checkIsFile(ref);
		
		String root = packager.build_dir.getAbsolutePath().replaceAll("\\\\", "/");
		String file_dir = ref.getParentFile().getAbsolutePath().replaceAll("\\\\", "/");
		
		if (file_dir.startsWith(root) == false) {
			throw new IOException("Can't add a file outside the mydmam local build directory: " + ref.getAbsolutePath());
		}
		
		this.file_name = ref.getName();
		this.file_path = file_dir.substring(root.length());
		this.size = ref.length();
		
		String base_dest_dir = manifest.getWorkingDir(packager).getAbsolutePath();
		
		FileInputStream fis = null;
		FileOutputStream fos = null;
		FileChannel in_channel = null;
		FileChannel out_channel = null;
		try {
			MessageDigest md = MessageDigest.getInstance(manifest.digest_type);
			
			fis = new FileInputStream(ref);
			fos = new FileOutputStream(base_dest_dir + File.separator + ref.getName());
			
			in_channel = fis.getChannel();
			out_channel = fos.getChannel();
			byte_buffer.clear();
			
			while (in_channel.read(byte_buffer) > -1) {
				byte_buffer.flip();
				md.update(byte_buffer);
				byte_buffer.flip();
				out_channel.write(byte_buffer);
				byte_buffer.clear();
			}
			
			in_channel.close();
			out_channel.close();
			fis.close();
			digest = MyDMAM.byteToString(md.digest());
		} catch (NoSuchAlgorithmException e) {
		} catch (IOException e) {
			if (out_channel != null) {
				IOUtils.closeQuietly(out_channel);
			}
			if (in_channel != null) {
				IOUtils.closeQuietly(in_channel);
			}
			if (fis != null) {
				IOUtils.closeQuietly(fis);
			}
			if (fos != null) {
				IOUtils.closeQuietly(fos);
			}
			throw e;
		}
		
		if (digest == null) {
			throw new IOException("Can't compute digest for " + ref.getAbsolutePath());
		}
		
		if (Loggers.Update.isTraceEnabled()) {
			Loggers.Update.trace("Import a packfile: " + this);
		}
	}
	
	public String toString() {
		LinkedHashMap<String, Object> log = new LinkedHashMap<String, Object>();
		log.put("file_name", file_name);
		log.put("file_path", file_path);
		log.put("size", size);
		log.put("digest", digest);
		return log.toString();
	}
	
}
