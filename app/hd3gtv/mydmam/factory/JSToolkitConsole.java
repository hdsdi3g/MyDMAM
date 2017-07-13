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
package hd3gtv.mydmam.factory;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

public class JSToolkitConsole {
	
	private Logger logger;
	
	JSToolkitConsole(Logger logger) {
		this.logger = logger;
		if (logger == null) {
			throw new NullPointerException("\"logger\" can't to be null");
		}
	}
	
	private String join(Object... content) {
		return Arrays.asList(content).stream().map(raw -> {
			return String.valueOf(raw);
		}).collect(Collectors.joining(", "));
	}
	
	public void log(Object... content) {
		if (logger.isInfoEnabled()) {
			logger.info("Javascript says: " + join(content));
		}
	}
	
	public void error(Object... content) {
		logger.error("Javascript says: " + join(content));
	}
	
	public void err(Object... content) {
		logger.error("Javascript says: " + join(content));
	}
	
	public void trace(Object... content) {
		if (logger.isDebugEnabled()) {
			logger.debug("Javascript says: " + join(content));
		}
	}
	
}
