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

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonArray;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;

import hd3gtv.mydmam.MyDMAM;

public class DARReport {
	
	String account_name;
	long creation_date;
	String event_name;
	JsonArray content;
	
	private static final int TTL = (int) TimeUnit.DAYS.toSeconds(30 * 6);
	
	static String getKey(String account_name, String event_name) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(account_name.getBytes("UTF-8"));
			md.update(";".getBytes());
			md.update(event_name.getBytes("UTF-8"));
			return "event:" + MyDMAM.byteToString(md.digest());
		} catch (NoSuchAlgorithmException e) {
		} catch (UnsupportedEncodingException e) {
		}
		return null;
	}
	
	public DARReport save() throws ConnectionException {
		MutationBatch mutator = DARDB.getKeyspace().prepareMutationBatch();
		mutator.withRow(DARDB.CF_DAR, getKey(account_name, event_name)).putColumn("json", MyDMAM.gson_kit.getGsonSimple().toJson(this), TTL);
		mutator.execute();
		return this;
	}
	
	public static DARReport get(String account_name, String event_name) throws ConnectionException {
		ColumnList<String> row = DARDB.getKeyspace().prepareQuery(DARDB.CF_DAR).getKey(getKey(account_name, event_name)).execute().getResult();
		if (row == null) {
			return null;
		}
		if (row.isEmpty()) {
			return null;
		}
		
		return MyDMAM.gson_kit.getGsonSimple().fromJson(row.getStringValue("json", "{}"), DARReport.class);
	}
	
	public static void delete(String key) throws ConnectionException {
		MutationBatch mutator = DARDB.getKeyspace().prepareMutationBatch();
		mutator.withRow(DARDB.CF_DAR, key).delete();
		mutator.execute();
	}
	
	public static ArrayList<DARReport> list() throws ConnectionException {
		ArrayList<DARReport> result = new ArrayList<DARReport>();
		Rows<String, String> rows = DARDB.getKeyspace().prepareQuery(DARDB.CF_DAR).getAllRows().execute().getResult();
		for (Row<String, String> row : rows) {
			result.add(MyDMAM.gson_kit.getGsonSimple().fromJson(row.getColumns().getStringValue("json", "{}"), DARReport.class));
		}
		return result;
	}
	
	// TODO get all reports for an event name
	
}
