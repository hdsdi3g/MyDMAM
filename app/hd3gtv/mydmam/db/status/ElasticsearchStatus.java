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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

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
	
	private ClusterHealthStatus last_cluster_health_status = null;
	private ArrayList<String> last_invalid_nodes;
	private ArrayList<String> last_missing_nodes;
	private final ClusterStatus referer;
	private LinkedHashMap<String, StatusReport> last_status_reports;
	private boolean last_status_full_disconnected;
	
	ElasticsearchStatus(ClusterStatus referer) {
		this.referer = referer;
		last_invalid_nodes = new ArrayList<String>();
		last_missing_nodes = new ArrayList<String>();
		last_status_full_disconnected = false;
	}
	
	private static ArrayList<TransportAddress> convertList(ImmutableList<DiscoveryNode> list) {
		ArrayList<TransportAddress> nodes = new ArrayList<TransportAddress>();
		for (int pos = 0; pos < list.size(); pos++) {
			nodes.add(list.get(pos).address());
		}
		return nodes;
	}
	
	void refreshStatus(boolean prepare_report) {
		TransportClient client = Elasticsearch.getClient();
		
		ArrayList<TransportAddress> current_connected_nodes = convertList(client.connectedNodes());
		if (current_connected_nodes.isEmpty()) {
			if (last_status_full_disconnected == false) {
				referer.onGrave(getClass(), "No connected nodes." + ClusterStatus.NEW_LINE);
				last_status_full_disconnected = true;
				last_cluster_health_status = null;
			}
			return;
		}
		if (last_status_full_disconnected) {
			last_status_full_disconnected = false;
			referer.onRecovered(getClass(), "Cluster is now connected." + ClusterStatus.NEW_LINE);
		}
		
		ArrayList<TransportAddress> current_listed_nodes = convertList(client.listedNodes());
		ArrayList<TransportAddress> current_filtered_nodes = convertList(client.filteredNodes());
		
		ArrayList<String> current_invalid_nodes = new ArrayList<String>();
		ArrayList<String> current_missing_nodes = new ArrayList<String>();
		
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
					current_invalid_nodes.add(problem_node.toString());
				} else {
					current_missing_nodes.add(problem_node.toString());
				}
			}
		}
		
		processInvalidAndMissingNodes(current_invalid_nodes, current_missing_nodes);
		
		ClusterAdminClient cluster_admin_client = client.admin().cluster();
		ClusterStatsRequestBuilder cluster_stats_request = cluster_admin_client.prepareClusterStats();
		ClusterStatsResponse cluster_stats_response = cluster_stats_request.execute().actionGet();
		
		ClusterHealthStatus current_cluster_health_status = cluster_stats_response.getStatus();
		
		if (last_cluster_health_status != null) {
			if (last_cluster_health_status.ordinal() < current_cluster_health_status.ordinal()) {
				if (current_cluster_health_status == ClusterHealthStatus.YELLOW) {
					referer.onWarning(getClass(), "Cluster health status is " + last_cluster_health_status.name() + " to " + current_cluster_health_status.name() + "." + ClusterStatus.NEW_LINE);
				} else if (current_cluster_health_status == ClusterHealthStatus.RED) {
					referer.onGrave(getClass(), "Cluster health status is " + last_cluster_health_status.name() + " to " + current_cluster_health_status.name() + "." + ClusterStatus.NEW_LINE);
				}
			} else if (last_cluster_health_status.ordinal() > current_cluster_health_status.ordinal()) {
				referer.onRecovered(getClass(), "Cluster health status is " + last_cluster_health_status.name() + " to " + current_cluster_health_status.name() + "." + ClusterStatus.NEW_LINE);
			}
		}
		last_cluster_health_status = current_cluster_health_status;
		
		if (prepare_report == false) {
			return;
		}
		
		last_status_reports = new LinkedHashMap<String, StatusReport>();
		last_status_reports.put("Cluster health status", new StatusReport().addCell("Color", "Cluster", last_cluster_health_status.name()));
		
		processHostsNodesLists(client.connectedNodes(), client.listedNodes(), client.filteredNodes());
		processStats(cluster_stats_response.getIndicesStats());
		processStats(cluster_stats_response.getNodesStats());
	}
	
	private void processInvalidAndMissingNodes(ArrayList<String> current_invalid_nodes, ArrayList<String> current_missing_nodes) {
		StringBuffer sb = new StringBuffer();
		for (int pos = 0; pos < current_invalid_nodes.size(); pos++) {
			if (last_invalid_nodes.contains(current_invalid_nodes.get(pos))) {
				continue;
			}
			sb.append("Invalid node: " + current_invalid_nodes.get(pos));
			sb.append(ClusterStatus.NEW_LINE);
		}
		
		for (int pos = 0; pos < current_missing_nodes.size(); pos++) {
			if (last_missing_nodes.contains(current_missing_nodes.get(pos))) {
				continue;
			}
			sb.append("Missing node: " + current_missing_nodes.get(pos));
			sb.append(ClusterStatus.NEW_LINE);
		}
		
		if (sb.length() > 0) {
			referer.onWarning(getClass(), sb.toString());
			sb = new StringBuffer();
		}
		
		if (current_invalid_nodes.isEmpty() & (last_invalid_nodes.isEmpty() == false)) {
			sb.append("All invalid nodes are now recovered.");
			sb.append(ClusterStatus.NEW_LINE);
		}
		if (current_missing_nodes.isEmpty() & (last_missing_nodes.isEmpty() == false)) {
			sb.append("All missing nodes are now retrieved.");
			sb.append(ClusterStatus.NEW_LINE);
		}
		if (sb.length() > 0) {
			referer.onRecovered(getClass(), sb.toString());
		}
		
		last_invalid_nodes = current_invalid_nodes;
		last_missing_nodes = current_missing_nodes;
	}
	
	private class HostNodeState {
		boolean client;
		boolean data;
		boolean master;
		boolean connected;
		boolean filtered;
		
		String host_addr;
		String host_name;
		// String id;
		String name;
		Version version;
		
		void report(TransportAddress transport, StatusReport report) {
			report.addCell("Connected", transport.toString(), connected);
			report.addCell("Filtered", transport.toString(), filtered);
			report.addCell("Master/Data/Client", transport.toString(), "/", master, data, client);
			report.addCell("Host", transport.toString(), "/", host_name, host_addr);
			// report.addCell("Id", transport.toString(), id);
			report.addCell("Name", transport.toString(), name);
			report.addCell("Version", transport.toString(), ".", version.major, version.minor, version.revision);
			report.addCell("Build/Id", transport.toString(), "/", version.build, version.id);
			report.addCell("Lucene version", transport.toString(), version.luceneVersion);
			report.addCell("Snapshot", transport.toString(), version.snapshot);
		}
	}
	
	private void processHostsNodesLists(ImmutableList<DiscoveryNode> current_connected_nodes, ImmutableList<DiscoveryNode> current_listed_nodes, ImmutableList<DiscoveryNode> current_filtered_nodes) {
		LinkedHashMap<TransportAddress, HostNodeState> nodes = new LinkedHashMap<TransportAddress, ElasticsearchStatus.HostNodeState>();
		
		for (int pos = 0; pos < current_listed_nodes.size(); pos++) {
			HostNodeState state = new HostNodeState();
			DiscoveryNode node = current_listed_nodes.get(pos);
			state.client = node.isClientNode();
			state.data = node.isDataNode();
			state.master = node.isMasterNode();
			state.host_addr = node.getHostAddress();
			state.host_name = node.getHostName();
			// state.id = node.getId();
			state.name = node.getName();
			state.version = node.getVersion();
			nodes.put(current_listed_nodes.get(pos).getAddress(), state);
		}
		
		for (int pos = 0; pos < current_connected_nodes.size(); pos++) {
			DiscoveryNode node = current_connected_nodes.get(pos);
			HostNodeState state = nodes.get(node.getAddress());
			state.connected = true;
			state.client = node.isClientNode();
			state.data = node.isDataNode();
			state.master = node.isMasterNode();
			state.host_addr = node.getHostAddress();
			state.host_name = node.getHostName();
			// state.id = node.getId();
			state.name = node.getName();
			state.version = node.getVersion();
		}
		
		for (int pos = 0; pos < current_filtered_nodes.size(); pos++) {
			DiscoveryNode node = current_filtered_nodes.get(pos);
			nodes.get(node.getAddress()).filtered = true;
		}
		
		StatusReport report = new StatusReport();
		for (Map.Entry<TransportAddress, HostNodeState> entry : nodes.entrySet()) {
			entry.getValue().report(entry.getKey(), report);
		}
		last_status_reports.put("Nodes", report);
		
	}
	
	private void processStats(ClusterStatsNodes nodes_stats) {
		StatusReport report;
		int pos;
		
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
		pos = 1;
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
	
	LinkedHashMap<String, StatusReport> getLastStatusReports() {
		return last_status_reports;
	}
}
