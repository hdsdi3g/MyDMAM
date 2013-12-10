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
 * Copyright (C) hdsdi3g for hd3g.tv 2012-2013
 * 
*/
package hd3gtv.mydmam.db;

import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;

import com.netflix.astyanax.ColumnMutation;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.exceptions.OperationException;
import com.netflix.astyanax.query.ColumnFamilyQuery;

abstract class ColumnFamilyScheme<K, C> implements Log2Dumpable {
	
	public abstract ColumnFamilyQuery<K, C> prepareQuery();
	
	public abstract ColumnMutation prepareColumnMutation(K rowKey, C column);
	
	public abstract OperationResult<Void> truncateColumnFamily() throws OperationException, ConnectionException;
	
	protected Keyspace keyspace;
	
	public ColumnFamilyScheme(Keyspace keyspace) {
		this.keyspace = keyspace;
	}
	
	public abstract String getColumnFamilyName();
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("cfname", getColumnFamilyName());
		return dump;
	}
	
	void deployColumnFamily(Keyspace keyspace) throws Exception {
		CassandraDb.createColumnFamilyString(keyspace, getColumnFamilyName());
		updateCF_Def(keyspace);
	}
	
	protected abstract void updateCF_Def(Keyspace keyspace) throws ConnectionException;
	
	boolean cfExists() throws ConnectionException {
		return (keyspace.describeKeyspace().getColumnFamily(getColumnFamilyName()) != null);
	}
}
