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

import java.io.IOException;

import org.apache.log4j.Layout;
import org.apache.log4j.RollingFileAppender;

public class MyRollingFileAppender extends RollingFileAppender {
	
	public MyRollingFileAppender() {
		super();
	}
	
	public MyRollingFileAppender(Layout layout, String filename) throws IOException {
		super(layout, filename);
	}
	
	public MyRollingFileAppender(Layout layout, String filename, boolean append) throws IOException {
		super(layout, filename, append);
	}
	
	public void rollOver() {
		this.qw.write("=== END OF LOG FILE ===" + System.getProperty("line.separator"));
		super.rollOver();
	}
	
}
