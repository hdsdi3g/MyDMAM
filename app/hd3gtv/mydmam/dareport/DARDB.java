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
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
*/
package hd3gtv.mydmam.dareport;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.mail.internet.InternetAddress;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.serializers.StringSerializer;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.db.CassandraDb;

public class DARDB {
	
	static final ColumnFamily<String, String> CF_DAR = new ColumnFamily<String, String>("dareport", StringSerializer.get(), StringSerializer.get());
	private Keyspace keyspace;
	static final int TTL = (int) TimeUnit.DAYS.toSeconds(31);
	
	private static DARDB importFromConfiguration(Configuration configuration) throws ConnectionException {
		Keyspace keyspace = CassandraDb.getkeyspace();
		String default_keyspacename = CassandraDb.getDefaultKeyspacename();
		if (CassandraDb.isColumnFamilyExists(keyspace, CF_DAR.getName()) == false) {
			CassandraDb.createColumnFamilyString(default_keyspacename, CF_DAR.getName(), true);
		}
		
		JsonElement dar_conf = MyDMAM.gson_kit.getGsonSimple().toJsonTree(configuration.getRaw("dareport_setup"));
		DARDB result = MyDMAM.gson_kit.getGsonSimple().fromJson(dar_conf, DARDB.class);
		result.keyspace = keyspace;
		return result;
	}
	
	private static DARDB instance;
	
	public static DARDB get() {
		if (instance == null) {
			try {
				instance = importFromConfiguration(Configuration.global);
			} catch (ConnectionException e) {
				Loggers.DAReport.error("Can't init Cassandra CF", e);
			}
		}
		return instance;
	}
	
	private LinkedHashMap<String, ArrayList<Panel>> panels;
	private LinkedHashMap<String, Job> jobs;
	private ArrayList<InternetAddress> manager_addrs;
	private Locale mail_locale;
	
	/**
	 * Like 03:00:00
	 */
	private String send_time;
	
	class Job {
		String name;
		String panels;
		
		public String toString() {
			return "Job " + name + ": " + panels;
		}
	}
	
	enum PanelType {
		radiobox
	}
	
	class Panel {
		PanelType type;
		String label;
		String tips;
		boolean isstrong;
		boolean reverse_boolean;
		
		public String toString() {
			return type + " \"" + label + "\" [" + tips + "] S:" + isstrong;
		}
	}
	
	LinkedHashMap<String, Job> getJobs() {
		return jobs;
	}
	
	/**
	 * @return never null
	 */
	Locale getMailLocale() {
		if (mail_locale == null) {
			return Locale.getDefault();
		}
		return mail_locale;
	}
	
	private ScheduledExecutorService scheduled_ex_service;
	
	private DARDB() {
		/*
		scheduled_ex_service.shutdown();
		try {
			scheduled_ex_service.awaitTermination(500, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e1) {
		}
		scheduled_ex_service.shutdownNow();
		 */
		
		scheduled_ex_service = Executors.newScheduledThreadPool(MyDMAM.CPU_COUNT, r -> {
			Thread t = new Thread(r);
			t.setDaemon(true);
			t.setPriority(Thread.MIN_PRIORITY);
			t.setName("Regular DARDB");
			return t;
		});
		
	}
	
	public String toString() {
		LinkedHashMap<String, Object> log = new LinkedHashMap<String, Object>();
		log.put("jobs", jobs);
		log.put("panels", panels);
		log.put("manager_addrs", manager_addrs);
		log.put("send_time", send_time);
		return log.toString();
	}
	
	Keyspace getKeyspace() {
		return keyspace;
	}
	
	ArrayList<InternetAddress> getManagerAddrs() {
		return manager_addrs;
	}
	
	public ArrayList<Panel> getPanelsForJob(String job_name) {
		String panel_name = jobs.getOrDefault(job_name, new Job()).panels;
		
		if (panel_name == null) {
			new ArrayList<>(1);
		}
		
		return panels.getOrDefault(panel_name, new ArrayList<>(1));
	}
	
