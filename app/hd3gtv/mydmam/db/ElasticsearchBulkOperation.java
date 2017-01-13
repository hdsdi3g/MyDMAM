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

import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.support.replication.ReplicationType;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.unit.TimeValue;

import hd3gtv.mydmam.Loggers;

/**
 * @see BulkRequestBuilder
 */
public final class ElasticsearchBulkOperation {
	
	private Client client;
	private int window_update_size = 500;
	private BulkRequestBuilder bulk_request_builder;
	private Runnable on_push_callback;
	
	ElasticsearchBulkOperation() {
		this.client = Elasticsearch.getClient();
		bulk_request_builder = client.prepareBulk();
		bulkconfiguration = new BulkConfiguration();
	}
	
	public void onPush(Runnable on_push_callback) {
		this.on_push_callback = on_push_callback;
	}
	
	public int getWindowUpdateSize() {
		return window_update_size;
	}
	
	public void setWindowUpdateSize(int window_update_size) {
		this.window_update_size = window_update_size;
	}
	
	public Client getClient() {
		return client;
	}
	
	private void execute() {
		Loggers.ElasticSearch.debug("Prepare to update database with numberOfActions: " + bulk_request_builder.numberOfActions());
		
		final BulkRequest bu_r = bulk_request_builder.request();
		
		BulkResponse bulkresponse = null;
		
		bulkresponse = Elasticsearch.withRetry(new ElasticsearchWithRetry<BulkResponse>() {
			public BulkResponse call(Client client) throws NoNodeAvailableException {
				return client.bulk(bu_r).actionGet();
			}
		});
		
		if (bulkresponse != null) {
			if (bulkresponse.hasFailures()) {
				Loggers.ElasticSearch.error("Errors during update database: " + bulkresponse.buildFailureMessage());
			}
		}
		
		bulk_request_builder.request().requests().clear();
		
		if (on_push_callback != null) {
			on_push_callback.run();
		}
	}
	
	private BulkConfiguration bulkconfiguration;
	
	public BulkConfiguration getConfiguration() {
		return bulkconfiguration;
	}
	
	public class BulkConfiguration {
		private BulkConfiguration() {
		}
		
		public BulkConfiguration setReplicationType(ReplicationType replicationType) {
			bulk_request_builder.setReplicationType(replicationType);
			refresh();
			return this;
		}
		
		public BulkConfiguration setConsistencyLevel(WriteConsistencyLevel consistencyLevel) {
			bulk_request_builder.setConsistencyLevel(consistencyLevel);
			refresh();
			return this;
		}
		
		public BulkConfiguration setRefresh(boolean refresh) {
			bulk_request_builder.setRefresh(refresh);
			refresh();
			return this;
		}
		
		public final BulkConfiguration setTimeout(TimeValue timeout) {
			bulk_request_builder.setTimeout(timeout);
			refresh();
			return this;
		}
		
		public final BulkConfiguration setTimeout(String timeout) {
			bulk_request_builder.setTimeout(timeout);
			refresh();
			return this;
		}
	}
	
	void refresh() {
		synchronized (bulk_request_builder) {
			if (bulk_request_builder.numberOfActions() > (window_update_size - 1)) {
				execute();
			}
		}
	}
	
	public void terminateBulk() {
		synchronized (bulk_request_builder) {
			if (bulk_request_builder.numberOfActions() > 0) {
				Loggers.ElasticSearch.debug("Terminate bulk");
				execute();
			}
		}
	}
	
	public ElasticsearchBulkOperation add(IndexRequest request) {
		synchronized (bulk_request_builder) {
			bulk_request_builder.add(request);
		}
		refresh();
		return this;
	}
	
	public ElasticsearchBulkOperation add(IndexRequestBuilder request) {
		synchronized (bulk_request_builder) {
			bulk_request_builder.add(request.request());
		}
		refresh();
		return this;
	}
	
	public ElasticsearchBulkOperation add(DeleteRequest request) {
		synchronized (bulk_request_builder) {
			bulk_request_builder.add(request);
		}
		refresh();
		return this;
	}
	
	public ElasticsearchBulkOperation add(DeleteRequestBuilder request) {
		synchronized (bulk_request_builder) {
			bulk_request_builder.add(request.request());
		}
		refresh();
		return this;
	}
	
	public ElasticsearchBulkOperation add(UpdateRequest request) {
		synchronized (bulk_request_builder) {
			bulk_request_builder.add(request);
		}
		refresh();
		return this;
	}
	
	public ElasticsearchBulkOperation add(UpdateRequestBuilder request) {
		synchronized (bulk_request_builder) {
			bulk_request_builder.add(request.request());
		}
		refresh();
		return this;
	}
	
	public ElasticsearchBulkOperation add(byte[] data, int from, int length, boolean contentUnsafe) throws Exception {
		synchronized (bulk_request_builder) {
			bulk_request_builder.add(data, from, length, contentUnsafe, null, null);
		}
		refresh();
		return this;
	}
	
	public ElasticsearchBulkOperation add(byte[] data, int from, int length, boolean contentUnsafe, @Nullable String defaultIndex, @Nullable String defaultType) throws Exception {
		synchronized (bulk_request_builder) {
			bulk_request_builder.add(data, from, length, contentUnsafe, defaultIndex, defaultType);
		}
		refresh();
		return this;
	}
	
}
