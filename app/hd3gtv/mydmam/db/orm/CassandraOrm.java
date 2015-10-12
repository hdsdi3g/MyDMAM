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
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
*/
package hd3gtv.mydmam.db.orm;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.Transient;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.gson.Gson;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.BadRequestException;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.query.ColumnFamilyQuery;
import com.netflix.astyanax.query.IndexQuery;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.db.AllRowsFoundRow;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.db.DeployColumnDef;
import hd3gtv.mydmam.db.orm.annotations.CassandraIndexed;

@SuppressWarnings("unchecked")
public class CassandraOrm<T extends OrmModel> {
	
	private MutationBatch mutator;
	private ColumnFamily<String, String> columnfamily;
	private Class<? extends T> reference;
	private String[] column_names;
	
	/**
	 * Stupid string for null value.
	 */
	private static final String null_value = "null\0" + "BEA3CFC0-36F4-4B3A-905D-4F54419B41E1";
	
	public CassandraOrm(Class<T> constructor_reference, ColumnFamily<String, String> columnfamily) throws ConnectionException, IOException {
		this.columnfamily = columnfamily;
		if (columnfamily == null) {
			throw new NullPointerException("\"columnfamily\" can't to be null");
		}
		reference = constructor_reference;
		
		if (isColumnFamilyExists() == false) {
			createColumnFamily();
		}
	}
	
	public void executeMutation() throws ConnectionException {
		if (mutator == null) {
			return;
		}
		try {
			mutator.execute();
		} catch (BadRequestException e) {
			if (isColumnFamilyExists() == false) {
				try {
					createColumnFamily();
					mutator.execute();
				} catch (IOException e2) {
					Loggers.ORM.error("Can't create ColumnFamily " + this.columnfamily.getName() + ", referer: " + e.getMessage(), e2);
				}
			} else {
				throw e;
			}
		}
		mutator = null;
	}
	
	public void resetMutator() throws ConnectionException {
		mutator = CassandraDb.prepareMutationBatch();
	}
	
	static String serialize(Object o) {
		Gson g = new Gson();
		return g.toJson(o);
	}
	
	private static Object deserialize(String src, Class<?> type) {
		Gson g = new Gson();
		return g.fromJson(src, type);
	}
	
	public void pushColumn(String rowkey, Integer ttl, String name, Object o) throws InvalidClassException, ConnectionException {
		if (rowkey == null) {
			return;
		}
		if (rowkey.equals("")) {
			return;
		}
		if (mutator == null) {
			mutator = CassandraDb.prepareMutationBatch();
		}
		if (ttl == 0) {
			ttl = null;
		}
		if (o == null) {
			mutator.withRow(columnfamily, rowkey).putColumn(name, null_value, ttl);
		} else if (o instanceof String) {
			if (((String) o).equals(null_value)) {
				StringBuffer sb = new StringBuffer();
				sb.append("String value equals to null String reference : security problem ?");
				sb.append("[");
				sb.append(rowkey);
				sb.append("]");
				sb.append(name);
				throw new InvalidClassException(o.getClass().getName(), sb.toString());
			}
			mutator.withRow(columnfamily, rowkey).putColumn(name, (String) o, ttl);
		} else if (o instanceof byte[]) {
			mutator.withRow(columnfamily, rowkey).putColumn(name, (byte[]) o, ttl);
		} else if (o instanceof Integer) {
			mutator.withRow(columnfamily, rowkey).putColumn(name, (Integer) o, ttl);
		} else if (o instanceof Long) {
			mutator.withRow(columnfamily, rowkey).putColumn(name, (Long) o, ttl);
		} else if (o instanceof Boolean) {
			if ((Boolean) o) {
				mutator.withRow(columnfamily, rowkey).putColumn(name, "true", ttl);
			} else {
				mutator.withRow(columnfamily, rowkey).putColumn(name, "false", ttl);
			}
		} else if (o instanceof Date) {
			mutator.withRow(columnfamily, rowkey).putColumn(name, (Date) o, ttl);
		} else if (o instanceof Float) {
			mutator.withRow(columnfamily, rowkey).putColumn(name, String.valueOf((Float) o), ttl);
		} else if (o instanceof Double) {
			mutator.withRow(columnfamily, rowkey).putColumn(name, String.valueOf((Double) o), ttl);
		} else if (o instanceof UUID) {
			mutator.withRow(columnfamily, rowkey).putColumn(name, (UUID) o, ttl);
		} else if (o instanceof JSONObject) {
			mutator.withRow(columnfamily, rowkey).putColumn(name, ((JSONObject) o).toJSONString(), ttl);
		} else if (o instanceof JSONArray) {
			mutator.withRow(columnfamily, rowkey).putColumn(name, ((JSONArray) o).toJSONString(), ttl);
		} else if (o instanceof InetAddress) {
			mutator.withRow(columnfamily, rowkey).putColumn(name, ((InetAddress) o).getHostAddress(), ttl);
		} else if (o instanceof StringBuffer) {
			mutator.withRow(columnfamily, rowkey).putColumn(name, ((StringBuffer) o).toString(), ttl);
		} else if (o instanceof Calendar) {
			mutator.withRow(columnfamily, rowkey).putColumn(name, ((Calendar) o).getTimeInMillis(), ttl);
		} else if (o instanceof String[]) {
			JSONArray ja = new JSONArray();
			String[] o_str = (String[]) o;
			for (int pos = 0; pos < o_str.length; pos++) {
				ja.add(o_str[pos]);
			}
			mutator.withRow(columnfamily, rowkey).putColumn(name, ja.toJSONString(), ttl);
		} else if (o instanceof Serializable) {
			mutator.withRow(columnfamily, rowkey).putColumn(name, serialize(o), ttl);
		} else if (o instanceof Enum) {
			mutator.withRow(columnfamily, rowkey).putColumn(name, serialize(o), ttl);
		} else {
			StringBuffer sb = new StringBuffer();
			sb.append("Class type is not managed for this record ");
			sb.append("[");
			sb.append(rowkey);
			sb.append("]");
			sb.append(name);
			throw new InvalidClassException(o.getClass().getName(), sb.toString());
		}
	}
	
