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
package hd3gtv.archivecircleapi;

public class ACTape {
	
	public String barcode;
	public String name;
	public String type;
	public String poolName;
	public PoolType poolType;
	public boolean fragmented;
	public int fragmentation;
	public boolean closed;
	public boolean full;
	public long capacity;
	public long used;
	public String externalId;
	public String externalLocationCode;
	public long lastMount;
	public ACAccessibility accessibility;
	public TapeLocation location;
	public String address;
	public long lastUpdated;
	
	public enum PoolType {
		UNKNOWN, CLEAN, NOT_ASSIGNED, USER;
	}
	
	public enum TapeLocation {
		DRIVE, MAILBOX, SLOT, PICKER;
	}
	
	public String toString() {
		String s_closed = "";
		if (closed) {
			s_closed = "closed ";
		}
		
		String s_full = "";
		if (full) {
			s_full = "full ";
		}
		
		String s_location = "";
		if (location != null) {
			s_location = "in " + location + " ";
		}
		String s_address = "";
		if (address != null) {
			s_address = address + " ";
		}
		
		return barcode + " " + accessibility + " " + type + " from " + poolName + " " + s_closed + s_full + s_location + s_address;
	}
}
