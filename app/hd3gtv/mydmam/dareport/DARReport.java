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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;

public class DARReport {
	
	String account_user_key;
	String account_job;
	long created_at;
	String event_name;
	/**
	 * Maybe null
	 */
	ArrayList<Content> content;
	
	/**
	 * For archive reasons
	 */
	String account_user_name;
	/**
	 * For archive reasons
	 */
	String account_job_name;
	
	class Content {
		String question;
		boolean check;
		String comment;
		
		private Content() {
		}
	}
	
	void addContent(String question, boolean check, String comment) {
		if (content == null) {
			content = new ArrayList<>();
		}
		if (question == null) {
			throw new NullPointerException("\"question\" can't to be null");
		}
		if (comment == null) {
			throw new NullPointerException("\"comment\" can't to be null");
		}
		
		Content c = new Content();
		c.question = question;
		c.check = check;
		c.comment = comment;
		content.add(c);
	}
	
	DARReport() {
	}
	
	/**
	 * Sorted by jobs position in configuration
	 * @return reports
	 */
	static List<DARReport> sortDARReport(List<DARReport> reports) {
		List<String> job_list = DARDB.get().getJobs().keySet().stream().collect(Collectors.toList());
		
		reports.sort((a, b) -> {
			int job_pos_a = job_list.indexOf(a.account_job);
			int job_pos_b = job_list.indexOf(b.account_job);
			
			if (job_pos_a == -1 && job_pos_b > -1) {
				return -1;
			} else if (job_pos_a > -1 && job_pos_b == -1) {
				return 1;
			} else if (job_pos_a == -1 && job_pos_b == -1) {
				return 0;
			}
			
			return job_pos_a - job_pos_b;
		});
		
		return reports;
	}
	
