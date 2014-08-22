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
package hd3gtv.mydmam.useraction;

import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;

public class UAFinisherConfiguration implements Log2Dumpable {
	
	boolean remove_user_basket_item;
	
	@Override
	public Log2Dump getLog2Dump() {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * TODO Useraction finisher, one by Useraction created task (and required by this).
	 * - It can be, after the job is ended, the refresh original selected Storage index items.
	 * TODO Add Explorer option: simple refresh for a directory (only add new elements) or a forced refresh (recursive with delete).
	 */
}
