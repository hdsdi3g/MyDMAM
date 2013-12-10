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
import com.netflix.astyanax.query.IndexQuery;

/**
 * If add new status, don't forget to add it in queue.js order
 */
public enum TaskJobStatus {
	
	TOO_OLD, CANCELED, POSTPONED, WAITING, DONE, PROCESSING, STOPPED, ERROR, PREPARING;
	
	void pushToDatabase(MutationBatch mutator, String key, int ttl) {
		mutator.withRow(Broker.CF_TASKQUEUE, key).putColumnIfNotNull("status", this.name().toUpperCase(), ttl);
	}
	
	static TaskJobStatus pullFromDatabase(ColumnList<String> columns) {
		return fromString(columns.getStringValue("status", "WAITING"));
	}
	
	public static TaskJobStatus fromString(String status) {
		if (status == null) {
			throw new NullPointerException("\"status\" can't to be null");
		}
		if (status.equalsIgnoreCase("TOO_OLD")) {
			return TOO_OLD;
		} else if (status.equalsIgnoreCase("CANCELED")) {
			return CANCELED;
		} else if (status.equalsIgnoreCase("POSTPONED")) {
			return POSTPONED;
		} else if (status.equalsIgnoreCase("DONE")) {
			return DONE;
		} else if (status.equalsIgnoreCase("PROCESSING")) {
			return PROCESSING;
		} else if (status.equalsIgnoreCase("STOPPED")) {
			return STOPPED;
		} else if (status.equalsIgnoreCase("ERROR")) {
			return ERROR;
		} else if (status.equalsIgnoreCase("PREPARING")) {
			return PREPARING;
		} else if (status.equalsIgnoreCase("WAITING")) {
			return WAITING;
		} else {
			throw new NumberFormatException("Bad status value : " + status);
		}
	}
	
	static void selectByName(IndexQuery<String, String> index_query, TaskJobStatus status) {
		if (status == null) {
			throw new NullPointerException("\"status\" can't to be null");
		}
		index_query.addExpression().whereColumn("status").equals().value(status.name().toUpperCase());
	}
	
}
