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
 * Copyright (C) hdsdi3g for hd3g.tv 2015
 * 
*/
package hd3gtv.mydmam.useraction.fileoperation;

import hd3gtv.mydmam.metadata.MetadataIndexingOperation.MetadataIndexingLimit;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public class UAFileOperationReProcessMetadatasConfigurator implements Serializable {
	
	public boolean refresh_path_index_before;
	
	public MetadataIndexingLimit limit_processing;
	
	public enum LimitToRecent {
		ALL, LAST_MONTH, LAST_WEEK, LAST_DAY, LAST_HOUR, LAST_10MINUTES;
		
		long toDate() {
			switch (this) {
			case ALL:
				return 0;
			case LAST_MONTH:
				return System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30);
			case LAST_WEEK:
				return System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7);
			case LAST_DAY:
				return System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
			case LAST_HOUR:
				return System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);
			case LAST_10MINUTES:
				return System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10);
			default:
				return 0;
			}
		}
		
	}
	
	public LimitToRecent limit_to_recent;
	
}
