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
package hd3gtv.mydmam.assetsxcross;

import java.io.File;
import java.nio.charset.Charset;

import hd3gtv.archivecircleapi.ACAPI;
import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.cli.CLIDefinition;
import hd3gtv.tools.ApplicationArgs;
import hd3gtv.tools.CopyMove;

public class CliModuleAssetsXcross implements CLIDefinition {
	
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
		if (args.getParamExist("-csves")) {
			String celement = args.getSimpleParamValue("-celement");
			String ckey = args.getSimpleParamValue("-ckey");
			Object conf = Configuration.global.getRawValue(celement, ckey);
			if (conf == null) {
				System.err.println("Can't found conf with " + celement + ":" + ckey + ", see -celement and -ckey params");
				return;
			}
			
			Charset charset = Charset.forName("ISO-8859-15");
			if (args.getParamExist("-charset")) {
				charset = Charset.forName(args.getSimpleParamValue("-charset"));
			}
			
			String esindex = args.getSimpleParamValue("-esindex");
			String estype = args.getSimpleParamValue("-estype");
			if (esindex == null) {
				System.err.println("-esindex must to be set");
				return;
			}
			if (estype == null) {
				System.err.println("-estype must to be set");
				return;
			}
			
			if (args.getParamExist("-csv") == false) {
				System.err.println("You must to set a file name with -csv");
				return;
			}
			File csv = new File(args.getSimpleParamValue("-csv"));
			CopyMove.checkExistsCanRead(csv);
			
			CSVESImporter csves_importer = new CSVESImporter(charset, conf, esindex, estype);
			csves_importer.importCSV(csv);
			return;
		}
		showFullCliModuleHelp();
	}
	
	public void showFullCliModuleHelp() {
		System.out.println("Usage (with no confirm)");
		System.out.println(" * Tag Interplay assets with ACAPI");
		if (isACAPIInterplayTagIsFunctionnal()) {
			System.out.println("  " + getCliModuleName() + " -acitptag [-v] For start " + ACAPIInterplayTag.class.getSimpleName() + " operation");
		}
		System.out.println(" * Import MS Excel CSV to ElasticSearch");
		System.out.println("  " + getCliModuleName() + " -csves -celement <CElement> -ckey <CKey> -esindex <Index> -estype <Type> [-charset ISO-8859-15] -csv <file.csv>");
		System.out.println("  With:");
		System.out.println("  -celement Configuration Element for setup importation");
		System.out.println("  -ckey     Configuration Key for setup importation");
		System.out.println("  -esindex  ES Index to push datas");
		System.out.println("  -estype   ES Type to push datas");
		System.out.println("  -charset  Source datas charset code");
		System.out.println("  -csv      CSV file to import");
	}
	
}
