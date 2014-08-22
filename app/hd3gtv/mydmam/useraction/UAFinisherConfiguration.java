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
	boolean soft_refresh_source_storage_index_item;
	boolean force_refresh_source_storage_index_item;
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("remove_user_basket_item", remove_user_basket_item);
		dump.add("soft_refresh_source_storage_index_item", soft_refresh_source_storage_index_item);
		dump.add("force_refresh_source_storage_index_item", force_refresh_source_storage_index_item);
		return dump;
	}
	
	public UAFinisherConfiguration setRemove_user_basket_item(boolean remove_user_basket_item) {
		this.remove_user_basket_item = remove_user_basket_item;
		return this;
	}
	
	public UAFinisherConfiguration setForce_refresh_source_storage_index_item(boolean force_refresh_source_storage_index_item) {
		this.force_refresh_source_storage_index_item = force_refresh_source_storage_index_item;
		return this;
	}
	
	public UAFinisherConfiguration setSoft_refresh_source_storage_index_item(boolean soft_refresh_source_storage_index_item) {
		this.soft_refresh_source_storage_index_item = soft_refresh_source_storage_index_item;
		return this;
	}
	
}
