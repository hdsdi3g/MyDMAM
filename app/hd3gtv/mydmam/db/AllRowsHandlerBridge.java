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
package hd3gtv.mydmam.db;

import com.google.common.base.Function;
import com.netflix.astyanax.model.Row;

import hd3gtv.mydmam.Loggers;

class AllRowsHandlerBridge implements Function<Row<String, String>, Boolean> {
	
	AllRowsFoundRow handler;
	
	AllRowsHandlerBridge(AllRowsFoundRow handler) {
		this.handler = handler;
	}
	
	public synchronized Boolean apply(Row<String, String> row) {
		try {
			handler.onFoundRow(row);
		} catch (Exception e) {
			Loggers.Cassandra.error("Error during AllRowsReader", e);
			return false;
		}
		return true;
	}
	
}
