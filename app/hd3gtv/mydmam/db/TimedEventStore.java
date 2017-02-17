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
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
*/
package hd3gtv.mydmam.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Consumer;

import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.serializers.StringSerializer;

import hd3gtv.mydmam.Loggers;

public class TimedEventStore {
	
	private Keyspace keyspace;
	private ColumnFamily<String, String> cf;
	private long min_event_date;
	private int default_ttl;
	private static final String col_name_event_date = "_event_date";
	
	public TimedEventStore(Keyspace keyspace, String cf_name) throws ConnectionException {
		this(keyspace, cf_name, 0);
	}
	
	public TimedEventStore(Keyspace keyspace, String cf_name, long max_event_age) throws ConnectionException {
		this.keyspace = keyspace;
		if (keyspace == null) {
			throw new NullPointerException("\"keyspace\" can't to be null");
		}
		if (cf_name == null) {
			throw new NullPointerException("\"cf_name\" can't to be null");
		}
		cf = new ColumnFamily<String, String>(cf_name, StringSerializer.get(), StringSerializer.get());
		if (CassandraDb.isColumnFamilyExists(keyspace, cf_name) == false) {
			Loggers.Cassandra.info("Create Cassandra CF " + cf_name);
			CassandraDb.createColumnFamilyString(keyspace.getKeyspaceName(), cf_name, false);
		}
		if (max_event_age > 0) {
			min_event_date = System.currentTimeMillis() - max_event_age;
			default_ttl = (int) max_event_age;
		}
	}
	
	/**
	 * Don't forget to close or createAnother.
	 * You must set max_event_age in constructor.
	 * @return null if event is too old.
	 */
	public TimedEvent createEvent(String event_key, long date) throws ConnectionException {
		if (isTooOld(date)) {
			return null;
		}
		return new TimedEvent(event_key, date);
	}
	
	/**
	 * You must set max_event_age in constructor
	 */
	public boolean isTooOld(long date) {
		if (min_event_date == 0) {
			throw new NullPointerException("You can't use this without set max_event_age in constructor");
		}
		return date < min_event_date;
	}
	
	/**
	 * You must set max_event_age in constructor
	 */
	public class TimedEvent {
		
		private MutationBatch mutator;
		private String event_key;
		private long date;
		private int computed_ttl;
		
		private TimedEvent(String event_key, long date) throws ConnectionException {
			if (default_ttl == 0) {
				throw new NullPointerException("You can't use this without set max_event_age in constructor");
			}
			
			mutator = CassandraDb.prepareMutationBatch(keyspace.getKeyspaceName());
			this.event_key = event_key;
			if (event_key == null) {
				throw new NullPointerException("\"event_key\" can't to be null");
			}
			this.date = date;
			processDate();
			getMutator().putColumn(col_name_event_date, date);
		}
		
		private TimedEvent(MutationBatch mutator, String event_key, long date) {
			if (default_ttl == 0) {
				throw new NullPointerException("You can't use this without set max_event_age in constructor");
			}
			
			this.mutator = mutator;
			this.event_key = event_key;
			if (event_key == null) {
				throw new NullPointerException("\"event_key\" can't to be null");
			}
			this.date = date;
			processDate();
			getMutator().putColumn(col_name_event_date, date);
		}
		
		/**
		 * Don't forget to close or createAnother.
		 * @return null if event is too old.
		 */
		public TimedEvent createAnother(String event_key, long date) {
			if (isTooOld(date)) {
				return null;
			}
			return new TimedEvent(mutator, event_key, date);
		}
		
		private void processDate() {
			if (System.currentTimeMillis() < date) {
				/**
				 * Asrun
				 */
				computed_ttl = default_ttl - (int) (System.currentTimeMillis() - date);
			} else if (System.currentTimeMillis() < date) {
				/**
				 * Playlist
				 */
				computed_ttl = default_ttl + (int) (date - System.currentTimeMillis());
			} else {
				computed_ttl = default_ttl;
			}
		}
		
		public void close() throws ConnectionException {
			if (mutator.isEmpty()) {
				return;
			}
			mutator.execute();
		}
		
		/**
		 * ttl is already set.
		 */
		public ColumnListMutation<String> getMutator() {
			return mutator.withRow(cf, event_key).setDefaultTtl(computed_ttl);
		}
		
	}
	
	public class TimedEventFromDb {
		
		private String event_key;
		private ColumnList<String> cols;
		private long date;
		
		private TimedEventFromDb(String event_key, ColumnList<String> cols) {
			this.event_key = event_key;
			this.cols = cols;
			date = cols.getLongValue(col_name_event_date, 0l);
			if (date == 0) {
				throw new IndexOutOfBoundsException("No event date !");
			}
		}
		
		public String getKey() {
			return event_key;
		}
		
		public ColumnList<String> getCols() {
			return cols;
		}
		
		public long getDate() {
			return date;
		}
		
	}
	
	private static final EventDateComparator comparator = new EventDateComparator();
	
	private static class EventDateComparator implements Comparator<TimedEventFromDb> {
		public int compare(TimedEventFromDb o1, TimedEventFromDb o2) {
			if (o1.date < o2.date) {
				return -1;
			}
			if (o1.date > o2.date) {
				return 1;
			}
			return 0;
		}
	}
	
	/**
	 * @return sorted
	 */
	public ArrayList<TimedEventFromDb> getAll() throws Exception {
		final ArrayList<TimedEventFromDb> result = new ArrayList<>();
		
		CassandraDb.allRowsReader(cf, new AllRowsFoundRow() {
			public void onFoundRow(Row<String, String> row) throws Exception {
				result.add(new TimedEventFromDb(row.getKey(), row.getColumns()));
			}
		});
		
		result.sort(comparator);
		return result;
	}
	
	/**
	 * @return sorted
	 */
	public ArrayList<TimedEventFromDb> getByKeys(Collection<String> keys) throws Exception {
		final ArrayList<TimedEventFromDb> result = new ArrayList<>();
		
		if (keys == null) {
			return result;
		}
		if (keys.isEmpty()) {
			return result;
		}
		
		Rows<String, String> rows = keyspace.prepareQuery(cf).getKeySlice(keys).execute().getResult();
		rows.forEach(r -> {
			result.add(new TimedEventFromDb(r.getKey(), r.getColumns()));
		});
		
		result.sort(comparator);
		return result;
	}
	
	/**
	 * @return non date sorted
	 */
	public void getAllFutureKeys(Consumer<String> result) throws Exception {
		CassandraDb.allRowsReader(cf, new AllRowsFoundRow() {
			public void onFoundRow(Row<String, String> row) throws Exception {
				long date = row.getColumns().getColumnByName(col_name_event_date).getLongValue();
				if (date > System.currentTimeMillis()) {
					result.accept(row.getKey());
				}
			}
		}, col_name_event_date);
	}
	
}
