/*
 * This file is part of hd3g.tv' Java Storage Abstraction
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

enum StorageType {
	
	ftp, smb, file, ftpnexio;
	
	public static StorageType fromString(String value) {
		if (value == null) {
			throw new NullPointerException("No storage type");
		}
		if (value.equalsIgnoreCase("ftp")) {
			return ftp;
		} else if (value.equalsIgnoreCase("smb")) {
			return smb;
		} else if (value.equalsIgnoreCase("file")) {
			return file;
		} else if (value.equalsIgnoreCase("ftpnexio")) {
			return ftpnexio;
		}
		throw new StringIndexOutOfBoundsException(value);
	}
	
}
