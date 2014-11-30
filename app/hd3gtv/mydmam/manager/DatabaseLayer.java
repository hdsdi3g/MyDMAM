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

import java.util.ArrayList;
import java.util.List;

import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.query.ColumnFamilyQuery;
import com.netflix.astyanax.serializers.StringSerializer;

public class DatabaseLayer {
	
	private static final ColumnFamily<String, String> CF_INSTANCES = new ColumnFamily<String, String>("mgrInstances", StringSerializer.get(), StringSerializer.get());
	private static final ColumnFamily<String, String> CF_WORKERS = new ColumnFamily<String, String>("mgrWorkers", StringSerializer.get(), StringSerializer.get());
	// private static final ColumnFamily<String, String> CF_ACTIONS = new ColumnFamily<String, String>("mgrAction", StringSerializer.get(), StringSerializer.get());
	private static final InstanceStatus.Serializer instancestatus_serializer;
	private static final WorkerStatus.Serializer workerstatus_serializer;
	
	static {
		try {
			Keyspace keyspace = CassandraDb.getkeyspace();
			String default_keyspacename = CassandraDb.getDefaultKeyspacename();
			if (CassandraDb.isColumnFamilyExists(keyspace, CF_WORKERS.getName()) == false) {
				CassandraDb.createColumnFamilyString(default_keyspacename, CF_WORKERS.getName(), false);
			}
			if (CassandraDb.isColumnFamilyExists(keyspace, CF_INSTANCES.getName()) == false) {
				CassandraDb.createColumnFamilyString(default_keyspacename, CF_INSTANCES.getName(), false);
			}
			/*if (CassandraDb.isColumnFamilyExists(keyspace, CF_QUEUE.getName()) == false) {
				 CassandraDb.createColumnFamilyString(default_keyspacename, CF_QUEUE.getName(), false);
			}*/
			/*if (CassandraDb.isColumnFamilyExists(keyspace, CF_ACTIONS.getName()) == false) {
				 CassandraDb.createColumnFamilyString(default_keyspacename, CF_ACTIONS.getName(), false);
			}*/
		} catch (Exception e) {
			Log2.log.error("Can't init database CFs", e);
		}
		instancestatus_serializer = new InstanceStatus.Serializer();
		workerstatus_serializer = new WorkerStatus.Serializer();
		
		/*
		CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_TASKQUEUE, "status", CF_TASKQUEUE.getName() + "_status", DeployColumnDef.ColType_AsciiType);
		CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_TASKQUEUE, "profile_category", CF_TASKQUEUE.getName() + "_profile_category", DeployColumnDef.ColType_UTF8Type);
		CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_TASKQUEUE, "max_date_to_wait_processing", CF_TASKQUEUE.getName() + "_max_date_to_wait_processing",
				DeployColumnDef.ColType_LongType);
		CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_TASKQUEUE, "creator_hostname", CF_TASKQUEUE.getName() + "_creator_hostname", DeployColumnDef.ColType_UTF8Type);
		CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_TASKQUEUE, "updatedate", CF_TASKQUEUE.getName() + "_updatedate", DeployColumnDef.ColType_LongType);
		CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_TASKQUEUE, "delete", CF_TASKQUEUE.getName() + "_delete", DeployColumnDef.ColType_Int32Type);
		CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_TASKQUEUE, "indexingdebug", CF_TASKQUEUE.getName() + "_indexingdebug", DeployColumnDef.ColType_Int32Type);
		 * */
	}
	
	private static <T> void exportToDatabase(ColumnFamily<String, String> cf, CassandraDbImporterExporter<T> serializer, T item) {
		try {
			MutationBatch mutator = CassandraDb.prepareMutationBatch();
			serializer.exportToDatabase(item, mutator.withRow(cf, serializer.getDatabaseKey(item)));
			mutator.execute();
		} catch (ConnectionException e) {
			error(e);
		}
	}
	
	private static <T> void exportToDatabase(ColumnFamily<String, String> cf, CassandraDbImporterExporter<T> serializer, List<T> item) {
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
			error(e);
		}
	}
	
	/**
	 * @return true if import is ok
	 */
	private static <T> boolean importFromDatabase(ColumnFamily<String, String> cf, CassandraDbImporterExporter<T> serializer, String key, CassandraDbImporterExporter result) {
		try {
			ColumnFamilyQuery<String, String> rows_asset = CassandraDb.getkeyspace().prepareQuery(cf);
			OperationResult<ColumnList<String>> row = rows_asset.getKey(key).execute(); // .withColumnSlice(this.column_names)
			result.importFromDatabase(row.getResult());
			return true;
		} catch (ConnectionException e) {
			error(e);
		}
		return false;
	}
	
	/**
	 * @param T must assignable from CassandraDbImporterExporter, and to be a valid instanciable class.
	 */
	private static <T> List<T> importAllFromDatabase(ColumnFamily<String, String> cf, CassandraDbImporterExporter<T> serializer, final Class<T> result_class) {
		try {
			if (CassandraDbImporterExporter.class.isAssignableFrom(result_class) == false) {
				throw new Exception(result_class.getName() + " is not assignable from " + CassandraDbImporterExporter.class.getName());
			}
			final List<T> result = new ArrayList<T>();
			
			CassandraDb.allRowsReader(cf, new AllRowsFoundRow() {
				@SuppressWarnings("unchecked")
				public void onFoundRow(Row<String, String> row) throws Exception {
					CassandraDbImporterExporter item = (CassandraDbImporterExporter) result_class.newInstance();
					item.importFromDatabase(row.getColumns());
					result.add((T) item);
				}
			});
			return result;
		} catch (Exception e) {
			error(e);
		}
		return null;
	}
	
	private static void error(Exception e) {
		Log2.log.error("Non managed and non fatal error", e);// TODO alert ?
	}
	
	static void updateInstanceStatus(InstanceStatus instance_status) {
		if (instance_status == null) {
			throw new NullPointerException("\"instance_statuses\" can't to be null");
		}
		exportToDatabase(CF_INSTANCES, instancestatus_serializer, instance_status);
	}
	
	public static List<InstanceStatus> getAllInstancesStatus() {
		return importAllFromDatabase(CF_INSTANCES, instancestatus_serializer, InstanceStatus.class);
	}
	
	static void updateWorkerStatus(List<WorkerNG> workers) {
		if (workers == null) {
			throw new NullPointerException("\"workers\" can't to be null");
		}
		ArrayList<WorkerStatus> worker_statuses = new ArrayList<WorkerStatus>();
		for (int pos = 0; pos < workers.size(); pos++) {
			worker_statuses.add(workers.get(pos).getStatus());
		}
		exportToDatabase(CF_WORKERS, workerstatus_serializer, worker_statuses);
	}
	
	public static List<WorkerStatus> getAllWorkerStatus() {
		return importAllFromDatabase(CF_WORKERS, workerstatus_serializer, WorkerStatus.class);
	}
	
}
