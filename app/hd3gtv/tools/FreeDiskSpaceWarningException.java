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
package hd3gtv.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FreeDiskSpaceWarningException extends IOException {
	
	private FreeDiskSpaceWarningException(String message) {
		super(message);
	}
	
	/**
	 * @throws FreeDiskSpaceWarningException if warning_threshold > item > error_threshold
	 * @throws IOException if error_threshold > item
	 * @throws FileNotFoundException if item not exists
	 */
	public static long check(File item, long warning_threshold, long error_threshold) throws FreeDiskSpaceWarningException, IOException, FileNotFoundException {
		if (item.exists() == false) {
			throw new FileNotFoundException("Can't found " + item.getPath());
		}
		if (item.getTotalSpace() == 0) {
			return -1;
		}
		
		long free_space = item.getFreeSpace();
		
		if (error_threshold > free_space) {
			throw new IOException("No free space left on " + item.getAbsolutePath() + ", actually " + free_space + " bytes");
		} else if (warning_threshold > free_space) {
			throw new FreeDiskSpaceWarningException("Not much free space left on " + item.getAbsolutePath() + ", actually " + free_space + " bytes");
		}
		return free_space;
	}
	
	/**
	 * @throws FreeDiskSpaceWarningException if warning_threshold > item > error_threshold
	 * @throws IOException if error_threshold > item
	 * @throws FileNotFoundException if item not exists
	 */
	public static long check(File item, long warning_threshold) throws FreeDiskSpaceWarningException, FileNotFoundException {
		if (item.exists() == false) {
			throw new FileNotFoundException("Can't found " + item.getPath());
		}
		if (item.getTotalSpace() == 0) {
			return -1;
		}
		
		long free_space = item.getFreeSpace();
		if (warning_threshold > free_space) {
			throw new FreeDiskSpaceWarningException("Not much free space left on " + item.getAbsolutePath() + ", actually " + free_space + " bytes");
		}
		
		return free_space;
	}
	
}
