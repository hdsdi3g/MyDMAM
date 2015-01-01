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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.mydmam.manager;

import hd3gtv.log2.Log2;
import hd3gtv.mydmam.db.AllRowsFoundRow;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.useraction.UAFunctionalityDefinintion;
import hd3gtv.mydmam.useraction.UAManager;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonObject;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.query.AllRowsQuery;
import com.netflix.astyanax.serializers.StringSerializer;

public final class DatabaseLayer {
	
	private static final ColumnFamily<String, String> CF_INSTANCES = new ColumnFamily<String, String>("mgrInstances", StringSerializer.get(), StringSerializer.get());
	private static final ColumnFamily<String, String> CF_WORKERS = new ColumnFamily<String, String>("mgrWorkers", StringSerializer.get(), StringSerializer.get());
	private static final InstanceStatus.Serializer instancestatus_serializer;
	private static final WorkerExporter.Serializer workerstatus_serializer;
	private static Keyspace keyspace;
	static {
		try {
			keyspace = CassandraDb.getkeyspace();
			String default_keyspacename = CassandraDb.getDefaultKeyspacename();
			if (CassandraDb.isColumnFamilyExists(keyspace, CF_WORKERS.getName()) == false) {
				CassandraDb.createColumnFamilyString(default_keyspacename, CF_WORKERS.getName(), false);
			}
			if (CassandraDb.isColumnFamilyExists(keyspace, CF_INSTANCES.getName()) == false) {
				CassandraDb.createColumnFamilyString(default_keyspacename, CF_INSTANCES.getName(), false);
			}
		} catch (Exception e) {
			Log2.log.error("Can't init database CFs", e);
		}
		instancestatus_serializer = new InstanceStatus.Serializer();
		workerstatus_serializer = new WorkerExporter.Serializer();
	}
	
	private AppManager manager;
	
	DatabaseLayer(AppManager manager) {
		this.manager = manager;
	}
	
	private <T> void exportToDatabase(ColumnFamily<String, String> cf, CassandraDbImporterExporter<T> serializer, T item) {
		try {
			MutationBatch mutator = CassandraDb.prepareMutationBatch();
			serializer.exportToDatabase(item, mutator.withRow(cf, serializer.getDatabaseKey(item)));
			mutator.execute();
		} catch (ConnectionException e) {
			manager.getServiceException().onCassandraError(e);
		}
	}
	
	private <T> void exportToDatabase(ColumnFamily<String, String> cf, CassandraDbImporterExporter<T> serializer, List<T> item) {
		if (item == null) {
			Log2.log.error("Can't store a null item", new NullPointerException("item"));
			return;
		}
		if (item.isEmpty()) {
			return;
		}
		try {
			MutationBatch mutator = CassandraDb.prepareMutationBatch();
			for (int pos = 0; pos < item.size(); pos++) {
				serializer.exportToDatabase(item.get(pos), mutator.withRow(cf, serializer.getDatabaseKey(item.get(pos))));
			}
			mutator.execute();
		} catch (ConnectionException e) {
			manager.getServiceException().onCassandraError(e);
		}
	}
	
	/**
	 * @ return true if import is ok
	 */
	/*private <T> T importFromDatabase(ColumnFamily<String, String> cf, CassandraDbImporterExporter<T> serializer, String key, Class<T> result_class) {
		try {
			ColumnFamilyQuery<String, String> rows_asset = CassandraDb.getkeyspace().prepareQuery(cf);
			OperationResult<ColumnList<String>> row = rows_asset.getKey(key).execute(); // .withColumnSlice(this.column_names)
			return serializer.importFromDatabase(row.getResult());
		} catch (ConnectionException e) {
			manager.getServiceException().onCassandraError(e);
		}
		return null;
	}*/
	
	/**
	 * @param T must assignable from CassandraDbImporterExporter, and to be a valid instanciable class.
	 */
	private <T> List<T> importAllFromDatabase(ColumnFamily<String, String> cf, final CassandraDbImporterExporter<T> serializer, final Class<T> result_class) {
		try {
			final List<T> result = new ArrayList<T>();
			
			CassandraDb.allRowsReader(cf, new AllRowsFoundRow() {
				public void onFoundRow(Row<String, String> row) throws Exception {
					result.add(serializer.importFromDatabase(row.getColumns()));
				}
			});
			return result;
		} catch (Exception e) {
			manager.getServiceException().onCassandraError(e);
		}
		return null;
	}
	
	void updateInstanceStatus(InstanceStatus instance_status) {
		if (instance_status == null) {
			throw new NullPointerException("\"instance_statuses\" can't to be null");
		}
		exportToDatabase(CF_INSTANCES, instancestatus_serializer, instance_status);
	}
	