	private ArrayList<Field> getOrmobjectUsableFields() {
		ArrayList<Field> result = new ArrayList<Field>();
		
		Field[] fields = this.reference.getFields();
		Field field;
		int mod;
		for (int pos_df = 0; pos_df < fields.length; pos_df++) {
			field = fields[pos_df];
			if (field.isAnnotationPresent(Transient.class)) {
				/**
				 * Is transient ?
				 */
				continue;
			}
			if (field.getName().equals("key")) {
				/**
				 * Not this (primary key)
				 */
				continue;
			}
			mod = field.getModifiers();
			
			if ((mod & Modifier.PROTECTED) != 0) continue;
			if ((mod & Modifier.PRIVATE) != 0) continue;
			if ((mod & Modifier.ABSTRACT) != 0) continue;
			if ((mod & Modifier.STATIC) != 0) continue;
			if ((mod & Modifier.FINAL) != 0) continue;
			if ((mod & Modifier.TRANSIENT) != 0) continue;
			if ((mod & Modifier.INTERFACE) != 0) continue;
			
			try {
				result.add(field);
			} catch (IllegalArgumentException e) {
				Loggers.ORM.warn("Generic error", e);
			} catch (SecurityException e) {
				Loggers.ORM.warn("Generic error", e);
			}
		}
		
		return result;
	}
	
	/**
	 * ttl form ormobject
	 */
	public void pushObject(T ormobject) throws InvalidClassException, ConnectionException {
		pushObject(ormobject, ormobject.getTTL());
	}
	
	/**
	 * @param ttl overwrite from ormobject
	 */
	public void pushObject(T ormobject, int ttl) throws InvalidClassException, ConnectionException {
		if (ormobject.key == null) {
			throw new NullPointerException("\"key\" can't to be null");
		}
		ArrayList<Field> fields = getOrmobjectUsableFields();
		for (int pos_df = 0; pos_df < fields.size(); pos_df++) {
			try {
				pushColumn(ormobject.key, ttl, fields.get(pos_df).getName(), this.reference.getField(fields.get(pos_df).getName()).get(ormobject));
			} catch (IllegalAccessException e) {
				Loggers.ORM.error("Grave problem with ORM object " + ormobject + "(" + ormobject.getClass().getName() + ") field name: " + fields.get(pos_df).getName(), e);
			} catch (NoSuchFieldException e) {
				e.printStackTrace();
			}
		}
	}
	
