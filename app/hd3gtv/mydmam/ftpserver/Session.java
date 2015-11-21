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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.apache.ftpserver.filesystem.nativefs.impl.NativeFtpFile;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;

import com.google.common.reflect.TypeToken;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.query.IndexQuery;
import com.netflix.astyanax.serializers.StringSerializer;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.db.DeployColumnDef;

public class Session {
	
	private static final ColumnFamily<String, String> CF_SESSION = new ColumnFamily<String, String>("ftpServerSession", StringSerializer.get(), StringSerializer.get());
	private static final ColumnFamily<String, String> CF_ACTIVITY = new ColumnFamily<String, String>("ftpServerSessionActivity", StringSerializer.get(), StringSerializer.get());
	private static Keyspace keyspace;
	private static final int TTL_SESSION = (int) TimeUnit.DAYS.toSeconds(365 * 2);
	private static final int TTL_ACTIVITY = (int) TimeUnit.DAYS.toSeconds(30);
	
	static {
		try {
			keyspace = CassandraDb.getkeyspace();
			String default_keyspacename = CassandraDb.getDefaultKeyspacename();
			if (CassandraDb.isColumnFamilyExists(keyspace, CF_SESSION.getName()) == false) {
				CassandraDb.createColumnFamilyString(default_keyspacename, CF_SESSION.getName(), true);
				// String queue_name = CF_FTPUSER.getName();
				// CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "status", queue_name + "_status", DeployColumnDef.ColType_AsciiType);
				// CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "creator_hostname", queue_name + "_creator_hostname", DeployColumnDef.ColType_UTF8Type);
				// CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "expiration_date", queue_name + "_expiration_date", DeployColumnDef.ColType_LongType);
				// CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "update_date", queue_name + "_update_date", DeployColumnDef.ColType_LongType);
				// CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "delete", queue_name + "_delete", DeployColumnDef.ColType_Int32Type);
				// CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "indexingdebug", queue_name + "_indexingdebug", DeployColumnDef.ColType_Int32Type);
			}
			if (CassandraDb.isColumnFamilyExists(keyspace, CF_ACTIVITY.getName()) == false) {
				CassandraDb.createColumnFamilyString(default_keyspacename, CF_ACTIVITY.getName(), false);
				CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_ACTIVITY, "session_key", CF_ACTIVITY.getName() + "_session_key", DeployColumnDef.ColType_AsciiType);
			}
		} catch (Exception e) {
			Loggers.FTPserver.error("Can't init database CFs", e);
		}
	}
	
	private String session_key;
	private String client_host;
	private long login_time;
	private String user_id;
	private long creation_date;
	
	private ArrayList<SessionActivity> activity;
	
	private static final Type al_SessionActivity = new TypeToken<ArrayList<SessionActivity>>() {
	}.getType();
	
	private transient FTPUser user;
	
	private Session() {
	}
	
	private static String makeSessionId(FtpSession ftp_session) {
		return "ftpsession-" + ftp_session.getSessionId().toString();
	}
	
	/**
	 * Session will be saved.
	 */
	static Session create(FtpSession ftp_session) throws ConnectionException {
		Session session = new Session();
		session.session_key = makeSessionId(ftp_session);
		session.client_host = ftp_session.getClientAddress().getHostString();
		session.login_time = ftp_session.getLoginTime().getTime();
		session.user_id = ((FTPUser) ftp_session.getUser()).getUserId();
		session.creation_date = System.currentTimeMillis();
		session.activity = new ArrayList<Session.SessionActivity>(1);
		session.save();
		return session;
	}
	
	static Session get(FtpSession ftp_session) throws ConnectionException {
		String key = makeSessionId(ftp_session);
		
		ColumnList<String> row = keyspace.prepareQuery(CF_SESSION).getKey(key).execute().getResult();
		if (row.isEmpty()) {
			return create(ftp_session);
		}
		
		Session session = new Session();
		session.importFromDb(key, row);
		return session;
	}
	
	private void importFromDb(String key, ColumnList<String> cols) {
		session_key = key;
		client_host = cols.getStringValue("client_host", "");
		login_time = cols.getLongValue("login_time", -1l);
		user_id = cols.getStringValue("user_id", "");
		creation_date = cols.getLongValue("creation_date", -1l);
		activity = FTPOperations.getGson().fromJson(cols.getStringValue("activity", "[]"), al_SessionActivity);
	}
	
	void save(MutationBatch mutator) {
		mutator.withRow(CF_SESSION, session_key).putColumn("client_host", client_host, TTL_SESSION);
		mutator.withRow(CF_SESSION, session_key).putColumn("login_time", login_time, TTL_SESSION);
		mutator.withRow(CF_SESSION, session_key).putColumn("user_id", user_id, TTL_SESSION);
		mutator.withRow(CF_SESSION, session_key).putColumn("creation_date", creation_date, TTL_SESSION);
		mutator.withRow(CF_SESSION, session_key).putColumn("activity", FTPOperations.getGson().toJson(activity), TTL_SESSION);
	}
	
	void save() throws ConnectionException {
		MutationBatch mutator = keyspace.prepareMutationBatch();
		save(mutator);
		mutator.execute();
	}
	
	Session flush() throws ConnectionException {
		IndexQuery<String, String> index_query = keyspace.prepareQuery(CF_ACTIVITY).searchWithIndex();
		index_query.addExpression().whereColumn("session_key").equals().value(session_key);
		
		Rows<String, String> rows = index_query.execute().getResult();
		if (rows.isEmpty() == false) {
			activity.ensureCapacity(activity.size() + rows.size());
			
			MutationBatch mutator = keyspace.prepareMutationBatch();
			for (Row<String, String> row : rows) {
				if (row.getColumns().isEmpty()) {
					continue;
				}
				activity.add(new SessionActivity(row.getKey(), row.getColumns()));
				
				mutator.withRow(CF_ACTIVITY, row.getKey()).delete();
			}
			save(mutator);
			mutator.execute();
		}
		
		return this;
	}
	
	void close() {
		FTPOperations.get().closeSession(this);
	}
	
	FTPUser getUser() throws ConnectionException {
		if (user == null) {
			user = FTPUser.getUserId(user_id, false);
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
		
		private SessionActivity(Session reference, FtpSession session, FtpRequest request) throws FtpException, ConnectionException {
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
			
			MutationBatch mutator = keyspace.prepareMutationBatch();
			mutator.withRow(CF_ACTIVITY, activity_key).putColumn("working_directory", working_directory, TTL_ACTIVITY);
			mutator.withRow(CF_ACTIVITY, activity_key).putColumn("action", action.name(), TTL_ACTIVITY);
			mutator.withRow(CF_ACTIVITY, activity_key).putColumn("session_key", session_key, TTL_ACTIVITY);
			mutator.withRow(CF_ACTIVITY, activity_key).putColumn("session_user_id", session_user_id, TTL_ACTIVITY);
			mutator.withRow(CF_ACTIVITY, activity_key).putColumn("argument", argument, TTL_ACTIVITY);
			mutator.withRow(CF_ACTIVITY, activity_key).putColumn("activity_date", activity_date, TTL_ACTIVITY);
			mutator.execute();
		}
		
		private SessionActivity(String key, ColumnList<String> cols) {
			activity_key = key;
			working_directory = cols.getStringValue("working_directory", "");
			action = Action.valueOf(cols.getStringValue("action", null));
			session_key = cols.getStringValue("", "");
			session_user_id = cols.getStringValue("session_user_id", "");
			argument = cols.getStringValue("argument", "");
			activity_date = cols.getLongValue("activity_date", -1l);
		}
		
	}
	
	void pushActivity(FtpSession session, FtpRequest request) throws FtpException {
		try {
			new SessionActivity(this, session, request);
		} catch (ConnectionException e) {
			Loggers.FTPserver.warn("Can't create/save session activity", e);
		}
	}
	
}