	static String getKey(String account_user_key, String event_name) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(account_user_key.getBytes("UTF-8"));
			md.update(";".getBytes());
			md.update(event_name.getBytes("UTF-8"));
			return "event:" + MyDMAM.byteToString(md.digest());
		} catch (NoSuchAlgorithmException e) {
		} catch (UnsupportedEncodingException e) {
		}
		return null;
	}
	
	public DARReport save() throws ConnectionException {
		MutationBatch mutator = DARDB.get().getKeyspace().prepareMutationBatch();
		mutator.withRow(DARDB.CF_DAR, getKey(account_user_key, event_name)).putColumn("json-report", MyDMAM.gson_kit.getGsonSimple().toJson(this), DARDB.TTL);
		mutator.withRow(DARDB.CF_DAR, getKey(account_user_key, event_name)).putColumn("account_user_key", account_user_key, DARDB.TTL);
		mutator.withRow(DARDB.CF_DAR, getKey(account_user_key, event_name)).putColumn("event_name", event_name, DARDB.TTL);
		mutator.execute();
		return this;
	}
	
	public static DARReport get(String account_user_key, String event_name) throws ConnectionException {
		ColumnList<String> row = DARDB.get().getKeyspace().prepareQuery(DARDB.CF_DAR).getKey(getKey(account_user_key, event_name)).withColumnSlice("json-report").execute().getResult();
		if (row == null) {
			return null;
		}
		if (row.isEmpty()) {
			return null;
		}
		
		return MyDMAM.gson_kit.getGsonSimple().fromJson(row.getStringValue("json-report", "{}"), DARReport.class);
	}
	
	public static ArrayList<DARReport> getAll(String account_user_key, Collection<String> events_name) throws ConnectionException {
		List<String> keys = events_name.stream().map(name -> {
			return getKey(account_user_key, name);
		}).collect(Collectors.toList());
		
		Rows<String, String> rows = DARDB.get().getKeyspace().prepareQuery(DARDB.CF_DAR).getKeySlice(keys).withColumnSlice("json-report").execute().getResult();
		
		ArrayList<DARReport> result = new ArrayList<DARReport>();
		for (Row<String, String> row : rows) {
			result.add(MyDMAM.gson_kit.getGsonSimple().fromJson(row.getColumns().getStringValue("json-report", "{}"), DARReport.class));
		}
		return result;
	}
	
	public static void delete(String key) throws ConnectionException {
		MutationBatch mutator = DARDB.get().getKeyspace().prepareMutationBatch();
		mutator.withRow(DARDB.CF_DAR, key).delete();
		mutator.execute();
	}
	
	public static ArrayList<DARReport> list() throws ConnectionException {
		ArrayList<DARReport> result = new ArrayList<DARReport>();
		Rows<String, String> rows = DARDB.get().getKeyspace().prepareQuery(DARDB.CF_DAR).getAllRows().withColumnSlice("json-report").execute().getResult();
		for (Row<String, String> row : rows) {
			result.add(MyDMAM.gson_kit.getGsonSimple().fromJson(row.getColumns().getStringValue("json-report", "{}"), DARReport.class));
		}
		return result;
	}
	
	static void truncate() throws ConnectionException {
		MutationBatch mutator = DARDB.get().getKeyspace().prepareMutationBatch();
		
		Rows<String, String> rows = DARDB.get().getKeyspace().prepareQuery(DARDB.CF_DAR).getAllRows().withColumnSlice("json-report").execute().getResult();
		for (Row<String, String> row : rows) {
			mutator.withRow(DARDB.CF_DAR, row.getKey()).delete();
		}
		mutator.execute();
	}
	
	public static HashMap<String, ArrayList<String>> listAuthorsByEvents() throws ConnectionException {
		HashMap<String, ArrayList<String>> result = new HashMap<String, ArrayList<String>>();
		Rows<String, String> rows = DARDB.get().getKeyspace().prepareQuery(DARDB.CF_DAR).getAllRows().withColumnSlice("account_user_key", "event_name").execute().getResult();
		for (Row<String, String> row : rows) {
			String account_user_key = row.getColumns().getStringValue("account_user_key", "");
			String event_name = row.getColumns().getStringValue("event_name", "");
			if (event_name.equals("") | account_user_key.equals("")) {
				Loggers.DAReport.warn("Empty values for event_name or account_user_key in [" + row.getKey() + "]");
				continue;
			}
			
			if (result.containsKey(event_name) == false) {
				result.put(event_name, new ArrayList<>(5));
			}
			
			result.get(event_name).add(account_user_key);
		}
		return result;
	}
	
	public static ArrayList<DARReport> listByEventname(String event_name) throws ConnectionException {
		ArrayList<DARReport> result = new ArrayList<DARReport>();
		Rows<String, String> rows = DARDB.get().getKeyspace().prepareQuery(DARDB.CF_DAR).getAllRows().withColumnSlice("json-report", "event_name").execute().getResult();
		for (Row<String, String> row : rows) {
			if (row.getColumns().getStringValue("event_name", "").equals(event_name)) {
				result.add(MyDMAM.gson_kit.getGsonSimple().fromJson(row.getColumns().getStringValue("json-report", "{}"), DARReport.class));
			}
		}
		return result;
	}
	
	public static LinkedHashMap<String, ArrayList<DARReport>> listByEventsname(Collection<String> events_name) throws ConnectionException {
		LinkedHashMap<String, ArrayList<DARReport>> result = new LinkedHashMap<>();
		Rows<String, String> rows = DARDB.get().getKeyspace().prepareQuery(DARDB.CF_DAR).getAllRows().withColumnSlice("json-report", "event_name").execute().getResult();
		for (Row<String, String> row : rows) {
			String current_event_name = row.getColumns().getStringValue("event_name", "");
			if (events_name.contains(current_event_name)) {
				if (result.containsKey(current_event_name) == false) {
					result.put(current_event_name, new ArrayList<>(1));
				}
				result.get(current_event_name).add(MyDMAM.gson_kit.getGsonSimple().fromJson(row.getColumns().getStringValue("json-report", "{}"), DARReport.class));
			}
		}
		return result;
	}
	
}
