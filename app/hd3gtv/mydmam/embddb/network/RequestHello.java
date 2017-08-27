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
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.google.gson.JsonObject;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonKit;

public class RequestHello extends RequestHandler<Void> {
	
	private static Logger log = Logger.getLogger(RequestHello.class);
	
	public RequestHello(PoolManager pool_manager) {
		super(pool_manager);
	}
	
	public String getHandleName() {
		return "hello";
	}
	
	public void onRequest(DataBlock block, Node source_node) {
		try {
			source_node.setDistantDate(block.getCreateDate());
			
			JsonObject jo = block.getJsonDatas().getAsJsonObject();
			source_node.setUUIDRef(UUID.fromString(jo.get("uuid").getAsString()));
			source_node.setLocalServerNodeAddresses(MyDMAM.gson_kit.getGsonSimple().fromJson(jo.get("listen_on"), GsonKit.type_ArrayList_InetSocketAddr));
			
			pool_manager.getNode_scheduler().add(source_node, source_node.getScheduledAction());
		} catch (IOException e) {
			log.error("Node " + source_node + " return invalid hello request... disconnect it", e);
			source_node.close(getClass());
		}
	}
	
	public DataBlock createRequest(Void options) {
		JsonObject jo = new JsonObject();
		jo.addProperty("uuid", pool_manager.getUUIDRef().toString());
		jo.add("listen_on", MyDMAM.gson_kit.getGsonSimple().toJsonTree(pool_manager.getListenedServerAddress().collect(Collectors.toList())));
		return new DataBlock(this, jo);
	}
	
	protected boolean isCloseChannelRequest(Void options) {
		return false;
	}
	
}
