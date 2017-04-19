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
package hd3gtv.mydmam.dareport;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;

import hd3gtv.mydmam.MyDMAM;

public class DAREvent {
	
	String name;
	long planned_date;
	long created_at;
	String creator;
	
	private static final int TTL = (int) TimeUnit.DAYS.toSeconds(30 * 6);
	
	static String getKey(String name) {
		return "event:" + name;
	}
	
	public DAREvent save() throws ConnectionException {
		MutationBatch mutator = DARDB.getKeyspace().prepareMutationBatch();
		mutator.withRow(DARDB.CF_DAR, getKey(name)).putColumn("json", MyDMAM.gson_kit.getGsonSimple().toJson(this), TTL);
		mutator.execute();
		return this;
	}
	
	public static DAREvent get(String name) throws ConnectionException {
		ColumnList<String> row = DARDB.getKeyspace().prepareQuery(DARDB.CF_DAR).getKey(getKey(name)).execute().getResult();
		if (row == null) {
			return null;
		}
		if (row.isEmpty()) {
			return null;
		}
		
		return MyDMAM.gson_kit.getGsonSimple().fromJson(row.getStringValue("json", "{}"), DAREvent.class);
	}
	
	public static void delete(String name) throws ConnectionException {
		MutationBatch mutator = DARDB.getKeyspace().prepareMutationBatch();
		mutator.withRow(DARDB.CF_DAR, getKey(name)).delete();
		mutator.execute();
	}
	
	public static ArrayList<DAREvent> list() throws ConnectionException {
		ArrayList<DAREvent> result = new ArrayList<DAREvent>();
		Rows<String, String> rows = DARDB.getKeyspace().prepareQuery(DARDB.CF_DAR).getAllRows().execute().getResult();
		for (Row<String, String> row : rows) {
			result.add(MyDMAM.gson_kit.getGsonSimple().fromJson(row.getColumns().getStringValue("json", "{}"), DAREvent.class));
		}
		return result;
	}
	
}