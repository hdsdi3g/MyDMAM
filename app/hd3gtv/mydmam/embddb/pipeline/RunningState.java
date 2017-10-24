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
 * Copyright (C) hdsdi3g for hd3g.tv 24 oct. 2017
 * 
*/
package hd3gtv.mydmam.embddb.pipeline;

public enum RunningState {
	
	WAKE_UP {
		public boolean canAccessToDatas() {
			return false;
		}
	},
	SYNC_LAST {
		public boolean canAccessToDatas() {
			return false;
		}
	},
	ON_THE_FLY {
		public boolean canAccessToDatas() {
			return true;
		}
	};
	
	public abstract boolean canAccessToDatas();
	
}
