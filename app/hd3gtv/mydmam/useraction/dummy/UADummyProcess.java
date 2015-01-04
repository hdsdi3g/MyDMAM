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

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.manager.JobProgression;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.useraction.UAConfigurator;
import hd3gtv.mydmam.useraction.UAJobProcess;

import java.util.HashMap;
import java.util.Map;

import models.UserProfile;

public class UADummyProcess implements UAJobProcess {
	
	private boolean stop;
	
	public void process(JobProgression progression, UserProfile userprofile, UAConfigurator user_configuration, HashMap<String, SourcePathIndexerElement> source_elements) throws Exception {
		stop = false;
		
		// progress.updateProgress(0);
		// progress.updateProgress_size(100);
		
		Log2Dump dump = new Log2Dump();
		dump.add("by", userprofile.longname);
		
		Log2.log.info("Start dummy process", dump);
		
		UADummyConfigurator configurator = user_configuration.getObject(UADummyConfigurator.class);
		if (configurator != null) {
			dump.addAll(configurator.getLog2Dump());
		} else {
			Log2.log.info("No configurator for this dummy process !", dump);
		}
		
		for (Map.Entry<String, SourcePathIndexerElement> entry : source_elements.entrySet()) {
			if (entry.getValue().directory) {
				dump.add("item", entry.getValue().storagename + ":" + entry.getValue().currentpath + " [dir]");
			} else {
				dump.add("item", entry.getValue().storagename + ":" + entry.getValue().currentpath + " [file]");
			}
		}
		
		// progress.updateLastMessage("Starts with " + source_elements.size() + " items");
		for (int pos = 0; pos < 100; pos++) {
			if (stop) {
				return;
			}
			// progress.updateProgress(pos);
			Thread.sleep(20);
		}
		// progress.updateProgress(100);
		
		// progress.updateLastMessage("Done with " + source_elements.size() + " items");
		
		Log2.log.info("Dummy process is done", dump);
	}
	
	public synchronized void forceStopProcess() throws Exception {
		stop = true;
	}
	
}
