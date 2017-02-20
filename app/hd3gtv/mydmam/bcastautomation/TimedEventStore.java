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
package hd3gtv.mydmam.bcastautomation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Function;

import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.serializers.StringSerializer;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.tools.TableList;
import hd3gtv.tools.TableList.Row;
import hd3gtv.tools.TimeUtils;

public class TimedEventStore {
	
	private Keyspace keyspace;
	private ColumnFamily<String, String> cf;
	private long min_event_date;
	
	/**
	 * In ms
	 */
	private long default_ttl;
	private static final String col_name_event_start_date = "_event_start_date";
	private static final String col_name_event_end_date = "_event_end_date";
	private static final String col_name_event_aired = "_event_aired";
	
	public TimedEventStore(Keyspace keyspace, String cf_name) throws ConnectionException {
		this(keyspace, cf_name, 0);
	}
	
	/**
	 * @param max_event_age in ms
	 */
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
			default_ttl = max_event_age;
		}
	}
	
	/**
	 * Don't forget to close or createAnother.
	 * You must set max_event_age in constructor.
	 * @return null if event is too old.
	 */
	public TimedEvent createEvent(String event_key, long start_date, long duration) throws ConnectionException {
		if (isTooOld(start_date)) {
			return null;
		}
		return new TimedEvent(event_key, start_date, duration);
	}
	
	/**
	 * You must set max_event_age in constructor
	 */
	public boolean isTooOld(long start_date) {
		if (min_event_date == 0) {
			throw new NullPointerException("You can't use this without set max_event_age in constructor");
		}
		return start_date < min_event_date;
	}
	
	/**
	 * You must set max_event_age in constructor
	 */
	public class TimedEvent {
		
		private MutationBatch mutator;
		private String event_key;
		private int computed_ttl;
		
		private TimedEvent(String event_key, long start_date, long duration) throws ConnectionException {
			if (default_ttl == 0) {
				throw new NullPointerException("You can't use this without set max_event_age in constructor");
			}
			
			mutator = CassandraDb.prepareMutationBatch(keyspace.getKeyspaceName());
			init(event_key, start_date, duration);
		}
		
		private TimedEvent(MutationBatch mutator, String event_key, long start_date, long duration) {
			if (default_ttl == 0) {
				throw new NullPointerException("You can't use this without set max_event_age in constructor");
			}
			this.mutator = mutator;
			init(event_key, start_date, duration);
		}
		
		private void init(String event_key, long start_date, long duration) {
			this.event_key = event_key;
			if (event_key == null) {
				throw new NullPointerException("\"event_key\" can't to be null");
			}
			
			if (System.currentTimeMillis() > start_date) {
				/**
				 * Asrun
				 */
				computed_ttl = (int) ((default_ttl - (System.currentTimeMillis() - start_date)) / 1000l);
			} else if (System.currentTimeMillis() < start_date) {
				/**
				 * Playlist
				 */
				computed_ttl = (int) ((default_ttl + (start_date - System.currentTimeMillis())) / 1000l);
			} else {
				/**
				 * On air (ultra rare)
				 */
				computed_ttl = (int) (default_ttl / 1000l);
			}
			
			getMutator().putColumn(col_name_event_start_date, start_date);
			getMutator().putColumn(col_name_event_end_date, start_date + duration);
			getMutator().putColumn(col_name_event_aired, start_date + duration < System.currentTimeMillis());
		}
		
		/**
		 * Don't forget to close or createAnother.
		 * @return null if event is too old.
		 */
		public TimedEvent createAnother(String event_key, long start_date, long duration) {
			if (isTooOld(start_date)) {
				return null;
			}
			return new TimedEvent(mutator, event_key, start_date, duration);
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
		private long start_date;
		private long end_date;
		private boolean aired;
		
		private TimedEventFromDb(String event_key, ColumnList<String> cols) {
			this.event_key = event_key;
			this.cols = cols;
			start_date = cols.getLongValue(col_name_event_start_date, 0l);
			end_date = cols.getLongValue(col_name_event_end_date, 0l);
			aired = cols.getBooleanValue(col_name_event_aired, false);
			
			if (start_date == 0 | end_date == 0) {
				throw new IndexOutOfBoundsException("No event date !");
			}
		}
		
		public String getKey() {
			return event_key;
		}
		
		public ColumnList<String> getCols() {
			return cols;
		}
		
		public long getStartDate() {
			return start_date;
		}
		
		public long getEndDate() {
			return end_date;
		}
		
		public boolean isAired() {
			return aired;
		}
		
		/**
		 * table size must ==
		 */
		public void toTable(TableList table, boolean show_key, Function<String, ArrayList<String>> raw_event_reducer) {
			Row row = table.createRow();
			
			if (show_key) {
				row.addCell(event_key);
			}
			
			row.addCell(Loggers.dateLog(start_date));
			
			long from_next_time = end_date - System.currentTimeMillis();
			boolean if_future = from_next_time > 0;
			
			if (if_future) {
				row.addCell("+" + TimeUtils.secondsToYWDHMS(Math.abs(from_next_time) / 1000));
			} else {
				row.addCell("-" + TimeUtils.secondsToYWDHMS(Math.abs(from_next_time) / 1000));
			}
			
			row.addCells(raw_event_reducer.apply(cols.getColumnByName(BCAWatcher.DB_COL_CONTENT_NAME).getStringValue()));
			row.addBoolean(aired, "AIRED", "");
		}
	}
	
	private static final EventDateComparator comparator = new EventDateComparator();
	
	private static class EventDateComparator implements Comparator<TimedEventFromDb> {
		public int compare(TimedEventFromDb o1, TimedEventFromDb o2) {
			if (o1.start_date < o2.start_date) {
				return -1;
			}
			if (o1.start_date > o2.start_date) {
				return 1;
			}
			return 0;
		}
	}
	
	/**
	 * @return sorted, but not past events no-aired
	 */
	public ArrayList<TimedEventFromDb> getFilteredAll() throws Exception {
		final ArrayList<TimedEventFromDb> result = new ArrayList<>();
		
		CassandraDb.allRowsReader(cf, row -> {
			TimedEventFromDb event = new TimedEventFromDb(row.getKey(), row.getColumns());
			if (event.aired == true || event.end_date > System.currentTimeMillis()) {
				result.add(event);
			}
		});
		
		result.sort(comparator);
		return result;
	}
	
	/**
	 * @return sorted, with past events no-aired
	 */
	public ArrayList<TimedEventFromDb> getNonFilteredAll() throws Exception {
		final ArrayList<TimedEventFromDb> result = new ArrayList<>();
		
		CassandraDb.allRowsReader(cf, row -> {
			result.add(new TimedEventFromDb(row.getKey(), row.getColumns()));
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
	public void getAllKeys(Consumer<String> future, Consumer<String> aired, Consumer<String> non_aired) throws Exception {
		CassandraDb.allRowsReader(cf, row -> {
			long end_date = row.getColumns().getColumnByName(col_name_event_end_date).getLongValue();
			if (end_date > System.currentTimeMillis()) {
				future.accept(row.getKey());
			} else {
				boolean isaired = row.getColumns().getColumnByName(col_name_event_aired).getBooleanValue();
				if (isaired) {
					aired.accept(row.getKey());
				} else {
					non_aired.accept(row.getKey());
				}
				
			}
		}, col_name_event_end_date, col_name_event_aired);
	}
	
}
