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

import java.net.InetSocketAddress;

import hd3gtv.mydmam.MyDMAM;

public class ErrorReturn {
	
	private String message;
	private String caller;
	private boolean disconnectme;
	private long date;
	private InetSocketAddress node;
	
	public ErrorReturn(Node node, String message, Class<?> caller, boolean disconnectme) {
		this.message = message;
		if (message == null) {
			throw new NullPointerException("\"message\" can't to be null");
		}
		if (caller == null) {
			throw new NullPointerException("\"caller\" can't to be null");
		}
		this.caller = caller.getName();
		
		this.disconnectme = disconnectme;
		date = System.currentTimeMillis();
		this.node = node.getSocketAddr();
	}
	
	protected ErrorReturn() {
	}
	
	public static String toJsonString(PoolManager pool_manager, ErrorReturn error) {
		return MyDMAM.gson_kit.getGsonSimple().toJson(error);
	}
	
	public static ErrorReturn fromJsonString(PoolManager pool_manager, String json) {
		return MyDMAM.gson_kit.getGsonSimple().fromJson(json, ErrorReturn.class);
	}
	
	public String getCaller() {
		return caller;
	}
	
	public long getDate() {
		return date;
	}
	
	public String getMessage() {
		return message;
	}
	
	public InetSocketAddress getNode() {
		return node;
	}
	
	public boolean isDisconnectme() {
		return disconnectme;
	}
}
