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
package hd3gtv.mydmam;

import org.boris.winrun4j.AbstractService;
import org.boris.winrun4j.EventLog;
import org.boris.winrun4j.ServiceException;

public class WindowsService extends AbstractService {
	
	public static final String SERVICE_NAME = "MyDMAM Service";
	
	public int serviceMain(String[] args) throws ServiceException {
		EventLog.report(SERVICE_NAME, EventLog.INFORMATION, "Starting...");
		
		try {
			MainClass.main(args);
			
			while (!shutdown) {
				Thread.sleep(1000);
			}
		} catch (Exception e) {
		}
		EventLog.report(SERVICE_NAME, EventLog.INFORMATION, "Stopping");
		System.exit(0);
		return 0;
	}
	
}
