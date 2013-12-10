/*
 * This file is part of MyDMAM
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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.storage;

public abstract class IgnoreFiles {
	
	public abstract boolean isFileNameIsAllowed(String filename);
	
	public abstract boolean isDirNameIsAllowed(String dirname);
	
	public static IgnoreFiles default_list = new IgnoreFiles() {
		
		public boolean isFileNameIsAllowed(String filename) {
			if (filename.endsWith(".lnk")) {
				return false;
			}
			return true;
		}
		
		public boolean isDirNameIsAllowed(String dirname) {
			return true;
		}
	};
	
}
