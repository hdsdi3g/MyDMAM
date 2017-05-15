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

import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;

import hd3gtv.mydmam.MyDMAM;

public class DARAccount {
	
	String userkey;
	String jobkey;
	long created_at;
	
	static String getKey(String userkey) {
		return "account:" + userkey;
	}
	
	public DARAccount save() throws ConnectionException {
		MutationBatch mutator = DARDB.get().getKeyspace().prepareMutationBatch();
		mutator.withRow(DARDB.CF_DAR, getKey(userkey)).putColumn("json-account", MyDMAM.gson_kit.getGsonSimple().toJson(this));
		mutator.execute();
		return this;
	}
	
	public static DARAccount get(String userkey) throws ConnectionException {
		ColumnList<String> row = DARDB.get().getKeyspace().prepareQuery(DARDB.CF_DAR).getKey(getKey(userkey)).execute().getResult();
		if (row == null) {
			return null;
		}
		if (row.isEmpty()) {
			return null;
		}
		
		return MyDMAM.gson_kit.getGsonSimple().fromJson(row.getStringValue("json-account", "{}"), DARAccount.class);
	}
	
	public static void delete(String user_key) throws ConnectionException {
		MutationBatch mutator = DARDB.get().getKeyspace().prepareMutationBatch();
		mutator.withRow(DARDB.CF_DAR, getKey(user_key)).delete();
		mutator.execute();
	}
	
	public static ArrayList<DARAccount> list() throws ConnectionException {
		ArrayList<DARAccount> result = new ArrayList<DARAccount>();
		Rows<String, String> rows = DARDB.get().getKeyspace().prepareQuery(DARDB.CF_DAR).getAllRows().withColumnSlice("json-account").execute().getResult();
		for (Row<String, String> row : rows) {
			result.add(MyDMAM.gson_kit.getGsonSimple().fromJson(row.getColumns().getStringValue("json-account", "{}"), DARAccount.class));
		}
		return result;
	}
	
	public String getJobKey() {
		return jobkey;
	}
}
