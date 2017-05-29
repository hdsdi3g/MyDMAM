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
 * Copyright (C) hdsdi3g for hd3g.tv 2015
 * 
*/
package hd3gtv.mydmam.accesscontrol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.serializers.StringSerializer;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.db.CassandraDb;

public class AccessControlEntry {
	
	private static final ColumnFamily<String, String> CF_ACCESS_CONTROL = new ColumnFamily<String, String>("accessControl", StringSerializer.get(), StringSerializer.get());
	private static Keyspace keyspace;
	
	static {
		try {
			keyspace = CassandraDb.getkeyspace();
			String default_keyspacename = CassandraDb.getDefaultKeyspacename();
			if (CassandraDb.isColumnFamilyExists(keyspace, CF_ACCESS_CONTROL.getName()) == false) {
				CassandraDb.createColumnFamilyString(default_keyspacename, CF_ACCESS_CONTROL.getName(), true);
			}
		} catch (Exception e) {
			Loggers.AccessControl.error("Can't init database CFs", e);
			System.exit(1);
		}
	}
	
	private String address;
	private long first_attempt;
	private long last_attempt;
	private String last_attempt_username;
	private int attempt;
	
	private AccessControlEntry() {
	}
	
	private AccessControlEntry save() throws ConnectionException {
		MutationBatch mutator = keyspace.prepareMutationBatch();
		mutator.withRow(CF_ACCESS_CONTROL, address).putColumn("first_attempt", first_attempt);
		mutator.withRow(CF_ACCESS_CONTROL, address).putColumn("last_attempt", last_attempt);
		mutator.withRow(CF_ACCESS_CONTROL, address).putColumn("last_attempt_username", last_attempt_username);
		mutator.withRow(CF_ACCESS_CONTROL, address).putColumn("attempt", attempt);
		mutator.execute();
		return this;
	}
	
	private AccessControlEntry importFromDb(String key, ColumnList<String> cols) {
		address = key;
		first_attempt = cols.getLongValue("first_attempt", 0l);
		last_attempt = cols.getLongValue("last_attempt", 0l);
		last_attempt_username = cols.getStringValue("last_attempt_username", "");
		attempt = cols.getIntegerValue("attempt", 0);
		return this;
	}
	
	public static List<AccessControlEntry> getAll() throws ConnectionException {
		ArrayList<AccessControlEntry> result = new ArrayList<AccessControlEntry>();
		Rows<String, String> rows = keyspace.prepareQuery(CF_ACCESS_CONTROL).getAllRows().execute().getResult();
		for (Row<String, String> row : rows) {
			result.add(new AccessControlEntry().importFromDb(row.getKey(), row.getColumns()));
		}
		return result;
	}
	
	static AccessControlEntry getFromAdress(String address) throws ConnectionException {
		ColumnList<String> row = keyspace.prepareQuery(CF_ACCESS_CONTROL).getKey(address).execute().getResult();
		if (row == null) {
			return null;
		}
		if (row.isEmpty()) {
			return null;
		}
		AccessControlEntry entry = new AccessControlEntry();
		return entry.importFromDb(address, row);
	}
	
	static AccessControlEntry create(String address, String login_name) throws ConnectionException {
		if (address == null) {
			throw new NullPointerException("\"address\" can't to be null");
		}
		if (login_name == null) {
			throw new NullPointerException("\"login_name\" can't to be null");
		}
		AccessControlEntry entry = new AccessControlEntry();
		entry.address = address;
		entry.last_attempt_username = login_name;
		entry.first_attempt = System.currentTimeMillis();
		entry.last_attempt = entry.first_attempt;
		entry.attempt = 1;
		entry.save();
		return entry;
	}
	
	AccessControlEntry update(String login_name) throws ConnectionException {
		attempt++;
		last_attempt = System.currentTimeMillis();
		last_attempt_username = login_name;
		save();
		return this;
	}
	
	void delete() throws ConnectionException {
		MutationBatch mutator = keyspace.prepareMutationBatch();
		mutator.withRow(CF_ACCESS_CONTROL, address).delete();
		mutator.execute();
	}
	
	public static void delete(String address) throws ConnectionException {
		MutationBatch mutator = keyspace.prepareMutationBatch();
		mutator.withRow(CF_ACCESS_CONTROL, address).delete();
		mutator.execute();
	}
	
	public int getAttempt() {
		return attempt;
	}
	
	public long getFirstAttemptDate() {
		return first_attempt;
	}
	
	public long getLastAttemptDate() {
		return last_attempt;
	}
	
	public String getLastAttemptUsername() {
		return last_attempt_username;
	}
	
	public String getAddress() {
		return address;
	}
	
	public String toString() {
		LinkedHashMap<String, Object> log = new LinkedHashMap<String, Object>();
		log.put("address", address);
		log.put("attempt", attempt);
		log.put("first_attempt", Loggers.dateLog(first_attempt));
		log.put("last_attempt", Loggers.dateLog(last_attempt));
		log.put("last_attempt_username", last_attempt_username);
		return log.toString();
	}
	
	public String statusForThisIP() {
		if (attempt > AccessControl.max_attempt_for_blocking_addr) {
			return "Too many attempts from this IP: BLOCKED";
		}
		if (attempt <= AccessControl.grace_attempt_count) {
			return "Under the grace attempt count: not blocked";
		}
		if ((last_attempt + (long) ((attempt - AccessControl.grace_attempt_count) * AccessControl.grace_period_factor_time * 1000)) > (System.currentTimeMillis())) {
			return "Too early from the grace period: BLOCKED";
		}
		return "After the grace period: not blocked";
	}
	
}
