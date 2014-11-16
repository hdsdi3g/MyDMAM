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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.mydmam.useraction.dummy;

import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;
import hd3gtv.mydmam.db.orm.annotations.TypeEmail;
import hd3gtv.mydmam.db.orm.annotations.TypeLongText;
import hd3gtv.mydmam.db.orm.annotations.TypeNavigatorInputSelection;
import hd3gtv.mydmam.db.orm.annotations.TypePassword;
import hd3gtv.mydmam.pathindexing.Explorer;

import java.io.Serializable;

public class UADummyConfigurator implements Serializable, Log2Dumpable {
	
	public String avalue;
	
	public int anumber;
	
	@TypeEmail
	public String amail;
	
	@TypeLongText
	public String alongtext;
	
	@TypePassword
	public String apassword;
	
	@TypeNavigatorInputSelection(canselectdirs = false, canselectstorages = false)
	public String path;
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("avalue", avalue);
		dump.add("anumber", anumber);
		dump.add("amail", amail);
		dump.add("alongtext", alongtext);
		dump.add("apassword", apassword);
		Explorer ex = new Explorer();
		dump.add("path", path);
		dump.add("path file", ex.getelementByIdkey(path));
		return dump;
	}
}
