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
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.tools.CopyMove;

class PackFile {
	
	String stored_name;
	String file_name;
	
	/**
	 * Only with "/", no "\"
	 */
	String file_path;
	long size;
	String digest;
	
	/**
	 * Only for Gson
	 */
	PackFile() {
	}
	
	public PackFile(PackManifest manifest, File ref, Packager packager) throws NullPointerException, IOException {
		if (manifest == null) {
			throw new NullPointerException("\"manifest\" can't to be null");
		}
		if (packager == null) {
			throw new NullPointerException("\"packager\" can't to be null");
		}
		
		if (ref == null) {
			throw new NullPointerException("\"ref\" can't to be null");
		}
		CopyMove.checkExistsCanRead(ref);
		CopyMove.checkIsFile(ref);
		
		ZipOutputStream zipdata = manifest.getOutputZipPackage();
		
		String root = packager.build_dir.getAbsolutePath().replaceAll("\\\\", "/");
		String file_dir = ref.getParentFile().getAbsolutePath().replaceAll("\\\\", "/");
		
		if (file_dir.startsWith(root) == false) {
			throw new IOException("Can't add a file outside the mydmam local build directory: " + ref.getAbsolutePath());
		}
		
		file_name = ref.getName();
		file_path = file_dir.substring(root.length());
		size = ref.length();
		stored_name = manifest.validStoredName(file_name.toLowerCase().replace(" ", ""));
		
		FileInputStream fis = null;
		try {
			MessageDigest md = MessageDigest.getInstance(manifest.digest_type);
			
			fis = new FileInputStream(ref);
			
			ZipEntry entry = new ZipEntry(stored_name);
			entry.setTime(manifest.creation_date);
			zipdata.putNextEntry(entry);
			// entry.setSize(len);
			
			int len;
			byte[] buffer = packager.buffer;
			while ((len = fis.read(buffer, 0, buffer.length)) != -1) {
				md.update(buffer, 0, len);
				zipdata.write(buffer, 0, len);
			}
			
			zipdata.flush();
			zipdata.closeEntry();
			
			fis.close();
			digest = MyDMAM.byteToString(md.digest());
		} catch (NoSuchAlgorithmException e) {
		} catch (IOException e) {
			if (fis != null) {
				IOUtils.closeQuietly(fis);
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
	
	File getLocalFile(File root) {
		return new File(root + file_path.replace("/", File.separator) + File.separator + file_name);
	}
	
	public String toString() {
		LinkedHashMap<String, Object> log = new LinkedHashMap<String, Object>();
		log.put("file_name", file_name);
		log.put("file_path", file_path);
		log.put("stored_name", stored_name);
		log.put("size", size);
		log.put("digest", digest);
		return log.toString();
	}
	
	/*
	 * 			ByteArrayOutputStream bias = new ByteArrayOutputStream(Protocol.BUFFER_SIZE);
			IOUtils.copy(zipdatas, bias);
			
			datas = bias.toByteArray();
			len = datas.length;
			date = entry.getTime();
			name = entry.getName();
	 * */
}
