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

import hd3gtv.mydmam.useraction.UASelectAsyncOptions;

import java.util.ArrayList;
import java.util.List;

public class UADummyStaticChooseList implements UASelectAsyncOptions {
	
	public List<String> getSelectOptionsList() {
		ArrayList<String> list = new ArrayList<String>();
		list.add("huey_duck");
		list.add("dewey_duck");
		list.add("louie_duck");
		return list;
	}
	
}