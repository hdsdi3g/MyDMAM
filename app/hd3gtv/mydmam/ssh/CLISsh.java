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
package hd3gtv.mydmam.ssh;

import hd3gtv.mydmam.cli.CLIDefinition;
import hd3gtv.tools.ApplicationArgs;

public class CLISsh implements CLIDefinition {
	
	public String getCliModuleName() {
		return "ssh";
	}
	
	public String getCliModuleShortDescr() {
		return "Operations on internal ssh remote hosts database";
	}
	
	public void execCliModule(ApplicationArgs args) throws Exception {
		if (args.getParamExist("-add")) {
			String connection_name = args.getSimpleParamValue("-add");
			String host = args.getSimpleParamValue("-host");
			String username = args.getSimpleParamValue("-user");
			String password = args.getSimpleParamValue("-password");
			int port = args.getSimpleIntegerParamValue("port", 22);
			boolean create_remote_authorized_file = !args.getParamExist("-nocreaterauth");
			Ssh.getGlobal().declareHost(host, port, username, password, connection_name, create_remote_authorized_file);
		} else {
			showFullCliModuleHelp();
		}
	}
	
	public void showFullCliModuleHelp() {
		System.out.println("Usage for declare a new remote host: ");
		System.out.println(" " + getCliModuleName() + " -add connectionname -host hostname [-port port] -user username -password password [-nocreaterauth]");
		System.out.println(" -nocreaterauth for no automatic create authorized file and add MyDMAM id_rsa public key on remote host.");
	}
	
}
