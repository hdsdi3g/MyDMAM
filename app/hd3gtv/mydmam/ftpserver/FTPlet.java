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

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.ftpserver.Session.Action;

public class FTPlet implements Ftplet {
	
	private static final HashSet<String> ignore_commands;
	private static final HashSet<String> valid_commands;
	
	static {
		ignore_commands = new HashSet<String>();
		ignore_commands.add("PASS");
		ignore_commands.add("SYST");
		ignore_commands.add("FEAT");
		ignore_commands.add("PWD");
		ignore_commands.add("TYPE");
		ignore_commands.add("TYPE");
		ignore_commands.add("EPSV");
		ignore_commands.add("LIST");
		ignore_commands.add("QUIT");
		ignore_commands.add("MKD");
		ignore_commands.add("OPTS");
		ignore_commands.add("CWD");
		
		valid_commands = new HashSet<String>();
		for (int pos = 0; pos < Action.values().length; pos++) {
			valid_commands.add(Action.values()[pos].name());
		}
	}
	
	public void init(FtpletContext ftpletContext) throws FtpException {
	}
	
	public FtpletResult onConnect(FtpSession session) throws FtpException, IOException {
		Session.create(session);
		return null;
	}
	
	public FtpletResult onDisconnect(FtpSession session) throws FtpException, IOException {
		Session.get(session).commit();
		return null;
	}
	
	public void destroy() {
	}
	
	public FtpletResult beforeCommand(FtpSession session, FtpRequest request) throws FtpException, IOException {
		return null;
	}
	
	public FtpletResult afterCommand(FtpSession session, FtpRequest request, FtpReply reply) throws FtpException, IOException {
		if (session.getUser() == null) {
			return null;
		}
		if (request.hasArgument() == false) {
			return null;
		}
		if (ignore_commands.contains(request.getCommand())) {
			return null;
		}
		if (valid_commands.contains(request.getCommand()) == false) {
			Loggers.FTPserver.warn("Unknow FTP command: " + request.getCommand() + " " + request.getArgument());
			return null;
		}
		Session.get(session).pushActivity(session, request);
		return null;
	}
	
}
