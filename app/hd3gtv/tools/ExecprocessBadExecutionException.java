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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.tools;

import java.io.IOException;

public class ExecprocessBadExecutionException extends IOException {
	
	private static final long serialVersionUID = -8807563945004074931L;
	private String processname;
	private String commandline;
	private int returncode;
	
	ExecprocessBadExecutionException(String processname, String commandline, int returncode) {
		super("Exec \"" + commandline + "\" return code " + returncode);
		this.commandline = commandline;
		this.returncode = returncode;
		this.processname = processname;
	}
	
	public int getReturncode() {
		return returncode;
	}
	
	public String getCommandline() {
		return commandline;
	}
	
	public String getProcessname() {
		return processname;
	}
	
}
