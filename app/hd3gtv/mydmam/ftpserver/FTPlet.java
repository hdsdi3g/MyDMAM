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
package hd3gtv.mydmam.ftpserver;

import java.io.IOException;
import java.util.HashSet;

import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.FtpletContext;
import org.apache.ftpserver.ftplet.FtpletResult;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.ftpserver.Session.Action;

public class FTPlet implements Ftplet {
	
	private static final HashSet<String> valid_commands;
	
	static {
		valid_commands = new HashSet<String>();
		for (int pos = 0; pos < Action.values().length; pos++) {
			valid_commands.add(Action.values()[pos].name());
		}
	}
	
	public void init(FtpletContext ftpletContext) throws FtpException {
	}
	
	public FtpletResult onConnect(FtpSession session) throws FtpException, IOException {
		try {
			session.setAttribute("dbsession", Session.create(session));
		} catch (Exception e) {
			Loggers.FTPserver.warn("Can't create session for " + session.getUser().getName() + " " + session.getSessionId(), e);
		}
		return null;
	}
	
	public FtpletResult onDisconnect(FtpSession session) throws FtpException, IOException {
		try {
			getSessionFromServer(session).flush().close();
		} catch (Exception e) {
			Loggers.FTPserver.warn("Can't flush/close session for " + session.getUser().getName() + " " + session.getSessionId(), e);
		}
		return null;
	}
	
	public void destroy() {
	}
	
	public FtpletResult beforeCommand(FtpSession session, FtpRequest request) throws FtpException, IOException {
		return null;
	}
	
	private static Session getSessionFromServer(FtpSession ftpsession) throws ConnectionException {
		Object _session = ftpsession.getAttribute("dbsession");
		if (_session == null) {
			return Session.get(ftpsession);
		}
		if (_session instanceof Session) {
			return (Session) _session;
		} else {
			return Session.get(ftpsession);
		}
	}
	
	public FtpletResult afterCommand(FtpSession session, FtpRequest request, FtpReply reply) throws FtpException, IOException {
		if (session.getUser() == null) {
			return null;
		}
		if (request.hasArgument() == false) {
			return null;
		}
		if (valid_commands.contains(request.getCommand()) == false) {
			return null;
		}
		
		try {
			getSessionFromServer(session).pushActivity(session, request);
		} catch (ConnectionException e) {
			Loggers.FTPserver.warn("Can't get session entry for " + session.getUser().getName() + " " + session.getSessionId(), e);
		}
		
		return null;
	}
	
}
