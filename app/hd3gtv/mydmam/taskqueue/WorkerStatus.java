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

public enum WorkerStatus {
	
	PROCESSING, WAITING, STOPPED, PENDING_STOP, PENDING_CANCEL_TASK;
	
	void pushToDatabase(MutationBatch mutator, String worker_ref, int ttl) {
		mutator.withRow(Broker.CF_WORKERGROUPS, worker_ref).putColumnIfNotNull("status", this.name().toUpperCase(), ttl);
	}
	
	static WorkerStatus pullFromDatabase(ColumnList<String> columns) {
		String status = columns.getStringValue("status", "");
		if (status.equalsIgnoreCase("PROCESSING")) {
			return PROCESSING;
		} else if (status.equalsIgnoreCase("STOPPED")) {
			return STOPPED;
		} else if (status.equalsIgnoreCase("PENDING_STOP")) {
			return PENDING_STOP;
		} else if (status.equalsIgnoreCase("PENDING_CANCEL_TASK")) {
			return PENDING_CANCEL_TASK;
		} else if (status.equalsIgnoreCase("WAITING")) {
			return WAITING;
		} else {
			throw new NumberFormatException("Bad status value : " + status);
		}
	}
}
