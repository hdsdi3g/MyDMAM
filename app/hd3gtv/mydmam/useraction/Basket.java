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
package hd3gtv.mydmam.useraction;

import hd3gtv.log2.Log2;
import hd3gtv.mydmam.db.AllRowsFoundRow;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.db.orm.CrudOrmEngine;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.UserProfile;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.serializers.StringSerializer;

import controllers.Secure;

public class Basket {
	
	/**
	 * Cassandra declarations
	 */
	private static final ColumnFamily<String, String> CF_BASKETS = new ColumnFamily<String, String>("baskets", StringSerializer.get(), StringSerializer.get());
	private static final Integer TTL = 3600 * 24 * 365 * 2; // 2 years
	private static final String DEFAULT_BASKET_NAME = "default";
	private static Gson gson;
	
	static {
		gson = new Gson();
		try {
			if (CassandraDb.isColumnFamilyExists(CassandraDb.getkeyspace(), CF_BASKETS.getName()) == false) {
				CassandraDb.createColumnFamilyString(CassandraDb.getDefaultKeyspacename(), CF_BASKETS.getName(), false);
			}
		} catch (Exception e) {
			Log2.log.error("Can't prepare Cassandra connection", e);
		}
	}
	
	// TODO keep_index_deleted_basket_items
	
	/**
	 * Play only side.
	 */
	public static Basket getBasketForCurrentPlayUser() throws Exception {
		String user_key = UserProfile.prepareKey(Secure.connected());
		return new Basket(user_key);
	}
	
	private CrudOrmEngine<UserProfile> user_profile_orm;
	private String user_key;
	
	public Basket(String user_key) throws NullPointerException, Exception {
		this.user_key = user_key;
		if (user_key == null) {
			throw new NullPointerException("\"user_key\" can't to be null");
		}
		
		user_profile_orm = UserProfile.getORMEngine(user_key);
		if (user_profile_orm.getInternalElement().selectedbasketname == null) {
			user_profile_orm.getInternalElement().selectedbasketname = DEFAULT_BASKET_NAME;
			user_profile_orm.saveInternalElement();
		} else if (user_profile_orm.getInternalElement().selectedbasketname.equals("")) {
			user_profile_orm.getInternalElement().selectedbasketname = DEFAULT_BASKET_NAME;
			user_profile_orm.saveInternalElement();
		}
	}
	
	public String getSelectedName() {
		return user_profile_orm.getInternalElement().selectedbasketname;
	}
	
	public void deleteSelected() throws ConnectionException, IOException {
		delete(getSelectedName());
	}
	
	/**
	 * Delete all baskets in ES and Cassandra for User
	 */
	public void deleteAll() throws ConnectionException {
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		mutator.withRow(CF_BASKETS, user_key).delete();
		mutator.execute();
	}
	
	private boolean isSelected(String name) {
		return getSelectedName().equals(name);
	}
	
	public void setSelected(String basket_name) throws IOException, ConnectionException {
		if (basket_name == null) {
			user_profile_orm.getInternalElement().selectedbasketname = DEFAULT_BASKET_NAME;
		} else {
			user_profile_orm.getInternalElement().selectedbasketname = basket_name;
		}
		user_profile_orm.saveInternalElement();
	}
	
	private static List<String> column_content_to_list(Column<String> content) {
		if (content.hasValue() == false) {
			return new ArrayList<String>(1);
		}
		Type type_arrayList = new TypeToken<ArrayList<String>>() {
		}.getType();
		return gson.fromJson(content.getStringValue(), type_arrayList);
	}
	
	private static String list_to_column_content(List<String> list) {
		return gson.toJson(list);
	}
	
	/**
	 * @return never null
	 */
	public List<String> getSelectedContent() throws ConnectionException {
		return getContent(getSelectedName());
	}
	
	/**
	 * @return never null, [pathindexkey]
	 */
	public String getSelectedContentJson() throws ConnectionException {
		return gson.toJson(getContent(getSelectedName()));
	}
	
	public void setSelectedContent(List<String> content) throws NullPointerException, ConnectionException {
		if (content == null) {
			throw new NullPointerException("\"content\" can't to be null");
		}
		setContent(getSelectedName(), content);
	}
	
