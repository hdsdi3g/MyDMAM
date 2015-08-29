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
package hd3gtv.mydmam.transcode.watchfolder;

import hd3gtv.log2.Log2;
import hd3gtv.mydmam.db.CassandraDb;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonParser;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.recipes.locks.ColumnPrefixDistributedRowLock;
import com.netflix.astyanax.serializers.StringSerializer;

public class WatchFolderDB {
	
	static final ColumnFamily<String, String> CF_WATCHFOLDERS = new ColumnFamily<String, String>("WatchFolders", StringSerializer.get(), StringSerializer.get());
	private static Keyspace keyspace;
	private static JsonParser parser = new JsonParser();
	
	static {
		try {
			keyspace = CassandraDb.getkeyspace();
			String default_keyspacename = CassandraDb.getDefaultKeyspacename();
			if (CassandraDb.isColumnFamilyExists(keyspace, CF_WATCHFOLDERS.getName()) == false) {
				CassandraDb.createColumnFamilyString(default_keyspacename, CF_WATCHFOLDERS.getName(), false);
				// String queue_name = CF_WATCHFOLDERS.getName();
				// CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "status", queue_name + "_status", DeployColumnDef.ColType_AsciiType);
				// CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "creator_hostname", queue_name + "_creator_hostname", DeployColumnDef.ColType_UTF8Type);
				// CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "expiration_date", queue_name + "_expiration_date", DeployColumnDef.ColType_LongType);
				// CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "update_date", queue_name + "_update_date", DeployColumnDef.ColType_LongType);
				// CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "delete", queue_name + "_delete", DeployColumnDef.ColType_Int32Type);
				// CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "indexingdebug", queue_name + "_indexingdebug", DeployColumnDef.ColType_Int32Type);
			}
		} catch (Exception e) {
			Log2.log.error("Can't init database CFs", e);
		}
	}
	
	private WatchFolderDB() {
	}
	
	static List<AbstractFoundedFile> get(List<AbstractFoundedFile> items_to_check) throws ConnectionException {
		if (items_to_check.isEmpty()) {
			return new ArrayList<AbstractFoundedFile>(1);
		}
		List<String> key_slice = new ArrayList<String>(items_to_check.size());
		for (int pos = 0; pos < items_to_check.size(); pos++) {
			key_slice.add(items_to_check.get(pos).getPathIndexKey());
		}
		
		List<AbstractFoundedFile> result = new ArrayList<AbstractFoundedFile>(items_to_check.size());
		OperationResult<Rows<String, String>> rows = keyspace.prepareQuery(CF_WATCHFOLDERS).getKeySlice(key_slice).execute();
		for (Row<String, String> row : rows.getResult()) {
			result.add(new AbstractFoundedFile(row));
		}
		return result;
	}
	
	static void push(List<AbstractFoundedFile> files) throws ConnectionException {
		if (files.isEmpty()) {
			return;
		}
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		for (int pos = 0; pos < files.size(); pos++) {
			files.get(pos).saveToCassandra(mutator);
		}
		mutator.execute();
	}
	
	static ColumnPrefixDistributedRowLock<String> prepareLock(String pathindexkey) {
		return new ColumnPrefixDistributedRowLock<String>(keyspace, CF_WATCHFOLDERS, pathindexkey);
	}
	
}
