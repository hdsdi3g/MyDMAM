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
package hd3gtv.mydmam.probe;

import hd3gtv.configuration.Configuration;
import hd3gtv.log2.Log2;
import hd3gtv.mydmam.MyDMAM;

public class MainClass {
	
	public static void main(String[] args) throws Exception {
		Configuration.importLog2Configuration(Configuration.global, true);
		
		MyDMAM.testIllegalKeySize();
		
		Log2.log.info("Start application");
		
		new MyDMAMProbeService(args, null);
		
	}
}
