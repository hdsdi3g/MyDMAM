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

import java.util.HashMap;

import org.apache.ftpserver.DataConnectionConfigurationFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.listener.ListenerFactory;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.manager.ServiceNG;

public class ServiceNGFTPServer extends ServiceNG {
	
	public ServiceNGFTPServer(String[] args) throws Exception {
		super(args, "Apache FTP Server service handler");
	}
	
	private FtpServer server;
	
	protected void startService() throws Exception {
		FtpServerFactory serverFactory = new FtpServerFactory();
		
		ListenerFactory factory = new ListenerFactory();
		factory.setPort(2221);
		
		DataConnectionConfigurationFactory dccf = new DataConnectionConfigurationFactory();
		dccf.setActiveEnabled(true);
		dccf.setActiveIpCheck(true);
		dccf.setActiveLocalAddress("127.0.0.1");
		dccf.setActiveLocalPort(2220);
		dccf.setIdleTime(600);
		dccf.setImplicitSsl(false);
		dccf.setPassiveAddress("127.0.0.1");
		dccf.setPassiveExternalAddress("127.0.0.1");
		dccf.setPassivePorts("12250-13250");
		factory.setDataConnectionConfiguration(dccf.createDataConnectionConfiguration());
		serverFactory.addListener("default", factory.createListener());
		
		FTPUserManager ftpum = new FTPUserManager(Configuration.global.getValue("ftpserver", "domain", ""));
		serverFactory.setUserManager(ftpum);
		
		HashMap<String, Ftplet> ftplets = new HashMap<String, Ftplet>(1);
		ftplets.put("default", new FTPlet());
		serverFactory.setFtplets(ftplets);
		
		server = serverFactory.createServer();
		server.start();
	}
	
	protected void stopService() throws Exception {
		if (server != null) {
			server.stop();
		}
		server = null;
	}
	
	protected boolean startBroker() {
		return false;
	}
	
}
