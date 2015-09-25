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
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

import hd3gtv.configuration.Configuration;

public class MyDMAMPatternLayout extends PatternLayout {
	
	private static boolean log_show_debug_caller = false;
	private static boolean log_show_normal_caller = false;
	private static boolean log_show_warn_caller = false;
	
	static {
		if (Configuration.global.isElementKeyExists("service", "log_show_debug_caller")) {
			log_show_debug_caller = Configuration.global.getValueBoolean("service", "log_show_debug_caller");
		}
		if (Configuration.global.isElementKeyExists("service", "log_show_normal_caller")) {
			log_show_normal_caller = Configuration.global.getValueBoolean("service", "log_show_normal_caller");
		}
		if (Configuration.global.isElementKeyExists("service", "log_show_warn_caller")) {
			log_show_warn_caller = Configuration.global.getValueBoolean("service", "log_show_warn_caller");
		}
	}
	
	private String pattern_normal = "%d{ISO8601} %-5p %c\t%m%n";
	private String pattern_verbose = "%d{ISO8601} %-5p %c \"%m\" %t%n at %C.%M(%F:%L)%n";
	
	private PatternLayout normal_pl;
	private PatternLayout verbose_pl;
	
	public MyDMAMPatternLayout() {
		normal_pl = new PatternLayout(pattern_normal);
		verbose_pl = new PatternLayout(pattern_verbose);
	}
	
	public MyDMAMPatternLayout(String pattern) {
		super(pattern);
		normal_pl = new PatternLayout(pattern_normal);
		verbose_pl = new PatternLayout(pattern_verbose);
	}
	
	public String format(LoggingEvent event) {
		if (log_show_debug_caller & (event.getLevel().isGreaterOrEqual(Level.INFO) == false)) {
			return verbose_pl.format(event);
		}
		if (log_show_normal_caller & (event.getLevel().isGreaterOrEqual(Level.WARN) == false)) {
			return verbose_pl.format(event);
		}
		if (log_show_warn_caller) {
			return verbose_pl.format(event);
		}
		return normal_pl.format(event);
	}
	
}
