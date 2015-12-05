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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVStrategy;
import org.apache.ftpserver.filesystem.nativefs.impl.NativeFtpFile;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.db.ElastisearchCrawlerHit;
import hd3gtv.mydmam.db.ElastisearchCrawlerReader;

public class FTPActivity {
	
	private static final int TTL_LONG_ACTIVITY = (int) TimeUnit.DAYS.toSeconds(365 * 2);
	private static final int TTL_SHORT_ACTIVITY = (int) TimeUnit.DAYS.toSeconds(30 * 1);
	
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
	private String client_host;
	private long login_time;
	private String user_id;
	private String working_directory;
	private Action action;
	private String argument;
	private long activity_date;
	
	private FTPActivity() {
	}
	
	public String toString() {
		LinkedHashMap<String, Object> log = new LinkedHashMap<String, Object>();
		log.put("activity_key", activity_key);
		log.put("session_key", session_key);
		log.put("client_host", client_host);
		log.put("login_time", new Date(login_time));
		log.put("user_id", user_id);
		log.put("working_directory", working_directory);
		log.put("action", action);
		log.put("argument", argument);
		log.put("activity_date", activity_date);
		return log.toString();
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
		
		if (Loggers.FTPserver.isTraceEnabled()) {
			Loggers.FTPserver.trace("Push FTP Activity: " + activity);
		}
		
		long ttl = TTL_LONG_ACTIVITY;
		if (user.getGroup().isShortActivityLog()) {
			ttl = TTL_SHORT_ACTIVITY;
		}
		
		try {
			Elasticsearch.getClient().prepareIndex(ES_INDEX, ES_TYPE, activity.activity_key).setSource(FTPOperations.getGson().toJson(activity)).setTTL(ttl)
					.execute(Elasticsearch.createEmptyActionListener(Loggers.FTPserver, FTPActivity.class));
		} catch (Exception e) {
			Loggers.FTPserver.warn("Error during store activity in DB: " + e);
		}
		
		if (activity.action != Action.REST & activity.action != Action.RETR) {
			FTPOperations.get().addActiveUser((FTPUser) session.getUser());
		}
	}
	
	private static QueryBuilder searchByUserId(String user_id) {
		if (user_id == null) {
			throw new NullPointerException("\"user_id\" can't to be null");
		}
		if (user_id.isEmpty()) {
			throw new NullPointerException("\"user_id\" can't to be empty");
		}
		return QueryBuilders.termQuery("user_id", user_id);
	}
	
	static void purgeUserActivity(String user_id) {
		Loggers.FTPserver.info("Purge user activity: " + user_id);
		// (ES_INDEX).setTypes(ES_TYPE); request.setQuery(searchByUserId(user_id));
		// TODO re-implement DeleteByQuery with search + bulk
	}
	
	private static String[] getCSVLine(Map<String, Object> source) {
		String[] result = new String[8];
		long activity_date = (Long) source.get("activity_date");
		result[0] = MyDMAM.DATE_TIME_FORMAT.format(new Date(activity_date));
		result[1] = (String) source.get("session_key");
		result[2] = (String) source.get("user_id");
		result[3] = Action.valueOf((String) source.get("action")).name();
		result[4] = (String) source.get("working_directory");
		result[5] = (String) source.get("argument");
		result[6] = String.valueOf(Math.round((activity_date - (Long) source.get("login_time")) / 1000));
		result[7] = (String) source.get("client_host");
		return result;
	}
	
	public static String getAllUserActivitiesCSV(String user_id) throws Exception {
		if (Elasticsearch.isIndexExists(ES_INDEX) == false) {
			return "";
		}
		
		CSVStrategy strategy = new CSVStrategy(';', '\"', '#', '\\', true, true, true, true);
		StringWriter sw = new StringWriter();
		final CSVPrinter printer = new CSVPrinter(sw).setStrategy(strategy);
		
		printer.println(new String[] { "Date", "Session", "User id", "Action", "Working Directory", "Argument", "Sec after login", "Client" });
		
		ElastisearchCrawlerReader ecr = Elasticsearch.createCrawlerReader();
		ecr.setIndices(ES_INDEX);
		ecr.setTypes(ES_TYPE);
		ecr.setQuery(searchByUserId(user_id));
		ecr.addSort("activity_date", SortOrder.ASC);
		
		ecr.allReader(new ElastisearchCrawlerHit() {
			
			public boolean onFoundHit(SearchHit hit) throws Exception {
				printer.println(getCSVLine(hit.getSource()));
				return true;
			}
		});
		
		return sw.toString();
	}
	
	public static ArrayList<FTPActivity> getRecentActivities(String user_id, long last_time) throws Exception {
		final ArrayList<FTPActivity> ftp_activity = new ArrayList<FTPActivity>();
		
		if (Elasticsearch.isIndexExists(ES_INDEX) == false) {
			return ftp_activity;
		}
		
		QueryBuilder querybuilder = null;
		if (last_time > 0) {
			querybuilder = QueryBuilders.rangeQuery("activity_date").gt(last_time);
			if (user_id != null) {
				querybuilder = QueryBuilders.boolQuery().must(querybuilder).must(searchByUserId(user_id));
			}
			if (Elasticsearch.countRequest(ES_INDEX, querybuilder, ES_TYPE) == 0) {
				querybuilder = null;
			}
		}
		
		if (querybuilder == null) {
			querybuilder = searchByUserId(user_id);
		}
		
		ElastisearchCrawlerReader ecr = Elasticsearch.createCrawlerReader();
		ecr.setIndices(ES_INDEX);
		ecr.setTypes(ES_TYPE);
		ecr.setQuery(querybuilder);
		ecr.addSort("activity_date", SortOrder.DESC);
		ecr.setMaximumSize(100);
		
		ecr.allReader(new ElastisearchCrawlerHit() {
			
			public boolean onFoundHit(SearchHit hit) throws Exception {
				ftp_activity.add(FTPOperations.getGson().fromJson(hit.getSourceAsString(), FTPActivity.class));
				return true;
			}
		});
		
		return ftp_activity;
	}
}
