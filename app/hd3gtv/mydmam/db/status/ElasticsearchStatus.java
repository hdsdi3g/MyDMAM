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

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.db.Elasticsearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.elasticsearch.Version;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.action.admin.cluster.node.info.PluginInfo;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsIndices;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsNodes;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsNodes.JvmVersion;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsRequestBuilder;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsResponse;
import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.collect.ImmutableList;
import org.elasticsearch.common.hppc.ObjectIntOpenHashMap;
import org.elasticsearch.common.hppc.cursors.ObjectIntCursor;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.transport.TransportAddress;

public class ElasticsearchStatus {
	
	private final ClusterHealthStatus base_cluster_health_status;
	
	ElasticsearchStatus() {
		String color = Configuration.global.getValue("elasticsearch", "cluster_health_status_color", "green");
		if (color.equalsIgnoreCase(ClusterHealthStatus.YELLOW.name())) {
			base_cluster_health_status = ClusterHealthStatus.YELLOW;
		} else {
			base_cluster_health_status = ClusterHealthStatus.GREEN;
		}
		
	}
	
	private static ArrayList<TransportAddress> convertList(ImmutableList<DiscoveryNode> list) {
		ArrayList<TransportAddress> nodes = new ArrayList<TransportAddress>();
		for (int pos = 0; pos < list.size(); pos++) {
			nodes.add(list.get(pos).address());
		}
		return nodes;
	}
	
	LinkedHashMap<String, StatusReport> last_status_reports;
	
	void refreshStatus(boolean prepare_report) {
		TransportClient client = Elasticsearch.getClient();
		
		ArrayList<TransportAddress> current_connected_nodes = convertList(client.connectedNodes());
		ArrayList<TransportAddress> current_listed_nodes = convertList(client.listedNodes());
		ArrayList<TransportAddress> current_filtered_nodes = convertList(client.filteredNodes());
		
		if (current_listed_nodes.isEmpty()) {
			System.out.println("not connected"); // TODO handle disconnected
		} else {
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
			
			ClusterHealthStatus current_cluster_health_status = cluster_stats_response.getStatus();
			if (base_cluster_health_status.ordinal() < current_cluster_health_status.ordinal()) {
				System.out.println("health: " + cluster_stats_response.getStatus()); // TODO handle health
			}
			
			if (prepare_report == false) {
				return;
			}
			last_status_reports = new LinkedHashMap<String, StatusReport>();
			processStats(cluster_stats_response.getIndicesStats());
			processStats(cluster_stats_response.getNodesStats());
		}
	}
	
	private void processStats(ClusterStatsNodes nodes_stats) {
		StatusReport report = new StatusReport();
		
		report.addCell("Client", "Values", nodes_stats.getCounts().getClient());
		report.addCell("Data only", "Values", nodes_stats.getCounts().getDataOnly());
		report.addCell("Master data", "Values", nodes_stats.getCounts().getMasterData());
		report.addCell("Master only", "Values", nodes_stats.getCounts().getMasterOnly());
		report.addCell("Total", "Values", nodes_stats.getCounts().getTotal());
		last_status_reports.put("Count stats", report);
		
		report = new StatusReport();
		report.addCell("Dev", "Values", nodes_stats.getFs().getDev());
		report.addCell("Disk queue", "Values", Strings.format1Decimals(nodes_stats.getFs().getDiskQueue(), ""));
		report.addCell("Disk reads", "Values", nodes_stats.getFs().getDiskReads());
		report.addCell("Disk read size", "Values", nodes_stats.getFs().getDiskReadSizeSize());
		report.addCell("Disk service time", "Values", Strings.format1Decimals(nodes_stats.getFs().getDiskServiceTime(), ""));
		report.addCell("Disk writes", "Values", nodes_stats.getFs().getDiskWrites());
		report.addCell("Disk write size", "Values", nodes_stats.getFs().getDiskWriteSizeSize());
		report.addCell("Free", "Values", nodes_stats.getFs().getFree());
		report.addCell("Mount", "Values", nodes_stats.getFs().getMount());
		report.addCell("Path", "Values", nodes_stats.getFs().getPath());
		report.addCell("Total", "Values", nodes_stats.getFs().getTotal());
		last_status_reports.put("File systems stats", report);
		
		report = new StatusReport();
		int pos = 1;
		for (PluginInfo plugin : nodes_stats.getPlugins()) {
			report.addCell("Name", "Plugin #" + pos, plugin.getName());
			report.addCell("Description", "Plugin #" + pos, plugin.getDescription());
			report.addCell("Version", "Plugin #" + pos, plugin.getVersion());
			report.addCell("Jvm", "Plugin #" + pos, plugin.isJvm());
			report.addCell("Site", "Plugin #" + pos, plugin.isSite());
			report.addCell("Url", "Plugin #" + pos, plugin.getUrl());
			pos++;
		}
		last_status_reports.put("Plugins stats", report);
		
		report = new StatusReport();
		report.addCell("CPU %", "Values", nodes_stats.getProcess().getCpuPercent());
		report.addCell("Min open file descriptors", "Values", nodes_stats.getProcess().getMinOpenFileDescriptors());
		report.addCell("Average open file descriptors", "Values", nodes_stats.getProcess().getAvgOpenFileDescriptors());
		report.addCell("Max open file descriptors", "Values", nodes_stats.getProcess().getMaxOpenFileDescriptors());
		last_status_reports.put("Process stats", report);
		
		report = new StatusReport();
		report.addCell("Threads", "Values", nodes_stats.getJvm().getThreads());
		report.addCell("Heap Max", "Values", nodes_stats.getJvm().getHeapMax());
		report.addCell("Heap Used", "Values", nodes_stats.getJvm().getHeapUsed());
		report.addCell("Max uptime", "Values", nodes_stats.getJvm().getMaxUpTime());
		last_status_reports.put("JVM stats", report);
		
		report = new StatusReport();
		pos = 1;
		ObjectIntOpenHashMap<JvmVersion> jvm_versions = nodes_stats.getJvm().getVersions();
		Iterator<ObjectIntCursor<JvmVersion>> iterator = jvm_versions.iterator();
		while (iterator.hasNext()) {
			ObjectIntCursor<JvmVersion> int_version = iterator.next();
			JvmVersion version = int_version.key;
			/**
			 * Stupid Hack because JvmVersion.version, vmName, vmVersion and vmVendor are not accessibles.
			 */
			HackedStreamOutput stream = new HackedStreamOutput();
			try {
				version.writeTo(stream);
			} catch (IOException e) {
			}
			if (stream.content.size() == 4) {
				report.addCell("Version", "JVM #" + pos, stream.content.get(0));
				report.addCell("VM Name", "JVM #" + pos, stream.content.get(1));
				report.addCell("VM Version", "JVM #" + pos, stream.content.get(2));
				report.addCell("VM Vendor", "JVM #" + pos, stream.content.get(3));
				report.addCell("Count", "JVM #" + pos, int_version.value);
				pos++;
			}
		}
		last_status_reports.put("JVM versions", report);
		
		report = new StatusReport();
		pos = 1;
		for (Version version : nodes_stats.getVersions()) {
			report.addCell("Major.Minor.Revision", "Version #" + pos, ".", version.major, version.minor, version.revision);
			report.addCell("Build", "Version #" + pos, version.build);
			report.addCell("Id", "Version #" + pos, version.id);
			report.addCell("LuceneVersion", "Version #" + pos, version.luceneVersion);
			report.addCell("Snapshot", "Version #" + pos, version.snapshot);
			pos++;
		}
		last_status_reports.put("Elasticsearch versions", report);
	}
	
