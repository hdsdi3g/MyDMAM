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
 * Copyright (C) hdsdi3g for hd3g.tv 13 nov. 2017
 * 
*/
package hd3gtv.mydmam.embddb.pipeline;

import org.apache.log4j.Logger;

import hd3gtv.mydmam.embddb.network.DataBlock;
import hd3gtv.mydmam.embddb.network.HandleName;
import hd3gtv.mydmam.embddb.network.Node;
import hd3gtv.mydmam.embddb.network.PoolManager;
import hd3gtv.mydmam.embddb.network.RequestHandler;

public class RequestHandlerKeyContentBulkDatas extends RequestHandler<MessageKeyContentBulkDatas> {
	private static Logger log = Logger.getLogger(RequestHandlerKeyContentBulkDatas.class);
	
	private final IOPipeline pipeline;
	
	public RequestHandlerKeyContentBulkDatas(PoolManager pool_manager, IOPipeline pipeline) {
		super(pool_manager);
		this.pipeline = pipeline;
		if (pipeline == null) {
			throw new NullPointerException("\"pipeline\" can't to be null");
		}
	}
	
	static final HandleName HANDLE_NAME = new HandleName("bulkupd_dstore");
	
	public HandleName getHandleName() {
		return HANDLE_NAME;
	}
	
	public void onRequest(DataBlock block, Node source_node) {
		if (log.isTraceEnabled()) {
			log.trace("Get message from " + source_node + ": " + block.getDataSize() + " bytes");
		}
		pipeline.onExternalContentBulkListDatas(new MessageKeyContentBulkDatas(block), source_node);
	}
	
	public DataBlock createRequest(MessageKeyContentBulkDatas options) {
		return options.makeDatablock(this);
	}
	
}
