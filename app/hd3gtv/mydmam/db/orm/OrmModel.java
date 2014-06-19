/*
 * This file is part of MyDMAM
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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam.db.orm;

import hd3gtv.mydmam.db.CassandraDb;

/**
 * Don't forget to set to public var;
 */
public abstract class OrmModel {
	
	public String key;
	
	/**
	 * @return In seconds, 0 by default.
	 */
	protected int getTTL() {
		return 0;
	}
	
	/**
	 * Static call, only for create CF
	 * Set true if datas are not continually refreshed. False if CF will be always small (like a lock table).
	 * @see CassandraDb
	 */
	protected abstract boolean hasLongGracePeriod();
	
}
