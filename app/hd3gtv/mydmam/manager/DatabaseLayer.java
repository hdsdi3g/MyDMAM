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
import hd3gtv.mydmam.db.CassandraDb;

import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.serializers.StringSerializer;

public class DatabaseLayer {
	
	private static final ColumnFamily<String, String> CF_INSTANCES = new ColumnFamily<String, String>("manager-instances", StringSerializer.get(), StringSerializer.get());
	private static final ColumnFamily<String, String> CF_WORKERS = new ColumnFamily<String, String>("manager-workers", StringSerializer.get(), StringSerializer.get());
	private static final ColumnFamily<String, String> CF_QUEUE = new ColumnFamily<String, String>("manager-queue", StringSerializer.get(), StringSerializer.get());
	private static final ColumnFamily<String, String> CF_ACTIONS = new ColumnFamily<String, String>("manager-action", StringSerializer.get(), StringSerializer.get());
	
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
			if (CassandraDb.isColumnFamilyExists(keyspace, CF_QUEUE.getName()) == false) {
				CassandraDb.createColumnFamilyString(default_keyspacename, CF_QUEUE.getName(), false);
			}
			if (CassandraDb.isColumnFamilyExists(keyspace, CF_ACTIONS.getName()) == false) {
				CassandraDb.createColumnFamilyString(default_keyspacename, CF_ACTIONS.getName(), false);
			}
		} catch (Exception e) {
			Log2.log.error("Can't init database CFs", e);
		}
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
	
	static void updateInstanceStatus(InstanceStatus instance_status) {
		if (instance_status == null) {
			throw new NullPointerException("\"instance_status\" can't to be null");
		}
		// String json = AppManager.getGson().toJson(instance_status);
		
	}
	
}
