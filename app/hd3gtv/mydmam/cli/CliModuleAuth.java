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
package hd3gtv.mydmam.cli;

import hd3gtv.mydmam.auth.AuthTurret;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.tools.ApplicationArgs;

public class CliModuleAuth implements CliModule {
	
	public String getCliModuleName() {
		return "auth";
	}
	
	public String getCliModuleShortDescr() {
		return "Auth tools for db";
	}
	
	public void execCliModule(ApplicationArgs args) throws Exception {
		if (args.getParamExist("-resetadminpasswd")) {
			AuthTurret turret = new AuthTurret(CassandraDb.getkeyspace());
			String new_password = turret.resetAdminPassword();
			System.out.println("New password for admin account: " + new_password);
			return;
		}
		
		showFullCliModuleHelp();
	}
	
	public void showFullCliModuleHelp() {
		System.out.println("Usage for reset default admin password:");
		System.out.println(" -resetadminpasswd");
	}
	
}
