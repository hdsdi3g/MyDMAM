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

public class FTPlet implements Ftplet {
	
	private static final HashSet<String> ignore_commands;
	
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
	}
	
	public void init(FtpletContext ftpletContext) throws FtpException {
	}
	
	public void destroy() {
	}
	
	@Override
	public FtpletResult beforeCommand(FtpSession session, FtpRequest request) throws FtpException, IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
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
		System.err.println(session.getFileSystemView().getWorkingDirectory().getAbsolutePath());
		// TODO triggers : discard { DELE, REST }, add/update { STOR }, get { RETR }, session.getSessionId()
		Loggers.FTPserver.info("After cmd: " + session.getUser().getName() + ":" + session.getClientAddress().getHostString() + " " + request.getCommand() + " > " + request.getArgument());
		return null;
	}
	
	@Override
	public FtpletResult onConnect(FtpSession session) throws FtpException, IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public FtpletResult onDisconnect(FtpSession session) throws FtpException, IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
}