	private T createOrmObject() {
		try {
			return (T) reference.newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private void prepareColumnlistnames() {
		if (column_names != null) {
			return;
		}
		ArrayList<String> colnamelist = new ArrayList<String>();
		
		ArrayList<Field> fields = getOrmobjectUsableFields();
		for (int pos_df = 0; pos_df < fields.size(); pos_df++) {
			colnamelist.add(fields.get(pos_df).getName());
		}
		column_names = colnamelist.toArray(new String[colnamelist.size()]);
	}
	
	public String[] getColumnNames() {
		prepareColumnlistnames();
		return column_names;
	}
	
	public T pullObject(String rowkey) throws ConnectionException {
		if (rowkey == null) {
			return null;
		}
		if (rowkey.equals("")) {
			return null;
		}
		prepareColumnlistnames();
		ColumnFamilyQuery<String, String> rows_asset = CassandraDb.getkeyspace().prepareQuery(this.columnfamily);
		OperationResult<ColumnList<String>> row = rows_asset.getKey(rowkey).withColumnSlice(this.column_names).execute();
		return pullObject(rowkey, row.getResult());
	}
	
	public boolean exists(String rowkey) throws ConnectionException {
		if (rowkey == null) {
			return false;
		}
		if (rowkey.equals("")) {
			return false;
		}
		prepareColumnlistnames();
		ColumnFamilyQuery<String, String> rows_asset = CassandraDb.getkeyspace().prepareQuery(this.columnfamily);
		OperationResult<ColumnList<String>> row = rows_asset.getKey(rowkey).withColumnSlice(this.column_names).execute();
		return (row.getResult().isEmpty() == false);
	}
	
	public List<T> pullObjects(String... rowkey) throws ConnectionException {
		if (rowkey == null) {
			return null;
		}
		if (rowkey.length == 0) {
			return null;
		}
		ArrayList<String> rowkeys = new ArrayList<String>(rowkey.length);
		for (int pos = 0; pos < rowkey.length; pos++) {
			if (rowkey[pos] == null) {
				continue;
			}
			if (rowkey[pos].equals("")) {
				continue;
			}
			rowkeys.add(rowkey[pos]);
		}
		if (rowkeys.size() == 0) {
			return null;
		}
		
		prepareColumnlistnames();
		ColumnFamilyQuery<String, String> rows_asset = CassandraDb.getkeyspace().prepareQuery(this.columnfamily);
		OperationResult<Rows<String, String>> rows = rows_asset.getKeySlice(rowkeys).withColumnSlice(this.column_names).execute();
		return pullObjects(rows.getResult());
	}
	
	public void pullAllObjectsToExporter(final CassandraOrmExporter<T> exporter) throws Exception {
		prepareColumnlistnames();
		
		CassandraDb.allRowsReader(columnfamily, new AllRowsFoundRow() {
			public void onFoundRow(Row<String, String> row) throws Exception {
				if (exporter.exportFromCassandra(pullObject(row.getKey(), row.getColumns())) == false) {
					throw new Exception("Exporter from Cassandra want to stop");
				}
				exporter.count++;
			}
		}, column_names);
	}
	
	public JSONArray pullAllObjectsToJSON() throws Exception {
		prepareColumnlistnames();
		final JSONArray ja = new JSONArray();
		final JSONParser jp = new JSONParser();
		CassandraDb.allRowsReader(columnfamily, new AllRowsFoundRow() {
			public void onFoundRow(Row<String, String> row) throws Exception {
				jp.reset();
				ja.add(jp.parse((new Gson()).toJson(pullObject(row.getKey(), row.getColumns()))));
			}
		}, column_names);
		
		return ja;
	}
	
	public List<T> pullAllObjects() throws Exception {
		prepareColumnlistnames();
		final List<T> results = new ArrayList<T>();
		CassandraDb.allRowsReader(columnfamily, new AllRowsFoundRow() {
			public void onFoundRow(Row<String, String> row) throws Exception {
				results.add(pullObject(row.getKey(), row.getColumns()));
			}
		}, column_names);
		return results;
	}
	
	public List<T> pullObjects(Rows<String, String> rows) throws ConnectionException {
		List<T> result = new ArrayList<T>();
		T temp;
		for (Row<String, String> row : rows) {
			temp = pullObject(row.getKey(), row.getColumns());
			if (temp != null) {
				result.add(temp);
			}
		}
		return result;
	}
	
	public int countAllRows() throws Exception {
		prepareColumnlistnames();
		final List<Boolean> count = new ArrayList<Boolean>();
		CassandraDb.allRowsReader(columnfamily, new AllRowsFoundRow() {
			public void onFoundRow(Row<String, String> row) throws Exception {
				count.add(true);
			}
		}, column_names);
		
		return count.size();
	}
	
	public T pullObject(String rowkey, ColumnList<String> columnlist) {
		if (rowkey == null) {
			return null;
		}
		if (rowkey.equals("")) {
			return null;
		}
		if (columnlist.size() == 0) {
			return null;
		}
		
		T recevier = createOrmObject();
		recevier.key = rowkey;
		
		ArrayList<Field> fields = getOrmobjectUsableFields();
		Field field;
		Object o;
		for (int pos_df = 0; pos_df < fields.size(); pos_df++) {
			try {
				field = fields.get(pos_df);
				o = pullColumn(columnlist, field);
				if (o == null) {
					continue;
				}
				field.set(recevier, o);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvalidClassException e) {
				e.printStackTrace();
			}
		}
		return recevier;
	}
	
	private Object pullColumn(ColumnList<String> columnlist, Field field) throws InvalidClassException {
		String name = field.getName();
		Class<?> fieldtype = field.getType();
		
		if (columnlist.getColumnByName(name) == null) {
			return null;
		}
		if (columnlist.getColumnByName(name).hasValue() == false) {
			return null;
		}
		
		if (columnlist.getColumnByName(name).getStringValue().equals(null_value)) {
			return null;
		} else if (fieldtype.isAssignableFrom(String.class)) {
			return columnlist.getColumnByName(name).getStringValue();
		} else if (fieldtype.isAssignableFrom(Byte[].class) | fieldtype.isAssignableFrom(byte[].class)) {
			return columnlist.getColumnByName(name).getByteArrayValue();
		} else if (fieldtype.isAssignableFrom(Integer.class) | fieldtype.isAssignableFrom(int.class)) {
			return columnlist.getColumnByName(name).getIntegerValue();
		} else if (fieldtype.isAssignableFrom(Long.class) | fieldtype.isAssignableFrom(long.class)) {
			return columnlist.getColumnByName(name).getLongValue();
		} else if (fieldtype.isAssignableFrom(Boolean.class) | fieldtype.isAssignableFrom(boolean.class)) {
			return columnlist.getColumnByName(name).getStringValue().equals("true");
		} else if (fieldtype.isAssignableFrom(Date.class)) {
			return columnlist.getColumnByName(name).getDateValue();
		} else if (fieldtype.isAssignableFrom(Float.class) | fieldtype.isAssignableFrom(float.class)) {
			return Float.valueOf(columnlist.getColumnByName(name).getStringValue());
		} else if (fieldtype.isAssignableFrom(Double.class) | fieldtype.isAssignableFrom(double.class)) {
			return Double.valueOf(columnlist.getColumnByName(name).getStringValue());
		} else if (fieldtype.isAssignableFrom(UUID.class)) {
			return columnlist.getColumnByName(name).getUUIDValue();
		} else if (fieldtype.isAssignableFrom(JSONObject.class)) {
			String value = "";
			try {
				Object o = fieldtype.newInstance();
				if (o instanceof JSONObject) {
					value = columnlist.getColumnByName(name).getStringValue();
					return (JSONObject) (new JSONParser()).parse(value);
				} else {
					return deserialize(columnlist.getColumnByName(name).getStringValue(), JSONObject.class);
				}
			} catch (InstantiationException e1) {
				return null;
			} catch (IllegalAccessException e1) {
				return null;
			} catch (ParseException e) {
				e.printStackTrace();
				System.err.println(value);
				return null;
			}
		} else if (fieldtype.isAssignableFrom(JSONArray.class)) {
			String value = "";
			try {
				Object o = fieldtype.newInstance();
				if (o instanceof JSONArray) {
					value = columnlist.getColumnByName(name).getStringValue();
					return (JSONArray) (new JSONParser()).parse(value);
				} else {
					return deserialize(columnlist.getColumnByName(name).getStringValue(), JSONArray.class);
				}
			} catch (InstantiationException e1) {
				return null;
			} catch (IllegalAccessException e1) {
				return null;
			} catch (ParseException e) {
				e.printStackTrace();
				System.err.println(value);
				return null;
			}
		} else if (fieldtype.isAssignableFrom(InetAddress.class)) {
			String value = columnlist.getColumnByName(name).getStringValue();
			try {
				return InetAddress.getByName(value);
			} catch (UnknownHostException e) {
				e.printStackTrace();
				System.err.println(value);
				return null;
			}
		} else if (fieldtype.isAssignableFrom(StringBuffer.class)) {
			String value = columnlist.getColumnByName(name).getStringValue();
			return new StringBuffer(value);
		} else if (fieldtype.isAssignableFrom(Calendar.class)) {
			long value = columnlist.getColumnByName(name).getLongValue();
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(value);
			return c;
		} else if (fieldtype.isAssignableFrom(String[].class)) {
			String value = columnlist.getColumnByName(name).getStringValue();
			try {
				JSONArray ja = (JSONArray) (new JSONParser()).parse(value);
				String[] result = new String[ja.size()];
				for (int pos = 0; pos < ja.size(); pos++) {
					result[pos] = (String) ja.get(pos);
				}
				return result;
			} catch (ParseException e) {
				e.printStackTrace();
				System.err.println(value);
				return null;
			}
		} else {
			return deserialize(columnlist.getColumnByName(name).getStringValue(), fieldtype);
		}
	}
	
	public void dropColumnFamily() throws ConnectionException {
		if (isColumnFamilyExists()) {
			Loggers.ORM.info("Drop " + columnfamily.getName() + " CF");
			CassandraDb.dropColumnFamilyString(CassandraDb.getkeyspace(), columnfamily.getName());
		}
	}
	
	public void truncateColumnFamily() throws ConnectionException {
		if (isColumnFamilyExists()) {
			CassandraDb.getkeyspace().truncateColumnFamily(columnfamily.getName()).getResult();
		}
	}
	
	ColumnFamily<String, String> getColumnfamily() {
		return columnfamily;
	}
	
	private boolean isColumnFamilyExists() throws ConnectionException {
		return CassandraDb.isColumnFamilyExists(CassandraDb.getkeyspace(), columnfamily.getName());
	}
	
	private void createColumnFamily() throws IOException, ConnectionException {
		
		CassandraDb.createColumnFamilyString(CassandraDb.getDefaultKeyspacename(), columnfamily.getName(), createOrmObject().hasLongGracePeriod());
		
		ArrayList<Field> fields = getOrmobjectUsableFields();
		Field field;
		String colstype;
		Class<?> fieldtype;
		for (int pos_df = 0; pos_df < fields.size(); pos_df++) {
			field = fields.get(pos_df);
			if (field.isAnnotationPresent(CassandraIndexed.class) == false) {
				continue;
			}
			fieldtype = field.getType();
			if (fieldtype.isAssignableFrom(String.class)) {
				colstype = DeployColumnDef.ColType_UTF8Type;
			} else if (fieldtype.isAssignableFrom(Byte[].class) | fieldtype.isAssignableFrom(byte.class)) {
				colstype = DeployColumnDef.ColType_BytesType;
			} else if (fieldtype.isAssignableFrom(Integer.class) | fieldtype.isAssignableFrom(int.class)) {
				colstype = DeployColumnDef.ColType_IntegerType;
			} else if (fieldtype.isAssignableFrom(Long.class) | fieldtype.isAssignableFrom(long.class)) {
				colstype = DeployColumnDef.ColType_LongType;
			} else if (fieldtype.isAssignableFrom(Boolean.class) | fieldtype.isAssignableFrom(boolean.class)) {
				colstype = DeployColumnDef.ColType_IntegerType;
			} else if (fieldtype.isAssignableFrom(Date.class)) {
				colstype = DeployColumnDef.ColType_LongType;
			} else if (fieldtype.isAssignableFrom(Float.class) | fieldtype.isAssignableFrom(float.class)) {
				colstype = DeployColumnDef.ColType_AsciiType;
			} else if (fieldtype.isAssignableFrom(Double.class) | fieldtype.isAssignableFrom(double.class)) {
				colstype = DeployColumnDef.ColType_AsciiType;
			} else if (fieldtype.isAssignableFrom(UUID.class)) {
				colstype = DeployColumnDef.ColType_LexicalUUIDType;
			} else if (fieldtype.isAssignableFrom(JSONObject.class)) {
				colstype = DeployColumnDef.ColType_UTF8Type;
			} else if (fieldtype.isAssignableFrom(JSONArray.class)) {
				colstype = DeployColumnDef.ColType_UTF8Type;
			} else if (fieldtype.isAssignableFrom(InetAddress.class)) {
				colstype = DeployColumnDef.ColType_AsciiType;
			} else if (fieldtype.isAssignableFrom(StringBuffer.class)) {
				colstype = DeployColumnDef.ColType_UTF8Type;
			} else if (fieldtype.isAssignableFrom(Calendar.class)) {
				colstype = DeployColumnDef.ColType_LongType;
			} else if (fieldtype.isAssignableFrom(String[].class)) {
				colstype = DeployColumnDef.ColType_UTF8Type;
			} else {
				colstype = DeployColumnDef.ColType_BytesType;
			}
			
			CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), columnfamily, field.getName(), getIndexName(field.getName()), colstype);
		}
	}
	
