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
package hd3gtv.mydmam.operation;

import hd3gtv.log2.Log2;
import hd3gtv.mydmam.db.CassandraDb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.serializers.StringSerializer;

public class Basket {
	
	private static final ColumnFamily<String, String> CF_BASKETS = new ColumnFamily<String, String>("baskets", StringSerializer.get(), StringSerializer.get());
	
	static {
		try {
			if (CassandraDb.isColumnFamilyExists(CassandraDb.getkeyspace(), CF_BASKETS.getName()) == false) {
				CassandraDb.createColumnFamilyString(CassandraDb.getDefaultKeyspacename(), CF_BASKETS.getName(), false);
			}
		} catch (Exception e) {
			Log2.log.error("Can't prepare Cassandra connection", e);
		}
	}
	
	private String user_key;
	
	public Basket(String user_key) {
		this.user_key = user_key;
		if (user_key == null) {
			throw new NullPointerException("\"user_key\" can't to be null");
		}
	}
	
	public Collection<String> getContent() throws ConnectionException {
		ColumnList<String> cols = CassandraDb.getkeyspace().prepareQuery(CF_BASKETS).getKey(user_key).execute().getResult();
		if (cols.isEmpty()) {
			return new ArrayList<String>(1);
		}
		return cols.getColumnNames();
	}
	
	public void setContent(List<String> content) throws ConnectionException {
		if (content == null) {
			throw new NullPointerException("\"content\" can't to be null");
		}
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		mutator.withRow(CF_BASKETS, user_key).delete();
		for (int pos = 0; pos < content.size(); pos++) {
			mutator.withRow(CF_BASKETS, user_key).putColumn(content.get(pos), true);
		}
		mutator.execute();
	}
}
