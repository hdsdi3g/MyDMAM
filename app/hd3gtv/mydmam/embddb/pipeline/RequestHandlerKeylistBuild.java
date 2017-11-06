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
 * Copyright (C) hdsdi3g for hd3g.tv 6 nov. 2017
 * 
*/
package hd3gtv.mydmam.embddb.pipeline;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.google.gson.JsonParseException;

import hd3gtv.mydmam.embddb.network.DataBlock;
import hd3gtv.mydmam.embddb.network.Node;
import hd3gtv.mydmam.embddb.network.PoolManager;
import hd3gtv.mydmam.embddb.network.RequestHandler;

class RequestHandlerKeylistBuild extends RequestHandler<MessageKeylistBuild> {
	
	private static Logger log = Logger.getLogger(RequestHandlerKeylistBuild.class);
	private final IOPipeline pipeline;
	
	public RequestHandlerKeylistBuild(PoolManager pool_manager, IOPipeline pipeline) {
		super(pool_manager);
		this.pipeline = pipeline;
		if (pipeline == null) {
			throw new NullPointerException("\"pipeline\" can't to be null");
		}
	}
	
	public String getHandleName() {
		return "keylistbuild_distributed_store";
	}
	
	public void onRequest(DataBlock block, Node source_node) {
		if (log.isTraceEnabled()) {
			log.trace("Get KeyListBuild message from " + source_node + ": " + block.getStringDatas());
		}
		try {
			pipeline.onExternalKeylistBuild(MessageKeylistBuild.fromJson(block.getJsonDatas()), source_node);
		} catch (JsonParseException | IOException e) {
			throw new RuntimeException("Can't create key list", e);
		}
	}
	
	public DataBlock createRequest(MessageKeylistBuild options) {
		return new DataBlock(this, options.toDataBlock());
	}
	
}