	public String getJobLongName(String job_key) {
		return jobs.getOrDefault(job_key, new Job()).name;
	}
	
	public JsonObject allDeclaredJobs() {
		JsonObject result = new JsonObject();
		
		jobs.forEach((name, job) -> {
			result.addProperty(name, job.name);
		});
		
		return result;
	}
	
	private Calendar parseSendTime() {
		try {
			String[] time_unit = send_time.split(":");
			Calendar c = Calendar.getInstance();
			c.set(Calendar.HOUR_OF_DAY, Integer.valueOf(time_unit[0]));
			c.set(Calendar.MINUTE, Integer.valueOf(time_unit[1]));
			if (time_unit.length > 2) {
				c.set(Calendar.SECOND, Integer.valueOf(time_unit[2]));
			} else {
				c.set(Calendar.SECOND, 0);
			}
			c.set(Calendar.MILLISECOND, 0);
			return c;
		} catch (NullPointerException | IndexOutOfBoundsException | NumberFormatException e) {
			Loggers.DAReport.error("Can't parse send_time configuration key: " + send_time, e);
			throw e;
		}
	}
	
	long getNextSendTime() {
		Calendar c = parseSendTime();
		if (c.getTimeInMillis() < System.currentTimeMillis()) {
			return c.getTimeInMillis() + TimeUnit.DAYS.toMillis(1);
		}
		
		return c.getTimeInMillis();
	}
	
	long getPreviousSendTime() {
		Calendar c = parseSendTime();
		if (c.getTimeInMillis() > System.currentTimeMillis()) {
			return c.getTimeInMillis() - TimeUnit.DAYS.toMillis(1);
		}
		
		return c.getTimeInMillis();
	}
	
	public static void setPlannedTask() {
		if (Configuration.global.isElementExists("dareport_setup") == false) {
			return;
		}
		String[] time_unit = get().send_time.split(":");
		long start_time_after_midnight = TimeUnit.HOURS.toMillis(Integer.parseInt(time_unit[0]));
		start_time_after_midnight += TimeUnit.MINUTES.toMillis(Integer.parseInt(time_unit[1]));
		if (time_unit.length > 2) {
			start_time_after_midnight += TimeUnit.SECONDS.toMillis(Integer.parseInt(time_unit[2]));
		}
		
		final String name = Configuration.global.getValue("dareport_setup", "name", "Daily activity report mail");
		
		/**
		 * Local time
		 */
		Calendar c_first_time_to_start = Calendar.getInstance();
		c_first_time_to_start.set(Calendar.HOUR_OF_DAY, 0);
		c_first_time_to_start.set(Calendar.MINUTE, 0);
		c_first_time_to_start.set(Calendar.SECOND, 0);
		c_first_time_to_start.set(Calendar.MILLISECOND, 0);
		c_first_time_to_start.setTimeInMillis(c_first_time_to_start.getTimeInMillis() + start_time_after_midnight);
		if (c_first_time_to_start.getTimeInMillis() < System.currentTimeMillis()) {
			c_first_time_to_start.setTimeInMillis(c_first_time_to_start.getTimeInMillis() + TimeUnit.DAYS.toMillis(1));
		}
		long initial_delay = c_first_time_to_start.getTimeInMillis() - System.currentTimeMillis();
		
		Loggers.DAReport.info("Prepare regular reports send to " + Loggers.dateLog(c_first_time_to_start.getTimeInMillis()) + " (in " + initial_delay / 1000 + " sec), every the " + start_time_after_midnight / 1000 + " sec after midnight, for " + name);
		
		DARDB.instance.scheduled_ex_service.scheduleAtFixedRate(() -> {
			try {
				new DARMails().sendDailyReports(name, DARDB.instance);
			} catch (Exception e) {
				Loggers.DAReport.error("Can't send report for " + name, e);
			}
		}, initial_delay, TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);
	}
	
}
