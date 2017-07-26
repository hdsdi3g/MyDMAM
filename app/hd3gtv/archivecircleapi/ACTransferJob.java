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
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
*/
package hd3gtv.archivecircleapi;

import java.util.ArrayList;
import java.util.stream.Collectors;

import com.google.gson.annotations.SerializedName;

import hd3gtv.mydmam.Loggers;

/**
 * Not for workflow == TRANSFER_TAPE
 */
public class ACTransferJob {
	
	/**
	 * URL
	 */
	public String self;
	public int max;
	public int offset;
	public int from_id;
	public String sort;
	public String order;
	public int id;
	
	/**
	 * ArchiveJob / RestoreJob
	 */
	public String type;
	
	public String groupId;
	
	public int priority;
	public String node;
	public String userName;
	public long dateCreated;
	public long lastUpdated;
	public long dateEnd;
	/**
	 * DESTAGE
	 */
	public String workflow;
	public String stage;
	
	public enum Status {
		NEW, SCHEDULED, PAUSED, STOPPED, DONE;
	}
	
	public Status status;
	public boolean running;
	public boolean bestEffort;
	
	public int count;
	public int size;
	
	public ArrayList<ACTransfertFile> files;
	
	public class ACTransfertFile {
		private ACTransfertFile() {
		}
		
		public String src;
		public String dst;
		public int idx;
		public long size;
		/**
		 * TRANSFER
		 */
		public String stage;
		/**
		 * SUCCESS/BROWSED... != Status
		 */
		public String status;
		/**
		 * CPFILE
		 */
		public String action;
		public long mtime;
		public int uid;
		public int gid;
		public short mode;
		public String externalId;
	}
	
	public boolean isDatamover() {
		return "datamover".equalsIgnoreCase(groupId);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(id);
		if (running) {
			sb.append(" RUN ");
		} else {
			sb.append("     ");
		}
		sb.append(type);
		if (node != null) {
			sb.append("by ");
			sb.append(node);
		}
		if (userName != null) {
			sb.append(": ");
			sb.append(userName);
		}
		sb.append(" the ");
		sb.append(Loggers.dateLog(lastUpdated));
		sb.append(", ");
		sb.append(status);
		if (files == null) {
			return sb.toString();
		}
		
		sb.append(", ");
		sb.append(files.size());
		
		if (files.size() > 1) {
			sb.append(" files ");
		} else {
			sb.append(" file ");
		}
		sb.append(files.stream().filter(f -> {
			return f.status != null;
		}).collect(Collectors.groupingBy(f -> {
			return f.status;
		}, Collectors.counting())));
		
		if (counters != null) {
			if (counters.allBytesTransfered()) {
				sb.append(" ");
				sb.append(counters.size_transfert_success / 1024l);
				sb.append(" kb");
			} else {
				sb.append(" ");
				sb.append(counters.size_transfert_success / 1024l);
				sb.append("/");
				sb.append(counters.size_transfert_total / 1024l);
				sb.append(" kb");
			}
		}
		
		return sb.toString();
	}
	
	public ACCounters counters;
	
	/**
	 * Some entries from datamover responses are missing.
	 */
	public class ACCounters {
		private ACCounters() {
		}
		
		@SerializedName("COUNT.TRANSFER.SUCCESS")
		public int count_transfert_success;
		
		@SerializedName("SIZE.PREPARE.TOTAL")
		public long size_prepare_total;
		
		@SerializedName("SIZE.TRANSFER.BROWSED")
		public long size_transfert_browsed;
		
		@SerializedName("SIZE.TRANSFER.TOTAL")
		public long size_transfert_total;
		
		@SerializedName("COUNT.PREPARE.TOTAL")
		public int count_prepare_total;
		
		@SerializedName("COUNT.TRANSFER.BROWSED")
		public int count_transfert_browsed;
		
		@SerializedName("COUNT.PREPARE.TO_SYNC")
		public int count_prepare_to_sync;
		
		@SerializedName("SIZE.PREPARE.TO_SYNC")
		public long size_prepare_to_sync;
		
		@SerializedName("SIZE.TRANSFER.SUCCESS")
		public long size_transfert_success;
		
		@SerializedName("COUNT.TRANSFER.TOTAL")
		public int count_transfert_total;
		
		public boolean allBytesTransfered() {
			return size_transfert_success == size_transfert_total;
		}
		
	}
	
}
