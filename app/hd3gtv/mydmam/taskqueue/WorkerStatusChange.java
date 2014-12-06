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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam.taskqueue;

import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.model.ColumnList;

@Deprecated
public enum WorkerStatusChange {
	
	ENABLED, DISABLED;
	
	final void pushToDatabase(MutationBatch mutator, String worker_ref, int ttl) {
		mutator.withRow(Broker.CF_WORKERGROUPS, worker_ref).putColumnIfNotNull(WorkerGroup.COL_NAME_STATUSCHANGE, name().toUpperCase(), ttl);
	}
	
	static WorkerStatusChange pullFromDatabase(ColumnList<String> columns) {
		String status = columns.getStringValue(WorkerGroup.COL_NAME_STATUSCHANGE, "");
		if (status.equals("")) {
			return null;
		}
		return fromString(status);
	}
	
	public static WorkerStatusChange fromString(String status) {
		if (status == null) {
			throw new NullPointerException("\"status\" can't to be null");
		}
		if (status.equalsIgnoreCase("ENABLED")) {
			return ENABLED;
		} else if (status.equalsIgnoreCase("DISABLED")) {
			return DISABLED;
		} else {
			throw new NumberFormatException("Bad status value : " + status);
		}
	}
	
}
