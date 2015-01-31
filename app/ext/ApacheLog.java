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
 * Copyright (C) hdsdi3g for hd3g.tv 2015
 * 
*/
package ext;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import play.PlayPlugin;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import controllers.Secure;

public class ApacheLog extends PlayPlugin {
	
	public static final SimpleDateFormat APACHE_DATE_FORMAT = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss ZZZ", DateFormatSymbols.getInstance(Locale.ENGLISH));
	
	/**
	 * Display Apache "NCSA extended/combined" log format
	 * %h %l %u %t \"%r\" %>s %b \"%{Referer}i\" \"%{User-agent}i\"
	 */
	private static void displayLogMessage(Request request, Response response) {
		StringBuilder sb = new StringBuilder();
		
		/**
		 * %h Remote host
		 */
		sb.append(request.remoteAddress);
		sb.append(" ");
		
		/**
		 * %l Remote logname (from identd, if supplied). This will return a dash unless mod_ident is present and IdentityCheck is set On.
		 */
		if (Secure.isConnected() == false) {
			sb.append("- ");
		} else {
			sb.append(Secure.connected());
			sb.append(" ");
		}
		
		/**
		 * %u Remote user (from auth; may be bogus if return status (%s) is 401)
		 */
		if (request.user == null) {
			sb.append("- ");
		} else {
			sb.append(request.user);
			sb.append(" ");
		}
		
		/**
		 * %t Time the request was received (standard english format)
		 */
		sb.append("[");
		sb.append(APACHE_DATE_FORMAT.format(request.date));
		sb.append("] ");
		
		/**
		 * \"%r\" First line of request
		 */
		sb.append("\"");
		sb.append(request.method);
		sb.append(" ");
		sb.append(request.path);
		if (request.querystring.isEmpty() == false) {
			sb.append("?");
			sb.append(request.querystring);
		}
		sb.append(" HTTP/1.1\" ");
		
		/**
		 * %>s Status. For requests that got internally redirected, this is the status of the *original* request --- %>s for the last.
		 */
		sb.append(response.status);
		sb.append(" ");
		
		/**
		 * %b Size of response in bytes, excluding HTTP headers. In CLF format, i.e. a '-' rather than a 0 when no bytes are sent.
		 */
		sb.append(response.out.size());
		sb.append(" ");
		
		/**
		 * \"%{Referer}i\" request header "Referer" value
		 */
		sb.append("\"");
		if (request.headers.containsKey("referer")) {
			sb.append(request.headers.get("referer").value());
		} else {
			sb.append("-");
		}
		sb.append("\" ");
		
		/**
		 * \"%{User-agent}i\" request header "User-agent" value
		 */
		sb.append("\"");
		if (request.headers.containsKey("user-agent")) {
			sb.append(request.headers.get("user-agent").value());
		} else {
			sb.append("-");
		}
		sb.append("\"");
		
		System.out.println(sb.toString());// TODO to real log file
	}
	
	public void afterInvocation() {
		displayLogMessage(Http.Request.current(), Http.Response.current());
	}
	
	public static final SimpleDateFormat APACHE_ERROR_DATE_FORMAT = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", DateFormatSymbols.getInstance(Locale.ENGLISH));
	
	public void onInvocationException(Throwable e) {
		
		Request request = Http.Request.current();
		
		StringBuilder sb = new StringBuilder();
		
		// [Sat Jan 31 16:35:01 2015] [error] [client 5.196.27.8] client sent HTTP/1.1 request without hostname (see RFC2616 section 14.23): /w00tw00t.at.ISC.SANS.DFind:)
		sb.append("[");
		sb.append(APACHE_ERROR_DATE_FORMAT.format(new Date()));
		sb.append("] ");
		
		sb.append("[error] ");
		
		sb.append("[client ");
		sb.append(request.remoteAddress);
		sb.append("] ");
		
		if (e.getCause() != null) {
			sb.append(e.getCause().getClass().getName());
		} else {
			sb.append(e.getClass().getName());
		}
		
		sb.append(": ");
		sb.append(e.getMessage());
		System.err.println(sb.toString());// TODO to real log file
	}
	
}
