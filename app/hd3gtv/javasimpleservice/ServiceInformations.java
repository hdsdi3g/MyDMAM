/*
 * This file is part of Java Simple ServiceManager
 * 
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2008-2013
 * 
*/

package hd3gtv.javasimpleservice;

import hd3gtv.log2.Log2Dumpable;

/**
 * @deprecated
 */
public interface ServiceInformations extends Log2Dumpable {
	
	String getApplicationName();
	
	String getApplicationVersion();
	
	String getApplicationCopyright();
	
	String getApplicationShortName();
}
