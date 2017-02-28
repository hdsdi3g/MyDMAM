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
 * Copyright (C) hdsdi3g for hd3g.tv 2012-2013
 * 
*/
package hd3gtv.mydmam;

import hd3gtv.mydmam.manager.ServiceNG;

public class MainClass {
	
	public static void main(String[] args) throws Exception {
		Loggers.Manager.info("Start application");
		Loggers.displayCurrentConfiguration();
		MyDMAM.testIllegalKeySize();
		new ServiceNG(args).startAllServices();
	}
}
