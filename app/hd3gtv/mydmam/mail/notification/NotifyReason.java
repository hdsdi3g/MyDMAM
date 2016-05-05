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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.mydmam.mail.notification;

/**
 * @deprecated
 */
public enum NotifyReason {
	ERROR, DONE, READED, CLOSED, COMMENTED;
	
	String getDbRecordName() {
		return "notify_if_" + this.name().toLowerCase();
	}
	
	public static NotifyReason getFromDbRecordName(String dbname) {
		if (dbname == null) {
			return null;
		}
		NotifyReason[] values = NotifyReason.values();
		for (int pos = 0; pos < values.length; pos++) {
			if (dbname.equalsIgnoreCase("notify_if_" + values[pos].name())) {
				return values[pos];
			}
		}
		return null;
	}
	
	public static NotifyReason getFromString(String value) {
		if (value == null) {
			return null;
		}
		NotifyReason[] values = NotifyReason.values();
		for (int pos = 0; pos < values.length; pos++) {
			if (value.equalsIgnoreCase(values[pos].name())) {
				return values[pos];
			}
		}
		return null;
	}
	
}
