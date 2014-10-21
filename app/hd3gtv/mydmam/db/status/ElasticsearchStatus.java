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
package hd3gtv.mydmam.db.status;

import hd3gtv.mydmam.db.Elasticsearch;

import java.util.ArrayList;

import org.elasticsearch.action.admin.cluster.stats.ClusterStatsRequestBuilder;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsResponse;
import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.collect.ImmutableList;
import org.elasticsearch.common.transport.TransportAddress;

public class ElasticsearchStatus {
	
	private static ArrayList<TransportAddress> convertList(ImmutableList<DiscoveryNode> list) {
		ArrayList<TransportAddress> nodes = new ArrayList<TransportAddress>();
		for (int pos = 0; pos < list.size(); pos++) {
			nodes.add(list.get(pos).address());
		}
		return nodes;
	}
	
	void refreshStatus() {
		TransportClient client = Elasticsearch.getClient();
		
		ArrayList<TransportAddress> current_connected_nodes = convertList(client.connectedNodes());
		ArrayList<TransportAddress> current_listed_nodes = convertList(client.listedNodes());
		ArrayList<TransportAddress> current_filtered_nodes = convertList(client.filteredNodes());
		
		for (int pos_lstd = 0; pos_lstd < current_listed_nodes.size(); pos_lstd++) {
			boolean founded = false;
			for (int pos_cntd = 0; pos_cntd < current_connected_nodes.size(); pos_cntd++) {
				if (current_connected_nodes.get(pos_cntd).equals(current_listed_nodes.get(pos_lstd))) {
					founded = true;
					break;
				}
			}
			if (founded == false) {
				TransportAddress problem_node = current_listed_nodes.get(pos_lstd);
				for (int pos_cfilt = 0; pos_cfilt < current_filtered_nodes.size(); pos_cfilt++) {
					if (problem_node.equals(current_filtered_nodes.get(pos_cfilt))) {
						founded = true;
						break;
					}
				}
				if (founded) {
					System.out.println("invalid: " + problem_node.toString()); // TODO handle invalid
				} else {
					System.out.println("missing: " + problem_node.toString()); // TODO handle missing
				}
			}
		}
		
		ClusterAdminClient cluster_admin_client = client.admin().cluster();
		ClusterStatsRequestBuilder cluster_stats_request = cluster_admin_client.prepareClusterStats();
		ClusterStatsResponse cluster_stats_response = cluster_stats_request.execute().actionGet();
		// return cluster_stats_response.getStatus();
		
	}
	
	/*private TransportClient getClient() {
		// client.connectedNodes()
		return client;
	}
	
	private ClusterHealthStatus getClusterColor() {
		//System.out.println(cluster_stats_response.getStatus().name());
		//System.out.println(cluster_stats_response.getIndicesStats().getCompletion().getSizeInBytes());
	}
	
	public boolean isValid() {
		String clustername = Configuration.global.getValue("elasticsearch", "clustername", null);
		
		return false;
	}*/
	
}
