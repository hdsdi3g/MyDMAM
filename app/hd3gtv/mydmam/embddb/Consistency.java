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
 * Copyright (C) hdsdi3g for hd3g.tv 19 oct. 2017
 * 
*/
package hd3gtv.mydmam.embddb;

public enum Consistency {
	
	ONLY_LOCAL {
		int getMinimumNodeCountUpdate(int total_nodes_count) {
			return 0;
		}
	},
	ONE_OTHER_NODE {
		int getMinimumNodeCountUpdate(int total_nodes_count) {
			return Math.min(total_nodes_count, 1);
		}
	},
	TWO_OTHER_NODES {
		int getMinimumNodeCountUpdate(int total_nodes_count) {
			return Math.min(total_nodes_count, 2);
		}
	},
	QUORUM {
		int getMinimumNodeCountUpdate(int total_nodes_count) {
			return Math.min(total_nodes_count, (total_nodes_count / 2) + 1);
		}
	},
	ALL_CLUSTER_NODES {
		int getMinimumNodeCountUpdate(int total_nodes_count) {
			return total_nodes_count;
		}
	};
	
	abstract int getMinimumNodeCountUpdate(int total_nodes_count);
}
