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

import java.util.concurrent.TimeUnit;

import org.apache.ftpserver.filesystem.nativefs.impl.NativeFtpFile;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequestBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.db.Elasticsearch;

public class FTPActivity {
	
	private static final int TTL_LONG_ACTIVITY = (int) TimeUnit.DAYS.toMillis(365 * 2);
	private static final int TTL_SHORT_ACTIVITY = (int) TimeUnit.DAYS.toMillis(30 * 1);
	
	private static final String ES_INDEX = "ftpserver";
	private static final String ES_TYPE = "ftpserverlogactivity";
	
	static {
		try {
			Elasticsearch.enableTTL(ES_INDEX, ES_TYPE);
		} catch (Exception e) {
			Loggers.FTPserver.error("Can't to set TTL for ES", e);
		}
	}
	
	enum Action {
		DELE, REST, STOR, RETR, APPE, RMD, RNFR, RNTO, MKD;
	}
	
	private transient String activity_key;
	
	private String session_key;
	@SuppressWarnings("unused")
	private String client_host;
	private long login_time;
	@SuppressWarnings("unused")
	private String user_id;
	@SuppressWarnings("unused")
	private String working_directory;
	private Action action;
	@SuppressWarnings("unused")
	private String argument;
	private long activity_date;
	
	private FTPActivity() {
	}
	
	@SuppressWarnings("unchecked")
	static void push(FtpSession session, FtpRequest request) throws FtpException {
		FTPActivity activity = new FTPActivity();
		activity.activity_date = System.currentTimeMillis();
		activity.session_key = session.getSessionId().toString();
		activity.login_time = session.getLoginTime().getTime();
		activity.activity_key = activity.session_key + "-" + (activity.activity_date - activity.login_time);
		activity.client_host = session.getClientAddress().getHostString();
		
		FTPUser user = (FTPUser) session.getUser();
		activity.user_id = user.getUserId();
		activity.working_directory = ((NativeFtpFile) session.getFileSystemView().getWorkingDirectory()).getAbsolutePath();
		activity.action = Action.valueOf(request.getCommand());
		activity.argument = request.getArgument();
		
		long ttl = TTL_LONG_ACTIVITY;
		if (user.getGroup().isShortActivityLog()) {
			ttl = TTL_SHORT_ACTIVITY;
		}
		
		Elasticsearch.getClient().prepareIndex(ES_INDEX, ES_TYPE, activity.activity_key).setSource(FTPOperations.getGson().toJson(activity)).setTTL(ttl)
				.execute(Elasticsearch.createEmptyActionListener(Loggers.FTPserver, FTPActivity.class));
				
		if (activity.action != Action.REST & activity.action != Action.RETR) {
			FTPOperations.get().addActiveUser((FTPUser) session.getUser());
		}
	}
	
	static void purgeUserActivity(String user_id) {
		DeleteByQueryRequestBuilder request = Elasticsearch.getClient().prepareDeleteByQuery(ES_INDEX).setTypes(ES_TYPE);
		request.setQuery(QueryBuilders.termQuery("user_id", user_id));
		Elasticsearch.deleteByQuery(request.request());
	}
	
}
