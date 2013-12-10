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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam.module;

import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public interface ArchivingTapeLocalisator {
	
	/**
	 * @return [storageindexname1, storageindexname2]
	 */
	public JSONArray getStorageIndexNameJsonListForHostedInArchiving();
	
	/**
	 * Resolve pathindexes keys in tapes location
	 * @return key1 : [Loc1, Loc2],
	 */
	public Map<String, List<String>> getPositions(String[] key);
	
	/**
	 * @return tapename: {isexternal, barcode, location},
	 */
	public JSONObject getPositionInformationsByTapeName(String... tapenames);
	
}
