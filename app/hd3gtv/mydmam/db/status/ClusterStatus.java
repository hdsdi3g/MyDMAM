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

import java.util.HashMap;
import java.util.Map;

public class ClusterStatus {
	
	public enum ClusterType {
		CASSANDRA, ELASTICSEARCH
	}
	
	ElasticsearchStatus es_status;
	
	final String NEW_LINE = System.getProperty("line.separator");
	
	public ClusterStatus() {
		es_status = new ElasticsearchStatus();
	}
	
	public void refresh(boolean prepare_reports) {
		es_status.refreshStatus(prepare_reports);
	}
	
	public Map<ClusterType, Map<String, StatusReport>> getAllReports() {
		Map<ClusterType, Map<String, StatusReport>> response = new HashMap<ClusterType, Map<String, StatusReport>>(2);
		response.put(ClusterType.ELASTICSEARCH, es_status.last_status_reports);
		return response;
	}
	
	public String getAllReportsToCSVString() {
		StringBuffer sb = new StringBuffer();
		
		for (Map.Entry<ClusterType, Map<String, StatusReport>> entry : getAllReports().entrySet()) {
			// entry.getKey() entry.getValue()
			sb.append("############# ");
			sb.append(entry.getKey().name());
			sb.append(" #############");
			sb.append(NEW_LINE);
			for (Map.Entry<String, StatusReport> report : entry.getValue().entrySet()) {
				sb.append("=== ");
				sb.append(report.getKey());
				sb.append(" ===");
				sb.append(NEW_LINE);
				sb.append(report.getValue().toCSVString());
			}
		}
		return sb.toString();
	}
	
}
