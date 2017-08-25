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

import org.apache.log4j.Logger;

public class RequestDisconnect extends RequestHandler<String> {
	
	private static Logger log = Logger.getLogger(RequestDisconnect.class);
	
	RequestDisconnect(PoolManager pool_manager) {
		super(pool_manager);
	}
	
	public String getHandleName() {
		return "disconnectme";
	}
	
	public void onRequest(DataBlock block, Node node) {
		try {
			log.info("Distant node " + node + " ask to to close because it say \"" + block.getByName("reason").getDatasAsString() + "\"");
		} catch (IOException e) {
			log.info("Distant node " + node + " ask to to close", e);
		}
		node.close(getClass());
	}
	
	public DataBlock createRequest(String options) {
		return new DataBlock(getHandleName()).createEntry("reason", options);
	}
	
	protected boolean isCloseChannelRequest(String options) {
		return true;
	}
	
}
