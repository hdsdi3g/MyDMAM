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
 * Copyright (C) hdsdi3g for hd3g.tv 5 sept. 2017
 * 
*/
package hd3gtv.mydmam.embddb;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.embddb.network.DataBlock;
import hd3gtv.mydmam.embddb.network.Node;
import hd3gtv.mydmam.embddb.network.PoolManager;
import hd3gtv.mydmam.embddb.network.RequestHandler;
import hd3gtv.mydmam.gson.GsonIgnore;
import hd3gtv.tools.InteractiveConsoleOrder;
import hd3gtv.tools.InteractiveConsoleOrderProducer;

@GsonIgnore
public class Telemetry implements InteractiveConsoleOrderProducer {
	private static Logger log = Logger.getLogger(Telemetry.class);
	
	private final EmbDDB embddb;
	private final ConcurrentHashMap<String, TelemetryReport> last_reports_by_node_uuid;
	
	Telemetry(EmbDDB embddb) {
		this.embddb = embddb;
		if (embddb == null) {
			throw new NullPointerException("\"embddb\" can't to be null");
		}
		last_reports_by_node_uuid = new ConcurrentHashMap<>();
		embddb.poolmanager.addRequestHandler(new RequestSendTelemetryReport(embddb.poolmanager));
		embddb.poolmanager.addRequestHandler(new RequestTelemetryReport(embddb.poolmanager));
		
		embddb.poolmanager.addRemoveNodeCallback(n -> {
			last_reports_by_node_uuid.remove(n.getUUID().toString());
		});
	}
	
	/**
	 * Only fo this host.
	 * Sync
	 * @return never null
	 */
	public TelemetryReport createReport() {
		TelemetryReport report = new TelemetryReport();
		report.embddb = MyDMAM.gson_kit.getGson().toJsonTree(embddb).getAsJsonObject();
		report.creation_date = System.currentTimeMillis();
		return report;
	}
	
	/**
	 * Async.
	 * See results with getLastTelemetryReportsSendedFromAllNodes
	 */
	public void askToAllNodesATelemetryReport() {
		last_reports_by_node_uuid.clear();
		last_reports_by_node_uuid.put(embddb.poolmanager.getUUIDRef().toString(), createReport());
		
		List<Node> nodes = embddb.poolmanager.sayToAllNodes(RequestSendTelemetryReport.class, null, null);
		if (nodes.isEmpty()) {
			return;
		}
		log.info("Ask to " + nodes.size() + " node(s) their last telemetry reports");
	}
	
	/**
	 * Create results with askToAllNodesATelemetryReport
	 * My last report is in the list
	 * @return node uuid -> Report
	 */
	public HashMap<String, TelemetryReport> getLastTelemetryReportsSendedFromAllNodes() {
		return new HashMap<>(last_reports_by_node_uuid);
	}
	
	public class RequestSendTelemetryReport extends RequestHandler<Void> {
		
		public RequestSendTelemetryReport(PoolManager pool_manager) {
			super(pool_manager);
		}
		
		public String getHandleName() {
			return "send-telemetry-report";
		}
		
		public void onRequest(DataBlock block, Node source_node) {
			log.debug("Send report request to " + source_node);
			source_node.sendRequest(RequestTelemetryReport.class, createReport());
		}
		
		public DataBlock createRequest(Void options) {
			return new DataBlock(this, "telemetry");
		}
	}
	
	public class RequestTelemetryReport extends RequestHandler<TelemetryReport> {
		
		public RequestTelemetryReport(PoolManager pool_manager) {
			super(pool_manager);
		}
		
		public String getHandleName() {
			return "telemetry-report";
		}
		
		public void onRequest(DataBlock block, Node source_node) {
			log.debug("Get report from " + source_node);
			TelemetryReport report = MyDMAM.gson_kit.getGsonSimple().fromJson(block.getJsonDatas(), TelemetryReport.class);
			last_reports_by_node_uuid.put(source_node.getUUID().toString(), report);
		}
		
		public DataBlock createRequest(TelemetryReport report) {
			return new DataBlock(this, MyDMAM.gson_kit.getGsonSimple().toJsonTree(report));
		}
		
	}
	
	// TODO AJS
	
	public void setConsoleActions(InteractiveConsoleOrder console) {
		
		Consumer<PrintStream> help = out -> {
			out.println("Usage: telemetry [g|get]|[r|retrieve]|[l|last]");
			out.println("With:");
			out.println("  g, get:       display the actual generated report for this node");
			out.println("  r, retrieve:  send to all connected nodes a request for get a report");
			out.println("  l, last:      display the last generated report for all connected nodes");
		};
		
		console.addOrder("telemetry", "Cluster telemetry", "Generate and retrieve telemetry reports", Telemetry.class, (param, out) -> {
			if (param == null) {
				help.accept(out);
				return;
			}
			switch (param) {
			case "g":
			case "get":
				out.println(MyDMAM.gson_kit.getGsonPretty().toJson(createReport()));
				break;
			case "r":
			case "retrieve":
				askToAllNodesATelemetryReport();
				out.println("For get results, enter \"telemetry l\"");
				break;
			case "l":
			case "last":
				String this_uuid = embddb.poolmanager.getUUIDRef().toString();
				getLastTelemetryReportsSendedFromAllNodes().forEach((uuid, report) -> {
					if (this_uuid.equalsIgnoreCase(uuid)) {
						out.println("INSTANCE UUID: " + uuid + " (THIS INSTANCE)");
					} else {
						out.println("INSTANCE UUID: " + uuid);
					}
					out.println(MyDMAM.gson_kit.getGsonPretty().toJson(report));
				});
				break;
			default:
				out.println("Invalid \"" + param + "\"...");
				help.accept(out);
				break;
			}
		});
	}
	
}
