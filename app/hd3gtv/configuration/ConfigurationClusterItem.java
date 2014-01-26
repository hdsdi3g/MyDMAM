/*
 * This file is part of YAML Configuration for MyDMAM
 * 
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.configuration;

import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;

public class ConfigurationClusterItem implements Log2Dumpable {
	
	public String address;
	public int port;
	
	ConfigurationClusterItem(String address, int port) {
		this.address = address;
		this.port = port;
	}
	
	public Log2Dump getLog2Dump() {
		return new Log2Dump("clusterhost", address + ":" + port);
	}
	
}