	/**
	 * Stupid Hack because JvmVersion.version, vmName, vmVersion and vmVendor are not accessibles.
	 */
	private class HackedStreamOutput extends StreamOutput {
		ArrayList<String> content = new ArrayList<String>();
		
		public void writeByte(byte b) throws IOException {
		}
		
		public void writeBytes(byte[] b, int offset, int length) throws IOException {
		}
		
		public void flush() throws IOException {
		}
		
		public void close() throws IOException {
		}
		
		public void reset() throws IOException {
		}
		
		public void writeString(String str) throws IOException {
			content.add(str);
		}
	}
	
	private void processStats(ClusterStatsIndices stats) {
		StatusReport report = new StatusReport();
		report.addCell("Values", "Completion size", stats.getCompletion().getSize().toString());
		report.addCell("Values", "Docs (count/deleted)", "/", stats.getDocs().getCount(), stats.getDocs().getDeleted());
		report.addCell("Values", "Field data evictions, and memory size", ", ", stats.getFieldData().getEvictions(), stats.getFieldData().getMemorySize());
		report.addCell("Values", "Index count", stats.getIndexCount());
		report.addCell("Values", "Percolate count/current/num queries", "/", stats.getPercolate().getCount(), stats.getPercolate().getCurrent(), stats.getPercolate().getNumQueries());
		report.addCell("Values", "Percolate time and memory size", ", ", stats.getPercolate().getTime(), stats.getPercolate().getMemorySize());
		report.addCell("Values", "Segments count and memory size", ", ", stats.getSegments().getCount(), stats.getSegments().getMemory());
		report.addCell("Values", "Store size and throttle time", ", ", stats.getStore().getSize(), stats.getStore().getThrottleTime());
		report.addCell("Values", "Shards total", stats.getShards().getTotal());
		last_status_reports.put("Global stats", report);
		
		report = new StatusReport();
		report.addCell("Current", "Primaries", stats.getShards().getPrimaries());
		report.addCell("Min", "Primaries", stats.getShards().getMinIndexPrimaryShards());
		report.addCell("Average", "Primaries", stats.getShards().getAvgIndexPrimaryShards());
		report.addCell("Max", "Primaries", stats.getShards().getMaxIndexPrimaryShards());
		
		report.addCell("Current", "Replication", stats.getShards().getReplication());
		report.addCell("Min", "Replication", stats.getShards().getMinIndexReplication());
		report.addCell("Average", "Replication", stats.getShards().getAvgIndexReplication());
		report.addCell("Max", "Replication", stats.getShards().getMaxIndexReplication());
		
		report.addCell("Current", "Indices", stats.getShards().getIndices());
		report.addCell("Min", "Indices", stats.getShards().getMinIndexShards());
		report.addCell("Average", "Indices", stats.getShards().getAvgIndexShards());
		report.addCell("Max", "Indices", stats.getShards().getMaxIndexShards());
		last_status_reports.put("Shards stats", report);
	}
	
}
