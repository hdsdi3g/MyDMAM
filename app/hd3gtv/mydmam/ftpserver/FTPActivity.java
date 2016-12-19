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

import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVStrategy;
import org.apache.ftpserver.filesystem.nativefs.impl.NativeFtpFile;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.db.ElasticsearchCrawlerToBulk;
import hd3gtv.mydmam.db.ElasticsearchCrawlerToDelete;
import hd3gtv.mydmam.db.ElastisearchCrawlerHit;
import hd3gtv.mydmam.db.ElastisearchCrawlerReader;
import hd3gtv.mydmam.ftpserver.AJSRequestRecent.SearchBySelectActionType;
import hd3gtv.mydmam.web.search.SearchQuery;

public class FTPActivity {
	
	private static final long TTL_LONG_ACTIVITY = TimeUnit.DAYS.toMillis(365 * 2);
	private static final long TTL_SHORT_ACTIVITY = TimeUnit.DAYS.toMillis(30 * 1);
	
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
		DELE, REST, STOR, RETR, APPE, RMD, RNTO, RNFR, MKD;
		
		String toLogString() {
			switch (this) {
			case DELE:
				return "delete";
			case REST:
				return "restore";
			case STOR:
				return "store";
			case RETR:
				return "retrieve";
			case APPE:
				return "append";
			case RMD:
				return "rmdir";
			case RNTO:
				return "rename_to";
			case RNFR:
				return "rename_from";
			case MKD:
				return "mkdir";
			default:
				return "Other";
			}
		}
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
	private String user_session_ref;
	private long file_size;
	private long file_offset;
	
	private String user_name;
	private String user_domain;
	private String user_group;
	
	private FTPActivity() {
	}
	
	public String toString() {
		LinkedHashMap<String, Object> log = new LinkedHashMap<String, Object>();
		log.put("activity_key", activity_key);
		log.put("session_key", session_key);
		log.put("client_host", client_host);
		log.put("login_time", new Date(login_time));
		log.put("user_id", user_id);
		log.put("user_name", user_name);
		log.put("user_domain", user_domain);
		log.put("user_group", user_group);
		log.put("user_session_ref", user_session_ref);
		log.put("working_directory", working_directory);
		log.put("action", action);
		log.put("argument", argument);
		log.put("activity_date", activity_date);
		log.put("file_size", file_size);
		log.put("file_offset", file_offset);
		return log.toString();
	}
	
