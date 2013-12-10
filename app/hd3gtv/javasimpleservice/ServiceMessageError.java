/*
 * This file is part of Java Simple ServiceManager
 * 
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2008-2013
 * 
*/

package hd3gtv.javasimpleservice;

import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;
import hd3gtv.log2.Log2Event;

import java.util.ArrayList;

public class ServiceMessageError implements ServiceMessage {
	
	private Throwable throwable;
	private String basemessagelocalized;
	private Log2Dump dump;
	private ArrayList<Log2Dump> tablecontent;
	
	public ServiceMessageError(String basemessagelocalized, Throwable throwable, Log2Dumpable dump) {
		this.basemessagelocalized = basemessagelocalized;
		if (basemessagelocalized == null) {
			throw new NullPointerException("\"basemessagelocalized\" can't to be null");
		}
		this.throwable = throwable;
		if (dump != null) {
			this.dump = dump.getLog2Dump();
		}
	}
	
	public ServiceMessageError(String basemessagelocalized, Throwable throwable) {
		this.basemessagelocalized = basemessagelocalized;
		if (basemessagelocalized == null) {
			throw new NullPointerException("\"basemessagelocalized\" can't to be null");
		}
		this.throwable = throwable;
	}
	
	public ServiceMessageError(String basemessagelocalized, Throwable throwable, Log2Dump dump) {
		this.basemessagelocalized = basemessagelocalized;
		if (basemessagelocalized == null) {
			throw new NullPointerException("\"basemessagelocalized\" can't to be null");
		}
		this.throwable = throwable;
		this.dump = dump;
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump finaldump = new Log2Dump();
		finaldump.addAll(dump);
		finaldump.add("basemessagelocalized", basemessagelocalized);
		return dump;
	}
	
	public void setTablecontent(ArrayList<Log2Dump> tablecontent) {
		this.tablecontent = tablecontent;
	}
	
	public String getSubjectContent() {
		return "General error: " + basemessagelocalized;
	}
	
	public String getMessageHeader() {
		return "An error occurred when running the application.";
	}
	
	public String getMessageFooter() {
		return "Thank you for checking as soon as the state of the application (logs, service, mount ...).";
	}
	
	public String getMessageContent(boolean htmlallowed) {
		StringBuffer sb = new StringBuffer();
		
		sb.append(basemessagelocalized);
		sb.append("\r\n");
		sb.append("\r\n");
		
		sb.append("Thread : ");
		sb.append(Thread.currentThread().getName());
		sb.append(" (");
		sb.append(Thread.currentThread().getId());
		sb.append(")\r\n");
		
		if (throwable != null) {
			sb.append("\r\n");
			sb.append(throwable.getClass().getName());
			sb.append(": ");
			sb.append(throwable.getMessage());
			sb.append("\r\n");
			Log2Event.throwableToString(throwable, sb, "\r\n");
		}
		
		if (dump != null) {
			sb.append("\r\n");
			dump.dumptoString(sb, "\r\n");
			sb.append("\r\n");
		}
		
		return sb.toString();
	}
	
	public ArrayList<Log2Dump> getTablecontent() {
		return tablecontent;
	}
	
}
