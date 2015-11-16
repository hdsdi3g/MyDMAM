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

import java.util.List;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.apache.ftpserver.filesystem.nativefs.impl.NativeFtpFile;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;

public class Session {
	
	private String key;
	private String client_host;
	private long login_time;
	private String user_id;
	
	private Session() {
	}
	
	static Session create(FtpSession ftp_session) {
		Session session = new Session();
		session.key = "ftpsession-" + ftp_session.getSessionId().toString();
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
	
	void commit() {
		// TODO
	}
	
	enum Action {
		DELE, REST, STOR, RETR
	}
	
	public class SessionActivity implements Delayed {
		private String activity_key;
		private String working_directory;
		private Action action;
		private String session_key;
		private String session_user_id;
		private String argument;
		private long creation_date;
		
		private SessionActivity(String working_directory, String command, String argument) {
			this.working_directory = working_directory;
			if (working_directory == null) {
				throw new NullPointerException("\"working_directory\" can't to be null");
			}
			this.argument = argument;
			if (argument == null) {
				throw new NullPointerException("\"argument\" can't to be null");
			}
			
			this.action = Action.valueOf(command);
			session_key = key;
			session_user_id = user_id;
			
			creation_date = System.currentTimeMillis();
			activity_key = key + "-" + creation_date;
		}
		
		void save() {
			// TODO
		}
		
		@Override
		public long getDelay(TimeUnit unit) {
			// TODO Auto-generated method stub
			return 0;
		}
		
		public int compareTo(Delayed o) {
			if (this.creation_date < ((SessionActivity) o).creation_date) {
				return -1;
			}
			if (this.creation_date > ((SessionActivity) o).creation_date) {
				return 1;
			}
			return 0;
		}
	}
	
	void pushActivity(FtpSession session, FtpRequest request) throws FtpException {
		SessionActivity activity = new SessionActivity(((NativeFtpFile) session.getFileSystemView().getWorkingDirectory()).getAbsolutePath(), request.getCommand(), request.getArgument());
		activity.save();
	}
	
	List<SessionActivity> getSessionActivities() {
		// TODO
		return null;
	}
	
}
