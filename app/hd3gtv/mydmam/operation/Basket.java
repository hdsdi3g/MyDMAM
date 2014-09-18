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
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.db.AllRowsFoundRow;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.db.ElastisearchCrawlerHit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;

import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.serializers.StringSerializer;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class Basket {
	
	private static final ColumnFamily<String, String> CF_BASKETS = new ColumnFamily<String, String>("baskets", StringSerializer.get(), StringSerializer.get());
	public static final String ES_INDEX = "baskets";
	public static final String ES_DEFAULT_TYPE = "global";
	
	private static final Integer TTL = 3600 * 24 * 365 * 2; // 2 years
	public static final String DEFAULT_BASKET_NAME = "default";
	
	private static final String SELECTED_TOKEN_KEY = "#selected";
	
	private static Client client;
	
	static {
		try {
			if (CassandraDb.isColumnFamilyExists(CassandraDb.getkeyspace(), CF_BASKETS.getName()) == false) {
				CassandraDb.createColumnFamilyString(CassandraDb.getDefaultKeyspacename(), CF_BASKETS.getName(), false);
			}
			client = Elasticsearch.getClient();
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
	
	/**
	 * @return never null
	 */
	public List<String> getSelectedContent() throws ConnectionException {
		ColumnList<String> cols = CassandraDb.getkeyspace().prepareQuery(CF_BASKETS).getKey(user_key).execute().getResult();
		if (cols.isEmpty()) {
			return new ArrayList<String>(1);
		}
		Collection<String> names = cols.getColumnNames();
		ArrayList<String> result = new ArrayList<String>(names.size());
		for (String name : names) {
			result.add(name);
		}
		return result;
	}
	
	public void setSelectedContent(List<String> content) throws ConnectionException {
		if (content == null) {
			throw new NullPointerException("\"content\" can't to be null");
		}
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		mutator.withRow(CF_BASKETS, user_key).delete();
		for (int pos = 0; pos < content.size(); pos++) {
			mutator.withRow(CF_BASKETS, user_key).putColumn(content.get(pos), true, TTL);
		}
		mutator.execute();
	}
	
	public void dropBasketContent() throws ConnectionException {
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		mutator.withRow(CF_BASKETS, user_key).delete();
		mutator.execute();
	}
	
	/**
	 * @return baskets[name, content[pathindexhash]], never null
	 */
	public List<Object> getAllBaskets() {
		try {
			GetResponse response = client.get(new GetRequest(ES_INDEX, ES_DEFAULT_TYPE, user_key)).actionGet();
			if (response.isExists() == false) {
				return new ArrayList<Object>(1);
			}
			Map<String, Object> source = response.getSource();
			
			if (source.containsKey("baskets") == false) {
				return new ArrayList<Object>(1);
			}
			return (List) source.get("baskets");
		} catch (IndexMissingException e) {
			return new ArrayList<Object>(1);
		}
	}
	
	public void importSelectedContent() throws ConnectionException {
		/**
		 * Cassandra -> update ES
		 */
		Map<String, Object> source;
		
		try {
			GetResponse response = client.get(new GetRequest(ES_INDEX, ES_DEFAULT_TYPE, user_key)).actionGet();
			if (response.isExists()) {
				source = response.getSource();
			} else {
				source = new HashMap<String, Object>(1);
			}
		} catch (IndexMissingException e) {
			source = new HashMap<String, Object>(1);
		}
		
		List<Object> baskets;
		
		if (source.containsKey("baskets")) {
			baskets = (List) source.get("baskets");
		} else {
			baskets = new ArrayList<Object>(1);
			source.put("baskets", baskets);
		}
		
		HashMap<String, Object> basket = null;
		
		for (int pos = 0; pos < baskets.size(); pos++) {
			basket = (HashMap) baskets.get(pos);
			if (basket.get("name").equals(getSelected())) {
				break;
			}
			basket = null;
		}
		if (basket == null) {
			basket = new HashMap<String, Object>();
			baskets.add(basket);
		}
		
		Collection<String> selected_content = getSelectedContent();
		basket.put("content", selected_content);
		basket.put("name", getSelected());
		
		IndexRequest ir = new IndexRequest(ES_INDEX, ES_DEFAULT_TYPE, user_key);
		ir.source(source);
		ir.ttl((long) TTL);
		ir.refresh(true);
		client.index(ir);
	}
	
	public void switchSelectedBasket(String name) throws ConnectionException, NullPointerException {
		importSelectedContent();
		
		/**
		 * ES -> Cassandra
		 */
		Map<String, Object> source;
		GetResponse response = client.get(new GetRequest(ES_INDEX, ES_DEFAULT_TYPE, user_key)).actionGet();
		if (response.isExists() == false) {
			throw new NullPointerException("No declared baskets for user " + user_key);
		}
		source = response.getSource();
		
		List<Object> baskets;
		
		if (source.containsKey("baskets") == false) {
			throw new NullPointerException("No baskets for user " + user_key);
		}
		baskets = (List) source.get("baskets");
		
		HashMap<String, Object> basket = null;
		
		for (int pos = 0; pos < baskets.size(); pos++) {
			basket = (HashMap) baskets.get(pos);
			if (basket.get("name").equals(name)) {
				break;
			}
			basket = null;
		}
		if (basket == null) {
			throw new NullPointerException("Can't found basket " + name + " for user " + user_key);
		}
		
		List<Object> content = (List) basket.get("content");
		
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		mutator.withRow(CF_BASKETS, user_key).delete();
		for (int pos = 0; pos < content.size(); pos++) {
			mutator.withRow(CF_BASKETS, user_key).putColumn((String) content.get(pos), true, TTL);
		}
		mutator.execute();
		setSelected(name);
	}
	
	public List<String> getBasketContent(String name) throws NullPointerException {
		Map<String, Object> source;
		GetResponse response = client.get(new GetRequest(ES_INDEX, ES_DEFAULT_TYPE, user_key)).actionGet();
		if (response.isExists() == false) {
			throw new NullPointerException("No declared baskets for user " + user_key);
		}
		source = response.getSource();
		
		List<Object> baskets;
		
		if (source.containsKey("baskets") == false) {
			throw new NullPointerException("No baskets for user " + user_key);
		}
		baskets = (List) source.get("baskets");
		
		HashMap<String, Object> basket = null;
		
		for (int pos = 0; pos < baskets.size(); pos++) {
			basket = (HashMap) baskets.get(pos);
			if (basket.get("name").equals(name)) {
				break;
			}
			basket = null;
		}
		if (basket == null) {
			throw new NullPointerException("Can't found basket " + name + " for user " + user_key);
		}
		
		return (List) basket.get("content");
	}
	
	public void setBasketContent(String name, List<String> newcontent) throws ConnectionException {
		Map<String, Object> source;
		GetResponse response = client.get(new GetRequest(ES_INDEX, ES_DEFAULT_TYPE, user_key)).actionGet();
		if (response.isExists() == false) {
			throw new NullPointerException("No declared baskets for user " + user_key);
		}
		source = response.getSource();
		
		List<Object> baskets;
		
		if (source.containsKey("baskets") == false) {
			throw new NullPointerException("No baskets for user " + user_key);
		}
		baskets = (List) source.get("baskets");
		
		HashMap<String, Object> basket = null;
		
		for (int pos = 0; pos < baskets.size(); pos++) {
			basket = (HashMap) baskets.get(pos);
			if (basket.get("name").equals(name)) {
				break;
			}
			basket = null;
		}
		if (basket == null) {
			throw new NullPointerException("Can't found basket " + name + " for user " + user_key);
		}
		basket.put("content", newcontent);
		
		IndexRequest ir = new IndexRequest(ES_INDEX, ES_DEFAULT_TYPE, user_key);
		ir.source(source);
		ir.ttl((long) TTL);
		ir.refresh(true);
		client.index(ir);
		
		if (isSelected(name)) {
			setSelectedContent(newcontent);
		}
	}
	
	/**
	 * If the basket exists, it will be overwritten.
	 */
	public void createNew(String name, boolean switch_to_selected) throws ConnectionException {
		importSelectedContent();
		
		Map<String, Object> source;
		GetResponse response = client.get(new GetRequest(ES_INDEX, ES_DEFAULT_TYPE, user_key)).actionGet();
		if (response.isExists()) {
			source = response.getSource();
		} else {
			source = new HashMap<String, Object>(1);
		}
		
		List<Object> baskets;
		if (source.containsKey("baskets")) {
			baskets = (List) source.get("baskets");
		} else {
			baskets = new ArrayList<Object>(1);
			source.put("baskets", baskets);
		}
		
		HashMap<String, Object> basket = null;
		int pos_to_delete = -1;
		for (int pos = 0; pos < baskets.size(); pos++) {
			basket = (HashMap) baskets.get(pos);
			pos_to_delete = pos;
			if (basket.get("name").equals(name)) {
				break;
			}
			pos_to_delete = -1;
		}
		if (pos_to_delete > -1) {
			baskets.remove(pos_to_delete);
		}
		
		basket = new HashMap<String, Object>();
		basket.put("content", new ArrayList<String>(1));
		basket.put("name", name);
		baskets.add(basket);
		
		IndexRequest ir = new IndexRequest(ES_INDEX, ES_DEFAULT_TYPE, user_key);
		ir.source(source);
		ir.ttl((long) TTL);
		ir.refresh(true);
		client.index(ir);
		
		if (switch_to_selected) {
			MutationBatch mutator = CassandraDb.prepareMutationBatch();
			mutator.withRow(CF_BASKETS, user_key).delete();
			mutator.execute();
			setSelected(name);
		}
	}
	
	public void delete(String name) throws ConnectionException {
		Map<String, Object> source;
		GetResponse response = client.get(new GetRequest(ES_INDEX, ES_DEFAULT_TYPE, user_key)).actionGet();
		if (response.isExists() == false) {
			throw new NullPointerException("No declared baskets for user " + user_key);
		}
		source = response.getSource();
		
		List<Object> baskets;
		
		if (source.containsKey("baskets") == false) {
			throw new NullPointerException("No baskets for user " + user_key);
		}
		baskets = (List) source.get("baskets");
		
		HashMap<String, Object> basket = null;
		
		int pos_to_delete = -1;
		for (int pos = 0; pos < baskets.size(); pos++) {
			pos_to_delete = pos;
			basket = (HashMap) baskets.get(pos);
			if (basket.get("name").equals(name)) {
				break;
			}
			pos_to_delete = -1;
		}
		if (pos_to_delete == -1) {
			throw new NullPointerException("Can't found basket " + name + " for user " + user_key);
		}
		baskets.remove(pos_to_delete);
		
		IndexRequest ir = new IndexRequest(ES_INDEX, ES_DEFAULT_TYPE, user_key);
		ir.source(source);
		ir.ttl((long) TTL);
		ir.refresh(true);
		client.index(ir);
		
		if (isSelected(name)) {
			MutationBatch mutator = CassandraDb.prepareMutationBatch();
			mutator.withRow(CF_BASKETS, user_key).delete();
			mutator.withRow(CF_BASKETS, user_key + SELECTED_TOKEN_KEY).delete();
			mutator.execute();
		}
	}
	
	public void rename(String name, String newname) throws ConnectionException {
		importSelectedContent();
		
		Map<String, Object> source;
		GetResponse response = client.get(new GetRequest(ES_INDEX, ES_DEFAULT_TYPE, user_key)).actionGet();
		if (response.isExists() == false) {
			throw new NullPointerException("No declared baskets for user " + user_key);
		}
		source = response.getSource();
		
		List<Object> baskets;
		
		if (source.containsKey("baskets") == false) {
			throw new NullPointerException("No baskets for user " + user_key);
		}
		baskets = (List) source.get("baskets");
		
		HashMap<String, Object> basket = null;
		
		for (int pos = 0; pos < baskets.size(); pos++) {
			basket = (HashMap) baskets.get(pos);
			if (basket.get("name").equals(name)) {
				break;
			}
			basket = null;
		}
		if (basket == null) {
			throw new NullPointerException("Can't found basket " + name + " for user " + user_key);
		}
		basket.put("name", newname);
		
		IndexRequest ir = new IndexRequest(ES_INDEX, ES_DEFAULT_TYPE, user_key);
		ir.source(source);
		ir.ttl((long) TTL);
		ir.refresh(true);
		client.index(ir);
		
		if (isSelected(name)) {
			setSelected(newname);
		}
	}
	
	public static void addBasketsElementsToListFromRawDb(List<String> current_list, Map<String, Object> source) {
		if (current_list == null) {
			throw new NullPointerException("\"current_list\" can't to be null");
		}
		if (source == null) {
			throw new NullPointerException("\"source\" can't to be null");
		}
		List<Object> baskets = (List) source.get("baskets");
		Map<String, Object> basket;
		List<Object> content;
		for (int pos_b = 0; pos_b < baskets.size(); pos_b++) {
			basket = (Map) baskets.get(pos_b);
			content = (List) basket.get("content");
			for (int pos_c = 0; pos_c < content.size(); pos_c++) {
				if (current_list.contains(content.get(pos_c)) == false) {
					current_list.add((String) content.get(pos_c));
				}
			}
		}
	}
	
	private boolean isSelected(String name) throws ConnectionException {
		return getSelected().equals(name);
	}
	
	private void setSelected(String name) throws ConnectionException {
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		setSelected(mutator, user_key, name);
		mutator.execute();
	}
	
	private static void setSelected(MutationBatch mutator, String user_key, String basketname) throws ConnectionException {
		if (basketname == null) {
			mutator.withRow(CF_BASKETS, user_key + SELECTED_TOKEN_KEY).putColumn("name", DEFAULT_BASKET_NAME, TTL);
		} else {
			mutator.withRow(CF_BASKETS, user_key + SELECTED_TOKEN_KEY).putColumn("name", basketname, TTL);
		}
	}
	
	public String getSelected() throws ConnectionException {
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		String basketname = getSelected(mutator, user_key);
		mutator.execute();
		return basketname;
	}
	
	public static String getSelected(MutationBatch mutator, String user_key) throws ConnectionException {
		ColumnList<String> cols = CassandraDb.getkeyspace().prepareQuery(CF_BASKETS).getKey(user_key + SELECTED_TOKEN_KEY).withColumnSlice("name").execute().getResult();
		if (cols.isEmpty()) {
			setSelected(mutator, user_key, DEFAULT_BASKET_NAME);
			return DEFAULT_BASKET_NAME;
		}
		if (cols.getColumnByName("name").hasValue() == false) {
			setSelected(mutator, user_key, DEFAULT_BASKET_NAME);
			return DEFAULT_BASKET_NAME;
		}
		return cols.getStringValue("name", DEFAULT_BASKET_NAME);
	}
	
	/**
	 * Delete all baskets in ES and Cassandra for User
	 */
	public void destroy() throws ConnectionException {
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		mutator.withRow(CF_BASKETS, user_key).delete();
		mutator.withRow(CF_BASKETS, user_key + SELECTED_TOKEN_KEY).delete();
		mutator.execute();
		
		client.delete(new DeleteRequest(ES_INDEX, ES_DEFAULT_TYPE, user_key)).actionGet();
	}
	
	public static class All {
		
		public static void importSelectedContent() throws Exception {
			final BulkRequestBuilder bulkrequest = client.prepareBulk();
			final MutationBatch mutator = CassandraDb.prepareMutationBatch();
			
			CassandraDb.allRowsReader(CF_BASKETS, new AllRowsFoundRow() {
				public void onFoundRow(Row<String, String> row) throws Exception {
					if (row.getColumns().isEmpty()) {
						return;
					}
					if (row.getKey().endsWith(SELECTED_TOKEN_KEY)) {
						return;
					}
					String user_key = row.getKey();
					
					/**
					 * Cassandra -> update ES
					 */
					Map<String, Object> source;
					
					try {
						GetResponse response = client.get(new GetRequest(ES_INDEX, ES_DEFAULT_TYPE, user_key)).actionGet();
						if (response.isExists()) {
							source = response.getSource();
						} else {
							source = new HashMap<String, Object>(1);
						}
					} catch (IndexMissingException e) {
						source = new HashMap<String, Object>(1);
					}
					
					List<Object> baskets;
					
					if (source.containsKey("baskets")) {
						baskets = (List) source.get("baskets");
					} else {
						baskets = new ArrayList<Object>(1);
						source.put("baskets", baskets);
					}
					
					HashMap<String, Object> basket = null;
					
					for (int pos = 0; pos < baskets.size(); pos++) {
						basket = (HashMap) baskets.get(pos);
						if (basket.get("name").equals(getSelected(mutator, user_key))) {
							break;
						}
						basket = null;
					}
					if (basket == null) {
						basket = new HashMap<String, Object>();
						baskets.add(basket);
					}
					
					basket.put("content", row.getColumns().getColumnNames());
					basket.put("name", getSelected(mutator, user_key));
					
					IndexRequest ir = new IndexRequest(ES_INDEX, ES_DEFAULT_TYPE, user_key);
					ir.source(source);
					ir.ttl((long) TTL);
					ir.refresh(true);
					bulkrequest.add(ir);
				}
			});
			
			try {
				if (mutator.isEmpty() == false) {
					mutator.execute();
				}
			} catch (Exception e) {
				Log2.log.error("Can't update Cassandra", e);
			}
			
			if (bulkrequest.numberOfActions() > 0) {
				BulkResponse bulkresponse = bulkrequest.execute().actionGet();
				if (bulkresponse.hasFailures()) {
					Log2Dump dump = new Log2Dump();
					dump.add("failure message", bulkresponse.buildFailureMessage());
					Log2.log.error("Can't update ES", null, dump);
				}
			}
		}
		
		/**
		 * @return User Key -> ES record
		 */
		public static Map<String, Object> getAllUsersAllBasketsSize() throws ConnectionException {
			final HashMap<String, Object> all_baskets = new HashMap<String, Object>();
			
			Elasticsearch.createCrawlerReader().setIndices(ES_INDEX).setTypes(ES_DEFAULT_TYPE).allReader(new ElastisearchCrawlerHit() {
				public boolean onFoundHit(SearchHit hit) {
					all_baskets.put(hit.getId(), hit.getSource());
					return true;
				}
			});
			
			return all_baskets;
		}
		
	}
}