	public String getIndexName(String fieldname) {
		return columnfamily.getName() + "_" + fieldname;
	}
	
	/**
	 * Atomic (prepare and execute mutation)
	 */
	public void delete(String rowkey) throws ConnectionException {
		if (rowkey == null) {
			return;
		}
		if (rowkey.equals("")) {
			return;
		}
		resetMutator();
		ArrayList<ColumnFamily<String, String>> cf = new ArrayList<ColumnFamily<String, String>>();
		cf.add(columnfamily);
		mutator.deleteRow(cf, rowkey);
		executeMutation();
	}
	
	/**
	 * Atomic (prepare and execute mutation)
	 */
	public void delete(T element) throws ConnectionException {
		delete(element.key);
	}
	
	/**
	 * Limited to 1M rows
	 */
	public List<T> pullObjectsByIndexes(Expression... indexqueryexpressions) throws ConnectionException {
		prepareColumnlistnames();
		IndexQuery<String, String> idx_rows = CassandraDb.getkeyspace().prepareQuery(columnfamily).searchWithIndex();
		
		for (int pos = 0; pos < indexqueryexpressions.length; pos++) {
			indexqueryexpressions[pos].applyExpression(idx_rows, this);
		}
		idx_rows.withColumnSlice(column_names);
		idx_rows.setRowLimit(1000000);
		
		OperationResult<Rows<String, String>> rows = idx_rows.execute();
		List<T> results = new ArrayList<T>();
		for (Row<String, String> row : rows.getResult()) {
			results.add(pullObject(row.getKey(), row.getColumns()));
		}
		return results;
	}
	
