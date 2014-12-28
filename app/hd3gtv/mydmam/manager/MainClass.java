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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.mydmam.manager;

import hd3gtv.configuration.Configuration;

// TODO #78.3, remove this test class
public class MainClass {
	
	// TODO #78.2, check if UA capacity need no read only for storage
	
	public static void main(String[] args) throws Exception {
		
		Configuration.importLog2Configuration(Configuration.global, true);
		new ServiceNGProbe(args);
	}
	
	// TODO #78.1 Refactoring all actuals workers and tasks creations.
	// TODO #78.1 Refactoring User Action API and display all availability for user and admin
}
