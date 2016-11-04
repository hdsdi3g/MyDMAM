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
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
*/
package hd3gtv.mydmam.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import hd3gtv.mydmam.MyDMAM;

public class BabelException extends Exception {
	
	private ArrayList<String> babel_stacktrace;
	
	private BabelException(String babel_error_message, ArrayList<String> babel_stacktrace) {
		super(babel_error_message);
		this.babel_stacktrace = babel_stacktrace;
		if (babel_stacktrace == null) {
			this.babel_stacktrace = new ArrayList<>(1);
		}
	}
	
	private BabelException(String babel_error_message) {
		super(babel_error_message);
		this.babel_stacktrace = new ArrayList<>(1);
	}
	
	static BabelException create(String raw_stderr_message) throws IOException {
		if (raw_stderr_message == null) {
			throw new NullPointerException("\"raw_stderr_message\" can't to be null");
		}
		ArrayList<String> message = new ArrayList<String>(Arrays.asList(raw_stderr_message.trim().split(MyDMAM.LINESEPARATOR)));
		
		if (message.get(0).toLowerCase().startsWith("SyntaxError".toLowerCase())) {
			if (message.size() > 1) {
				String babel_error_message = message.get(0);
				message.remove(0);
				return new BabelException(babel_error_message, message);
			}
			return new BabelException(message.get(0));
		} else {
			throw new IOException("Babel error: " + raw_stderr_message);
		}
	}
	
	ArrayList<String> getBabelStacktrace() {
		return babel_stacktrace;
	}
}