	/**
	 * Limited to 1M rows
	 */
	public void pullAllObjectsToExporter(final CassandraOrmExporter<T> exporter, Expression... indexqueryexpressions) throws Exception {
		prepareColumnlistnames();
		IndexQuery<String, String> idx_rows = CassandraDb.getkeyspace().prepareQuery(columnfamily).searchWithIndex();
		
		for (int pos = 0; pos < indexqueryexpressions.length; pos++) {
			indexqueryexpressions[pos].applyExpression(idx_rows, this);
		}
		idx_rows.withColumnSlice(column_names);
		idx_rows.setRowLimit(1000000);
		
		OperationResult<Rows<String, String>> rows = idx_rows.execute();
		for (Row<String, String> row : rows.getResult()) {
			if (exporter.exportFromCassandra(pullObject(row.getKey(), row.getColumns())) == false) {
				return;
			}
		}
	}
	
	public JSONArray pullObjectsByIndexesToJSON(Expression... indexqueryexpressions) throws ConnectionException, ParseException {
		List<T> values = pullObjectsByIndexes(indexqueryexpressions);
		JSONArray ja = new JSONArray();
		JSONParser jp = new JSONParser();
		for (int pos = 0; pos < values.size(); pos++) {
			jp.reset();
			ja.add(jp.parse((new Gson()).toJson(values.get(pos))));
		}
		return ja;
	}
}