	public void setSelectedContent(String[] content) throws NullPointerException, ConnectionException {
		if (content == null) {
			throw new NullPointerException("\"content\" can't to be null");
		}
		if (content.length == 0) {
			setContent(getSelectedName(), new ArrayList<String>(1));
		}
		
		ArrayList<String> al_content = new ArrayList<String>(content.length);
		for (int pos = 0; pos < content.length; pos++) {
			al_content.add(content[pos]);
		}
		setContent(getSelectedName(), al_content);
	}
	
	/**
	 * @return never null
	 */
	public List<String> getContent(String basket_name) throws NullPointerException, ConnectionException {
		ColumnList<String> contents = CassandraDb.getkeyspace().prepareQuery(CF_BASKETS).getKey(user_key).withColumnSlice(basket_name).execute().getResult();
		if (contents.isEmpty()) {
			return new ArrayList<String>(1);
		}
		return column_content_to_list(contents.getColumnByIndex(0));
	}
	
	public void setContent(String basket_name, List<String> newcontent) throws ConnectionException {
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		mutator.withRow(CF_BASKETS, user_key).putColumn(basket_name, list_to_column_content(newcontent), TTL);
		mutator.execute();
	}
	
	/**
	 * If the basket exists, it will be overwritten.
	 */
	public void create(String basket_name, boolean switch_to_selected) throws ConnectionException, IOException {
		setContent(basket_name, new ArrayList<String>(1));
		if (switch_to_selected) {
			setSelected(basket_name);
		}
	}
	
	public void delete(String basket_name) throws ConnectionException, IOException {
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		mutator.withRow(CF_BASKETS, user_key).deleteColumn(basket_name);
		mutator.execute();
		
		if (isSelected(basket_name)) {
			setSelected(DEFAULT_BASKET_NAME);
		}
	}
	
	public void rename(String basket_name, String new_basket_name) throws ConnectionException, IOException {
		List<String> list = getContent(basket_name);
		setContent(new_basket_name, list);
		if (isSelected(basket_name)) {
			setSelected(new_basket_name);
		}
		delete(basket_name);
	}
	
	/**
	 * @return [{name -> "basket name", content -> ["pathindexhash"], selected -> boolean}], never null
	 */
	private static List<Map<String, Object>> getAllBaskets(ColumnList<String> all_cols, String selected_basket_name) throws ConnectionException {
		if (all_cols.isEmpty()) {
			return new ArrayList<Map<String, Object>>(1);
		}
		
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(all_cols.size());
		for (int pos = 0; pos < all_cols.size(); pos++) {
			Column<String> content = all_cols.getColumnByIndex(pos);
			HashMap<String, Object> item = new HashMap<String, Object>(2);
			item.put("name", content.getName());
			item.put("content", column_content_to_list(content));
			item.put("selected", content.getName().equals(selected_basket_name));
			result.add(item);
		}
		return result;
	}
	
	/**
	 * @return [{name -> "basket name", content -> ["pathindexhash"], selected -> boolean}], never null
	 */
	public List<Map<String, Object>> getAll() throws ConnectionException {
		// ColumnList<String> contents = CassandraDb.getkeyspace().prepareQuery(CF_BASKETS).getKey(user_key).withColumnSlice(name).execute().getResult();
		return getAllBaskets(CassandraDb.getkeyspace().prepareQuery(CF_BASKETS).getKey(user_key).execute().getResult(), getSelectedName());
	}
	
	/**
	 * @return {user -> [{name -> "basket name", content -> ["pathindexhash"], selected -> boolean}]}, never null
	 */
	public static Map<String, List<Map<String, Object>>> getAllBasketsForAllUsers() throws Exception {
		final Map<String, List<Map<String, Object>>> result = new HashMap<String, List<Map<String, Object>>>();
		final CrudOrmEngine<UserProfile> orm = new CrudOrmEngine<UserProfile>(new UserProfile());
		
		CassandraDb.allRowsReader(CF_BASKETS, new AllRowsFoundRow() {
			public void onFoundRow(Row<String, String> row) throws Exception {
				result.put(row.getKey(), getAllBaskets(row.getColumns(), orm.read(row.getKey()).selectedbasketname));
			}
		});
		return result;
	}
	
}
