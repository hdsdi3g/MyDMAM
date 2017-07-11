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
package hd3gtv.mydmam.cli;

import hd3gtv.archivecircleapi.ACAPI;
import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.assetsxcross.ACAPIInterplayTag;
import hd3gtv.tools.ApplicationArgs;

public class CliModuleAssetsXcross implements CliModule {
	
	private boolean is_interplay_api_configured;
	private boolean is_acapi_configured;
	private boolean is_acapiinterplaytag_configured;
	
	public CliModuleAssetsXcross() {
		is_interplay_api_configured = Configuration.global.isElementKeyExists("interplay", "host");
		is_acapi_configured = ACAPI.loadFromConfiguration() != null;
		is_acapiinterplaytag_configured = ACAPIInterplayTag.isConfigured();
	}
	
	private boolean isACAPIInterplayTagIsFunctionnal() {
		return is_interplay_api_configured & is_acapi_configured & is_acapiinterplaytag_configured;
	}
	
	public String getCliModuleName() {
		return "assets";
	}
	
	public String getCliModuleShortDescr() {
		return "Media databases exchange";
	}
	
	public void execCliModule(ApplicationArgs args) throws Exception {
		if (args.getParamExist("-acitptag") & isACAPIInterplayTagIsFunctionnal()) {
			ACAPIInterplayTag.createFromConfiguration().process(args.getParamExist("-v"));
			return;
		}
		showFullCliModuleHelp();
	}
	
	public void showFullCliModuleHelp() {
		System.out.println("Usage (with no confirm)");
		if (isACAPIInterplayTagIsFunctionnal()) {
			System.out.println(" " + getCliModuleName() + " -acitptag [-v] For start " + ACAPIInterplayTag.class.getSimpleName() + " operation");
		}
	}
	
	public boolean isFunctionnal() {
		return isACAPIInterplayTagIsFunctionnal();
	}
}
