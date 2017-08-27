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

public class RequestError extends RequestHandler<ErrorReturn> {
	
	public RequestError(PoolManager pool_manager) {
		super(pool_manager);
	}
	
	public String getHandleName() {
		return "error";
	}
	
	public void onRequest(DataBlock blocks, Node source_node) {
		ErrorReturn error = ErrorReturn.fromJsonString(pool_manager, blocks.getStringDatas());
		source_node.onErrorReturnFromNode(error);
	}
	
	public DataBlock createRequest(ErrorReturn options) {
		return new DataBlock(this, ErrorReturn.toJsonString(pool_manager, options));
	}
	
	public void directSendError(Node node, String message, Class<?> caller, boolean disconnectme) {
		RequestError er = pool_manager.getAllRequestHandlers().getRequestByClass(RequestError.class);
		ErrorReturn error = new ErrorReturn(node, message, caller, disconnectme);
		er.sendRequest(error, node);
	}
	
	protected boolean isCloseChannelRequest(ErrorReturn options) {
		return false;
	}
	
}
