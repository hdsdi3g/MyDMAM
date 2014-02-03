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

package hd3gtv.mydmam.auth;

import java.io.File;
import java.io.IOException;

public class LocalAuthentication implements AuthenticationConfiguration {
	
	private File dbfile;
	
	LocalAuthentication(File dbfile) throws IOException {
		if (dbfile == null) {
			throw new NullPointerException("\"dbfile\" can't to be null");
		}
		if (dbfile.exists()) {
			if (dbfile.isFile() == false) {
				throw new IOException(dbfile.getPath() + " is not a file");
			}
			if (dbfile.canRead() == false) {
				throw new IOException("Can't read " + dbfile.getPath());
			}
		} else {
			// TODO create sqlite db
		}
		this.dbfile = dbfile;
	}
	
	// TODO sqlite ?
	// TODO CRUD users ?
	
}
