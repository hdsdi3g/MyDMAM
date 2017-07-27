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

import hd3gtv.mydmam.Loggers;

public class ACTapeAudit {
	
	public int id;
	public String barcode;
	public long date;
	public TapeAuditEvent event;
	
	public String toString() {
		return Loggers.dateLog(date) + " " + barcode + " " + event + " [" + id + "]";
	}
	
	public enum TapeAuditEvent {
		CREATED, REMOVED, DELETED, MOVED_OUT_FROM_LIBRARY, MOVED_INTO_LIBRARY;
	}
	
}
