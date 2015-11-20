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

import org.apache.ftpserver.filesystem.nativefs.impl.NativeFtpFile;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;

public class Session {
	
	private String session_key;
	private String client_host;
	private long login_time;
	private String user_id;
	private long creation_date;
	
	private transient FTPUser user;
	
	private Session() {
	}
	
	static Session create(FtpSession ftp_session) {
		Session session = new Session();
		session.session_key = "ftpsession-" + ftp_session.getSessionId().toString();
		session.client_host = ftp_session.getClientAddress().getHostString();
		session.login_time = ftp_session.getLoginTime().getTime();
		session.user_id = ((FTPUser) ftp_session.getUser()).getUserId();
		session.save();
		return session;
	}
	
	static Session get(FtpSession ftp_session) {
		Session session = null; // TODO get from db
		if (session == null) {
			session = create(ftp_session);
		}
		return session;
	}
	
	void save() {
		// TODO
	}
	
	void close() {
		// TODO
		FTPOperations.get().closeSession(this);
	}
	
	FTPUser getUser() {
		if (user == null) {
			user = FTPUser.getUserId(user_id);
		}
		return user;
	}
	
	enum Action {
		DELE, REST, STOR, RETR, APPE, RMD, RNFR, RNTO, MKD;
	}
	
	public class SessionActivity {
		private String activity_key;
		private String working_directory;
		private Action action;
		private String session_key;
		private String session_user_id;
		private String argument;
		private long activity_date;
		
		private SessionActivity(Session reference, FtpSession session, FtpRequest request) throws FtpException {
			working_directory = ((NativeFtpFile) session.getFileSystemView().getWorkingDirectory()).getAbsolutePath();
			argument = request.getArgument();
			
			action = Action.valueOf(request.getCommand());
			session_key = reference.session_key;
			session_user_id = reference.user_id;
			
			activity_date = System.currentTimeMillis();
			activity_key = session_key + "-" + activity_date;
			
			if (action != Action.REST & action != Action.RETR) {
				FTPOperations.get().addSession(reference);
			}
		}
		
		void save() {
			// TODO
		}
	}
	
	void pushActivity(FtpSession session, FtpRequest request) throws FtpException {
		new SessionActivity(this, session, request).save();
	}
	
}
