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
package hd3gtv.mydmam.db;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.indices.IndexMissingException;

/**
 * @see MultiGetRequestBuilder
 */
public class ElasticsearchMultiGetRequest {
	private Client client;
	private List<QueryItem> queries;
	private String preference;
	private boolean realtime;
	private boolean refresh;
	
	ElasticsearchMultiGetRequest(Client client) {
		this.client = client;
		if (client == null) {
			throw new NullPointerException("\"client\" can't to be null");
		}
		queries = new ArrayList<QueryItem>();
	}
	
	public ElasticsearchMultiGetRequest add(String index, @Nullable String type, String id) {
		queries.add(new QueryItem(index, type, id));
		return this;
	}
	
	public ElasticsearchMultiGetRequest add(String index, @Nullable String type, Iterable<String> ids) {
		for (String id : ids) {
			queries.add(new QueryItem(index, type, id));
		}
		return this;
	}
	
	public ElasticsearchMultiGetRequest add(String index, @Nullable String type, String... ids) {
		for (String id : ids) {
			queries.add(new QueryItem(index, type, id));
		}
		return this;
	}
	
	public ElasticsearchMultiGetRequest setPreference(String preference) {
		this.preference = preference;
		return this;
	}
	
	public ElasticsearchMultiGetRequest setRefresh(boolean refresh) {
		this.refresh = refresh;
		return this;
	}
	
	public ElasticsearchMultiGetRequest setRealtime(boolean realtime) {
		this.realtime = realtime;
		return this;
	}
	
	private class QueryItem {
		String index;
		String type;
		String id;
		
		public QueryItem(String index, @Nullable String type, String id) {
			this.index = index;
			this.type = type;
			this.id = id;
		}
	}
	
	public List<GetResponse> responses() throws Exception {
		try {
			
			final MultiGetRequestBuilder multigetrequestbuilder = new MultiGetRequestBuilder(client);
			for (int pos = 0; pos < queries.size(); pos++) {
				multigetrequestbuilder.add(queries.get(pos).index, queries.get(pos).type, queries.get(pos).id);
			}
			multigetrequestbuilder.setPreference(preference);
			multigetrequestbuilder.setRealtime(realtime);
			multigetrequestbuilder.setRefresh(refresh);
			
			MultiGetResponse all_response = Elasticsearch.withRetry(new ElasticsearchWithRetry<MultiGetResponse>() {
				public MultiGetResponse call(Client client) throws NoNodeAvailableException {
					return client.multiGet(multigetrequestbuilder.request()).actionGet();
				}
			});
			
			MultiGetItemResponse[] responses = all_response.getResponses();
			if (responses.length == 0) {
				return new ArrayList<GetResponse>(1);
			}
			List<GetResponse> result = new ArrayList<GetResponse>(responses.length);
			
			for (int pos = 0; pos < responses.length; pos++) {
				if (responses[pos] == null) {
					continue;
				}
				if (responses[pos].getResponse() == null) {
					continue;
				}
				if (responses[pos].getResponse().isExists() == false) {
					continue;
				}
				result.add(responses[pos].getResponse());
			}
			return result;
		} catch (IndexMissingException ime) {
			return new ArrayList<GetResponse>(1);
		} catch (SearchPhaseExecutionException e) {
			return new ArrayList<GetResponse>(1);
		}
	}
}
