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
package hd3gtv.mydmam.log;

import org.apache.log4j.Level;

public class MainClass {
	
	public static void main(String[] args) {
		// TODO live edit filter level...
		
		// Log2.log.info("Test");
		System.out.println(Loggers.getAllLevels());
		Loggers.changeRootLevel(Level.INFO);
		
		Loggers.LogTest.assertLog(false, Level.DEBUG, "bad boolean");
		
		Loggers.LogTest.trace("msg de trace");
		Loggers.LogTest.debug("msg de debogage");
		Loggers.LogTest.info("msg d'information");
		Loggers.LogTest.warn("msg d'avertissement");
		Loggers.LogTest.error("msg d'erreur", new Exception("A"));
		Loggers.LogTest.fatal("msg d'erreur fatale", new Exception("A"));
		
	}
}
