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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.ftpserver.DataConnectionConfigurationFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.listener.ListenerFactory;

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.ConfigurationClusterItem;
import hd3gtv.configuration.ConfigurationItem;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.manager.ServiceNG;

public class ServiceNGFTPServer extends ServiceNG {
	
	public ServiceNGFTPServer(String[] args) throws Exception {
		super(args, "Apache FTP Server service handler");
		servers = new ArrayList<FtpServer>();
		FTPGroup.registerAppManager(getManager());
	}
	
	private ArrayList<FtpServer> servers;
	private FTPOperations operations;
	
	protected void startService() throws Exception {
		if (Configuration.global.isElementExists("ftpserverinstances") == false) {
			return;
		}
		
		HashMap<String, ConfigurationItem> all_instances_confs = Configuration.global.getElement("ftpserverinstances");
		if (all_instances_confs.isEmpty()) {
			return;
		}
		
		HashMap<String, Ftplet> ftplets = new HashMap<String, Ftplet>(1);
		ftplets.put("default", new FTPlet());
		
		for (Map.Entry<String, ConfigurationItem> entry : all_instances_confs.entrySet()) {
			try {
				servers.add(ftpServerFactory(entry.getKey(), all_instances_confs, ftplets));
			} catch (Exception e) {
				Loggers.FTPserver.error("Can't load FTP server instance " + entry.getKey(), e);
			}
		}
		
		if (servers.isEmpty()) {
			return;
		}
		
		operations = FTPOperations.get();
		operations.start();
	}
	
	protected void stopService() throws Exception {
		for (int pos = 0; pos < servers.size(); pos++) {
			servers.get(pos).stop();
		}
		
		if (operations != null) {
			operations.stop();
		}
		operations = null;
	}
	
	protected boolean startBroker() {
		return false;
	}
	
	private FtpServer ftpServerFactory(String name, HashMap<String, ConfigurationItem> all_instances_confs, HashMap<String, Ftplet> ftplets) throws Exception {
		FtpServerFactory server_factory = new FtpServerFactory();
		
		LinkedHashMap<String, Object> log = new LinkedHashMap<String, Object>();
		
		ListenerFactory factory = new ListenerFactory();
		factory.setPort(Configuration.getValue(all_instances_confs, name, "listen", 21));
		log.put("port", factory.getPort());
		
		DataConnectionConfigurationFactory dccf = new DataConnectionConfigurationFactory();
		dccf.setActiveEnabled(true);
		dccf.setActiveIpCheck(true);
		log.put("active", dccf.isActiveEnabled());
		log.put("active IP check", dccf.isActiveIpCheck());
		
		ConfigurationClusterItem local = Configuration.getClusterConfiguration(all_instances_confs, name, "active", "0.0.0.0", 20).get(0);
		dccf.setActiveLocalAddress(local.address);
		dccf.setActiveLocalPort(local.port);
		log.put("active local", dccf.getActiveLocalAddress() + ":" + dccf.getActiveLocalPort());
		
		dccf.setIdleTime(Configuration.getValue(all_instances_confs, name, "idle", 21));
		log.put("Idle time", dccf.getIdleTime());
		
		dccf.setImplicitSsl(false);
		dccf.setPassiveAddress(Configuration.getValue(all_instances_confs, name, "passive-internal", "0.0.0.0"));
		dccf.setPassiveExternalAddress(Configuration.getValue(all_instances_confs, name, "passive-external", "0.0.0.0"));
		dccf.setPassivePorts(Configuration.getValue(all_instances_confs, name, "passive-ports", "30000-40000"));
		log.put("passive", dccf.getPassiveAddress() + ">" + dccf.getPassiveExternalAddress());
		log.put("passive ports", dccf.getPassivePorts());
		
		factory.setDataConnectionConfiguration(dccf.createDataConnectionConfiguration());
		
		server_factory.addListener("default", factory.createListener());
		
		FTPUserManager ftpum;
		if (name == "default") {
			ftpum = new FTPUserManager("");
		} else {
			ftpum = new FTPUserManager(name);
			server_factory.setUserManager(ftpum);
		}
		server_factory.setUserManager(ftpum);
		log.put("User Manager", ftpum);
		
		server_factory.setFtplets(ftplets);
		
		Loggers.FTPserver.info("Start FTP Server: " + log);
		
		FtpServer server = server_factory.createServer();
		server.start();
		return server;
	}
	
}