	@SuppressWarnings("unchecked")
	static void push(FtpSession session, FtpRequest request) throws FtpException {
		FTPUser user = (FTPUser) session.getUser();
		
		Action action = Action.valueOf(request.getCommand());
		if (action != Action.REST & action != Action.RETR) {
			FTPOperations.get().addActiveUser((FTPUser) session.getUser());
		}
		
		if (user.getGroup().isUserHasActivityDisabled(user.getName())) {
			if (Loggers.FTPserver.isTraceEnabled()) {
				Loggers.FTPserver.trace("No activity for " + user);
			}
			return;
		}
		
		FTPActivity activity = new FTPActivity();
		activity.activity_date = System.currentTimeMillis();
		activity.session_key = session.getSessionId().toString();
		activity.login_time = session.getLoginTime().getTime();
		activity.activity_key = activity.session_key + "-" + (activity.activity_date - activity.login_time);
		activity.client_host = session.getClientAddress().getHostString();
		
		activity.user_id = user.getUserId();
		activity.working_directory = ((NativeFtpFile) session.getFileSystemView().getWorkingDirectory()).getAbsolutePath();
		activity.user_name = user.getName();
		activity.user_domain = user.getDomain();
		activity.user_group = user.getGroupName();
		
		activity.action = action;
		if (activity.action == Action.RNFR) {
			activity.argument = request.getArgument() + " >";
		} else if (activity.action == Action.RNTO) {
			activity.argument = "> " + request.getArgument();
		} else {
			activity.argument = request.getArgument();
		}
		
		if (activity.action != Action.MKD & activity.action != Action.RMD) {
			FtpFile ftp_file = session.getFileSystemView().getFile(activity.argument);
			if (ftp_file != null) {
				if (ftp_file.doesExist() & ftp_file.isFile()) {
					activity.file_size = ftp_file.getSize();
				}
			}
			activity.file_offset = session.getFileOffset();
		}
		
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(activity.user_id.getBytes("UTF-8"));
			activity.user_session_ref = MyDMAM.byteToString(md.digest());
		} catch (Exception e) {
			Loggers.FTPserver.error("Can't compute digest", e);
			activity.user_session_ref = activity.user_id;
		}
		
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
	}
	
	/**
	 * @param user_session_ref is null, return null;
	 */
	private static QueryBuilder searchByUserSessionRef(String user_session_ref) {
		if (user_session_ref == null) {
			return null;
		}
		if (user_session_ref.isEmpty()) {
			return null;
		}
		return QueryBuilders.termQuery("user_session_ref", user_session_ref);
	}
	
	public static void purgeUserActivity(String user_id) throws Exception {
		Loggers.FTPserver.info("Purge user activity: " + user_id);
		
		String user_session_ref = null;
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(user_id.getBytes("UTF-8"));
		user_session_ref = MyDMAM.byteToString(md.digest());
		
		ElasticsearchCrawlerToBulk bulk_delete = Elasticsearch.createCrawlerToBulk();
		bulk_delete.setIndices(ES_INDEX);
		bulk_delete.setTypes(ES_TYPE);
		bulk_delete.setQuery(searchByUserSessionRef(user_session_ref));
		bulk_delete.process(new ElasticsearchCrawlerToDelete());
	}
	
	private static String[] getCSVLine(Map<String, Object> source) {
		ArrayList<String> result = new ArrayList<String>();
		long activity_date = (Long) source.get("activity_date");
		
		result.add(MyDMAM.DATE_TIME_FORMAT.format(new Date(activity_date)));
		result.add((String) source.get("session_key"));
		result.add((String) source.get("user_name"));
		result.add((String) source.get("user_domain"));
		result.add((String) source.get("user_group"));
		result.add((String) source.get("user_session_ref"));
		result.add(Action.valueOf((String) source.get("action")).toLogString());
		result.add((String) source.get("working_directory"));
		result.add((String) source.get("argument"));
		result.add(String.valueOf(source.get("file_size")));
		result.add(String.valueOf(source.get("file_offset")));
		result.add(String.valueOf(Math.round((activity_date - (Long) source.get("login_time")) / 1000)));
		result.add((String) source.get("client_host"));
		return result.toArray(new String[result.size()]);
	}
	
	public static void getAllUserActivitiesCSV(String user_session_ref, OutputStream destination) throws Exception {
		if (Elasticsearch.isIndexExists(ES_INDEX) == false) {
			throw new Exception("noindex");
		}
		
		CSVStrategy strategy = new CSVStrategy(';', '\"', '#', '\\', true, true, true, true);
		final CSVPrinter printer = new CSVPrinter(destination).setStrategy(strategy);
		
		printer.println(new String[] { "Date", "Session", "User name", "User domain", "User group", "User session ref", "Action", "Working Directory", "Argument", "File size", "File offset",
				"Sec after login", "Client" });
		
		ElastisearchCrawlerReader ecr = Elasticsearch.createCrawlerReader();
		ecr.setIndices(ES_INDEX);
		ecr.setTypes(ES_TYPE);
		ecr.setQuery(searchByUserSessionRef(user_session_ref));
		ecr.addSort("activity_date", SortOrder.ASC);
		
		ecr.allReader(new ElastisearchCrawlerHit() {
			
			public boolean onFoundHit(SearchHit hit) throws Exception {
				printer.println(getCSVLine(hit.getSource()));
				return true;
			}
		});
	}
	
	public static ArrayList<FTPActivity> getRecentActivities(String user_session_ref, int max_items, String user_searched_text, SearchBySelectActionType searched_action_type) throws Exception {
		final ArrayList<FTPActivity> ftp_activity = new ArrayList<FTPActivity>();
		
		if (Elasticsearch.isIndexExists(ES_INDEX) == false) {
			return ftp_activity;
		}
		
		BoolQueryBuilder boolquerybuilder = QueryBuilders.boolQuery();
		String search_text = SearchQuery.cleanUserTextSearch(user_searched_text);
		if (search_text != null) {
			if (user_session_ref == null) {
				BoolQueryBuilder bqb_text = QueryBuilders.boolQuery();
				bqb_text.should(QueryBuilders.wildcardQuery("argument", "*" + search_text.toLowerCase() + "*"));
				bqb_text.should(QueryBuilders.wildcardQuery("user_name", "*" + search_text.toLowerCase() + "*"));
				boolquerybuilder.must(bqb_text);
			} else {
				boolquerybuilder.must(QueryBuilders.wildcardQuery("argument", "*" + search_text.toLowerCase() + "*"));
			}
		}
		
		QueryBuilder qb_search_by_user = searchByUserSessionRef(user_session_ref);
		if (qb_search_by_user != null) {
			boolquerybuilder.must(qb_search_by_user);
		}
		
		if (searched_action_type != SearchBySelectActionType.ALL) {
			boolquerybuilder.must(QueryBuilders.termsQuery("action", searched_action_type.toActionString()));
		}
		
		ElastisearchCrawlerReader ecr = Elasticsearch.createCrawlerReader();
		ecr.setIndices(ES_INDEX);
		ecr.setTypes(ES_TYPE);
		ecr.setQuery(boolquerybuilder);
		ecr.addSort("activity_date", SortOrder.DESC);
		if (max_items > 0 & max_items < 100) {
			ecr.setMaximumSize(max_items);
		} else {
			ecr.setMaximumSize(100);
		}
		// ecr.setDisplayTTLForEachResult(true);
		
		ecr.allReader(new ElastisearchCrawlerHit() {
			
			public boolean onFoundHit(SearchHit hit) throws Exception {
				ftp_activity.add(FTPOperations.getGson().fromJson(hit.getSourceAsString(), FTPActivity.class));
				return true;
			}
		});
		
		return ftp_activity;
	}
}
