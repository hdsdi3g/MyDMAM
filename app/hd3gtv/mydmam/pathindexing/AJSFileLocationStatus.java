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
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
*/
package hd3gtv.mydmam.pathindexing;

import java.util.ArrayList;

import hd3gtv.archivecircleapi.ACAccessibility;
import hd3gtv.archivecircleapi.ACFile;
import hd3gtv.archivecircleapi.ACFileLocationCache;
import hd3gtv.archivecircleapi.ACFileLocationPack;
import hd3gtv.archivecircleapi.ACFileLocationTape;
import hd3gtv.archivecircleapi.ACLocationType;
import hd3gtv.archivecircleapi.ACPositionType;

public class AJSFileLocationStatus {
	
	public ACAccessibility availability = ACAccessibility.OFFLINE;
	public ArrayList<Location> locations = new ArrayList<>(1);
	
	public class Location {
		ACLocationType type;
		ACAccessibility availability;
		String source;
		
	}
	
	private void addLocation(ACLocationType type, ACAccessibility availability, String source) {
		Location l = new Location();
		l.availability = availability;
		l.type = type;
		l.source = source;
		locations.add(l);
	}
	
	public void getFromACAPI(ACFile acfile) {
		if (acfile.this_locations != null) {
			/**
			 * File
			 */
			availability = acfile.accessibility;
			
			acfile.this_locations.forEach(l -> {
				if (l instanceof ACFileLocationCache) {
					ACFileLocationCache location = (ACFileLocationCache) l;
					location.nodes.forEach(n -> {
						addLocation(ACLocationType.CACHE, ACAccessibility.ONLINE, n);
					});
				} else if (l instanceof ACFileLocationPack) {
					ACFileLocationPack location = (ACFileLocationPack) l;
					
					location.partitions.forEach(p -> {
						if (p.accessibility == ACAccessibility.OFFLINE) {
							addLocation(ACLocationType.PACK, ACAccessibility.OFFLINE, location.pool);
						} else {
							addLocation(ACLocationType.PACK, ACAccessibility.ONLINE, location.pool + " " + p.name);
						}
					});
					
				} else if (l instanceof ACFileLocationTape) {
					ACFileLocationTape location = (ACFileLocationTape) l;
					location.tapes.forEach(t -> {
						if (t.accessibility == ACAccessibility.OFFLINE) {
							addLocation(ACLocationType.TAPE, ACAccessibility.OFFLINE, t.barcode);
						} else {
							addLocation(ACLocationType.TAPE, ACAccessibility.NEARLINE, t.barcode);
						}
					});
				}
			});
		}
		
		if (acfile.sub_locations != null) {
			/**
			 * Dir
			 */
			availability = acfile.sub_locations.worstAccessibility;
			if (acfile.sub_locations.tapes != null) {
				ACPositionType pt_tapes = acfile.sub_locations.tapes;
				if (pt_tapes.nearline != null) {
					pt_tapes.nearline.forEach(t -> {
						addLocation(ACLocationType.TAPE, ACAccessibility.NEARLINE, t);
					});
				}
				if (pt_tapes.offline != null) {
					pt_tapes.offline.forEach(t -> {
						addLocation(ACLocationType.TAPE, ACAccessibility.OFFLINE, t);
					});
				}
			}
		}
	}
	
}
