/*
 * This file is part of MyDMAM
 * 
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam.db;

import com.netflix.astyanax.ColumnMutation;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.exceptions.OperationException;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.query.ColumnFamilyQuery;
import com.netflix.astyanax.serializers.StringSerializer;

public class SchemeWorkers extends ColumnFamilyScheme<String, String> {
	
	SchemeWorkers(Keyspace keyspace) {
		super(keyspace);
	}
	
	private static final String cfname = "workers";
	
	public static final ColumnFamily<String, String> CF = new ColumnFamily<String, String>(cfname, StringSerializer.get(), StringSerializer.get());
	
	public String getColumnFamilyName() {
		return cfname;
	}
	
	public ColumnFamilyQuery<String, String> prepareQuery() {
		return keyspace.prepareQuery(CF);
	}
	
	public ColumnMutation prepareColumnMutation(String rowKey, String column) {
		return keyspace.prepareColumnMutation(CF, rowKey, column);
	}
	
	public OperationResult<Void> truncateColumnFamily() throws OperationException, ConnectionException {
		return keyspace.truncateColumnFamily(CF);
	}
	
	protected void updateCF_Def(Keyspace keyspace) throws ConnectionException {
	}
	
	/*		defs.add(new DeployColumnDef("app-name", DeployColumnDef.ColType_UTF8Type, false));
			defs.add(new DeployColumnDef("app-version", DeployColumnDef.ColType_AsciiType, false));
			defs.add(new DeployColumnDef("java-uptime", DeployColumnDef.ColType_LongType, false));
			defs.add(new DeployColumnDef("cyclicservices", DeployColumnDef.ColType_UTF8Type, false));
			defs.add(new DeployColumnDef("stacktraces", DeployColumnDef.ColType_UTF8Type, false));
			defs.add(new DeployColumnDef("java-version", DeployColumnDef.ColType_AsciiType, false));
			defs.add(new DeployColumnDef("java-address", DeployColumnDef.ColType_AsciiType, false));*/
}
