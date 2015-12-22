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
package hd3gtv.mydmam.manager;

import hd3gtv.mydmam.manager.JobNG.AlterJobOrderName;
import hd3gtv.mydmam.manager.JobNG.JobStatus;

public class AsyncJSBrokerRequestAction {
	
	public String job_key;
	
	public JobStatus all_status;
	
	public AlterJobOrderName order;
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Order: ");
		sb.append(order);
		if (job_key != null) {
			sb.append(", job_key: ");
			sb.append(job_key);
		}
		if (all_status != null) {
			sb.append(", all_status: ");
			sb.append(all_status);
		}
		return sb.toString();
	}
	
}
