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
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

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
	
	// TODO mail engine
	// TODO CLI for cron > send admin mails and send for each user all its reports
	
	static final ColumnFamily<String, String> CF_DAR = new ColumnFamily<String, String>("dareport", StringSerializer.get(), StringSerializer.get());
	private Keyspace keyspace;
	static final int TTL = (int) TimeUnit.DAYS.toSeconds(30 * 6);
	
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
	private ArrayList<String> manager_addrs;
	
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
		
		public String toString() {
			return type + " \"" + label + "\" [" + tips + "] S:" + isstrong;
		}
	}
	
	LinkedHashMap<String, Job> getJobs() {
		return jobs;
	}
	
	private DARDB() {
	}
	
	public String toString() {
		LinkedHashMap<String, Object> log = new LinkedHashMap<String, Object>();
		log.put("jobs", jobs);
		log.put("panels", panels);
		log.put("manager_addrs", manager_addrs);
		return log.toString();
	}
	
	Keyspace getKeyspace() {
		return keyspace;
	}
	
	public ArrayList<Panel> getPanelsForJob(String job_name) {
		String panel_name = jobs.getOrDefault(job_name, new Job()).panels;
		
		if (panel_name == null) {
			new ArrayList<>(1);
		}
		
		return panels.getOrDefault(panel_name, new ArrayList<>(1));
	}
	
	public String getJobLongName(String job_name) {
		return jobs.getOrDefault(job_name, new Job()).name;
	}
	
	public JsonObject allDeclaredJobs() {
		JsonObject result = new JsonObject();
		
		jobs.forEach((name, job) -> {
			result.addProperty(name, job.name);
		});
		
		return result;
	}
	
}
