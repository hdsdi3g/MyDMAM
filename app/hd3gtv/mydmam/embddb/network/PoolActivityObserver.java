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
 * Copyright (C) hdsdi3g for hd3g.tv 12 nov. 2017
 * 
*/
package hd3gtv.mydmam.embddb.network;

public interface PoolActivityObserver {
	
	/**
	 * This is fired only if old_node has an UUID.
	 * @param old_node can be closed or in a non clean state.
	 */
	public default void onPoolRemoveNode(Node old_node) {
	}
	
	/**
	 * This is fired after a valid Hello handshake.
	 * @param node is ready to do requests.
	 */
	public default void onPoolAddAReadyNode(Node node) {
	}
	
}
