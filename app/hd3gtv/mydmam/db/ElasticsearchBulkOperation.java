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

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.manager.StatisticsTime;

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

/**
 * @see BulkRequestBuilder
 */
public final class ElasticsearchBulkOperation {
	
	private Client client;
	private int window_update_size = 500;
	private int max_retry = 5;
	private StatisticsTime stat_time;
	private BulkRequestBuilder bulk_request_builder;
	
	ElasticsearchBulkOperation(Client client) {
		this.client = client;
		stat_time = new StatisticsTime();
		bulk_request_builder = client.prepareBulk();
		configuration = new Configuration();
		stat_time = new StatisticsTime();
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
		Log2Dump dump = new Log2Dump();
		dump.add("numberOfActions", bulk_request_builder.numberOfActions());
		Log2.log.debug("Prepare to update database", dump);
		
		BulkRequest bu_r = bulk_request_builder.request();
		
		BulkResponse bulkresponse = null;
		stat_time.startMeasure();
		boolean it_was_hard = false;
		
		for (int pos_retry = 0; pos_retry < max_retry; pos_retry++) {
			try {
				bulkresponse = Elasticsearch.getClient().bulk(bu_r).actionGet();
			} catch (NoNodeAvailableException e) {
				it_was_hard = true;
				try {
					/**
					 * Wait before to retry, after the 2nd try.
					 */
					Thread.sleep(pos_retry * 100);
				} catch (InterruptedException e1) {
					Log2.log.error("Stop sleep", e1);
					return;
				}
				if (pos_retry == (max_retry - 2)) {
					/**
					 * Before the last try, force refesh configuration.
					 */
					Elasticsearch.refeshconfiguration();
				} else if (pos_retry + 1 == max_retry) {
					/**
					 * The last try has failed, throw error.
					 */
					throw e;
				}
				
			}
		}
		
		if (bulkresponse != null) {
			if (bulkresponse.hasFailures()) {
				dump = new Log2Dump();
				dump.add("failure message", bulkresponse.buildFailureMessage());
				Log2.log.error("Errors during update database", null, dump);
			} else {
				stat_time.endMeasure();
			}
		}
		
		if (it_was_hard) {
			Log2.log.debug("Current bulk stat time", stat_time.getStatisticTimeResult());
		}
		
		bulk_request_builder.request().requests().clear();
	}
	
	private Configuration configuration;
	
	public Configuration getConfiguration() {
		return configuration;
	}
	
	public class Configuration {
		private Configuration() {
		}
		
		public Configuration setReplicationType(ReplicationType replicationType) {
			bulk_request_builder.setReplicationType(replicationType);
			refresh();
			return this;
		}
		
		public Configuration setConsistencyLevel(WriteConsistencyLevel consistencyLevel) {
			bulk_request_builder.setConsistencyLevel(consistencyLevel);
			refresh();
			return this;
		}
		
		public Configuration setRefresh(boolean refresh) {
			bulk_request_builder.setRefresh(refresh);
			refresh();
			return this;
		}
		
		public final Configuration setTimeout(TimeValue timeout) {
			bulk_request_builder.setTimeout(timeout);
			refresh();
			return this;
		}
		
		public final Configuration setTimeout(String timeout) {
			bulk_request_builder.setTimeout(timeout);
			refresh();
			return this;
		}
	}
	
	private void refresh() {
		if (bulk_request_builder.numberOfActions() > (window_update_size - 1)) {
			execute();
		}
	}
	
	public void terminateBulk() {
		if (bulk_request_builder.numberOfActions() > 0) {
			execute();
			Log2.log.debug("Bulk stat time", stat_time.getStatisticTimeResult());
		}
	}
	
	public ElasticsearchBulkOperation add(IndexRequest request) {
		bulk_request_builder.add(request);
		refresh();
		return this;
	}
	
	public ElasticsearchBulkOperation add(IndexRequestBuilder request) {
		bulk_request_builder.add(request.request());
		refresh();
		return this;
	}
	
	public ElasticsearchBulkOperation add(DeleteRequest request) {
		bulk_request_builder.add(request);
		refresh();
		return this;
	}
	
	public ElasticsearchBulkOperation add(DeleteRequestBuilder request) {
		bulk_request_builder.add(request.request());
		refresh();
		return this;
	}
	
	public ElasticsearchBulkOperation add(UpdateRequest request) {
		bulk_request_builder.add(request);
		refresh();
		return this;
	}
	
	public ElasticsearchBulkOperation add(UpdateRequestBuilder request) {
		bulk_request_builder.add(request.request());
		refresh();
		return this;
	}
	
	public ElasticsearchBulkOperation add(byte[] data, int from, int length, boolean contentUnsafe) throws Exception {
		bulk_request_builder.add(data, from, length, contentUnsafe, null, null);
		refresh();
		return this;
	}
	
	public ElasticsearchBulkOperation add(byte[] data, int from, int length, boolean contentUnsafe, @Nullable String defaultIndex, @Nullable String defaultType) throws Exception {
		bulk_request_builder.add(data, from, length, contentUnsafe, defaultIndex, defaultType);
		refresh();
		return this;
	}
	
}
