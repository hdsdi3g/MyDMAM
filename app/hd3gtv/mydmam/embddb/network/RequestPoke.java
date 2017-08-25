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
 * Copyright (C) hdsdi3g for hd3g.tv 8 janv. 2017
 * 
*/
package hd3gtv.mydmam.embddb.network;

import java.io.IOException;
import java.util.UUID;

import org.apache.log4j.Logger;

public class RequestPoke extends RequestHandler<Void> {
	
	private static Logger log = Logger.getLogger(RequestPoke.class);
	
	public RequestPoke(PoolManager pool_manager) {
		super(pool_manager);
	}
	
	public String getHandleName() {
		return "poke";
	}
	
	public void onRequest(DataBlock block, Node source_node) {
		try {
			long node_date = Long.valueOf(block.getByName("now").getDatasAsString());
			UUID current_uuid = UUID.fromString(block.getByName("uuid").getDatasAsString());
			
			source_node.setDistantDate(node_date);
			source_node.setUUIDRef(current_uuid);
		} catch (IOException e) {
			log.error("Node return invalid response... disconnect it", e);
			source_node.close(getClass());
		}
	}
	
	public DataBlock createRequest(Void options) {
		return new DataBlock(getHandleName()).createEntry("now", String.valueOf(System.currentTimeMillis())).createEntry("uuid", pool_manager.getUUIDRef().toString());
	}
	
	protected boolean isCloseChannelRequest(ErrorReturn options) {
		return false;
	}
	
	protected boolean isCloseChannelRequest(Void options) {
		return false;
	}
	
}
