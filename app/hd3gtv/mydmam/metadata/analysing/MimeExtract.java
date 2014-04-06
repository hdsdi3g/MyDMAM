/*
 * 
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2010-2014
 * 
*/

package hd3gtv.mydmam.metadata.analysing;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Collection;

import eu.medsea.mimeutil.MimeUtil;
import eu.medsea.mimeutil.detector.ExtensionMimeDetector;

public class MimeExtract {
	
	public static final String default_mime = "application/octet-stream";
	
	static {
		MimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetector");
		MimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.ExtensionMimeDetector");
	}
	
	public static String getMime(File file) {
		try {
			return getMime(MimeUtil.getMimeTypes(file));
		} catch (Exception e) {
			e.printStackTrace();
			return default_mime;
		}
	}
	
	private static String getMime(Collection<?> mimeTypes) {
		return String.valueOf(mimeTypes.toArray()[0]).toLowerCase();
	}
	
	/**
	 * @param in will be wrapped in BufferedInputStream if it is not already the case, and will be close at end.
	 */
	public static String getMime(InputStream in, String simplefilename) {
		try {
			BufferedInputStream bis;
			if (in instanceof BufferedInputStream) {
				bis = (BufferedInputStream) in;
			} else {
				bis = new BufferedInputStream(in, 8 * 1024);
			}
			
			String result;
			
			result = getMime(MimeUtil.getMimeTypes(bis));
			if (result.equals(default_mime)) {
				result = getMime(((ExtensionMimeDetector) MimeUtil.getMimeDetector("eu.medsea.mimeutil.detector.ExtensionMimeDetector")).getMimeTypesFileName(simplefilename));
			}
			
			try {
				bis.close();
			} catch (Exception e) {
			}
			
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return default_mime;
		}
	}
	
}