	List<InstanceStatus> getAllInstancesStatus() {
		return importAllFromDatabase(CF_INSTANCES, instancestatus_serializer, InstanceStatus.class);
	}
	
	void updateWorkerStatus(List<WorkerNG> workers) {
		if (workers == null) {
			throw new NullPointerException("\"workers\" can't to be null");
		}
		ArrayList<WorkerExporter> worker_statuses = new ArrayList<WorkerExporter>();
		for (int pos = 0; pos < workers.size(); pos++) {
			worker_statuses.add(workers.get(pos).getExporter());
		}
		exportToDatabase(CF_WORKERS, workerstatus_serializer, worker_statuses);
	}
	
	List<WorkerExporter> getAllWorkerStatus() {
		return importAllFromDatabase(CF_WORKERS, workerstatus_serializer, WorkerExporter.class);
	}
	
	static WorkerExporter getWorkerStatusByKey(String worker_key) throws ConnectionException {
		ColumnList<String> cols = keyspace.prepareQuery(CF_WORKERS).getKey(worker_key).execute().getResult();
		if (cols.isEmpty()) {
			return null;
		}
		return workerstatus_serializer.importFromDatabase(cols);
	}
	
	public static void truncateAll() throws ConnectionException {
		CassandraDb.truncateColumnFamilyString(keyspace, CF_INSTANCES.getName());
		CassandraDb.truncateColumnFamilyString(keyspace, CF_WORKERS.getName());
	}
	
	public static String getCurrentAvailabilitiesAsJsonString(ArrayList<String> privileges_for_user) throws ConnectionException {
		if (privileges_for_user == null) {
			return "{}";
		}
		if (privileges_for_user.isEmpty()) {
			return "{}";
		}
		
		Type useraction_functionality_list_typeOfT = new TypeToken<List<UAFunctionalityDefinintion>>() {
		}.getType();
		
		AllRowsQuery<String, String> all_rows = CassandraDb.getkeyspace().prepareQuery(CF_INSTANCES).getAllRows().withColumnSlice(InstanceStatus.COL_NAME_UA_LIST);
		OperationResult<Rows<String, String>> rows = all_rows.execute();
		
		Map<String, List<UAFunctionalityDefinintion>> all = new HashMap<String, List<UAFunctionalityDefinintion>>();
		
		List<UAFunctionalityDefinintion> list;
		for (Row<String, String> row : rows.getResult()) {
			Column<String> col = row.getColumns().getColumnByName("useraction_functionality_list");
			if (col == null) {
				continue;
			}
			list = UAManager.getGson().fromJson(col.getStringValue(), useraction_functionality_list_typeOfT);
			
			for (int pos = list.size() - 1; pos > -1; pos--) {
				if (privileges_for_user.contains(list.get(pos).classname) == false) {
					list.remove(pos);
				}
			}
			all.put(row.getKey(), list);
		}
		
		List<UAFunctionalityDefinintion> merged_definitions = new ArrayList<UAFunctionalityDefinintion>();
		List<UAFunctionalityDefinintion> current_definitions;
		for (Map.Entry<String, List<UAFunctionalityDefinintion>> entry : all.entrySet()) {
			current_definitions = entry.getValue();
			for (int pos_current = 0; pos_current < current_definitions.size(); pos_current++) {
				UAFunctionalityDefinintion.mergueInList(merged_definitions, current_definitions.get(pos_current));
			}
		}
		
		JsonObject result = new JsonObject();
		JsonObject result_implementation;
		JsonObject result_capability;
		JsonObject result_configurator;
		UAFunctionalityDefinintion current;
		
		for (int pos = 0; pos < merged_definitions.size(); pos++) {
			current = merged_definitions.get(pos);
			
			result_implementation = new JsonObject();
			result_implementation.addProperty("messagebasename", current.messagebasename);
			result_implementation.addProperty("section", current.section.name());
			result_implementation.addProperty("powerful_and_dangerous", current.powerful_and_dangerous);
			
			result_capability = (JsonObject) UAManager.getGson().toJsonTree(current.capability);
			result_capability.remove("musthavelocalstorageindexbridge");
			result_implementation.add("capability", result_capability);
			
			result_configurator = (JsonObject) UAManager.getGson().toJsonTree(current.configurator);
			result_configurator.remove("type");
			result_configurator.remove("origin");
			result_implementation.add("configurator", result_configurator);
			
			result.add(current.classname, result_implementation);
		}
		
		return UAManager.getGson().toJson(result);
	}
	
}
