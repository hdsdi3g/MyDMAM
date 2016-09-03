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
package hd3gtv.mydmam.bcastautomation;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import hd3gtv.mydmam.bcastautomation.BCAWatcher.AutomationEventProcessor;

public class BCAMorpheus implements BCAEngine {
	
	public String getVendorName() {
		return "MyDMAM";
	}
	
	public String getName() {
		return "Morpheus";
	}
	
	public String getVersion() {
		return "1.0";
	}
	
	public List<String> getValidFileExtension() {
		return Arrays.asList("sch");
	}
	
	public int processAsRunFile(File asrun, AutomationEventProcessor processor) throws IOException {
		BCAMorpheusScheduleParser parser = new BCAMorpheusScheduleParser(this, asrun);
		
		parser.getEvents().forEach(event -> {
			processor.onAutomationEvent(event.getBCAEvent(parser));
		});
		return parser.getEvents().size();
	}
	
	public int processPlaylistFile(File playlist, AutomationEventProcessor processor) throws IOException {
		BCAMorpheusScheduleParser parser = new BCAMorpheusScheduleParser(this, playlist);
		
		parser.getEvents().forEach(event -> {
			processor.onAutomationEvent(event.getBCAEvent(parser));
		});
		return parser.getEvents().size();
	}
	
}
