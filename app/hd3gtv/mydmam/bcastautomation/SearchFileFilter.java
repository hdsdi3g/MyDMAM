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
package hd3gtv.mydmam.bcastautomation;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

class SearchFileFilter implements FilenameFilter {
	
	boolean functionnal = true;
	List<String> ext;
	
	SearchFileFilter(List<String> ext) {
		this.ext = ext;
		
		if (ext == null) {
			functionnal = false;
		}
		if (ext.isEmpty()) {
			functionnal = false;
		}
	}
	
	public boolean accept(File dir, String name) {
		if (functionnal == false) {
			return true;
		}
		for (int pos = 0; pos < ext.size(); pos++) {
			if (name.toLowerCase().endsWith("." + ext.get(pos).toLowerCase())) {
				return true;
			}
		}
		return false;
	}
	
}