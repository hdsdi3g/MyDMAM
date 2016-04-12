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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;
import org.apache.ftpserver.filesystem.nativefs.impl.NativeFtpFile;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.FtpletContext;
import org.apache.ftpserver.ftplet.FtpletResult;

import hd3gtv.mydmam.ftpserver.FTPActivity.Action;
import hd3gtv.tools.CopyMove;

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
		return null;
	}
	
	public FtpletResult onDisconnect(FtpSession session) throws FtpException, IOException {
		FTPOperations.get().removeActiveUser((FTPUser) session.getUser());
		return null;
	}
	
	public void destroy() {
	}
	
	public FtpletResult beforeCommand(FtpSession session, FtpRequest request) throws FtpException, IOException {
		if (request.getCommand().equals("RMD")) {
			NativeFtpFile selected_file = (NativeFtpFile) session.getFileSystemView().getFile(request.getArgument());
			File real_file = selected_file.getPhysicalFile();
			if (real_file.exists() == false | real_file.isDirectory() == false | real_file.canWrite() == false) {
				return null;
			}
			
			/**
			 * Search no hidden files
			 */
			if (real_file.listFiles(new FileFilter() {
				
				public boolean accept(File pathname) {
					return CopyMove.isHidden(pathname) == false;
				}
			}).length > 0) {
				/**
				 * Don't force delete
				 */
				return null;
			}
			
			/**
			 * No files / no visible file (== maybe some hidden files), clean dir.
			 */
			FileUtils.cleanDirectory(real_file);
		}
		return null;
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
		FTPActivity.push(session, request);
		
		return null;
	}
	
}
